/*
 *  # BudgetFree
 *
 *  This file is part of BudgetFree - A FREE desktop budgeting application that
 *  helps you track your finances and literally FREES you from complex budgeting.
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
 *
 *  BudgetFree is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  BudgetFree is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with BudgetFree.  If not, see <http://www.gnu.org/licenses/>.
 */


package budgetfree.core

import budgetfree.core.event.EventService
import budgetfree.core.persist.PersistenceManager
import budgetfree.events.ProjectChanged
import grizzled.slf4j.Logging

import scala.util.{Failure, Success, Try}

object BudgetFree extends Logging {

  def startup(): Try[Unit] = Success(Unit)

  def listProjectNames: Seq[String] = PersistenceManager.listProjectNames

  def apply(projectName: String): Try[BudgetFree] = PersistenceManager.openProject(projectName).flatMap { _ =>
    logger.debug(s"Database for project $projectName successfully opened.")
    val result = Try(new BudgetFree(projectName))
    result.foreach(_ => EventService.publish(ProjectChanged(Some(projectName))))
    result
  }.recoverWith {
    case error: Throwable =>
      logger.error(s"Project with name $projectName could not be initialized. Closing database (if it was open).")
      PersistenceManager.closeCurrentProject
      Failure(error)
  }

  def closeCurrentProject(): Try[Unit] = PersistenceManager.closeCurrentProject

  def shutdown(): Try[Unit] = PersistenceManager.closeCurrentProject.map { _ =>
    Unit
  }
}

final class BudgetFree private(val projectName: String) {

  override def toString: String = projectName
}
