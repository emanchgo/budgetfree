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

package trove.core.accounts

import trove.models.{Account, AccountType}
import trove.services.AccountsService

import scala.util.{Success, Try}

class AccountsServiceImpl extends AccountsService {
  import AccountType._

  override def getAllAccounts: Try[Seq[Account]] =
    Success(
      Seq(
        Account(id = Some(110), accountType=Asset, name="Asset A", isPlaceholder = true, Some("Asset A Account"), parentAccountId = None),
        Account(id = Some(111), accountType=Asset, name="Asset A1", isPlaceholder = false, Some("Asset A1 Account"), parentAccountId = Some(110)),
        Account(id = Some(112), accountType=Asset, name="Asset A2", isPlaceholder = false, Some("Asset A2 Account"), parentAccountId = Some(110)),

        Account(id = Some(115), accountType=Asset, name="Asset B", isPlaceholder = true, Some("Asset B Account"), parentAccountId = None),
        Account(id = Some(116), accountType=Asset, name="Asset B1", isPlaceholder = false, Some("Asset B1 Account"), parentAccountId = Some(115)),
        Account(id = Some(117), accountType=Asset, name="Asset B2", isPlaceholder = false, Some("Asset B2 Account"), parentAccountId = Some(115)),
        
        Account(id = Some(120), accountType=Liability, name="Liability A", isPlaceholder = true, Some("Liability A Account"), parentAccountId = None),
        Account(id = Some(121), accountType=Liability, name="Liability A1", isPlaceholder = false, Some("Liability A1 Account"), parentAccountId = Some(120)),
        Account(id = Some(122), accountType=Liability, name="Liability A2", isPlaceholder = false, Some("Liability A2 Account"), parentAccountId = Some(120)),
        
        Account(id = Some(125), accountType=Liability, name="Liability B", isPlaceholder = true, Some("Liability B Account"), parentAccountId = None),
        Account(id = Some(126), accountType=Liability, name="Liability B1", isPlaceholder = false, Some("Liability B1 Account"), parentAccountId = Some(125)),
        Account(id = Some(127), accountType=Liability, name="Liability B2", isPlaceholder = false, Some("Liability B2 Account"), parentAccountId = Some(125)),

        Account(id = Some(130), accountType=Income, name="Income A", isPlaceholder = true, Some("Income A Account"), parentAccountId = None),
        Account(id = Some(131), accountType=Income, name="Income A1", isPlaceholder = false, Some("Income A1 Account"), parentAccountId = Some(130)),
        Account(id = Some(132), accountType=Income, name="Income A2", isPlaceholder = false, Some("Income A2 Account"), parentAccountId = Some(130)),

        Account(id = Some(135), accountType=Income, name="Income B", isPlaceholder = true, Some("Income B Account"), parentAccountId = None),
        Account(id = Some(136), accountType=Income, name="Income B1", isPlaceholder = false, Some("Income B1 Account"), parentAccountId = Some(135)),
        Account(id = Some(137), accountType=Income, name="Income B2", isPlaceholder = false, Some("Income B2 Account"), parentAccountId = Some(135)),
        Account(id = Some(5042), accountType=Income, name="Income B20", isPlaceholder = false, Some("Income 2B Account"), parentAccountId = Some(137)),
        Account(id = Some(6042), accountType=Income, name="Income B21", isPlaceholder = false, Some("Income B21 Account"), parentAccountId = Some(137)),
        Account(id = Some(7042), accountType=Income, name="Income B22", isPlaceholder = false, Some("Income B22 Account"), parentAccountId = Some(137)),
        Account(id = Some(8042), accountType=Income, name="Income B23", isPlaceholder = false, Some("Income B23 Account"), parentAccountId = Some(137)),

        Account(id = Some(140), accountType=Expense, name="Expense A", isPlaceholder = true, Some("Expense A Account"), parentAccountId = None),
        Account(id = Some(141), accountType=Expense, name="Expense A1", isPlaceholder = false, Some("Expense A1 Account"), parentAccountId = Some(140)),
        Account(id = Some(142), accountType=Expense, name="Expense A2", isPlaceholder = false, Some("Expense A2 Account"), parentAccountId = Some(140)),
        Account(id = Some(4042), accountType=Expense, name="Expense A20", isPlaceholder = false, Some("Expense A2 Account"), parentAccountId = Some(142)),
        Account(id = Some(1042), accountType=Expense, name="Expense A21", isPlaceholder = false, Some("Expense A21 Account"), parentAccountId = Some(142)),
        Account(id = Some(2042), accountType=Expense, name="Expense A22", isPlaceholder = false, Some("Expense A22 Account"), parentAccountId = Some(142)),
        Account(id = Some(3042), accountType=Expense, name="Expense A23", isPlaceholder = false, Some("Expense A23 Account"), parentAccountId = Some(142)),


        Account(id = Some(145), accountType=Expense, name="Expense B", isPlaceholder = true, Some("Expense B Account"), parentAccountId = None),
        Account(id = Some(146), accountType=Expense, name="Expense B1", isPlaceholder = false, Some("Expense B1 Account"), parentAccountId = Some(145)),
        Account(id = Some(147), accountType=Expense, name="Expense B2", isPlaceholder = false, Some("Expense B2 Account"), parentAccountId = Some(145)),

        Account(id = Some(150), accountType=Equity, name="Equity A", isPlaceholder = true, Some("Equity A Account"), parentAccountId = None),
        Account(id = Some(151), accountType=Equity, name="Equity A1", isPlaceholder = false, Some("Equity A1 Account"), parentAccountId = Some(150)),
        Account(id = Some(152), accountType=Equity, name="Equity A2", isPlaceholder = false, Some("Equity A2 Account"), parentAccountId = Some(150)),

        Account(id = Some(155), accountType=Equity, name="Equity B", isPlaceholder = true, Some("Equity B Account"), parentAccountId = None),
        Account(id = Some(156), accountType=Equity, name="Equity B1", isPlaceholder = false, Some("Equity B1 Account"), parentAccountId = Some(155)),
        Account(id = Some(157), accountType=Equity, name="Equity B2", isPlaceholder = false, Some("Equity B2 Account"), parentAccountId = Some(155)),

      )
    )

  override def createAccount(account: Account): Try[Unit] = ???

  override def updateAccount(account: Account): Try[Unit] = ???

  override def deleteAccount(account: Account): Try[Unit] = ???
}
