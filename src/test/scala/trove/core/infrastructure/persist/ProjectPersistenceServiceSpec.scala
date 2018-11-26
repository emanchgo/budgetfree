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
import java.sql.SQLException

import javax.sql.DataSource
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.DriverDataSource
import slick.util.ClassLoaderUtil
import trove.constants.ProjectsHomeDir
import trove.core.infrastructure.persist.lock.ProjectLock
import trove.core.infrastructure.persist.schema.Tables
import trove.exceptional.{PersistenceException, SystemError, SystemException}

import scala.concurrent.Future
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

class ProjectPersistenceServiceSpec extends FlatSpec with Matchers with MockitoSugar {
  import ProjectPersistenceService._

  trait ProjectDirFixture {
    import slick.jdbc.SQLiteProfile.backend._

    val mockDbFile: File = mock[File]
    when(mockDbFile.exists()).thenReturn(true)
    when(mockDbFile.getAbsolutePath).thenReturn("/foo/bar")
    var runDbIOActions: Seq[DBIOAction[_, NoStream, Nothing]] = Seq.empty
    var forDataSourceArgs: Seq[(DataSource, Int)] = Seq.empty

    val mockLock: ProjectLock = mock[ProjectLock]
    when(mockLock.lock()).thenReturn(Success((): Unit))

    val mockDb = mock[DatabaseDef]

    var mockDbError: Boolean = false
    val SqlException = new SQLException("Mock DB Error")

    var dbActionCount = 0
    var dbActionFailOn: Int = -1

    var dbVersionQueryResult: Seq[DBVersion] = Seq(Tables.CurrentDbVersion)

    trait MockPersistence extends PersistenceOps {


      override def newProjectLock(projectsHomeDir: File, projectName: String): ProjectLock =
        mockLock

      override def createDbFile(directory: File, filename: String): File =
        mockDbFile

      override def forDataSource(ds: DataSource, numWorkers: Int): DatabaseDef = {
        forDataSourceArgs :+= (ds, numWorkers)
        if(mockDbError) throw SqlException
        mockDb
      }

