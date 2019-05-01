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
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{ButtonType, Dialog}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.GridPane
import trove.constants.ApplicationName
import trove.models.{Account, AccountTypes}
import trove.ui.fxext._
import trove.ui._
import trove.ui.{ApplicationIconImage64, ButtonTypes, Main}

trait AccountDialogFieldMetadata {
  val Id = FieldMetadata("Id")
  val AccountTypeMeta = FieldMetadata("Account Type")
  val AccountNameMeta = FieldMetadata("Account Name", length=30)
  val AccountCodeMeta = FieldMetadata("Account Code", length=10)
  val AccountDescriptionMeta = FieldMetadata("Description", length=50)
  val IsPlaceholderMeta = FieldMetadata("Transfers Allowed\nIn Subaccounts Only")
}

private[tracking] object AccountDialog {

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

private[tracking] final class AccountDialog(account: Option[Account] = None)
  extends Dialog[ButtonType]
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
  initOwner(Main.stage)

  dialogPane().setMinHeight(625)
  dialogPane().setMaxHeight(625)
  dialogPane().setMinWidth(750)
  dialogPane().setMaxWidth(750)

  private[this] val id: Option[Long] = account.flatMap(_.id)
  private[this] val nextVersion: Long = account.map(_.version + 1).getOrElse(0)

  private[this] val accountTypeField = ChoiceBox(AccountTypeMeta, AccountTypes.values.toSeq) // replace later with parent selector
  private[this] val accountNameField = TextField(AccountNameMeta)
  private[this] val accountCodeField = TextField(AccountCodeMeta)
  private[this] val accountDescriptionField = TextField(AccountDescriptionMeta)
  private[this] val isPlaceholderField = CheckBox(IsPlaceholderMeta)

  dialogPane().content = new GridPane {
    hgap = 5
    vgap = 5
    padding = Insets(10, 10, 10, 10)

    add(accountNameField.label, 0, 0)
    add(accountNameField, 1, 0)

    add(accountDescriptionField.label, 2, 0)
    add(accountDescriptionField, 3, 0)

    add(accountCodeField.label, 0, 1)
    add(accountCodeField, 1, 1)

    add(isPlaceholderField.label, 2, 1)
    add(isPlaceholderField, 3, 1)

    add(accountTypeField.label, 0, 2)
    add(accountTypeField, 1, 2)
  }

  override protected def buildFromInput: Account = Account(
    id = id,
    version = nextVersion,
    accountType = accountTypeField.value(),
    name = accountNameField.text().trim,
    code = accountCodeField.text().trim.toOption,
    description = accountDescriptionField.text().trim.toOption,
    isPlaceholder = isPlaceholderField.isSelected
  )

  private[this] def updateButtonStatus(): Unit =
    saveButton.disable = accountNameField.text().trim.isEmpty || accountTypeField.value() == null

  // Disable when minimally required fields are empty
  accountTypeField.value.onChange {
    (_,_,_) => updateButtonStatus()
  }
  accountNameField.text.onChange {
    (_,_,_) => updateButtonStatus()
  }

  account.foreach { acct =>
    accountTypeField.value = acct.accountType
    accountNameField.text = acct.name
    accountCodeField.text = acct.code.getOrElse("")
    isPlaceholderField.selected = acct.isPlaceholder
    accountDescriptionField.text = acct.description.getOrElse("")
  }

  updateButtonStatus()

}
