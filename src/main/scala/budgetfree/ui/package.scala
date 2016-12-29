package budgetfree

import budgetfree.constants._
import budgetfree.exceptional.{FailQuietly, ValidationException}
import grizzled.slf4j.Logging

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
import scala.util.control.NonFatal
import scala.util.{Failure, Try}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonBar, ButtonType, Label}
import scalafx.scene.image.{Image, ImageView}

package object ui extends Logging {

  private[this] val ApplicationIconPath = "MoneyTransfer-100.png"
  private[ui] val ApplicationIconUrl = getClass.getClassLoader.getResource(ApplicationIconPath)
  private[ui] val ApplicationIconImage = new Image(ApplicationIconUrl.toExternalForm, 64, 64, true, true)
  private[ui] def ApplicationIconImageView = new ImageView(ApplicationIconImage)

  private[this] val ThirdPartyLicenseTextPath = "THIRD_PARTY_LICENSING"
  private[ui] val ThirdPartyLicenseUrlTextUri = getClass.getClassLoader.getResource(ThirdPartyLicenseTextPath).toURI

  def blankLabel = Label(" ")

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
    new Alert(AlertType.Error) {
      initOwner(Main.stage)
      title = ApplicationName
      buttonTypes = Seq(Close)
      headerText = s"$ApplicationName Error"
      contentText = msg
      dialogPane().setPrefSize(375, 200) // I tried setting the width/height values and the width didn't work.
      resizable = true
    }.showAndWait()
  }

}
