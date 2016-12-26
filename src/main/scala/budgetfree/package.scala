import java.io.File
import java.math.BigDecimal
import java.text.DecimalFormat

import grizzled.slf4j.Logging

package object budgetfree extends Logging {

  object constants {
    val ApplicationName = "BudgetFree"
    val ApplicationVersion = "0.1.0"

    val UserHomeDir = new File(System.getProperty("user.home"))
    val ApplicationHomeDir = new File(UserHomeDir, ".budgetfree")
    val ProjectsHomeDir = new File(ApplicationHomeDir, "projects")
    ProjectsHomeDir.mkdirs()
  }

  val monetaryValueFormatter = new DecimalFormat() {
    setMinimumFractionDigits(2)
    setMaximumFractionDigits(2)
  }
}