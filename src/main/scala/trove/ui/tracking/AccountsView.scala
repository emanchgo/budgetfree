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

import scalafx.scene.control.{TreeItem, TreeView}
import trove.models.Account
import trove.models.AccountType.AccountType
import trove.services.AccountsService

import scala.collection.mutable
import scala.util.Try

private[tracking] sealed trait AccountTreeViewable {
  def accountType: AccountType
}

private[tracking] class AccountTypeView(val accountType: AccountType) extends AccountTreeViewable {
  override def toString: String = accountType.toString
}

private[tracking] class AccountView(account: Account) extends AccountTreeViewable {
  override def toString: String = account.name
  def id: Option[Int] = account.id
  def parentAccountId: Option[Int] = account.parentAccountId
  override def accountType: AccountType = account.accountType
}

private[tracking] class AccountRootItem() extends TreeItem[AccountTreeViewable] {
  expanded = true
}

private[tracking] case class AccountItem(accountView: AccountView) extends TreeItem[AccountTreeViewable](accountView)

private[tracking] case class AccountTypeItem(accountTypeView: AccountTypeView) extends TreeItem[AccountTreeViewable](accountTypeView)

private[ui] class AccountsView(accountsService: AccountsService) extends TreeView[AccountTreeViewable] {
  import trove.ui._

  root = new AccountRootItem {
    children = dialogOnError(accountTrees).getOrElse(Seq.empty)
  }
  showRoot = false

  private[tracking] def accountTrees: Try[Seq[AccountTypeItem]] = {
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

  def expandTree(node: Account, accountsByParentId: mutable.MultiMap[Option[Int], Account]): AccountItem = {
    new AccountItem(new AccountView(node)) {
      children = accountsByParentId.get(node.id).map { children =>
        children.map(child => expandTree(child, accountsByParentId)).toSeq.sortBy(_.accountView.toString)
      }.getOrElse(Seq.empty)
    }
  }
}

