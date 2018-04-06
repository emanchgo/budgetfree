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

package trove.ui

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar
import trove.core.infrastructure.event.{Event, EventService}

class UIEventListenerSpec extends FlatSpec with MockitoSugar with Matchers {

  case class EventA(id: Int) extends Event
  case class EventB(id: Int) extends Event

  class Fixture {

    @volatile var currentlySubscribed = false
    @volatile var events: List[Event] = List.empty

    val listener: UIEventListener = new UIEventListener {
      override def onReceive: PartialFunction[Event, Unit] = {
        case ev@EventA(_) =>
          events = ev :: events
      }

      override def _subscribed(): Unit = {
        currentlySubscribed = true
      }

      override def _unsubscribed(): Unit = {
        currentlySubscribed = false
      }

      protected override def _runLater(op: => Unit): Unit = op
    }
  }

  it should "automatically subscribe and propagate events" in new Fixture {
    currentlySubscribed shouldBe true
    EventService.publish(EventA(1))
    EventService.publish(EventA(2))
    eventually {
      events shouldBe List(EventA(2), EventA(1))
    }
  }

  it should "only forward events for which the onReceive partial function has a mapping" in new Fixture {
    EventService.publish(EventA(1))
    EventService.publish(EventB(2))
    Thread.sleep(1000)
    eventually {
      events shouldBe List(EventA(1))
    }
  }

  it should "unsubscribe when requested" in new Fixture {
    listener.unsubscribe()
    currentlySubscribed shouldBe false
    Thread.sleep(1000)
    EventService.publish(EventA(1))
    events shouldBe empty
  }
}
