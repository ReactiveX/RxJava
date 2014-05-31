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

import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.scalatest.junit.JUnitSuite

class SubscriberTests extends JUnitSuite {

  @Test def testIssue1173() {
    // https://github.com/Netflix/RxJava/issues/1173
    val subscriber = Subscriber((n: Int) => println(n))
    assertNotNull(subscriber.asJavaObserver)
    assertNotNull(subscriber.asJavaSubscription)
    assertNotNull(subscriber.asJavaSubscriber)
  }

  @Test def testUnsubscribeForSubscriber() {
    var innerSubscriber: Subscriber[Int] = null
    val o = Observable[Int](subscriber => {
      Observable[Int](subscriber => {
        innerSubscriber = subscriber
      }).subscribe(subscriber)
    })
    o.subscribe().unsubscribe()
    // If we unsubscribe outside, the inner Subscriber should also be unsubscribed
    assertTrue(innerSubscriber.isUnsubscribed)
  }

  @Test def testBlockCallbackOnlyOnce() {
    var called = false
    val o = Observable[Int](subscriber => {
      subscriber.add({ called = !called })
    })

    val subscription = o.subscribe()
    subscription.unsubscribe()
    subscription.unsubscribe()

    // Even if called multiple times, callback is only called once
    assertTrue(called)
    assertTrue(subscription.isUnsubscribed)
  }

}
