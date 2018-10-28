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

import java.io.File

import javax.sql.DataSource
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import slick.dbio.{DBIOAction, NoStream}
import slick.util.AsyncExecutor
import trove.constants.ProjectsHomeDir
import trove.core.infrastructure.persist.lock.ProjectLock
import trove.core.infrastructure.persist.schema.Tables

import scala.concurrent.Future
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

class ProjectPersistenceServiceSpec extends FlatSpec with Matchers with MockitoSugar {
  import ProjectPersistenceService._

  trait ProjectDirFixture {
    val tempDir: File = mock[File]
    when(tempDir.isDirectory).thenReturn(true)
    when(tempDir.listFiles()).thenReturn(Array.empty[File])
    val projectService: ProjectServiceImpl = new ProjectServiceImpl(tempDir) with MockPersistence

    def mockFile(name: String, directory: Boolean = false): File = {
      require(name != null)
      val file = mock[File]
      when(file.isFile).thenReturn(!directory)
      when(file.getName).thenReturn(name)
      file
    }
  }

  trait IgnoredContentsFixture extends ProjectDirFixture {
    val subdir: File = mockFile("subdir", directory = true)
    val lockFile: File = mockFile(s"junk.${ProjectLock.LockfileSuffix}")
    val dotFile: File = mockFile(".dotfile")
    when(tempDir.listFiles()).thenReturn(Array(subdir, lockFile, dotFile))
  }

  trait NormalProjectsFixture extends ProjectDirFixture {
    import ProjectPersistenceService._

    val abc: File = mockFile(s"abc$DbFilenameSuffix")
    val `def`: File = mockFile(s"def$DbFilenameSuffix")
    val ghi: File = mockFile(s"ghi$DbFilenameSuffix")
    when(tempDir.listFiles()).thenReturn(Array(abc, `def`, ghi))
  }

  trait MockPersistence extends PersistenceOps {
    import slick.jdbc.SQLiteProfile.backend._

    val mockDb = mock[DatabaseDef]

    override def newProjectLock(projectsHomeDir: File, projectName: String): ProjectLock = {
      val lock = mock[ProjectLock]
      when(lock.lock()).thenReturn(Success({}))
      lock
    }

    override def createDbFile(directory: File, filename: String): File = mock[File]

    override def forDataSource(
      ds: DataSource,
      maxConnections: Option[Int],
      executor: AsyncExecutor,
      keepAliveConnection: Boolean): DatabaseDef = mockDb

    override def runDbIOAction[R: TypeTag](a: DBIOAction[R,NoStream,Nothing])(db: DatabaseDef) : Future[R] = {
      require(db == mockDb)
      typeOf[R] match {
        case r if r =:= typeOf[Unit] =>
          Future.successful({}).asInstanceOf[Future[R]]
        case r if r =:= typeOf[Seq[Tables.Version#TableElementType]] =>
          Future.successful(Seq(Tables.CurrentDbVersion)).asInstanceOf[Future[R]]
        case _ =>
          Future.failed[R](new RuntimeException(s"Unknown type: ${typeOf[R]}"))
      }
    }

  }

  "Trove persist persistence service" should "utilize persist home dir" in {
    ProjectPersistenceService().asInstanceOf[ProjectServiceImpl].projectsHomeDir shouldBe ProjectsHomeDir
  }

  it should "add shutdown hook" in {
    val shutdownHook = ProjectPersistenceService().asInstanceOf[ShutdownHook].shutdownHook
    Runtime.getRuntime.removeShutdownHook(shutdownHook) shouldBe true
  }

  "listProjectNames" should "return nothing if the persist directory is empty" in new ProjectDirFixture {
    projectService.listProjects shouldBe Success(Seq.empty)
  }

  it should "return nothing if the persist directory contains only ignored files" in new IgnoredContentsFixture {
    projectService.listProjects shouldBe Success(Seq.empty)
  }

  it should "strip the filename extension from valid persist files" in new NormalProjectsFixture {
    val result: Try[Seq[String]] = projectService.listProjects
    result.isFailure shouldBe false
    val projectNames: Seq[String] = result.get
    projectNames should not be empty
    projectNames.foreach(_.endsWith(DbFilenameSuffix) shouldBe false)
  }

  it should "return a sorted list of persist names with filename extensions stripped" in new NormalProjectsFixture {
    val result: Try[Seq[String]] = projectService.listProjects
    result.isFailure shouldBe false
    result.get shouldBe Seq("abc", "def", "ghi")
  }

  it should "return a failure if an exception is thrown while listing files" in {
    val file = mock[File]
    when(file.isDirectory).thenReturn(true)
    val ex = new RuntimeException("doom")
    when(file.listFiles).thenThrow(ex)
    val projectService = new ProjectServiceImpl(file) with MockPersistence
    projectService.listProjects match {
      case Failure(e) => e shouldBe ex
      case somethingElse => fail(s"Unexpected result: $somethingElse")

    }
  }

  "open" should "open an existing persist and lock it" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    projectNames.isSuccess shouldBe true
    val projectName: String = projectNames.get.head
    projectService.open(projectName) match {
      case Success(p) =>
        p shouldBe a [ProjectImpl]
        val project = p.asInstanceOf[ProjectImpl]
        project.name shouldBe projectName
        project.db should not be null
        verify(project.lock, times(1)).lock()
        verify(project.lock, never()).release()
      case somethingElse => fail(s"Wrong result when opening persist: $somethingElse")
    }
  }
/*
Persistence service
===================

it should "open an existing persist without creating database or tables"
it should "create a new persist with initial tables and lock the persist"
it should "populate the version number table when creating a new persist"
it should "open the database with all the right settings"
it should "set the current persist upon successful persist opening"

it should "fail with a SystemError if unable to obtain persist lock"
it should "fail with a PersistenceError and not lock a persist if another persist is already open"
it should "fail with a PersistenceError and clean up persist lock if unable to open database"
it should "fail with a PersistenceError if the wrong database version exists and clean up the persist lock"
it should "fail with a PersistenceError if there are too many rows in the database version table and clean up the persist lock"

"closeCurrentProject" should "clear the current persist upon successful persist closing"
it should "return success if there is no open persist"
it should "close the database upon successful persist closing"
it should "release the persist lock upon successful persist closing"
it should "remove the shutdown hook upon successful persist closing"
it should "fail with a PersistenceError if the database cannot be closed"
it should "fail with a PersistenceError if it cannot release the persist lock"
it should "fail with a PersistenceError if it cannot remove the shutdown hook"

"shutdown hook" should "close the database and release the persist lock if invoked"
it should "not try to remove itself from the jvm shutdown hooks"
*/
}
