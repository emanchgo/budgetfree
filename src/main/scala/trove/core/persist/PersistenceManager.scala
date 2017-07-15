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

import scala.util.{Success, Try}

private[core] object PersistenceManager extends Logging {

  val DbSuffix: String = ".sqlite3"
  val ValidChars: String = "^[a-zA-Z0-9_\\-]*$"

  private[this] case class Project(name: String) {
    def close(): Unit = {
      ???
    }
  }

  @volatile private[this] var currentProject: Option[Project] = None

  def listProjectNames: Seq[String] =
    ProjectsHomeDir.listFiles.filter(_.isFile).map(_.getName).filterNot(_.endsWith(ProjectGuard.lockfileSuffix))
      .map(_.stripSuffix(DbSuffix)).toSeq.sorted

  def openProject(projectName: String): Try[Unit] = {
    if(projectName.matches(ValidChars)) {
      logger.debug(s"Opening project: $projectName")
      //ejf-fixMe: Move ProjectGuard into Project object
      val lockResult: Try[FileLock] = ProjectGuard(projectName).lock
      val result: Try[Project] = lockResult.flatMap{ _ => Try(Project(projectName))}
      result.recover {
        case e: Exception => logger.error(s"Error locking project $projectName", e)
      }
      currentProject = result.toOption
      result.map { _ =>
       logger.debug(s"Opened project $projectName")
        Unit
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
