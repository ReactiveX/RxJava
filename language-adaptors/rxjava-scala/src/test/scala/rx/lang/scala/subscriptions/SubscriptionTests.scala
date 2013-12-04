package rx.lang.scala.subscriptions

import org.junit.Assert._
import org.junit.Test
import org.scalatest.junit.JUnitSuite

import rx.lang.scala.Subscription

class SubscriptionTests extends JUnitSuite {

  @Test
  def anonymousSubscriptionCreate() {
    val subscription = Subscription{}
    assertNotNull(subscription)
  }

  @Test
  def anonymousSubscriptionDispose() {
    var unsubscribed = false
    val subscription = Subscription{ unsubscribed = true }
    assertFalse(unsubscribed)
    subscription.unsubscribe()
    assertTrue(unsubscribed)
  }

  @Test
  def emptySubscription() {
    val subscription = Subscription()
    subscription.unsubscribe()
  }

  @Test
  def booleanSubscription() {
    val subscription = BooleanSubscription()
    assertFalse(subscription.isUnsubscribed)
    subscription.unsubscribe()
    assertTrue(subscription.isUnsubscribed)
    subscription.unsubscribe()
    assertTrue(subscription.isUnsubscribed)
  }

  @Test
  def compositeSubscriptionAdd() {

    var u0 = false
    val s0 = BooleanSubscription{ u0 = true }

    var u1 = false
    val s1 = Subscription{ u1 = true }

    val composite = CompositeSubscription()

    assertFalse(composite.isUnsubscribed)

    composite += s0
    composite += s1

    composite.unsubscribe()

    assertTrue(composite.isUnsubscribed)
    assertTrue(s0.isUnsubscribed)
    assertTrue(u0)
    assertTrue(u1)

    val s2 = BooleanSubscription()
    assertFalse(s2.isUnsubscribed)
    composite += s2
    assertTrue(s2.isUnsubscribed)

  }

  @Test
  def compositeSubscriptionRemove() {

    val s0 = BooleanSubscription()
    val composite = CompositeSubscription()

    composite += s0
    assertFalse(s0.isUnsubscribed)
    composite -= s0
    assertTrue(s0.isUnsubscribed)

    composite.unsubscribe()

    assertTrue(composite.isUnsubscribed)
  }

  @Test
  def multiAssignmentSubscriptionAdd() {

      val s0 = BooleanSubscription()
      val s1 = BooleanSubscription()
      val multiple = MultipleAssignmentSubscription()

      assertFalse(multiple.isUnsubscribed)

      multiple.subscription = s0
      assertEquals(s0.asJavaSubscription, multiple.subscription.asJavaSubscription)

      multiple.subscription = s1
      assertEquals(s1.asJavaSubscription, multiple.subscription.asJavaSubscription)

      assertFalse(s0.isUnsubscribed)
      assertFalse(s1.isUnsubscribed)

      multiple.unsubscribe()

      assertTrue(multiple.isUnsubscribed)
      assertFalse(s0.isUnsubscribed)
      assertTrue(s1.isUnsubscribed)

      val s2 = BooleanSubscription()
      assertFalse(s2.isUnsubscribed)
      multiple.subscription = s2
      assertTrue(s2.isUnsubscribed)
  }

}
