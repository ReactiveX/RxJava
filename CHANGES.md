# RxJava Releases #

Version 1.x can be found at https://github.com/ReactiveX/RxJava/blob/1.x/CHANGES.md

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
