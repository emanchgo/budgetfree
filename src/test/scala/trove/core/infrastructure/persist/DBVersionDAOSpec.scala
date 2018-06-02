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

package trove.core.infrastructure.persist

import org.scalatest.{FlatSpec, Matchers}
import trove.core.infrastructure.persist.Tables._
import trove.exceptional.PersistenceError

import scala.util.{Failure, Success, Try}

class DBVersionDAOSpec extends FlatSpec with Matchers {

  // For when the DB successfully returns some valid result (could be empty), default is a single row
  trait DbSuccessFixture {

    def _produce: Try[Seq[DBVersion]] = Try(Seq(Tables.CurrentDbVersion))

    val dao: DBVersionDAOImpl with DbRunOp[DBVersion] = new DBVersionDAOImpl with TestDbRunOp[DBVersion] {
      def produce: Try[Seq[DBVersion]] = _produce
    }
  }

  // For when the DB itself throws some error
  trait DbFailureFixture {
    val dbEx = new RuntimeException("doom")
    val dao: DBVersionDAOImpl with DbRunOp[DBVersion] = new DBVersionDAOImpl with TestDbRunOp[DBVersion] {
      def produce: Try[Seq[DBVersion]] = Failure(dbEx)
    }
  }

  "DBVersionDAO" should "return a DBVersion" in new DbSuccessFixture {
    dao.get shouldBe Success(CurrentDbVersion)
  }

  it should "return a failure" in new DbFailureFixture {
    dao.get match {
      case Success(_) => fail("Should have failed")
      case PersistenceError(e) if Option(e.getCause).map(_.getMessage).getOrElse("") == "doom" => // success
      case Failure(e) => fail("Wrong error: ", e)
    }
  }

  it should "return a failure when no rows returned" in new DbSuccessFixture {
    override def _produce: Try[Seq[DBVersion]] = Try(Seq.empty)

    dao.get match {
      case Success(_) => fail("Should have failed")
      case PersistenceError(_) => // success
      case Failure(e) => fail("Wrong error: ", e)
    }
  }

  it should "return a failure when too many rows returned" in new DbSuccessFixture {
    override def _produce: Try[Seq[DBVersion]] = Try(Seq(Tables.CurrentDbVersion, Tables.CurrentDbVersion))

    dao.get match {
      case Success(_) => fail("Should have failed")
      case PersistenceError(_) => // success
      case Failure(e) => fail("Wrong error: ", e)
    }
  }

}
