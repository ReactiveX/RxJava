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

object Observer {...}
```

To create an instance of say `Observer[SensorEvent]` in user code, you can create a new instance of the `Observer` trait
and implement any of the methods that you care about:
```scala
   val printObserver = new Observer[SensorEvent] {
      override def onNext(value: SensorEvent): Unit = {...value.toString...}
      override def onError(error: Throwable): Unit = {...}
      override def onCompleted(): Unit = {...}
   }
```
 or you can use one of the overloads of the companion `Observer` object by passing in implementations of the `onNext`,
 `onError` or `onCompleted` methods.

Note that typically you do not need to create an `Observer` since all of the methods that accept an `Observer[T]`
(for instance `subscribe`) usually come with overloads that accept the individual methods
`onNext`, `onError`, and `onCompleted` and will automatically create an `Observer` for you under the covers.

While *technically* it is a breaking change make the `asJavaObserver` property private, you should probably not have
touched `asJavaObserver` in the first place. If you really feel you need to access the underlying `rx.Observer`
call `toJava`.

Observable
----------

Just like for `Observer`, the `Observable` trait now also hides its `asJavaObservable` property and makes the constructor
function in the companion object that takes an `rx.Observable` private (but leaves the companion object itself public).
Again, while *technically* this is a breaking change, this should not have any influence on user code.

```scala
trait Observable[+T] {
   private [scala] val asJavaObservable: rx.Observable[_ <: T]
}

object Observable {
   private [scala] def apply[T](observable: rx.Observable[_ <: T]): Observable[T] = {...}
}
```

The major changes in `Observable` are wrt to the factory methods where too libral use of overloading of the `apply`
method hindered type inference and made Scala code look unnecessarily different than that in other language bindings.
In fact the only occurence left of `apply` if for the varargs case. All other factory methods now have their own name.

* `def apply[T](items: T*): Observable[T]`
* `def from[T](f: Future[T]): Observable[T]`
* `def from[T](iterable: Iterable[T]): Observable[T]`
* `def create[T](subscribe: Observer[T] => Subscription): Observable[T]`
* `def error[T](exception: Throwable): Observable[T]`

Subject
-------

The `Subject` trait now also hides the underlying Java `asJavaSubject: rx.subjects.Subject[_ >: T, _<: T]`
and takes only a single *invariant* type parameter `T`. all existing implementations of `Subject` are parametrized
by a single type, and this reflects that reality.

```scala
trait Subject[T] extends Observable[T] with Observer[T] {
  private [scala] val asJavaSubject: rx.subjects.Subject[_ >: T, _<: T]
}
```
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
making `asJavaSubject` private, and collapsing its type parameters. Neither of these should cause trouble.

Schedulers
----------

The biggest breaking change compared to the 0.15.1 release is giving `Scheduler` the same structure as the other types.
The trait itself remains unchanged, except that we made the underlying Java representation hidden as above.
The scheduler package has been renamed from `rx.lang.scala.concurrency` to `rx.lang.scala.schedulers`.

```scala
trait Scheduler {
   private[scala] val asJavaScheduler: rx.Scheduler;
}

private [scala] object Scheduler {...}
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

 In case you do feel tempted to call `new Subscription{...}` directly make sure you wire up `isUnsubscribed`
 and with the `unsubscribed` field properly, but for all practical purposes you should just use one of the factory methods.

Notifications
-------------

All underlying wrapped `Java` types in the `Notification` trait are made private like all previous types. The companion
objects of `Notification` now have both constructor (`apply`) and extractor (`unapply`) functions:

```scala
object Notification {...}
trait Notification[+T] {
  private [scala] val asJavaNotification: rx.Notification[_ <: T]
}

object Notification {
   object OnNext { def apply(...){}; def unapply(...){...} }
   object OnError { def apply(...){}; def unapply(...){...} }
   object OnCompleted { def apply(...){}; def unapply(...){...} }
}
```
To construct a `Notification`, you import `rx.lang.scala.Notification._` and use `OnNext("hello")`,
or `OnError(new Exception("Oops!"))`, or `OnCompleted()`.

To pattern match on a notification you can create a partial function like so: `case OnNext(v) => { ... v ... }`.

There are no breaking changes for notifications.

Java Interop Helpers
--------------------

Since the Scala traits *wrap* the underlying Java types, yoo may occasionally will have to wrap an unwrap
between the two representations. The `JavaConversion` object provides helper functions of the form `toJavaXXX` and
`toScalaXXX` for this purpose, properly hiding how precisely the wrapped types are stored.
Note the (un)wrap conversions are defined as implicits in Scala, but in the unlikely event that you do need them
be kind to the reader of your code and call them explicitly.

```scala
object JavaConversions {
  import language.implicitConversions

  implicit def toJavaNotification[T](s: Notification[T]): rx.Notification[_ <: T] = {...}
  implicit def toScalaNotification[T](s: rx.Notification[_ <: T]): Notification[T] = {...}
  implicit def toJavaSubscription(s: Subscription): rx.Subscription = {...}
  implicit def toScalaSubscription(s: rx.Subscription): Subscription = {...}
  implicit def scalaSchedulerToJavaScheduler(s: Scheduler): rx.Scheduler = {...}
  implicit def javaSchedulerToScalaScheduler(s: rx.Scheduler): Scheduler = {...}
  implicit def toJavaObserver[T](s: Observer[T]): rx.Observer[_ >: T] = {...}
  implicit def toScalaObserver[T](s: rx.Observer[_ >: T]): Observer[T] = {...}
  implicit def toJavaObservable[T](s: Observable[T]): rx.Observable[_ <: T] = {...}
  implicit def toScalaObservable[T](observable: rx.Observable[_ <: T]): Observable[T] = {...}
}
```