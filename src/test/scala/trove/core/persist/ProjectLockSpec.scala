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

import java.io.File
import java.nio.channels.FileChannel

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import trove.constants._

import scala.util.{Success, Try}

class ProjectLockSpec extends FlatSpec with MockitoSugar with BeforeAndAfter with Matchers {

  import ProjectLock._

  val projectName = "unittest"
  val actualFile = new File(ProjectsHomeDir, constructLockfileName(projectName))

  after {
    if(actualFile.exists()) {
      actualFile.deleteOnExit()
    }
  }

  trait Fixture {

    var mockFilesCreated: List[(File, String)] = List.empty
    val mockFile: File = mock[File]

    var mockChannelsCreated: List[(File, String)] = List.empty
    val mockChannel: FileChannel = mock[FileChannel]

    var shutdownHooksAdded: List[Thread] = List.empty
    var shutdownHooksRemoved: List[Thread] = List.empty
    var logErrorArgs: List[Try[Unit]] = List.empty

    val lock: ProjectLock = new ProjectLock(projectName) with EnvironmentOps {

      def newFile(directory: File, filename: String): File = {
        mockFilesCreated = (directory, filename) +: mockFilesCreated
        mockFile
      }

      def newChannel(file: File, mode: String): FileChannel = {
        mockChannelsCreated = (file, mode) +: mockChannelsCreated
        mockChannel
      }

      def addShutdownHook(thread: Thread): Unit = shutdownHooksAdded = thread +: shutdownHooksAdded

      def removeShutdownHook(thread: Thread): Unit = shutdownHooksRemoved = thread +: shutdownHooksRemoved

      def logError(result: Try[Unit]): Unit = logErrorArgs = result +: logErrorArgs
    }
  }

  "ProjectLock" should "construct new file channel when lock is called" in new Fixture {
    lock.lock() shouldBe Success(())
    mockFilesCreated should contain theSameElementsAs List.empty

  }

  it should "return SystemError if it cannot acquire lock" in new Fixture {
    fail("not yet implemented")
  }

  it should "return failure if an exception is thrown while it is trying to acquire lock" in new Fixture {
    fail("not yet implemented")
  }

  it should "close channel if an exception is thrown while it is trying to acquire lock" in new Fixture {
    fail("not yet implemented")
  }

  it should "add shutdown hook if it acquired lock" in new Fixture {
    fail("not yet implemented")
  }

  it should "return failure if an exception is thrown while it is trying to add shutdown hook" in new Fixture {
    fail("not yet implemented")
  }

  it should "release resources when release is called" in new Fixture {
    fail("not yet implemented")
  }

  it should "delete file when release is called" in new Fixture {
    fail("not yet implemented")
  }

  it should "remove shutdown hook when release is called" in new Fixture {
    fail("not yet implemented")
  }

  it should "release resources when shutdown hook is called" in new Fixture {
    fail("not yet implemented")
  }

  it should "delete file when shutdown hook is called" in new Fixture {
    fail("not yet implemented")
  }

  it should "log error when shutdown hook execution experiences an error" in new Fixture {
    fail("not yet implemented")
  }

  it should "not log error when lock owner calls release and there is an exception" in new Fixture {
    fail("not yet implemented")
  }

  it should "remove shutdown hook when shutdown hook is called" in new Fixture {
    fail("not yet implemented")
  }

  it should "perform all other cleanup if exception is thrown releasing lock" in new Fixture {
    fail("not yet implemented")
  }

  it should "perform all other cleanup if exception is thrown closing channel" in new Fixture {
    fail("not yet implemented")
  }

  it should "perform all other cleanup if exception is thrown deleting file" in new Fixture {
    fail("not yet implemented")
  }

  it should "perform all other cleanup if exception is thrown removing shutdown hook" in new Fixture {
    fail("not yet implemented")
  }
  
}
