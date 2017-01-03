package budgetfree.ui

import budgetfree.core.event.{Event, EventListener}

import scalafx.application.Platform

// Wrapper to ensure we update UI on event dispatch thread.
trait UIEventListener extends EventListener {

    final override def onEvent: PartialFunction[Event,Unit] = {
      case e: Event =>
        if(onReceive.isDefinedAt(e)) {
          Platform.runLater {
            onReceive(e)
        }
      }
    }

    def onReceive: PartialFunction[Event,Unit]
    
    subscribeEvents()
}
