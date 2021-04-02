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

import grizzled.slf4j.Logging
import slick.jdbc.SQLiteProfile.backend._
import trove.core.Project
import trove.core.accounts.AccountsServiceImpl
import trove.core.infrastructure.persist.lock.ProjectLock

private[persist] class ProjectImpl(
  val name: String,
  val lock: ProjectLock,
  val db: DatabaseDef)
  extends Project with Logging {

  override def toString: String = s"Project($name)"

  val accountsService = new AccountsServiceImpl

  def close(): Unit = {
    db.close()
    logger.debug(s"Database for project $name closed")
    lock.release()
    logger.debug(s"Lock for project $name released")
    logger.info(s"Closed project $name")
  }
}

