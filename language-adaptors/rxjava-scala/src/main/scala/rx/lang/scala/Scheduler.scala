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

import scala.concurrent.duration.Duration
import rx.functions.Action0
import rx.lang.scala.schedulers._
import scala.concurrent.duration
import rx.lang.scala.JavaConversions._

/**
 * Represents an object that schedules units of work.
 */
trait Scheduler {

  private [scala] val asJavaScheduler: rx.Scheduler

  /**
   * Parallelism available to a Scheduler.
   *
   * This defaults to `Runtime.getRuntime().availableProcessors()` but can be overridden for use cases such as scheduling work on a computer cluster.
   *
   * @return the scheduler's available degree of parallelism.
   */
  def parallelism: Int =  asJavaScheduler.parallelism()

  /**
   * @return the scheduler's notion of current absolute time in milliseconds.
   */
  def now: Long = this.asJavaScheduler.now()

  /**
   * Retrieve or create a new [[rx.lang.scala.Worker]] that represents serial execution of actions.
   * <p>
   * When work is completed it should be unsubscribed using [[rx.lang.scala.Worker unsubscribe]].
   * <p>
   * Work on a [[rx.lang.scala.Worker]] is guaranteed to be sequential.
   *
   * @return Inner representing a serial queue of actions to be executed
   */
  def createWorker: Worker = this.asJavaScheduler.createWorker()

}

object Worker {
  def apply(worker: rx.Scheduler.Worker): Worker = new Worker { private[scala] val asJavaWorker = worker }
}

trait Worker extends Subscription {
  private [scala] val asJavaWorker: rx.Scheduler.Worker

  /**
   * Schedules an Action for execution at some point in the future.
   *
   * @param action the Action to schedule
   * @param delay time to wait before executing the action
   * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
   */
  def schedule(delay: Duration)(action: => Unit): Subscription = {
    this.asJavaWorker.schedule(
      new Action0 {
        override def call(): Unit = action
      },
      delay.length,
      delay.unit)
  }

  /**
   * Schedules an Action for execution.
   *
   * @param action the Action to schedule
   * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
   */
  def schedule(action: => Unit): Subscription = {
    this.asJavaWorker.schedule(
      new Action0 {
        override def call(): Unit = action
      }
    )
  }

  /**
   * Schedules a cancelable action to be executed periodically. This default implementation schedules
   * recursively and waits for actions to complete (instead of potentially executing long-running actions
   * concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
   *
   * @param action the Action to execute periodically
   * @param initialDelay  time to wait before executing the action for the first time
   * @param period the time interval to wait each time in between executing the action
   * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
   */
  def schedulePeriodically(initialDelay: Duration, period: Duration)(action: => Unit): Subscription = {
    this.asJavaWorker.schedulePeriodically(
      new Action0 {
        override def call(): Unit = action
      },
      initialDelay.toNanos,
      period.toNanos,
      duration.NANOSECONDS
    )
  }

  /**
   * @return the scheduler's notion of current absolute time in milliseconds.
   */
  def now: Long = this.asJavaWorker.now()
}


private [scala] object Scheduler {
  def apply(scheduler: rx.Scheduler): Scheduler = scheduler match {
    case s: rx.schedulers.TestScheduler => new TestScheduler(s)
    case s: rx.Scheduler => new Scheduler{ val asJavaScheduler = s }
  }

}


