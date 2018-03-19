package trove.models

object AccountType extends Enumeration {
  type AccountType = Value
  val Equity: AccountType             = Value("Equity")
  val Income: AccountType             = Value("Income")
  val Expense: AccountType            = Value("Expense")
  val Asset: AccountType              = Value("Asset")
  val Cash: AccountType               = Value("Cash")
  val Bank: AccountType               = Value("Bank")
  val Security: AccountType           = Value("Security")
  val Lending: AccountType            = Value("Lending") // for loans user makes to others
  val Liability: AccountType          = Value("Liability")
  val CreditCard: AccountType         = Value("CreditCard")
  val FixedRateLoan: AccountType      = Value("FixedRateLoan")
  val AdjustableRateLoan: AccountType = Value("AdjustableRateLoan")
}
