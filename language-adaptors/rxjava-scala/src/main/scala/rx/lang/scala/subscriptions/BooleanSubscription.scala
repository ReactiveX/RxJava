package rx.lang.scala.subscriptions

import rx.lang.scala._

object BooleanSubscription {

  /**
   * Creates a [[rx.lang.scala.subscriptions.BooleanSubscription]].
   */
  def apply(): BooleanSubscription =  {
    new BooleanSubscription(new rx.subscriptions.BooleanSubscription())
  }

  /**
   * Creates a [[rx.lang.scala.subscriptions.BooleanSubscription]] that invokes the specified action when unsubscribed.
   */
  def apply(u: => Unit): BooleanSubscription = {
    new BooleanSubscription(new rx.subscriptions.BooleanSubscription {
      override def unsubscribe(): Unit = {
        if(!super.isUnsubscribed()) {
            u
            super.unsubscribe()
        }
      }
    })
  }
}

/**
 * Represents a [[rx.lang.scala.Subscription]] that can be checked for status.
 */
class BooleanSubscription private[scala] (val asJavaSubscription: rx.subscriptions.BooleanSubscription)
  extends Subscription {

  /**
   * Checks whether the subscription has been unsubscribed.
   */
  def isUnsubscribed: Boolean = asJavaSubscription.isUnsubscribed

}
