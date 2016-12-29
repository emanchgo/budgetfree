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

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Background, BackgroundFill, BorderPane, VBox}
import scalafx.scene.paint.Color

private[ui] class EmptyProjectScene extends Scene with Logging {

  private[this] val buttonWidth: Double = 175

  private[ui] val image = new Image(ApplicationIconUrl.toExternalForm, 96, 96, true, true)
  private[ui] val imageView = new ImageView(image)

  private[this] val newProjectButton = new Button {
    mnemonicParsing = true
    text = "_New Project..."
    tooltip = "Create a new BudgetFree Project"
    onAction = _ => logger.debug("Create new project called")
    minWidth = buttonWidth
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
  }

  private[this] val aboutButton = new Button {
    mnemonicParsing = true
    text = "_About BudgetFree..."
    tooltip = "About BudgetFree"
    onAction = _ => new HelpAboutDialog().showAndWait()
    minWidth = buttonWidth
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
  }

  root = new BorderPane {
    padding = Insets(100, 250, 100, 250)
    background = new Background(Seq(new BackgroundFill(BudgetFreeColor.PenguinPurple, null, null)), Seq.empty)

    center = new VBox {
      spacing = 20
      alignment = Pos.Center
      children = Seq(imageView, newProjectButton, openProjectButton, aboutButton, exitButton)
    }
  }
}
