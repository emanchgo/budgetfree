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


package budgetfree.core

import budgetfree.exceptional.ValidationError
import budgetfree.util.AppSingleInstance

import scala.util.{Success, Try}

object BudgetFree {

  def startup(): Try[Unit] = {

    if (AppSingleInstance.verify) {
      Success(Unit)
    }
    else {
      ValidationError("There is already an instance of BudgetFree running.")
    }
  }

  def shutdown(): Try[Unit] = Try {

  }
}
