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

package trove.core.infrastructure.event

import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class EventServiceSpec extends FlatSpec with MockitoSugar with Matchers {

  case class TestEvent(id: Int) extends Event

  class Fixture {

    @volatile var currentlySubscribed = false
    @volatile var ids: List[Int] = List.empty

    class Listener extends EventListener {
      def onEvent: PartialFunction[Event, Unit] = {
        case TestEvent(id) =>
          ids = id :: ids
      }

      override def _subscribed(): Unit = currentlySubscribed = true

      override def _unsubscribed(): Unit = currentlySubscribed = false
    }

    val listener: EventListener = new Listener
  }

  "subscribe and publish" should "work correctly" in new Fixture {
    listener.subscribe()
    currentlySubscribed shouldBe true
    EventService.publish(TestEvent(1))
    EventService.publish(TestEvent(2))
    eventually {
      ids shouldBe List(2, 1)
    }
  }

  "subscribe" should "only deliver events to a subscriber once if it is subscribed more than once" in new Fixture {
    listener.subscribe()
    currentlySubscribed shouldBe true
    EventService.publish(TestEvent(1))
    Thread.sleep(1000)
    ids shouldBe List(1)
  }

  "unsubscribe" should "work correctly" in new Fixture {
    listener.subscribe()
    currentlySubscribed shouldBe true
    EventService.publish(TestEvent(1))
    EventService.publish(TestEvent(2))
    eventually {
      ids shouldBe List(2, 1)
      currentlySubscribed shouldBe true
    }

    listener.unsubscribe()
    ids = List.empty
    eventually {
      currentlySubscribed shouldBe false
    }

    Thread.sleep(1000)
    EventService.publish(TestEvent(3))
    listener.subscribe()
    eventually {
      currentlySubscribed shouldBe true
      ids shouldBe empty
    }
  }

  it should "have no effect on a listener that is not subscribed" in new Fixture {
    listener.unsubscribe()
  }
}