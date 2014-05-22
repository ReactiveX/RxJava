# Scala Adaptor for RxJava

This adaptor allows to use RxJava in Scala with anonymous functions, e.g.

```scala
val o = Observable.interval(200 millis).take(5)
o.subscribe(n => println("n = " + n))
Observable.items(1, 2, 3, 4).reduce(_ + _)
```

For-comprehensions are also supported:

```scala
val first = Observable.items(10, 11, 12)
val second = Observable.items(10, 11, 12)
val booleans = for ((n1, n2) <- (first zip second)) yield (n1 == n2)
```

Further, this adaptor attempts to expose an API which is as Scala-idiomatic as possible. This means that certain methods have been renamed, their signature was changed, or static methods were changed to instance methods. Some examples:

```scala
 // instead of concat:
def ++[U >: T](that: Observable[U]): Observable[U]

// instance method instead of static:
def zip[U](that: Observable[U]): Observable[(T, U)] 

// the implicit evidence argument ensures that dematerialize can only be called on Observables of Notifications:
def dematerialize[U](implicit evidence: T <:< Notification[U]): Observable[U] 

// additional type parameter U with lower bound to get covariance right:
def onErrorResumeNext[U >: T](resumeFunction: Throwable => Observable[U]): Observable[U] 

// curried in Scala collections, so curry fold also here:
def fold[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] 

// using Duration instead of (long timepan, TimeUnit duration):
def sample(duration: Duration): Observable[T] 

// called skip in Java, but drop in Scala
def drop(n: Int): Observable[T] 

// there's only mapWithIndex in Java, because Java doesn't have tuples:
def zipWithIndex: Observable[(T, Int)] 

// corresponds to Java's toList:
def toSeq: Observable[Seq[T]] 

// the implicit evidence argument ensures that switch can only be called on Observables of Observables:
def switch[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U]

// Java's from becomes apply, and we use Scala Range
def apply(range: Range): Observable[Int]

// use Bottom type:
def never: Observable[Nothing] 
```

Also, the Scala Observable is fully covariant in its type parameter, whereas the Java Observable only achieves partial covariance due to limitations of Java's type system (or if you can fix this, your suggestions are very welcome).

For more examples, see [RxScalaDemo.scala](https://github.com/Netflix/RxJava/blob/master/language-adaptors/rxjava-scala/src/examples/scala/rx/lang/scala/examples/RxScalaDemo.scala).

Scala code using Rx should only import members from `rx.lang.scala` and below.


## Documentation

The API documentation can be found [here](http://rxscala.github.io/scaladoc/index.html#rx.lang.scala.Observable).

Note that starting from version 0.15, `rx.lang.scala.Observable` is not a value class any more.  [./Rationale.md](https://github.com/Netflix/RxJava/blob/master/language-adaptors/rxjava-scala/Rationale.md) explains why.

You can build the API documentation yourself by running `./gradlew scaladoc` in the RxJava root directory.

Then navigate to `RxJava/language-adaptors/rxjava-scala/build/docs/scaladoc/index.html` to display it.


## Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-scala%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-scala</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-scala" rev="x.y.z" />
```

and for sbt:

```scala
libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "x.y.z"
)
```
