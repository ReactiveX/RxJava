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
import rx.util.functions.Action1
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
   * This defaults to {@code Runtime.getRuntime().availableProcessors()} but can be overridden for use cases such as scheduling work on a computer cluster.
   *
   * @return the scheduler's available degree of parallelism.
   */
  def degreeOfParallelism: Int =  asJavaScheduler.degreeOfParallelism

  /**
   * @return the scheduler's notion of current absolute time in milliseconds.
   */
  def now: Long = this.asJavaScheduler.now()

  /**
   * Schedules a cancelable action to be executed.
   *
   * @param action Action to schedule.
   * @return a subscription to be able to unsubscribe from action.
   */
  def schedule(action: Inner => Unit): Subscription = this.asJavaScheduler.schedule(action)

  /**
   * Schedules a cancelable action to be executed periodically.
   * This default implementation schedules recursively and waits for actions to complete (instead of potentially executing
   * long-running actions concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
   *
   * @param action
   * The action to execute periodically.
   * @param initialDelay
   * Time to wait before executing the action for the first time.
   * @param period
   * The time interval to wait each time in between executing the action.
   * @return A subscription to be able to unsubscribe from action.
   */
  def schedulePeriodically(action: Inner => Unit, initialDelay: Duration, period: Duration): Subscription =
     this.asJavaScheduler.schedulePeriodically (
       new Action1[rx.Scheduler.Inner] {
         override def call(inner: rx.Scheduler.Inner): Unit = action(javaInnerToScalaInner(inner))
       },
       initialDelay.toNanos,
       period.toNanos,
       duration.NANOSECONDS
     )

  def scheduleRec(work: (=>Unit)=>Unit): Subscription = {
    Subscription(asJavaScheduler.schedule(new Action1[rx.Scheduler.Inner] {
      override def call(inner: rx.Scheduler.Inner): Unit =  work{ inner.schedule(this) }
    }))
  }
}

object Inner {
  def apply(inner: rx.Scheduler.Inner): Inner = new Inner { private[scala] val asJavaInner = inner }
}

trait Inner extends Subscription {
  private [scala] val asJavaInner: rx.Scheduler.Inner

  /**
   * Schedules a cancelable action to be executed in delayTime.
   */
  def schedule(action: Inner => Unit, delayTime: Duration): Unit =
    this.asJavaInner.schedule(
      new Action1[rx.Scheduler.Inner] {
        override def call(inner: rx.Scheduler.Inner): Unit = action(javaInnerToScalaInner(inner))
      },
      delayTime.length,
      delayTime.unit)

  /**
   * Schedules a cancelable action to be executed immediately.
   */
  def schedule(action: Inner=>Unit): Unit = this.asJavaInner.schedule(
    new Action1[rx.Scheduler.Inner]{
      override def call(inner: rx.Scheduler.Inner): Unit = action(javaInnerToScalaInner(inner))
    }
  )

  /**
   * @return the scheduler's notion of current absolute time in milliseconds.
   */
  def now: Long = this.asJavaInner.now()
}


private [scala] object Scheduler {
  def apply(scheduler: rx.Scheduler): Scheduler = scheduler match {
    case s: rx.schedulers.TestScheduler => new TestScheduler(s)
    case s: rx.Scheduler => new Scheduler{ val asJavaScheduler = s }
  }

}


