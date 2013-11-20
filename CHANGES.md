# RxJava Releases #

### Version 0.15.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.15.0%22)) ###

This release contains a refactor of the Scala Bindings by @headinthebox that results in some breaking changes. 
The previous solution ended up not working well in all cases for idiomatic Scala usage. Java/Scala interop has been changed and is no longer transparent so as to optimize for native Scala usage.
Read the [rxjava-scala README](https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-scala) for more information.

* [Pull 503](https://github.com/Netflix/RxJava/pull/503) New Scala Bindings
* [Pull 502](https://github.com/Netflix/RxJava/pull/502) Fix ObserveOn and add ParallelMerge Scheduler overload
* [Pull 499](https://github.com/Netflix/RxJava/pull/499) ObserveOn Refactor
* [Pull 492](https://github.com/Netflix/RxJava/pull/492) Implement the scheduler overloads for Range, From, StartWith
* [Pull 496](https://github.com/Netflix/RxJava/pull/496) Add contravariant for min and max

### Version 0.14.11 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.11%22)) ###

* [Pull 486](https://github.com/Netflix/RxJava/pull/486) BugFix: AsyncSubject
* [Pull 483](https://github.com/Netflix/RxJava/pull/483) Tweaks to DoOnEach and added DoOnError/DoOnCompleted

This has a very slight breaking change by removing one `doOnEach` overload. The version was not bumped from 0.14 to 0.15 as it is so minor and the offending method was just released in the previous version.

### Version 0.14.10 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.10%22)) ###

* [Pull 481](https://github.com/Netflix/RxJava/pull/481) Operator: Using
* [Pull 480](https://github.com/Netflix/RxJava/pull/480) BugFix: Emit an IllegalArgumentException instead of ArithmeticException if the observable is empty
* [Pull 479](https://github.com/Netflix/RxJava/pull/479) Operator: DoOnEach
* [Pull 478](https://github.com/Netflix/RxJava/pull/478) Operator: Min, MinBy, Max, MaxBy
* [Pull 463](https://github.com/Netflix/RxJava/pull/463) Add Timeout Overloads

### Version 0.14.9 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.9%22)) ###

* [Pull 477](https://github.com/Netflix/RxJava/pull/477) BugFix: CompositeSubscription
* [Pull 476](https://github.com/Netflix/RxJava/pull/476) BugFix: Don't emit null onComplete when no onNext received in AsyncSubject
* [Pull 474](https://github.com/Netflix/RxJava/pull/474) BugFix: Reduce an empty observable
* [Pull 474](https://github.com/Netflix/RxJava/pull/474) BugFix: non-deterministic unit test
* [Pull 472](https://github.com/Netflix/RxJava/pull/472) BugFix: Issue 431 Unsubscribe with Schedulers.newThread
* [Pull 470](https://github.com/Netflix/RxJava/pull/470) Operator: Last

### Version 0.14.8 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.8%22)) ###

* [Pull 460](https://github.com/Netflix/RxJava/pull/460) Operator: Amb
* [Pull 466](https://github.com/Netflix/RxJava/pull/466) Refactor Unit Tests

### Version 0.14.7 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.7%22)) ###

* [Pull 459](https://github.com/Netflix/RxJava/pull/459) Fix multiple unsubscribe behavior
* [Pull 458](https://github.com/Netflix/RxJava/pull/458) rxjava-android: OperationObserveFromAndroidComponent
* [Pull 453](https://github.com/Netflix/RxJava/pull/453) Fix error handling in map operator
* [Pull 450](https://github.com/Netflix/RxJava/pull/450) Operator: TimeInterval
* [Pull 452](https://github.com/Netflix/RxJava/pull/451) Scheduler Overload of Just/Return Operator
* [Pull 433](https://github.com/Netflix/RxJava/pull/433) Fixes: Next Operator
* [Commit d64a8c5](https://github.com/Netflix/RxJava/commit/d64a8c5f73d8d1a5de1861e0d20f12609b408880) Update rxjava-apache-http to Apache HttpAsyncClient 4.0 GA 

### Version 0.14.6 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.6%22)) ###

* [Pull 441](https://github.com/Netflix/RxJava/pull/441) Fixed the issue that 'take' does not call 'onError'
* [Pull 443](https://github.com/Netflix/RxJava/pull/443) OperationSwitch notify onComplete() too early.
* [Pull 434](https://github.com/Netflix/RxJava/pull/434) Timeout operator and SerialSubscription
* [Pull 447](https://github.com/Netflix/RxJava/pull/447) Caching the result of 'isInternalImplementation'

### Version 0.14.5 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.5%22)) ###

* [Pull 438](https://github.com/Netflix/RxJava/pull/438) Kotlin Language Adaptor

### Version 0.14.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.4%22)) ###

* [Issue 428](https://github.com/Netflix/RxJava/issues/428) Fix: buffer() using TimeAndSizeBasedChunks incorrectly forces thread into interrupted state
* [Pull 435](https://github.com/Netflix/RxJava/pull/435) rx-apache-http recognizes "Transfer-Encoding: chunked" as an HTTP stream
* [Pull 437](https://github.com/Netflix/RxJava/pull/437) Fixes: Scheduler and Merge


### Version 0.14.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.3%22)) ###

* [Pull 407](https://github.com/Netflix/RxJava/pull/407) Operator: RefCount
* [Pull 410](https://github.com/Netflix/RxJava/pull/410) Operator: Contains
* [Pull 411](https://github.com/Netflix/RxJava/pull/411) Unit Test fix: update counter before triggering latch 
* [Pull 413](https://github.com/Netflix/RxJava/pull/413) Fixed the issues of takeLast(items, 0) and null values
* [Pull 414](https://github.com/Netflix/RxJava/pull/414) Operator: SkipLast
* [Pull 415](https://github.com/Netflix/RxJava/pull/415) Operator: Empty with scheduler
* [Pull 416](https://github.com/Netflix/RxJava/pull/416) Operator: Throw with scheduler 
* [Pull 420](https://github.com/Netflix/RxJava/pull/420) Scala Adaptor Improvements
* [Pull 422](https://github.com/Netflix/RxJava/pull/422) JRuby function wrapping support
* [Pull 424](https://github.com/Netflix/RxJava/pull/424) Operator: IgnoreElements
* [Pull 426](https://github.com/Netflix/RxJava/pull/426) PublishSubject ReSubscribe for publish().refCount() Behavior

### Version 0.14.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.2%22)) ###

* [Pull 403](https://github.com/Netflix/RxJava/pull/403) Operators: Cast and OfType
* [Pull 401](https://github.com/Netflix/RxJava/pull/401) Operator: DefaultIfEmpty
* [Pull 409](https://github.com/Netflix/RxJava/pull/409) Operator: Synchronize with object

### Version 0.14.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.1%22)) ###

* [Pull 402](https://github.com/Netflix/RxJava/pull/402) rxjava-apache-http improvements

### Version 0.14.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.0%22)) ###

Further progress to the Scala adaptor and a handful of new operators.

Bump to 0.14.0 due to small breaking change to `distinct` operator removing overloaded methods with `Comparator`. These methods were added in 0.13.2 and determined to be incorrect.

This release also includes a new contrib module, [rxjava-apache-http](https://github.com/Netflix/RxJava/tree/master/rxjava-contrib/rxjava-apache-http) that provides an Observable API to the Apache HttpAsyncClient.

* [Pull 396](https://github.com/Netflix/RxJava/pull/396) Add missing methods to Scala Adaptor
* [Pull 390](https://github.com/Netflix/RxJava/pull/390) Operators: ElementAt and ElementAtOrDefault
* [Pull 398](https://github.com/Netflix/RxJava/pull/398) Operators: IsEmpty and Exists (instead of Any)
* [Pull 397](https://github.com/Netflix/RxJava/pull/397) Observable API for Apache HttpAsyncClient 4.0
* [Pull 400](https://github.com/Netflix/RxJava/pull/400) Removing `comparator` overloads of `distinct`

### Version 0.13.5

* Upload to Sonatype failed so version skipped

### Version 0.13.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.13.4%22)) ###

* [Pull 393](https://github.com/Netflix/RxJava/pull/393) Parallel Operator & ObserveOn/ScheduledObserver Fixes
* [Pull 394](https://github.com/Netflix/RxJava/pull/394) Change Interval and Sample default Scheduler
* [Pull 391](https://github.com/Netflix/RxJava/pull/391) Fix OSGI support for rxjava-scala

### Version 0.13.3

* Upload to Sonatype failed so version skipped

### Version 0.13.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.13.2%22)) ###

* [Pull 389](https://github.com/Netflix/RxJava/pull/389) Scala Adaptor Improvements
* [Pull 382](https://github.com/Netflix/RxJava/pull/382) Removing deprecated RxImplicits from rxjava-scala
* [Pull 381](https://github.com/Netflix/RxJava/pull/381) Operator: mapWithIndex
* [Pull 380](https://github.com/Netflix/RxJava/pull/380) Implemented `distinct` and `distinctUntilChanged` variants using a comparator
* [Pull 379](https://github.com/Netflix/RxJava/pull/379) Make `interval` work with multiple subscribers

### Version 0.13.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.13.1%22)) ###

This release includes a new Scala adaptor as part of the effort from issue https://github.com/Netflix/RxJava/issues/336 pursuing idiomatic Scala support.

* [Pull 376](https://github.com/Netflix/RxJava/pull/376) Idiomatic Scala Adaptor
* [Pull 375](https://github.com/Netflix/RxJava/pull/375) Operator: Distinct
* [Pull 374](https://github.com/Netflix/RxJava/pull/374) Operator: DistinctUntilChanged
* [Pull 373](https://github.com/Netflix/RxJava/pull/373) Fixes and Cleanup

### Version 0.13.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.13.0%22)) ###

This release has some minor changes related to varargs that could break backwards compatibility 
if directly passing arrays but for most this release should not be breaking.

* [Pull 354](https://github.com/Netflix/RxJava/pull/354) Operators: Count, Sum, Average
* [Pull 355](https://github.com/Netflix/RxJava/pull/355) Operators: skipWhile and skipWhileWithIndex
* [Pull 356](https://github.com/Netflix/RxJava/pull/356) Operator: Interval
* [Pull 357](https://github.com/Netflix/RxJava/pull/357) Operators: first and firstOrDefault
* [Pull 368](https://github.com/Netflix/RxJava/pull/368) Operators: Throttle and Debounce
* [Pull 371](https://github.com/Netflix/RxJava/pull/371) Operator: Retry
* [Pull 370](https://github.com/Netflix/RxJava/pull/370) Change zip method signature from Collection to Iterable
* [Pull 369](https://github.com/Netflix/RxJava/pull/369) Generics Improvements: co/contra-variance
* [Pull 361](https://github.com/Netflix/RxJava/pull/361) Remove use of varargs from API

### Version 0.12.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.12.2%22)) ###

* [Pull 352](https://github.com/Netflix/RxJava/pull/352) Groovy Language Adaptor: Add Func5-9 and N to the wrapper

### Version 0.12.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.12.1%22)) ###

* [Pull 350](https://github.com/Netflix/RxJava/pull/350) Swing module enhancements
* [Pull 351](https://github.com/Netflix/RxJava/pull/351) Fix Observable.window static/instance bug

### Version 0.12.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.12.0%22)) ###

This version adds to the static typing changes in 0.11 and adds covariant/contravariant typing via super/extends generics.

Additional cleanup was done, particularly related to `BlockingObservable`. Also the `window` operator was added.

The largest breaking change is that `Observable.create` now accepts an `OnSubscribeFunc` rather than a `Func1`.

This means that instead of this:

```java
public static <T> Observable<T> create(Func1<? super Observer<? super T>, ? extends Subscription> func)
```

it is now:

```java
public static <T> Observable<T> create(OnSubscribeFunc<T> func)
```

This was done to simplify the usage of `Observable.create` which was already verbose but made far worse by the `? super` generics.

For example, instead of writing this:

```java
Observable.create(new Func1<Observer<? super SomeType>, Subscription>() {
   /// body here
}
```

it is now written as:

```java
Observable.create(new OnSubscribeFunc<SomeType>() {
   /// body here
}
```

* [Pull 343](https://github.com/Netflix/RxJava/pull/343) Covariant Support with super/extends and `OnSubscribeFunc` as type for `Observable.create`
* [Pull 337](https://github.com/Netflix/RxJava/pull/337) Operator: `window`
* [Pull 348](https://github.com/Netflix/RxJava/pull/348) Rename `switchDo` to `switchOnNext` (deprecate `switchDo` for eventual deletion)
* [Pull 348](https://github.com/Netflix/RxJava/pull/348) Delete `switchDo` instance method in preference for static
* [Pull 346](https://github.com/Netflix/RxJava/pull/346) Remove duplicate static methods from `BlockingObservable` 
* [Pull 346](https://github.com/Netflix/RxJava/pull/346) `BlockingObservable` no longer extends from `Observable`
* [Pull 345](https://github.com/Netflix/RxJava/pull/345) Remove unnecessary constructor from `Observable`

### Version 0.11.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.11.2%22)) ###

* [Commit ccf53e8]( https://github.com/Netflix/RxJava/commit/ccf53e84835d99136cce80a4c508bae787d5da45) Update to Scala 2.10.2

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
