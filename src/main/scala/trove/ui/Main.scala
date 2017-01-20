/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances and literally FREES you from complex budgeting.
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
 *
 *  Trove is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import javafx.application.{Application => JFXApplication}
import javafx.scene.control.{Button => JFXButton}
import javafx.scene.input.{KeyCode => JFXKeyCode, KeyEvent => JFXKeyEvent}

import trove.constants.ApplicationName
import trove.core.Trove
import trove.core.event.Event
import trove.events.ProjectChanged
import trove.ui.ButtonTypes._
import trove.ui.fxext.AppModalAlert
import com.sun.javafx.scene.control.behavior.{ButtonBehavior => JFXButtonBehavior, KeyBinding => JFXKeyBinding}
import grizzled.slf4j.Logging

import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.Scene
import scalafx.stage.WindowEvent

private[ui] object Main extends JFXApp with Logging {

  System.setProperty("prism.lcdtext", "true")
  JFXApplication.setUserAgentStylesheet(JFXApplication.STYLESHEET_MODENA)

  Platform.implicitExit = false

  // This code will force buttons to fire when ENTER is pressed.
  new JFXButtonBehavior[JFXButton](new JFXButton()) {
    import JFXButtonBehavior._
    BUTTON_BINDINGS.add(new JFXKeyBinding(JFXKeyCode.ENTER, JFXKeyEvent.KEY_PRESSED, "Press"))
    BUTTON_BINDINGS.add(new JFXKeyBinding(JFXKeyCode.ENTER, JFXKeyEvent.KEY_RELEASED, "Release"))
  }

  errorDialogIntercept(Trove.startup()).recover { case _ => shutdown()}

  stage = new PrimaryStage with UIEventListener {
    title = ApplicationName
    //    minWidth = 1200
    //    minHeight = 800
    //delegate.setMaximized(true)
    icons += ApplicationIconImage64

    scene = new WelcomeScene

    onCloseRequest = (ev: WindowEvent) => {
      logger.debug("user close requested")
      if (conditionallyClose()) {
        logger.debug("Close request confirmed")
      }
      else {
        ev.consume()
      }
    }

    def onReceive: PartialFunction[Event, Unit] = {
      case ProjectChanged(projectName) => projectName.fold[Unit]{scene = new WelcomeScene}{pn => scene = new ActiveProjectScene(pn)}
    }
  }

  def conditionallyClose(): Boolean = {
    if(confirmQuitWithUser()) {
      shutdown()
      true
    }
    else {
      false
    }
  }

  def showHelpAboutDialog() {
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

  def shutdown() {
    errorDialogIntercept(Trove.shutdown())
    logger.debug("Application closing")
    Platform.exit()
  }
}
