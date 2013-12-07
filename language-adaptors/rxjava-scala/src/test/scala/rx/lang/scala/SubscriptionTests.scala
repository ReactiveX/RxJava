package rx.lang.scala


import org.junit.{Assert, Test}
import org.junit.Assert
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import org.mockito.Mockito._
import org.mockito.Matchers._
import rx.lang.scala.subscriptions.{SerialSubscription, MultipleAssignmentSubscription, CompositeSubscription}

class SubscriptionTests extends JUnitSuite {
  @Test
  def subscriptionCreate() {

    val subscription = Subscription()

    Assert.assertFalse(subscription.isUnsubscribed)

    subscription.unsubscribe()
    Assert.assertTrue(subscription.isUnsubscribed)
  }

  @Test
  def subscriptionUnsubscribeIdempotent() {
    var called = false

    val subscription = Subscription{ called = !called }

    Assert.assertFalse(called)
    Assert.assertFalse(subscription.isUnsubscribed)

    subscription.unsubscribe()
    Assert.assertTrue(called)
    Assert.assertTrue(subscription.isUnsubscribed)

    subscription.unsubscribe()
    Assert.assertTrue(called)
    Assert.assertTrue(subscription.isUnsubscribed)
  }

  @Test
  def compositeSubscriptionAdd() {

    val s0 = Subscription()
    val s1 = Subscription()

    val composite = CompositeSubscription()

    Assert.assertFalse(composite.isUnsubscribed)

    composite += s0
    composite += s1

    composite.unsubscribe()

    Assert.assertTrue(composite.isUnsubscribed)
    Assert.assertTrue(s0.isUnsubscribed)
    Assert.assertTrue(s0.isUnsubscribed)

    val s2 = Subscription{}
    Assert.assertFalse(s2.isUnsubscribed)
    composite += s2
    Assert.assertTrue(s2.isUnsubscribed)

  }

  @Test
  def compositeSubscriptionRemove() {

    val s0 = Subscription()
    val composite = CompositeSubscription()

    composite += s0
    Assert.assertFalse(s0.isUnsubscribed)

    composite -= s0
    Assert.assertTrue(s0.isUnsubscribed)

    composite.unsubscribe()
    Assert.assertTrue(composite.isUnsubscribed)
  }

  @Test
  def multiAssignmentSubscriptionAdd() {

      val s0 = Subscription()
      val s1 = Subscription()
      val multiple = MultipleAssignmentSubscription()

      Assert.assertFalse(multiple.isUnsubscribed)
      Assert.assertFalse(s0.isUnsubscribed)
      Assert.assertFalse(s1.isUnsubscribed)

      multiple.subscription = s0
      Assert.assertFalse(s0.isUnsubscribed)
      Assert.assertFalse(s1.isUnsubscribed)

      multiple.subscription = s1
      Assert.assertFalse(s0.isUnsubscribed)   // difference with SerialSubscription
      Assert.assertFalse(s1.isUnsubscribed)

      multiple.unsubscribe()
      Assert.assertTrue(multiple.isUnsubscribed)
      Assert.assertFalse(s0.isUnsubscribed)
      Assert.assertTrue(s1.isUnsubscribed)

      val s2 = Subscription()
      Assert.assertFalse(s2.isUnsubscribed)
      multiple.subscription = s2
      Assert.assertTrue(s2.isUnsubscribed)
      Assert.assertFalse(s0.isUnsubscribed)
  }

  @Test
  def serialSubscriptionAdd() {

    val s0 = Subscription()
    val s1 = Subscription()
    val serial = SerialSubscription()

    Assert.assertFalse(serial.isUnsubscribed)
    Assert.assertFalse(s0.isUnsubscribed)
    Assert.assertFalse(s1.isUnsubscribed)

    serial.subscription = s0
    Assert.assertFalse(s0.isUnsubscribed)
    Assert.assertFalse(s1.isUnsubscribed)

    serial.subscription = s1
    Assert.assertTrue(s0.isUnsubscribed)    // difference with MultipleAssignmentSubscription
    Assert.assertFalse(s1.isUnsubscribed)

    serial.unsubscribe()
    Assert.assertTrue(serial.isUnsubscribed)
    Assert.assertTrue(s1.isUnsubscribed)

    val s2 = Subscription()
    Assert.assertFalse(s2.isUnsubscribed)
    serial.subscription = s2
    Assert.assertTrue(s2.isUnsubscribed)
  }
}
