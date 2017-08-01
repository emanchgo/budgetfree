package trove.core.persist

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait DbConfiguration {

  lazy val config: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("db")
}
