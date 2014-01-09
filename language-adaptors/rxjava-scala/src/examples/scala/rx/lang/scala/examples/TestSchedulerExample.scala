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
package rx.lang.scala.examples

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.junit.Test
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.junit.JUnitSuite

import rx.lang.scala._
import rx.lang.scala.schedulers.TestScheduler

class TestSchedulerExample extends JUnitSuite {

  @Test def testInterval() {
    val scheduler = TestScheduler()
    // Use a Java Observer for Mockito
    val observer = mock(classOf[rx.Observer[Long]])

    val o = Observable.interval(1 second, scheduler)

    // Wrap Java Observer in Scala Observer, then subscribe
    val sub = o.subscribe(Observer(observer))

    verify(observer, never).onNext(0L)
    verify(observer, never).onCompleted()
    verify(observer, never).onError(any(classOf[Throwable]))

    scheduler.advanceTimeTo(2 seconds)

    val inOrdr = inOrder(observer)
    inOrdr.verify(observer, times(1)).onNext(0L)
    inOrdr.verify(observer, times(1)).onNext(1L)
    inOrdr.verify(observer, never).onNext(2L)
    verify(observer, never).onCompleted()
    verify(observer, never).onError(any(classOf[Throwable]))

    verify(observer, never).onNext(2L)
    
    sub.unsubscribe()

    scheduler.advanceTimeTo(4 seconds)
    
    // after unsubscription we expect no further events
    verifyNoMoreInteractions(observer)
  }

}


