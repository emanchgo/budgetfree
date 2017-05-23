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

import slick.util.Logging

import scala.util.{Failure, Success, Try}
import trove.constants._
import trove.exceptional.{PersistenceError, SystemError}

object TroveDatabase {

  val lockFileSuffix: String = ".lck"
  val dbFileSuffix: String = ".sqlite3"

  val validFilenameChars: String = "^[a-zA-Z0-9_\\-]*$"

  def apply(projectName: String): Try[TroveDatabase] = {
    if(projectName.matches(validFilenameChars)) {
      new TroveDatabase(projectName).init()
    }
    else {
      PersistenceError(s"Invalid database name: $projectName. Database name may only contain alphanumeric characters, dashes, and underscores.")
    }
  }
}

class TroveDatabase private(projectName: String) extends Logging {
  import TroveDatabase._

  private def init(): Try[TroveDatabase] =
  {
    val lockfileName = s"$projectName$lockFileSuffix"
    val lockFile = new File(ProjectsHomeDir, lockfileName)
    val lockFileChannel = new RandomAccessFile(lockFile, "rw").getChannel

    val dbFileName = s"$projectName$dbFileSuffix"
    val dbFile = new File(ProjectsHomeDir, dbFileName)

    logger.debug(s"Trying to acquire lock: ${lockFile.getAbsolutePath}")
    val lock = lockFileChannel.tryLock()
    if(lock != null && lock.isSuccess) {
      logger.debug(s"Acquired single application instance lock for project $projectName.")
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run() {
          cleanup(lockFile, lock, lockFileChannel)
        }
      })
      Success(this)
    }
    else {
      PersistenceError(s"""Another instance of Trove currently has the database for project "$projectName" open.""").get
    }
  }

  private[this] def cleanup(lockFile: File, lock: FileLock, lockFileChannel: FileChannel): Try[Unit] = {
    logger.debug("Releasing lock file.")
    Try(lock.release()).flatMap { _ =>
      logger.debug("Closing channel.")
      Try(lockFileChannel.close())
    }.map(_ => lockFile.delete())
  }



  /*
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

    private[this] def deleteFile() {
      logger.debug("Deleting lock file.")
      scala.util.Try(file.delete())
    }
  }

   */

}