      override def runDBIOAction[R: TypeTag](a: DBIOAction[R,NoStream,Nothing])(db: DatabaseDef) : Future[R] = {
        runDbIOActions :+= a
        assume(db == mockDb)
        dbActionCount += 1
        if(dbActionFailOn == dbActionCount)
          Future.failed[R](new Exception(s"failed as expected: dbActionFailOn: $dbActionFailOn"))
        else {
          typeOf[R] match {
            case r if r =:= typeOf[Unit] =>
              Future.successful({}).asInstanceOf[Future[R]]
            case r if r =:= typeOf[Seq[Tables.Version#TableElementType]] =>
              Future.successful(dbVersionQueryResult).asInstanceOf[Future[R]]
            case _ =>
              Future.failed[R](new RuntimeException(s"Unknown type: ${typeOf[R]}"))
          }
        }
      }
    }

    val tempDir: File = mock[File]
    when(tempDir.isDirectory).thenReturn(true)
    when(tempDir.listFiles()).thenReturn(Array.empty[File])
    val projectService: ProjectPersistenceServiceImpl = new ProjectPersistenceServiceImpl(tempDir) with MockPersistence

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

  "Trove project persistence service" should "utilize project home dir" in {
    ProjectPersistenceService().asInstanceOf[ProjectPersistenceServiceImpl].projectsHomeDir shouldBe ProjectsHomeDir
  }

  it should "add shutdown hook" in {
    val shutdownHook = ProjectPersistenceService().asInstanceOf[HasShutdownHook].shutdownHook
    Runtime.getRuntime.removeShutdownHook(shutdownHook) shouldBe true
  }

  "listProjects" should "return nothing if the project directory is empty" in new ProjectDirFixture {
    projectService.listProjects shouldBe Success(Seq.empty)
  }

  it should "return nothing if the project directory contains only ignored files" in new IgnoredContentsFixture {
    projectService.listProjects shouldBe Success(Seq.empty)
  }

  it should "strip the filename extension from valid project files" in new NormalProjectsFixture {
    val result: Try[Seq[String]] = projectService.listProjects
    result.isFailure shouldBe false
    val projectNames: Seq[String] = result.get
    projectNames should not be empty
    projectNames.foreach(_.endsWith(DbFilenameSuffix) shouldBe false)
  }

  it should "return a sorted list of project names with filename extensions stripped" in new NormalProjectsFixture {
    val result: Try[Seq[String]] = projectService.listProjects
    result.isFailure shouldBe false
    result.get shouldBe Seq("abc", "def", "ghi")
  }

  it should "return a failure if an exception is thrown while listing files" in new ProjectDirFixture {
    val file: File = mock[File]
    when(file.isDirectory).thenReturn(true)
    val ex = new RuntimeException("doom")
    when(file.listFiles).thenThrow(ex)
    override val projectService = new ProjectPersistenceServiceImpl(file) with MockPersistence
    projectService.listProjects match {
      case Failure(e) =>
        e shouldBe ex
      case somethingElse =>
        fail(s"Unexpected result: $somethingElse")
    }
  }

  "initializeProject" should "open an existing project and lock it" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    projectNames.isSuccess shouldBe true
    val projectName: String = projectNames.get.head
    projectService.initializeProject(projectName) match {
      case result@Success(project) =>
        project.name shouldBe projectName
        project.db should not be null
        project.lock shouldBe mockLock
        verify(project.lock, times(1)).lock()
        verify(project.lock, never()).release()
        runDbIOActions should contain theSameElementsAs List(Tables.versionQuery)
        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        val dds = ds.asInstanceOf[DriverDataSource]
        dds.url shouldBe "jdbc:sqlite:/foo/bar"
        dds.user shouldBe null
        dds.password shouldBe null
        dds.properties shouldBe null
        dds.driverClassName shouldBe "org.sqlite.JDBC"
        dds.classLoader shouldBe ClassLoaderUtil.defaultClassLoader
        numWorkers shouldBe 1
        projectService.currentProject shouldBe result.toOption
        verifyNoMoreInteractions(mockDb, mockLock)
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }

  it should "create a new project with initial tables, create the version table, and lock the project" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    projectNames.isSuccess shouldBe true
    val allProjectNames: Seq[String] = projectNames.get
    val newProjectName = "foo"
    assume(!allProjectNames.contains(newProjectName)) // sanity check
    when(mockDbFile.exists()).thenReturn(false)
    projectService.initializeProject(newProjectName)  match {
      case result@Success(project) =>
        project.name shouldBe newProjectName
        project.db should not be null
        project.lock shouldBe mockLock
        verify(project.lock, times(1)).lock()
        verify(project.lock, never()).release()
        runDbIOActions should contain theSameElementsInOrderAs  List(Tables.setupAction, Tables.versionQuery)
        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        val dds = ds.asInstanceOf[DriverDataSource]
        dds.url shouldBe "jdbc:sqlite:/foo/bar"
        dds.user shouldBe null
        dds.password shouldBe null
        dds.properties shouldBe null
        dds.driverClassName shouldBe "org.sqlite.JDBC"
        dds.classLoader shouldBe ClassLoaderUtil.defaultClassLoader
        numWorkers shouldBe 1
        projectService.currentProject shouldBe result.toOption
        verifyNoMoreInteractions(mockDb, mockLock)
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }

  it should "fail with a SystemError if unable to obtain a project lock" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    projectNames.isSuccess shouldBe true
    val projectName: String = projectNames.get.head
    val ise = new IllegalStateException("lock error")
    when(mockLock.lock()).thenReturn(SystemError("doom", ise))
    projectService.initializeProject(projectName) match {
      case Failure(ex: SystemException) =>
        ex.cause shouldBe Some(ise)
        verify(mockLock, times(1)).lock()
        verify(mockLock, times(1)).release()
        verifyNoMoreInteractions(mockDb, mockLock)
        runDbIOActions shouldBe empty
        forDataSourceArgs shouldBe empty
        projectService.currentProject shouldBe None
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }

  it should "return a PersistenceError if a project is already open" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    projectNames.isSuccess shouldBe true
    val projectName: String = projectNames.get.head
    projectService.initializeProject(projectName) match {
      case Success(_) =>

        verify(mockLock, times(1)).lock()
        verify(mockLock, never()).release()

        val anotherProj = "foobarbaz"
        assume(!projectNames.get.contains(anotherProj))
        when(mockDbFile.exists()).thenReturn(false)

        projectService.initializeProject(anotherProj) match {
          case Failure(_: PersistenceException) =>
            // ok
          case somethingUnexpected =>
            fail(s"Wrong result when opening second project: $somethingUnexpected")
        }


        verifyNoMoreInteractions(mockDb, mockLock)
      case somethingElse =>
        fail(s"Wrong result when opening first project: $somethingElse")
    }
  }

