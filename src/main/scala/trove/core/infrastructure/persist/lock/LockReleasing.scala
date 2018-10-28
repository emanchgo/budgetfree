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

package trove.core.infrastructure.persist.lock

import grizzled.slf4j.Logging

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

private[persist] trait LockReleasing extends Logging {
  final def releaseLock(lock: ProjectLock): Unit =
    lock.release() match {
      case Success(_) =>
      case Failure(NonFatal(e)) =>
        logger.error("Error releasing persist lock", e)
      case Failure(e) =>
        throw e
    }
}
