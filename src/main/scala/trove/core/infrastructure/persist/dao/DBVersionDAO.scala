/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2021 Eric John Fredericks.
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

package trove.core.infrastructure.persist.dao

import slick.jdbc.SQLiteProfile.api._
import slick.jdbc.SQLiteProfile.backend._
import trove.core.infrastructure.persist.DBVersion
import trove.core.infrastructure.persist.schema.Tables
import trove.exceptional.PersistenceError

import scala.util.{Success, Try}

private[persist] trait DBVersionDAO extends DbRunOp[DBVersion] {
  def get: Try[DBVersion]
}

private[persist] object DBVersionDAO {
  def apply(dbDef: DatabaseDef): DBVersionDAO = new DBVersionDAOImpl with LiveDbRunOp[DBVersion] {
    val db: DatabaseDef = dbDef
  }
}

private[persist] abstract class DBVersionDAOImpl extends DBVersionDAO with DbExec[DBVersion] {
  override def get: Try[DBVersion] = {
    exec(Tables.version.result).flatMap {
        case rows: Seq[DBVersion] if rows.length == 1 && rows.head == Tables.CurrentDbVersion =>
          Success(rows.head)
        case rows: Seq[DBVersion] if rows.length == 1 =>
          PersistenceError(s"Invalid database version: ${rows.head.id}")
        case rows: Seq[DBVersion] =>
          PersistenceError(s"Incorrect number of rows in the VERSION table: found ${rows.size} rows")
      }
    }
}