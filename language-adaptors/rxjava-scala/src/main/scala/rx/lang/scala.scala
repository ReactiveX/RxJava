package rx.lang

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

package object scala {
  
  type Notification[+T] = rx.Notification[_ <: T]
  object Notification {
    def apply[T](): Notification[T] = new rx.Notification()
    def apply[T](value: T): Notification[T] = new rx.Notification(value)
    def apply[T](t: Throwable): Notification[T] = new rx.Notification(t)
  }
  
  // Observable is not here because it's a full-fledged class
  type Observer[-T] = rx.Observer[_ >: T]  
  type Scheduler = rx.Scheduler
  type Subscription = rx.Subscription
}

