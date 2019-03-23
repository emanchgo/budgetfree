/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2019 Eric John Fredericks.
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

package trove

import trove.core.Project
import trove.core.infrastructure.event.Event
import trove.models.Account
import trove.models.AccountType.AccountType

package object events {
  case class ProjectChanged(project: Option[Project]) extends Event

  case class AccountAdded(account: Account) extends Event
  case class AccountUpdated(account: Account) extends Event
  case class AccountDeleted(id: Int, parent: Either[Int, AccountType]) extends Event
  case class AccountParentChanged(account: Account, oldParent: Either[Int, AccountType]) extends Event
}
