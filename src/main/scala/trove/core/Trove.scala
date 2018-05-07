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

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import trove.core.infrastructure.event.EventService
import trove.core.infrastructure.project.ProjectService
import trove.events.ProjectChanged
import trove.exceptional.ValidationError

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object Trove extends Logging {

  private[this] val actorSystem: ActorSystem = ActorSystem("Trove_Actor_System")

  val eventService: EventService = EventService(actorSystem)
  val persistenceService: ProjectService = ProjectService()

  // For project name validation
  private[this] val ValidProjectNameChars: String = "^[a-zA-Z0-9_\\-]*$"

  def startup(): Try[Unit] = Success(Unit)

  def listProjectNames: Try[Seq[String]] = persistenceService.listProjects

  def apply(projectName: String): Try[Trove] =
    if (projectName.matches(ValidProjectNameChars)) {
      persistenceService.open(projectName).map { _ =>
        logger.debug(s"Database for project $projectName successfully opened.")
        val result = new Trove(projectName)
        eventService.publish(ProjectChanged(Some(projectName)))
        result
      }.recoverWith {
        case NonFatal(e) =>
          logger.error(s"Project with name $projectName could not be initialized. Closing project (if it was open).")
          persistenceService.closeCurrentProject()
          Failure(e)
      }
    }
    else {
      ValidationError(s"""Invalid project name: "$projectName." Valid characters are US-ASCII alphanumeric characters, '_', and '-'.""")
    }

  def closeCurrentProject(): Try[Unit] = persistenceService.closeCurrentProject().map { _ =>
    eventService.publish(ProjectChanged(None))
  }

  def shutdown(): Try[Unit] = persistenceService.closeCurrentProject().flatMap { _ =>
    Try(eventService.shutdown()).map(_ => Unit)
  }
}

final class Trove private(val projectName: String) {
  override def toString: String = projectName
}
