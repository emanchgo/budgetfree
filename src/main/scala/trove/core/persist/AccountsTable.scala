package trove.core.persist

import slick.lifted.{Index, ProvenShape}
import trove.domain.Account
import trove.domain.AccountTypes.AccountType

trait AccountsTable { this: Db =>
  import config.profile.api._


  private[persist] class Accounts(tag: Tag) extends Table[Account](tag, "accounts") {

    // Columns
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def accountType: Rep[AccountType] = column[AccountType]("account_type")
    def name: Rep[String] = column[String]("account_name")
    def placeholder: Rep[Boolean] = column[Boolean]("is_placeholder")
    def description: Rep[String] = column[String]("description")
    def parentAccountId: Rep[Int] = column[Int]("parent_account_id")


    // Foreign Key
    // (None)
/*
    // ForeignKey
    def userId = column[Int]("USER_ID")
    def userFk = foreignKey("USER_FK", userId, users)
      (_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)
*/

    // Indexes
    def nameIndex: Index = index("account_name_idx", name, unique=true)

    // Select
    def * : ProvenShape[Account] =
      (id, accountType, name, placeholder, description.?, parentAccountId.?) <> (Account.tupled, Account.unapply)

  }

  val accounts: TableQuery[Accounts] = TableQuery[Accounts]

}
