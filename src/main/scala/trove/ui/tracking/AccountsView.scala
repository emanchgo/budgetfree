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

package trove.ui.tracking

import scalafx.beans.property.ReadOnlyObjectWrapper
import scalafx.scene.control.{TreeItem, TreeTableColumn, TreeTableView}
import trove.core.infrastructure.event
import trove.events.{AccountAdded, AccountDeleted, AccountParentChanged}
import trove.models.Account
import trove.models.AccountType.AccountType
import trove.services.AccountsService
import trove.ui.UIEventListener

import scala.collection.mutable
import scala.util.Try

// A marker type used for interaction with SFX.
private[tracking] sealed trait AccountTreeViewable {
  def accountType: AccountType
  def id: Option[Int]
}

// The tree item for the root of the tree in the tree table view
private[tracking] class AccountRootItem extends TreeItem[AccountTreeViewable] {
  expanded = true
}

// The tree item for an account type, which occurs just under the root of the tree in the tree table view.
private[tracking] class AccountTypeItem(val accountTypeView: AccountTypeView) extends TreeItem[AccountTreeViewable](accountTypeView) with UIEventListener {
  expanded = true

  def accountType: AccountType = accountTypeView.accountType

  override def onReceive: PartialFunction[event.Event, Unit] = {
    case AccountAdded(account) if account.parentAccountId.isEmpty && account.accountType == accountType =>
      // New top-level account
      ???
    case AccountDeleted(id, Right(parentAccountType)) if parentAccountType == accountType =>
      // Delete a child account, because it is being deleted from Trove
      ???
    case AccountParentChanged(account, Right(oldParentAccountType)) if oldParentAccountType == accountType =>
      // Delete a child account, because its parent is changing to something else
      ???
    case AccountParentChanged(account, _) if account.parentAccountId.isEmpty && account.accountType == accountType =>
      // Add a child account to this type
/*
  case class AccountAdded(account: Account) extends Event
  case class AccountUpdated(account: Account) extends Event
  case class AccountDeleted(id: Int, parent: Either[Int, AccountType]) extends Event
  case class AccountParentChanged(account: Account, oldParent: Either[Int, AccountType]) extends Event
 */

  }

}

// The tree item for an account
private[tracking] class AccountItem(var accountView: AccountView) extends TreeItem[AccountTreeViewable](accountView) with UIEventListener {
  expanded = true

  override def onReceive: PartialFunction[event.Event, Unit] = ???

}

// Provides a view on an account type
// This class makes the account type display just the name of the type in the UI.
private[tracking] class AccountTypeView(val accountType: AccountType) extends AccountTreeViewable {
  override def id: Option[Int] = None
  override def toString: String = accountType.toString
}

// Provides a view on an account
// This class makes the account type display just the name of the account in the UI.
private[tracking] class AccountView(account: Account) extends AccountTreeViewable {
  override def toString: String = account.name
  def id: Option[Int] = account.id
  def parentAccountId: Option[Int] = account.parentAccountId
  override def accountType: AccountType = account.accountType
}

// The accounts view. We use a tree table view to get the account name column, although we do
// disable user sorting of the data.
private[ui] class AccountsView(accountsService: AccountsService) extends TreeTableView[AccountTreeViewable] {
  import trove.ui._

  root = new AccountRootItem {
    children = dialogOnError(accountTrees).getOrElse(Seq.empty)
  }
  showRoot = false

  private[this] val accountNameColumn = new TreeTableColumn[AccountTreeViewable,AccountTreeViewable]("Account Name") {
    // We want the ordering to be first by account type, and then by account name
    comparator = (a: AccountTreeViewable, b: AccountTreeViewable) => {
      val accountTypeCompare = a.accountType compare b.accountType
      if (accountTypeCompare == 0) {
        a.toString compare b.toString
      }
      else {
        accountTypeCompare
      }
    }
    sortable = false
    cellValueFactory = { cdf =>
      new ReadOnlyObjectWrapper[AccountTreeViewable](AccountsView.this, cdf.value.value().toString, cdf.value.value())
    }
  }
  columns += accountNameColumn

  // Fits the columns into the widget.
  columnResizePolicy = TreeTableView.ConstrainedResizePolicy

  sortOrder += accountNameColumn

  // Builds the account trees, with each element returned representing the root of the account hierarchy tree for that account type.
  private[this] def accountTrees: Try[Seq[AccountTypeItem]] = {
    for {
      accounts <- accountsService.getAllAccounts
    }
    yield {

      val accountsByParentId = new mutable.HashMap[Option[Int], mutable.Set[Account]] with mutable.MultiMap[Option[Int], Account]
      accounts.foreach(a => accountsByParentId.addBinding(a.parentAccountId, a))

      val roots = accountsByParentId(None).toSeq
      val accountTrees = for {
        root <- roots
      } yield {
        expandTree(root, accountsByParentId)
      }

      accountTrees.groupBy { accountItem =>
       accountItem.accountView.accountType
      }.map { case (atv, treeItems) =>
        new AccountTypeItem(new AccountTypeView(atv)) {
          children = treeItems.sortBy(_.accountView.toString)
        }
      }.toSeq.sortBy(_.accountTypeView.accountType)
    }
  }

  // Recursive method to expand a tree
  def expandTree(account: Account, accountsByParentId: mutable.MultiMap[Option[Int], Account]): AccountItem = {
    new AccountItem(new AccountView(account)) {
      children = accountsByParentId.get(account.id).map { children =>
        children.map(child => expandTree(child, accountsByParentId)).toSeq.sortBy(_.accountView.toString)
      }.getOrElse(Seq.empty)
    }
  }

}

