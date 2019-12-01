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

import java.util.concurrent.ConcurrentHashMap

import scalafx.beans.property.ReadOnlyObjectWrapper
import scalafx.scene.control.{SelectionMode, TreeTableColumn, TreeTableView}
import trove.models.Account
import trove.models.AccountTypes.AccountType

import scala.collection.mutable

private[tracking] abstract class AccountTreeTableView(accounts: Seq[Account]) extends TreeTableView[AccountTreeViewable] {

  // ejf-fixMe: analysis: remove @volatile in favor of ConcurrentHashMap everywhere
  protected val accountTypeItemsByAccountType: ConcurrentHashMap[AccountType, AccountTypeItem] = new ConcurrentHashMap
  protected val accountItemsByAccountId: ConcurrentHashMap[Long, AccountItem] = new ConcurrentHashMap

  selectionModel().setSelectionMode(SelectionMode.Single)

  root = new AccountRootItem {
    children = accountTrees
  }
  showRoot = false

  private[this] val accountColumn = new TreeTableColumn[AccountTreeViewable, AccountTreeViewable]("Account Name") {
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
      new ReadOnlyObjectWrapper[AccountTreeViewable](this, cdf.value.value().toString, cdf.value.value())
    }
  }
  columns += accountColumn

  // Fits the columns into the widget.
  columnResizePolicy = TreeTableView.ConstrainedResizePolicy

  sortOrder += accountColumn

  // Builds the account trees, with each element returned representing the root of the account hierarchy tree for that account type.
  private[this] def accountTrees: Seq[AccountTypeItem] = {
    // Recursive method to expand a tree
    def expandTree(account: Account, accountsByParentId: mutable.MultiMap[Option[Long], Account]): AccountItem = {
      val accountItem = new AccountItem(new AccountView(account)) {
        children = accountsByParentId.get(account.id).map { children =>
          children.map(child => expandTree(child, accountsByParentId)).toSeq.sortBy(_.accountView.toString)
        }.getOrElse(Seq.empty)
      }
      accountItemsByAccountId.put(account.id.get, accountItem)
      accountItem
    }

    val accountsByParentId = new mutable.HashMap[Option[Long], mutable.Set[Account]] with mutable.MultiMap[Option[Long], Account]
    accounts.foreach(a => accountsByParentId.addBinding(a.parentAccountId, a))

    val topLevelAccounts = accountsByParentId.get(None).map(_.toSeq).getOrElse(Seq.empty)
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
      accountTypeItemsByAccountType.put(accountType, accountTypeItem)
      accountTypeItem
    }.toSeq.sortBy(_.accountTypeView.accountType)
  }
}
