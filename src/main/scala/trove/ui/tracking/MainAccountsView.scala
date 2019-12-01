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

import trove.core.infrastructure.event
import trove.events._
import trove.models.{Account, AccountParent}
import trove.services.AccountsService
import trove.ui._

// The accounts view. We use a tree table view to get the account name column, although we do
// disable user sorting of the data.
private[tracking] class MainAccountsView(override val eventSubscriberGroup: Int, accountsService: AccountsService) extends AccountTreeTableView(
  promptUserWithError(accountsService.getAllAccounts).toOption.getOrElse(Seq.empty)
  ) with UIEventListener {

  override def onReceive: PartialFunction[event.Event, Unit] = {
    case ItemAdded(_, account: Account) =>
      addAccount(account)
    case ItemUpdated(_, account: Account) =>
      updateAccount(account)
    case AccountParentChanged(id, oldParent, newParent) =>
      updateParent(id, oldParent, newParent)
    case ItemDeleted(_, account: Account) =>
      deleteAccount(account.id.get, account.parentAccountId.map[AccountParent](Right(_)).getOrElse(Left(account.accountType)))
    case ProjectChanged(_) =>
      unsubscribe() // ejf-fixMe: Unsubscribe group! Might be a good way to quickly remove subscriptions for all things related to a project.
  }

  private[this] def addAccount(account: Account): Unit = {
    val accountItem = new AccountItem(new AccountView(account))

    account.parentAccountId match {
      case Some(parentAccountId) =>
        accountItemsByAccountId.get(parentAccountId).children += accountItem
      case None =>
        accountTypeItemsByAccountType.get(account.accountType).children += accountItem
    }
    accountItemsByAccountId.put(account.id.get, accountItem)
  }

  private[this] def updateAccount(account: Account): Unit =
    accountItemsByAccountId.get(account.id.get).update(account)

  private[this] def updateParent(accountId: Int, oldParent: AccountParent, newParent: AccountParent): Unit = {
    val accountItem = accountItemsByAccountId.get(accountId)
    oldParent match {
      case Left(accountType) =>
        accountTypeItemsByAccountType.get(accountType).children -= accountItem
      case Right(oldParentId) =>
        accountItemsByAccountId.get(oldParentId).children -= accountItem
    }
    newParent match {
      case Left(accountType) =>
        accountTypeItemsByAccountType.get(accountType).children += accountItem
      case Right(newParentId) =>
        accountItemsByAccountId.get(newParentId).children += accountItem
    }
  }

  private[this] def deleteAccount(accountId: Long, parent: AccountParent): Unit = {
    val accountItem = accountItemsByAccountId.get(accountId)
    accountItem.accountView.account.parentAccountId match {
      case Some(id) =>
        accountItemsByAccountId.get(id).children -= accountItem
      case None =>
        accountTypeItemsByAccountType.get(accountItem.accountView.account.accountType).children -= accountItem
    }
    accountItemsByAccountId.remove(accountId)
  }
}
