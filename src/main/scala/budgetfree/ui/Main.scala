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

import javafx.application.Application

import budgetfree.constants.ApplicationName
import budgetfree.core.BudgetFree
import budgetfree.ui.ButtonTypes._
import grizzled.slf4j.Logging

import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.stage.Modality._
import scalafx.stage.WindowEvent

private[ui] object Main extends JFXApp with Logging {

  System.setProperty("prism.lcdtext", "true")
  Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA)

  Platform.implicitExit = false

  errorDialogIntercept(BudgetFree.startup()).map(_ => showHelpAboutDialog()).recover { case _ => shutdown()}

  stage = new PrimaryStage {
    title = ApplicationName
    //    minWidth = 1200
    //    minHeight = 800
    //delegate.setMaximized(true)
    icons += ApplicationIconImage64

    scene = new WelcomeScreenScene

    onCloseRequest = (ev: WindowEvent) => {
      logger.debug("user close requested")
      if (conditionallyClose()) {
        logger.debug("Close request confirmed")
      }
      else {
        ev.consume()
      }
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

  def changeScene(): Unit = {
    stage.scene = new ActiveProjectScene
    stage.delegate.setMaximized(true)
  }

  def showHelpAboutDialog() {
    logger.debug("showHelpAboutDialog called")
    new HelpAboutDialog().showAndWait()
  }

  private[this] def confirmQuitWithUser(): Boolean = {
    logger.debug("showQuitDialog called")

    val result = new Alert(AlertType.Confirmation) {
      title = ApplicationName
      initModality(ApplicationModal)
      initOwner(Main.stage)
      headerText = "Exit BudgetFree?"
      buttonTypes = Seq(Yes,No)
      contentText = "Are you sure you want to exit BudgetFree?"
    }.showAndWait()

    result.map(bt => if(bt == Yes) true else false).fold(false)(identity)
  }

  def shutdown() {
    errorDialogIntercept(BudgetFree.shutdown())
    logger.info("Application closing")
    Platform.exit()
  }
}
