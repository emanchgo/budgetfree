package budgetfree.ui

import javafx.application.Application

import budgetfree.constants.{ApplicationName, ApplicationVersion}
import budgetfree.core.BudgetFree
import budgetfree.ui.ButtonTypes._
import grizzled.slf4j.Logging

import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.{BorderPane, VBox}
import scalafx.scene.paint.Color
import scalafx.stage.WindowEvent

private[ui] object Main extends JFXApp with Logging {

  System.setProperty("prism.lcdtext", "true")
  Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA)

  Platform.implicitExit = false

  errorDialogIntercept(BudgetFree.startup()).map(_ => showHelpAboutDialog()).recover { case _ => shutdown()}

  stage = new PrimaryStage {
    title = ApplicationName
    minWidth = 1200
    minHeight = 800
    delegate.setMaximized(true)

    scene = new Scene {
      icons += ApplicationIconImage
      fill = Color.LightGray

      // With border panes - last in has priority.
      private[this] val accountPane = new BorderPane {
        padding = Insets(20, 10, 10, 10)
//        center = BidderUI.tableView
//        top = BidderUI.header
      }

      private[this] val transactionPane = new BorderPane {
        padding = Insets(20, 10, 10, 10)
//        center = AuctionItemUI.tableView
//        top = AuctionItemUI.header
      }

      root = new BorderPane { // main pane
        center = new SplitPane {
          dividerPositions_=(0.30, 0.70)
          items ++= Seq(accountPane, transactionPane)
        }

        top = new VBox
      }
    }

    onCloseRequest = (ev:WindowEvent) => {
      logger.debug("user close requested")
      if(confirmQuitWithUser()) {
        shutdown()
        // If the user does not confirm that they want to close, consume the event to prevent
        // SFX/JFX from terminating.
      }
      else {
        ev.consume()
      }
    }
  }

  //  stage = new PrimaryStage {
//    title = ApplicationName
//    minWidth = 1200
//    minHeight = 800
//    delegate.setMaximized(true)
//
//    scene = new Scene {
//      icons += ApplicationIconImage
//      fill = Color.LightGray
//
//      // With border panes - last in has priority.
//      private[this] val bidderPane = new BorderPane {
//        padding = Insets(20, 10, 10, 10)
//        center = BidderUI.tableView
//        top = BidderUI.header
//      }
//
//      private[this] val itemPane = new BorderPane {
//        padding = Insets(20, 10, 10, 10)
//        center = AuctionItemUI.tableView
//        top = AuctionItemUI.header
//      }
//
//      root = new BorderPane { // main pane
//        center = new SplitPane {
//          dividerPositions_=(0.30, 0.70)
//          items ++= Seq(bidderPane, itemPane)
//        }
//
//        top = MainMenuBar
//      }
//    }
//
//    onCloseRequest = (ev:WindowEvent) => {
//      logger.debug("user close requested")
//      if(confirmQuitWithUser()) {
//        shutdown()
//        // If the user does not confirm that they want to close, consume the event to prevent
//        // SFX/JFX from terminating.
//      }
//      else {
//        ev.consume()
//      }
//    }
//  }

  def showHelpAboutDialog() {
    logger.debug("showHelpAboutDialog called")
    new Alert(AlertType.Information) {
      title = ApplicationName
      initOwner(Main.stage)
      graphic = ApplicationIconImageView
      headerText = "About BudgetFree"

      def blankLabel = new Label(" ")

      val appLabel_1 = new Label(s"BudgetFree Version $ApplicationVersion")
      val description_1 = new Label("A FREE desktop application that helps you track your finances")
      val description_2 = new Label("and literally FREES you from complex budgeting!")
      val copyrightLabel = new Label("Copyright © 2016 Eric John Fredericks")
      val licenseLinkLabel = new Label("This software is licensed  under the")
      val licenseLink = new Hyperlink {
        text = "GNU General Public License, version 3.0"
        onAction = (_: ActionEvent) => { Main.hostServices.showDocument("https://www.gnu.org/licenses/gpl-3.0.txt")}
      }
      val iconLinkLabel = new Label("Icons provided are free for personal or commercial use under license by")
      val iconLink = new Hyperlink {
        text = "Icons8."
        onAction = (_: ActionEvent) => { Main.hostServices.showDocument("https://icons8.com")}
      }
      val thirdPartyLicenseLinkLabel = new Label("This software incorporates many open source libraries.")
      val thridPartyLicenseButton = new Button {
        text = "Third-Party Licenses..."
        tooltip = "Click here for third-party licensing information"
        onAction = _ => new Alert(AlertType.Information) {
          title = ApplicationName
          initOwner(Main.stage)
          headerText = "Third Party Licensing"
          contentText = "THIRD PARTY LICENSE INFO HERE"
          buttonTypes = Seq(Ok)
          resizable = false
        }.showAndWait()
      }

      val theContent = new VBox {
        children = Seq(appLabel_1,
          blankLabel,
          description_1, description_2,
          blankLabel,
          copyrightLabel,
          blankLabel,
          licenseLinkLabel, licenseLink,
          blankLabel,
          iconLinkLabel, iconLink,
          blankLabel,
          thirdPartyLicenseLinkLabel,
          blankLabel,
          thridPartyLicenseButton
        )
      }

      dialogPane().contentProperty().set(theContent)
      buttonTypes = Seq(Ok)
      // Linux workaround
      resizable = true
      //dialogPane().setPrefSize(600, 400) // I tried setting the width/height values and the width didn't work.
      resizable = false
    }.showAndWait()
  }




  def confirmQuitWithUser(): Boolean = {
    logger.debug("showQuitDialog called")

    val result = new Alert(AlertType.Confirmation) {
      title = ApplicationName
      initOwner(Main.stage)
      headerText = "Exit BudgetFree?"
      buttonTypes = Seq(Yes,No)
      contentText = "Are you sure you want to exit BudgetFree?"
    }.showAndWait()

    result.map(bt => if(bt == Yes) true else false).orElse(Some(false)).get
  }

  private[ui] def shutdown() {
    errorDialogIntercept(BudgetFree.shutdown())
    logger.info("Application closing")
    Platform.exit()
  }
}
