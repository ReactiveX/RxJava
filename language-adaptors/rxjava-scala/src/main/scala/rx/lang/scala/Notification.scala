/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala

/**
 * Emitted by Observables returned by [[rx.lang.scala.Observable.materialize]].
 */
sealed trait Notification[+T] {
  private [scala] val asJavaNotification: rx.Notification[_ <: T]

  override def equals(that: Any): Boolean = that match {
    case other: Notification[_] => asJavaNotification.equals(other.asJavaNotification)
    case _ => false
  }
  override def hashCode(): Int = asJavaNotification.hashCode()

  /**
   * Invokes the function corresponding to the notification.
   *
   * @param onNext
   *               The function to invoke for an [[rx.lang.scala.Notification.OnNext]] notification.
   * @param onError
   *               The function to invoke for an [[rx.lang.scala.Notification.OnError]] notification.
   * @param onCompleted
   *               The function to invoke for an [[rx.lang.scala.Notification.OnCompleted]] notification.
   */
  def accept[R](onNext: T=>R, onError: Throwable=>R, onCompleted: ()=>R): R = {
    this match {
      case Notification.OnNext(value)  => onNext(value)
      case Notification.OnError(error) => onError(error)
      case Notification.OnCompleted  => onCompleted()
    }
  }

  def apply[R](onNext: T=>R, onError: Throwable=>R, onCompleted: ()=>R): R =
     accept(onNext, onError, onCompleted)

  /**
   * Invokes the observer corresponding to the notification
   *
   * @param observer
   *                 The observer that to observe the notification
   */
  def accept(observer: Observer[T]): Unit = {
    this match {
      case Notification.OnNext(value)  => observer.onNext(value)
      case Notification.OnError(error) => observer.onError(error)
      case Notification.OnCompleted  => observer.onCompleted()
    }
  }

  def apply(observer: Observer[T]): Unit = accept(observer)
}

/**
 * Provides pattern matching support and constructors for Notifications.
 * 
 * Example:
 * {{{
 * import Notification._
 * Observable(1, 2, 3).materialize.subscribe(n => n match {
 *   case OnNext(v)     => println("Got value " + v)
 *   case OnCompleted() => println("Completed")
 *   case OnError(err)  => println("Error: " + err.getMessage)
 * })
 * }}}
 */
object Notification {

  private [scala] def apply[T](n: rx.Notification[_ <: T]): Notification[T] = n.getKind match {
    case rx.Notification.Kind.OnNext => new OnNext(n)
    case rx.Notification.Kind.OnCompleted => OnCompleted
    case rx.Notification.Kind.OnError => new OnError(n)
  }
  
  // OnNext, OnError, OnCompleted are not case classes because we don't want pattern matching
  // to extract the rx.Notification

  object OnNext {

    /**
     * Constructor for onNext notifications.
     *
     * @param value
     * The item passed to the onNext method.
     */
    def apply[T](value: T): Notification[T] = {
      Notification(rx.Notification.createOnNext[T](value))
    }

    /**
     * Extractor for onNext notifications.
     * @param notification
     *                     The [[rx.lang.scala.Notification]] to be destructed.
     * @return
     *         The item contained in this notification.
     */
    def unapply[U](notification: Notification[U]): Option[U] = notification match {
      case onNext: OnNext[U] => Some(onNext.value)
      case _ => None
    }
  }

  class OnNext[+T] private[scala] (val asJavaNotification: rx.Notification[_ <: T]) extends Notification[T] {
    def value: T = asJavaNotification.getValue
    override def toString = s"OnNext($value)"
  }

  object OnError {

    /**
     * Constructor for onError notifications.
     *
     * @param error
     * The exception passed to the onNext method.
     */
    def apply[T](error: Throwable): Notification[T] = {
      Notification(rx.Notification.createOnError[T](error))
    }

    /**
     * Destructor for onError notifications.
     *
     * @param notification
     *                     The [[rx.lang.scala.Notification]] to be deconstructed
     * @return
     *         The `java.lang.Throwable` value contained in this notification.
     */
    def unapply[U](notification: Notification[U]): Option[Throwable] = notification match {
      case onError: OnError[U] => Some(onError.error)
      case _ => None
    }
  }

  class OnError[+T] private[scala] (val asJavaNotification: rx.Notification[_ <: T]) extends Notification[T] {
    def error: Throwable = asJavaNotification.getThrowable
    override def toString = s"OnError($error)"
  }

  object OnCompleted extends Notification[Nothing] {
    override def toString = "OnCompleted"
    val asJavaNotification = rx.Notification.createOnCompleted[Nothing]()
  }

}

