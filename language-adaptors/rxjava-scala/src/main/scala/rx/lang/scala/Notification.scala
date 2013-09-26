package rx.lang.scala

/**
 * Emitted by Observables returned by [[rx.lang.scala.Observable.materialize]].
 */
sealed trait Notification[+T] {
  def asJava: rx.Notification[_ <: T]
}

/**
 * Provides pattern matching support for Notifications.
 * 
 * Example:
 * {{{
 * import Notification._
 * Observable(1, 2, 3).materialize.subscribe(n => n match {
 *   case OnNext(v) => println("Got value " + v)
 *   case OnCompleted() => println("Completed")
 *   case OnError(err) => println("Error: " + err.getMessage)
 * })
 * }}}
 */
object Notification {

  def apply[T](n: rx.Notification[_ <: T]): Notification[T] = n.getKind match {
    case rx.Notification.Kind.OnNext => new OnNext(n)
    case rx.Notification.Kind.OnCompleted => new OnCompleted(n)
    case rx.Notification.Kind.OnError => new OnError(n)
  }
  
  // OnNext, OnError, OnCompleted are not case classes because we don't want pattern matching
  // to extract the rx.Notification
  
  class OnNext[+T](val asJava: rx.Notification[_ <: T]) extends Notification[T] {
    def value: T = asJava.getValue
  }
  
  object OnNext {
    def unapply[U](n: Notification[U]): Option[U] = n match {
      case n2: OnNext[U] => Some(n.asJava.getValue)
      case _ => None
    }
  }
  
  class OnError[+T](val asJava: rx.Notification[_ <: T]) extends Notification[T] {
    def error: Throwable = asJava.getThrowable()
  }
  
  object OnError {
    def unapply[U](n: Notification[U]): Option[Throwable] = n match {
      case n2: OnError[U] => Some(n2.asJava.getThrowable)
      case _ => None
    }
  }
  
  class OnCompleted[T](val asJava: rx.Notification[_ <: T]) extends Notification[T] {}
  
  object OnCompleted {
    def unapply[U](n: Notification[U]): Option[Unit] = n match {
      case n2: OnCompleted[U] => Some()
      case _ => None
    }
  }

}

