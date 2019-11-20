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

import javafx.scene.control.TreeTableView
import scalafx.beans.property.ObjectProperty
import scalafx.geometry.Insets
import scalafx.scene.control.{Label, SelectionMode, TreeItem}
import trove.models.Account
import trove.models.AccountTypes.AccountType
import trove.ui.fxext.FieldMetadata

object ParentAccountSelector {
  def apply(metadata: FieldMetadata, accounts: Seq[Account]): ParentAccountSelector = new ParentAccountSelector(metadata, accounts)
}

class ParentAccountSelector(metadata: FieldMetadata, accounts: Seq[Account]) extends AccountTreeTableView(accounts) {
  val label: Label = new Label(metadata.name)
  label.setPadding(Insets(5))

  selectionModel().setSelectionMode(SelectionMode.Single)

  def selectedParentAccountId: Option[Long] = selectionModel().getSelectedItem.getValue.id
  def selectedParentAccountType: AccountType = selectionModel().getSelectedItem.getValue.accountType
  def value: ObjectProperty[TreeTableView.TreeTableViewSelectionModel[AccountTreeViewable]] = selectionModel

  def setSelectedParent(parentId: Either[Long, AccountType]): Unit = {
    val selectedTreeItem: TreeItem[AccountTreeViewable] = parentId match {
      case Left(id) =>
        accountItemsByAccountId.get(id)
      case Right(accountType) =>
        accountTypeItemsByAccountType.get(accountType)
    }
    selectionModel().select(selectedTreeItem)
  }
}
