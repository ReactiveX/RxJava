# RxJava: Reactive Extensions for the JVM

<a href='https://travis-ci.org/ReactiveX/RxJava/builds'><img src='https://travis-ci.org/ReactiveX/RxJava.svg?branch=2.x'></a>
[![codecov.io](http://codecov.io/github/ReactiveX/RxJava/coverage.svg?branch=2.x)](https://codecov.io/gh/ReactiveX/RxJava/branch/2.x)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.reactivex.rxjava2/rxjava/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.reactivex.rxjava2/rxjava)

RxJava is a Java VM implementation of [Reactive Extensions](http://reactivex.io): a library for composing asynchronous and event-based programs by using observable sequences.

It extends the [observer pattern](http://en.wikipedia.org/wiki/Observer_pattern) to support sequences of data/events and adds operators that allow you to compose sequences together declaratively while abstracting away concerns about things like low-level threading, synchronization, thread-safety and concurrent data structures.

#### Version 2.x

- single dependency: [Reactive-Streams](https://github.com/reactive-streams/reactive-streams-jvm)  
- continued support for Java 6+ & [Android](https://github.com/ReactiveX/RxAndroid) 2.3+
- performance gains through design changes learned through the 1.x cycle and through [Reactive-Streams-Commons](https://github.com/reactor/reactive-streams-commons) research project.
- Java 8 lambda-friendly API
- non-opinionated about source of concurrency (threads, pools, event loops, fibers, actors, etc)
- async or synchronous execution
- virtual time and schedulers for parameterized concurrency


Version 2.x and 1.x will live side-by-side for several years. They will have different group ids (`io.reactivex.rxjava2` vs `io.reactivex`) and namespaces (`io.reactivex` vs `rx`). 

See the differences between version 1.x and 2.x in the wiki article [What's different in 2.0](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0). Learn more about RxJava in general on the <a href="https://github.com/ReactiveX/RxJava/wiki">Wiki Home</a>.

## Communication

- Google Group: [RxJava](http://groups.google.com/d/forum/rxjava)
- Twitter: [@RxJava](http://twitter.com/RxJava)
- [GitHub Issues](https://github.com/ReactiveX/RxJava/issues)

## Versioning

Version 2.x is now considered stable and final. Version 1.x will be supported for several years along with 2.x. Enhancements and bugfixes will be synchronized between the two in a timely manner.

Minor 2.x increments (such as 2.1, 2.2, etc) will occur when non-trivial new functionality is added or significant enhancements or bug fixes occur that may have behavioral changes that may affect some edge cases (such as dependence on behavior resulting from a bug). An example of an enhancement that would classify as this is adding reactive pull backpressure support to an operator that previously did not support it. This should be backwards compatible but does behave differently.

Patch 2.x.y increments (such as 2.0.0 -> 2.0.1, 2.3.1 -> 2.3.2, etc) will occur for bug fixes and trivial functionality (like adding a method overload). New functionality marked with an [`@Beta`][beta source link] or [`@Experimental`][experimental source link] annotation can also be added in patch releases to allow rapid exploration and iteration of unstable new functionality. 

#### @Beta

APIs marked with the [`@Beta`][beta source link] annotation at the class or method level are subject to change. They can be modified in any way, or even removed, at any time. If your code is a library itself (i.e. it is used on the CLASSPATH of users outside your own control), you should not use beta APIs, unless you repackage them (e.g. using ProGuard, shading, etc).

#### @Experimental

APIs marked with the [`@Experimental`][experimental source link] annotation at the class or method level will almost certainly change. They can be modified in any way, or even removed, at any time. You should not use or rely on them in any production code. They are purely to allow broad testing and feedback. 

#### @Deprecated

APIs marked with the `@Deprecated` annotation at the class or method level will remain supported until the next major release but it is recommended to stop using them. 

#### io.reactivex.internal.*

All code inside the `io.reactivex.internal.*` packages is considered private API and should not be relied upon at all. It can change at any time. 

## Full Documentation

- [Wiki](https://github.com/ReactiveX/RxJava/wiki)
- [Javadoc](http://reactivex.io/RxJava/2.x/javadoc/)

## Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cio.reactivex.rxjava2).

Example for Gradle:

```groovy
compile 'io.reactivex.rxjava2:rxjava:x.y.z'
```

and for Maven:

```xml
<dependency>
    <groupId>io.reactivex.rxjava2</groupId>
    <artifactId>rxjava</artifactId>
    <version>x.y.z</version>
</dependency>
```
and for Ivy:

```xml
<dependency org="io.reactivex.rxjava2" name="rxjava" rev="x.y.z" />
```

Snapshots are available via [JFrog](https://oss.jfrog.org/webapp/search/artifact/?5&q=rxjava):

```groovy
repositories {
    maven { url 'https://oss.jfrog.org/libs-snapshot' }
}

dependencies {
    compile 'io.reactivex.rxjava2:rxjava:2.0.0-DP0-SNAPSHOT'
}
```

## Build

To build:

```
$ git clone git@github.com:ReactiveX/RxJava.git
$ cd RxJava/
$ git checkout -b 2.x
$ ./gradlew build
```

Further details on building can be found on the [Getting Started](https://github.com/ReactiveX/RxJava/wiki/Getting-Started) page of the wiki.

## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/ReactiveX/RxJava/issues).

 
## LICENSE

Copyright 2013-2016 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[beta source link]: https://github.com/ReactiveX/RxJava/blob/master/src/main/java/rx/annotations/Beta.java
[experimental source link]: https://github.com/ReactiveX/RxJava/blob/master/src/main/java/rx/annotations/Experimental.java