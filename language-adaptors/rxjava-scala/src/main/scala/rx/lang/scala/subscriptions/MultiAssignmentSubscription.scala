package rx.lang.scala.subscriptions

import rx.lang.scala._

object MultipleAssignmentSubscription {

  /**
   * Creates a [[rx.lang.scala.subscriptions.MultipleAssignmentSubscription]] that invokes the specified action when unsubscribed.
   */
  def apply(subscription: => Unit): MultipleAssignmentSubscription = {
    val m = MultipleAssignmentSubscription()
    m.subscription = Subscription{ subscription }
    m
  }

  /**
   * Creates a [[rx.lang.scala.subscriptions.MultipleAssignmentSubscription]].
   */
  def apply(): MultipleAssignmentSubscription = {
    new MultipleAssignmentSubscription(new rx.subscriptions.MultipleAssignmentSubscription())
  }
}



/**
 * Represents a [[rx.lang.scala.Subscription]] whose underlying subscription can be swapped for another subscription.
 */
class MultipleAssignmentSubscription private[scala] (val asJavaSubscription: rx.subscriptions.MultipleAssignmentSubscription)
  extends Subscription {

  /**
   * Gets the underlying subscription.
   */
  def subscription: Subscription = Subscription(asJavaSubscription.getSubscription)

  /**
   * Gets the underlying subscription
   * @param that the new subscription
   * @return the [[rx.lang.scala.subscriptions.MultipleAssignmentSubscription]] itself.
   */
  def subscription_=(that: Subscription): this.type = {
    asJavaSubscription.setSubscription(that.asJavaSubscription)
    this
  }

  /**
   * Checks whether the subscription has been unsubscribed.
   */
  def isUnsubscribed: Boolean = asJavaSubscription.isUnsubscribed

}


