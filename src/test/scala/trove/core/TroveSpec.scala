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

package trove.core

import akka.actor.ActorSystem
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.mockito.Mockito._
import trove.core.infrastructure.event.{Event, EventListener, EventService, EventServiceImpl}
import trove.services.ProjectService

import scala.util.Success

class TroveSpec extends FlatSpec with MockitoSugar with Matchers {

  class StartupFixture {
    class TestTroveImpl extends TroveImpl {

      var mockActorSystem: Option[ActorSystem] = None
      var mockEventService: Option[EventServiceImpl] = None
      var mockProjectService: Option[ProjectService] = None

      private[this] def failIfDefined[T](opt: Option[T])(construct: => T): T =
        if(opt.isDefined) fail("object already defined") else {
          construct
        }

      override def actorSystem: ActorSystem = failIfDefined(mockActorSystem) {
        mockActorSystem = Some(mock[ActorSystem])
        mockActorSystem.get
      }
      override def eventService: EventService = failIfDefined(mockEventService) {
        val mes = mock[EventServiceImpl]
        mockEventService = Some(mes)
        when(mes.actorSystem).thenReturn(mockActorSystem.get)
        mes
      }
      override def projectService: ProjectService = failIfDefined(mockProjectService) {
        mockProjectService = Some(mock[ProjectService])
        mockProjectService.get
      }
    }

    val trove: TestTroveImpl = new TestTroveImpl
  }

  class ShutdownFixture {

    class MyEventService extends EventService {
      @volatile var shutDownCallCount = 0

      override def publish(event: Event): Unit = ()
      override def subscribe(listener: EventListener): Unit = ()
      override def unsubscribe(listener: EventListener): Unit = ()
      override def shutdown(): Unit = shutDownCallCount += 1
    }

    class TestTroveImpl extends TroveImpl {
      override val actorSystem: ActorSystem = mock[ActorSystem]
      override val eventService: EventService = new MyEventService
      override val projectService: ProjectService = mock[ProjectService]
      when(projectService.closeCurrentProject()).thenReturn(Success {})

    }
    val trove: TestTroveImpl = new TestTroveImpl
  }

  "TroveImpl" should "startup the application correctly" in new StartupFixture {
    trove.startup().isSuccess shouldBe true
    trove.mockActorSystem should not be empty
    trove.mockEventService should not be empty
    trove.mockProjectService should not be empty
    trove.mockEventService.get.actorSystem shouldBe trove.mockActorSystem.get
  }

  it should "shutdown the event service correctly" in new ShutdownFixture {
    trove.startup()
    trove.shutdown().isSuccess shouldBe true
    verify(trove.projectService, times(1)).closeCurrentProject()
    trove.eventService.asInstanceOf[MyEventService].shutDownCallCount shouldBe 1
  }
}
