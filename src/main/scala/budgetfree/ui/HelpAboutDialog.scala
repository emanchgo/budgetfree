package budgetfree.ui

import budgetfree.constants.{ApplicationName, ApplicationVersion}
import budgetfree.ui.ButtonTypes._

import scala.io.Source
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, _}
import scalafx.scene.layout.VBox
/**
  * Created by eric on 12/25/16.
  */
private[ui] class HelpAboutDialog extends Alert(AlertType.Information) {
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

  private[this] def thirdPartyLicenseText = {
    val fileContents = Source.fromFile(ThirdPartyLicenseUrlTextUri).getLines.mkString("\n")
    val ta = new TextArea(fileContents)
    ta.editable = false
    ta
  }

  val thirdPartyLicenseButton = new Button {
    mnemonicParsing = true
    text = "_Third-Party Licenses..."

    tooltip = "Click here for third-party licensing information"
    onAction = _ => new Alert(AlertType.Information) {
      title = ApplicationName
      initOwner(Main.stage)
      headerText = "Third Party Licensing"
      dialogPane().content = thirdPartyLicenseText
      dialogPane().setPrefSize(700, 800) // I tried setting the width/height values and the width didn't work.
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
      thirdPartyLicenseButton
    )
  }

  dialogPane().content = theContent
  buttonTypes = Seq(Ok)
  // Linux workaround
  resizable = true
  //dialogPane().setPrefSize(600, 400) // I tried setting the width/height values and the width didn't work.
  resizable = false

}
