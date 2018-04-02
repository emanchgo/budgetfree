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

package trove.core.event

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
