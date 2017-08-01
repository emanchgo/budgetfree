package trove.core

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import trove.domain.AccountTypes._
import slick.jdbc.SQLiteProfile.api._

package object persist {

  private[persist] case class DbVersion(version: Int)

  val accountTypeMap: Map[AccountType,String] =
    Map(
      Equity -> "Equity",
      Income -> "Income",
      Expense -> "Expense",
      Asset -> "Asset",
      Cash -> "Cash",
      Bank -> "Bank",
      Security -> "Security",
      Lending -> "Lending",
      Liability -> "Liability",
      CreditCard -> "CreditCard",
      FixedRateLoan -> "FixedRateLoan",
      AdjustableRateLoan -> "AdjustableRateLoan"
    )

  implicit val accountTypeMapper: JdbcType[AccountType] with BaseTypedType[AccountType] =
    MappedColumnType.base[AccountType, String](
      accountTypeMap,
      accountTypeMap.map(_.swap)
  )

}
