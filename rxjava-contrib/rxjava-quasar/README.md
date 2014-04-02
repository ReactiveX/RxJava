# rxjava-quasar

Integrates RxJava with [Quasar](https://github.com/puniverse/quasar).
Includes a fiber (lightweight-thread) based scheduler, and an Observable API for Quasar channels.

Main Classes:

- [NewFiberScheduler](https://github.com/Netflix/RxJava/blob/master/rxjava-contrib/rxjava-quasar/src/main/java/rx/quasar/NewFiberScheduler.java)
- [ChannelObservable](https://github.com/Netflix/RxJava/blob/master/rxjava-contrib/rxjava-quasar/src/main/java/rx/quasar/ChannelObservable.java)
- [BlockingObservable](https://github.com/Netflix/RxJava/blob/master/rxjava-contrib/rxjava-quasar/src/main/java/rx/quasar/BlockingObservable.java)


# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ccom.netflix.rxjava).

Example for [Maven](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-apache-http%22):

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-quasar</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-quasar" rev="x.y.z" />
```

# Usage

As always when using Quasar, the java agent has to be started by adding the following JVM option:

```
-javaagent:path-to-quasar-jar.jar
```

Alternatively, you can use AOT instrumentation (see [the Quasar documentation](http://docs.paralleluniverse.co/quasar/#instrumentation)).

Or, if you're running in Tomcat, you can use the Quasar class loader (see [the Comsat documentation](http://docs.paralleluniverse.co/comsat/#enabling-comsat)).

`Observer`, `Function` or `Action` method implementations can call fiber-blocking operations when using the `NewFiberScheduler` if the method is annotated with `@Suspendable`.
(rx-core `Observer`s, `Function`s or `Action`s that manipulate, transform or delegate to other `Observer`s, `Function`s or `Action`s are automatically instrumented).

