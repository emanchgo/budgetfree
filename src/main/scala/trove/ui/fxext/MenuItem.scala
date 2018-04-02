package trove.ui.fxext

import scalafx.scene.input.{KeyCode, KeyCodeCombination, KeyCombination}

class MenuItem(text: String, keyCode: Option[KeyCode] = None) extends scalafx.scene.control.MenuItem(text) {
  mnemonicParsing = true
  keyCode.foreach(kc => accelerator = new KeyCodeCombination(kc, KeyCombination.AltAny))
}
