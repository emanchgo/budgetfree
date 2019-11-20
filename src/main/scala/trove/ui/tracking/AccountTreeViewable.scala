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

package trove.ui.tracking

import scalafx.scene.control.TreeItem
import trove.models.Account
import trove.models.AccountTypes.AccountType


// A marker type used for interaction with SFX.
private[tracking] sealed trait AccountTreeViewable {
  def accountType: AccountType
  def id: Option[Long]
}

// Provides a view on an account type
// This class makes the account type display just the name of the account type in the UI.
private[tracking] class AccountTypeView(val accountType: AccountType) extends AccountTreeViewable {
  override def id: Option[Long] = None
  override def toString: String = accountType.toString
}

// Provides a view on an account
// This class makes the account type display just the name of the account in the UI.
private[tracking] class AccountView(@volatile var account: Account) extends AccountTreeViewable {
  override def toString: String = account.name
  def id: Option[Long] = account.id
  def parentAccountId: Option[Long] = account.parentAccountId
  override def accountType: AccountType = account.accountType
  def update(updatedAccount: Account): Unit = this.account = account
}

// The tree item for the root of the tree in the tree table view
private[tracking] class AccountRootItem extends TreeItem[AccountTreeViewable] {
  expanded = true
}

// The tree item for an account type, which occurs just under the root of the tree in the tree table view.
private[tracking] class AccountTypeItem(val accountTypeView: AccountTypeView) extends TreeItem[AccountTreeViewable](accountTypeView) {
  expanded = true
}

// The tree item for an account
private[tracking] class AccountItem(val accountView: AccountView) extends TreeItem[AccountTreeViewable](accountView) {
  expanded = true
  def update(account: Account): Unit = accountView.update(account)
}
