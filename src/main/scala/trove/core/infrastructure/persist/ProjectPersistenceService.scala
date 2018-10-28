/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2018 Eric John Fredericks.
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
import slick.jdbc.SQLiteProfile.backend._
import slick.util.{AsyncExecutor, ClassLoaderUtil}
import trove.constants.ProjectsHomeDir
import trove.core.infrastructure.persist.lock.{LockReleasing, ProjectLock}
import trove.core.infrastructure.persist.schema.Tables
import trove.core.services.ProjectService
import trove.exceptional.PersistenceError
import trove.models.Project

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

private[persist] trait ShutdownHook extends Logging { self : ProjectService =>

  private[persist] val shutdownHook = new Thread() {
    override def run(): Unit = {
      logger.warn(s"Shutdown hook executing")
      closeCurrentProject() match {
        case Success(_) =>
        case Failure(NonFatal(e)) =>
          logger.error("Shutdown hook was unable to execute for persist service", e)
        case Failure(e) =>
          throw e
      }
    }
  }
  Runtime.getRuntime.addShutdownHook(shutdownHook)
}

private[persist] class ProjectImpl(val name: String, val lock: ProjectLock, val db: DatabaseDef)
  extends LockReleasing with Logging {

  def close(): Unit = {
    db.close()
    logger.debug(s"Database for persist $name closed")
    releaseLock(lock)
    logger.debug(s"Lock for persist $name released")
    logger.info(s"Closed persist $name")
  }
}

private[core] object ProjectPersistenceService {

  val JdbcPrefix = "jdbc:sqlite:"
  val DbFilenameSuffix: String = ".sqlite3"

  private[this] lazy val instance = new ProjectServiceImpl(ProjectsHomeDir) with LivePersistence with ShutdownHook

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

private[persist] abstract class ProjectServiceImpl(val projectsHomeDir: File) extends ProjectService with PersistenceOps with LockReleasing
  with Logging {

  require(projectsHomeDir.isDirectory)

  import ProjectPersistenceService._
  import slick.jdbc.SQLiteProfile.backend._

  // The current active persist
  @volatile private[this] var currentProject: Option[ProjectImpl] = None

  override def listProjects: Try[Seq[String]] = Try {
    projectsHomeDir.listFiles.filter(_.isFile).map(_.getName).filterNot(_.endsWith(ProjectLock.LockfileSuffix)).filterNot(_.startsWith("."))
      .map(_.stripSuffix(DbFilenameSuffix)).sorted
  }

  override def open(projectName: String): Try[Project] = initializeProject(projectName).map(p => Project(p.name))

  private[persist] def initializeProject(projectName: String): Try[ProjectImpl] = currentProject.fold[Try[ProjectImpl]] {
    logger.debug(s"Opening persist: $projectName")

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

        import slick.jdbc.SQLiteProfile.api._

        val setupResult: Future[Unit] = if (create) {
          runDbIOAction(Tables.setupAction)(db)
        }
        else {
          Future.successful(())
        }

        val versionCheckResult: Future[Try[ProjectImpl]] = setupResult.flatMap { _ =>
          runDbIOAction(Tables.version.result)(db).map {
            case rows: Seq[DBVersion] if rows.length == 1 && rows.head == Tables.CurrentDbVersion =>
              val prj = new ProjectImpl(projectName, projectLock, db)
              Success(prj)
            case rows: Seq[DBVersion] if rows.length == 1 =>
              PersistenceError(s"Invalid database version: ${rows.head.id}")
            case rows: Seq[DBVersion] =>
              PersistenceError(s"Incorrect number of rows in the VERSION table: found ${rows.size} rows")
          }
        }

        Await.result(versionCheckResult, Duration.Inf)
    }

    projectResult match {
      case Success(prj) =>
        currentProject = Some(prj)
        logger.info(s"Project opened: ${prj.name}")
      case Failure(e) =>
        logger.error("Error creating persist", e)
        releaseLock(projectLock)
    }

    projectResult

  } (prj =>
    PersistenceError(s"Unable to open persist - ${prj.name} is currently open")
  )

  private[this] def openDatabase(dbConfig: DatabaseConfig): Try[DatabaseDef] = Try {

    val dds = new DriverDataSource(
      url = dbConfig.url,
      user = dbConfig.user,
      password = dbConfig.password,
      properties = dbConfig.properties,
      driverClassName = dbConfig.driverClassName,
      classLoader = dbConfig.classLoader)

    // N.B. We only need a single thread, so we pass 1 here.
    val numWorkers = 1

    val executor = AsyncExecutor(
      name = "AsyncExecutor.trove",
      minThreads = numWorkers,
      maxThreads = numWorkers,
      queueSize = numWorkers,
      maxConnections = numWorkers,
      keepAliveTime = 1.minute,
      registerMbeans = false
    )

    forDataSource(ds = dds, maxConnections =  Some(numWorkers), executor = executor, keepAliveConnection = false)
  }.recoverWith {
    case NonFatal(e) =>
      PersistenceError("Unable to open database", e)
  }


  override def closeCurrentProject(): Try[Unit] =
    currentProject.fold[Try[Unit]](Success({})) { project: ProjectImpl =>
      Try(project.close()).map { _ =>
        currentProject = None
      }.recoverWith {
        case NonFatal(e) =>
          logger.error("Error closing persist")
          PersistenceError("Unable to close persist", e)
      }
  }
}
