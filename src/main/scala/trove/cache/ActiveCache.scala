/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2021 Eric John Fredericks.
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
import java.util.function.BiFunction

import grizzled.slf4j.Logging
import trove.core.Trove
import trove.core.infrastructure.event.{Event, EventListener}
import trove.events.{ItemAdded, ItemDeleted, ItemUpdated}

import scala.collection.JavaConverters._

// This cache initializes itself at instantiation time and automatically updates itself when elements change.
object ActiveCache {

  def apply[A: Manifest](
    cacheName: String,
    eventSubscriberGroup: Int,
    keyFn: A => Long,
    versionFn: A => Long,
    initialize: => Seq[A])  : ActiveCache[A] = new ActiveCache[A](cacheName, eventSubscriberGroup, keyFn, versionFn, initialize)

  private[ActiveCache] implicit class BifunctionConverter[A](fn: (Long, A) => Option[A]) {
    private[this] var nullA : A = _ // must be a var to get null to work for type A
    def asJava: BiFunction[Long, A, A] = (key: Long, element: A) => fn(key, element).getOrElse(nullA)
  }
}

class ActiveCache[A: Manifest] private[ActiveCache] (
  cacheName: String,
  override val eventSubscriberGroup: Int,
  keyFn: A => Long,
  version: A => Long,
  initialize: => Seq[A])
  extends EventListener with Logging {

  import ActiveCache._

  private[this] val data = new ConcurrentHashMap[Long, A]

  Trove.eventService.subscribe(listener = this)

  private[this] val updateFn: (A, Boolean) => (Long, A) => Option[A] = (newElement, delete) => (_, element) => {
    val prevVer = version(element)
    val newVer = version(newElement)
    if(newVer > prevVer){
      if(delete) None else Some(newElement)
    } else {
      // Do not overwrite later version!
      Some(element)
    }
  }

  // Initialize.
  for {
    element <- initialize
  } {
    updateFn(element, false)
  }

  def getAll: Seq[A] = Seq(data.values().asScala.toSeq: _*)

  def get(key: Long): Option[A] = Option(data.get(Long))

  override def onEvent: PartialFunction[Event, Unit] = {
    case ItemAdded(id, item: A) =>
      data.compute(id, updateFn(item, false).asJava)
    case ItemUpdated(id, item: A) =>
      data.compute(id, updateFn(item, false).asJava)
    case ItemDeleted(id, item: A) =>
      data.compute(id, updateFn(item, true).asJava)
  }
}
