package rx.lang


/*
 * This object contains aliases to all types Scala users need to import.
 * Note that:
 * -  Scala users cannot use Java's type with variance without always using writing
 *    e.g. rx.Notification[_ <: T], so we create aliases fixing the variance
 * -  For consistency, we create aliases for all types
 * -  Type aliases cannot be at top level, they have to be inside an object or class
 */
package object scala {

  type Notification[+T] = rx.Notification[_ <: T]
  object Notification {
    def apply[T](): Notification[T] = new rx.Notification()
    def apply[T](value: T): Notification[T] = new rx.Notification(value)
    def apply[T](t: Throwable): Notification[T] = new rx.Notification(t)
  }
  
  type Observer[-T] = rx.Observer[_ >: T]  
  type Scheduler = rx.Scheduler
  type Subscription = rx.Subscription
  
}

/*

TODO make aliases for these types because:
* those which are covariant or contravariant do need an alias to get variance correct
* the others for consistency

rx.observables.BlockingObservable
rx.observables.ConnectableObservable
rx.observables.GroupedObservable

rx.plugins.RxJavaErrorHandler
rx.plugins.RxJavaObservableExecutionHook
rx.plugins.RxJavaPlugins

rx.subjects.AsyncSubject
rx.subjects.BehaviorSubject
rx.subjects.PublishSubject
rx.subjects.ReplaySubject
rx.subjects.Subject

rx.subscriptions.BooleanSubscription
rx.subscriptions.CompositeSubscription
rx.subscriptions.Subscriptions

*/

