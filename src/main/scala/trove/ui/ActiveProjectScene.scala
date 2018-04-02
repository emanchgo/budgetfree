/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2018 Eric John Fredericks.
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

import trove.core.Trove
import trove.ui.ButtonTypes.{No, Yes}
import trove.ui.fxext.{AppModalAlert, Menu, MenuItem}

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Orientation.Vertical
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.input.KeyCode
import scalafx.scene.layout.{BorderPane, VBox}

private[ui] class ActiveProjectScene(projectName: String) extends Scene {

  // With border panes - last in has priority.
  private[this] val accountPane = new BorderPane {
    padding = Insets(20, 10, 10, 10)
    center = Label("Accounts Pane")
    top = Label("Accounts Button Bar")
    //        center = BidderUI.tableView
    //        top = BidderUI.header
    minWidth = 300
    prefWidth = 300
  }

  private[this] val transactionPane = new BorderPane {
    padding = Insets(20, 10, 10, 10)
    center = Label("Transactions Pane")
    top = Label("Transactions Button Bar")
    prefWidth = 700
    minWidth = 700
    //        center = AuctionItemUI.tableView
    //        top = AuctionItemUI.header
  }

  private[this] val mainPane = new BorderPane {
    center = new SplitPane {
      //dividerPositions_=(0)
      dividerPositions = 0
      items ++= Seq(accountPane, transactionPane)
    }
    prefHeight = 500
    minHeight = 500
    top = new VBox
  }

  private[this] val transactionDetailPane = new BorderPane {
    center = Label("Transaction Detail Pane")
    top = Label("Transaction Detail Button Bar")
    prefHeight = 100
    minHeight = 100
  }

  private[this] val trackingPane =  new BorderPane {
    center = new SplitPane {
      orientation = Vertical
      dividerPositions = 0
      //dividerPositions_=(0)
      items ++= Seq(mainPane, transactionDetailPane)
    }
  }

  private[this] val tabPane = new TabPane {
    tabs = Seq(
      new Tab {
        text = "HOME"
        tooltip = "High level view"
        closable = false
        tabMaxHeight = 56
        graphic = new ImageView(getImage("pie-chart-48.png", 48))
      },
      new Tab {
        text = "CASH FLOWS"
        tooltip = "Create cash flow plans"
        closable = false
        tabMaxHeight = 56
        graphic = new ImageView(getImage("plumbing-48.png", 48))
      },
      new Tab {
        text = "TRACKING"
        tooltip = "Track individual transactions"
        closable = false
        content = trackingPane
        tabMaxHeight = 56
        graphic = new ImageView(getImage("ledger-48.png", 48))
      },
      new Tab {
        text = "REPORTS"
        tooltip = "Create and view customized reports"
        closable = false
        tabMaxHeight = 56
        graphic = new ImageView(getImage("report-card-48.png", 48))
      },
      new Tab {
        text = "TROVE"
        tooltip = "See how you stand on your savings goals"
        closable = false
        tabMaxHeight = 56
        graphic = new ImageView(getImage("gold-pot-48.png", 48))
      }
    )
  }

  private[this] val fileMenu = new Menu("_File", Some(KeyCode.F)) {
    items = Seq(
      new MenuItem("_Close Project", Some(KeyCode.C)) {
        onAction = _ => if(confirmCloseCurrentProjectWithUser()) {
          Trove.closeCurrentProject()
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
      contentText = s"Are you sure you want to close project '$projectName?'"
    }.showAndWait()

    result.map(bt => if(bt == Yes) true else false).fold(false)(identity)
  }

}
