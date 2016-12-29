package budgetfree.ui

import scalafx.scene.Scene
import scalafx.scene.control.Label
import scalafx.scene.layout.BorderPane
import scalafx.scene.paint.Color


/**
  * Created by eric on 12/26/16.
  */
private[ui] class BudgetFreeScene extends Scene {

  root = new BorderPane { // main pane
    center = new EmptyProjectPane
    top = Label("Main Menu")
  }

  fill = Color.LightSlateGrey

}
