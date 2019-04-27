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

import scalafx.beans.property.ReadOnlyObjectWrapper
import scalafx.scene.control.{TreeItem, TreeTableColumn, TreeTableView}
import trove.core.infrastructure.event
import trove.events._
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
private[tracking] class AccountTypeItem(val accountTypeView: AccountTypeView) extends TreeItem[AccountTreeViewable](accountTypeView) {
  expanded = true
}

// The tree item for an account
private[tracking] class AccountItem(val accountView: AccountView) extends TreeItem[AccountTreeViewable](accountView) {
  expanded = true
  def update(account: Account): Unit = accountView.update(account)
}

// Provides a view on an account type
// This class makes the account type display just the name of the account type in the UI.
private[tracking] class AccountTypeView(val accountType: AccountType) extends AccountTreeViewable {
  override def id: Option[Int] = None
  override def toString: String = accountType.toString
}

// Provides a view on an account
// This class makes the account type display just the name of the account in the UI.
private[tracking] class AccountView(@volatile var account: Account) extends AccountTreeViewable {
  override def toString: String = account.name
  def id: Option[Int] = account.id
  def parentAccountId: Option[Int] = account.parentAccountId
  override def accountType: AccountType = account.accountType
  def update(updatedAccount: Account): Unit = this.account = account
}

// The accounts view. We use a tree table view to get the account name column, although we do
// disable user sorting of the data.
private[ui] class AccountsView(accountsService: AccountsService) extends TreeTableView[AccountTreeViewable] with UIEventListener {

  import trove.ui._

  @volatile private[this] var accountTypeItemsByAccountType: Map[AccountType, AccountTypeItem] = Map.empty
  @volatile private[this] var accountItemsByAccountId: Map[Int, AccountItem] = Map.empty

  root = new AccountRootItem {
    children = promptUserWithError(accountTrees).getOrElse(Seq.empty)
  }
  showRoot = false

  private[this] val accountNameColumn = new TreeTableColumn[AccountTreeViewable, AccountTreeViewable]("Account Name") {
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
    // Recursive method to expand a tree
    def expandTree(account: Account, accountsByParentId: mutable.MultiMap[Option[Int], Account]): AccountItem = {
      val accountItem = new AccountItem(new AccountView(account)) {
        children = accountsByParentId.get(account.id).map { children =>
          children.map(child => expandTree(child, accountsByParentId)).toSeq.sortBy(_.accountView.toString)
        }.getOrElse(Seq.empty)
      }
      accountItemsByAccountId += account.id.get -> accountItem
      accountItem
    }

    for {
      accounts <- accountsService.getAllAccounts
    }
    yield {

      val accountsByParentId = new mutable.HashMap[Option[Int], mutable.Set[Account]] with mutable.MultiMap[Option[Int], Account]
      accounts.foreach(a => accountsByParentId.addBinding(a.parentAccountId, a))

      val topLevelAccounts = accountsByParentId(None).toSeq
      val accountTrees = for {
        topLevelAccount <- topLevelAccounts
      } yield {
        expandTree(topLevelAccount, accountsByParentId)
      }

      accountTrees.groupBy { accountItem =>
        accountItem.accountView.accountType
      }.map { case (accountType, treeItems) =>
        val accountTypeItem = new AccountTypeItem(new AccountTypeView(accountType)) {
          children = treeItems.sortBy(_.accountView.toString)
        }
        accountTypeItemsByAccountType += accountType -> accountTypeItem
        accountTypeItem
      }.toSeq.sortBy(_.accountTypeView.accountType)
    }
  }

  override def onReceive: PartialFunction[event.Event, Unit] = {
    case AccountAdded(account) =>
      addAccount(account)
    case AccountUpdated(account) =>
      updateAccount(account)
    case AccountParentChanged(id, oldParent, newParent) =>
      updateParent(id, oldParent, newParent)
    case AccountDeleted(id, parent) =>
      deleteAccount(id, parent)
    case ProjectChanged(_) =>
      unsubscribe()
  }

  private[this] def addAccount(account: Account): Unit = {
    val accountItem = new AccountItem(new AccountView(account))

    account.parentAccountId match {
      case Some(parentAccountId) =>
        accountItemsByAccountId(parentAccountId).children += accountItem
      case None =>
        accountTypeItemsByAccountType(account.accountType).children += accountItem
    }
    accountItemsByAccountId += account.id.get -> accountItem
  }

  private[this] def updateAccount(account: Account): Unit =
    accountItemsByAccountId(account.id.get).update(account)

  private[this] def updateParent(accountId: Int, oldParent: Either[AccountType, Int], newParent: Either[AccountType, Int]): Unit = {
    val accountItem = accountItemsByAccountId(accountId)
    oldParent match {
      case Left(accountType) =>
        accountTypeItemsByAccountType(accountType).children -= accountItem
      case Right(oldParentId) =>
        accountItemsByAccountId(oldParentId).children -= accountItem
    }
    newParent match {
      case Left(accountType) =>
        accountTypeItemsByAccountType(accountType).children += accountItem
      case Right(newParentId) =>
        accountItemsByAccountId(newParentId).children += accountItem
    }
  }

  private[this] def deleteAccount(accountId: Int, parent: Either[AccountType, Int]): Unit = {
    val accountItem = accountItemsByAccountId(accountId)
    accountItem.accountView.account.parentAccountId match {
      case Some(id) =>
        accountItemsByAccountId(id).children -= accountItem
      case None =>
        accountTypeItemsByAccountType(accountItem.accountView.account.accountType).children -= accountItem
    }
    accountItemsByAccountId -= accountId
  }
}
