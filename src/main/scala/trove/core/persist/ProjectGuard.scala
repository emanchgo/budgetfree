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
import java.nio.channels.FileLock

import grizzled.slf4j.Logging
import trove.constants.ProjectsHomeDir
import trove.exceptional.SystemError

import scala.util.{Success, Try}

//ejf-fixMe: move to db impl, as this is specific to sqlite.
//ejf-fixMe: explore opening a db by more than one process ... if it fails, this can go away.
private[persist] object ProjectGuard {

  val lockFileSuffix: String = ".lck"
  val dbFileSuffix: String = ".sqlite3"

  def apply(projectName: String): ProjectGuard = new ProjectGuard(projectName)
}

private[persist] class ProjectGuard private(projectName: String) extends Logging {

  import ProjectGuard._

  private[this] val lockfileName = s"$projectName$lockFileSuffix"
  private[this] val file = new File(ProjectsHomeDir, lockfileName)
  private[this] val dbFileName = s"$projectName$dbFileSuffix"
  private[this] val dbFile = new File(ProjectsHomeDir, dbFileName)
  private[this] val channel = new RandomAccessFile(file, "rw").getChannel

  val lock: Try[File] = {
    logger.debug(s"Trying to acquire single application instance lock: .${file.getAbsolutePath}")
    val lck = channel.tryLock()
    if(lck != null) {
      logger.debug(s"Acquired single application instance lock for project $projectName.")
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run() {
          close()
          deleteFile()
        }
      })
      Success(dbFile)
    }
    else {
      SystemError(s"""Another instance of Trove currently has project "$projectName" open.""")
    }
  }

  if(lock.isFailure) {
    fail()
  }

  def verify: Boolean = lock.isSuccess

  private[this] def fail() {
    logger.error("Failed to acquire single application instance lock.")
    close()
  }

  private[this] def close() {
    lock.foreach(lck => {
      logger.debug("Releasing lock file.")
      scala.util.Try(lck.release())
    })
    logger.debug("Closing channel.")
    scala.util.Try(channel.close())
  }

  private[this] def deleteFile() {
    logger.debug("Deleting lock file.")
    scala.util.Try(file.delete())
  }
}
