package rx.lang.scala.subscriptions

object Subscription {

  import java.util.concurrent.atomic.AtomicBoolean
  import rx.lang.scala.subscriptions._
  import rx.lang.scala.Subscription

  /**
   * Creates an [[rx.lang.scala.Subscription]] from an [[rx.Subscription]].
   */
  def apply(subscription: rx.Subscription): Subscription = {
    subscription match {
      case x: rx.subscriptions.BooleanSubscription => new BooleanSubscription(x)
      case x: rx.subscriptions.CompositeSubscription => new CompositeSubscription(x)
      case x: rx.subscriptions.MultipleAssignmentSubscription => new MultipleAssignmentSubscription(x)
      case x: rx.subscriptions.SerialSubscription => new SerialSubscription(x)
      case x: rx.Subscription => Subscription { x.unsubscribe() }
    }
  }

  /**
   * Creates an [[rx.lang.scala.Subscription]] that invokes the specified action when unsubscribed.
   */
  def apply(u: => Unit): Subscription = {
    new Subscription() {

      private val unsubscribed = new AtomicBoolean(false)
      def isUnsubscribed = unsubscribed.get()

      val asJavaSubscription = new rx.Subscription {
        def unsubscribe() { if(!unsubscribed.get()) { u ; unsubscribed.set(true) }}
      }
    }
  }

}