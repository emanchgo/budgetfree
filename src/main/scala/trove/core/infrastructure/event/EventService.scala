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

trait EventService {
  def publish(event: Event): Unit
  def subscribe(listener: EventListener): Unit
  def unsubscribe(listener: EventListener): Unit
  def shutdown(): Unit
}

private[event] sealed case class UntypedEvent(event: Event)

sealed class Subscriber(listener: EventListener) extends Actor {
  override def receive: PartialFunction[Any, Unit] = {
    case UntypedEvent(event) if listener.onEvent.isDefinedAt(event) =>
      listener.onEvent(event)
  }
}

private[core] object EventService {
  def apply(actorSystem: ActorSystem): EventService = new EventServiceImpl(actorSystem)
}

class EventServiceImpl(actorSystem: ActorSystem) extends EventService with Logging {
  logger.info("Starting event service!")

    @volatile private[this] var subscriptions = Map.empty[EventListener,ActorRef]

    override def publish(event: Event): Unit = {
      actorSystem.eventStream.publish(UntypedEvent(event))
    }

    override def shutdown(): Unit = {
      logger.debug("Shutting down actor service")
      Await.result(actorSystem.terminate(), Duration.Inf)
    }

    override def subscribe(listener: EventListener): Unit = {

      subscriptions.get(listener).fold[Unit]{
        logger.debug(s"Adding subscription for listener ${listener.toString} : ${listener.getClass.getName}")
        val props: Props = Props(classOf[Subscriber], listener)
        val subscriber = actorSystem.actorOf(props)
        actorSystem.eventStream.subscribe(subscriber, classOf[UntypedEvent])
        subscriptions += listener -> subscriber
        listener.subscribed()
        logger.debug(s"Subscriber map size: ${subscriptions.size}")
      } { _ =>
        logger.warn(s"Listener ${listener.toString} : ${listener.getClass.getName} is already subscribed for events")
      }
    }

    override def unsubscribe(listener: EventListener): Unit = {
      subscriptions.get(listener).foreach { actorRef =>
        logger.debug(s"Removing subscription for listener ${listener.toString} : ${listener.getClass.getName}")
        actorSystem.stop(actorRef)
        listener.unsubscribed()
      }
      subscriptions -= listener
      logger.debug(s"Subscriber map size: ${subscriptions.size}")
    }

}
