# RxJava 2 Releases #

The changelog of version 1.x can be found at https://github.com/ReactiveX/RxJava/blob/1.x/CHANGES.md

### Version 2.1.2 - July 23, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.1.2%7C))

#### Documentation changes

- [Pull 5432](https://github.com/ReactiveX/RxJava/pull/5432): Fix/clarify the `Observable` class' javadoc.
- [Pull 5444](https://github.com/ReactiveX/RxJava/pull/5444): Fix wording in `Async` and `Publish` processors javadoc.
- [Pull 5413](https://github.com/ReactiveX/RxJava/pull/5413): Add empty source clauses to javadocs of `combineLatest` operators accepting unspecified number of sources.
- [Pull 5465](https://github.com/ReactiveX/RxJava/pull/5465): Fix wording of toList, fix a/an in `subscribeOn`.
- [Pull 5476](https://github.com/ReactiveX/RxJava/pull/5476): Fix Javadoc for `Flowable` and `Observable` reduce.
- [Pull 5478](https://github.com/ReactiveX/RxJava/pull/5478): Corrected return type in doc for `F.reduce(seed, reducer)`, `F.reduceWith(seedSupplier, reducer)` and `O.reduce(seed, reducer)`.
- [Pull 5486](https://github.com/ReactiveX/RxJava/pull/5486): Small note on `Maybe.defaultIfEmpty` regarding `toSingle`.

#### Bugfixes

- [Pull 5434](https://github.com/ReactiveX/RxJava/pull/5434): Fix time bounded `ReplaySubject.getValue()` inconsistency with `getValues()` on old items.
- [Pull 5440](https://github.com/ReactiveX/RxJava/pull/5440): `concat` to report `isDisposed` consistently with termination.
- [Pull 5441](https://github.com/ReactiveX/RxJava/pull/5441): Fix periodic scheduler purging config not honored.
- [Pull 5494](https://github.com/ReactiveX/RxJava/pull/5494): Fix `FlowableWithLatestFrom` not requesting more when the other hasn't emitted yet.
- [Pull 5493](https://github.com/ReactiveX/RxJava/pull/5493): Fix `ReplayProcessor` backpressure and `NotificationLite` emission bug.
- [Pull 5507](https://github.com/ReactiveX/RxJava/pull/5507): Fix GC nepotism in `SpscLinkedArrayQueue`.
- [Pull 5511](https://github.com/ReactiveX/RxJava/pull/5511): Remove unnecessary generic type parameter for the timed `Single.delaySubscription` methods.

#### Other

- [Pull 5447](https://github.com/ReactiveX/RxJava/pull/5447): Remove `@NonNull` annotation in `Consumer` method parameter.
- [Pull 5449](https://github.com/ReactiveX/RxJava/pull/5449): Remove the `@NonNull` annotation from `Function`.
- [Commit 4d8f008c](https://github.com/ReactiveX/RxJava/commit/4d8f008cb6823730b5e25fea559905a811d8ce32): add missing 'the' to the changed sentences of Pull 5413
- [Pull 5460](https://github.com/ReactiveX/RxJava/pull/5460): Fix Javadoc mistakes and some style.
- [Pull 5466](https://github.com/ReactiveX/RxJava/pull/5466): Use a mutable field in `FlowableTimeoutTimed` instead of an `AtomicReference`.
- [Commit 5d2e8fb4](https://github.com/ReactiveX/RxJava/commit/5d2e8fb4363f18c5cbb247e2d4c6ed1c71527128): Fix `Schedulers.io()` javadoc `{link` missing the `@` symbol.
- [Pull 5495](https://github.com/ReactiveX/RxJava/pull/5495): Make `withLatestFrom` conditional subscriber, test cold consumption.

### Version 2.1.1 - June 21, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.1.1%7C))

#### Notable changes

The emitter API (such as `FlowableEmitter`, `SingleEmitter`, etc.) now features a new method, `tryOnError` that tries to emit the `Throwable` if the sequence is not cancelled/disposed. Unlike the regular `onError`, if the downstream is no longer willing to accept events, the method returns false and doesn't signal an `UndeliverableException`.

#### API enhancements

- [Pull 5344](https://github.com/ReactiveX/RxJava/pull/5344): Add `tryOnError` to `create/XEmitter` API.
- [Pull 5386](https://github.com/ReactiveX/RxJava/pull/5386): Add `subscribeOn` overload to avoid same-pool deadlock with create.

#### Documentation changes

- [Pull 5343](https://github.com/ReactiveX/RxJava/pull/5343): Fix Javadoc for `Maybe.toSingle`.
- [Pull 5347](https://github.com/ReactiveX/RxJava/pull/5347): Fix Javadoc for `FunctionX`
- [Pull 5351](https://github.com/ReactiveX/RxJava/pull/5351): Update some marbles of `Observable`
- [Commit b4aeb6e3](https://github.com/ReactiveX/RxJava/commit/0b0355e3bc09326c8005fd26d09e7c1eb4aeb6e3): Replace `Action1` with `Consumer` in docs.
- [Pull 5383](https://github.com/ReactiveX/RxJava/pull/5383): Fixed Javadoc for `SingleFlatMapIterableObservable`.
- [Pull 5403](https://github.com/ReactiveX/RxJava/pull/5403): Fix the copy-paste error in the Javadoc of `Single.doAfterTeminate` mentioning `doAfterSuccess`.
- [Pull 5405](https://github.com/ReactiveX/RxJava/pull/5405): `DefaultObserver` javadoc fix: use subscribe, remove disposable code.
- [Pull 5407](https://github.com/ReactiveX/RxJava/pull/5407): `DefaultSubscriber` javadoc sample fix.
- [Pull 5406](https://github.com/ReactiveX/RxJava/pull/5406): Fix javadoc for `Observable.reduce()` and `Observable.reduceWith()`.
- [Pull 5409](https://github.com/ReactiveX/RxJava/pull/5409): Corrected `Single.delay` documentation.

#### Bugfixes

- [Pull 5367](https://github.com/ReactiveX/RxJava/pull/5367): Make sure `interval+trampoline` can be stopped.
- [Pull 5378](https://github.com/ReactiveX/RxJava/pull/5378): Make `SingleMap` not allow map function return null.
- [Pull 5379](https://github.com/ReactiveX/RxJava/pull/5379): Add missing null checks on values returned by user functions.
- [Pull 5415](https://github.com/ReactiveX/RxJava/pull/5415): Fix `doOnNext` failure not triggering `doOnError` when fused.
- [Pull 5419](https://github.com/ReactiveX/RxJava/pull/5419): Fix periodic scheduling with negative period causing IAE.
- [Pull 5427](https://github.com/ReactiveX/RxJava/pull/5427): Fix `buffer(time, maxSize)` duplicating buffers on time-size race. 

#### Other

- [Pull 5324](https://github.com/ReactiveX/RxJava/pull/5324): Mark `VolatileSizeArrayList` as `RandomAccess` list
- [Pull 5354](https://github.com/ReactiveX/RxJava/pull/5354): Fix typo in error message in `BaseTestConsumer.assertValueSequence`.
- [Pull 5391](https://github.com/ReactiveX/RxJava/pull/5391): Changed minimum value of `rx2.buffer-size` to 1.

### Version 2.1.0 - April 29, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.1.0%7C))

#### Summary

Version 2.1.0 is the next minor release of the 2.x era and contains the standardization of many experimental API additions from the past half a year since version 2.0.0. Therefore, the following components are now considered stable and will be supported throughout the rest of the life of RxJava 2.x.

**Classes, Enums, Annotations**

- Annotation: `CheckReturnValue`
- Subjects: `CompletableSubject`, `MaybeSubject`, `SingleSubject`
- Enum: `TestWaitStrategy`

**Operators**

- **`Flowable`**: `doAfterNext`, `doFinally`, `sample` (emitLast)
- **`Observable`**: `doAfterNext`, `doFinally`, `sample` (emitLast)
- **`Single`**: `doAfterSuccess`, `doAfterTerminate`, `doFinally`
- **`Maybe`**: `doAfterSuccess`, `doFinally`, `flatMapSingleElement`
- **`Completable`**: `cache`, `doFinally`, `hide`
- **`Test{Observer, Subscriber}`**: `assertNever`, `assertTimeout`, `assertNoTimeout`, `awaitCount`, `clearTimeout`, `isTimeout`, `withTag`
- **`RxJavaPlugins`**: `createComputationScheduler`, `createIoScheduler`, `createNewThreadScheduler`, `createSingleScheduler`, `getOnBeforeBlocking`, `setOnBeforeBlocking`, `isFailOnBlockingScheduler`, `setFailOnBlockingScheduler`.
- Other: `Scheduler.when`, `TestSubscriber.requestMore`

*(For the complete list and details on the promotions, see issue [5243](https://github.com/ReactiveX/RxJava/issues/5243).)*

Release 2.1.0 is functionally identical to 2.0.9 except the removal of [now unnecessary](https://github.com/ReactiveX/RxJava/releases/tag/v2.0.7) `Flowable.strict()` operator. To clarify, just like with previous minor version increments with RxJava, there won't be any further development or updates on the version 2.0.x (patch) level. 

##### Beta promotions

Some of the enhancements of RxJava 2.0.x were added recently which often represent complex additions to RxJava itself (such as the whole `ParallelFlowable`). We are confident their functionality adds value to the library but not enough time elapsed since their introduction for the community to try it out and provide feedback on them (i.e., naming, encompassed functionality, etc.). To indicate we are willing to support them and eventually standardize them in the next minor release, the following components have been promoted to the status of `Beta`:

- Classes: `OnErrorNotImplementedException`, `ProtocolViolationException`, `UndeliverableException`, 
 `ParallelFlowable`
- Interface: `FlowableSubscriber`
- Methods
  - **`Flowable`**: `parallel`, `subscribe(FlowableSubscriber)`
  - **`RxJavaPlugins`**: `getOnParallelAssembly`, `onAssembly(ParallelFlowable)`, `setOnParallelAssembly`

#### Non-functional changes between 2.0.9 and 2.1

- [Pull 5306](https://github.com/ReactiveX/RxJava/pull/5306): Change `ObservableSource.defer` to `Observable.defer` in `Observable.scan()` documentation.
- [Pull 5309](https://github.com/ReactiveX/RxJava/pull/5309): Fix Javadoc of `Flowable.toObservable` referring to `Publisher` instead of `Observable`.

#### Project statistics

- Unique contributors: **41**
- Issues closed: **315**
- Bugs reported: **43**
  - by community: **40** (93%)
- Commits: **193**
- PRs: **225**
  - PRs accepted: **198** (88%)
  - Community PRs: **76** (38.4% of all accepted)
- Bugs fixed: **58**
  - by community: **8** (13.9%)
- Documentation enhancements: **46**
  - by community: **22** (52.2%)
- Cleanup: **40**
  - by community: **22** (55%)
- Lines
  - added: **44,931**
  - removed: **7,405**

#### Acknowledgements

The project would like to thank the following contributors for their work on various code and documentation improvements (in the order they appear on the [commit](https://github.com/ReactiveX/RxJava/commits/2.x) page):

@vanniktech, @veyndan, @mauin, @smartbeng, @ImangazalievM, @bloderxd, @mibac138, @ggikko,  @mostroverkhov, @sadegh, @nmorioka, @SleimanJneidi, @davidmoten, @hkurokawa, @jbarr21, @alexandre-dubois, @VeskoI, @Stephan202, @PaulWoitaschek, @soulkeykim, @stevepeak, @jschneider, @JakeWharton, @tonycosentini, @hzsweers, @passsy, @sergiomarquesmoura, @ikesyo, @benjchristensen, @zsavely, @DDesideria, @gaemi, @Jawnnypoo, @artem-zinnatullin, @mkobit, @abersnaze, @tbcs, @gengjiawen, @qwert2603, @DmitriyZaitsev 

**(40 contributors)**

The project would also thank its tireless reviewers, @JakeWharton and @vanniktech for all their efforts on verifying and providing feedback on the many PRs from the project lead himself. :+1:

### Version 2.0.9 - April 21, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.9%7C))

**API enhancements**

- [Pull 5302](https://github.com/ReactiveX/RxJava/pull/5302): Add `Single.unsubscribeOn()`.

**Bugfixes**
- [Pull 5247](https://github.com/ReactiveX/RxJava/pull/5247): Fix `Flowable.toList()` `onNext`/`cancel` race.
- [Pull 5256](https://github.com/ReactiveX/RxJava/pull/5256): Fix `flatMapIterable` appearing to be empty when fused.
- [Pull 5277](https://github.com/ReactiveX/RxJava/pull/5277): Fix `Single.subscribe(BiConsumer)` to be consistent with `isDisposed`.
- [Pull 5281](https://github.com/ReactiveX/RxJava/pull/5281): Fix `BehaviorProcessor` & `BehaviorSubject` terminate-subscribe race. 
- [Pull 5287](https://github.com/ReactiveX/RxJava/pull/5287): Fix `Flowable.flatMapMaybe`/`Flowable.flatMapSingle` `maxConcurrency` not requesting more.

**Documentation**
- [Pull 5271](https://github.com/ReactiveX/RxJava/pull/5271): enable link to external JDK, fix `Schedulers` style.
- [Pull 5286](https://github.com/ReactiveX/RxJava/pull/5286): Cleanup for text and Javadoc 04/15.
- [Commit 7c95808](https://github.com/ReactiveX/RxJava/commit/7c95808f077537428f2ae80fffd15e2848a2de31): Fix `DisposableX` copy-paste error in Javadoc.
- [Pull 5296](https://github.com/ReactiveX/RxJava/pull/5296): Improve `doOnDispose` JavaDoc.
- [Pull 5297](https://github.com/ReactiveX/RxJava/pull/5297): Fix JavaDoc image for `Single.flatMapObservable()`.
- [Pull 5304](https://github.com/ReactiveX/RxJava/pull/5304): Correct documented return type of `Single.flatMapObservable()`'s function argument.

**Other**
- [Pull 5255](https://github.com/ReactiveX/RxJava/pull/5255): Add `NullPointerException` comments and `ObjectHelper` test code.
- [Pull 5251](https://github.com/ReactiveX/RxJava/pull/5251):  More nullability annotations.
- [Pull 5257](https://github.com/ReactiveX/RxJava/pull/5257): Remove `@NonNull` annotations from `BiConsumer`.
- [Pull 5268](https://github.com/ReactiveX/RxJava/pull/5268): Remove commented out code from `IoScheduler`.
- [Pull 5301](https://github.com/ReactiveX/RxJava/pull/5301): More detailed no-multi-subscribe error message with the standard consumer types (such as `DisposableObserver`).

### Version 2.0.8 - March 29, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.8%7C))

**API enhancements**
- [Pull 5161](https://github.com/ReactiveX/RxJava/pull/5161): Add `Observable.switchMapSingle()`
- [Pull 5184](https://github.com/ReactiveX/RxJava/pull/5184): Add `offer()` method to `PublishProcessor` & `BehaviorProcessor`.
- [Pull 5197](https://github.com/ReactiveX/RxJava/pull/5197): Add `ParallelTransformer` interface.
- [Pull 5217](https://github.com/ReactiveX/RxJava/pull/5217): `UnicastSubject` fail-fast support.
- [Pull 5202](https://github.com/ReactiveX/RxJava/pull/5202): Add resilient versions of parallel `map()`, `filter()` and `doOnNext()`.
- [Pull 5226](https://github.com/ReactiveX/RxJava/pull/5226): `UnicastProcessor` fail-fast support.

**Bugfixes**
- [Pull 5163](https://github.com/ReactiveX/RxJava/pull/5163): `Single.subscribe()` to report `isDisposed()` true on success/error.
- [Pull 5170](https://github.com/ReactiveX/RxJava/pull/5170): Fix `LambdaObserver` not cancelling the upstream.
- [Pull 5182](https://github.com/ReactiveX/RxJava/pull/5182): Fix `replay().refCount()` leaking items between connections.
- [Pull 5188](https://github.com/ReactiveX/RxJava/pull/5188): Fix `flatMap` emitting the terminal exception indicator on cancel.
- [Pull 5207](https://github.com/ReactiveX/RxJava/pull/5207): Prevent tasks to self interrupt on the standard schedulers.
- [Pull 5213](https://github.com/ReactiveX/RxJava/pull/5213): Fix `window()` with time+size emission problems.
- [Pull 5240](https://github.com/ReactiveX/RxJava/pull/5240): fix `CallbackCompletableObserver` calling `onError`.

**Documentation**
- [Pull 5189](https://github.com/ReactiveX/RxJava/pull/5189): Declare `concatMapEager` requires positive prefetch amount.
- [Pull 5191](https://github.com/ReactiveX/RxJava/pull/5191): Correct java doc for `refCount()` return type.
- [Pull 5208](https://github.com/ReactiveX/RxJava/pull/5208): Fix images of `firstElement`, `flattenAsX`, `flatMapIterable`, `UnicastSubject` and `UnicastProcessor`.
- [Pull 5210](https://github.com/ReactiveX/RxJava/pull/5210): Better documentation on the abstract consumer classes (such as `DisposableSubscriber`).
- [Pull 5223](https://github.com/ReactiveX/RxJava/pull/5223): Improve the documentation of `Schedulers` utility class.
- [Pull 5230](https://github.com/ReactiveX/RxJava/pull/5230): Fix wrong comments in `Functions` “Function3” -> “BiFunction, Function3”

**Other**
- Remove anonymous inner classes.
   - [Pull 5174](https://github.com/ReactiveX/RxJava/pull/5174)
   - [Pull 5177](https://github.com/ReactiveX/RxJava/pull/5177)
- [Pull 5183](https://github.com/ReactiveX/RxJava/pull/5183): Test to disallow anonymous inner classes.
- [Pull 5187](https://github.com/ReactiveX/RxJava/pull/5187): Reflection-based parameter validator & fixes.
- [Pull 5196](https://github.com/ReactiveX/RxJava/pull/5196): Add a few more `@Nullable` & `@NonNull` annotations to public interfaces.

### Version 2.0.7 - March 7, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.7%7C))

**Reactive-Streams compliance** 

*[Related issue 5110](https://github.com/ReactiveX/RxJava/issues/5110), [related pull 5112](https://github.com/ReactiveX/RxJava/pull/5112).*

RxJava 2's `Flowable` was designed with Reactive-Streams compliance in mind but didn't follow all rules to the letter. A temporary workaround was the introduction of the `strict()` operator to enable full compliance. Unfortunately, according to the clarified stance from the specification leads, implementors of `Publisher` must honor the specification (despite its shortcomings) without excuses or workarounds. 

Honoring the specification adds a per-item cost of two atomic increments, manifesting in between the `Flowable` and an arbitrary `Reactive-Streams` compliant `org.reactivestreams.Subscriber`. See [Pull 5115](https://github.com/ReactiveX/RxJava/pull/5115) for the benchmark comparison.

Starting from 2.0.7, the `Flowable.subscribe(org.reactivestreams.Subscriber)` now follows the spec by wrapping via the `StrictSubscriber` of the `strict()` operator unless the consuming instance implements a new interface specific to RxJava 2: `FlowableSubscriber`. 

The `FlowableSubscriber` extends `org.reactivestreams.Subscriber` but doesn't add any new methods and only overrides four of the textual rules of the specification to enable a relaxed operation within RxJava 2. All internal operators have been converted to use this new interface and thus don't have suffer the per-item overhead required by the specification, including the standard `DisposableSubscriber`, `TestSubscriber` and `ResourceSubscriber`. The lambda-based `subscribe()` operators were also retrofitted.

If you were implementing a `Subscriber` (directly or anonymously), you may want to change the interface to `FlowableSubscriber`. In order to avoid some of the runtime checks when subscribing to `Flowable` the new `subscribe(FlowableSubscriber)` has been introduced.

Note that the other reactive base types, `Observable`, `Single`, `Maybe` and `Completable` are not affected as these were never intended to implement the Reactive-Streams specification (they were only inspired by the spec and are RxJava 2 only to begin with).

**API enhancements**
- [Pull 5117](https://github.com/ReactiveX/RxJava/pull/5117): Add `ParallelFlowable.sequentialDelayError`.
- [Pull 5137](https://github.com/ReactiveX/RxJava/pull/5137): Add `TestSubscriber.withTag`.
- [Pull 5140](https://github.com/ReactiveX/RxJava/pull/5140): Fix timed replay-like components replaying outdated items.
- [Pull 5155](https://github.com/ReactiveX/RxJava/pull/5155): Add `TestSubscriber.awaitCount`, `assertTimeout` & `assertNoTimeout`, improve assertion error message

**API deprecations**
- [Pull 5112](https://github.com/ReactiveX/RxJava/pull/5112): `Flowable.strict()` deprecated and will be removed in 2.1.0 - the default `Flowable` behavior is now strict.

**Bugfixes**
- [Pull 5101](https://github.com/ReactiveX/RxJava/pull/5101): Fix `Maybe.concat()` subscribe-after-cancel, verify others.
- [Pull 5103](https://github.com/ReactiveX/RxJava/pull/5103): Fix `doOnSubscribe` signalling `UndeliverableException` instead of `onError`.
- [Pull 5106](https://github.com/ReactiveX/RxJava/pull/5106): Fix `window(time, size)` not completing windows on timeout.
- [Pull 5114](https://github.com/ReactiveX/RxJava/pull/5114): Fix `Observable.combineLatest` to dispose eagerly.
- [Pull 5121](https://github.com/ReactiveX/RxJava/pull/5121): Fix `Observable.zip` to dispose eagerly.
- [Pull 5133](https://github.com/ReactiveX/RxJava/pull/5133): Fix `flatMap` not cancelling the upstream eagerly.
- [Pull 5136](https://github.com/ReactiveX/RxJava/pull/5136): Fix `repeatWhen` and `retryWhen` signatures.

**Other**
- [Pull 5102](https://github.com/ReactiveX/RxJava/pull/5102): Added missing `@NonNull` attribute to `Function7`.
- [Pull 5112](https://github.com/ReactiveX/RxJava/pull/5112): `Flowable` as a `Publisher` to be fully RS compliant.
- [Pull 5127](https://github.com/ReactiveX/RxJava/pull/5127): Update javadoc of `flatMap()` overloads.
- [Pull 5153](https://github.com/ReactiveX/RxJava/pull/5153): Java 9 compatibility fixes
- [Pull 5156](https://github.com/ReactiveX/RxJava/pull/5156): Add `@NonNull` to the methods of `Emitter`

### Version 2.0.6 - February 15, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.6%7C))

**Undeliverable exceptions**

One of the design goals of 2.x was that no errors can be lost. Sometimes, the sequence ends or gets cancelled before the source could emit an `onError` which has nowhere to go at that point and gets routed to the `RxJavaPlugins.onError`. 

Unlike 1.x, 2.x by default calls `Thread.currentThread().getUncaughtExceptionHandler().uncaughtException()` which crashes an Android app. Many developers have noticed the increased number of app crashes when porting to 2.x due to this behavior change. Note that this doesn't mean RxJava 2 is unstable but it means you likely had these exceptions all along but they were silently dropped.

Unfortunately, RxJava can't tell which of these out-of-lifecycle, undeliverable exceptions should or shouldn't crash your app. Identifying the source and reason for these exceptions can be tiresome, especially if they originate from a source and get routed to `RxJavaPlugins.onError` somewhere lower the chain.

Therefore, 2.0.6 introduces specific exception wrappers to help distinguish and track down what was happening the time of the error:

  - `OnErrorNotImplementedException`: reintroduced to detect when the user forgot to add error handling to `subscribe()`.
  - `ProtocolViolationException`: indicates a bug in an operator
  - `UndeliverableException`: wraps the original exception that can't be delivered due to lifecycle restrictions on a `Subscriber`/`Observer`. It is automatically applied by `RxJavaPlugins.onError` with intact stacktrace that may help find which exact operator rerouted the original error.

If an undeliverable exception is an instance/descendant of `NullPointerException`, `IllegalStateException` (`UndeliverableException` and `ProtocolViolationException` extend this), `IllegalArgumentException`, `CompositeException`, `MissingBackpressureException` or `OnErrorNotImplementedException`, the `UndeliverableException` wrapping doesn't happen.

In addition, some 3rd party libraries/code throw when they get interrupted by a cancel/dispose call which leads to an undeliverable exception most of the time. Internal changes in 2.0.6 now consistently cancel or dispose a `Subscription`/`Disposable` before cancelling/disposing a task or worker (which causes the interrupt on the target thread).

```java
// in some library
try {
   doSomethingBlockingly()
} catch (InterruptedException ex) {
   // check if the interrupt is due to cancellation
   // if so, no need to signal the InterruptedException
   if (!disposable.isDisposed()) {
      observer.onError(ex);
   }
}
```

If the library/code already did this, the undeliverable `InterruptedException`s should stop now. If this pattern was not employed before, we encourage updating the code/library in question.


**API enhancements**
- [Pull 5036](https://github.com/ReactiveX/RxJava/pull/5036): Reintroduce `OnErrorNotImplementedException` for 0-1 arg subscribe.
- [Pull 5043](https://github.com/ReactiveX/RxJava/pull/5043): Add parallel hooks to `RxJavaPlugins`, add missing params validation
- [Pull 5080](https://github.com/ReactiveX/RxJava/pull/5080): Wrap undeliverable errors.
- [Pull 5093](https://github.com/ReactiveX/RxJava/pull/5093): Add `Single.doAfterTerminate`

**Bugfixes**
- [Pull 5064](https://github.com/ReactiveX/RxJava/pull/5064): Fix `replay()` cancel/dispose `NullPointerException`.
- [Pull 5090](https://github.com/ReactiveX/RxJava/pull/5090): fix `scan(seed, f)` to emit accumulated values without delay.

**Other**
- [Pull 5027](https://github.com/ReactiveX/RxJava/pull/5027): Dedicated `Single.zip` implementation, no dispose on all-success.
- [Pull 5023](https://github.com/ReactiveX/RxJava/pull/5023): Annotate function interfaces.
- [Pull 5047](https://github.com/ReactiveX/RxJava/pull/5047): Document and test `amb` subscription ordering.
- [Pull 5051](https://github.com/ReactiveX/RxJava/pull/5051): More `@Nonnull` annotations.
- [Pull 5054](https://github.com/ReactiveX/RxJava/pull/5054): Add `@Nullable` annotation to `SimpleQueue`.
- [Pull 5055](https://github.com/ReactiveX/RxJava/pull/5055): More null checks.
- [Pull 5049](https://github.com/ReactiveX/RxJava/pull/5049): Use bounded wildcards for `errorHandler`.
- [Pull 5075](https://github.com/ReactiveX/RxJava/pull/5075): Cancel upstream first, dispose worker last.
- [Pull 5058](https://github.com/ReactiveX/RxJava/pull/5058): More generics in `RxJavaPlugins`.
- [Pull 5076](https://github.com/ReactiveX/RxJava/pull/5076): Removed documentation leftover of `Completable.subscribe`.
- [Pull 5087](https://github.com/ReactiveX/RxJava/pull/5087): Correct marble diagram dimensions.

### Version 2.0.5 - January 27, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.5%7C))

The most notable enhancement of this version is the inclusion of the `ParallelFlowable` API that allows parallel execution of a few select operators such as `map`, `filter`, `concatMap`, `flatMap`, `collect`, `reduce` and so on. Note that is a **parallel mode** for `Flowable` (a sub-domain specific language) instead of a new reactive base type. 

Consequently, several typical operators such as `take`, `skip` and many others are not available and there is no `ParallelObservable` because **backpressure** is essential in not flooding the internal queues of the parallel operators as by expectation, we want to go parallel because the processing of the data is slow on one thread.

The easiest way of entering the parallel world is by using `Flowable.parallel`:

```java
ParallelFlowable<Integer> source = Flowable.range(1, 1000).parallel();
```

By default, the parallelism level is set to the number of available CPUs (`Runtime.getRuntime().availableProcessors()`) and the prefetch amount from the sequential source is set to `Flowable.bufferSize()` (128). Both can be specified via overloads of `parallel()`.

`ParallelFlowable` follows the same principles of parametric asynchrony as `Flowable` does, therefore, `parallel()` on itself doesn't introduce the asynchronous consumption of the sequential source but only prepares the parallel flow; the asynchrony is defined via the `runOn(Scheduler)` operator.

```java
ParallelFlowable<Integer> psource = source.runOn(Schedulers.io());
```

The parallelism level (`ParallelFlowable.parallelism()`) doesn't have to match the parallelism level of the `Scheduler`. The `runOn` operator will use as many `Scheduler.Worker` instances as defined by the parallelized source. This allows `ParallelFlowable` to work for CPU intensive tasks via `Schedulers.computation()`, blocking/IO bound tasks through `Schedulers.io()` and unit testing via `TestScheduler`. You can specify the prefetch amount on `runOn` as well.

Once the necessary parallel operations have been applied, you can return to the sequential `Flowable` via the `ParallelFlowable.sequential()` operator.

```java
Flowable<Integer> result = psource.filter(v -> v % 3 == 0).map(v -> v * v).sequential();
```

Note that `sequential` doesn't guarantee any ordering between values flowing through the parallel operators.

For further details, please visit the [wiki page](https://github.com/ReactiveX/RxJava/wiki/Parallel-flows) about Parallel flows. (New content will be added there as time permits.)

**API enhancements**
- [Pull 4955](https://github.com/ReactiveX/RxJava/pull/4955): add `sample()` overload that can emit the very last buffered item.
- [Pull 4966](https://github.com/ReactiveX/RxJava/pull/4966): add `strict()` operator for strong Reactive-Streams conformance
- [Pull 4967](https://github.com/ReactiveX/RxJava/pull/4967): add subjects for `Single`, `Maybe` and `Completable`
- [Pull 4972](https://github.com/ReactiveX/RxJava/pull/4972): Improve `compose()` generics
- [Pull 4973](https://github.com/ReactiveX/RxJava/pull/4973): Add `Completable.hide()`
- [Pull 4974](https://github.com/ReactiveX/RxJava/pull/4974): add `Flowable.parallel()` and parallel operators
- [Pull 5002](https://github.com/ReactiveX/RxJava/pull/5002): Add scheduler creation factories

**Bugfixes**
- [Pull 4957](https://github.com/ReactiveX/RxJava/pull/4957): fix `LambdaObserver` calling dispose when terminating
- [Pull 4962](https://github.com/ReactiveX/RxJava/pull/4962): fix `takeUntil()` other triggering twice
- [Pull 4970](https://github.com/ReactiveX/RxJava/pull/4970): fix `withLatestFrom` null checks, lifecycle
- [Pull 4982](https://github.com/ReactiveX/RxJava/pull/4982): fix `Observable.concatMapEager` bad logic for immediate scalars.
- [Pull 4984](https://github.com/ReactiveX/RxJava/pull/4984): fix cross-boundary invalid fusion with `observeOn`, `flatMap` & `zip`
- [Pull 4987](https://github.com/ReactiveX/RxJava/pull/4987): Make `Observable.combineLatest` consistent with `Flowable`, fix early termination cancelling the other sources and document empty source case
- [Pull 4992](https://github.com/ReactiveX/RxJava/pull/4992): `A.flatMapB` to eagerly check for cancellations before subscribing
- [Pull 5005](https://github.com/ReactiveX/RxJava/pull/5005): `ExecutorScheduler.scheduleDirect` to report `isDisposed` on task completion

**Other**
- [Pull 4971](https://github.com/ReactiveX/RxJava/pull/4971): Add `@CheckReturnValue` to `create()` methods of `Subjects` + `Processors`
- [Pull 4980](https://github.com/ReactiveX/RxJava/pull/4980): Update Copyright to 'RxJava Contributors'
- [Pull 4990](https://github.com/ReactiveX/RxJava/pull/4990): Update marble diagrams for `sample()` overloads, Maybe and `Maybe.switchIfEmpty()`
- [Pull 5015](https://github.com/ReactiveX/RxJava/pull/5015): Fix Reactive-Streams dependency to be `compile` in the library's POM
- [Pull 5020](https://github.com/ReactiveX/RxJava/pull/5020): option to fail for using blockingX on the computation/single scheduler

### Version 2.0.4 - January 4, 2017 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.4%7C))

**API enhancements**
- [Pull 4930](https://github.com/ReactiveX/RxJava/pull/4930): Add `Completable.cache()` 

**Bugfixes**
- [Pull 4927](https://github.com/ReactiveX/RxJava/pull/4927): fix `timer()` `IllegalStateException` due to bad resource management
- [Pull 4932](https://github.com/ReactiveX/RxJava/pull/4932): Add safeguards to `generate()` 
- [Pull 4943](https://github.com/ReactiveX/RxJava/pull/4943): Fix `publish(Function)` not replenishing its internal queue 
- [Pull 4945](https://github.com/ReactiveX/RxJava/pull/4945): Fix `timeout` with fallback not cancelling/disposing the main source connection. 

### Version 2.0.3 - December 18, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.3%7C))

**Bugfixes**
- [Pull 4899](https://github.com/ReactiveX/RxJava/pull/4899): `FlowableScanSeed` - prevent post-terminal events
- [Pull 4901](https://github.com/ReactiveX/RxJava/pull/4901): `FlowableScan` - prevent multiple terminal emissions
- [Pull 4903](https://github.com/ReactiveX/RxJava/pull/4903): `doAfterNext` - prevent post-terminal emission
- [Pull 4904](https://github.com/ReactiveX/RxJava/pull/4904): `Observable.scan` no seed fix post-terminal behaviour
- [Pull 4911](https://github.com/ReactiveX/RxJava/pull/4911): fix & update `Observable.repeatWhen` and `retryWhen`
- [Pull 4924](https://github.com/ReactiveX/RxJava/pull/4924): `flatMapCompletable` change `Completable` to `CompletableSource`

**Other**
- [Pull 4907](https://github.com/ReactiveX/RxJava/pull/4907): Use `t` instead of value to allow for IDE naming

### Version 2.0.2 - December 2, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.2%7C))

**API enhancements**
- [Pull 4858](https://github.com/ReactiveX/RxJava/pull/4858): add `Maybe.flatMapSingleElement` returning `Maybe`
- [Pull 4881](https://github.com/ReactiveX/RxJava/pull/4881): Add `@CheckReturnValue` annotation to aid static verification tools

**Performance enhancements**
- [Pull 4885](https://github.com/ReactiveX/RxJava/pull/4885): Dedicated `reduce()` op implementations

**Bugfixes**
- [Pull 4873](https://github.com/ReactiveX/RxJava/pull/4873): `TestObserver` shouldn't clear the upstream disposable on terminated
- [Pull 4877](https://github.com/ReactiveX/RxJava/pull/4877): Apply missing `RxJavaPlugins.onAssembly` on ops
- [Commit bf0c](https://github.com/ReactiveX/RxJava/commit/8429dc7671e2b26b6048ab47fa59e286dd77bf0c): Fix `firstOrError` converted back to `Observable`/`Flowable` not reporting `NoSuchElementException`.


### Version 2.0.1 - November 12, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.1%7C))

**API enhancements**
- [Pull 4827](https://github.com/ReactiveX/RxJava/pull/4827): Porting the `Scheduler.when` operator from 1.x to 2.x
- [Pull 4831](https://github.com/ReactiveX/RxJava/pull/4831): add `Flowable.doFinally(Action)` for handling post-terminal or cancel cleanup.
- [Pull 4832](https://github.com/ReactiveX/RxJava/pull/4832): add `doFinally` to the rest of the reactive base classes
- [Pull 4833](https://github.com/ReactiveX/RxJava/pull/4833): add `Flowable.doAfterNext` operator
- [Pull 4835](https://github.com/ReactiveX/RxJava/pull/4835): add `Observable.doAfterNext` and `{Single|Maybe}.doAfterSuccess`.
- [Pull 4838](https://github.com/ReactiveX/RxJava/pull/4838): add fluent `TestSubscriber.requestMore`

**Documentation fixes/enhancements**
- [Pull 4793](https://github.com/ReactiveX/RxJava/pull/4793): Fix javadoc mentioning `IllegalArgumentException` instead of `NullPointerException` for calling with `null` parameter(s).
- [Pull 4798](https://github.com/ReactiveX/RxJava/pull/4798): Fix `Observable.toFlowable` documentation
- [Pull 4803](https://github.com/ReactiveX/RxJava/pull/4803): Fix `ObservableEmitter` mentioning `FlowableEmitter`.
- [Pull 4810](https://github.com/ReactiveX/RxJava/pull/4810): Fix `Completable.retryWhen` terminology about signal emission.
- [Pull 4815](https://github.com/ReactiveX/RxJava/pull/4815): Fix typo in javadoc of `Maybe.toSingle`
- [Pull 4839](https://github.com/ReactiveX/RxJava/pull/4839): fix wording of some operators, remove `@throws` implications

**Bugfixes**
- [Pull 4783](https://github.com/ReactiveX/RxJava/pull/4783): Fix `Observable.repeatWhen` & `retryWhen` not disposing the inner.
- [Pull 4819](https://github.com/ReactiveX/RxJava/pull/4819): Fix `Observable.repeatWhen` not reacting to upstream `onError` properly.

### Version 2.0.0 - October 29, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.0%7C))

This is it, general availability of RxJava 2! Rewritten from scratch to offer better performance, lower overhead, more features, a modern underlying technology and interoperation with the Reactive-Streams ecosystem. Big thanks goes to the several dozen people who gave feedback, fixes, enhancements and reviewed pull requests in the past, very intensive, 4 months.

Users are encouraged to read the wiki articles [What's different in 2.0](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0) and [Writing operators for 2.0](https://github.com/ReactiveX/RxJava/wiki/Writing-operators-for-2.0) to get a overview about changes and differences between working with RxJava 1 and 2. If you find something missing or under-explained, don't worry and open an [issue](https://github.com/ReactiveX/RxJava/issues) about it!

Some other common libraries such as [RxAndroid](https://github.com/ReactiveX/RxAndroid/tree/2.x) and [Retrofit 2 Adapter](https://github.com/JakeWharton/retrofit2-rxjava2-adapter) were already following the 2.x development and you can expect them to release versions supporting the 2.0.0 GA shortly. In addition, there is an ongoing effort to port companion libraries of RxJava itself to support the 2.x line. For now, several ported features are available as part of the [RxJava2Extensions](https://github.com/akarnokd/RxJava2Extensions) project. RxJava 1 and 2 can live side by side in the same project and the [RxJava2Interop](https://github.com/akarnokd/RxJava2Interop) library allows dataflow conversions between the two versions.

The sections below contain the changes since 2.0.0-RC5 beyond the general quality and test coverage improvements of the codebase.

**API enhancements**

  - [Pull 4760](https://github.com/ReactiveX/RxJava/pull/4760): Add `Single.fromObservable(ObservableSource)`
  - [Pull 4767](https://github.com/ReactiveX/RxJava/pull/4767): Rename `BackpressureStrategy.NONE` to `MISSING`

**Documentation enhancements**

  - [Pull 4744](https://github.com/ReactiveX/RxJava/pull/4744): Fixed Javadoc for `Disposables.fromFuture`
  - [Pull 4749](https://github.com/ReactiveX/RxJava/pull/4749): New 2.x marble diagrams
  - [Pull 4752](https://github.com/ReactiveX/RxJava/pull/4752): Add more new marble diagrams & update old ones

**Performance enhancements**

  - [Pull 4742](https://github.com/ReactiveX/RxJava/pull/4742): 
    - cleanup `Flowable.publish()` and enable operator fusion on its input
    - compact `Flowable.blockingSubscribe()`, `Observable.blockingSubscribe()` and `Flowable.subscribeOn()`
  - [Pull 4761](https://github.com/ReactiveX/RxJava/pull/4761):
    - Unify `MapNotification`, `Materialize`, `OnErrorReturn` last element backpressure under the same helper class.
    - Reuse parts of `FlowableSequenceEqual` in `FlowableSequenceEqualSingle`.
  - [Pull 4766](https://github.com/ReactiveX/RxJava/pull/4766): `Flowable.scan(T, BiFunction)` now emits the initial value only when the upstream signals an event. 

**Bugfixes**

  - [Pull 4742](https://github.com/ReactiveX/RxJava/pull/4742):
    - Fix `Flowable.materialize()` terminal signal emission in face of backpressure.
    - Fix `Flowable.onErrorReturn()` terminal signal emission in face of backpressure.
    - fix `Flowable.flatMapSingle() `and `Flowable.flatMapMaybe()` termination detection
  - [Pull 4747](https://github.com/ReactiveX/RxJava/pull/4747): `distinctUntilChanged` to store the selected key instead of the value
  - [Pull 4751](https://github.com/ReactiveX/RxJava/pull/4751): fix `Flowable.concatMapEager` hang due to bad request management.
  - [Pull 4761](https://github.com/ReactiveX/RxJava/pull/4761): Fix cancellation bugs in `Flowable` operators.
  - [Pull 4763](https://github.com/ReactiveX/RxJava/pull/4763): rewrite of `takeUntil` to avoid `onSubscribe()` races.
  - [Pull 4766](https://github.com/ReactiveX/RxJava/pull/4766):
    - Fix `Flowable.skipUntil` lifecycle and concurrency properties.
    - Fix `Flowable.concatMapEager` error management.
  - [Pull 4770](https://github.com/ReactiveX/RxJava/pull/4770): allow `subscribeOn` to work with blocking create.

### Version 2.0.0-RC5 - October 21, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.0-RC5%7C))

This release contains API fixes, further cleanups to code and javadoc, better test coverage and bugfixes. Thanks to the respective contributors and @JakeWharton for the reviews.

**API enhancements**

  - [Pull 4685](https://github.com/ReactiveX/RxJava/pull/4685): Test static from methods and add `Maybe.fromSingle` & `fromCompletable`
  - [Pull 4687](https://github.com/ReactiveX/RxJava/pull/4687): Add `Observable.rangeLong` & `Flowable.rangeLong`.
  - [Pull 4690](https://github.com/ReactiveX/RxJava/pull/4690): `BaseTestConsumer` add `assertValueAt(index, Predicate<T>)`.
  - [Pull 4711](https://github.com/ReactiveX/RxJava/pull/4711): Decouple stream operators from Function interface (`FlowableOperator` and co).
  - [Pull 4712](https://github.com/ReactiveX/RxJava/pull/4712): make `Observable.sequenceEqual` return Single<Boolean>
  - [Pull 4714](https://github.com/ReactiveX/RxJava/pull/4714): have `Flowable.toList(Callable)` return Single
  - [Pull 4720](https://github.com/ReactiveX/RxJava/pull/4720): remove variance from the input source of `retryWhen`
  - [Pull 4723](https://github.com/ReactiveX/RxJava/pull/4723): remove `flatMapIterable(Function, int)` overload and have `flatMapIterable(Function)` use the flatten operator.
  - [Pull 4729](https://github.com/ReactiveX/RxJava/pull/4729): Merge `FlowableEmitter.BackpressureMode` into `BackpressureStrategy`
  - [Pull 4710](https://github.com/ReactiveX/RxJava/pull/4710): Remove checked exceptions from transformer interfaces.

**Performance enhancements**

  - [Pull 4723](https://github.com/ReactiveX/RxJava/pull/4723): enable fusion on `Observable.observeOn`

**Bugfixes**

  - [Pull 4681](https://github.com/ReactiveX/RxJava/pull/4681): Fix `Flowable` + `Single` `elementAt` and `elementAtOrError` operators on empty sources.
  - [Pull 4686](https://github.com/ReactiveX/RxJava/pull/4686): Fix `flatMapX` over-cancellation in case of an inner error.
  - [Pull 4689](https://github.com/ReactiveX/RxJava/pull/4689): Fix `doOnEvent` NPE on `dispose()`
  - [Pull 4695](https://github.com/ReactiveX/RxJava/pull/4695): `CompositeException` fix order of exceptions
  - [Pull 4696](https://github.com/ReactiveX/RxJava/pull/4696): Fix inner `Throwable` order for `CompletablePeek`
  - [Pull 4705](https://github.com/ReactiveX/RxJava/pull/4705): fix `Observable.flatMap`'s dispose behavior and error accumulation
  - [Pull 4707](https://github.com/ReactiveX/RxJava/pull/4707): Fix `Flowable.elementAt` on empty sources. 
  - [Pull 4708](https://github.com/ReactiveX/RxJava/pull/4708): fix `Observable.publish(Function)` latecommer behavior
  - [Pull 4712](https://github.com/ReactiveX/RxJava/pull/4712): fix `Observable.combineLatest` error management, fix `Observable.flatMap` `maxConcurrency` behavior with scalars, use of unbounded queue, fix `Observable.timeInterval` not saving the `Disposable`
  - [Pull 4723](https://github.com/ReactiveX/RxJava/pull/4723): fix fusion of `Observable.just`, fix `Observable.replay()` potential emission before `onSubscribe` call
  - [Pull 4731](https://github.com/ReactiveX/RxJava/pull/4731): Delegate null `Collection`s down to `onError` in `toList`
  - [Pull 4736](https://github.com/ReactiveX/RxJava/pull/4736): fix `onBackpressureBuffer(long, Action, BufferOverflowStrategy)` return type, fix `concatMapDelayError` wrong barrier mode selected.
  - [Pull 4738](https://github.com/ReactiveX/RxJava/pull/4738): Fix `Flowable.flatMap` error, cancellation and resource management.

**Removals**

  - [Pull 4689](https://github.com/ReactiveX/RxJava/pull/4689): Remove `Maybe.toCompletable`, use `Maybe.ignoreElement`.
  - [Pull 4708](https://github.com/ReactiveX/RxJava/pull/4708): remove `bufferSize` overloads of `Observable.publish` as there is no need to buffer anything for an `Observable`
  - [Pull 4723](https://github.com/ReactiveX/RxJava/pull/4723): remove `flatMapIterable(Function, int)` overload

**Other**

  - [Pull 4682](https://github.com/ReactiveX/RxJava/pull/4682): Update Mockito to 2.1.0
  - [Pull 4699](https://github.com/ReactiveX/RxJava/pull/4699): Clean up null usages by using `ObjectHelper.requireNonNull`.


### Version 2.0.0-RC4 - October 7, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.0-RC4%7C))

This release contains new operators, further cleanups, better test coverage and bugfixes. Thanks to the respective contributors and @JakeWharton for the reviews.

**API enhancements**

  - [Pull 4589](https://github.com/ReactiveX/RxJava/pull/4589): Add `singleOrError`, `firstOrError`, `lastOrError` & `elementAtOrError` to `Observable` and `Flowable`
  - [Pull 4616](https://github.com/ReactiveX/RxJava/pull/4616): Add `Completable.andThen(MaybeSource)`
  - [Pull 4614](https://github.com/ReactiveX/RxJava/pull/4614): Add `Maybe.flatMapSingle`
  - [Pull 4617](https://github.com/ReactiveX/RxJava/pull/4617): Add `Single.flatMapMaybe`
  - [Pull 4585](https://github.com/ReactiveX/RxJava/pull/4585): Evaluate `Schedulers` initialization via `Callable`
  - [Pull 4607](https://github.com/ReactiveX/RxJava/pull/4607): `TestSubscriber` & `TestObserver` add `assertValue(Predicate)`.
  - [Pull 4627](https://github.com/ReactiveX/RxJava/pull/4627): Use predicates in `BaseTestConsumer.assertError(Class/Throwable)` to remove duplicate code, tests tweaks to remove few IDE warnings
  - [Pull 4629](https://github.com/ReactiveX/RxJava/pull/4629): Add `Completable.fromRunnable()`
  - [Pull 4631](https://github.com/ReactiveX/RxJava/pull/4631): `TestConsumer` don't wrap with `CompositeException` when there is only one error
  - [Pull 4604](https://github.com/ReactiveX/RxJava/pull/4604): add `flattenAs{Observable,Flowable}` to `Single` and `Maybe`
  - [Pull 4658](https://github.com/ReactiveX/RxJava/pull/4658): `Observable.compose` to use `ObservableTransformer`.
  - [Pull 4667](https://github.com/ReactiveX/RxJava/pull/4667): Add `flatMap{Completable, Maybe, Single}` operators to `Flowable` and `Observable`.
  - [Pull 4672](https://github.com/ReactiveX/RxJava/pull/4672): Remove `Function` from transformer interfaces to allow a single obj.
  - `Maybe.ignoreElement` to return `Completable`.

**Performance enhancements**

  - [Pull 4612](https://github.com/ReactiveX/RxJava/pull/4612): Improve performance of `Observable.flatMapIterable`
  - [Pull 4622](https://github.com/ReactiveX/RxJava/pull/4622): Enable operator fusion in `onBackpressureBuffer`

**Bugfixes**

  - [Pull 4590](https://github.com/ReactiveX/RxJava/pull/4590): Report errors from `onError` to Plugin when done.
  - [Pull 4592](https://github.com/ReactiveX/RxJava/pull/4592): `UnicastSubject` fix `onTerminate`
  - [Pull 4593](https://github.com/ReactiveX/RxJava/pull/4593): Enhance NPE messages
  - [Pull 4603](https://github.com/ReactiveX/RxJava/pull/4603): RxJavaPlugins - Don't pass null throwable down to Error Handler
  - [Pull 4619](https://github.com/ReactiveX/RxJava/pull/4619): Make `CompositeExcepetion` thread-safe like 1.x and also fix some issues.
  - [Pull 4645](https://github.com/ReactiveX/RxJava/pull/4645): Signal NPE `ObservableAmb` `FlowableAmb`
  - [Pull 4651](https://github.com/ReactiveX/RxJava/pull/4651): Switch `Maybe` and `Single` to use their `Transformer`s in `compose()`.
  - [Pull 4654](https://github.com/ReactiveX/RxJava/pull/4654): Upcast `ConcurrentHashMap` to `Map` to avoid compatibility issue.


**Removals**

  - [Pull 4595](https://github.com/ReactiveX/RxJava/pull/4595): Remove `takeFirst(predicate)` from `Observable` & `Flowable`

**Other**

  - [Pull 4647](https://github.com/ReactiveX/RxJava/pull/4647): Merge `AmbArray` and `AmbIterable` into `Amb` for `Single`, `Maybe` and `Completable` types.

### Version 2.0.0-RC3 - September 23, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.0-RC3%7C))

This Release Candidate features mostly internal cleanups of code and Javadoc, Reactive-Streams Test Compatibility Kit implementations (and thus verification) of our `Flowable` implementation and additional unit-test coverage. Big tanks to @vanniktech for the many contributions and @JakeWharton for the continued reviews.

Other notable changes:

  - The new `Maybe`  type of RC2 now features all relevant operators.
  - Many `Observable` and `Flowable` operators [now return](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#different-return-types) `Single` or `Maybe`.
  - [Pull 4525](https://github.com/ReactiveX/RxJava/pull/4525) : Fixed generics of `combineLatest` and `zip` because `Function<? super T[], R>` doesn't work.
  - [Pull 4522](https://github.com/ReactiveX/RxJava/pull/4522) : Fixed `delay` operator to call `onError` on the provided scheduler 
  - [Pull 4490](https://github.com/ReactiveX/RxJava/pull/4490) : rename `isCancelled` to `isDisposed` inside the varios emitters of `create()`
  - [Pull 4504](https://github.com/ReactiveX/RxJava/pull/4504) : add default `XObserver` implementation to `Single`, `Maybe` and `Completable`
  - [Pull 4518](https://github.com/ReactiveX/RxJava/pull/4518) : add `ResourceXObserver` implementation to `Single`, `Maybe` and `Completable`
  - [Pull 4536](https://github.com/ReactiveX/RxJava/pull/4536) : fix `timeout` operators not properly cancelling/disposing the upstream.
  - [Pull 4583](https://github.com/ReactiveX/RxJava/pull/4583) : `Flowable.create()`'s `FlowableEmitter` didn't handle `null`s properly.
  - [Pull 4586](https://github.com/ReactiveX/RxJava/pull/4586) : Add error assertion with predicate to `TestSubscriber` and `TestObserver`.

In addition, the wiki page [What's different in 2.0](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0) has been extended with more information.

### Version 2.0.0-RC2 - September 5, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.0-RC2%7C))

This Release Candidate features a large amount of internal fixes, cleanups and enhancements. Please refer to the [closed PRs](https://github.com/ReactiveX/RxJava/pulls?q=is%3Apr+is%3Aclosed+milestone%3A%222.0+RC+2%22) for its milestone. This includes the bugfix for the `Single.subscribeOn` failing with `Disposable already set!` error printed to the console ([Issue 4448](https://github.com/ReactiveX/RxJava/issue/4448), [Pull 4450](https://github.com/ReactiveX/RxJava/pull/4450)).

The library now has a new base reactive type: `Maybe` which is essentially a `Single` and `Completable` combined. You can have exactly 1 onSuccess, 1 onError or 1 onComplete signals with it. Note however that it doesn't have all the operators possible for this type of source but you can convert it back and forth to the other base reactive types (i.e., `Flowable`, `Single`, `Completable` and `Observable`). Unlike Project Reactor's `Mono` type, this doesn't implement the Reactive-Streams `Publisher` (but is designed along the idea) and doesn't have backpressure (as there is no chance of buffer-bloat like with unknown length `Flowable`s and `Observable`s).


### Version 2.0.0-RC1 - August 25, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex.rxjava2%7Crxjava%7C2.0.0-RC1%7C))

RxJava 2.0 has been rewritten from scratch to be more performant, lower overhead (memory and cpu) and to natively implement the Reactive-Streams specification in its new `Flowable` type. The library still supports Java 6+ and is considered a **4th generation reactive library**.

There have been hundreds of PRs and large amounts of changes compared to the 1.x version. The two are binary-incompatible but their distinct maven coordinates (`io.reactivex.rxjava2:rxjava:2.0.0-RC1`) allow them to co-exist. You can use the [rxjava2-interop](https://github.com/akarnokd/RxJava2Interop) library to convert between 1.x and 2.x types.

Please refer to the [wiki page about the differences between the two major versions](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0).

Converting the companion libraries is an ongoing effort. For now, see the [rxjava2-extensions](https://github.com/akarnokd/RxJava2Extensions) project that contains the port of the [RxJavaMath](https://github.com/ReactiveX/RxJavaMath) features. Note that most companion libraries of RxJava are currently not set up properly for release (the signing keys are missing from the CI release process) and is unknown if and when they will have their own 2.x release branch.

I'd like to thank for the contributions of the following community members (in LIFO commit-merge order):

@davidmoten, @JakeWharton, @vanniktech, @dimitar-asenov, @artem-zinnatullin, @bobvanderlinden, @DmitriyZaitsev, @zsxwing, @bcorne, @stevegury, @benjchristensen, @stealthcode, @adam-arold and @abersnaze.

A special thanks goes out to @smaldini, [Project-Reactor](https://github.com/reactor/reactor-core) lead at Pivotal. Our ongoing cooperation lead to the massive improvement of the RxJava 2.x architecture (originally designed in August 2015) by learning from and building upon the results of the [Reactive-Streams-Commons](https://github.com/reactor/reactive-streams-commons) research project.
