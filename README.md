# RxJava: Reactive Extensions for the JVM

This library is a Java implementation of <a href="https://rx.codeplex.com">Rx Observables</a>.

Some of the goals of RxJava are:

- Stay close to other Rx implementations while adjusting naming conventions and idioms to Java
- Match contracts of Rx should be the same
- Target the JVM not a language to allow JVM-language bindings (such as [Scala](https://github.com/ReactiveX/RxScala), [Groovy](https://github.com/ReactiveX/RxGroovy), [Clojure](https://github.com/ReactiveX/RxClojure) and [Kotlin](https://github.com/ReactiveX/RxKotlin)).
- Support Java 6+ (to include Android support) 

Learn more about RxJava on the <a href="https://github.com/ReactiveX/RxJava/wiki">Wiki Home</a> and the <a href="http://techblog.netflix.com/2013/02/rxjava-netflix-api.html">Netflix TechBlog post</a> where RxJava was introduced.

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
