RxScala Release Notes
=====================

This release of the RxScala bindings builds on the previous 0.15 release to make the Rx bindings for Scala
include all Rx types. In particular this release focuses on fleshing out the bindings for the `Subject` and `Scheduler` types.
To makes these notes self-contained, we will start with the `Observer[T]` and `Observable[T]` traits
that lay at the heart of Rx.

Observer
--------

In this release we have made the `asJavaObserver` property in `Observable[T]`as well the the factory method in the
 companion object that takes an `rx.Observer` private to the Scala bindings package, thus properly hiding irrelevant
 implementation details from the user-facing API. The `Observer[T]` trait now looks like a clean, native Scala type:

```scala
trait Observer[-T] {
  def onNext(value: T): Unit
  def onError(error: Throwable): Unit
  def onCompleted(): Unit
}

object Observer {...}
```

To create an instance of a specific `Observer`, say  `Observer[SensorEvent]` in user code, you can create a new instance
of the `Observer` trait by implementing any of the methods that you care about:
```scala
   val printObserver = new Observer[SensorEvent] {
      override def onNext(value: SensorEvent): Unit = {...value.toString...}
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
    def subscribe(observer: Observer[T]): Subscription = {...}
    def apply(observer: Observer[T]): Subscription = {...}
    ...
}
object Observable {
   def create[T](func: Observer[T] => Subscription): Observable[T] = {...}
   ...
}
```

The major changes in `Observable` are wrt to the factory methods where too libral use of overloading of the `apply`
method hindered type inference and made Scala code look unnecessarily different than that in other language bindings.
All factory methods now have their own name corresponding to the Java and .NET operators
(plus overloads that take a `Scheduler`).

* `def from[T](future: Future[T]): Observable[T]`
* `def from[T](iterable: Iterable[T]): Observable[T]`
* `def error[T](exception: Throwable): Observable[T]`
* `def empty[T]: Observable[T]`
* `def items[T](items: T*): Observable[T]

In the *pre-release* of this version, we expose both `apply` and `create` for the mother of all creation functions.
We would like to solicit feedback which of these two names is preferred
(or both, but there is a high probability that only one will be chosen).

* `def apply[T](subscribe: Observer[T]=>Subscription): Observable[T]`
* `def create[T](subscribe: Observer[T] => Subscription): Observable[T]`

Subject
-------

The `Subject` trait now also hides the underlying Java `asJavaSubject: rx.subjects.Subject[_ >: T, _<: T]`
and takes only a single *invariant* type parameter `T`. all existing implementations of `Subject` are parametrized
by a single type, and this reflects that reality.

```scala
trait Subject[T] extends Observable[T] with Observer[T] {}
object Subject {
   def apply(): Subject[T] = {...}
}
```
For each kind of subject, there is a class with a private constructor and a companion object that you should use
to create a new kind of subject. The subjects that are available are:

* `AsyncSubject[T]()`
* `BehaviorSubject[T](value)`
* `Subject[T]()`
* `ReplaySubject[T]()`

The latter is still missing various overloads http://msdn.microsoft.com/en-us/library/hh211810(v=vs.103).aspx which
you can expect to appear once they are added to the underlying RxJava implementation.

Compared with release 0.15.1, the breaking changes in `Subject` for this release are
making `asJavaSubject` private, and collapsing its type parameters, neither of these should cause trouble,
and renaming `PublishSubject` to `Subject`.

Schedulers
----------

The biggest breaking change compared to the 0.15.1 release is giving `Scheduler` the same structure as the other types.
The trait itself remains unchanged, except that we made the underlying Java representation hidden as above.
as part of this reshuffling, the scheduler package has been renamed from `rx.lang.scala.concurrency`
to `rx.lang.scala.schedulers`. There is a high probability that this package renaming will also happen in RxJava.

```scala
trait Scheduler {...}
```

In the previous release, you created schedulers by selecting them from the `Schedulers` object,
as in `Schedulers.immediate` or `Schedulers.newThread` where each would return an instance of the `Scheduler` trait.
However, several of the scheduler implementations have additional methods, such as the `TestScheduler`,
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
and `BooleanSubscription`, and the latter has been removed from the public surface. Pending a bug fix in RxJava,
`SerialSubscription` implements its own `isUnsubscribed`.


```scala
trait Subscription {
  def unsubscribe(): Unit = { ... }
  def isUnsubscribed: Boolean = ...
}

object Subscription {...}
 ```

 To create a `Subscription` use one of the following factory methods:

 * `Subscription{...}`, `Subscription()`
 * `CompositeSubscription(subscriptions)`
 * `MultipleAssignmentSubscription()`
 * `SerialSubscription()`

 In case you do feel tempted to call `new Subscription{...}` directly make sure you wire up `isUnsubscribed`
 and `unsubscribe()` properly, but for all practical purposes you should just use one of the factory methods.

Notifications
-------------

All underlying wrapped `Java` types in the `Notification` trait are made private like all previous types. The companion
objects of `Notification` now have both constructor (`apply`) and extractor (`unapply`) functions:

```scala
object Notification {...}
trait Notification[+T] {
   override def equals(that: Any): Boolean = {...}
   override def hashCode(): Int = {...}
   def apply[R](onNext: T=>R, onError: Throwable=>R, onCompleted: ()=>R): R = {...}
}
```
The nested companion objects of `Notification` now have both constructor (`apply`) and extractor (`unapply`) functions:
```scala
object Notification {
   object OnNext { def apply(...){}; def unapply(...){...} }
   object OnError { def apply(...){}; def unapply(...){...} }
   object OnCompleted { def apply(...){}; def unapply(...){...} }
}
```
To construct a `Notification`, you import `rx.lang.scala.Notification._` and use `OnNext("hello")`,
or `OnError(new Exception("Oops!"))`, or `OnCompleted()`.

To pattern match on a notification you create a partial function like so: `case Notification.OnNext(v) => { ... v ... }`,
or you use the `apply` function to pass in functions for each possibility.

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