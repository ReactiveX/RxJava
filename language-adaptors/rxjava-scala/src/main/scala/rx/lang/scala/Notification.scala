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
  private [scala] def asJava: rx.Notification[_ <: T]
}

/**
 * Provides pattern matching support and constructors for Notifications.
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

  private [scala] def apply[T](n: rx.Notification[_ <: T]): Notification[T] = n.getKind match {
    case rx.Notification.Kind.OnNext => new OnNext(n)
    case rx.Notification.Kind.OnCompleted => new OnCompleted(n)
    case rx.Notification.Kind.OnError => new OnError(n)
  }
  
  // OnNext, OnError, OnCompleted are not case classes because we don't want pattern matching
  // to extract the rx.Notification
  

  
  object OnNext {

    def apply[T](value: T): Notification[T] = {
      Notification(new rx.Notification[T](value))
    }

    def unapply[U](n: Notification[U]): Option[U] = n match {
      case n2: OnNext[U] => Some(n.getValue)
      case _ => None
    }
  }

  class OnNext[+T] private[scala] (val asJava: rx.Notification[_ <: T]) extends Notification[T] {
    def value: T = asJava.getValue
  }
  
  object OnError {

    def apply[T](error: Throwable): Notification[T] = {
      Notification(new rx.Notification[T](error))
    }

    def unapply[U](n: Notification[U]): Option[Throwable] = n match {
      case n2: OnError[U] => Some(n2.error)
      case _ => None
    }
  }

  class OnError[+T] private[scala] (val asJava: rx.Notification[_ <: T]) extends Notification[T] {
    def error: Throwable = asJava.getThrowable
  }

  object OnCompleted {

    def apply[T](): Notification[T] = {
      Notification(new rx.Notification())
    }

    def unapply[U](n: Notification[U]): Option[Unit] = n match {
      case n2: OnCompleted[U] => Some()
      case _ => None
    }
  }

  class OnCompleted[T] private[scala](val asJava: rx.Notification[_ <: T]) extends Notification[T] {}


}

