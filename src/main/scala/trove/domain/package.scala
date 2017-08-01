package trove

import trove.domain.AccountTypes.AccountType

package object domain {

  object AccountTypes {
    sealed trait AccountType
    case object Equity extends AccountType
    case object Income extends AccountType
    case object Expense extends AccountType
    case object Asset extends AccountType
    case object Cash extends AccountType
    case object Bank extends AccountType
    case object Security extends AccountType
    case object Lending extends AccountType // for loans user makes to others
    case object Liability extends AccountType
    case object CreditCard extends AccountType
    case object FixedRateLoan extends AccountType
    case object AdjustableRateLoan extends AccountType
  }

  case class Account(id: Int, accountType: AccountType, name: String, placeholder: Boolean = false, description: Option[String] = None,
                     parentAccountId: Option[Int] = None)

}