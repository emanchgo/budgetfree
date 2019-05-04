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
import javafx.application.{Application => JFXApplication}
import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.control.Alert.AlertType
import trove.constants.ApplicationName
import trove.core.Trove
import trove.core.infrastructure.event.Event
import trove.events.ProjectChanged
import trove.ui.ButtonTypes._
import trove.ui.fxext.AppModalAlert

private[ui] object Main extends JFXApp with Logging {

  System.setProperty("prism.lcdtext", "true")
  JFXApplication.setUserAgentStylesheet(JFXApplication.STYLESHEET_MODENA)

  Platform.implicitExit = false

  promptUserWithError(Trove.startup()).recover { case _ => shutdown()}

  stage = new PrimaryStage with UIEventListener {
    title = ApplicationName

    setWelcomeScene()

    icons += ApplicationIconImage64

    onCloseRequest = ev => {
      logger.debug("user close requested")
      if (conditionallyQuit()) {
        logger.debug("Close request confirmed")
      }
      else {
        ev.consume()
      }
    }

    def onReceive: PartialFunction[Event, Unit] = {
      case ProjectChanged(maybeProject) => maybeProject.fold[Unit](setWelcomeScene()){ prj =>
        hide()
        maximized = true
        title = s"$ApplicationName [ ${prj.name} ]"
        scene = new ActiveProjectScene(prj)
        show()
      }
    }

    private[this] def setWelcomeScene(): Unit = {
      title = ApplicationName
      maximized = false
      height = 600
      width = 800
      centerOnScreen()
      scene = new WelcomeScene
    }
  }

  def conditionallyQuit(): Boolean = {
    if(confirmQuitWithUser()) {
      shutdown()
      true
    }
    else {
      false
    }
  }

  def showHelpAboutDialog(): Unit = {
    logger.debug("showHelpAboutDialog called")
    new HelpAboutDialog().showAndWait()
  }

  private[this] def confirmQuitWithUser(): Boolean = {
    logger.debug("showQuitDialog called")

    val result = new AppModalAlert(AlertType.Confirmation) {
      headerText = "Exit Trove?"
      buttonTypes = Seq(Yes,No)
      contentText = "Are you sure you want to exit Trove?"
    }.showAndWait()

    result.map(bt => if(bt == Yes) true else false).fold(false)(identity)
  }

  def shutdown(): Unit = {
    promptUserWithError(Trove.shutdown())
    logger.debug("Application closing")
    Platform.exit()
  }
}
