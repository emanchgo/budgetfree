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

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Orientation.Vertical
import scalafx.scene.Scene
import scalafx.scene.control.{Label, SplitPane}
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
      dividerPositions_=(0)
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

  root = new BorderPane {
    center = new SplitPane {
      orientation = Vertical
      dividerPositions_=(0)
      items ++= Seq(mainPane, transactionDetailPane)
    }
    top = Label("Main Menu")
  }

}
