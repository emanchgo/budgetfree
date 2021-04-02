/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package trove.util.collections

import scala.collection.mutable

// ejf-fixMe: should probably add tests and verify attribution
trait MultiMap[K, V] extends mutable.Map[K, mutable.Set[V]] {
  /** Creates a new set.
    *
    *  Classes that use this trait as a mixin can override this method
    *  to have the desired implementation of sets assigned to new keys.
    *  By default this is `HashSet`.
    *
    *  @return An empty set of values of type `V`.
    */
  protected def makeSet: mutable.Set[V] = new mutable.HashSet[V]

  /** Assigns the specified `value` to a specified `key`.  If the key
    *  already has a binding to equal to `value`, nothing is changed;
    *  otherwise a new binding is added for that `key`.
    *
    *  @param key    The key to which to bind the new value.
    *  @param value  The value to bind to the key.
    *  @return       A reference to this multimap.
    */
  def addBinding(key: K, value: V): this.type = {
    get(key) match {
      case None =>
        val set = makeSet
        set += value
        this(key) = set
      case Some(set) =>
        set += value
    }
    this
  }

  /** Removes the binding of `value` to `key` if it exists, otherwise this
    *  operation doesn't have any effect.
    *
    *  If this was the last value assigned to the specified key, the
    *  set assigned to that key will be removed as well.
    *
    *  @param key     The key of the binding.
    *  @param value   The value to remove.
    *  @return        A reference to this multimap.
    */
  def removeBinding(key: K, value: V): this.type = {
    get(key) match {
      case None =>
      case Some(set) =>
        set -= value
        if (set.isEmpty) this -= key
    }
    this
  }

  /** Checks if there exists a binding to `key` such that it satisfies the predicate `p`.
    *
    *  @param key   The key for which the predicate is checked.
    *  @param p     The predicate which a value assigned to the key must satisfy.
    *  @return      A boolean if such a binding exists
    */
  def entryExists(key: K, p: V => Boolean): Boolean = get(key) match {
    case None => false
    case Some(set) => set exists p
  }
}
