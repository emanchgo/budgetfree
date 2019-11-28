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
package trove.ui.tracking

import grizzled.slf4j.Logging
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{Button, ButtonType}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.AnchorPane
import trove.constants.ApplicationName
import trove.models.Account
import trove.ui.fxext._
import trove.ui.{ApplicationIconImage64, ButtonTypes, Main, _}

trait AccountDialogFieldMetadata {
  val Id = FieldMetadata("Id")
  val AccountParentMeta = FieldMetadata("Parent Account")
  val AccountNameMeta = FieldMetadata("Account Name", maxChars=30, width=270)
  val AccountCodeMeta = FieldMetadata("Account Code", maxChars=10, width=105)
  val AccountDescriptionMeta = FieldMetadata("Description", maxChars=50, width=435)
  val IsPlaceholderMeta = FieldMetadata("Transfers Allowed\nIn Subaccounts Only")
  val UnitMeta = FieldMetadata("Currency/Security")
  val SmallestFractionMeta = FieldMetadata("Smallest Fraction")
}

private[tracking] object AccountDialog {

  val LineHeight: Int = 40
  val Line1: Int = 0 * LineHeight
  val Line2: Int = 1 * LineHeight
  val Line3: Int = 2 * LineHeight
  val Line4: Int = 3 * LineHeight

  sealed trait AccountDialogType {
    def headerText: String
    def saveButtonType: ButtonType
  }

  case object AddAccount extends AccountDialogType {
    override def headerText: String = "Add New Account"
    def saveButtonType: ButtonType = ButtonTypes.Add
  }

  case object EditAccount extends AccountDialogType {
    override def headerText: String = "Edit Account"
    def saveButtonType: ButtonType = ButtonTypes.Update
  }
}

private[tracking] final class AccountDialog(parentCandidates: Seq[Account], account: Option[Account] = None)
  extends Dialog[ButtonType](Main.stage)
  with AccountDialogFieldMetadata
  with PromptUntilValid[Account]
  with Logging {

  import AccountDialog._

  lazy val dialogType: AccountDialogType = account.map(_ => EditAccount).getOrElse(AddAccount)

  dialogPane().buttonTypes = Seq(dialogType.saveButtonType, ButtonTypes.Cancel)

  private[this] val saveButton: Node = dialogPane().lookupButton(dialogType.saveButtonType)

  title = ApplicationName
  graphic = new ImageView(ApplicationIconImage64)
  headerText = dialogType.headerText

  dialogPane().setMinHeight(625)
  dialogPane().setMaxHeight(625)
  dialogPane().setMinWidth(750)
  dialogPane().setMaxWidth(750)

  private[this] val id: Option[Long] = account.flatMap(_.id)
  private[this] val nextVersion: Long = account.map(_.version + 1).getOrElse(0)

  private[this] val accountParentField = ParentAccountSelector(AccountParentMeta,
    account.map(a => parentCandidates.filterNot(_.id.get == a.id.get)).getOrElse(parentCandidates))
  private[this] val accountNameField = TextField(AccountNameMeta)
  private[this] val accountCodeField = TextField(AccountCodeMeta)
  private[this] val accountDescriptionField = TextField(AccountDescriptionMeta)
  private[this] val isPlaceholderField = CheckBox(IsPlaceholderMeta)
  private[this] val unitField = TextField(UnitMeta)
  private[this] val unitSelectButton = new Button("...") {
    //ejf-fixMe: implement, defaults USD for most things?
  }
  private[this] val smallestFraction = ChoiceBox(SmallestFractionMeta, Seq(".01", ".001", ".0001", ".00001", ".000001"))

  dialogPane().content = new AnchorPane {
    padding = Insets(10)

    // Line 1
    AnchorPaneExt.setAnchors(accountNameField.label, Line1, 0)
    AnchorPaneExt.setAnchors(accountNameField, Line1, 110)
    AnchorPaneExt.setAnchors(accountCodeField.label, Line1, 400)
    AnchorPaneExt.setAnchors(accountCodeField, Line1, 510)

    // Line 2
    AnchorPaneExt.setAnchors(accountDescriptionField.label, Line2, 0)
    AnchorPaneExt.setAnchors(accountDescriptionField, Line2, 85)
    AnchorPaneExt.setAnchors(isPlaceholderField.label, Line2, 540)
    AnchorPaneExt.setAnchors(isPlaceholderField, Line2 + 5, 685)

    // Line 3
    AnchorPaneExt.setAnchors(accountParentField.label, Line3, 0)
    AnchorPaneExt.setAnchors(accountParentField, Line3, 120)
    AnchorPaneExt.setAnchors(unitField.label, Line3, 380)
    AnchorPaneExt.setAnchors(unitSelectButton, Line3, 510)
    AnchorPaneExt.setAnchors(unitField, Line3, 550)

    // Line 4 - pushed to the right because of the parent choice box
    AnchorPaneExt.setAnchors(smallestFraction.label, Line4, 380)
    AnchorPaneExt.setAnchors(smallestFraction, Line4, 510)

    children = Seq(
      accountNameField.label, accountNameField,
      accountCodeField.label, accountCodeField,
      accountDescriptionField.label, accountDescriptionField,
      isPlaceholderField.label, isPlaceholderField,
      unitField.label, unitField, unitSelectButton,
      accountParentField.label, accountParentField,
      smallestFraction.label, smallestFraction
    )
  }

  override protected def buildFromInput: Account = Account(
    id = id,
    version = nextVersion,
    accountType = accountParentField.selectedParentAccountType,
    name = accountNameField.text().trim,
    code = accountCodeField.text().trim.toOption,
    description = accountDescriptionField.text().trim.toOption,
    isPlaceholder = isPlaceholderField.isSelected,
    parentAccountId = accountParentField.selectedParentAccountId
    //ejf-fixMe: add currency
    //ejf-fixMe: add smallest fraction
  )

  private[this] def updateButtonStatus(): Unit =
    saveButton.disable = accountNameField.text().trim.isEmpty || accountParentField.value() == null

  // Disable when minimally required fields are empty
  accountParentField.value.onChange {
    (_,_,_) => updateButtonStatus()
  }
  accountNameField.text.onChange {
    (_,_,_) => updateButtonStatus()
  }

  account.foreach { acct =>
    accountParentField.setSelectedParent(acct.id.map(Left(_)).getOrElse(Right(acct.accountType)))
    accountNameField.text = acct.name
    accountCodeField.text = acct.code.getOrElse("")
    isPlaceholderField.selected = acct.isPlaceholder
    accountDescriptionField.text = acct.description.getOrElse("")
  }

  updateButtonStatus()

}
