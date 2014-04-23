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
   * This defaults to {@code Runtime.getRuntime().availableProcessors()} but can be overridden for use cases such as scheduling work on a computer cluster.
   *
   * @return the scheduler's available degree of parallelism.
   */
  def degreeOfParallelism: Int =  asJavaScheduler.degreeOfParallelism

  /**
   * @return the scheduler's notion of current absolute time in milliseconds.
   */
  def now: Long = this.asJavaScheduler.now()

  def createWorker: Worker = this.asJavaScheduler.createWorker()

}

object Worker {
  def apply(worker: rx.Scheduler.Worker): Worker = new Worker { private[scala] val asJavaWorker = worker }
}

trait Worker extends Subscription {
  private [scala] val asJavaWorker: rx.Scheduler.Worker

  /**
   * Schedules a cancelable action to be executed in delayTime.
   */
  def schedule(action: Unit => Unit, delayTime: Duration): Subscription =
    this.asJavaWorker.schedule(
      new Action0 {
        override def call(): Unit = action()
      },
      delayTime.length,
      delayTime.unit)

  /**
   * Schedules a cancelable action to be executed immediately.
   */
  def schedule(action: Unit => Unit): Subscription = this.asJavaWorker.schedule(
    new Action0 {
      override def call(): Unit = action()
    }
  )

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


