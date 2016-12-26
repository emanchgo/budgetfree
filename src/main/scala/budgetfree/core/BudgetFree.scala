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
