package rx.lang.scala


import org.junit.{Assert, Test}
import org.junit.Assert._
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import org.mockito.Mockito._
import org.mockito.Matchers._
import rx.lang.scala.subscriptions.{MultipleAssignmentSubscription, CompositeSubscription}

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
    assertTrue(s0.isUnsubscribed)

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
      assertFalse(s0.isUnsubscribed)
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
}
