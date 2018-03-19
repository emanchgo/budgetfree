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

package trove.core.persist

import java.io.File

import grizzled.slf4j.Logging
import slick.jdbc.SQLiteProfile
import trove.constants.ProjectsHomeDir
import trove.exceptional.ValidationError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Success, Try}

private[core] object PersistenceManager extends Logging {

  import SQLiteProfile.backend._

  val DbSuffix: String = ".sqlite3"
  val ValidChars: String = "^[a-zA-Z0-9_\\-]*$"

  logger.info("Loading SQLite JDBC driver...")
  Class.forName("org.sqlite.JDBC")
  logger.info("SQLite JDBC driver loaded!")

  private[persist] class Project(name: String, lock: ProjectLock, db: DatabaseDef) {
    def close(): Unit = {
      logger.info(s"Closing project $name")
      lock.release()
    }
  }

  @volatile private[this] var currentProject: Option[Project] = None

  def listProjectNames: Seq[String] =
    ProjectsHomeDir.listFiles.filter(_.isFile).map(_.getName).filterNot(_.endsWith(ProjectLock.lockfileSuffix))
      .map(_.stripSuffix(DbSuffix)).toSeq.sorted

  def openProject(projectName: String): Try[Project] = {
    if(projectName.matches(ValidChars)) {
      logger.debug(s"Opening project: $projectName")
      val projectLock: ProjectLock = ProjectLock(projectName)
      val lockResult = projectLock.lock()
      lockResult.map { _ =>
        val dbFileName = s"$projectName$DbSuffix"
        val dbFile = new File(ProjectsHomeDir, dbFileName)
        val create: Boolean = !dbFile.exists()
        //ejf-fixMe: if db doesn't open, release lock
        val dbURL = s"jdbc:sqlite:${dbFile.getAbsolutePath}"
        val db: DatabaseDef = Database.forURL(dbURL)
        (db, create)
      }.map { case (db, create) =>
        val setupFuture = if(create) {
          logger.info(s"Creating database for project $projectName")
          db.run(Tables.setupAction)
        }
        else {
          logger.info(s"Database exists for project $projectName")
          Future successful Unit
        }
        val result: Future[Project] = setupFuture.map(_ => new Project(projectName, projectLock, db))
        val project = Await.result(result, Duration.Inf)
        currentProject = Some(project)
        project
      }
    }
    else {
      ValidationError(s"""Invalid project name: "$projectName." Valid characters are US-ASCII alphanumeric characters, '_', and '-'.""")
    }
  }

  def closeCurrentProject: Try[Unit] = currentProject.fold[Try[Unit]](Success(Unit)) { project =>
    currentProject = None
    Try(project.close())
  }
}
