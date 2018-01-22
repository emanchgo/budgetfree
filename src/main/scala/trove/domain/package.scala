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

package trove

import trove.domain.AccountTypes.AccountType

package object domain {

  object AccountTypes {
    sealed trait AccountType
    case object Equity extends AccountType
    case object Income extends AccountType
    case object Expense extends AccountType
    case object Asset extends AccountType
    case object Cash extends AccountType
    case object Bank extends AccountType
    case object Security extends AccountType
    case object Lending extends AccountType // for loans user makes to others
    case object Liability extends AccountType
    case object CreditCard extends AccountType
    case object FixedRateLoan extends AccountType
    case object AdjustableRateLoan extends AccountType
  }

  case class Account(id: Int, accountType: AccountType, name: String, placeholder: Boolean = false, description: Option[String] = None,
                     parentAccountId: Option[Int] = None)

}