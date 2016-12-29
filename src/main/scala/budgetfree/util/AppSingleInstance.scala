/*
 *  # BudgetFree
 *
 *  This file is part of BudgetFree - A FREE desktop budgeting application that
 *  helps you track your finances and literally FREES you from complex budgeting.
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
 *
 *  BudgetFree is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  BudgetFree is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with BudgetFree.  If not, see <http://www.gnu.org/licenses/>.
 */


package budgetfree.util

import java.io.{File, RandomAccessFile}
import java.nio.channels.FileLock

import budgetfree.constants.ApplicationHomeDir
import budgetfree.exceptional.FailQuietly
import grizzled.slf4j.Logging

import scala.util.{Success, Try}

object AppSingleInstance {
  def verify: Boolean = new AppSingleInstance().verify
}

private[util] class AppSingleInstance extends Logging {

  private[this] val file = new File(ApplicationHomeDir, "app.lck")
  private[this] val channel = new RandomAccessFile(file, "rw").getChannel

  private[this] val lock: Try[FileLock] = {
    logger.info(s"Trying to acquire single application instance lock: .${file.getAbsolutePath}")
    val lck = channel.tryLock()
    if(lck != null) {
      logger.info("Acquired single application instance lock.")
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run() {
          close()
          deleteFile()
        }
      })
      Success(lck)
    }
    else {
      FailQuietly
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
      logger.info("Releasing lock file.")
      scala.util.Try(lck.release())
    })
    logger.info("Closing channel.")
    scala.util.Try(channel.close())
  }

  private[this] def deleteFile() {
    logger.debug("Deleting lock file.")
    scala.util.Try(file.delete())
  }
}
