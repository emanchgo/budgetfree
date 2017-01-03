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

package budgetfree.ui
import budgetfree.constants._
import budgetfree.core.BudgetFree
import budgetfree.ui.ButtonTypes.{Cancel, Open}
import grizzled.slf4j.Logging

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
  headerText = "Open BudgetFree Project"
  graphic = ApplicationIconImageView
  dialogPane().buttonTypes = Seq(Cancel, Open)

  private[this] val projectNames = BudgetFree.listProjectNames
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
