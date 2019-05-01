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

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.layout.BorderPane
import trove.core.Project

class AccountPane(project: Project) extends BorderPane {
  private[this] val accountFunctions = new AccountFunctions(project.accountsService)

  padding = Insets(10, 10, 10, 10)
  center = new AccountsView(project.accountsService)
  top = new AccountsButtonBar(
    addAccountFn = accountFunctions.addAccount
  )
  minWidth = 300
  prefWidth = 300


  // Sets margin for center and top items in border pane; net result is that 10 px will be inserted.
  BorderPane.setMargin(center(), Insets(5))
  BorderPane.setMargin(top(), Insets(5))
}
