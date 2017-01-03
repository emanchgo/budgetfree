package budgetfree.core.event

import akka.actor.SupervisorStrategy.{Resume, Stop}
import akka.actor.{ActorInitializationException, ActorKilledException, OneForOneStrategy, SupervisorStrategy, SupervisorStrategyConfigurator}
import grizzled.slf4j.Logging

class AkkaSupervision extends SupervisorStrategyConfigurator with Logging {
  override def create(): SupervisorStrategy = {
    logger.debug("Creating custom akka supervisor strategy")
    OneForOneStrategy() {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException         => Stop
      case e: Exception                    =>
        logger.error("Exception in event listener", e)
        Resume
    }    
  }
}
