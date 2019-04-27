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

package trove

import grizzled.slf4j.Logging
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{ButtonBar, ButtonType, Label}
import scalafx.scene.image.Image
import scalafx.scene.paint.Color
import trove.constants._
import trove.exceptional.{FailQuietly, ValidationError}
import trove.ui.fxext.AppModalAlert

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

package object ui extends Logging {

  private[this] val ApplicationIconPath = "gold-pot-96.png"
  private[ui] val ApplicationIconImage64 = getImage(ApplicationIconPath, 64)
  private[ui] val ApplicationIconImage96 = getImage(ApplicationIconPath, 96)

  private[ui] val ActiveProjectTabImageSize = 40
  private[ui] val ActiveProjectTabHeight = ActiveProjectTabImageSize + 8

  private[ui] def getImage(path: String, size: Int): Image = {
    val url = getClass.getClassLoader.getResource(path)
    new Image(url.toExternalForm, size, size, true, true)
  }

  private[this] val ThirdPartyLicenseTextPath = "THIRD_PARTY_LICENSING"
  private[ui] val ThirdPartyLicenseUrlTextUrl = getClass.getClassLoader.getResource(ThirdPartyLicenseTextPath)

  def blankLabel = Label(" ")

  private[ui] object TroveColor {
    val PenguinPurple: Color = Color.rgb(66,0,66)
  }

  private[ui] object ButtonTypes {
//    val Add = new ButtonType("_Add", ButtonBar.ButtonData.OKDone)
    val Cancel = new ButtonType("_Cancel", ButtonBar.ButtonData.CancelClose)
    val Create = new ButtonType("C_reate", ButtonBar.ButtonData.OKDone)
    val Close = new ButtonType("_Close", ButtonBar.ButtonData.OKDone)
//    val Finish = new ButtonType("F_inish", ButtonBar.ButtonData.Finish)
    val Ok = new ButtonType("_OK", ButtonBar.ButtonData.OKDone)
    val No = new ButtonType("_No", ButtonBar.ButtonData.CancelClose)
    val Open = new ButtonType("_Open", ButtonBar.ButtonData.OKDone)
//    val Update = new ButtonType("_Update", ButtonBar.ButtonData.CancelClose)
    val Yes = new ButtonType("_Yes", ButtonBar.ButtonData.OKDone)
  }

  import ButtonTypes._

  private[ui] def promptUserWithError[A](exec: => Try[A]): Try[A] = exec match {

    case FailQuietly =>
      logger.debug("Failing quietly")
      FailQuietly

    case result@Failure(ex) =>
      logger.error("Intercepted error.", ex)
      val msg: String = result match {
        case ValidationError(message, _, errors) => s"$message\n\n" + errors.mkString("\n")
        case Failure(NonFatal(e)) => messageOf(e)
        case e =>
          s"Fatal error - ${messageOf(ex)}"
      }
      errorDialog(msg)
      ex match {
        case NonFatal(e) =>
          logger.error("Error in application", e)
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

  private[ui] def errorDialog: String => Option[ButtonType] = (msg: String) => {
    new AppModalAlert(AlertType.Error) {
      buttonTypes = Seq(Close)
      headerText = s"$ApplicationName Error"
      contentText = msg
      dialogPane().setPrefSize(375, 200) // I tried setting the width/height values and the width didn't work.
    }.showAndWait()
  }

}
