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


package trove.core

import grizzled.slf4j.Logging
import trove.core.event.EventService
import trove.core.persist.PersistenceManager
import trove.events.ProjectChanged

import scala.util.{Failure, Success, Try}

object Trove extends Logging {

  def startup(): Try[Unit] = Success(Unit)

  def listProjectNames: Seq[String] = PersistenceManager.listProjectNames

  def apply(projectName: String): Try[Trove] = PersistenceManager.openProject(projectName).flatMap { _ =>
    logger.debug(s"Database for project $projectName successfully opened.")
    val result = Try(new Trove(projectName))
    result.foreach(_ => EventService.publish(ProjectChanged(Some(projectName))))
    result
  }.recoverWith {
    case error: Throwable =>
      logger.error(s"Project with name $projectName could not be initialized. Closing database (if it was open).")
      PersistenceManager.closeCurrentProject
      Failure(error)
  }

  def closeCurrentProject(): Try[Unit] = PersistenceManager.closeCurrentProject

  def shutdown(): Try[Unit] = PersistenceManager.closeCurrentProject.flatMap { _ =>
    Try(EventService.shutdown()).map(_ => Unit)
  }
}

final class Trove private(val projectName: String) {

  override def toString: String = projectName
}
