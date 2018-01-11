/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
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

package trove.core.persist

import java.io.{File, IOException}
import java.nio.channels.FileLock

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import trove.constants._
import trove.exceptional.SystemException

import scala.util.{Failure, Success, Try}

class ProjectLockSpec extends FlatSpec with MockitoSugar with BeforeAndAfter with Matchers {

  import ProjectLock._

  val userHome: String = System.getProperty("user.home")
  val projectName = "unittest"
  val actualFile = new File(ProjectsHomeDir, constructLockfileName(projectName))
  val separator: String = File.separator
  val expectedDirectory = new File(s"$userHome$separator.trove${separator}projects")
  val expectedFilename = s"$projectName${ProjectLock.lockfileSuffix}"

  val UnitSuccess: Success[Unit] = Success(())

  after {
    if(actualFile.exists()) {
      actualFile.deleteOnExit()
    }
  }

  trait Fixture {

    var mockFilesCreated: List[(File, String)] = List.empty
    val mockFile: File = mock[File]

    var mockChannelsCreated: List[(File, String)] = List.empty
    val mockChannel: LockableChannel = mock[LockableChannel]

    val mockFileLock: FileLock = mock[FileLock]

    var shutdownHooksAdded: List[Thread] = List.empty
    var shutdownHooksRemoved: List[Thread] = List.empty
    var logErrorArgs: List[Try[Unit]] = List.empty

    val throwExceptionOnAddShutdownHook = false

    val projectLock: ProjectLock = new ProjectLock(projectName) with EnvironmentOps {

      def newFile(directory: File, filename: String): File = {
        mockFilesCreated = (directory, filename) +: mockFilesCreated
        mockFile
      }

      def newChannel(file: File, mode: String): LockableChannel = {
        mockChannelsCreated = (file, mode) +: mockChannelsCreated
        mockChannel
      }

      def addShutdownHook(thread: Thread): Unit = if(throwExceptionOnAddShutdownHook) {
        throw new Exception("doom")
      } else {
        shutdownHooksAdded = thread +: shutdownHooksAdded
      }

      def removeShutdownHook(thread: Thread): Unit = shutdownHooksRemoved = thread +: shutdownHooksRemoved

      def logIfError(result: Try[Unit]): Unit = logErrorArgs = result +: logErrorArgs
    }
  }

  "ProjectLock" should "allocate resources and add shutdown hook when lock is called" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    projectLock.lock() shouldBe UnitSuccess

    mockFilesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    mockChannelsCreated should contain theSameElementsAs List((mockFile, "rw"))

    verify(mockChannel, times(1)).tryLock()
    shutdownHooksAdded.size shouldBe 1

    verify(mockFileLock, never()).release()
    verify(mockFileLock, never()).close()
    verify(mockChannel, never()).close()
    verify(mockFile, never()).delete()
    shutdownHooksRemoved shouldBe empty

    logErrorArgs shouldBe empty
}

  it should "return SystemError and not allocate resources if it cannot acquire lock (tryLock returns null)" in new Fixture {
    val result: Try[Unit] = projectLock.lock()
    result shouldBe Failure(_: SystemException)

    mockFilesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    mockChannelsCreated should contain theSameElementsAs List((mockFile, "rw"))

    verify(mockChannel, times(1)).tryLock()
    shutdownHooksAdded shouldBe empty

    verify(mockFileLock, never()).release()
    verify(mockFileLock, never()).close()

    verify(mockChannel, times(1)).close()
    verify(mockFile, never()).delete()
    shutdownHooksRemoved shouldBe empty

    logErrorArgs.foreach(_ shouldBe UnitSuccess)
  }

  it should "return SystemError, close the channel, and not allocate resources if an exception is thrown while it is trying to acquire lock" in new Fixture {

    when(mockChannel.tryLock()).thenThrow(new IOException("doom"))
    val result: Try[Unit] = projectLock.lock()
    result shouldBe Failure(_: SystemException)

    mockFilesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    mockChannelsCreated should contain theSameElementsAs List((mockFile, "rw"))

    verify(mockChannel, times(1)).tryLock()
    shutdownHooksAdded shouldBe empty

    verify(mockFileLock, never()).release()
    verify(mockFileLock, never()).close()

    verify(mockChannel, times(1)).close()
    verify(mockFile, never()).delete()
    shutdownHooksRemoved shouldBe empty

    logErrorArgs.foreach(_ shouldBe UnitSuccess)
  }


  it should "return failure and release all resources if an exception is thrown while it is trying to add shutdown hook" in new Fixture {
    override val throwExceptionOnAddShutdownHook: Boolean = true
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    val result: Try[Unit] = projectLock.lock()
    result shouldBe Failure(_: SystemException)

    mockFilesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    mockChannelsCreated should contain theSameElementsAs List((mockFile, "rw"))

    verify(mockChannel, times(1)).tryLock()
    shutdownHooksAdded shouldBe empty

    verify(mockFileLock, times(1)).release()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    logErrorArgs.foreach(_ shouldBe UnitSuccess)
  }


  it should "release resources, remove shutdown hook, and delete file when release is called" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    projectLock.lock() shouldBe UnitSuccess

    projectLock.release()
    verify(mockFileLock, times(1)).release()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    logErrorArgs.foreach(_ shouldBe UnitSuccess)
  }

  it should "cleanup all other resources, return failure, and log when exception is thrown when releasing lock" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    projectLock.lock() shouldBe UnitSuccess

    projectLock.release()
    doThrow(new IOException("doom")).when(mockFileLock).release()
    verify(mockFileLock, times(1)).release()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    projectLock.release()

    logErrorArgs.foreach(_ shouldBe UnitSuccess)
  }

  it should "cleanup all other resources, return failure, and log when exception is thrown when closing channel" in new Fixture {
    fail("not yet implemented")
  }

  it should "cleanup all other resources, return success, and log when exception is thrown deleting file" in new Fixture {
    fail("not yet implemented")
  }

  it should "log error after previously performing other expected cleanup if exception is thrown removing shutdown hook" in new Fixture {
    fail("not yet implemented")
  }

  it should "not log error when lock owner calls release and there is an exception" in new Fixture {
    fail("not yet implemented")
  }

  it should "release resources, remove shutdown hook, and delete file when shutdown hook is called" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    projectLock.lock() shouldBe UnitSuccess

    shutdownHooksAdded.size shouldBe 1
    shutdownHooksAdded.head.run()

    verify(mockFileLock, times(1)).release()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    logErrorArgs shouldBe List(UnitSuccess, UnitSuccess, UnitSuccess, UnitSuccess)
  }

  it should "log error when shutdown hook execution experiences an error" in new Fixture {
    fail("not yet implemented")
  }
}
