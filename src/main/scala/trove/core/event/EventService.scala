package trove.core.event

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
