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
 * (at your option) any later version.
 *
 *  Trove is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Trove.  If not, see <http://www.gnu.org/licenses/>.
 */

package trove

import scala.util.Failure

package object exceptional {
  sealed trait ExceptionLike {
    def message: String
    def cause: Option[Throwable]
  }

  abstract class AppException(val message: String, _cause: Option[Throwable]) extends java.lang.Exception(message, _cause.orNull)
    with ExceptionLike {
    override final def cause = Option(getCause)
  }

  final case class SystemException private(_message: String, _cause: Option[Throwable] = None)
    extends AppException(_message, _cause)

  final case class NotFoundException private(_message: String, _cause: Option[Throwable] = None)
    extends AppException(_message, _cause)

  final case class ValidationException private(_message: String, _cause: Option[Throwable] = None,
                                               errors: Seq[String] = Seq.empty)
    extends AppException(_message, _cause)

  final case class PersistenceException private(_message: String, _cause: Option[Throwable] = None)
    extends AppException(_message, _cause)

  object SystemError {
    def apply(message: String, t: Throwable) = Failure(SystemException(message, Some(t)))
    def apply(message: String) = Failure(SystemException(message))
  }

  object NotFoundError {
    def apply(message: String, t: Throwable) = Failure(NotFoundException(message, Some(t)))
    def apply(message: String) = Failure(NotFoundException(message))
  }

  object ValidationError {
    def apply(message: String, t: Throwable) = Failure(ValidationException(message, Some(t)))
    def apply(message: String) = Failure(ValidationException(message))
    def apply(message: String, errors: Seq[String]) = Failure(ValidationException(message, None, errors))
    def apply(message: String, t: Throwable, errors: Seq[String]) = Failure(ValidationException(message, Some(t), errors))
    def apply(message: String, t: Throwable, error: String) = Failure(ValidationException(message, Some(t), Seq(error)))
  }

  object PersistenceError {
    def apply(message: String, t: Throwable) = Failure(PersistenceException(message, Some(t)))
    def apply(message: String) = Failure(PersistenceException(message))
  }

  val FailQuietly = Failure(new scala.Exception())

}
