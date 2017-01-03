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

import budgetfree.core.BudgetFree
import budgetfree.ui.fxext.AppModalAlert
import grizzled.slf4j.Logging

import scalafx.application.Platform
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Button, TextInputDialog}
import scalafx.scene.image.ImageView
import scalafx.scene.layout._
import scalafx.scene.paint.Color

private[ui] object WelcomeScene {

  abstract class WelcomeScreenButton extends Button {
    mnemonicParsing = true
    minWidth = 200
    defaultButton <== focused
  }
}

private[ui] class WelcomeScene extends Scene with Logging {

  import WelcomeScene._

  val imageView = new ImageView(ApplicationIconImage96)

  private[this] val overviewButton = new WelcomeScreenButton {
    text = "_BudgetFree Overview..."
    tooltip = "Display an overview of BudgetFree"
    onAction = _ => {
      logger.debug("BudgetFree overview called")
      new AppModalAlert(AlertType.Information) {
        headerText = "BudgetFree Overview"
        contentText = "OVERVIEW TEXT HERE"
      }.showAndWait()
    }
  }

  private[this] val openProjectButton = new WelcomeScreenButton {
    text = "_Open / Create Project..."
    tooltip = "Open an existing BudgetFree Project"
    onAction = { _ =>
      logger.debug("Open existing project called")
      ProjectChooser().foreach { chooser =>
        val result: Option[String] = chooser.showAndWait().asInstanceOf[Option[String]]
        result.flatMap {
          case "" => new TextInputDialog() {
              title = "BudgetFree"
              headerText = "Create new BudgetFree Project"
              contentText = "Enter the new project name:"
            }.showAndWait()
          case _ => result
        }.fold(logger.debug("No project selected!")) { projectName =>
          logger.debug(s"Opening project: $projectName")
          errorDialogIntercept(BudgetFree(projectName))
        }
      }
    }
  }

  private[this] val aboutButton = new WelcomeScreenButton {
    text = "_About BudgetFree..."
    tooltip = "About BudgetFree"
    onAction = _ => new HelpAboutDialog().showAndWait()
  }

  private[this] val exitButton = new WelcomeScreenButton {
    text = "_Exit BudgetFree..."
    tooltip = "Exit BudgetFree"
    onAction = _ => {
      logger.debug("Exit BudgetFree called")
      Main.conditionallyClose()
    }
  }

  root = new BorderPane {
    padding = Insets(100, 250, 100, 250)
    background = new Background(Seq(new BackgroundFill(Color.LightSlateGray, null, null)), Seq.empty)

    center = new VBox {
      spacing = 20
      alignment = Pos.Center
      children = Seq(imageView, overviewButton, blankLabel, openProjectButton, blankLabel, aboutButton, exitButton)
    }
  }

  Platform.runLater(openProjectButton.requestFocus())
}
