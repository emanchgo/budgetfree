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
import scala.util.{Success, Try}

// Only useful where the underlying database can be opened by different programs.
private[persist] object ProjectLock {

  val lockfileSuffix: String = ".lck"

  def apply(projectName: String): Try[ProjectLock] = {
      val res = new ProjectLock(projectName)
      res.resources.map(_ => res)
  }
}

private[persist] class ProjectLock private(projectName: String) extends Logging {

  import ProjectLock._

  private[this] case class Resources(raf: RandomAccessFile, channel: FileChannel, lock: FileLock, shutdownHook: Thread)

  private[this] val lockfileName = s"$projectName$lockfileSuffix"
  private[this] val file = new File(ProjectsHomeDir, lockfileName)
  @volatile private var resources: Try[Resources] = {
    var raf: RandomAccessFile = null
    var ch: FileChannel = null
    var lck: FileLock = null
    var success = false
    try {
      logger.debug(s"Trying to acquire single application instance lock: .${file.getAbsolutePath}")
      raf = new RandomAccessFile(file, "rw")
      ch = raf.getChannel
      lck = ch.tryLock()
      if(lck != null) {
        success = true
        logger.debug(s"Acquired single application instance lock for project $projectName.")
        val shutdownHook = new Thread() {
          override def run() {
            releaseResources(raf, ch, lck)
          }
        }
        Runtime.getRuntime.addShutdownHook(shutdownHook)
        Success(Resources(raf, ch, lck, shutdownHook))
      }
      else {
        SystemError(s"""Another instance of $ApplicationName currently has project "$projectName" open.""")
      }
    }
    catch {
      case NonFatal(e) => SystemError(s"Unable to acquire lock for project $projectName")
    }
    finally {
      if (!success) {
        releaseResources(raf, ch, lck)
      }
    }
  }

  def release(): Unit = this.synchronized {
    resources.map {
      case Resources(raf, ch, lck, hook) =>
        if(Runtime.getRuntime.removeShutdownHook(hook)) {
          logger.debug("removed shutdown hook")
        }
        else {
          logger.debug("unable to remove shutdown hook")
        }
        releaseResources(raf, ch, lck)
        SystemError("Lock has been released")
    }
  }

  private[this] def releaseResources(raf: RandomAccessFile, ch: FileChannel, lck: FileLock): Unit = {
    try {
      logger.debug("releasing lock")
      if(lck != null) lck.close()
    }
    finally {
      try {
        logger.debug("closing file channel")
        if(ch != null) ch.close()
      }
      finally {
        logger.debug("closing file")
        if(raf != null) raf.close()
      }
    }
  }
}
