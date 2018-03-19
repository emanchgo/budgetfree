package trove.models

import trove.models.AccountType.AccountType

case class Account(id: Option[Int],
                   accountType: AccountType,
                   name: String,
                   isPlaceholder: Boolean = false,
                   description: Option[String] = None,
                   parentAccountId: Option[Int] = None)
