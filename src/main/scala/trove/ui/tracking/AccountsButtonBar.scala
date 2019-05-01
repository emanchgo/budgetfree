package trove.ui.tracking

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Tooltip}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.text.{Font, FontWeight, Text}

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

private[tracking] class AccountsButtonBar(addAccountFn: ActionEvent => Unit) extends VBox {
  import trove.ui._

  children = Seq(
    new Text("Accounts") {
      font = Font("Verdana", FontWeight.Bold, 20)
    },

    new HBox {
      children = Seq(
        new Button { // Add
          graphic = new ImageView(getImage("add-new-16.png", 16))
          tooltip = Tooltip("Add an  account")
          onAction = addAccountFn
        },
        new Button { // Edit
          tooltip = Tooltip("Edit selected account")
          graphic = new ImageView(getImage("edit-16.png", 16))
          //onAction = (ae:ActionEvent) => editBidder(Option(tableView.selectionModel().getSelectedItem))
        },
        new Button { // Delete
          tooltip = Tooltip("Delete selected account")
          graphic = new ImageView(getImage("delete-16.png", 16))
          //onAction = (ae:ActionEvent) => deleteBidder(Option(tableView.selectionModel().getSelectedItem))
        }
      )
      spacing = 5
    },
  )
  spacing = 5
}
