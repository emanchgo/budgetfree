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

package trove.core.infrastructure.project

import java.io.File

import javax.sql.DataSource
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.SQLiteProfile.backend._
import slick.util.AsyncExecutor

import scala.concurrent.Future
import scala.reflect.runtime.universe._

private[project] trait PersistenceOps {

  def newProjectLock(projectsHomeDir: File, projectName: String): ProjectLock
  def createDbFile(directory: File, filename: String): File
  def forDataSource(ds: DataSource, maxConnections: Option[Int], executor: AsyncExecutor, keepAliveConnection: Boolean): DatabaseDef

  def runDbIOAction[R: TypeTag](a: DBIOAction[R,NoStream,Nothing])(db: DatabaseDef) : Future[R]
}

private[project] trait LivePersistence extends PersistenceOps {

  override def newProjectLock(projectsHomeDir: File, projectName: String): ProjectLock = ProjectLock(projectsHomeDir, projectName)

  override def createDbFile(directory: File, filename: String): File = new File(directory, filename)

  override def forDataSource(ds: DataSource, maxConnections: Option[Int], executor: AsyncExecutor, keepAliveConnection: Boolean):
    DatabaseDef = {

    Database.forDataSource(
      ds = ds,
      maxConnections = maxConnections,
      executor = executor,
      keepAliveConnection = false
    )
  }

  override def runDbIOAction[R: TypeTag](a: DBIOAction[R,NoStream,Nothing])(db: DatabaseDef): Future[R] = db.run(a)

}
