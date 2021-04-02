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

package trove.core.accounts

import trove.models.{Account, AccountTypes}
import trove.services.AccountsService

import scala.util.{Success, Try}

class AccountsServiceImpl extends AccountsService {
  import AccountTypes._

  override def getAllAccounts: Try[Seq[Account]] =
    Success(
      Seq(
        Account(id = Some(110), version = 1, accountType=Asset, name="Asset A", code=None, isPlaceholder = true, Some("Asset A Account"), parentAccountId = None),
        Account(id = Some(111), version = 1, accountType=Asset, name="Asset A1", code=None, isPlaceholder = false, Some("Asset A1 Account"), parentAccountId = Some(110)),
        Account(id = Some(112), version = 1, accountType=Asset, name="Asset A2", code=None, isPlaceholder = false, Some("Asset A2 Account"), parentAccountId = Some(110)),

        Account(id = Some(115), version = 1, accountType=Asset, name="Asset B", code=None, isPlaceholder = true, Some("Asset B Account"), parentAccountId = None),
        Account(id = Some(116), version = 1, accountType=Asset, name="Asset B1", code=None, isPlaceholder = false, Some("Asset B1 Account"), parentAccountId = Some(115)),
        Account(id = Some(117), version = 1, accountType=Asset, name="Asset B2", code=None, isPlaceholder = false, Some("Asset B2 Account"), parentAccountId = Some(115)),
        
        Account(id = Some(120), version = 1, accountType=Liability, name="Liability A", code=None, isPlaceholder = true, Some("Liability A Account"), parentAccountId = None),
        Account(id = Some(121), version = 1, accountType=Liability, name="Liability A1", code=None, isPlaceholder = false, Some("Liability A1 Account"), parentAccountId = Some(120)),
        Account(id = Some(122), version = 1, accountType=Liability, name="Liability A2", code=None, isPlaceholder = false, Some("Liability A2 Account"), parentAccountId = Some(120)),
        
        Account(id = Some(125), version = 1, accountType=Liability, name="Liability B", code=None, isPlaceholder = true, Some("Liability B Account"), parentAccountId = None),
        Account(id = Some(126), version = 1, accountType=Liability, name="Liability B1", code=None, isPlaceholder = false, Some("Liability B1 Account"), parentAccountId = Some(125)),
        Account(id = Some(127), version = 1, accountType=Liability, name="Liability B2", code=None, isPlaceholder = false, Some("Liability B2 Account"), parentAccountId = Some(125)),

        Account(id = Some(130), version = 1, accountType=Income, name="Income A", code=None, isPlaceholder = true, Some("Income A Account"), parentAccountId = None),
        Account(id = Some(131), version = 1, accountType=Income, name="Income A1", code=None, isPlaceholder = false, Some("Income A1 Account"), parentAccountId = Some(130)),
        Account(id = Some(132), version = 1, accountType=Income, name="Income A2", code=None, isPlaceholder = false, Some("Income A2 Account"), parentAccountId = Some(130)),

        Account(id = Some(135), version = 1, accountType=Income, name="Income B", code=None, isPlaceholder = true, Some("Income B Account"), parentAccountId = None),
        Account(id = Some(136), version = 1, accountType=Income, name="Income B1", code=None, isPlaceholder = false, Some("Income B1 Account"), parentAccountId = Some(135)),
        Account(id = Some(137), version = 1, accountType=Income, name="Income B2", code=None, isPlaceholder = false, Some("Income B2 Account"), parentAccountId = Some(135)),
        Account(id = Some(5042), version = 1, accountType=Income, name="Income B20", code=None, isPlaceholder = false, Some("Income 2B Account"), parentAccountId = Some(137)),
        Account(id = Some(6042), version = 1, accountType=Income, name="Income B21", code=None, isPlaceholder = false, Some("Income B21 Account"), parentAccountId = Some(137)),
        Account(id = Some(7042), version = 1, accountType=Income, name="Income B22", code=None, isPlaceholder = false, Some("Income B22 Account"), parentAccountId = Some(137)),
        Account(id = Some(8042), version = 1, accountType=Income, name="Income B23", code=None, isPlaceholder = false, Some("Income B23 Account"), parentAccountId = Some(137)),

        Account(id = Some(140), version = 1, accountType=Expense, name="Expense A", code=None, isPlaceholder = true, Some("Expense A Account"), parentAccountId = None),
        Account(id = Some(141), version = 1, accountType=Expense, name="Expense A1", code=None, isPlaceholder = false, Some("Expense A1 Account"), parentAccountId = Some(140)),
        Account(id = Some(142), version = 1, accountType=Expense, name="Expense A2", code=None, isPlaceholder = false, Some("Expense A2 Account"), parentAccountId = Some(140)),
        Account(id = Some(4042), version = 1, accountType=Expense, name="Expense A20", code=None, isPlaceholder = false, Some("Expense A2 Account"), parentAccountId = Some(142)),
        Account(id = Some(1042), version = 1, accountType=Expense, name="Expense A21", code=None, isPlaceholder = false, Some("Expense A21 Account"), parentAccountId = Some(142)),
        Account(id = Some(2042), version = 1, accountType=Expense, name="Expense A22", code=None, isPlaceholder = false, Some("Expense A22 Account"), parentAccountId = Some(142)),
        Account(id = Some(3042), version = 1, accountType=Expense, name="Expense A23", code=None, isPlaceholder = false, Some("Expense A23 Account"), parentAccountId = Some(142)),


        Account(id = Some(145), version = 1, accountType=Expense, name="Expense B", code=None, isPlaceholder = true, Some("Expense B Account"), parentAccountId = None),
        Account(id = Some(146), version = 1, accountType=Expense, name="Expense B1", code=None, isPlaceholder = false, Some("Expense B1 Account"), parentAccountId = Some(145)),
        Account(id = Some(147), version = 1, accountType=Expense, name="Expense B2", code=None, isPlaceholder = false, Some("Expense B2 Account"), parentAccountId = Some(145)),

        Account(id = Some(150), version = 1, accountType=Equity, name="Equity A", code=None, isPlaceholder = true, Some("Equity A Account"), parentAccountId = None),
        Account(id = Some(151), version = 1, accountType=Equity, name="Equity A1", code=None, isPlaceholder = false, Some("Equity A1 Account"), parentAccountId = Some(150)),
        Account(id = Some(152), version = 1, accountType=Equity, name="Equity A2", code=None, isPlaceholder = false, Some("Equity A2 Account"), parentAccountId = Some(150)),

        Account(id = Some(155), version = 1, accountType=Equity, name="Equity B", code=None, isPlaceholder = true, Some("Equity B Account"), parentAccountId = None),
        Account(id = Some(156), version = 1, accountType=Equity, name="Equity B1", code=None, isPlaceholder = false, Some("Equity B1 Account"), parentAccountId = Some(155)),
        Account(id = Some(157), version = 1, accountType=Equity, name="Equity B2", code=None, isPlaceholder = false, Some("Equity B2 Account"), parentAccountId = Some(155)),

      )
    )

  override def createAccount(account: Account): Try[Account] = Success(account)

  override def updateAccount(account: Account): Try[Account] = ???

  override def deleteAccount(account: Account): Try[Account] = ???
}
