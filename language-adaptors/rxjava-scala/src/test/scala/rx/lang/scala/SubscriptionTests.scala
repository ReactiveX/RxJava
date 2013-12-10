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


import org.junit.{Assert, Test}
import org.junit.Assert
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import rx.lang.scala.subscriptions.{SerialSubscription, MultipleAssignmentSubscription, CompositeSubscription}



class SubscriptionTests extends JUnitSuite {
  @Test
  def subscriptionCreate() {

    val subscription = Subscription()

      assertFalse(subscription.isUnsubscribed)

    subscription.unsubscribe()

      assertTrue(subscription.isUnsubscribed)
  }

  @Test
  def subscriptionUnsubscribeIdempotent() {

    var called = false

    val subscription = Subscription{ called = !called }

      assertFalse(called)
      assertFalse(subscription.isUnsubscribed)

    subscription.unsubscribe()

      assertTrue(called)
      assertTrue(subscription.isUnsubscribed)

    subscription.unsubscribe()

      assertTrue(called)
      assertTrue(subscription.isUnsubscribed)
  }

  @Test
  def compositeSubscriptionAdd() {

    val s0 = Subscription()
    val s1 = Subscription()

    val composite = CompositeSubscription()

      assertFalse(composite.isUnsubscribed)

    composite += s0
    composite += s1

    composite.unsubscribe()

      assertTrue(composite.isUnsubscribed)
      assertTrue(s0.isUnsubscribed)
      assertTrue(s1.isUnsubscribed)

    val s2 = Subscription{}

      assertFalse(s2.isUnsubscribed)

    composite += s2

      assertTrue(s2.isUnsubscribed)

  }

  @Test
  def compositeSubscriptionRemove() {

    val s0 = Subscription()
    val composite = CompositeSubscription()

    composite += s0
      assertFalse(s0.isUnsubscribed)

    composite -= s0
      assertTrue(s0.isUnsubscribed)

    composite.unsubscribe()

      assertTrue(composite.isUnsubscribed)
      assertTrue(s0.isUnsubscribed)
  }

  @Test
  def multiAssignmentSubscriptionAdd() {

      val s0 = Subscription()
      val s1 = Subscription()
      val multiple = MultipleAssignmentSubscription()

        assertFalse(multiple.isUnsubscribed)
        assertFalse(s0.isUnsubscribed)
        assertFalse(s1.isUnsubscribed)

      multiple.subscription = s0

        assertFalse(s0.isUnsubscribed)
        assertFalse(s1.isUnsubscribed)

      multiple.subscription = s1

        assertFalse(s0.isUnsubscribed)   // difference with SerialSubscription
        assertFalse(s1.isUnsubscribed)

      multiple.unsubscribe()

        assertTrue(multiple.isUnsubscribed)
        assertFalse(s0.isUnsubscribed)
        assertTrue(s1.isUnsubscribed)

      val s2 = Subscription()

        assertFalse(s2.isUnsubscribed)

      multiple.subscription = s2

        assertTrue(s2.isUnsubscribed)
        assertFalse(s0.isUnsubscribed)
  }

  @Test
  def serialSubscriptionAdd() {

    val s0 = Subscription()
    val s1 = Subscription()
    val serial = SerialSubscription()

      assertFalse(serial.isUnsubscribed)
      assertFalse(s0.isUnsubscribed)
      assertFalse(s1.isUnsubscribed)

    serial.subscription = s0

      assertFalse(s0.isUnsubscribed)
      assertFalse(s1.isUnsubscribed)

    serial.subscription = s1

      assertTrue(s0.isUnsubscribed)    // difference with MultipleAssignmentSubscription
      assertFalse(s1.isUnsubscribed)

    serial.unsubscribe()

      assertTrue(serial.isUnsubscribed)
      assertTrue(s1.isUnsubscribed)

    val s2 = Subscription()

      assertFalse(s2.isUnsubscribed)

    serial.subscription = s2

      assertTrue(s2.isUnsubscribed)
  }

}