  it should "fail with a PersistenceError and clean up project lock if unable to open database" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    projectNames.isSuccess shouldBe true
    val projectName: String = projectNames.get.head
    mockDbError = true // turn this on to throw exception when opening db
    projectService.initializeProject(projectName) match {
      case Failure(e: PersistenceException) =>
        verify(mockLock, times(1)).lock()
        verify(mockLock, times(1)).release()
        e.cause shouldBe Some(SqlException)
        verifyNoMoreInteractions(mockLock, mockDb)
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }

  it should "fail with a PersistenceError and clean up the project lock if there is a problem performing the DB version query for an existing project" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    val projectName: String = projectNames.get.head
    dbActionFailOn = 1
    projectService.initializeProject(projectName) match {
      case Failure(PersistenceException(_, cause)) =>
        verify(mockLock, times(1)).lock()
        verify(mockLock, times(1)).release()
        cause should not be empty
        cause.get.getMessage should endWith (s": $dbActionFailOn")
        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        numWorkers shouldBe 1
        runDbIOActions should have size 1
        runDbIOActions should contain theSameElementsAs List(Tables.versionQuery)
        verifyNoMoreInteractions(mockLock, mockDb)
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }


  it should "fail with a PersistenceError and clean up the project lock if there is a problem performing the DB version query for a new project" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects

    val anotherProj = "foobarbaz"
    assume(!projectNames.get.contains(anotherProj))
    when(mockDbFile.exists()).thenReturn(false)

    dbActionFailOn = 2
    projectService.initializeProject(anotherProj) match {
      case Failure(PersistenceException(_, cause)) =>
        verify(mockLock, times(1)).lock()
        verify(mockLock, times(1)).release()
        cause should not be empty
        cause.get.getMessage should endWith (s": $dbActionFailOn")

        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        numWorkers shouldBe 1

        runDbIOActions should have size 2
        runDbIOActions should contain theSameElementsAs List(Tables.setupAction, Tables.versionQuery)
        verifyNoMoreInteractions(mockLock, mockDb)
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }

  it should "fail with a PersistenceError and clean up the project lock if there is a problem setting up the tables for a new project" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects

    val anotherProj = "foobarbaz"
    assume(!projectNames.get.contains(anotherProj))
    when(mockDbFile.exists()).thenReturn(false)

    dbActionFailOn = 1
    projectService.initializeProject(anotherProj) match {
      case Failure(PersistenceException(_, cause)) =>
        verify(mockLock, times(1)).lock()
        verify(mockLock, times(1)).release()
        cause should not be empty
        cause.get.getMessage should endWith (s": $dbActionFailOn")

        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        numWorkers shouldBe 1

        runDbIOActions should have size 1
        runDbIOActions should contain theSameElementsAs List(Tables.setupAction)
        verifyNoMoreInteractions(mockLock, mockDb)

      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }

  it should "fail with a PersistenceError and clean up the project lock if the wrong database version exists" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    val projectName: String = projectNames.get.head

    val newDbVersion: Long = dbVersionQueryResult.head.id -1
    dbVersionQueryResult = Seq(dbVersionQueryResult.head.copy(id = newDbVersion))

    projectService.initializeProject(projectName) match {
      case Failure(PersistenceException(message, cause)) =>
        verify(mockLock, times(1)).lock()
        verify(mockLock, times(1)).release()

        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        numWorkers shouldBe 1

        runDbIOActions should have size 1
        runDbIOActions should contain theSameElementsAs List(Tables.versionQuery)
        verifyNoMoreInteractions(mockLock, mockDb)

        message should endWith (newDbVersion.toString)
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }

  it should "fail with a PersistenceError and clean up the project lock if there are too many rows in the database version table" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    val projectName: String = projectNames.get.head

