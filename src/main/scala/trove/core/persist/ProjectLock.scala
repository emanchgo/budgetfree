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
import java.io.{File, IOException, RandomAccessFile}
import java.nio.channels.{FileChannel, FileLock}

import grizzled.slf4j.Logging
import org.omg.CORBA.SystemException
import trove.constants._
import trove.core.persist.ProjectLock.EnvironmentOps
import trove.exceptional.SystemError

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

// Only useful where the underlying database can be opened by different programs concurrently.
private[persist] object ProjectLock {

  val lockfileSuffix: String = ".lck"

  private case class Resources(channel: LockableChannel, lock: FileLock, shutdownHook: Thread)

  def constructLockfileName(projectName: String): String = s"$projectName$lockfileSuffix"

  //ejf-fixMe: refactor this, move RAF to EnvironmentOps
  class LockableChannel(file: File) {
    val channel: FileChannel = new RandomAccessFile(file, "rw").getChannel
    @throws(clazz = classOf[IOException])
    def tryLock(): FileLock = channel.tryLock()
    def close(): Unit = channel.close()
  }

  trait EnvironmentOps {
    def newFile(directory: File, filename: String): File
    def newChannel(file: File, mode: String): LockableChannel
    def addShutdownHook(thread: Thread): Unit
    def removeShutdownHook(thread: Thread): Unit
    def logIfError(result: Try[Unit]): Unit
  }

  def apply(projectName: String): ProjectLock = new ProjectLock(projectName) with EnvironmentOps {
    def newFile(directory: File, filename: String): File = new File(directory, filename)
    def newChannel(file: File, mode: String): LockableChannel = new LockableChannel(file)
    def addShutdownHook(hook: Thread): Unit = Runtime.getRuntime.addShutdownHook(hook)
    def removeShutdownHook(hook: Thread): Unit = Runtime.getRuntime.removeShutdownHook(hook)
    def logIfError(result: Try[Unit]): Unit = result match {
      case Success(_) => // No-op
      case Failure(NonFatal(e)) => logger.error("Error!", e)
      case Failure(e) => throw e // Fatal, throw it, bubble it up!
    }
  }
}

private[persist] class ProjectLock(projectName: String) extends Logging { self: EnvironmentOps =>

  import ProjectLock._

  private[this] val lockfileName = constructLockfileName(projectName)
  private[this] val file = newFile(ProjectsHomeDir, lockfileName)

  @volatile private[this] var resources: Option[Resources] = None

  def lock(): Try[Unit] = Try {
    logger.debug(s"Trying to acquire single application instance lock: .${file.getAbsolutePath}")

    val channel = newChannel(file, "rw")

    var tryLockSuccess = false
    var tryLockResult: FileLock = null
    try {
      tryLockResult = channel.tryLock()
      tryLockSuccess = true
    } finally {
      if(!tryLockSuccess || tryLockResult == null) {
        close(channel = channel)
      }
    }

    Option(tryLockResult).fold[Try[Resources]] {
      logger.warn(s"Failed to acquire project lock for $projectName (${file.getAbsolutePath})")
      SystemError(s"""Another instance of $ApplicationName currently has project "$projectName" open.""")
    } {
      lock =>
        logger.debug(s"Acquired single application instance lock for project $projectName.")
        val shutdownHook = new Thread {
          override def run(): Unit = {
            logIfError(release())
          }
        }
        val res = Resources(channel, lock, shutdownHook)
        resources = Some(res)
        addShutdownHook(shutdownHook)
      Success(res)
    }
  }.flatten.map(_ => ()).recoverWith {
    case e: SystemException => Failure(e)
    case NonFatal(e) => SystemError("Error acquiring project lock", e)
  }.recoverWith {
    case e =>
      release()
      Failure(e)
  }

  def release(): Try[Unit] = resources.fold[Try[Unit]](SystemError(
    s"""$ApplicationName is not currently locked by the virtual machine.""")) { res =>
    close(res.channel, Some(res.lock), Some(res.shutdownHook), Some(file))
  }

  private[this] def close(channel: LockableChannel, lock: Option[FileLock] = None, shutdownHook: Option[Thread] = None,
                          file: Option[File] = None): Try[Unit] = {

    val result = lock.fold[Try[Unit]](Success(())) { lck: FileLock =>
      Try(lck.release())
    }

    logIfError(Try(channel.close()))
    file.foreach(f => logIfError(Try(f.delete())))

    shutdownHook.foreach { hook: Thread =>
      logIfError(Try(removeShutdownHook(hook)))
    }

    result
  }

}
