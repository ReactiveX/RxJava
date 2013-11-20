package rx.lang.scala.examples

import org.junit.{Assert, Test}
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.subscriptions.{MultipleAssignmentSubscription, CompositeSubscription, BooleanSubscription, Subscription}

class SubscriptionTests extends JUnitSuite {

  @Test
  def anonymousSubscriptionCreate() {
    val subscription = Subscription{}
    Assert.assertNotNull(subscription)
  }

  @Test
  def anonymousSubscriptionDispose() {
    var unsubscribed = false
    val subscription = Subscription{ unsubscribed = true }
    Assert.assertFalse(unsubscribed)
    subscription.unsubscribe()
    Assert.assertTrue(unsubscribed)
  }

  @Test
  def emptySubscription() {
    val subscription = Subscription()
    subscription.unsubscribe()
  }

  @Test
  def booleanSubscription() {
    val subscription = BooleanSubscription()
    Assert.assertFalse(subscription.isUnsubscribed)
    subscription.unsubscribe()
    Assert.assertTrue(subscription.isUnsubscribed)
    subscription.unsubscribe()
    Assert.assertTrue(subscription.isUnsubscribed)
  }

  @Test
  def compositeSubscriptionAdd() {

    var u0 = false
    val s0 = BooleanSubscription{ u0 = true }

    var u1 = false
    val s1 = Subscription{ u1 = true }

    val composite = CompositeSubscription()

    Assert.assertFalse(composite.isUnsubscribed)

    composite += s0
    composite += s1

    composite.unsubscribe()

    Assert.assertTrue(composite.isUnsubscribed)
    Assert.assertTrue(s0.isUnsubscribed)
    Assert.assertTrue(u0)
    Assert.assertTrue(u1)

    val s2 = BooleanSubscription()
    Assert.assertFalse(s2.isUnsubscribed)
    composite += s2
    Assert.assertTrue(s2.isUnsubscribed)

  }

  @Test
  def compositeSubscriptionRemove() {

    val s0 = BooleanSubscription()
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

      val s0 = BooleanSubscription()
      val s1 = BooleanSubscription()
      val multiple = MultipleAssignmentSubscription()

      Assert.assertFalse(multiple.isUnsubscribed)

      multiple.subscription = s0
      Assert.assertEquals(s0.asJavaSubscription, multiple.subscription.asJavaSubscription)

      multiple.subscription = s1
      Assert.assertEquals(s1.asJavaSubscription, multiple.subscription.asJavaSubscription)

      Assert.assertFalse(s0.isUnsubscribed)
      Assert.assertFalse(s1.isUnsubscribed)

      multiple.unsubscribe()

      Assert.assertTrue(multiple.isUnsubscribed)
      Assert.assertFalse(s0.isUnsubscribed)
      Assert.assertTrue(s1.isUnsubscribed)

      val s2 = BooleanSubscription()
      Assert.assertFalse(s2.isUnsubscribed)
      multiple.subscription = s2
      Assert.assertTrue(s2.isUnsubscribed)
  }

}
