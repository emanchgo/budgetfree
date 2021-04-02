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

package trove.core.infrastructure.persist.lock

import java.io.{File, IOException, RandomAccessFile}
import java.nio.channels.{FileChannel, FileLock}

import grizzled.slf4j.Logging
import trove.constants._
import trove.core.infrastructure.persist.lock.ProjectLock.EnvironmentOps
import trove.exceptional.{SystemError, SystemException}

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

// Only useful where the underlying database can be opened by different programs concurrently.
// NOTE that this class does not clean up after itself if the JVM receives a SIGTERM.
// Cleanup should be handled externally.
private[persist] object ProjectLock {

  val LockfileSuffix: String = ".lck"

  private case class Resources(channel: LockableChannel, lock: FileLock)

  def constructLockfileName(projectName: String): String = s"$projectName$LockfileSuffix"

  class LockableChannel(raf: RandomAccessFile) {
    val channel: FileChannel = raf.getChannel
    @throws(clazz = classOf[IOException])
    def tryLock(): FileLock = channel.tryLock()
    def close(): Unit = channel.close()
  }

  trait EnvironmentOps extends LockResourceReleaseErrorHandling {
    def newFile(directory: File, filename: String): File
    def newRandomAccessFile(file: File): RandomAccessFile
    def newChannel(raf: RandomAccessFile): LockableChannel
  }

  def apply(projectsHomeDir: File, projectName: String): ProjectLock = new ProjectLock(projectsHomeDir, projectName) with EnvironmentOps {
    override def newFile(directory: File, filename: String): File = new File(directory, filename)
    override def newRandomAccessFile(file: File): RandomAccessFile = new RandomAccessFile(file: File, "rw")
    override def newChannel(raf: RandomAccessFile): LockableChannel = new LockableChannel(raf)
  }
}

private[persist] class ProjectLock(projectsHomeDir: File, projectName: String) extends LockResourceReleaseErrorHandling with Logging {
  self: EnvironmentOps =>

  import ProjectLock._

  val lockfileName: String = constructLockfileName(projectName)
  private[this] val file = newFile(projectsHomeDir, lockfileName)

  @volatile private[this] var resources: Option[Resources] = None

  def lock(): Try[Unit] = Try {
    logger.debug(s"Trying to acquire single application instance lock: .${file.getAbsolutePath}")

    val raf = newRandomAccessFile(file)
    val channel = newChannel(raf)

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
        val res = Resources(channel, lock)
        resources = Some(res)
      Success(res)
    }
  }.flatten.map(_ =>
    ()).recoverWith {
    case e: SystemException =>
      Failure(e)
    case NonFatal(e) =>
      SystemError("Error acquiring project lock", e)
  }.recoverWith {
    case e =>
      release()
      Failure(e)
  }

  def isLocked: Boolean = resources.nonEmpty

  def release(): Unit =
    resources.map{ res =>

      val closeResult = close(res.channel, Some(res.lock), Some(file))
      handleLockResourceReleaseError(closeResult)
      closeResult.map { _ =>
        resources = None
      }
  }

  private[this] def close(channel: LockableChannel, lock: Option[FileLock] = None, file: Option[File] = None):
    Try[Unit] = {

    val result = lock.fold[Try[Unit]](Success(())) { lck: FileLock =>
      Try(lck.release())
    }

    handleLockResourceReleaseError(Try(channel.close()))
    file.foreach(f =>
      handleLockResourceReleaseError(Try(f.delete())))

    result
  }
}
