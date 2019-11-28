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
package trove.cache

import java.util.concurrent.ConcurrentHashMap

import trove.core.Trove
import trove.core.infrastructure.event.{Event, EventListener}

trait CacheKey
case class IndexDef[A](name: String, keyFn: A => CacheKey)

//ejf-fixMe: develop and test.
class ActiveCache[A](override val eventSubscriberGroup: Int, initialize: => Seq[A], indexDefs: Seq[IndexDef[A]]) extends EventListener {
  require(indexDefs.nonEmpty, "Indexes defined in a cache must not be empty")
  require(indexDefs.count(_.name == "Primary") == 1, """The cache must contain exactly one index defined with the name "Primary"""" )
  for(idxDef <- indexDefs) {
    require(indexDefs.count(_.name == idxDef.name) == 1, s"""There can be no duplicate indexes defined in a cache: "${idxDef.name}" occurs more than once! """)
  }


  // Initialize cache with indexes
  private[this] val indexes: Map[IndexDef[A], ConcurrentHashMap[CacheKey, A]] = indexDefs.map { indexDef =>
    indexDef -> new ConcurrentHashMap[CacheKey, A]
  }.toMap

  Trove.eventService.subscribe(this)

  def getAll: Seq[A] = ???
  def getById: A = ???

  override def onEvent: PartialFunction[Event, Unit] = ???
//
//
//  def get(id: Long): Option[Account] = Option(cache.get(id))
//  def getAllAccounts: Seq[Account] = cache.values().asScala.toSeq
//
//  override def onReceive: PartialFunction[Event,Unit] = {
//    case AccountAdded(account) =>
//      cache.put(account.id.get, account)
//    case AccountUpdated(account) =>
//      cache.put(account.id.get, account)
//    // NOTE: If we decide to track account parent-child relationships in the cache, we will need to consume and process AccountParentChanged.
//    case AccountDeleted(id, _) =>
//      cache.remove(id)
//  }
}
