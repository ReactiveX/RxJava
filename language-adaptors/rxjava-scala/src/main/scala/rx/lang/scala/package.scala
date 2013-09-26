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
   * Represents an object that schedules units of work.
   */
  abstract class Scheduler {

    /**
     * Schedules a cancelable action to be executed.
     *
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule[T](state: T, action: (Scheduler, T) => Subscription): Subscription

    /**
     * Schedules a cancelable action to be executed in delayTime.
     *
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @param delayTime
     *            Time the action is to be delayed before executing.
     * @param unit
     *            Time unit of the delay time.
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule[T](state: T, action: (Scheduler, T) => Subscription, delayTime: Long, unit: TimeUnit): Subscription

    /**
     * Schedules a cancelable action to be executed periodically.
     * This default implementation schedules recursively and waits for actions to complete (instead of potentially executing
     * long-running actions concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
     *
     * @param state
     *            State to pass into the action.
     * @param action
     *            The action to execute periodically.
     * @param initialDelay
     *            Time to wait before executing the action for the first time.
     * @param period
     *            The time interval to wait each time in between executing the action.
     * @param unit
     *            The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    def schedulePeriodically[T](state: T, action: (Scheduler, T) => Subscription, initialDelay: Long, period: Long, unit: TimeUnit): Subscription

    /**
     * Schedules a cancelable action to be executed at dueTime.
     *
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @param dueTime
     *            Time the action is to be executed. If in the past it will be executed immediately.
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule[T](state: T, action: (Scheduler, T) => Subscription, dueTime: Date): Subscription

    /**
     * Schedules an action to be executed.
     *
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule(action: () => Unit): Subscription

    /**
     * Schedules an action to be executed in delayTime.
     *
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule(action: () => Unit, delayTime: Long, unit: TimeUnit): Subscription

    /**
     * Schedules an action to be executed periodically.
     *
     * @param action
     *            The action to execute periodically.
     * @param initialDelay
     *            Time to wait before executing the action for the first time.
     * @param period
     *            The time interval to wait each time in between executing the action.
     * @param unit
     *            The time unit the interval above is given in.
     * @return A subscription to be able to unsubscribe from action.
     */
    def schedulePeriodically(action: () => Unit, initialDelay: Long, period: Long, unit: TimeUnit): Subscription

    /**
     * @return the scheduler's notion of current absolute time in milliseconds.
     */
    def now(): Long

    /**
     * Parallelism available to a Scheduler.
     * <p>
     * This defaults to {@code Runtime.getRuntime().availableProcessors()} but can be overridden for use cases such as scheduling work on a computer cluster.
     *
     * @return the scheduler's available degree of parallelism.
     */
    def degreeOfParallelism: Int

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

  private[scala] implicit def fakeObserver2RxObserver[T](o: Observer[T]): rx.Observer[_ >: T] = ???
  private[scala] implicit def rxObserver2fakeObserver[T](o: rx.Observer[_ >: T]): Observer[T] = ???
  
  private[scala] implicit def fakeScheduler2RxScheduler(s: Scheduler): rx.Scheduler = ???
  private[scala] implicit def rxScheduler2fakeScheduler(s: rx.Scheduler): Scheduler = ???
  
  *///#else
  
  type Observer[-T] = rx.Observer[_ >: T]

  type Scheduler = rx.Scheduler
  
  type Subscription = rx.Subscription
  
  //#endif

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
