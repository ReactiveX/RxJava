package rx.lang.scala.subscriptions

import rx.lang.scala.Subscription
import java.util.concurrent.atomic.AtomicBoolean


object SerialSubscription {

  /**
   * Creates a [[rx.lang.scala.subscriptions.SerialSubscription]].
   */
  def apply(): SerialSubscription =  {
    new SerialSubscription(new rx.subscriptions.SerialSubscription())
  }

  /**
   * Creates a [[rx.lang.scala.subscriptions.SerialSubscription]] that invokes the specified action when unsubscribed.
   */
  def apply(unsubscribe: => Unit): SerialSubscription = {
    val s= SerialSubscription()
    s.subscription  = Subscription{ unsubscribe }
    s
  }
}

/**
 * Represents a [[rx.lang.scala.Subscription]] that can be checked for status.
 */
class SerialSubscription private[scala] (val asJavaSubscription: rx.subscriptions.SerialSubscription)
  extends Subscription {

  private val _isUnsubscribed = new AtomicBoolean(false)

  /**
   * Checks whether the subscription has been unsubscribed.
   */
  def isUnsubscribed: Boolean = _isUnsubscribed.get()

  /**
   * Unsubscribes this subscription, setting isUnsubscribed to true.
   */
  override def unsubscribe(): Unit = { super.unsubscribe(); _isUnsubscribed.set(true) }

  def subscription_=(value: Subscription): Unit = asJavaSubscription.setSubscription(value.asJavaSubscription)
  def subscription: Subscription = Subscription(asJavaSubscription.getSubscription)

}

