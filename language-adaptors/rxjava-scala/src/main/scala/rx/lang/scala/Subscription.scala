package rx.lang.scala

/**
 * Subscriptions are returned from all `Observable.subscribe` methods to allow unsubscribing.
 *
 * This interface is the equivalent of `IDisposable` in the .NET Rx implementation.
 */
trait Subscription extends JavaWrapper[rx.Subscription] {

  /**
   * Call this method to stop receiving notifications on the Observer that was registered when
   * this Subscription was received.
   */
  def unsubscribe(): Unit = {
    asJava.unsubscribe()
  }
}

object Subscription {
  private[Subscription] class SubscriptionWrapper(val asJava: rx.Subscription) extends Subscription {}
  
  def apply(asJava: rx.Subscription): Subscription = {
    // no need to care if it's a subclass of rx.Subscription 
    new SubscriptionWrapper(asJava)
  }

  private[Subscription] class SubscriptionFromFunc(unsubscribe: => Unit) extends Subscription {
    val asJava: rx.Subscription = rx.subscriptions.Subscriptions.create(
        ImplicitFunctionConversions.scalaFunction0ProducingUnitToAction0(unsubscribe))
  }
  
  /**
   * Creates an [[rx.lang.scala.Subscription]] that invokes the specified action when unsubscribed.
   */
  def apply(u: => Unit): Subscription  = {
    new SubscriptionFromFunc(u)
  }
  
}
