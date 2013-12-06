RxScala Release Notes
=====================

This release of the RxScala bindings builds on the previous 0.15 release to make the Rx bindings for Scala
include all Rx types. In particular this release focuses on the `Subject` and `Scheduler` types.
To makes these notes self-contained, we will start with the `Observer[T]` and `Observable[T]` traits
that lay at the heart of Rx.

Observer
--------

In this release we have made the `asJavaObserver` property in `Observable[T]`as well the the factory method in the
 companion object that takes an `rx.Observer` private to the Scala bindings package.

```scala
trait Observer[-T] {
  private [scala] def asJavaObserver: rx.Observer[_ >: T]

  def onNext(value: T): Unit
  def onError(error: Throwable): Unit
  def onCompleted(): Unit
}

object Observer {…}
```

To create an instance of say `Observer[String]` in user code, you can create a new instance of the `Observer` trait
and implement any of the methods that you care about:
```scala
   val printObserver = new Observer[String] {
      override def onNext(value: String): Unit = {...}
      override def onError(error: Throwable): Unit = {...}
      override def onCompleted(): Unit = {...}
   }
```
 or you can use one of the overloads of the companion `Observer` object by passing in implementations of the `onNext`,
 `onError` or `onCompleted` methods. The advantage of this is that you get type inference as in `Observer(println(_))`.

Note that typically you do not need to create an `Observer` since all of the methods that accept an `Observer[T]`
(for instance `subscribe`) usually come with overloads that accept the individual methods
`onNext`, `onError`, and `onCompleted` and will automatically create an `Observer` for you under the covers.

While *technically* it is a breaking change make the `asJavaObserver` property
private, you should probably not have touched `asjavaObserver` in the first place.

Observable
----------

Just like for `Observer`, the `Observable` trait now also hides its `asJavaObservable` property and makes the constructor
function in the companion object that takes an `rx.Observable` private (but leaves the companion object itself public).
Again, while *technically* this is a breaking change, this should not have any influence on user code.

```scala
trait Observable[+T] {
   private [scala] def asJavaObservable: rx.Observable[_ <: T]
}

object Observable {
   private [scala] def apply[T](observable: rx.Observable[_ <: T]): Observable[T] = {...}
}
```

The major changes in `Observable` are wrt to the factory methods *Can't write this when I don't have Samuel's changes*.

Subject
-------

The `Subject` trait now also hides the underlying Java `asJavaSubject: rx.subjects.Subject[_ >: T, _<: R]`.

```scala
trait Subject[-T, +R] extends Observable[R] with Observer[T] {
  private [scala] val asJavaSubject: rx.subjects.Subject[_ >: T, _<: R]
}
```

*I will remove PublishSubject making it *Subject* There is no companion object for `Subject` but instead*
For each kind of subject, there is a pair of a companion object and a class with a private constructor:

```scala
object XXXSubject {
  def apply[T](...): XXXSubject[T] = {
    new XXXSubject[T](... create corresponding rx subject ...)
  }
}

class XXXSubject[T] private[scala] (val asJavaSubject: rx.subjects.XXXSubject[T]) extends Subject[T,T] {}
```

The subjects that are available are:

* `AsyncSubject[T]()`
* `BehaviorSubject[T](value)`
* `Subject[T]()`
* `ReplaySubject[T]()`

The latter is still missing various overloads http://msdn.microsoft.com/en-us/library/hh211810(v=vs.103).aspx which
you can expect to appear once they are added to the underlying RxJava implementation.

Compared with release 0.15.1 there are no breaking changes in `Subject` for this release, except for
making `asJavaSubject` private.

Schedulers
----------

The biggest breaking change compared to the 0.15.1 release is giving `Scheduler` the same structure as the other types.
The trait itself remains unchanged, except that we made the underlying Java representation hidden as above.
The scheduler package has been renamed from `rx.lang.scala.schedulers` to `rx.lang.scala.schedulers`.

```scala
trait Scheduler {
   private[scala] def asJavaScheduler: rx.Scheduler;
}

private [scala] object Scheduler {…}
```

In the previous release, you created schedulers by selecting them from the `Schedulers` object,
as in `Schedulers.immediate` or `Schedulers.newThread` where each would return an instance of the `Scheduler` trait.
However, several of the scheduler implementations have additional methods, such as the `testScheduler`,
which already deviated from the pattern.

In this release, we changed this to make scheduler more like `Subject` and provide a family of schedulers
that you create using their factory function:

* `CurrentThreadScheduler()`
* `ExecutorScheduler(executor)`
* `ImmediateScheduler()`
* `NewThreadScheduler()`
* `ScheduledExecutorServiceScheduler(scheduledExecutorService)`
* `TestScheduler()`
* `ThreadPoolForComputationScheduler()`
* `ThreadPoolForIOScheduler()`

In the future we expect that this list will grow further with new schedulers as they are imported from .NET
(http://msdn.microsoft.com/en-us/library/system.reactive.concurrency(v=vs.103).aspx).

To make your code compile in the new release you will have to change all occurrences of `Schedulers.xxx`
into `XxxScheduler()`, and import `rx.lang.scala.schedulers` instead of `rx.lang.scala.schedulers`.

Subscriptions
-------------

The `Subscription` trait in Scala now has `isUnsubscribed` as a member, effectively collapsing the old `Subscription`
and `BooleanSubscription`, and the latter has been removed from the public surface. Pending a bugfix in RxJava,
`SerialSubscription` implements its own `isUnsubscribed`.


```scala
trait Subscription {

  private [scala] val asJavaSubscription: rx.Subscription = {...}
  private [scala] val unsubscribed = new AtomicBoolean(false)

  def unsubscribe(): Unit = { ... }
  def isUnsubscribed: Boolean = ...
}

object Subscription {...}
 ```

 To create a `Subscription` use one of the following factory methods:

 * `Subscription{...}`, `Subscription()`
 * `CompositeSubscription(subscriptions)`
 * `MultipleAssignmentSubscription`
 * `SerialSubscription`

 In case you do feel tempted to call `new Subscription{ ...}` directly make sure you wire up `isUnsubscribed`
 and with the `unsubscribed` field properly, but for all practical purposes you should just use one of the factory methods.

Notifications
-------------

All underlying wrapped `Java` types in the `Notification` trait are made private like all previous types. The companion
`Notification` now has both constructor and destructor functions:

```scala
object Notification {…}
trait Notification[+T] {
  private [scala] def asJavaNotification: rx.Notification[_ <: T]
}

object Notification {
   object OnNext { def apply(...){}; def unapply(..){...} }
   object OnError { def apply(...){}; def unapply(..){...} }
   object OnCompleted { def apply(...){}; def unapply(..){...} }
}
```
To construct a `Notification`, you use `Notification.OnNext("hello")`, or `Notification.OnError`(new Exception("Oops!"))`.
To pattern match on a notification you can create a partial function like so: `case OnNext(v) => { ... v ... }`.

There are no breaking changes for notifications.
