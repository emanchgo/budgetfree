package trove.core.persist

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.SQLiteProfile.api._
import trove.models.{Account, AccountType}

import scala.util.Try

object Tables {

  def enumValueMapper(enum: Enumeration): JdbcType[enum.Value] with BaseTypedType[enum.Value] =
    MappedColumnType.base[enum.Value, String](
      _.toString,
      s => Try(enum.withName(s)).getOrElse(throw new IllegalArgumentException(
        s"enumeration $s doesn't exist $enum [${enum.values.mkString(",")}]"))
  )

  class Accounts(tag: Tag) extends Table[Account](tag, "ACCOUNTS") {
    import AccountType._

    // Auto Increment the id primary key column
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def accountType = column[AccountType]("ACCOUNT_TYPE")            // can't be null
    def name = column[String]("ACCOUNT_NAME")                   // can't be null
    def isPlaceHolder = column[Boolean]("IS_PLACEHOLDER")       // can't be null
    def description = column[String]("ACCOUNT_DESCRIPTION")
    def parentAccountId = column[Int]("PARENT_ACCOUNT_ID")

    implicit val accountTypeMapper: JdbcType[AccountType.Value] with BaseTypedType[AccountType.Value] = enumValueMapper(AccountType)

    // column names to/from an Account
    def * = (id.?, accountType, name, isPlaceHolder, description.?, parentAccountId.?) <> (Account.tupled, Account.unapply)
  }
}
