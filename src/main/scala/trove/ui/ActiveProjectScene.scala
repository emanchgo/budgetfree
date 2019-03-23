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

import javafx.beans.value.ObservableValue
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Orientation.Horizontal
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.input.KeyCode
import scalafx.scene.layout.BorderPane
import trove.core.{Project, Trove}
import trove.ui.ButtonTypes.{No, Yes}
import trove.ui.fxext.{AppModalAlert, Menu, MenuItem}
import trove.ui.tracking.{AccountsButtonBar, AccountsView}

private[ui] class ActiveProjectScene(project: Project) extends Scene {

  // With border panes - last in has priority.
  //ejf-fixMe: move to tracking pkg
  private[this] val accountPane = new BorderPane {
    padding = Insets(10, 10, 10, 10)
    center = new AccountsView(project.accountsService)
    top = new AccountsButtonBar
    minWidth = 300
    prefWidth = 300

    // Sets margin for center and top items in border pane; net result is that 10 px will be inserted.
    BorderPane.setMargin(center(), Insets(5))
    BorderPane.setMargin(top(), Insets(5))
  }

  private[this] val ledgerPane = new BorderPane {
    padding = Insets(20, 10, 10, 10)
    center = Label("Account Ledger Pane")
    top = Label("Account Ledger Button Bar")
    prefWidth = 700
    minWidth = 700
  }

  private[this] val trackingPane = new SplitPane {
    orientation = Horizontal
    dividerPositions = 0.05
    items ++= Seq(accountPane, ledgerPane)
  }

  private[this] val tabPane = new TabPane {
    tabs = Seq(
      new Tab {
        text = "Home"
        tooltip = "Overall view"
        closable = false
        tabMaxHeight = ActiveProjectTabHeight
        graphic = new ImageView(getImage("pie-chart-40.png", ActiveProjectTabImageSize))
      },
      new Tab {
        text = "Tracking"
        tooltip = "Track individual accounts and transactions"
        closable = false
        content = trackingPane
        tabMaxHeight = ActiveProjectTabHeight
        graphic = new ImageView(getImage("ledger-40.png", ActiveProjectTabImageSize))
      },
      new Tab {
        text = "Cash Flows"
        tooltip = "Create cash flow plans"
        closable = false
        tabMaxHeight = ActiveProjectTabHeight
        graphic = new ImageView(getImage("plumbing-40.png", ActiveProjectTabImageSize))
      },
      new Tab {
        text = "Reports"
        tooltip = "Create and view customized reports"
        closable = false
        tabMaxHeight = ActiveProjectTabHeight
        graphic = new ImageView(getImage("report-card-40.png", ActiveProjectTabImageSize))
      },
      new Tab {
        text = "Your Trove"
        tooltip = "See where you stand on your savings goals"
        closable = false
        tabMaxHeight = ActiveProjectTabHeight
        graphic = new ImageView(getImage("gold-pot-40.png", ActiveProjectTabImageSize))
      }
    )
  }

  private[this] val fileMenu = new Menu("_File", Some(KeyCode.F)) {
    items = Seq(
      new MenuItem("_Close Project", Some(KeyCode.C)) {
        onAction = _ => if(confirmCloseCurrentProjectWithUser()) {
          Trove.projectService.closeCurrentProject()
        }
      },
      new MenuItem("E_xit Trove", Some(KeyCode.X)) {
        onAction = _ =>  Main.conditionallyQuit()
      }
    )
  }

  private[this] val helpMenu = new Menu("_Help", Some(KeyCode.H)) {
    items = Seq(
      new MenuItem("_About", Some(KeyCode.A)) {
        onAction = _ => new HelpAboutDialog().showAndWait()
      }
    )
  }

  root = new BorderPane {
    center = tabPane
    top = new MenuBar {
      menus = Seq(fileMenu, helpMenu)
    }
  }

  private[this] def confirmCloseCurrentProjectWithUser(): Boolean = {
    val result = new AppModalAlert(AlertType.Confirmation) {
      headerText = "Close Project?"
      buttonTypes = Seq(Yes,No)
      contentText = s"Are you sure you want to close project '${project.name}?'"
    }.showAndWait()

    result.map(bt => if(bt == Yes) true else false).fold(false)(identity)
  }

  // The height and width listener will keep the divider positions where
  // we want them - so that when the scene is resized, the dividers don't move
  private[this] val resizeListener = new javafx.beans.value.ChangeListener[Number] {
    override def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
      trackingPane.dividerPositions = 0.05
    }
  }

  height.addListener(resizeListener)
  width.addListener(resizeListener)

}
