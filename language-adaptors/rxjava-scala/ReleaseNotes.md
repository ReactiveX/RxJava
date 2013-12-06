RxScala Release Notes
=====================

This release of the RxScala bindings builds on the previous 0.15 release to make the Rx bindings for Scala
include all Rx types. In particular this release focuses on the `Subject` and `Scheduler` types.
To makes these notes self-contained, we will start with the `Observer[T]` and `Observable[T]` traits
that lay at the heart of Rx.

In this release we have made the constructor in the companion object `Observer` and the `asJavaObserver` property
in `Observable[T]`private to the Scala bindings package:

```scala
trait Observer[-T] {
  private [scala] def asJavaObserver: rx.Observer[_ >: T]

  def onNext(value: T): Unit
  def onError(error: Throwable): Unit
  def onCompleted(): Unit
}

private [scala] object Observer {…}
```
To create an instance of say `Observer[String]` in user code, you create a new instance of the `Observer` trait
and implement any of the methods that you care about:
```scala
   val printObserver = new Observer[String] {
      def onNext(value: String): Unit = {...}
      def onError(error: Throwable): Unit = {...}
      def onCompleted(): Unit = {...}
   }
```
Note that typically you do not need to create an `Observer` since all of the methods that accept an `Observer[T]`
(for instance `subscribe`) usually come with overloads that accept the individual methods
`onNext`, `onError`, and `onCompleted` and will automatically create an `Observer` for you.

In the future we may make the `Observer` companion object public and add overloads to this extent.

```scala
object Observable {…}
trait Observable[+T] {
   def asJavaObservable: rx.Observable[_ <: T]
}

object Observer {…}
trait Observer[-T] {
  def asJavaObserver: rx.Observer[_ >: T]
}
```


object Subject {…}
trait Subject[-T, +R] extends Observable[R] with Observer[T] {
  val asJavaSubject: rx.subjects.Subject[_ >: T, _<: R]
}

object Scheduler {…}
trait Scheduler {
   def asJavaScheduler: rx.Scheduler;
}

object Notification {…}
trait Notification[+T] {
  def asJavaNotification: rx.Notification[_ <: T]
}

object Subscription {…}
trait Subscription {
   def asJavaSubscription: rx.Subscription
}
```