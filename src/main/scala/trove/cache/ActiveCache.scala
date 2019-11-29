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
import java.util.function.BiFunction

import grizzled.slf4j.Logging
import trove.cache.ActiveCache.CacheOperation
import trove.core.Trove
import trove.core.infrastructure.event.{Event, EventListener}

import scala.collection.JavaConverters._

case class IndexDef[+K, A](keyFn: A => K)

object ActiveCache {

  sealed trait CacheOperation

  def apply[A](
    cacheName: String,
    eventSubscriberGroup: Int,
    indexDefs: Seq[IndexDef[_, A]],
    determineCacheOperation: Event => CacheOperation,
    sequenceNumber: A => Long,
    initialize: => Seq[A])  : ActiveCache[A] = new ActiveCache[A](cacheName, eventSubscriberGroup, indexDefs, determineCacheOperation, sequenceNumber, initialize)

  private[ActiveCache] implicit class BifunctionConverter[A](fn: (Any, A) => Option[A]) {
    private[this] var nullA : A = _ // must be a var to get null to work for type A
    def asJava: BiFunction[Any, A, A] = (key: Any, element: A) => fn(key, element).getOrElse(nullA)
  }
}

class ActiveCache[A] private[ActiveCache] (
  cacheName: String,
  override val eventSubscriberGroup: Int,
  indexDefs: Seq[IndexDef[_, A]],
  determineCacheOperation: Event => CacheOperation,
  sequenceNumber: A => Long,
  initialize: => Seq[A])
  extends EventListener with Logging {

  import ActiveCache._

  object CacheOperations {
    case class Add(element: A) extends CacheOperation
    case class Update(element: A) extends CacheOperation
    case class Delete(indexDef: IndexDef[_, A], key: Any, sequenceNumber: Long) extends CacheOperation
    case object NoOp extends CacheOperation
  }

  require(indexDefs.nonEmpty, "Indexes defined in a cache must not be empty")

  private[this] val indexes: ConcurrentHashMap[IndexDef[_, A], ConcurrentHashMap[Any, A]] = new ConcurrentHashMap[IndexDef[_, A], ConcurrentHashMap[Any, A]]
  for {
    indexDef <- indexDefs
  } yield {
    indexes.put(indexDef, new ConcurrentHashMap[Any, A])
  }

  Trove.eventService.subscribe(listener = this)

  private[this] val updateFn: A => (Any, A) => Option[A] = newElement => (_, element) =>  {
    val oldSequenceNumber = sequenceNumber(element)
    val newSequenceNumber = sequenceNumber(newElement)
    if(newSequenceNumber > oldSequenceNumber){
      Some(newElement)
    } else {
      Some(element)
    }
  }

  private[this] val deleteFn: Long => (Any, A) => Option[A] = newSequenceNumber => (_, element) => {
    val oldSequenceNumber = sequenceNumber(element)
    if(newSequenceNumber > oldSequenceNumber) {
      None
    } else {
      Some(element)
    }
  }

  // Initialize.
  // This cache initializes itself at instantiation time and automatically updates itself when things change.
  for {
    element <- initialize
  } {
    // Put if absent: because if (for some reason) another thread published an update, it should take precedence.
    // When we update the cache due to the receipt of an event, we'll check versions.
    updateIndexes(element, updateFn(element))
  }

  def getAll[K](indexDef: IndexDef[K, A]): Seq[A] =
    getIndex(indexDef).map(_.values().asScala.toSeq).getOrElse(Seq.empty)

  def get[K](indexDef: IndexDef[K, A], key: K): Option[A] = getIndex(indexDef).map(_.get(key))

  private[this] def getIndex[K](indexDef: IndexDef[K, A]): Option[ConcurrentHashMap[Any, A]] = {
    val maybeIndex = Option(indexes.get(indexDef))
    if(maybeIndex.isEmpty) {
      logger.error(msg =s"Unknown index selected in active cache (name=$cacheName)")
    }
    maybeIndex
  }

  private[this] def updateIndexes(element: A, fn: (Any, A) => Option[A]): Unit =
    for {
      indexDef <- indexDefs
      key = indexDef.keyFn(element)
      index = indexes.get(indexDef)
    } {
      index.compute(key, fn.asJava)
    }

  import CacheOperations._

  override def onEvent: PartialFunction[Event, Unit] = {
    case event => determineCacheOperation(event) match {
      case Add(element) => updateIndexes(element, updateFn(element))
      case Update(element) => updateIndexes(element, updateFn(element))
      case Delete(idxDef, key, sequenceNum) =>
        get(idxDef, key).foreach { element =>
          updateIndexes(element, deleteFn(sequenceNum))
        }
      case NoOp =>
      case _ =>
        // invalid, you got some other cache's CacheOperation here!
        logger.error(msg = s"Error in cache $cacheName: Invalid CacheOperation object - perhaps the cache operations got mixed up!")
    }
  }
}
