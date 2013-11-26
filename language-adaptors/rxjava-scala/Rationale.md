Alternative Rx bindings for Scala
=================================

The current RxScala binding attempt to optimize for seamless interop between Scala and Java.
The intended interop is illustrated by the following example where in Scala a class is defined that takes
an `Observable[Movie]` that is transformed using RxScala operators:
```scala
class MovieLib(val moviesStream: Observable[Movie]) {
   val threshold = 1200
   def shortMovies: Observable[Movie] = ???
   def longMovies: Observable[Movie] = ???
}
```
which is then called in Java, passing a Java `Observable<Movie>` to the constructor
```java
public void test() {
   MovieLib lib = new MovieLib(Observable.from(...));

   lib.longMovies().subscribe(moviePrinter);
}
```
The technique used to obtain this transparency is to use a value class with a private constructor that implements
the Rx operators in an idiomatic Scala way, and a companion object that is used to construct instances in Scala
```scala
object Observable {
   def apply[T](asJava: rx.Observable[_ <: T]): Observable[T] = { new Observable[T](asJava) }
}

class Observable[+T] private[scala] (val asJava: rx.Observable[_ <: T])  extends AnyVal {
   // Idiomatic Scala friendly definitions of Rx operators
}
```
Since `rx.lang.scala.Observable[T] extends AnyVal`, the underlying representation of `rx.lang.scala.Observable[T]`
is the same as `rx.Observable<T>`. Because `rx.lang.scala.Observable[T]` is an opaque type in Scala,
the Scala programmer only sees the Scala-friendly operators.

However, in the current the illusion of interop is quickly lost when going beyond this simple example.
For example but type `Notification[T]` and `Scheduler[T]` are defined using wrappers,
and hence they are not compatible with `Notification<T>` respectively `Scheduler<T>`.
For instance, when materializing an `Observable[T]` in Scala to an `Observable[Notification[T]]`,
we lost the seamless interop with `Observable<Notification<T>>` on the Java side.

However, the real problems with seamless interop show up when we try to creating bindings for other Rx types.
In particular types that have inheritance or more structure.

For example, RxScala currently defines a type synonym `type Observer[-T] = rx.Observer[_ >: T]`,
but no further bindings for observers.
Similarly, for subjects RxScala defines `type Subject[-T, +R] = rx.subjects.Subject[_ >: T, _ <: R]`.
The problem with these definitions is that on the Java side, subjects are defined as:
```scala
public abstract class Subject<T, R> extends Observable<R> implements Observer<T> { …}
```
without binding any of the Rx subjects.

The consequence is that `Subject[S,T]` in Scala is unrelated to `rx.lang.scala.Observable[T]` in Scala,
but shows up as a `rx.Observable[T]`. The problem however is that if we want to expose subjects in Scala
such that they derive from both `Observable[S]` and `Observer[T]` we cannot use the `extend AnyVal` trick
we used for `Observable[T]` and immediately lose transparent interop with Java.

The problem is even worse because `AsyncSubject<T>`, `BehaviorSubject<T>`, … all derive from `Subject<T,T>`,
so if we want them to derive from a common base `Subject[T,T]` type in Scala we lose transparency for those as well.
And again, if we expose the various subjects by extending `AnyVal`, they are useless in Scala because they do not inherit
from a common base type. To avoid implementing all methods of observable and observer on each specific subject
we might add implicit conversions to `Observable[T]` and `Observer[T]` but that still does not give Scala users
a native `Subject[S,T]` type.
```scala
object AsyncSubject {
    def apply[T](): AsyncSubject[T] =
      new AsyncSubject[T](rx.subjects.AsyncSubject.create())
}

class AsyncSubject[T] private [scala] (val inner: rx.subjects.AsyncSubject[T])
    extends AnyVal
{ … }

implicit final def asObservable[T](subject: AsyncSubject[T]): Observable[T] =
  Observable(subject.inner)

implicit final def asObserver[T](subject: AsyncSubject[T]): Observer[T] =
  subject.inner
```
The inheritance problem is not just limited to subjects, but also surfaces for subscriptions.
Rx scala currently defines `type Subscription = rx.Subscription` using a type synonym as well,
and we run into exactly the same problems as with subjects when we try to bind the
various Rx subscriptions `BooleanSubscription`, `SerialSubscription`,  etc.

Since we cannot wrap Rx types in Scala such that they are both (a) transparently interoperable with Java,
and (b) feel native and idiomatic to Scala, we should decide in favor of optimizing RxScala for Scala
and consumption of Rx values from Java but not for Scala as a producer.

If we take that approach, we can make bindings that feels like a completely native Scala library,
without needing any complications of the Scala side.
```scala
object Observer { …}
trait Observable[+T] {
   def asJavaObservable: rx.Observable[_ <: T]
}

object Observer {…}
trait Observer[-T] {
  def asJavaObserver: rx.Observer[_ >: T]
}

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
You pay the price when crossing the Scala/Java interop boundary, which is where it should be.
The proper way is to put the burden of interop on the Scala side, in case you want to create
a reusable Rx-based library in Scala, or wrap and unwrap on the Java side.
```java
public static void main(String[] args) {

   Observable<Movie> movies = Observable.from(new Movie(3000), new Movie(1000), new Movie(2000));
   MovieLib lib = new MovieLib(toScalaObservable(movies));
   lib.longMovies().asJavaObservable().subscribe(m ->
      System.out.println("A movie of length " + m.lengthInSeconds() + "s")
   );
}
```
Delegation versus Inheritance
-----------------------------
The obvious thought is that using delegation instead of inheritance (http://c2.com/cgi/wiki?DelegationIsInheritance)
will lead to excessive wrapping, since all Scala types wrap and delegate to an underlying RxJava implementation.
Note however, that the wrapping happens at query generation time and incurs no overhead when messages are flowing
through the pipeline. Say we have a query `xs.map(f).filter(p).subscribe(o)`. Even though the Scala types are wrappers,
the callback that is registered with xs is something like `x => { val y = f(x); if(p(y)){ o.asJavaObserver.onNext(y) }}`
and hence there is no additional runtime penalty.