package rx.lang.scala.subscriptions

import rx.lang.scala._

object CompositeSubscription {

  /**
   * Creates a [[rx.lang.scala.subscriptions.CompositeSubscription]] from a group of [[rx.lang.scala.Subscription]].
   */
  def apply(subscriptions: Subscription*): CompositeSubscription = {
    new CompositeSubscription(new rx.subscriptions.CompositeSubscription(subscriptions.map(_.asJava).toArray : _*))
  }

  /**
   * Creates a [[rx.lang.scala.subscriptions.CompositeSubscription]].
   */
  def apply(): CompositeSubscription = {
    new CompositeSubscription(new rx.subscriptions.CompositeSubscription())
  }

  /**
   * Creates a [[rx.lang.scala.subscriptions.CompositeSubscription]].
   */
  def apply(subscription: rx.subscriptions.CompositeSubscription): CompositeSubscription = {
    new CompositeSubscription(subscription)
  }
}

/**
 * Represents a group of [[rx.lang.scala.Subscription]] that are disposed together.
 */
class CompositeSubscription private[scala] (val asJava: rx.subscriptions.CompositeSubscription)
  extends Subscription
{
  /**
   * Adds a subscription to the group,
   * or unsubscribes immediately is the [[rx.subscriptions.CompositeSubscription]] is unsubscribed.
   * @param subscription the subscription to be added.
   * @return the [[rx.subscriptions.CompositeSubscription]] itself.
   */
  def +=(subscription: Subscription): this.type = {
    asJava.add(subscription.asJava)
    this
  }

  /**
   * Removes and unsubscribes a subscription to the group,
   * @param subscription the subscription be removed.
   * @return the [[rx.subscriptions.CompositeSubscription]] itself.
   */
  def -=(subscription: Subscription): this.type = {
    asJava.remove(subscription.asJava)
    this
  }

  /**
   * Checks whether the subscription has been unsubscribed.
   */
  def isUnsubscribed: Boolean = asJava.isUnsubscribed

}
