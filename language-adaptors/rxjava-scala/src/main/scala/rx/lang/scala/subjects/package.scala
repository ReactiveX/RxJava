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
 * Provides the type `Subject`.
 */
package object subjects {

  /**
   * A Subject is an Observable and an Observer at the same time. 
   *
   * The Java Subject looks like this: 
   * {{{
   * public abstract class Subject<T,R> extends Observable<R> implements Observer<T>
   * }}}
   */
  type Subject[-T, +R] = rx.subjects.Subject[_ >: T, _ <: R]

  /**
   * Subject that publishes only the last event to each [[Observer]] that has subscribed when the
   * sequence completes.
   *
   * The Java AsyncSubject looks like this:
   * {{{
   * public class AsyncSubject<T> extends Subject<T, T>
   * }}}
   */
  type AsyncSubject[T] = rx.subjects.AsyncSubject[T]
  object AsyncSubject {
    /**
     * Creates an [[AsyncSubject]]
     */
    def apply[T]() = rx.subjects.AsyncSubject.create[T]()
  }

  /**
   * Subject that publishes the most recent and all subsequent events to each subscribed [[Observer]].
   *
   * The Java BehaviorSubject looks like this:
   * {{{
   * public class BehaviorSubject<T> extends Subject<T, T>
   * }}}
   */
  type BehaviorSubject[T] = rx.subjects.BehaviorSubject[T]
  object BehaviorSubject {
    /**
     * Creates a [[BehaviorSubject]]
     */
    def apply[T](defaultValue:T) =
      rx.subjects.BehaviorSubject.createWithDefaultValue[T](defaultValue)
  }

  /**
   * Subject that retains all events and will replay them to an [[Observer]] that subscribes.
   *
   * The Java PublishSubject looks like this:
   * {{{
   * public final class ReplaySubject<T> extends Subject<T, T>
   * }}}
   */
  type ReplaySubject[T] = rx.subjects.ReplaySubject[T]
  object ReplaySubject {
    /**
     * Creates a [[ReplaySubject]]
     */
    def apply[T]() = rx.subjects.ReplaySubject.create[T]()
  }

  /**
   * Subject that, once and [[Observer]] has subscribed, publishes all subsequent
   * events to the subscriber.
   *
   * The Java PublishSubject looks like this:
   * {{{
   * public class PublishSubject<T> extends Subject<T, T>
   * }}}
   */
  type PlubishSubject[T] = rx.subjects.PublishSubject[T]
  object PublishSubject {
    /**
     * Creates a [[PublishSubject]]
     */
    def apply[T]() = rx.subjects.PublishSubject.create[T]()
  }

  // For nicer scaladoc, we would like to present something like this:
  /*
  trait Observable[+R] {}
  trait Observer[-T] {}
  trait Subject[-T, +R] extends Observable[R] with Observer[T] { }
  */
}
