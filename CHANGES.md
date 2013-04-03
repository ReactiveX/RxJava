# RxJava Releases #

### Version 0.6.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.6.2%22)) ###

* [Issue 101](https://github.com/Netflix/RxJava/issues/101) Operator: Where (alias to filter)
* [Pull 197](https://github.com/Netflix/RxJava/pull/197) TakeWhile observables do not properly complete
* [Issue 21](https://github.com/Netflix/RxJava/issues/21) Operator: All
* [Pull 206](https://github.com/Netflix/RxJava/pull/206) Observable.toList breaks with multiple subscribers
* [Issue 29](https://github.com/Netflix/RxJava/issues/29) Operator: CombineLatest
* [Issue 211](https://github.com/Netflix/RxJava/issues/211) Remove use of JSR 305 and dependency on com.google.code.findbugs
* [Pull 212](https://github.com/Netflix/RxJava/pull/212) Operation take leaks errors
* [Pull 220](https://github.com/Netflix/RxJava/pull/220) TakeWhile protect calls to predicate
* [Pull 221](https://github.com/Netflix/RxJava/pull/221) Error Handling Improvements - User Provided Observers/Functions
* [Pull 201](https://github.com/Netflix/RxJava/pull/201) Synchronize Observer on OperationMerge
* [Issue 43](https://github.com/Netflix/RxJava/issues/43) Operator: Finally

### Version 0.6.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.6.1%22)) ###

* [Pull 190](https://github.com/Netflix/RxJava/pull/190) Fix generics issue with materialize() that prevented chaining

### Version 0.6.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.6.0%22)) ###

* [Issue 154](https://github.com/Netflix/RxJava/issues/154) Add OSGi manifest headers
* [Issue 173](https://github.com/Netflix/RxJava/issues/173) Subscription Utilities and Default Implementations
* [Pull 184](https://github.com/Netflix/RxJava/pull/184) Convert 'last' from non-blocking to blocking to match Rx.Net (see [Issue 57](https://github.com/Netflix/RxJava/issues/57))

*NOTE:* This is a version bump from 0.5 to 0.6 because [Issue 173](https://github.com/Netflix/RxJava/issues/173) and [Pull 184](https://github.com/Netflix/RxJava/pull/184) include breaking changes.

These changes are being done in the goal of matching the [Rx.Net](https://rx.codeplex.com) implementation so breaking changes will be made prior to 1.0 on 0.x releases if necessary.

It was found that the `last()` operator was implemented [incorrectly](https://github.com/Netflix/RxJava/issues/57) (non-blocking instead of blocking) so any use of `last()` on version 0.5.x should be changed to use `takeLast(1)`. Since the return type needed to change this could not be done via a deprecation.

Also [removed](https://github.com/Netflix/RxJava/issues/173) were the `Observable.createSubscription`/`Observable.noOpSubscription` methods which are now on the rx.subscriptions.Subscriptions utility class as `Subscriptions.create`/`Subscriptions.empty`. These methods could have been deprecated rather than removed but since another breaking change was being done they were just cleanly changed as part of the pre-1.0 process.


### Version 0.5.5 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.5%22)) ###

* [Issue 35](https://github.com/Netflix/RxJava/issues/35) Operator: Defer
* [Issue 37](https://github.com/Netflix/RxJava/issues/37) Operator: Dematerialize
* [Issue 50](https://github.com/Netflix/RxJava/issues/50) Operator: GetEnumerator (GetIterator)
* [Issue 64](https://github.com/Netflix/RxJava/issues/64) Operator: MostRecent
* [Issue 86](https://github.com/Netflix/RxJava/issues/86) Operator: TakeUntil

### Version 0.5.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.4%22)) ###

* [Issue 18](https://github.com/Netflix/RxJava/issues/18) Operator: ToIterable
* [Issue 58](https://github.com/Netflix/RxJava/issues/58) Operator: LastOrDefault
* [Issue 66](https://github.com/Netflix/RxJava/issues/66) Operator: Next
* [Issue 77](https://github.com/Netflix/RxJava/issues/77) Operator: Single and SingleOrDefault
* [Issue 164](https://github.com/Netflix/RxJava/issues/164) Range.createWithCount bugfix
* [Pull 161](https://github.com/Netflix/RxJava/pull/161) Build Status Badges and CI Integration

### Version 0.5.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.3%22)) ###

* [Issue 45](https://github.com/Netflix/RxJava/issues/45) Operator: ForEach
* [Issue 87](https://github.com/Netflix/RxJava/issues/87) Operator: TakeWhile
* [Pull 145](https://github.com/Netflix/RxJava/pull/145) IntelliJ IDEA support in Gradle build

### Version 0.5.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.2%22)) ###

* [Issue 68](https://github.com/Netflix/RxJava/issues/68) Operator: Range
* [Issue 76](https://github.com/Netflix/RxJava/issues/76) Operator: SequenceEqual
* [Issue 85](https://github.com/Netflix/RxJava/issues/85) Operator: TakeLast
* [Issue 139](https://github.com/Netflix/RxJava/issues/85) Plugin System
* [Issue 141](https://github.com/Netflix/RxJava/issues/85) Error Handler Plugin
* [Pull 134](https://github.com/Netflix/RxJava/pull/134) VideoExample in Clojure
* [Pull 135](https://github.com/Netflix/RxJava/pull/135) Idiomatic usage of import in ns macro in rx-examples.
* [Pull 136](https://github.com/Netflix/RxJava/pull/136) Add examples for jbundler and sbt

### Version 0.5.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.1%22)) ###

* variety of code cleanup commits
* [Pull 132](https://github.com/Netflix/RxJava/pull/132) Broke rxjava-examples module into each language-adaptor module
* [Issue 118](https://github.com/Netflix/RxJava/issues/118) & [Issue 119](https://github.com/Netflix/Hystrix/issues/119) Cleaned up Javadocs still referencing internal Netflix paths
* Javadoc and README changes

### Version 0.5.0  ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.0%22)) ###

* Initial open source release 
* See [Netflix Tech Blog](http://techblog.netflix.com/2013/02/rxjava-netflix-api.html) for introduction
