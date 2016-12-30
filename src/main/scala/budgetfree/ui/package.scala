/*
 *  # BudgetFree
 *
 *  This file is part of BudgetFree - A FREE desktop budgeting application that
 *  helps you track your finances and literally FREES you from complex budgeting.
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
 *
 *  BudgetFree is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  BudgetFree is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with BudgetFree.  If not, see <http://www.gnu.org/licenses/>.
 */

package budgetfree

import budgetfree.constants._
import budgetfree.exceptional.{FailQuietly, ValidationException}
import budgetfree.ui.fxext.AppModalAlert
import grizzled.slf4j.Logging

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
import scala.util.control.NonFatal
import scala.util.{Failure, Try}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{ButtonBar, ButtonType, Label}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.paint.Color

package object ui extends Logging {

  private[this] val ApplicationIconPath = "MoneyTransfer-100.png"
  private[ui] val ApplicationIconUrl = getClass.getClassLoader.getResource(ApplicationIconPath)
  private[ui] val ApplicationIconImage64 = new Image(ApplicationIconUrl.toExternalForm, 64, 64, true, true)
  private[ui] val ApplicationIconImage96 = new Image(ApplicationIconUrl.toExternalForm, 96, 96, true, true)
  private[ui] def ApplicationIconImageView = new ImageView(ApplicationIconImage64)


  private[this] val ThirdPartyLicenseTextPath = "THIRD_PARTY_LICENSING"
  private[ui] val ThirdPartyLicenseUrlTextUri = getClass.getClassLoader.getResource(ThirdPartyLicenseTextPath).toURI

  def blankLabel = Label(" ")

  private[ui] object BudgetFreeColor {
    val PenguinPurple: Color = Color.rgb(66,0,66)
  }

  private[ui] object ButtonTypes {
//    val Add = new ButtonType("_Add", ButtonBar.ButtonData.OKDone)
//    val Cancel = new ButtonType("_Cancel", ButtonBar.ButtonData.CancelClose)
    val Close = new ButtonType("_Close", ButtonBar.ButtonData.OKDone)
//    val Finish = new ButtonType("F_inish", ButtonBar.ButtonData.Finish)
    val Ok = new ButtonType("_OK")
    val No = new ButtonType("_No", ButtonBar.ButtonData.CancelClose)
//    val Open = new ButtonType("_Open")
//    val Update = new ButtonType("_Update", ButtonBar.ButtonData.CancelClose)
    val Yes = new ButtonType("_Yes")
  }

  import ButtonTypes._

  private[ui] def errorDialogIntercept[A](code: => Try[A]): Try[A] = code match {

    case FailQuietly =>
      logger.debug("Failing quietly")
      FailQuietly

    case result@Failure(ex) =>
      logger.error("Intercepted error.", ex)
      val msg: String = ex match {
        case ValidationException(message, _, errors) => s"$message\n\n" + errors.mkString("\n")
        case NonFatal(e) => messageOf(e)
        case e =>
          s"Fatal error - ${messageOf(e)}"
      }
      errorDialog(msg)
      ex match {
        case NonFatal(x) => // do nothing
        case _ =>
          logger.error("Fatal error, exiting application!", ex)
          Main.shutdown()
      }
      result

    case result@_ => result
  }

  private[this] def messageOf[T <: Throwable](e: T)(implicit tag: ru.TypeTag[T]): String = {
    if(e.getMessage == null) {
      val ct = ClassTag[T](e.getClass)
      ct.toString
    }
    else e.getMessage
  }

  private[ui] def errorDialog = (msg: String) => {
    new AppModalAlert(AlertType.Error) {
      buttonTypes = Seq(Close)
      headerText = s"$ApplicationName Error"
      contentText = msg
      dialogPane().setPrefSize(375, 200) // I tried setting the width/height values and the width didn't work.
    }.showAndWait()
  }

}
