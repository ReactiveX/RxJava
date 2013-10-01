/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang

import java.util.concurrent.TimeUnit
import java.util.Date

/* 
 * Note that:
 * -  Scala users cannot use Java's types with variance without always using writing
 *    e.g. rx.Notification[_ <: T], so we create aliases fixing the variance
 * -  For consistency, we create aliases for all types which Scala users need
 */

/**
 * This package contains all classes that RxScala users need.
 * 
 * It mirrors the structure of package `rx`, but implementation classes that RxScala users
 * will not need are left out.
 */
package object scala {

  /*
   * Here we're imitating C's preprocessor using Search & Replace.
   * 
   * To activate the code needed to get nice Scaladoc, do the following replacements:
   *    /*//#ifdef SCALADOC    -->   //#ifdef SCALADOC
   *    *///#else              -->   /*//#else
   *    //#endif               -->   *///#endif
   *
   * To get back to the actual code, undo the above replacements.
   * 
   */

  /*//#ifdef SCALADOC

  /**
   * Provides a mechanism for receiving push-based notifications.
   *
   * After an Observer calls an [[rx.lang.scala.Observable]]'s `subscribe` method, the Observable
   * calls the Observer's `onNext` method to provide notifications. A well-behaved Observable will
   * call an Observer's `onCompleted` method exactly once or the Observer's `onError` method exactly once.
   */
  trait Observer[-T] {

    /**
     * Notifies the Observer that the [[rx.lang.scala.Observable]] has finished sending push-based notifications.
     *
     * The [[rx.lang.scala.Observable]] will not call this method if it calls `onError`.
     */
    def onCompleted(): Unit

    /**
     * Notifies the Observer that the [[rx.lang.scala.Observable]] has experienced an error condition.
     *
     * If the [[rx.lang.scala.Observable]] calls this method, it will not thereafter call `onNext` or `onCompleted`.
     */
    def onError(e: Throwable): Unit

    /**
     * Provides the Observer with new data.
     *
     * The [[rx.lang.scala.Observable]] calls this closure 0 or more times.
     *
     * The [[rx.lang.scala.Observable]] will not call this method again after it calls either `onCompleted` or `onError`.
     */
    def onNext(arg: T): Unit
  }

  /**
   * Subscriptions are returned from all Observable.subscribe methods to allow unsubscribing.
   * 
   * This interface is the equivalent of IDisposable in the .NET Rx implementation.
   */
  trait Subscription {
    /**
     * Call this method to stop receiving notifications on the Observer that was registered when 
     * this Subscription was received.
     */
    def unsubscribe(): Unit
  }
  
  import language.implicitConversions
  
  private[scala] implicit def fakeSubscription2RxSubscription(s: Subscription): rx.Subscription = 
    new rx.Subscription {
      def unsubscribe() = s.unsubscribe()
    }
  private[scala] implicit def rxSubscription2FakeSubscription(s: rx.Subscription): Subscription = 
    new Subscription {
      def unsubscribe() = s.unsubscribe()
    }

  private[scala] implicit def schedulerActionToFunc2[T](action: (Scheduler, T) => Subscription) =
    new rx.util.functions.Func2[rx.Scheduler, T, rx.Subscription] {
      def call(s: rx.Scheduler, t: T): rx.Subscription = {
        action(ImplicitFunctionConversions.javaSchedulerToScalaScheduler(s), t)
      }
    }  
  
  private[scala] implicit def fakeObserver2RxObserver[T](o: Observer[T]): rx.Observer[_ >: T] = ???
  private[scala] implicit def rxObserver2fakeObserver[T](o: rx.Observer[_ >: T]): Observer[T] = ???
  
  *///#else
  
  type Observer[-T] = rx.Observer[_ >: T]

  type Subscription = rx.Subscription
  
  //#endif
 
  /**
   * Allows to construct observables in a similar way as futures.
   * 
   * Example:
   *
   * {{{
   * implicit val scheduler = Schedulers.threadPoolForIO
   * val o: Observable[List[Friend]] = observable {
   *    session.getFriends
   * }
   * o.subscribe(
   *   friendList => println(friendList),
   *   err => println(err.getMessage)
   * )
   * }}} 
   */
  def observable[T](body: => T)(implicit scheduler: Scheduler): Observable[T] = {
    Observable(1).observeOn(scheduler).map(_ => body)
  }
}

/*

These classes are considered unnecessary for Scala users, so we don't create aliases for them:

rx.plugins.RxJavaErrorHandler
rx.plugins.RxJavaObservableExecutionHook
rx.plugins.RxJavaPlugins

rx.subscriptions.BooleanSubscription
rx.subscriptions.CompositeSubscription
rx.subscriptions.Subscriptions

*/
