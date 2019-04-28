/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2019 Eric John Fredericks.
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

package trove.util.io

import org.scalatest.{FlatSpec, Matchers}

import scala.util.control.ControlThrowable


class IOUsingSpec extends FlatSpec with Matchers {

  class Fixture {

    class Fatal extends Exception with ControlThrowable

    class MyCloseable(toThrow: Option[Throwable] = None) extends AutoCloseable {

      def this(throwable: Throwable) = this(Some(throwable))

      @volatile var closeCallCount = 0
      override def close(): Unit = {
        closeCallCount += 1
        if(toThrow.nonEmpty) {
          throw toThrow.get
        }
      }
    }
  }

  // "using" should "return the value produced by the function passed to it"
  "using" should "return the value produced by the function passed to it and call close if the function succeeds" in new Fixture {
    val cl = new MyCloseable
    val result: Int = using(cl) { _ =>
      42
    }
    result shouldBe 42
    cl.closeCallCount shouldBe 1

  }

  it should "call close if the function succeeds and throw any exception thrown by close" in new Fixture {
    val ex = new Exception("doom")
    val cl = new MyCloseable(ex)
    val caught: Exception = intercept[Exception] {
      using(cl) { _ =>
        42
      }
    }
    caught shouldBe ex
    cl.closeCallCount shouldBe 1
  }

  it should "call close if the function throws and throw the original exception if close doesn't throw" in new Fixture {
    val ex = new Exception("doom")
    val cl = new MyCloseable
    val caught: Exception = intercept[Exception] {
      using(cl) { _ =>
        throw ex
      }
    }
    caught shouldBe ex
    cl.closeCallCount shouldBe 1
  }

  it should "call close if the function throws and throw the original exception suppressing a non-fatal exception thrown by close" in new Fixture {
    val nonFatal = new Exception("non-fatal")
    val nonFatalFromClose = new Exception("non-fatal from close")
    val cl = new MyCloseable(nonFatalFromClose)
    val caught: Exception = intercept[Exception] {
      using(cl) { _ =>
        throw nonFatal
      }
    }
    cl.closeCallCount shouldBe 1
    caught shouldBe nonFatal
    caught.getSuppressed should contain theSameElementsAs Seq(nonFatalFromClose)
  }

  it should "call close if the function throws and throw a fatal exception thrown by close suppressing an original non-fatal exception thrown by the function" in new Fixture {
    val nonFatal = new Exception("non-fatal")
    val fatalFromClose = new Fatal
    val cl = new MyCloseable(fatalFromClose)
    val caught: Fatal = intercept[Fatal] {
      using(cl) { _ =>
        throw nonFatal
      }
    }
    cl.closeCallCount shouldBe 1
    caught shouldBe fatalFromClose
    caught.getSuppressed should contain theSameElementsAs Seq(nonFatal)
  }

  it should "call close if the function throws and throw an interrupted exception thrown by close suppressing a non-fatal exception thrown by the function" in new Fixture {
    val nonFatal = new Exception("non-fatal")
    val interruptedFromClose = new InterruptedException
    val cl = new MyCloseable(interruptedFromClose)
    val caught: InterruptedException = intercept[InterruptedException] {
      using(cl) { _ =>
        throw nonFatal
      }
    }
    cl.closeCallCount shouldBe 1
    caught shouldBe interruptedFromClose
    caught.getSuppressed should contain theSameElementsAs Seq(nonFatal)
  }

  it should "call close if the function throws and throw an interrupted exception thrown by close suppressing a fatal exception thrown by the function" in new Fixture {
    val fatal = new Fatal
    val interruptedFromClose = new InterruptedException
    val cl = new MyCloseable(interruptedFromClose)
    val caught: InterruptedException = intercept[InterruptedException] {
      using(cl) { _ =>
        throw fatal
      }
    }
    cl.closeCallCount shouldBe 1
    caught shouldBe interruptedFromClose
    caught.getSuppressed should contain theSameElementsAs Seq(fatal)
  }

  it should "call close if the function throws and throw an original fatal exception thrown by the function suppressing a fatal exception that is not an interrupted exception thrown by close" in new Fixture {
    val fatal = new Fatal
    val fatalFromClose = new Fatal
    val cl = new MyCloseable(fatalFromClose)
    val caught: Fatal = intercept[Fatal] {
      using(cl) { _ =>
        throw fatal
      }
    }
    cl.closeCallCount shouldBe 1
    caught shouldBe fatal
    caught.getSuppressed should contain theSameElementsAs Seq(fatalFromClose)
  }
}
