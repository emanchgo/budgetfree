package trove.ui.fxext

import scalafx.scene.input.{KeyCode, KeyCodeCombination, KeyCombination}

class Menu(text: String, keyCode: Option[KeyCode] = None) extends scalafx.scene.control.Menu(text) {
  mnemonicParsing = true
  keyCode.foreach(kc => accelerator = new KeyCodeCombination(kc, KeyCombination.AltAny))
}