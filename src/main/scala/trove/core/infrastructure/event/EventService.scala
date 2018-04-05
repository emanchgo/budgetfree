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

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import grizzled.slf4j.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object EventService extends Logging {

  // ActorSystem is a heavy object: create only one per application
  // http://doc.example.io/docs/example/snapshot/scala/actors.html
  logger.debug("Starting actor system")
  private[this] val system = ActorSystem("actorsystem")

  @volatile private[this] var subscriptions = Map.empty[EventListener,ActorRef]

  def publish(event: Event) {
    system.eventStream.publish(UntypedEvent(event))
  }

  def shutdown(): Unit = {
    logger.debug("Shutting down actor service")
    Await.result(system.terminate(), Duration.Inf)
  }

  private[event] def subscribeEvents(listener: EventListener) {
    if(!subscriptions.contains(listener)) {
      logger.debug(s"Adding subscription for listener ${listener.toString} : ${listener.getClass.getName}")
      val props = Props(classOf[Subscriber], listener)
      val subscriber = system.actorOf(props)
      system.eventStream.subscribe(subscriber, classOf[UntypedEvent])
      subscriptions += listener -> subscriber
      logger.debug(s"Subscriber map size: ${subscriptions.size}")
    }
    else {
      logger.warn(s"Listener ${listener.toString} : ${listener.getClass.getName} is already subscribed for events")
    }
  }

  private[event] def unsubscribeEvents(listener: EventListener) {
    subscriptions.get(listener).foreach { _ =>
      logger.debug(s"Removing subscription for listener ${listener.toString} : ${listener.getClass.getName}")
      system.stop(_)
    }
    subscriptions -= listener
    logger.debug(s"Subscriber map size: ${subscriptions.size}")
  }

  sealed case class UntypedEvent(event: Event)

  sealed class Subscriber(listener: EventListener) extends Actor {
    override def receive: PartialFunction[Any, Unit] = {
      case UntypedEvent(event) => listener.onEvent(event)
    }
  }
}
