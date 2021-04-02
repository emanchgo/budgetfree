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

package trove.core.infrastructure.event

import akka.actor.ActorSystem
import org.mockito.MockitoSugar
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class EventServiceImplSpec extends AnyFlatSpec with MockitoSugar with should.Matchers {

  case class TestEvent(id: Int) extends Event

  val actorSystem: ActorSystem = ActorSystem("test")

  class Fixture {

    val eventService = new EventServiceImpl(actorSystem)

    @volatile var currentlySubscribed = false
    @volatile var ids: List[Int] = List.empty

     class Listener extends EventListener {

      override val eventSubscriberGroup: Int = 42

      override def onEvent: PartialFunction[Event, Unit] = {
        case TestEvent(id) =>
          ids = id :: ids
      }

      override def subscribed(): Unit = currentlySubscribed = true

      override def unsubscribed(): Unit = currentlySubscribed = false
    }

    val listener: EventListener = new Listener
  }

  "subscribe and publish" should "work correctly" in new Fixture {
    eventService.subscribe(listener)
    currentlySubscribed shouldBe true
    eventService.publish(TestEvent(1))
    eventService.publish(TestEvent(2))
    eventually {
      ids shouldBe List(2, 1)
    }
  }

  "subscribe" should "only deliver events to a subscriber once if it is subscribed more than once" in new Fixture {
    eventService.subscribe(listener)
    currentlySubscribed shouldBe true
    eventService.publish(TestEvent(1))
    Thread.sleep(1000)
    ids shouldBe List(1)
  }

  "unsubscribe" should "work correctly" in new Fixture {
    eventService.subscribe(listener)
    currentlySubscribed shouldBe true
    eventService.publish(TestEvent(1))
    eventService.publish(TestEvent(2))
    eventually {
      ids shouldBe List(2, 1)
      currentlySubscribed shouldBe true
    }

    eventService.unsubscribe(listener)
    ids = List.empty
    eventually {
      currentlySubscribed shouldBe false
    }

    Thread.sleep(1000)
    eventService.publish(TestEvent(3))
    eventService.subscribe(listener)
    eventually {
      currentlySubscribed shouldBe true
      ids shouldBe empty
    }
  }

  it should "have no effect on a listener that is not subscribed" in new Fixture {
    eventService.unsubscribe(listener)
  }
}