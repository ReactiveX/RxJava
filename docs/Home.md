RxJava is a Java VM implementation of [ReactiveX (Reactive Extensions)](https://reactivex.io): a library for composing asynchronous and event-based programs by using observable sequences.

For more information about ReactiveX, see the [Introduction to ReactiveX](http://reactivex.io/intro.html) page.

### RxJava is Lightweight

RxJava tries to be very lightweight. It is implemented as a single JAR that is focused on just the Observable abstraction and related higher-order functions.

### RxJava is a Polyglot Implementation

RxJava supports Java 6 or higher and JVM-based languages such as [Groovy](https://github.com/ReactiveX/RxGroovy), [Clojure](https://github.com/ReactiveX/RxClojure), [JRuby](https://github.com/ReactiveX/RxJRuby), [Kotlin](https://github.com/ReactiveX/RxKotlin) and [Scala](https://github.com/ReactiveX/RxScala).

RxJava is meant for a more polyglot environment than just Java/Scala, and it is being designed to respect the idioms of each JVM-based language. (<a href="https://github.com/Netflix/RxJava/pull/304">This is something we’re still working on.</a>)

### RxJava Libraries

The following external libraries can work with RxJava:

* [Hystrix](https://github.com/Netflix/Hystrix/wiki/How-To-Use#wiki-Reactive-Execution) latency and fault tolerance bulkheading library.
* [Camel RX](http://camel.apache.org/rx.html) provides an easy way to reuse any of the [Apache Camel components, protocols, transports and data formats](http://camel.apache.org/components.html) with the RxJava API
* [rxjava-http-tail](https://github.com/myfreeweb/rxjava-http-tail) allows you to follow logs over HTTP, like `tail -f`
* [mod-rxvertx - Extension for VertX](https://github.com/vert-x/mod-rxvertx) that provides support for Reactive Extensions (RX) using the RxJava library
* [rxjava-jdbc](https://github.com/davidmoten/rxjava-jdbc) - use RxJava with jdbc connections to stream ResultSets and do functional composition of statements
* [rtree](https://github.com/davidmoten/rtree) - immutable in-memory R-tree and R*-tree with RxJava api including backpressure