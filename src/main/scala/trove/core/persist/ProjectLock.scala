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
import java.io.{File, RandomAccessFile}
import java.nio.channels.{FileChannel, FileLock}

import grizzled.slf4j.Logging
import trove.constants._
import trove.exceptional.SystemError

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

// Only useful where the underlying database can be opened by different programs concurrently.
private[persist] object ProjectLock {

  private case class Resources(channel: FileChannel, lock: FileLock)

  val lockfileSuffix: String = ".lck"

  def apply(projectName: String): ProjectLock = new ProjectLock(projectName)
}

private[persist] class ProjectLock private(projectName: String) extends Logging {

  import ProjectLock._

  private[this] val lockfileName = s"$projectName$lockfileSuffix"
  private[this] val file = new File(ProjectsHomeDir, lockfileName)

  @volatile private[this] var resources: Option[Resources] = None

  def lock(): Try[Unit] = Try {
    val channel = new RandomAccessFile(file, "rw").getChannel
    logger.debug(s"Trying to acquire single application instance lock: .${file.getAbsolutePath}")

    Option(channel.tryLock()).fold[Try[Resources]] {
      logger.warn(s"Failed to acquire project lock for $projectName (${file.getAbsolutePath})")
      SystemError(s"""Another instance of $ApplicationName currently has project "$projectName" open.""")
    } {
      lock =>
        logger.debug(s"Acquired single application instance lock for project $projectName.")
        val res = Resources(channel, lock)
        resources = Some(res)
        Runtime.getRuntime.addShutdownHook(new Thread() {
          override def run() {
            logError(release())
          }
        })
      Success(res)
    }
  }.flatten.map(_ => ())

  def release(): Try[Unit] = resources.fold[Try[Unit]](SystemError(
    s"""$ApplicationName is not currently locked by the virtual machine.""")) { res =>
    close(res.channel, Some(res.lock))
  }

  private[this] def close(channel: FileChannel, lock: Option[FileLock] = None): Try[Unit] = {
    lock.fold[Try[Unit]](Success(())) { lck =>
      logger.debug("Releasing lock file.")
      Try(lck.release())
    }.map { _ =>
      logger.debug("Closing channel.")
      channel.close()
    }.map { _ =>
      logger.debug("Deleting lock file.")
      file.delete()
    }
  }

  private[this] def logError(result: Try[Unit]): Unit = result match {
    case Success(_) => // No-op
    case Failure(NonFatal(e)) => logger.error("Error!", e)
    case Failure(e) => throw e // Fatal, throw it, bubble it up!
  }
}
