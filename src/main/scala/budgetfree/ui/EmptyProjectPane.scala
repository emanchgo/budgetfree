package budgetfree.ui

import grizzled.slf4j.Logging

import scalafx.geometry.Insets
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.{Background, BackgroundFill, BorderPane, VBox}
import scalafx.scene.paint.Color

/**
  * Created by eric on 12/26/16.
  */
private[ui] class EmptyProjectPane extends BorderPane with Logging {

  padding = Insets(100, 250, 100, 250)

  private[this] val welcome = Label("Welcome to BudgetFree!")

  private[this] val buttonWidth: Double = 150

  private[this] val newProjectButton = new Button {
    mnemonicParsing = true
    text = "_New Project..."
    tooltip = "Create a new BudgetFree Project"
    onAction = _ => logger.info("Create new project called") //ejf-fixMe: debug!
    minWidth = buttonWidth
  }

  private[this] val existingProjectButton = new Button {
    mnemonicParsing = true
    text = "_Open Project..."
    tooltip = "Open an existing BudgetFree Project"
    onAction = _ => logger.info("Open existing project called") //ejf-fixMe: debug!
    minWidth = buttonWidth
  }

  private[this] val exitButton = new Button {
    mnemonicParsing = true
    text = "_Exit BudgetFree..."
    tooltip = "Exit BudgetFree"
    onAction = _ => logger.info("Exit BudgetFree called") //ejf-fixMe: debug!
    minWidth = buttonWidth
  }

  background = new Background(Seq(new BackgroundFill(Color.LightSlateGray, null, null)), Seq.empty)

  center = new VBox {
    spacing = 20
    children = Seq(welcome, newProjectButton, existingProjectButton, exitButton)
  }
}
