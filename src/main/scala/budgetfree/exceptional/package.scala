package budgetfree

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
