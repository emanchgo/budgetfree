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
import scalafx.scene.control.{TreeTableColumn, TreeTableView}
import trove.core.infrastructure.event
import trove.events._
import trove.models.Account
import trove.models.AccountTypes.AccountType
import trove.ui.UIEventListener

import scala.collection.mutable

// The accounts view. We use a tree table view to get the account name column, although we do
// disable user sorting of the data.
private[tracking] class MainAccountsView(accountsCache: AccountsCache) extends TreeTableView[AccountTreeViewable] with UIEventListener {

  @volatile private[this] var accountTypeItemsByAccountType: Map[AccountType, AccountTypeItem] = Map.empty
  @volatile private[this] var accountItemsByAccountId: Map[Long, AccountItem] = Map.empty

  root = new AccountRootItem {
    children = accountTrees
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
      new ReadOnlyObjectWrapper[AccountTreeViewable](MainAccountsView.this, cdf.value.value().toString, cdf.value.value())
    }
  }
  columns += accountNameColumn

  // Fits the columns into the widget.
  columnResizePolicy = TreeTableView.ConstrainedResizePolicy

  sortOrder += accountNameColumn

  // Builds the account trees, with each element returned representing the root of the account hierarchy tree for that account type.
  private[this] def accountTrees: Seq[AccountTypeItem] = {
    // Recursive method to expand a tree
    def expandTree(account: Account, accountsByParentId: mutable.MultiMap[Option[Long], Account]): AccountItem = {
      val accountItem = new AccountItem(new AccountView(account)) {
        children = accountsByParentId.get(account.id).map { children =>
          children.map(child => expandTree(child, accountsByParentId)).toSeq.sortBy(_.accountView.toString)
        }.getOrElse(Seq.empty)
      }
      accountItemsByAccountId += account.id.get -> accountItem
      accountItem
    }

    val accountsByParentId = new mutable.HashMap[Option[Long], mutable.Set[Account]] with mutable.MultiMap[Option[Long], Account]
    accountsCache.getAllAccounts.foreach(a => accountsByParentId.addBinding(a.parentAccountId, a))

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
