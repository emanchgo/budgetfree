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

import scalafx.scene.control.{ButtonType, Dialog}
import trove.exceptional.ValidationError
import trove.ui.{ButtonTypes, promptUserWithError}

import scala.annotation.tailrec
import scala.util.{Success, Try}

private[ui] trait PromptUntilValid[A] {

  self: Dialog[ButtonType] =>

  protected def buildFromInput: A

  @tailrec
  // The op could be a template method too, but we're decoupling the behavior interacting with the server.
  final def promptUntilValid(op: A => Try[A]): Option[A] = {
    showAndWait() match {
      case None => None
      case Some(bt) => bt match {
        case ButtonTypes.Cancel => None
        case _ => promptUserWithError(op(buildFromInput)) match {
          case Success(it) =>
            Some(it)
          case ValidationError(_, _, _) =>
            promptUntilValid(op)
          case _ => None
        }
      }
    }
  }
}
