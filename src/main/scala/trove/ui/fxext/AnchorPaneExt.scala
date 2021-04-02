/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2021 Eric John Fredericks.
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

package trove.ui.fxext

import scalafx.scene.Node
import scalafx.scene.layout.AnchorPane.{setBottomAnchor, setLeftAnchor, setRightAnchor, setTopAnchor}

object AnchorPaneExt {
  /**
    * Sets the anchors for the child when contained by an anchorpane.
    *
    * @param child Node to be set
    * @param top Top Anchor
    * @param left Left Anchor
    */
  def setAnchors(child: Node, top: Double, left: Double): Unit = {
    setTopAnchor(child, top)
    setLeftAnchor(child, left)
  }

  /**
    * Sets the anchors for the child when contained by an anchorpane.
    *
    * @param child Node to be set
    * @param top Top Anchor
    * @param right Right Anchor
    * @param bottom Bottom Anchor
    * @param left Left Anchor
    */
  def setAnchors(child: Node, top: Double, right: Double, bottom: Double, left: Double): Unit = {
    setTopAnchor(child, top)
    setRightAnchor(child, right)
    setBottomAnchor(child, bottom)
    setLeftAnchor(child, left)
  }

}
