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
