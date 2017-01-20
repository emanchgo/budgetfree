package trove.core

package object event {
  
  trait Event
  
  trait EventListener {
    
    final def subscribeEvents() {
      EventService.subscribeEvents(this)
    }
    
    final def unsubscribeEvents() {
      EventService.unsubscribeEvents(this)
    }
  
    def onEvent: PartialFunction[Event,Unit] = {
      case _: Event =>
    }
  }
}