    val newDbVersion: Long = dbVersionQueryResult.head.id -1
    dbVersionQueryResult = Seq(dbVersionQueryResult.head, dbVersionQueryResult.head.copy(id = newDbVersion))

    projectService.initializeProject(projectName) match {
      case Failure(PersistenceException(message, cause)) =>
        verify(mockLock, times(1)).lock()
        verify(mockLock, times(1)).release()

        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        numWorkers shouldBe 1

        runDbIOActions should have size 1
        runDbIOActions should contain theSameElementsAs List(Tables.versionQuery)
        verifyNoMoreInteractions(mockLock, mockDb)

        message should endWith (s"found ${dbVersionQueryResult.size} rows")
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }

  }

  "Public open method" should "open an existing project" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    projectNames.isSuccess shouldBe true
    val projectName: String = projectNames.get.head
    projectService.open(projectName) match {
      case result@Success(prj) =>
        prj.name shouldBe projectName
        prj shouldBe a [ProjectImpl]
        val project = prj.asInstanceOf[ProjectImpl]
        project.db should not be null
        project.lock shouldBe mockLock
        verify(project.lock, times(1)).lock()
        verify(project.lock, never()).release()
        runDbIOActions should contain theSameElementsAs List(Tables.versionQuery)
        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        val dds = ds.asInstanceOf[DriverDataSource]
        dds.url shouldBe "jdbc:sqlite:/foo/bar"
        dds.user shouldBe null
        dds.password shouldBe null
        dds.properties shouldBe null
        dds.driverClassName shouldBe "org.sqlite.JDBC"
        dds.classLoader shouldBe ClassLoaderUtil.defaultClassLoader
        numWorkers shouldBe 1
        projectService.currentProject shouldBe result.toOption
        verifyNoMoreInteractions(mockDb, mockLock)
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }

  }

  it should "create a new project performing the appropriate operations" in new NormalProjectsFixture {
    val projectNames: Try[Seq[String]] = projectService.listProjects
    projectNames.isSuccess shouldBe true
    val allProjectNames: Seq[String] = projectNames.get
    val newProjectName = "foo"
    assume(!allProjectNames.contains(newProjectName)) // sanity check
    when(mockDbFile.exists()).thenReturn(false)
    projectService.open(newProjectName)  match {
      case result@Success(prj) =>
        prj shouldBe a [ProjectImpl]
        val project = prj.asInstanceOf[ProjectImpl]
        prj.name shouldBe newProjectName
        project.db should not be null
        project.lock shouldBe mockLock
        verify(project.lock, times(1)).lock()
        verify(project.lock, never()).release()
        runDbIOActions should contain theSameElementsInOrderAs  List(Tables.setupAction, Tables.versionQuery)
        forDataSourceArgs should have size 1
        val (ds, numWorkers) = forDataSourceArgs.head
        ds shouldBe a [DriverDataSource]
        val dds = ds.asInstanceOf[DriverDataSource]
        dds.url shouldBe "jdbc:sqlite:/foo/bar"
        dds.user shouldBe null
        dds.password shouldBe null
        dds.properties shouldBe null
        dds.driverClassName shouldBe "org.sqlite.JDBC"
        dds.classLoader shouldBe ClassLoaderUtil.defaultClassLoader
        numWorkers shouldBe 1
        projectService.currentProject shouldBe result.toOption
        verifyNoMoreInteractions(mockDb, mockLock)
      case somethingElse =>
        fail(s"Wrong result when opening project: $somethingElse")
    }
  }

  /*
  Persistence service
  ===================


  "closeCurrentProject" should "clear the current project upon successful project closing"
  it should "return success if there is no open project"
  it should "close the database upon successful project closing"
  it should "release the project lock upon successful project closing"
  it should "remove the shutdown hook upon successful project closing"
  it should "fail with a PersistenceError if the database cannot be closed"
  it should "fail with a PersistenceError if it cannot release the project lock"
  it should "fail with a PersistenceError if it cannot remove the shutdown hook"

  "shutdown hook" should "close the database and release the project lock if invoked"
  it should "not try to remove itself from the jvm shutdown hooks"

  */

/*
  Test all these
  def open(projectName: String): Try[Project]
  def closeCurrentProject(): Try[Unit]

 */
}
