package trove.core.infrastructure.persist.schema

import slick.ast.BaseTypedType
import slick.dbio.Effect
import slick.jdbc.JdbcType
import slick.jdbc.SQLiteProfile.api._
import slick.lifted.ProvenShape
import slick.sql.FixedSqlStreamingAction
import trove.core.infrastructure.persist.DBVersion
import trove.models.{Account, AccountType}

import scala.util.Try

private[persist] object Tables {

  // N.B. Change this value when modifying the structure of the database!
  // TODO: Work out db evolution!
  val CurrentDbVersion = DBVersion(0)

  // VERSION TABLE
  class Version(tag: Tag) extends Table[DBVersion](tag, "VERSION") {
    def id: Rep[Long] = column[Long]("ID", O.PrimaryKey)

    def * : ProvenShape[DBVersion] = id <> (DBVersion.apply, DBVersion.unapply)
  }
  val version: TableQuery[Version] = TableQuery[Version]

  // ACCOUNTS TABLE
  class Accounts(tag: Tag) extends Table[Account](tag, "ACCOUNTS") {
    import AccountType._

    def id: Rep[Int] = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def accountType: Rep[AccountType] = column[AccountType]("ACCOUNT_TYPE")       // can't be null
    def name: Rep[String] = column[String]("ACCOUNT_NAME")                   // can't be null
    def code: Rep[String] = column[String]("ACCOUNT_CODE")
    def isPlaceHolder: Rep[Boolean] = column[Boolean]("IS_PLACEHOLDER")       // can't be null
    def description: Rep[String] = column[String]("ACCOUNT_DESCRIPTION")
    def parentAccountId: Rep[Int] = column[Int]("PARENT_ACCOUNT_ID")

    def * : ProvenShape[Account] =
      (id.?, accountType, name, code.?, isPlaceHolder, description.?, parentAccountId.?) <> (Account.tupled, Account.unapply)
  }
  val accounts: TableQuery[Accounts] = TableQuery[Accounts]

  // Some things needed - slick boilerplate
  def enumValueMapper(enum: Enumeration): JdbcType[enum.Value] with BaseTypedType[enum.Value] =
    MappedColumnType.base[enum.Value, String](
      _.toString,
      s =>
        Try(enum.withName(s)).getOrElse(throw new IllegalArgumentException(
        s"enumeration $s doesn't exist $enum [${enum.values.mkString(",")}]"))
    )

  implicit val accountTypeMapper: JdbcType[AccountType.Value] with BaseTypedType[AccountType.Value] = enumValueMapper(AccountType)

  // Setup action
  lazy val setupAction: DBIO[Unit] = DBIO.seq(
    version.schema.create,
    accounts.schema.create,

    version += CurrentDbVersion
  )

  lazy val versionQuery: FixedSqlStreamingAction[Seq[DBVersion], DBVersion, Effect.Read] = Tables.version.result
}
