package trove.core.persist

import slick.lifted.TableQuery

private[persist] object Tables {
  val accounts: TableQuery[Accounts] = TableQuery[Accounts]
}
