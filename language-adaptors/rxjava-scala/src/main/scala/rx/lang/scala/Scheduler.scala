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

import java.util.Date
import scala.concurrent.duration.Duration
import rx.util.functions.{Action0, Action1, Func2}

/**
 * Represents an object that schedules units of work.
 */
trait Scheduler {
  def asJavaScheduler: rx.Scheduler

  /**
   * Schedules a cancelable action to be executed.
   *
   * @param action Action to schedule.
   * @return a subscription to be able to unsubscribe from action.
   */
  def schedule(action: Scheduler => Subscription): Subscription = {
    this.schedule[Integer](0, (s: Scheduler, x: Integer) => action(s): Subscription): Subscription
  }

  /**
   * Schedules a cancelable action to be executed.
   *
   * @param state State to pass into the action.
   * @param action Action to schedule.
   * @return a subscription to be able to unsubscribe from action.
   */
  private def schedule[T](state: T, action: (Scheduler, T) => Subscription): Subscription = {
    Subscription(asJavaScheduler.schedule(state, new Func2[rx.Scheduler, T, rx.Subscription] {
      def call(t1: rx.Scheduler, t2: T): rx.Subscription = {
        action(Scheduler(t1), t2).asJavaSubscription
      }
    }))
  }

  /**
   * Schedules a cancelable action to be executed in delayTime.
   *
   * @param action Action to schedule.
   * @param delayTime  Time the action is to be delayed before executing.
   * @return a subscription to be able to unsubscribe from action.
   */
  def schedule(delayTime: Duration)(action: Scheduler => Subscription): Subscription = {
    this.schedule[Integer](0, (s: Scheduler, x: Integer) => action(s), delayTime: Duration): Subscription
  }

  /**
   * Schedules a cancelable action to be executed in delayTime.
   *
   * @param state
   *            State to pass into the action.
   * @param action
   *            Action to schedule.
   * @param delayTime
   *            Time the action is to be delayed before executing.
   * @return a subscription to be able to unsubscribe from action.
   */
  private def schedule[T](state: T, action: (Scheduler, T) => Subscription, delayTime: Duration): Subscription = {
    Subscription(asJavaScheduler.schedule(state, action, delayTime.length, delayTime.unit))
  }

  /**
   * Schedules a cancelable action to be executed periodically.
   * This default implementation schedules recursively and waits for actions to complete (instead of potentially executing
   * long-running actions concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
   *
   * @param action The action to execute periodically.
   * @param initialDelay Time to wait before executing the action for the first time.
   * @param period The time interval to wait each time in between executing the action.
   * @return A subscription to be able to unsubscribe from action.
   */
  def schedule(initialDelay: Duration, period: Duration)(action: Scheduler => Subscription): Subscription = {
    this.schedulePeriodically[Integer](0, (s: Scheduler, x:Integer) => action(s): Subscription, initialDelay: Duration, period: Duration): Subscription
  }

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
   * @return A subscription to be able to unsubscribe from action.
   */
  private def schedulePeriodically[T](state: T, action: (Scheduler, T) => Subscription, initialDelay: Duration, period: Duration): Subscription = {
    Subscription(asJavaScheduler.schedulePeriodically(state, action, initialDelay.length, initialDelay.unit.convert(period.length, period.unit), initialDelay.unit))
  }

  /**
   * Schedules a cancelable action to be executed at dueTime.
   *
   * @param action Action to schedule.
   * @param dueTime Time the action is to be executed. If in the past it will be executed immediately.
   * @return a subscription to be able to unsubscribe from action.
   */
  def schedule(dueTime: Date)(action: Scheduler => Subscription): Subscription = {
    this.schedule(0: Integer, (s: Scheduler, x: Integer) => action(s): Subscription, dueTime: Date): Subscription
  }

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
  private def schedule[T](state: T, action: (Scheduler, T) => Subscription, dueTime: Date): Subscription = {
    Subscription(asJavaScheduler.schedule(state, action, dueTime))
  }

  /**
   * Schedules an action to be executed.
   *
   * @param action
   *            action
   * @return a subscription to be able to unsubscribe from action.
   */
  def schedule(action: =>Unit): Subscription = {
    Subscription(asJavaScheduler.schedule(()=>action))
  }

  /**
   * Schedules an action to be executed in delayTime.
   *
   * @param action action
   * @return a subscription to be able to unsubscribe from action.
   */
  def schedule(delayTime: Duration)(action: =>Unit): Subscription = {
    Subscription(asJavaScheduler.schedule(()=>action, delayTime.length, delayTime.unit))
  }

  /**
   * Schedules an action to be executed periodically.
   *
   * @param action
   *            The action to execute periodically.
   * @param initialDelay
   *            Time to wait before executing the action for the first time.
   * @param period
   *            The time interval to wait each time in between executing the action.
   * @return A subscription to be able to unsubscribe from action.
   */
  def schedule(initialDelay: Duration, period: Duration)(action: =>Unit): Subscription = {
    Subscription(asJavaScheduler.schedulePeriodically(()=>action, initialDelay.length, initialDelay.unit.convert(period.length, period.unit), initialDelay.unit))
  }

  def scheduleRec(work: (=>Unit)=>Unit): Subscription = {
    Subscription(asJavaScheduler.schedule(new Action1[Action0] {
      def call(t1: Action0){
        work{ t1.call() }
      }
    }))
    //action1[action0]

//    val subscription = new rx.subscriptions.MultipleAssignmentSubscription()
//
//    subscription.setSubscription(
//      this.schedule(scheduler => {
//        def loop(): Unit =  subscription.setSubscription(scheduler.schedule{ work{ loop() }})
//        loop()
//        subscription
//      }))
//    subscription
  }

  /**
   * Returns the scheduler's notion of current absolute time in milliseconds.
   */
  def now: Long = {
    asJavaScheduler.now
  }

  /**
   * Parallelism available to a Scheduler.
   *
   * This defaults to {@code Runtime.getRuntime().availableProcessors()} but can be overridden for use cases such as scheduling work on a computer cluster.
   *
   * @return the scheduler's available degree of parallelism.
   */
  def degreeOfParallelism: Int = {
    asJavaScheduler.degreeOfParallelism
  }

}

