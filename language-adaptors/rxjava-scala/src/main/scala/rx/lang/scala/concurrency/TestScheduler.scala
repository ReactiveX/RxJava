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

import scala.concurrent.duration.Duration
import rx.lang.scala.Scheduler

/**
 * Scheduler with artificial time, useful for testing.
 * 
 * For example, you could test the `Observable.interval` operation using a `TestScheduler` as follows:
 * 
 * {{{
 * @Test def testInterval() {
 *   import org.mockito.Matchers._
 *   import org.mockito.Mockito._
 *   
 *   val scheduler = TestScheduler()
 *   val observer = mock(classOf[rx.Observer[Long]])
 *
 *   val o = Observable.interval(1 second, scheduler)
 *   val sub = o.subscribe(observer)
 *
 *   verify(observer, never).onNext(0L)
 *   verify(observer, never).onCompleted()
 *   verify(observer, never).onError(any(classOf[Throwable]))
 *
 *   scheduler.advanceTimeTo(2 seconds)
 *
 *   val inOrdr = inOrder(observer);
 *   inOrdr.verify(observer, times(1)).onNext(0L)
 *   inOrdr.verify(observer, times(1)).onNext(1L)
 *   inOrdr.verify(observer, never).onNext(2L)
 *   verify(observer, never).onCompleted()
 *   verify(observer, never).onError(any(classOf[Throwable]))
 *
 *   sub.unsubscribe();
 *   scheduler.advanceTimeTo(4 seconds)
 *   verify(observer, never).onNext(2L)
 *   verify(observer, times(1)).onCompleted()
 *   verify(observer, never).onError(any(classOf[Throwable]))
 * }
 * }}}
 */
class TestScheduler extends Scheduler {
  val asJavaScheduler = new rx.concurrency.TestScheduler

  def advanceTimeBy(time: Duration) {
    asJavaScheduler.advanceTimeBy(time.length, time.unit)
  }

  def advanceTimeTo(time: Duration) {
    asJavaScheduler.advanceTimeTo(time.length, time.unit)
  }

  def triggerActions() {
    asJavaScheduler.triggerActions()
  }
}

/**
 * Provides constructors for `TestScheduler`.
 */
object TestScheduler {
  def apply(): TestScheduler = {
    new TestScheduler
  }
}

