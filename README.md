# RxJava: Reactive Extensions for the JVM

RxJava is a Java VM implementation of [Reactive Extensions](://reactivex.io): a library for composing asynchronous and event-based programs by using observable sequences.

It extends the [observer pattern](http://en.wikipedia.org/wiki/Observer_pattern) to support sequences of data/events and adds operators that allow you to compose sequences together declaratively while abstracting away concerns about things like low-level threading, synchronization, thread-safety and concurrent data structures.

- Zero Dependencies
- < 700KB Jar
- Java 6+ & [Android](https://github.com/ReactiveX/RxAndroid) 2.3+
- Java 8 lambda support
- Polyglot ([Scala](https://github.com/ReactiveX/RxScala), [Groovy](https://github.com/ReactiveX/RxGroovy), [Clojure](https://github.com/ReactiveX/RxClojure) and [Kotlin](https://github.com/ReactiveX/RxKotlin))
- Non-opinionated about source of concurrency (threads, pools, event loops, fibers, actors, etc)
- Async or synchronous execution
- Virtual time and schedulers for parameterized concurrency

Learn more about RxJava on the <a href="https://github.com/ReactiveX/RxJava/wiki">Wiki Home</a>.

## Master Build Status

<a href='https://travis-ci.org/ReactiveX/RxJava/builds'><img src='https://travis-ci.org/ReactiveX/RxJava.svg?branch=1.x'></a>

## Communication

- Google Group: [RxJava](http://groups.google.com/d/forum/rxjava)
- Twitter: [@RxJava](http://twitter.com/RxJava)
- [GitHub Issues](https://github.com/ReactiveX/RxJava/issues)

## Versioning

As of 1.0.0 RxJava is following semantic versioning.
During the 0.x.y releases, the minor (.x) releases were breaking changes.

The 0.x releases were published under the `com.netflix.rxjava` GroupId. The 1.x releases are published under `io.reactivex`. All usage of 0.x and `com.netflix.rxjava` should eventually be migrated to 1.x and `io.reactivex`. This was done as part of the migration of the project from `Netflix/RxJava` to `ReactiveX/RxJava`.

During the transition it will be possible for an application to resolve both the `com.netflix.rxjava` and `io.reactivex` artifacts. This is unfortunate but was accepted as a reasonable cost for adopting the new name as we hit version 1.0.

The 0.20.x branch is being maintained with bug fixes on the `com.netflix.rxjava` GroupId until version 1.0 Final is released to allow time to migrate between the artifacts.

## Full Documentation

- [Wiki](https://github.com/ReactiveX/RxJava/wiki)
- [Javadoc](http://reactivex.io/RxJava/javadoc/)

## Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cio.reactivex.rxjava).

Example for Maven:

```xml
<dependency>
    <groupId>io.reactivex</groupId>
    <artifactId>rxjava</artifactId>
    <version>x.y.z</version>
</dependency>
```
and for Ivy:

```xml
<dependency org="io.reactivex" name="rxjava" rev="x.y.z" />
```

## Build

To build:

```
$ git clone git@github.com:ReactiveX/RxJava.git
$ cd RxJava/
$ ./gradlew build
```

Futher details on building can be found on the [Getting Started](https://github.com/ReactiveX/RxJava/wiki/Getting-Started) page of the wiki.

## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/ReactiveX/RxJava/issues).

 
## LICENSE

Copyright 2013 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
