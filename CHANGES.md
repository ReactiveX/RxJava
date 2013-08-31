# RxJava Releases #

### Version 0.11.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.11.1%22)) ###

* [Pull 325](https://github.com/Netflix/RxJava/pull/325) Clojure: Preserve metadata on fn and action macros

### Version 0.11.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.11.0%22)) ###

This is a major refactor of rxjava-core and the language adaptors. 

Note that there are *breaking changes* in this release. Details are below.

After this refactor it is expected that the API will settle down and allow us to stabilize towards a 1.0 release.

* [Pull 332](https://github.com/Netflix/RxJava/pull/332) Refactor Core to be Statically Typed

RxJava was written from the beginning to target the JVM, not any specific language.

As a side-effect of Java not having lambdas/clojures yet (and other considerations), Netflix used dynamic languages with it predominantly for the year of its existence prior to open sourcing.

To bridge the rxjava-core written in Java with the various languages a FunctionalLanguageAdaptor was registered at runtime for each language of interest.

To enable these language adaptors methods are overloaded with `Object` in the API since `Object` is the only super-type that works across all languages for their various implementations of lambdas and closures.

This downside of this has been that it breaks static typing for Java, Scala and other statically-typed languages. More can be read on this issue and discussion of the subject here: https://groups.google.com/forum/#!topic/rxjava/bVZoKSsb1-o

This release:

- removes all `Object` overload methods from rxjava-core so it is statically typed
- removes dynamic FunctionalLanguageAdaptors 
- uses idiomatic approaches for each language adaptor 
  - Java core is statically typed and has no knowledge of other languages
  - Scala uses implicits
  - Groovy uses an ExtensionModule
  - Clojure adds a new macro ([NOTE: this requires code changes](https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-clojure#basic-usage))
  - JRuby has been temporarily disabled (discussing new implementation at https://github.com/Netflix/RxJava/issues/320)
- language supports continue to be additive
  - the rxjava-core will always be required and then whichever language modules are desired such as rxjava-scala, rxjava-clojure, rxjava-groovy are added to the classpath
- deletes deprecated methods
- deletes redundant static methods on `Observable` that cluttered the API and in some cases caused dynamic languages trouble choosing which method to invoke
- deletes redundant methods on `Scheduler` that gave dynamic languages a hard time choosing which method to invoke

The benefits of this are:

1) Everything is statically typed so compile-time checks for Java, Scala, etc work correctly
2) Method dispatch is now done via native Java bytecode using types rather than going via `Object` which then has to do a lookup in a map. Memoization helped with the performance but each method invocation still required looking in a map for the correct adaptor. With this approach the appropriate methods will be compiled into the `rx.Observable` class to correctly invoke the right adaptor without lookups. 
3) Interaction from each language should work as expected idiomatically for that language.

Further history on the various discussions and different attempts at solutions can be seen at https://github.com/Netflix/RxJava/pull/304, https://github.com/Netflix/RxJava/issues/204 and https://github.com/Netflix/RxJava/issues/208


### Version 0.10.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.10.1%22)) ###

A new contrib module for Android: https://github.com/Netflix/RxJava/tree/master/rxjava-contrib/rxjava-android

* [Pull 318](https://github.com/Netflix/RxJava/pull/318) rxjava-android module with Android Schedulers

### Version 0.10.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.10.0%22)) ###

This release includes a breaking change as it changes `onError(Exception)` to `onError(Throwable)`. This decision was made via discussion at https://github.com/Netflix/RxJava/issues/296.

Any statically-typed `Observer` implementations with `onError(Exception)` will need to be updated to `onError(Throwable)` when moving to this version.

* [Pull 312](https://github.com/Netflix/RxJava/pull/312) Fix for OperatorOnErrorResumeNextViaObservable and async Resume
* [Pull 314](https://github.com/Netflix/RxJava/pull/314) Map Error Handling
* [Pull 315](https://github.com/Netflix/RxJava/pull/315) Change onError(Exception) to onError(Throwable) - Issue #296

### Version 0.9.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.9.2%22)) ###

* [Pull 308](https://github.com/Netflix/RxJava/pull/308) Ensure now() is always updated in TestScheduler.advanceTo/By
* [Pull 281](https://github.com/Netflix/RxJava/pull/281) Operator: Buffer

### Version 0.9.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.9.1%22)) ###

* [Pull 303](https://github.com/Netflix/RxJava/pull/303) CombineLatest
* [Pull 290](https://github.com/Netflix/RxJava/pull/290) Zip overload with FuncN
* [Pull 302](https://github.com/Netflix/RxJava/pull/302) NPE fix when no package on class
* [Pull 284](https://github.com/Netflix/RxJava/pull/284) GroupBy fixes (items still [oustanding](https://github.com/Netflix/RxJava/issues/282))
* [Pull 288](https://github.com/Netflix/RxJava/pull/288) PublishSubject concurrent modification fixes
* [Issue 198](https://github.com/Netflix/RxJava/issues/198) Throw if no onError handler specified
* [Issue 278](https://github.com/Netflix/RxJava/issues/278) Subscribe argument validation
* Javadoc improvements and many new marble diagrams

### Version 0.9.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.9.0%22)) ###

This release includes breaking changes that move all blocking operators (such as `single`, `last`, `forEach`) to `BlockingObservable`.

This means `Observable` has only non-blocking operators on it. The blocking operators can now be accessed via `.toBlockingObservable()` or `BlockingObservable.from(observable)`.

Notes and link to the discussion of this change can be found at https://github.com/Netflix/RxJava/pull/272.

* [Pull 272](https://github.com/Netflix/RxJava/pull/272) Move blocking operators into BlockingObservable
* [Pull 273](https://github.com/Netflix/RxJava/pull/273) Fix Concat (make non-blocking)
* [Issue 13](https://github.com/Netflix/RxJava/issues/13) Operator: Switch
* [Pull 274](https://github.com/Netflix/RxJava/pull/274) Remove SLF4J dependency (RxJava is now a single jar with no dependencies)

### Version 0.8.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.4%22)) ###

* [Pull 269](https://github.com/Netflix/RxJava/pull/269) (Really) Fix concurrency bug in ScheduledObserver

### Version 0.8.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.3%22)) ###

* [Pull 268](https://github.com/Netflix/RxJava/pull/268) Fix concurrency bug in ScheduledObserver

### Version 0.8.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.2%22)) ###

* [Issue 74](https://github.com/Netflix/RxJava/issues/74) Operator: Sample
* [Issue 93](https://github.com/Netflix/RxJava/issues/93) Operator: Timestamp
* [Pull 253](https://github.com/Netflix/RxJava/pull/253) Fix multiple subscription bug on operation filter
* [Pull 254](https://github.com/Netflix/RxJava/pull/254) SwingScheduler (new rxjava-swing module)
* [Pull 256](https://github.com/Netflix/RxJava/pull/256) BehaviorSubject
* [Pull 257](https://github.com/Netflix/RxJava/pull/257) Improved scan, reduce, aggregate
* [Pull 262](https://github.com/Netflix/RxJava/pull/262) SwingObservable (new rxjava-swing module)
* [Pull 264](https://github.com/Netflix/RxJava/pull/263) Publish, Replay and Cache Operators
* 
### Version 0.8.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.1%22)) ###

* [Pull 250](https://github.com/Netflix/RxJava/pull/250) AsyncSubject
* [Pull 252](https://github.com/Netflix/RxJava/pull/252) ToFuture
* [Pull 246](https://github.com/Netflix/RxJava/pull/246) Scheduler.schedulePeriodically
* [Pull 247](https://github.com/Netflix/RxJava/pull/247) flatMap aliased to mapMany

### Version 0.8.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.0%22)) ###

This is a breaking (non-backwards compatible) release that updates the Scheduler implementation released in 0.7.0.

See https://github.com/Netflix/RxJava/issues/19 for background, discussion and status of Schedulers.

It is believed that the public signatures of Scheduler and related objects is now stabilized but ongoing feedback and review by the community could still result in changes.

* [Issue 19](https://github.com/Netflix/RxJava/issues/19) Schedulers improvements, changes and additions
* [Issue 202](https://github.com/Netflix/RxJava/issues/202) Fix Concat bugs
* [Issue 65](https://github.com/Netflix/RxJava/issues/65) Multicast
* [Pull 218](https://github.com/Netflix/RxJava/pull/218) ReplaySubject

### Version 0.7.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.7.0%22)) ###

This release adds the foundations of Rx Schedulers.

There are still open questions, portions not implemented and assuredly bugs and behavior we didn't understand and thus implemented wrong.

Please provide bug reports, pull requests or feedback to help us on the road to version 1.0 and get schedulers implemented correctly.

See https://github.com/Netflix/RxJava/issues/19#issuecomment-15979582 for some known open questions that we could use help answering.

* [Issue 19](https://github.com/Netflix/RxJava/issues/19) Schedulers

### Version 0.6.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.6.3%22)) ###

* [Pull 224](https://github.com/Netflix/RxJava/pull/224) RxJavaObservableExecutionHook

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
