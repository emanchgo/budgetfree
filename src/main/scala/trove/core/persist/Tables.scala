/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2018 Eric John Fredericks.
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

package trove.core.persist

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.SQLiteProfile.api._
import trove.models.{Account, AccountType}

import scala.util.Try

private[persist] object Tables {

  class Accounts(tag: Tag) extends Table[Account](tag, "ACCOUNTS") {
    import AccountType._

    // Auto Increment the id primary key column
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def accountType = column[AccountType]("ACCOUNT_TYPE")       // can't be null
    def name = column[String]("ACCOUNT_NAME")                   // can't be null
    def isPlaceHolder = column[Boolean]("IS_PLACEHOLDER")       // can't be null
    def description = column[String]("ACCOUNT_DESCRIPTION")
    def parentAccountId = column[Int]("PARENT_ACCOUNT_ID")

    implicit val accountTypeMapper: JdbcType[AccountType.Value] with BaseTypedType[AccountType.Value] = enumValueMapper(AccountType)

    // column names to/from an Account
    def * = (id.?, accountType, name, isPlaceHolder, description.?, parentAccountId.?) <> (Account.tupled, Account.unapply)
  }

  val accounts: TableQuery[Accounts] = TableQuery[Accounts]

  val setupAction: DBIO[Unit] = DBIO.seq(
    accounts.schema.create
  )

  def enumValueMapper(enum: Enumeration): JdbcType[enum.Value] with BaseTypedType[enum.Value] =
    MappedColumnType.base[enum.Value, String](
      _.toString,
      s => Try(enum.withName(s)).getOrElse(throw new IllegalArgumentException(
        s"enumeration $s doesn't exist $enum [${enum.values.mkString(",")}]"))
    )

}
