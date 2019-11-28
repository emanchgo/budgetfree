package trove.ui.tracking

import scalafx.geometry.Orientation.Horizontal
import scalafx.Includes._
import scalafx.scene.control.SplitPane
import trove.core.Project

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

private[ui] class TrackingPane(eventSubscriberGroup: Int, project: Project) extends SplitPane {
  val accountPane = new AccountPane(eventSubscriberGroup, project)
  val ledgerPane = new LedgerPane(project)

  orientation = Horizontal
  dividerPositions = 0.05
  items ++= Seq(accountPane, ledgerPane)

}
