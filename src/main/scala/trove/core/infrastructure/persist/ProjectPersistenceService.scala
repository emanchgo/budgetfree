/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2021 Eric John Fredericks.
 *
 *  Trove is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Trove is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Trove.  If not, see <http://www.gnu.org/licenses/>.
 */

package trove.core.infrastructure.persist

import java.io.File
import java.util.Properties

import grizzled.slf4j.Logging
import slick.jdbc.DriverDataSource
import slick.util.ClassLoaderUtil
import trove.constants.ProjectsHomeDir
import trove.core.infrastructure.persist.lock.{LockResourceReleaseErrorHandling, ProjectLock}
import trove.core.infrastructure.persist.schema.Tables
import trove.core.{Project, Trove}
import trove.events.ProjectChanged
import trove.exceptional.{PersistenceError, PersistenceException, SystemException, ValidationError}
import trove.services.ProjectService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

private[persist] trait HasShutdownHook extends Logging { self : ProjectService =>

  private[persist] val shutdownHook = new Thread() {
    override def run(): Unit = {
      logger.warn(s"Shutdown hook executing")
      closeCurrentProject() match {
        case Success(_) =>
        case Failure(NonFatal(e)) =>
          logger.error("Shutdown hook was unable to execute for project persistence service", e)
        case Failure(e) =>
          throw e
      }
    }
  }
  Runtime.getRuntime.addShutdownHook(shutdownHook)
}

private[core] object ProjectPersistenceService {

  val ValidProjectNameChars: String = "^[a-zA-Z0-9_\\-]*$"

  val JdbcPrefix = "jdbc:sqlite:"
  val DbFilenameSuffix: String = ".sqlite3"

  private[this] lazy val instance = new ProjectPersistenceServiceImpl(ProjectsHomeDir) with LivePersistence with HasShutdownHook

  def apply(): ProjectService = instance
}

private[persist] case class DatabaseConfig(
  url: String,
  user: String = null,
  password: String = null,
  properties: Properties = null,
  driverClassName: String = "org.sqlite.JDBC",
  classLoader: ClassLoader = ClassLoaderUtil.defaultClassLoader,
  numThreads: Int = 1,
  minThreads: Int = 1,
  maxThreads: Int = 1,
  queueSize: Int = 1,
  maxConnections: Int = 1,
  keepAliveTime: Duration = 1.minute,
  registerMbeans: Boolean = false,
  keepAliveConnection: Boolean = false
)

private[persist] abstract class ProjectPersistenceServiceImpl(val projectsHomeDir: File)
  extends ProjectService
  with PersistenceOps
  with LockResourceReleaseErrorHandling
  with Logging {

  require(projectsHomeDir.isDirectory)

  import ProjectPersistenceService._
  import slick.jdbc.SQLiteProfile.backend._

  // The current active project
  @volatile private[this] var _currentProject: Option[ProjectImpl] = None
  def currentProject: Option[Project] = _currentProject

  override def listProjects: Try[Seq[String]] = Try {
    projectsHomeDir.listFiles.filter(_.isFile).map(_.getName).filterNot(_.endsWith(ProjectLock.LockfileSuffix)).filterNot(_.startsWith("."))
      .toSeq.map(_.stripSuffix(DbFilenameSuffix)).sorted
  }

  override def open(projectName: String): Try[Project] =
    if (projectName.matches(ValidProjectNameChars)) {
      initializeProject(projectName).flatMap { project =>
        logger.debug(s"Database for project $projectName successfully opened.")
        Trove.eventService.publish(ProjectChanged(Some(project)))
        Success(project)
      }.recoverWith {
        case NonFatal(e) =>
          logger.error(s"Project with name $projectName could not be initialized. Closing project (if it was open).")
          closeCurrentProject()
          Failure(e)
      }
    }
    else {
      ValidationError(s"""Invalid project name: "$projectName." Valid characters are US-ASCII alphanumeric characters, '_', and '-'.""")
    }

  private[persist] def initializeProject(projectName: String): Try[ProjectImpl] = _currentProject.fold[Try[ProjectImpl]] {
    logger.debug(s"Opening project: $projectName")

    val projectLock: ProjectLock = newProjectLock(projectsHomeDir, projectName)
    val lockResult = projectLock.lock()

    val dbFileName = s"$projectName$DbFilenameSuffix"
    val dbFile = createDbFile(projectsHomeDir, dbFileName)
    val dbURL = s"$JdbcPrefix${dbFile.getAbsolutePath}"
    val create: Boolean = !dbFile.exists()

    val openResult: Try[DatabaseDef] = lockResult.flatMap { _ =>
      openDatabase(DatabaseConfig(dbURL))
    }

    val projectResult: Try[ProjectImpl] = openResult.flatMap {
      db =>
        val setupResult: Future[Unit] = if (create) {
          db.runDbAction(Tables.setupAction)
        }
        else {
          Future.successful(())
        }

        val versionCheckResult: Future[Try[ProjectImpl]] = setupResult.flatMap { _ =>
          db.runDbAction(Tables.versionQuery).map { rows =>
            rows.toList match {
              case Tables.CurrentDbVersion :: Nil =>
                val prj = new ProjectImpl(projectName, projectLock, db)
                Success(prj)
              case _ :: Nil =>
                PersistenceError(s"Invalid database version: ${rows.head.id}")
              case _ =>
                PersistenceError(s"Incorrect number of rows in the VERSION table: found ${rows.size} rows")
            }
          }
        }

        Await.result(versionCheckResult, Duration.Inf)
    }

    projectResult match {
      case Success(prj) =>
        _currentProject = Some(prj)
        logger.info(s"Project opened: ${prj.name}")
      case Failure(e) =>
        logger.error("Error opening project", e)
        projectLock.release()
    }

    projectResult.recoverWith {
      case PersistenceException(_, _) | SystemException(_, _) =>
        projectResult
      case NonFatal(e) =>
        PersistenceError("Error opening project", e)
    }
  } (prj =>
    // Second arg list for the fold. No project.
    PersistenceError(s"Unable to open project - ${prj.name} is currently open")
  )

  private[this] def openDatabase(dbConfig: DatabaseConfig): Try[DatabaseDef] = Try {

    val dds = new DriverDataSource(
      url = dbConfig.url,
      user = dbConfig.user,
      password = dbConfig.password,
      properties = dbConfig.properties,
      driverClassName = dbConfig.driverClassName,
      classLoader = dbConfig.classLoader)

    // N.B. We only need a single thread/worker, so we pass 1 for the number of workers here.
    forDataSource(ds = dds, numWorkers = 1)
  }.recoverWith {
    case NonFatal(e) =>
      PersistenceError("Unable to open database", e)
  }


  override def closeCurrentProject(): Try[Unit] =
    _currentProject.fold[Try[Unit]](Success({})) { project: ProjectImpl =>
      Try(project.close()).map { _ =>
        _currentProject = None
        Trove.eventService.publish(ProjectChanged(None))
      }.recoverWith {
        case NonFatal(e) =>
          logger.error("Error closing project")
          PersistenceError("Unable to close project", e)
      }
  }
}
