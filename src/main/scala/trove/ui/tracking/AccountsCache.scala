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

import trove.core.Project
import trove.core.infrastructure.event.Event
import trove.events.{AccountAdded, AccountDeleted, AccountUpdated}
import trove.models.Account
import trove.ui.UIEventListener

import scala.collection.JavaConverters._

class AccountsCache(project: Project) extends UIEventListener {

  import trove.ui._

  private[this] val cache: ConcurrentHashMap[Long, Account] = new ConcurrentHashMap
  promptUserWithError(project.accountsService.getAllAccounts).foreach { accounts =>
    for {
      account <- accounts
    } {
      cache.put(account.id.get, account)
    }
  }

  def get(id: Long): Option[Account] = Option(cache.get(id))
  def getAllAccounts: Seq[Account] = cache.values().asScala.toSeq

  override def onReceive: PartialFunction[Event,Unit] = {
    case AccountAdded(account) =>
      cache.put(account.id.get, account)
    case AccountUpdated(account) =>
      cache.put(account.id.get, account)
    // NOTE: If we decide to track account parent-child relationships in the cache, we will need to consume and process AccountParentChanged.
    case AccountDeleted(id, _) =>
      cache.remove(id)
  }
}
