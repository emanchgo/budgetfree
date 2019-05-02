/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2019 Eric John Fredericks.
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


package trove.ui

import grizzled.slf4j.Logging
import scalafx.application.Platform
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Button, TextInputDialog}
import scalafx.scene.image.ImageView
import scalafx.scene.layout._
import trove.core.Trove
import trove.ui.fxext.AppModalAlert

private[ui] object WelcomeScene {

  abstract class WelcomeScreenButton extends Button {
    mnemonicParsing = true
    minWidth = 200
    defaultButton <== focused
  }
}

private[ui] class WelcomeScene extends Scene(800, 600) with Logging {

  import WelcomeScene._

  val imageView = new ImageView(ApplicationIconImage96)

  private[this] val overviewButton = new WelcomeScreenButton {
    text = "_Trove Overview..."
    tooltip = "Display an overview of Trove"
    onAction = _ => {
      logger.debug("Trove overview called")
      new AppModalAlert(AlertType.Information) {
        headerText = "Trove Overview"
        contentText = "OVERVIEW TEXT HERE"
      }.showAndWait()
    }
  }

  private[this] val openProjectButton = new WelcomeScreenButton {
    text = "_Open / Create Project..."
    tooltip = "Open an existing Trove Project"
    onAction = { _ =>
      logger.debug("Open existing project called")
      ProjectChooser().foreach { chooser =>
        val result: Option[String] = chooser.showAndWait().asInstanceOf[Option[String]]
        result.flatMap {
          case "" => new TextInputDialog() {
              title = "Trove"
              headerText = "Create new Trove Project"
              contentText = "Enter the new project name:"
            }.showAndWait()
          case _ => result
        }.fold(logger.debug("No project selected!")) { projectName =>
          logger.debug(s"Opening project: $projectName")
          promptUserWithError(Trove.projectService.open(projectName))
        }
      }
    }
  }

  private[this] val aboutButton = new WelcomeScreenButton {
    text = "_About Trove..."
    tooltip = "About Trove"
    onAction = _ => new HelpAboutDialog().showAndWait()
  }

  private[this] val exitButton = new WelcomeScreenButton {
    text = "E_xit Trove..."
    tooltip = "Exit Trove"
    onAction = _ => {
      logger.debug("Exit Trove called")
      Main.conditionallyQuit()
    }
  }

  root = new BorderPane {
    padding = Insets(100, 250, 100, 250)
    background = new Background(Seq(new BackgroundFill(TroveColor.RichDarkBlue, null, null)), Seq.empty)

    center = new VBox {
      spacing = 20
      alignment = Pos.Center
      children = Seq(imageView, overviewButton, blankLabel, openProjectButton, blankLabel, aboutButton, exitButton)
    }
  }

  Platform.runLater(openProjectButton.requestFocus())
}
