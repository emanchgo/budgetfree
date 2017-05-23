/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
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

import java.nio.channels.FileLock

import grizzled.slf4j.Logging
import trove.constants.ProjectsHomeDir
import trove.exceptional.ValidationError
import slick.jdbc.SQLiteProfile.api._

import scala.util.{Success, Try}

private[core] object PersistenceManager extends Logging {

  val validChars: String = "^[a-zA-Z0-9_\\-]*$"

  case class ProjectResources(projectName: String, db: Database)

  // The current project contains the project name and the database reference.
  @volatile private[this] var currentProject: Option[ProjectResources] = None

  def listProjectNames: Seq[String] =
    ProjectsHomeDir.listFiles.filter(_.isFile).map(_.getName).filterNot(_.endsWith(ProjectGuard.lockFileSuffix))
      .map(_.stripSuffix(dbSuffix)).toSeq.sorted

  def openProject(projectName: String): Try[Unit] = {
    if(projectName.matches(validChars)) {
      logger.debug(s"Opening project: $projectName")
      val result: Try[FileLock] = ProjectGuard(projectName).lock
      result.fold(err => logger.error(s"Error locking project $projectName", err), { _ =>
        val db: Database = Database.forURL("file:///home/eric/tmp/abc.sqlite3")
        logger.debug(s"Opened project $projectName")
        currentProject = Some(ProjectResources(projectName, db))
        //ejf-fixMe: publish event
        }
      )
      result.map(_ => Unit)
    }
    else {
      ValidationError(s"""Invalid project name: "$projectName." Valid characters are US-ASCII alphanumeric characters, '_', and '-'.""")
    }
  }

  def closeCurrentProject: Try[Unit] = {
    val (newCurrentProject, closeResult) = currentProject.fold(None, Try(())) { case ProjectResources(projectName, db) =>
      logger.debug(s"Closing project: $projectName")
      val result = Try(db.close())
      logger.debug(s"Project $projectName closed")
      (None, result)
    }

    //ejf-fixMe: publish event

    currentProject = newCurrentProject
    closeResult
  }
}
