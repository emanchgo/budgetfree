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
package trove.ui.fxext

object FieldMetadata {
  def apply(name: String, length: Int): FieldMetadata = FieldMetadata(name, Some(length))
  def apply(name: String, maxChars: Int, width: Int): FieldMetadata = FieldMetadata(name, Some(maxChars), Some(width))
}

case class FieldMetadata(name: String, maxChars: Option[Int] = None, controlWidth: Option[Int] = None) {
  maxChars.foreach(l => require(l > 0, "max chars must be a positive integer"))
  controlWidth.foreach(w => require(w > 0, "control width must be a positive integer"))
}
