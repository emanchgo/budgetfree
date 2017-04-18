/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
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

package trove.ui
import grizzled.slf4j.Logging
import trove.constants._
import trove.core.Trove
import trove.ui.ButtonTypes.{Cancel, Open}

import scala.util.Try
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.scene.control._
import scalafx.scene.layout.BorderPane

private[ui] object ProjectChooser {
  val NewProjectString = "[ New Project ]"
  def apply(): Try[ProjectChooser] = errorDialogIntercept(Try(new ProjectChooser()))
}

private[ui] class ProjectChooser private extends Dialog[String] with Logging {

  import ProjectChooser._

  title = ApplicationName
  headerText = "Open Trove Project"
  graphic = ApplicationIconImageView
  dialogPane().buttonTypes = Seq(Cancel, Open)

  private[this] val projectNames = Trove.listProjectNames
  private[this] val choiceStrings = NewProjectString +: projectNames.toList

  private[this] val projectChoices = new ComboBox[String] {
    items() = ObservableBuffer(choiceStrings: _*)
    selectionModel().selectFirst()
  }

  dialogPane().content = new BorderPane {
    center = projectChoices
  }

  resultConverter = dialogButton => {
    if(dialogButton == Open) {
      val selected: String = projectChoices.selectionModel().getSelectedItem
      selected match {
        case NewProjectString => "" // Empty String means create a new project!
        case _ => selected
      }
    }
    else {
      null
    }
  }

  Platform.runLater(projectChoices.requestFocus())
}
