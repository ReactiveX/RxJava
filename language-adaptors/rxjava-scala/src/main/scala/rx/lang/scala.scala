package rx.lang


/*
 * This object contains aliases to all types Scala users need to import.
 * Note that:
 * -  Scala users cannot use Java's type with variance without always using writing
 *    e.g. rx.Notification[_ <: T], so we create aliases fixing the variance
 * -  For consistency, we create aliases for all types
 * -  Type aliases cannot be at top level, they have to be inside an object or class
 * -  It's possible to make a "package object" instead of "object", but if there's a
 *    package with the same name as the package object, the gradle builder fails
 *    (the eclipse builder works). Using -Yresolve-term-conflict:package
 *    or -Yresolve-term-conflict:object as scalac options didn't help.
 *    See also http://stackoverflow.com/questions/8984730/package-contains-object-and-package-with-same-name
 */
object scala {

  type Notification[+T] = rx.Notification[_ <: T]
  object Notification {
    def apply[T](): Notification[T] = new rx.Notification()
    def apply[T](value: T): Notification[T] = new rx.Notification(value)
    def apply[T](t: Throwable): Notification[T] = new rx.Notification(t)
  }
  
  type Observable[+T] = rx.lang.scalaimpl.Observable[T]
  val Observable = rx.lang.scalaimpl.Observable
  type Observer[-T] = rx.Observer[_ >: T]  
  type Scheduler = rx.Scheduler
  type Subscription = rx.Subscription

  object util {
    type Closing = rx.util.Closing

    // TODO rx.util.Closings

    type CompositeException = rx.util.CompositeException

    // TODO rx.util.Exceptions

    // rx.util.OnErrorNotImplementedException TODO what's this?

    type Opening = rx.util.Opening

    // rx.util.Openings // TODO

    // rx.util.Range // TODO do we need this? Or the Scala Range?

    type Timestamped[+T] = rx.util.Timestamped[_ <: T]
    object Timestamped {
      def apply[T](timestampMillis: Long, value: T): Timestamped[T] = {
        new rx.util.Timestamped(timestampMillis, value)
      }
    }
  }
  
}

/*

TODO make aliases for these types because:
* those which are covariant or contravariant do need an alias to get variance correct
* the others for consistency

rx.concurrency.CurrentThreadScheduler
rx.concurrency.ExecutorScheduler
rx.concurrency.ImmediateScheduler
rx.concurrency.NewThreadScheduler
rx.concurrency.Schedulers
rx.concurrency.TestScheduler

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

