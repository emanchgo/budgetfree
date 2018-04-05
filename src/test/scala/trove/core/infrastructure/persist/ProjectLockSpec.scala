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

import java.io.{File, IOException, RandomAccessFile}
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

    var filesCreated: List[(File, String)] = List.empty
    val mockFile: File = mock[File]

    var randomAccessFilesCreated: List[File] = List.empty
    val mockRandomAccessFile: RandomAccessFile = mock[RandomAccessFile]

    var channelsCreated: List[RandomAccessFile] = List.empty
    val mockChannel: LockableChannel = mock[LockableChannel]

    val mockFileLock: FileLock = mock[FileLock]

    var shutdownHooksAdded: List[Thread] = List.empty
    var shutdownHooksRemoved: List[Thread] = List.empty
    var logErrorArgs: List[Try[Unit]] = List.empty

    val throwExceptionOnCreateRandomAccessFile = false
    val throwExceptionOnAddShutdownHook = false
    val throwExceptionOnRemoveShutdownHook = false

    val projectLock: ProjectLock = new ProjectLock(projectName) with EnvironmentOps {

      override def newFile(directory: File, filename: String): File = {
        filesCreated = (directory, filename) +: filesCreated
        mockFile
      }

      override def newRandomAccessFile(file: File): RandomAccessFile = if(throwExceptionOnCreateRandomAccessFile) {
        throw new RuntimeException("doom")
      }
      else {
        randomAccessFilesCreated = file +: randomAccessFilesCreated
        mockRandomAccessFile
      }

      override def newChannel(raf: RandomAccessFile): LockableChannel = {
        channelsCreated = raf +: channelsCreated
        mockChannel
      }

      override def addShutdownHook(thread: Thread): Unit = if(throwExceptionOnAddShutdownHook) {
        throw new Exception("doom")
      } else {
        shutdownHooksAdded = thread +: shutdownHooksAdded
      }

      override def removeShutdownHook(thread: Thread): Unit = if(throwExceptionOnRemoveShutdownHook) {
        throw new Exception("doom")
      } else {
        shutdownHooksRemoved = thread +: shutdownHooksRemoved
      }

      override def logIfError(result: Try[Unit]): Unit = logErrorArgs = result +: logErrorArgs
    }
  }

  "ProjectLock" should "allocate resources and add shutdown hook when lock is called" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated should contain theSameElementsAs List(mockFile)
    channelsCreated should contain theSameElementsAs List(mockRandomAccessFile)

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
    result match {
      case Failure(_: SystemException) => // no op
      case _ => fail("wrong result when locking")
    }

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated should contain theSameElementsAs List(mockFile)
    channelsCreated should contain theSameElementsAs List(mockRandomAccessFile)

    verify(mockChannel, times(1)).tryLock()
    shutdownHooksAdded shouldBe empty

    verify(mockFileLock, never()).release()
    verify(mockFileLock, never()).close()

    verify(mockChannel, times(1)).close()
    verify(mockFile, never()).delete()
    shutdownHooksRemoved shouldBe empty

    logErrorArgs should not be empty
    val failures: List[Try[Unit]] = logErrorArgs.filter(_.isFailure)
    failures shouldBe empty
  }

  it should "return SystemError and not allocate resources if exception is thrown while creating RandomAccessFIle" in new Fixture {
    override val throwExceptionOnCreateRandomAccessFile: Boolean = true
    val result: Try[Unit] = projectLock.lock()
    result match {
      case Failure(_: SystemException) => // no op
      case _ => fail("wrong result when locking")
    }

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated shouldBe empty
    channelsCreated shouldBe empty

    verify(mockChannel, never()).tryLock()
    shutdownHooksAdded shouldBe empty

    verify(mockFileLock, never()).release()
    verify(mockFileLock, never()).close()

    verify(mockChannel, never()).close()
    verify(mockFile, never()).delete()
    shutdownHooksRemoved shouldBe empty

    logErrorArgs shouldBe empty
  }

  it should "return SystemError, close the channel, and not allocate resources if an exception is thrown while it is trying to acquire lock" in new Fixture {

    when(mockChannel.tryLock()).thenThrow(new IOException("doom"))
    val result: Try[Unit] = projectLock.lock()
    result match {
      case Failure(_: SystemException) => // no op
      case _ => fail("wrong result when locking")
    }

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated should contain theSameElementsAs List(mockFile)
    channelsCreated should contain theSameElementsAs List(mockRandomAccessFile)

    verify(mockChannel, times(1)).tryLock()
    shutdownHooksAdded shouldBe empty

    verify(mockFileLock, never()).release()
    verify(mockFileLock, never()).close()

    verify(mockChannel, times(1)).close()
    verify(mockFile, never()).delete()
    shutdownHooksRemoved shouldBe empty

    logErrorArgs should not be empty
    logErrorArgs.filter(_.isFailure) shouldBe empty
  }


  it should "return failure and release all resources if an exception is thrown while it is trying to add shutdown hook" in new Fixture {
    override val throwExceptionOnAddShutdownHook: Boolean = true
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    val result: Try[Unit] = projectLock.lock()
    result match {
      case Failure(_: SystemException) => // no op
      case _ => fail("wrong result when locking")
    }

    filesCreated should contain theSameElementsAs List((expectedDirectory, expectedFilename))
    randomAccessFilesCreated should contain theSameElementsAs List(mockFile)
    channelsCreated should contain theSameElementsAs List(mockRandomAccessFile)

    verify(mockChannel, times(1)).tryLock()
    shutdownHooksAdded shouldBe empty

    verify(mockFileLock, times(1)).release()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    logErrorArgs should not be empty
    logErrorArgs.filter(_.isFailure) shouldBe empty
  }


  it should "release resources, remove shutdown hook, and delete file when release is called" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    val releaseResult: Try[Unit] = projectLock.release()
    releaseResult.isSuccess shouldBe true

    verify(mockChannel, times(1)).tryLock()
    verify(mockFileLock, times(1)).release()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()
    shutdownHooksRemoved.size shouldBe 1

    logErrorArgs should not be empty
    logErrorArgs.filter(_.isFailure) shouldBe empty
  }

  it should "cleanup all other resources, return failure, and not log when exception is thrown during file lock release when lock owner releases lock" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    doThrow(new IOException("doom")).when(mockFileLock).release()

    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    val releaseResult: Try[Unit] = projectLock.release()
    releaseResult match {
      case Failure(_: IOException) => // ok
      case _ => fail("Release should be a failure")
    }
    verify(mockFileLock, times(1)).release()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()
    shutdownHooksRemoved.size shouldBe 1

    logErrorArgs should not be empty
    logErrorArgs.filter(_.isFailure) shouldBe empty
  }

  it should "cleanup all other resources, return failure, and log when exception is thrown when closing channel" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    doThrow(new RuntimeException("doom")).when(mockChannel).close()

    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    val releaseResult: Try[Unit] = projectLock.release()
    releaseResult.isSuccess shouldBe true

    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()
    shutdownHooksRemoved.size shouldBe 1

    logErrorArgs should not be empty
    val failures: List[Try[Unit]] = logErrorArgs.filter(_.isFailure)
    failures.size shouldBe 1
    failures.head match {
      case Failure(_: RuntimeException) => // no op
      case a: Any => fail(s"Wrong logged result: $a")
    }
  }

  it should "cleanup all other resources, return success, and log when exception is thrown deleting file" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    doThrow(new RuntimeException("doom")).when(mockFile).delete()

    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    val releaseResult: Try[Unit] = projectLock.release()
    releaseResult.isSuccess shouldBe true

    verify(mockFileLock, times(1)).close()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()
    shutdownHooksRemoved.size shouldBe 1

    logErrorArgs should not be empty
    val failures: List[Try[Unit]] = logErrorArgs.filter(_.isFailure)
    failures.size shouldBe 1
    failures.head match {
      case Failure(_: RuntimeException) => // no op
      case a: Any => fail(s"Wrong logged result: $a")
    }
  }

  it should "log error after previously performing other expected cleanup if exception is thrown removing shutdown hook" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    override val throwExceptionOnRemoveShutdownHook: Boolean = true

    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true
    val releaseResult: Try[Unit] = projectLock.release()
    releaseResult.isSuccess shouldBe true

    verify(mockFileLock, times(1)).close()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()
    shutdownHooksRemoved.isEmpty shouldBe true

    logErrorArgs should not be empty
    val failures: List[Try[Unit]] = logErrorArgs.filter(_.isFailure)
    failures.size shouldBe 1
    failures.head match {
      case Failure(_: Exception) => // no op
      case a: Any => fail(s"Wrong logged result: $a")
    }
  }

  it should "release resources, remove shutdown hook, and delete file when shutdown hook is called" in new Fixture {
    when(mockChannel.tryLock()).thenReturn(mockFileLock)
    val result: Try[Unit] = projectLock.lock()
    result.isSuccess shouldBe true

    shutdownHooksAdded.size shouldBe 1
    shutdownHooksAdded.head.run()

    verify(mockFileLock, times(1)).release()
    verify(mockFileLock, times(1)).close()
    verify(mockChannel, times(1)).close()
    verify(mockFile, times(1)).delete()

    logErrorArgs should not be empty
    val failures: List[Try[Unit]] = logErrorArgs.filter(_.isFailure)
    failures shouldBe empty
  }

  "shutdown hook" should "do everything release does except remove itself from jvm shutdown hooks" in new Fixture {
    //ejf-fixMe: implement
    fail("Not implemented")
  }
}
