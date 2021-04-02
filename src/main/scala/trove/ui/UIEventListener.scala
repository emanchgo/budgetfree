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

package trove.ui

import scalafx.application.Platform
import trove.core.Trove
import trove.core.infrastructure.event.{Event, EventListener}

import scala.util.Try

// Wrapper to ensure we update UI on event dispatch thread.
// NOTE that unsubscribe MUST be called when a listener is de-allocated and garbage collection is intended.
private[ui] trait UIEventListener extends EventListener {

  def reportError[A](code: => Try[A]): Try[A] = promptUserWithError(code)

  protected def _runLater(op: => Unit): Unit = Platform.runLater(op)

  final override def onEvent: PartialFunction[Event,Unit] = {
    case e if onReceive.isDefinedAt(e) => reportError(Try(_runLater(onReceive(e))))
  }

  def onReceive: PartialFunction[Event,Unit]

  Trove.eventService.subscribe(this)

  def unsubscribe(): Unit = Trove.eventService.unsubscribe(this)
}
