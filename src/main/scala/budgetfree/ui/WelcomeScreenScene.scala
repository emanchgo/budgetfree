/*
 *  # BudgetFree
 *
 *  This file is part of BudgetFree - A FREE desktop budgeting application that
 *  helps you track your finances and literally FREES you from complex budgeting.
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
 *
 *  BudgetFree is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  BudgetFree is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with BudgetFree.  If not, see <http://www.gnu.org/licenses/>.
 */


package budgetfree.ui

import grizzled.slf4j.Logging

import scalafx.application.Platform
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.effect.DropShadow
import scalafx.scene.image.ImageView
import scalafx.scene.layout._
import scalafx.scene.paint.Color

private[ui] object WelcomeScreenScene {

  val buttonWidth: Double = 275
  val buttonStyle = "-fx-font-size: 1.25em; -fx-text-fill: #660066; border: 2px solid #000000; "
}

private[ui] class WelcomeScreenScene extends Scene with Logging {

  import WelcomeScreenScene._

  val imageView = new ImageView(ApplicationIconImage96)

  private[this] val overviewButton = new Button {
    mnemonicParsing = true
    text = "_BudgetFree Overview..."
    tooltip = "Display an overview of BudgetFree"
    onAction = _ => logger.debug("BudgetFree overview called")
    minWidth = buttonWidth
    style = buttonStyle
    effect = new DropShadow()
  }

  private[this] val newProjectButton = new Button {
    mnemonicParsing = true
    text = "_New Project..."
    tooltip = "Create a new BudgetFree Project"
    onAction = _ => logger.debug("Create new project called")
    minWidth = buttonWidth
    style = buttonStyle
    effect = new DropShadow()
  }

  private[this] val openProjectButton = new Button {
    mnemonicParsing = true
    text = "_Open Project..."
    tooltip = "Open an existing BudgetFree Project"
    onAction = { _ =>
      logger.debug("Open existing project called")
      Main.changeScene()
    }
    minWidth = buttonWidth
    style = buttonStyle
    effect = new DropShadow()
  }

  private[this] val aboutButton = new Button {
    mnemonicParsing = true
    text = "_About BudgetFree..."
    tooltip = "About BudgetFree"
    onAction = _ => new HelpAboutDialog().showAndWait()
    minWidth = buttonWidth
    style = buttonStyle
    effect = new DropShadow()
  }

  private[this] val exitButton = new Button {
    mnemonicParsing = true
    text = "_Exit BudgetFree..."
    tooltip = "Exit BudgetFree"
    onAction = _ => {
      logger.debug("Exit BudgetFree called")
      Main.conditionallyClose()
    }
    minWidth = buttonWidth
    style = buttonStyle
    effect = new DropShadow()
  }

  root = new BorderPane {
    padding = Insets(100, 250, 100, 250)
    background = new Background(Seq(new BackgroundFill(Color.LightSlateGray, null, null)), Seq.empty)

    center = new VBox {
      spacing = 20
      alignment = Pos.Center
      children = Seq(imageView, overviewButton, blankLabel, openProjectButton, newProjectButton, blankLabel, aboutButton, exitButton)
    }
  }

  Platform.runLater(openProjectButton.requestFocus())
}
