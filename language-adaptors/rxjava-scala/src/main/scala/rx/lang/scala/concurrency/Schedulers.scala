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
package rx.lang.scala.concurrency

import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import rx.lang.scala.Scheduler
import rx.lang.scala.ImplicitFunctionConversions._

/**
 * Factory methods for creating Schedulers.
 */
object Schedulers {

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that executes work immediately on the current thread.
   */
  def immediate: Scheduler = rx.concurrency.Schedulers.immediate()

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that queues work on the current thread to be executed after the current work completes.
   */
  def currentThread: Scheduler = rx.concurrency.Schedulers.currentThread()

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that creates a new {@link Thread} for each unit of work.
   */
  def newThread: Scheduler = rx.concurrency.Schedulers.newThread

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that queues work on an `java.util.concurrent.Executor`.
   * 
   * Note that this does not support scheduled actions with a delay.
   */
  def executor(executor: Executor): Scheduler = rx.concurrency.Schedulers.executor(executor)

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that queues work on an `java.util.concurrent.ScheduledExecutorService`.
   */
  def executor(executor: ScheduledExecutorService): Scheduler = rx.concurrency.Schedulers.executor(executor)

  /**
   * Returns a [[rx.lang.scala.Scheduler]] intended for computational work.
   * 
   * The implementation is backed by a `java.util.concurrent.ScheduledExecutorService` thread-pool sized to the number of CPU cores.
   *
   * This can be used for event-loops, processing callbacks and other computational work.
   * 
   * Do not perform IO-bound work on this scheduler. Use [[rx.lang.scala.concurrency.Schedulers.threadPoolForIO]] instead.
   */
  def threadPoolForComputation: Scheduler = rx.concurrency.Schedulers.threadPoolForComputation()
  
  /**
   * [[rx.lang.scala.Scheduler]] intended for IO-bound work.
   * 
   * The implementation is backed by an `java.util.concurrent.Executor` thread-pool that will grow as needed.
   * 
   * This can be used for asynchronously performing blocking IO.
   * 
   * Do not perform computational work on this scheduler. Use [[rx.lang.scala.concurrency.Schedulers.threadPoolForComputation]] instead.
   */
  def threadPoolForIO: Scheduler = rx.concurrency.Schedulers.threadPoolForIO()

}
