package budgetfree.ui

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Orientation.Vertical
import scalafx.scene.control.{Label, SplitPane}
import scalafx.scene.layout.{BorderPane, VBox}

/**
  * Created by eric on 12/26/16.
  */
private[ui] class ActiveProjectPane extends SplitPane {

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

  orientation = Vertical
  dividerPositions_=(0)
  items ++= Seq(mainPane, transactionDetailPane)
}
