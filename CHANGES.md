# RxJava Releases #

### Version 1.1.6 - June 15, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.1.6%7C))

#### API enhancements

  - [Pull 3934](https://github.com/ReactiveX/RxJava/pull/3934): `TestSubscriber` extra info on assertion failures
  - [Pull 3948](https://github.com/ReactiveX/RxJava/pull/3948): add `Completable.andThen(Completable)`
  - [Pull 3942](https://github.com/ReactiveX/RxJava/pull/3942): add `Completable.subscribe` to be safe, add `unsafeSubscribe` option + `RxJavaPlugins` hook support 
  - [Pull 3936](https://github.com/ReactiveX/RxJava/pull/3936): promote `UnicastSubject` to be a standard+experimental `Subject`
  - [Pull 3971](https://github.com/ReactiveX/RxJava/pull/3971): add `Observable.rebatchRequests` operator to change and stabilize request amounts of the downstream.
  - [Pull 3986](https://github.com/ReactiveX/RxJava/pull/3986): add `Schedulers.reset()` for better testing
  - [Pull 3918](https://github.com/ReactiveX/RxJava/pull/3918): `ReplaySubject` now supports backpressure 

#### API deprecations

  - [Pull 3948](https://github.com/ReactiveX/RxJava/pull/3948) deprecate `Completable.endWith()` in favor of `andThen()`

#### Performance enhancements

  - [Pull 3470](https://github.com/ReactiveX/RxJava/pull/3470): `replay` request coordination reduce overhead

#### Bugfixes

  - [Pull 3924](https://github.com/ReactiveX/RxJava/pull/3924): fix `RxRingBuffer`-pool depending on the `computation` scheduler 
  - [Pull 3922](https://github.com/ReactiveX/RxJava/pull/3922): fix `using()` resource cleanup when factory throws or being non-eager 
  - [Pull 3941](https://github.com/ReactiveX/RxJava/pull/3941): fix `Single.flatMap` not composing subscription through 
  - [Pull 3958](https://github.com/ReactiveX/RxJava/pull/3958): fix `just()` construction to call the `onCreate` execution hook 
  - [Pull 3977](https://github.com/ReactiveX/RxJava/pull/3977): Use the correct `Throwable` to set the cause for `CompositeException`
  - [Pull 4005](https://github.com/ReactiveX/RxJava/pull/4005): Fix Spsc queues reporting not empty but then poll() returns null.

### Version 1.1.5 - May 5, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.1.5%7C))

#### Bugfixes

  - [Pull 3912](https://github.com/ReactiveX/RxJava/pull/3912): fix filter() default-requesting and thus going unbounded

### Version 1.1.4 - May 3, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.1.4%7C))

#### API enhancements

  - [Pull 3856](https://github.com/ReactiveX/RxJava/pull/3856): Provide factories for creating the default scheduler instances.
  - [Pull 3866](https://github.com/ReactiveX/RxJava/pull/3866): Add `Single.toCompletable()`.
  - [Pull 3879](https://github.com/ReactiveX/RxJava/pull/3879): Expose scheduler factories which accept thread factories.
  - [Pull 3820](https://github.com/ReactiveX/RxJava/pull/3820): Making RxPlugins `reset()` public
  - [Pull 3888](https://github.com/ReactiveX/RxJava/pull/3888): New operator: `onTerminateDetach` - detach upstream/downstream for GC

#### API deprecations

  - [Pull 3871](https://github.com/ReactiveX/RxJava/pull/3871): Deprecate remaining public scheduler types.

#### Performance enhancements

  - [Pull 3761](https://github.com/ReactiveX/RxJava/pull/3761): optimize `merge`/`flatMap` for empty sources.
  - [Pull 3864](https://github.com/ReactiveX/RxJava/pull/3864): optimize `concatMapIterable`/`flatMapIterable`. 

#### Bugfixes

  - [Pull 3868](https://github.com/ReactiveX/RxJava/pull/3868): Fix an unsubscribe race in `EventLoopWorker`.
  - [Pull 3867](https://github.com/ReactiveX/RxJava/pull/3867): ensure waiting tasks are cancelled on worker unsubscription.
  - [Pull 3862](https://github.com/ReactiveX/RxJava/pull/3862): fix `from(Iterable)` error handling of Iterable/Iterator
  - [Pull 3886](https://github.com/ReactiveX/RxJava/pull/3886): `throwIfFatal()` now throws `OnCompletedFailedException`.
  - [Pull 3887](https://github.com/ReactiveX/RxJava/pull/3887): Have undeliverable errors on `subscribe()` sent to plugin error handler.
  - [Pull 3895](https://github.com/ReactiveX/RxJava/pull/3895): `cast()` should unsubscribe on crash eagerly.
  - [Pull 3896](https://github.com/ReactiveX/RxJava/pull/3896): `OperatorMapPair` should unsubscribe on crash eagerly.
  - [Pull 3890](https://github.com/ReactiveX/RxJava/pull/3890): `map()` and `filter()` should unsubscribe on crash eagerly.
  - [Pull 3893](https://github.com/ReactiveX/RxJava/pull/3893): enable backpressure with `from(Future)`
  - [Pull 3883](https://github.com/ReactiveX/RxJava/pull/3883): fix multiple chained `Single.doAfterTerminate` not calling actions
  - [Pull 3904](https://github.com/ReactiveX/RxJava/pull/3904): Fix `Completable` swallows `OnErrorNotImplementedException`
  - [Pull 3905](https://github.com/ReactiveX/RxJava/pull/3905): fix `singleOrDefault()` backpressure if source is empty

### Version 1.1.3 - April 8, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.1.3%7C))

#### API enhancements

  - [Pull 3780](https://github.com/ReactiveX/RxJava/pull/3780): `SyncOnSubscribe` has been promoted to `@Beta` API.
  - [Pull 3799](https://github.com/ReactiveX/RxJava/pull/3799): Added `Completable.andThen(Single)` operator
  - [Pull 3777](https://github.com/ReactiveX/RxJava/pull/3777): Added `observeOn` overload to configure the prefetch/buffer size
  - [Pull 3790](https://github.com/ReactiveX/RxJava/pull/3790): Make `Single.lift` public and `@Experimental`
  - [Pull 3818](https://github.com/ReactiveX/RxJava/pull/3818): `fromCallable` promotion to `@Beta` 
  - [Pull 3842](https://github.com/ReactiveX/RxJava/pull/3842): improve `ExecutorScheduler` worker unsubscription

#### API deprecations

  - [Pull 3762](https://github.com/ReactiveX/RxJava/pull/3762): Deprecate `CompositeException` constructor with message prefix

#### General enhancements

  - [Pull 3828](https://github.com/ReactiveX/RxJava/pull/3828): `AsyncSubject` now supports backpressure
  - [Pull 3829](https://github.com/ReactiveX/RxJava/pull/3829): Added `rx.unsafe-disable` system property to disable use of `sun.misc.Unsafe` even if it is available
  - [Pull 3757](https://github.com/ReactiveX/RxJava/pull/3757): **Warning: behavior change!** Operator `sample` emits last sampled value before termination

#### Performance enhancements

  - [Pull 3795](https://github.com/ReactiveX/RxJava/pull/3795): `observeOn` now replenishes with constant rate

#### Bugfixes

  - [Pull 3809](https://github.com/ReactiveX/RxJava/pull/3809): fix `merge`/`flatMap` crash when the inner source was `just(null)`
  - [Pull 3789](https://github.com/ReactiveX/RxJava/pull/3789): Prevent `Single.zip()` of zero `Single`s
  - [Pull 3787](https://github.com/ReactiveX/RxJava/pull/3787): fix `groupBy` delaying group completion till all groups were emitted
  - [Pull 3823](https://github.com/ReactiveX/RxJava/pull/3823): fix `DoAfterTerminate` handle if action throws
  - [Pull 3822](https://github.com/ReactiveX/RxJava/pull/3822): make defensive copy of the properties in `RxJavaPlugins`
  - [Pull 3836](https://github.com/ReactiveX/RxJava/pull/3836): fix `switchMap`/`switchOnNext` producer retention and backpressure
  - [Pull 3840](https://github.com/ReactiveX/RxJava/pull/3840): fix `concatMap` scalar/empty source behavior
  - [Pull 3839](https://github.com/ReactiveX/RxJava/pull/3839): fix `takeLast()` backpressure
  - [Pull 3845](https://github.com/ReactiveX/RxJava/pull/3845): fix delaySubscription(Observable) unsubscription before triggered


### Version 1.1.2 - March 18, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.1.2%7C))

#### API Enhancements

  - [Pull 3766](https://github.com/ReactiveX/RxJava/pull/3766): Add `Single.onErrorResumeNext(Func1)` operator
  - [Pull 3765](https://github.com/ReactiveX/RxJava/pull/3765): Add `Observable.switchOnNextDelayError` and `Observable.switchMapDelayError` operators
  - [Pull 3759](https://github.com/ReactiveX/RxJava/pull/3759): Add `Observable.concatDelayError` and `Observable.concatMapDelayError` operators
  - [Pull 3763](https://github.com/ReactiveX/RxJava/pull/3763): Add `Observable.combineLatestDelayError` operator
  - [Pull 3752](https://github.com/ReactiveX/RxJava/pull/3752): Add `Single.using` operator
  - [Pull 3722](https://github.com/ReactiveX/RxJava/pull/3722): Add `Observable.flatMapIterable` overload with `maxConcurrent` parameter
  - [Pull 3741](https://github.com/ReactiveX/RxJava/pull/3741): Add `Single.doOnSubscribe` operator
  - [Pull 3738](https://github.com/ReactiveX/RxJava/pull/3738): Add `Observable.create(SyncOnSubscribe)` and `Observable.create(AsyncOnSubscribe)` factory methods
  - [Pull 3718](https://github.com/ReactiveX/RxJava/pull/3718): Add `Observable.concatMapIterable` operator
  - [Pull 3712](https://github.com/ReactiveX/RxJava/pull/3712): Add `Single.takeUntil(Completable)` operator
  - [Pull 3696](https://github.com/ReactiveX/RxJava/pull/3696): Added Single execution hooks via `RxJavaSingleExecutionHook` class. **Warning**! This PR introduced a binary incompatible change of `Single.unsafeSubscribe(Subscriber)` by changing its return type from `void` to `Subscription`.
  - [Pull 3487](https://github.com/ReactiveX/RxJava/pull/3487): Add `onBackpressureBuffer` overflow strategies (oldest, newest, error)

#### API deprecations

  - [Pull 3701](https://github.com/ReactiveX/RxJava/pull/3701): deprecate `Completable.doOnComplete` in favor of `Completable.doOnCompleted` (note the last d in the method name)

#### Performance enhancements

  - [Pull 3759](https://github.com/ReactiveX/RxJava/pull/3759): Add `Observable.concatDelayError` and `Observable.concatMapDelayError` operators
  - [Pull 3476](https://github.com/ReactiveX/RxJava/pull/3476): reduced `range` and `flatMap/merge` overhead

#### Bugfixes

  - [Pull 3768](https://github.com/ReactiveX/RxJava/pull/3768): Fix `observeOn` in-sequence termination/unsubscription checking
  - [Pull 3733](https://github.com/ReactiveX/RxJava/pull/3733): Avoid swallowing errors in `Completable`
  - [Pull 3727](https://github.com/ReactiveX/RxJava/pull/3727): Fix `scan` not requesting `Long.MAX_VALUE` from upstream if downstream has requested `Long.MAX_VALUE`
  - [Pull 3707](https://github.com/ReactiveX/RxJava/pull/3707): Lambda-based `Completable.subscribe()` methods should report `isUnsubscribed` properly
  - [Pull 3702](https://github.com/ReactiveX/RxJava/pull/3702): Fix `mapNotification` backpressure handling
  - [Pull 3697](https://github.com/ReactiveX/RxJava/pull/3697): Fix `ScalarSynchronousObservable` expecting the `Scheduler.computation()` to be `EventLoopsScheduler` all the time
  - [Pull 3760](https://github.com/ReactiveX/RxJava/pull/3760): Fix ExecutorScheduler and GenericScheduledExecutorService reorder bug
  - [Pull 3678](https://github.com/ReactiveX/RxJava/pull/3678): Fix counted buffer and window backpressure


### Version 1.1.1 - February 11, 2016 ([Maven](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.1.1%7C))

#### The new `Completable` class

  - [Pull 3444](https://github.com/ReactiveX/RxJava/pull/3444) Completable class to support valueless event composition + tests

##### What is this Completable class?

We can think of a `Completable` object as a stripped version of `Observable` where only the terminal events, `onError` and `onCompleted` are ever emitted; they may look like an `Observable.empty()` typified in a concrete class but unlike `empty()`, `Completable` is an active class. `Completable` mandates side effects when subscribed to and it is its main purpose indeed. `Completable` contains some deferred computation with side effects and only notifies about the success or failure of such computation.

Similar to `Single`, the `Completable` behavior can be emulated with `Observable<?>` to some extent, but many API designers think codifying the valuelessness in a separate type is more expressive than messing with wildcards (and usually type-variance problems) in an `Observable` chain.

`Completable` doesn't stream a single value, therefore, it doesn't need backpressure, simplifying the internal structure from one perspective, however, optimizing these internals requires more lock-free atomics knowledge in some respect.


##### Hello World!

Let's see how one can build a (side-effecting) Hello World `Completable`:

```java
Completable.fromAction(() -> System.out.println("Hello World!"))
.subscribe();
```

Quite straightforward. We have a set of `fromXXX` method which can take many sources: `Action`, `Callable`, `Single` and even `Observable` (stripping any values generated by the latter 3 of course). On the receiving end, we have the usual subscribe capabilities: empty, lambdas for the terminal events, a `rx.Subscriber` and a `rx.Completable.CompletableSubscriber`, the main intended receiver for `Completable`s.

##### Further reading

  - [The new Completable API - part 1](http://akarnokd.blogspot.hu/2015/12/the-new-completable-api-part-1.html)
  - [The new Completable API - part 2](http://akarnokd.blogspot.hu/2015/12/the-new-completable-api-part-2-final.html)

#### API enhancements

  - [Pull 3434](https://github.com/ReactiveX/RxJava/pull/3434) Add Single.doAfterTerminate()
  - [Pull 3447](https://github.com/ReactiveX/RxJava/pull/3447) operator DelaySubscription with plain Observable
  - [Pull 3498](https://github.com/ReactiveX/RxJava/pull/3498) Rename cache(int) to cacheWithCapacityHint(int)
  - [Pull 3539](https://github.com/ReactiveX/RxJava/pull/3539) Add Single.zip() for Iterable of Singles
  - [Pull 3562](https://github.com/ReactiveX/RxJava/pull/3562) Add Single.doOnUnsubscribe()
  - [Pull 3566](https://github.com/ReactiveX/RxJava/pull/3566) Deprecate Observable.finallyDo() and add Observable.doAfterTerminate() instead
  - [Pull 3567](https://github.com/ReactiveX/RxJava/pull/3567) Implemented Observable#toCompletable
  - [Pull 3570](https://github.com/ReactiveX/RxJava/pull/3570) Implemented Completable#andThen(Observable)
  - [Pull 3627](https://github.com/ReactiveX/RxJava/pull/3627) Added MergeDelay operators for Iterable of Observables
  - [Pull 3655](https://github.com/ReactiveX/RxJava/pull/3655) Add Single.onErrorResumeNext(Single)
  - [Pull 3661](https://github.com/ReactiveX/RxJava/pull/3661) CombineLatest now supports any number of sources
  - [Pull 3682](https://github.com/ReactiveX/RxJava/pull/3682) fix observeOn resource handling, add delayError capability
  - [Pull 3686](https://github.com/ReactiveX/RxJava/pull/3686) Added retry and retryWhen support for Single
  
#### API deprecations

  - `cache(int)` via #3498, replaced by `cacheWithCapacityHint(int)`
  - `finallyDo(Action0)` via #3566, replaced by `doAfterTerminate(Action0)`
  
### Performance improvements

  - [Pull 3477](https://github.com/ReactiveX/RxJava/pull/3477) add a source OnSubscribe which works from an array directly
  - [Pull 3579](https://github.com/ReactiveX/RxJava/pull/3579) No more need to convert Singles to Observables for Single.zip()
  - [Pull 3587](https://github.com/ReactiveX/RxJava/pull/3587) Remove the need for javac to generate synthetic methods
  - [Pull 3614](https://github.com/ReactiveX/RxJava/pull/3614) just() now supports backpressure
  - [Pull 3642](https://github.com/ReactiveX/RxJava/pull/3642) Optimizate single just
  - [Pull 3589](https://github.com/ReactiveX/RxJava/pull/3589) concat reduce overhead when streaming a source 
  
#### Bugfixes

  - [Pull 3428](https://github.com/ReactiveX/RxJava/pull/3428) GroupBy backpressure fix
  - [Pull 3454](https://github.com/ReactiveX/RxJava/pull/3454) fix: bounded replay() not requesting enough for latecommers
  - [Pull 3467](https://github.com/ReactiveX/RxJava/pull/3467) compensate for drastic clock drifts when scheduling periodic tasks
  - [Pull 3555](https://github.com/ReactiveX/RxJava/pull/3555) fix toMap and toMultimap not handling exceptions of the callbacks
  - [Pull 3585](https://github.com/ReactiveX/RxJava/pull/3585) fix Completable.using not disposing the resource if the factory crashes during the subscription phase
  - [Pull 3588](https://github.com/ReactiveX/RxJava/pull/3588) Fix the initialization order in GenericScheduledExecutorService
  - [Pull 3620](https://github.com/ReactiveX/RxJava/pull/3620) Fix NPE in CompositeException when nested throws on initCause
  - [Pull 3630](https://github.com/ReactiveX/RxJava/pull/3630) ConcatMapEager allow nulls from inner Observables.
  - [Pull 3637](https://github.com/ReactiveX/RxJava/pull/3637) handle predicate exceptions properly in skipWhile
  - [Pull 3638](https://github.com/ReactiveX/RxJava/pull/3638) fix error handling in OperatorDistinctUntilChanged
  - [Pull 3639](https://github.com/ReactiveX/RxJava/pull/3639) fix error handling in onBackpressureBuffer
  - [Pull 3640](https://github.com/ReactiveX/RxJava/pull/3640) fix error handling in onBackpressureDrop
  - [Pull 3644](https://github.com/ReactiveX/RxJava/pull/3644) fix SyncOnSubscribe not signalling onError if the generator crashes
  - [Pull 3645](https://github.com/ReactiveX/RxJava/pull/3645) fix Amb sharing the choice among all subscribers
  - [Pull 3653](https://github.com/ReactiveX/RxJava/pull/3653) fix sample(Observable) not requesting Long.MAX_VALUE
  - [Pull 3658](https://github.com/ReactiveX/RxJava/pull/3658) fix unsubscription and producer issues in sample(other)
  - [Pull 3662](https://github.com/ReactiveX/RxJava/pull/3662) fix doOnRequest premature requesting
  - [Pull 3677](https://github.com/ReactiveX/RxJava/pull/3677) negative argument check for skip's count and merge's maxConcurrent
  - [Pull 3681](https://github.com/ReactiveX/RxJava/pull/3681) change publish(Func1) to use a dedicated subject-like dispatcher
  - [Pull 3688](https://github.com/ReactiveX/RxJava/pull/3688) Fix zip() - observer array becoming visible too early and causing NPE
  - [Pull 3689](https://github.com/ReactiveX/RxJava/pull/3689) unified onErrorX and onExceptionResumeNext and fixed backpressure


### Version 1.1.0 – December 2 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.1.0%7C)) ###

* [Pull 3550] (https://github.com/ReactiveX/RxJava/pull/3550) Public API changes for 1.1.0 release

#### Promotions to Public API

* Subscriptions.unsubscribed
* Subscribers.wrap
* 2 RxJavaErrorHandler methods
* Single + SingleSubscriber
* Exceptions.throwIfAny
* Observable.switchIfEmpty with Observable
* BackpressureDrainManager
* Observable.onBackpressureLatest
* Observable.onBackpressureDrop with action
* Observable.onBackpressureBuffer overloads
* 2 Observable.merge overloads for maxConcurrent
* TestSubscriber methods
* Observable.takeUntil with predicate 

#### Promotions to BETA

* ConnectableObservable.autoConnect
* Stateful Subject methods on ReplaySubject, PublishSubject, BehaviorSubject, and AsyncSubject

#### Experimental APIs Removed

* Observable.onBackpressureBlock
* rx.observables.AbstractOnSubscribe
* Removal of stateful methods from the generic rx.subjects.Subject abstract class

### Version 1.0.17 – December 1 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.17%7C)) ###

* [Pull 3491] (https://github.com/ReactiveX/RxJava/pull/3491) Make scan's delayed Producer independent of event serialization
* [Pull 3150] (https://github.com/ReactiveX/RxJava/pull/3150) Window operators now support backpressure in the inner observable
* [Pull 3535] (https://github.com/ReactiveX/RxJava/pull/3535) Don't swallow fatal errors in OperatorZipIterable
* [Pull 3528] (https://github.com/ReactiveX/RxJava/pull/3528) Avoid to call next when Iterator is drained
* [Pull 3436] (https://github.com/ReactiveX/RxJava/pull/3436) Add action != null check in OperatorFinally
* [Pull 3513] (https://github.com/ReactiveX/RxJava/pull/3513) Add shorter RxJavaPlugin class lookup approach

### Version 1.0.16 – November 11 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.16%7C)) ###

* [Pull 3169] (https://github.com/ReactiveX/RxJava/pull/3169) Merge can now operate in horizontally unbounded mode
* [Pull 3286] (https://github.com/ReactiveX/RxJava/pull/3286) Implements BlockingSingle
* [Pull 3433] (https://github.com/ReactiveX/RxJava/pull/3433) Add Single.defer()
* [Pull 3468] (https://github.com/ReactiveX/RxJava/pull/3468) Fix other places that may swallow OnErrorFailedException
* [Pull 3485] (https://github.com/ReactiveX/RxJava/pull/3485) fix scan() not accepting a null initial value
* [Pull 3488] (https://github.com/ReactiveX/RxJava/pull/3488) Replace all instances of Atomic*FieldUpdater with direct Atomic* instances
* [Pull 3493] (https://github.com/ReactiveX/RxJava/pull/3493) fix for zip(Obs<Obs<T>>) backpressure problem
* [Pull 3510] (https://github.com/ReactiveX/RxJava/pull/3510) eager concatMap to choose safe or unsafe queue based on platform
* [Pull 3512] (https://github.com/ReactiveX/RxJava/pull/3512) fix SafeSubscriber documentation regarding unsubscribe

### Version 1.0.15 – October 9 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.15%7C)) ###

* [Pull 3438] (https://github.com/ReactiveX/RxJava/pull/3438) Better null tolerance in rx.exceptions.*Exception classes
* [Pull 3455] (https://github.com/ReactiveX/RxJava/pull/3455) OnErrorFailedException fix
* [Pull 3448] (https://github.com/ReactiveX/RxJava/pull/3448) Single delay
* [Pull 3429] (https://github.com/ReactiveX/RxJava/pull/3429) Removed the alias BlockingObservable#run
* [Pull 3417] (https://github.com/ReactiveX/RxJava/pull/3417) Add Single.doOnSuccess()
* [Pull 3418] (https://github.com/ReactiveX/RxJava/pull/3418) Add Single.fromCallable()
* [Pull 3419] (https://github.com/ReactiveX/RxJava/pull/3419) Add Single.doOnError()
* [Pull 3423] (https://github.com/ReactiveX/RxJava/pull/3423) Renaming Observable#x to Observable#extend
* [Pull 3174] (https://github.com/ReactiveX/RxJava/pull/3174) Blocking subscribe methods for convenience
* [Pull 3351] (https://github.com/ReactiveX/RxJava/pull/3351) Make BlockingOperatorToIterator exert backpressure.
* [Pull 3357] (https://github.com/ReactiveX/RxJava/pull/3357) Eager ConcatMap
* [Pull 3342] (https://github.com/ReactiveX/RxJava/pull/3342) Remove redundant onStart implementation in OperatorGroupBy
* [Pull 3361] (https://github.com/ReactiveX/RxJava/pull/3361) Safer error handling in BlockingOperatorToFuture
* [Pull 3363] (https://github.com/ReactiveX/RxJava/pull/3363) Remove unused private method from CachedObservable and make "state" final
* [Pull 3408] (https://github.com/ReactiveX/RxJava/pull/3408) DoOnEach: report both original exception and callback exception.
* [Pull 3386] (https://github.com/ReactiveX/RxJava/pull/3386) Changed javadoc for Observable.doOnRequest(Action1)
* [Pull 3149] (https://github.com/ReactiveX/RxJava/pull/3149) Scheduler shutdown capability
* [Pull 3384] (https://github.com/ReactiveX/RxJava/pull/3384) Fix for take() reentrancy bug.
* [Pull 3356] (https://github.com/ReactiveX/RxJava/pull/3356) Fix to a bunch of bugs and issues with AsyncOnSubscribe
* [Pull 3362] (https://github.com/ReactiveX/RxJava/pull/3362) Fix synchronization on non-final field in BufferUntilSubscriber
* [Pull 3365] (https://github.com/ReactiveX/RxJava/pull/3365) Make field final and remove unnecessary unboxing in OnSubscribeRedo.RetryWithPredicate
* [Pull 3370] (https://github.com/ReactiveX/RxJava/pull/3370) Remove unused field updater from SubjectSubscriptionManager
* [Pull 3369] (https://github.com/ReactiveX/RxJava/pull/3369) Lint fixes for unnecessary unboxing
* [Pull 3203] (https://github.com/ReactiveX/RxJava/pull/3203) Implemented the AsyncOnSubscribe
* [Pull 3340] (https://github.com/ReactiveX/RxJava/pull/3340) test/subjects: Use statically imported never() methods
* [Pull 3154] (https://github.com/ReactiveX/RxJava/pull/3154) Add Observable.fromCallable() as a companion for Observable.defer()
* [Pull 3285] (https://github.com/ReactiveX/RxJava/pull/3285) Added latch to async SyncOnSubscrbeTest
* [Pull 3118] (https://github.com/ReactiveX/RxJava/pull/3118) Implementing the SyncOnSubscribe
* [Pull 3183] (https://github.com/ReactiveX/RxJava/pull/3183) Refactored exception reporting of most operators.
* [Pull 3214] (https://github.com/ReactiveX/RxJava/pull/3214) Fix to Notification equals method.
* [Pull 3171] (https://github.com/ReactiveX/RxJava/pull/3171) Scan backpressure and first emission fix
* [Pull 3181] (https://github.com/ReactiveX/RxJava/pull/3181) MapNotification producer NPE fix
* [Pull 3167] (https://github.com/ReactiveX/RxJava/pull/3167) Fixed negative request due to unsubscription of a large requester
* [Pull 3177] (https://github.com/ReactiveX/RxJava/pull/3177) BackpressureUtils capped add/multiply methods + tests
* [Pull 3155] (https://github.com/ReactiveX/RxJava/pull/3155) SafeSubscriber - report onCompleted unsubscribe error to RxJavaPlugin

### Version 1.0.14 – August 12th 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.14%7C)) ###

* [Pull 2963] (https://github.com/ReactiveX/RxJava/pull/2963) Set of standard producers and updated queue implementations
* [Pull 3138] (https://github.com/ReactiveX/RxJava/pull/3138) Range overhead reduction.
* [Pull 3137] (https://github.com/ReactiveX/RxJava/pull/3137) FromIterable overhead reduction.
* [Pull 3078] (https://github.com/ReactiveX/RxJava/pull/3078) switchOnNext() - fix lost requests race condition
* [Pull 3112] (https://github.com/ReactiveX/RxJava/pull/3112) Observers package test coverage and fixes.
* [Pull 3123] (https://github.com/ReactiveX/RxJava/pull/3123) Remove redundant type parameter in EmptyAction 
* [Pull 3104] (https://github.com/ReactiveX/RxJava/pull/3104) Fix SynchronizedQueue.equals
* [Pull 3147] (https://github.com/ReactiveX/RxJava/pull/3147) Remove unnecessary static modifier
* [Pull 3144] (https://github.com/ReactiveX/RxJava/pull/3144) Remove redundant cast in Exceptions
* [Pull 3143] (https://github.com/ReactiveX/RxJava/pull/3143) Fix for BackpressureUtils method javadoc
* [Pull 3141] (https://github.com/ReactiveX/RxJava/pull/3141) Improved Scheduler.Worker memory leak detection
* [Pull 3082] (https://github.com/ReactiveX/RxJava/pull/3082) Observable.x(ConversionFunc) to allow extensions to Observables
* [Pull 3103] (https://github.com/ReactiveX/RxJava/pull/3103) materialize() - add backpressure support
* [Pull 3129] (https://github.com/ReactiveX/RxJava/pull/3129) Fix retry with predicate ignoring backpressure.
* [Pull 3121] (https://github.com/ReactiveX/RxJava/pull/3121) Improve performance of NewThreadWorker, disable search for setRemoveOnCancelPolicy() on Android API < 21
* [Pull 3120] (https://github.com/ReactiveX/RxJava/pull/3120) No InterruptedException with synchronous BlockingObservable
* [Pull 3117] (https://github.com/ReactiveX/RxJava/pull/3117) Operator replay() now supports backpressure
* [Pull 3116] (https://github.com/ReactiveX/RxJava/pull/3116) cache() now supports backpressure
* [Pull 3110] (https://github.com/ReactiveX/RxJava/pull/3110) Test coverage of rx.functions utility methods.
* [Pull 3101] (https://github.com/ReactiveX/RxJava/pull/3101) Fix take swallowing exception if thrown by exactly the nth onNext call to it.
* [Pull 3109] (https://github.com/ReactiveX/RxJava/pull/3109) Unit tests and cleanup of JCTools' queues.
* [Pull 3108] (https://github.com/ReactiveX/RxJava/pull/3108) remove OperatorOnErrorFlatMap because unused
* [Pull 3079] (https://github.com/ReactiveX/RxJava/pull/3079) fix forEach javadoc
* [Pull 3085] (https://github.com/ReactiveX/RxJava/pull/3085) break tests as approach timeout so that don't fail on slow machines
* [Pull 3086] (https://github.com/ReactiveX/RxJava/pull/3086) improve ExecutorSchedulerTest.testOnBackpressureDrop
* [Pull 3093] (https://github.com/ReactiveX/RxJava/pull/3093) Fix request != 0 checking in the scalar paths of merge()

### Version 1.0.13 – July 20th 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.13%7C)) ###

This release has quite a few bug fixes and some new functionality. Items of note are detailed here with the list of changes at the bottom.

##### merge

The `merge` operator went through a major rewrite to fix some edge case bugs in the previous version. This has been sitting for months going through review and performance testing due to the importance and ubiquity of its usage. It is believed this rewrite is now production ready and achieves the goal of being more correct (no known edge cases at this time) while retaining comparable performance and memory usage. 

Special thanks to @akarnokd for this as `merge` is a challenging one to implement. 

##### window fix and behavior change

Unsubscription bugs were fixed in `window`. Along the way it also resulted in a fix to one of the `window` overloads that had a functional discrepancy.

```java
window(Func0<? extends Observable<? extends TClosing>> closingSelector)
```

This is a small behavior change that corrects it. If you use this overload, please review the change to ensure your application is not affected by an assumption of the previously buggy behavior: https://github.com/ReactiveX/RxJava/pull/3039

Note that this behavior change only affects that particular overload while the broader bug fixes affect all `window` overloads. 

##### rx.Single

After [much discussion](https://github.com/ReactiveX/RxJava/issues/1594) it was decided to add a new type to represent an `Observable` that emits a single item. Much bike-shedding led to the name `Single`. This was chosen because `Future`, `Promise` and `Task` are overused and already have nuanced connotations that differ from `rx.Single`, and we didn't want long, obnoxious names with `Observable` as a prefix or suffix. Read the issue thread if you want to dig into the long debates.

If you want to understand the reasoning behind adding this type, you can read about it [in this comment](https://github.com/ReactiveX/RxJava/issues/1594#issuecomment-101300655).

In short, request/response semantics are so common that it was decided worth creating a type that composes well with an `Observable` but only exposes request/response. The difference in behavior and comparability was also deemed worth having an alternative to `Future`. In particular, a `Single` is lazy whereas `Future` is eager. Additionally, merging of `Single`s becomes an `Observable`, whereas combining `Future`s always emits another `Future`. 

Note that the API is added in an `@Experimental` state. We are fairly confident this will stick around, but are holding final judgement until it is used more broadly. We will promote to a stable API in v1.1 or v1.2. 

Examples below demonstrate use of `Single`.

```java
// Hello World
Single<String> hello = Single.just("Hello World!");
hello.subscribe(System.out::println);

// Async request/response
Single<String> one = getData(1);
Single<String> two = getOtherData(2);

// merge request/responses into an Observable of multiple values (not possible with Futures)
Observable<String> merged = one.mergeWith(two);

// zip request/responses into another Single (similar to combining 2 Futures)
Single<String> zipped = one.zipWith(two, (a, b) -> a + b);

// flatMap to a Single
Single<String> flatMapSingle = one.flatMap(v -> {
	return getOtherData(5);
});

// flatMap to an Observable
Observable<Integer> flatMapObservable = one.flatMapObservable(v -> {
	return Observable.just(1, 2, 3);
});

// toObservable
Observable<String> toObservable = one.toObservable();

// toSingle
Single<Integer> toSingle = Observable.just(1).toSingle();

public static Single<String> getData(int id) {
	return Single.<String> create(s -> {
		// do blocking IO
		s.onSuccess("data_" + id);
	}).subscribeOn(Schedulers.io());
}

public static Single<String> getOtherData(int id) {
	return Single.<String> create(s -> {
		// simulate non-blocking IO
		new Thread(() -> {
			try {
				s.onSuccess("other_" + id);
			} catch (Exception e) {
				s.onError(e);
			}
		}).start();
	});
}
```

##### ConnectableObservable.autoConnect

A new feature was added to `ConnectableObservable` similar in behavior to `refCount()`, except that it doesn't disconnect when subscribers are lost. This is useful in triggering an "auto connect" once a certain number of subscribers have subscribed. 

The [JavaDocs](https://github.com/ReactiveX/RxJava/blob/1877fa7bbc176029bcb5af00d8a7715dfbb6d373/src/main/java/rx/observables/ConnectableObservable.java#L96) and [unit tests](https://github.com/ReactiveX/RxJava/blob/1.x/src/test/java/rx/observables/ConnectableObservableTest.java) are good places to understand the feature.

##### Deprecated onBackpressureBlock

The `onBackpressureBlock` operator has been deprecated. It will not ever be removed during the 1.x lifecycle, but it is recommended to not use it. It has proven to be a common source of deadlocks and is difficult to debug. It is instead recommended to use non-blocking approaches to backpressure, rather than callstack blocking. Approaches to backpressure and flow control are [discussed on the wiki](https://github.com/ReactiveX/RxJava/wiki/Backpressure).

#### Changes

* [Pull 3012] (https://github.com/ReactiveX/RxJava/pull/3012) rx.Single
* [Pull 2983] (https://github.com/ReactiveX/RxJava/pull/2983) Fixed multiple calls to onStart.
* [Pull 2970] (https://github.com/ReactiveX/RxJava/pull/2970) Deprecated onBackpressureBlock
* [Pull 2997] (https://github.com/ReactiveX/RxJava/pull/2997) Fix retry() race conditions
* [Pull 3028] (https://github.com/ReactiveX/RxJava/pull/3028) Delay: error cut ahead was not properly serialized
* [Pull 3042] (https://github.com/ReactiveX/RxJava/pull/3042) add backpressure support for defaultIfEmpty()
* [Pull 3049] (https://github.com/ReactiveX/RxJava/pull/3049) single: add toSingle method to Observable
* [Pull 3055] (https://github.com/ReactiveX/RxJava/pull/3055) toSingle() should use unsafeSubscribe
* [Pull 3023] (https://github.com/ReactiveX/RxJava/pull/3023) ConnectableObservable autoConnect operator
* [Pull 2928] (https://github.com/ReactiveX/RxJava/pull/2928) Merge and MergeMaxConcurrent unified and rewritten
* [Pull 3039] (https://github.com/ReactiveX/RxJava/pull/3039) Window with Observable: fixed unsubscription and behavior
* [Pull 3045] (https://github.com/ReactiveX/RxJava/pull/3045) ElementAt request management enhanced
* [Pull 3048] (https://github.com/ReactiveX/RxJava/pull/3048) CompositeException extra NPE protection
* [Pull 3052] (https://github.com/ReactiveX/RxJava/pull/3052) Reduce test failure likelihood of testMultiThreadedWithNPEinMiddle
* [Pull 3031] (https://github.com/ReactiveX/RxJava/pull/3031) Fix OperatorFlatMapPerf.flatMapIntPassthruAsync Perf Test
* [Pull 2975] (https://github.com/ReactiveX/RxJava/pull/2975) Deprecate and rename two timer overloads to interval
* [Pull 2982] (https://github.com/ReactiveX/RxJava/pull/2982) TestSubscriber - add factory methods
* [Pull 2995] (https://github.com/ReactiveX/RxJava/pull/2995) switchOnNext - ensure initial requests additive and fix request overflow
* [Pull 2972] (https://github.com/ReactiveX/RxJava/pull/2972) Fixed window(time) to work properly with unsubscription, added
* [Pull 2990] (https://github.com/ReactiveX/RxJava/pull/2990) Improve Subscriber readability
* [Pull 3018] (https://github.com/ReactiveX/RxJava/pull/3018) TestSubscriber - fix awaitTerminalEventAndUnsubscribeOnTimeout
* [Pull 3034] (https://github.com/ReactiveX/RxJava/pull/3034) Instantiate EMPTY lazily
* [Pull 3033] (https://github.com/ReactiveX/RxJava/pull/3033) takeLast() javadoc fixes, standardize parameter names (count instead of num)
* [Pull 3043] (https://github.com/ReactiveX/RxJava/pull/3043) TestSubscriber javadoc cleanup
* [Pull 3065] (https://github.com/ReactiveX/RxJava/pull/3065) add Subscribers.wrap
* [Pull 3091] (https://github.com/ReactiveX/RxJava/pull/3091) Fix autoConnect calling onStart twice.
* [Pull 3092] (https://github.com/ReactiveX/RxJava/pull/3092) Single.toObservable


### Version 1.0.12 – June 9th 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.12%7C)) ###

* [Pull 2963] (https://github.com/ReactiveX/RxJava/pull/2963) Set of standard producers and updated queue implementations
* [Pull 2961] (https://github.com/ReactiveX/RxJava/pull/2961) fix Amb backpressure bug
* [Pull 2960] (https://github.com/ReactiveX/RxJava/pull/2960) fix OperatorConcat race condition where request lost
* [Pull 2985] (https://github.com/ReactiveX/RxJava/pull/2985) improve OperatorSerializeTest.testMultiThreadedWithNPEinMiddle
* [Pull 2986] (https://github.com/ReactiveX/RxJava/pull/2986) OperatorAll - implement backpressure and include last value in exception cause 
* [Pull 2987] (https://github.com/ReactiveX/RxJava/pull/2987) fix skip() race condition and request overflow
* [Pull 2988] (https://github.com/ReactiveX/RxJava/pull/2988) Operator exists() - implement backpressure & include last value in exception cause
* [Pull 2989] (https://github.com/ReactiveX/RxJava/pull/2989) prevent take() from requesting more than needed
* [Pull 2991] (https://github.com/ReactiveX/RxJava/pull/2991) takeUntil(predicate) - include last value in error cause
* [Pull 2992] (https://github.com/ReactiveX/RxJava/pull/2992) Fix value rendering in error last cause for primitive types
* [Pull 2993] (https://github.com/ReactiveX/RxJava/pull/2993) takeWhile(predicate) - include last value in error cause
* [Pull 2996] (https://github.com/ReactiveX/RxJava/pull/2996) switchIfEmpty - fix backpressure bug and lost requests
* [Pull 2999] (https://github.com/ReactiveX/RxJava/pull/2999) Fix a wrong assertion in assertError
* [Pull 3000] (https://github.com/ReactiveX/RxJava/pull/3000) Replace the Java 7 AssertionError(message, cause) with initCause
* [Pull 3001] (https://github.com/ReactiveX/RxJava/pull/3001) use Subscribers.from()
* [Pull 3009] (https://github.com/ReactiveX/RxJava/pull/3009) Observable.from(iterable) - ensure it.hasNext() is not called unnecessarily

### Version 1.0.11 – May 19th 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.11%7C)) ###

* [Pull 2948] (https://github.com/ReactiveX/RxJava/pull/2948) More assertions for TestSubscriber
* [Pull 2956] (https://github.com/ReactiveX/RxJava/pull/2956) OperatorObserveOn should not request more after child is unsubscribed
* [Pull 2951] (https://github.com/ReactiveX/RxJava/pull/2951) OperatorConcat - prevent request overflow and fix race condition
* [Pull 2950] (https://github.com/ReactiveX/RxJava/pull/2950) OperatorGroupBy - check for request overflow and don't decrement when at Long.MAX_VALUE
* [Pull 2923] (https://github.com/ReactiveX/RxJava/pull/2923) OnBackpressureLatest: Non-blocking version of the toBlocking().latest() operator.
* [Pull 2907] (https://github.com/ReactiveX/RxJava/pull/2907) Fixed schedule race and task retention with ExecutorScheduler.
* [Pull 2929] (https://github.com/ReactiveX/RxJava/pull/2929) OperatorObserveOn onComplete can be emitted despite onError being called
* [Pull 2940] (https://github.com/ReactiveX/RxJava/pull/2940) Remove unnecessary localHasValue check
* [Pull 2939] (https://github.com/ReactiveX/RxJava/pull/2939) publish: Fix another race between terminalEvent and the queue being empty.
* [Pull 2938] (https://github.com/ReactiveX/RxJava/pull/2938) Fixed Observable.combineLatest overflow bug on Android
* [Pull 2936] (https://github.com/ReactiveX/RxJava/pull/2936) Fix TestSubject bug
* [Pull 2934] (https://github.com/ReactiveX/RxJava/pull/2934) Fix termination race condition in OperatorPublish.dispatch
* [Pull 2963] (https://github.com/ReactiveX/RxJava/pull/2963) Set of standard producers and updated queue implementations with some

### Version 1.0.10 – April 30th 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.10%7C)) ###

* [Pull 2892] (https://github.com/ReactiveX/RxJava/pull/2892) Fix Observable.range race condition 
* [Pull 2895] (https://github.com/ReactiveX/RxJava/pull/2895) Fix Observable.from(Iterable) race condition
* [Pull 2898] (https://github.com/ReactiveX/RxJava/pull/2898) Observable.range - add unit test for eager completion on empty
* [Pull 2899] (https://github.com/ReactiveX/RxJava/pull/2899) Observable.from(empty) to emit onComplete even when 0 requested
* [Pull 2894] (https://github.com/ReactiveX/RxJava/pull/2894) Concat: fixed reentrancy problem in completeInner
* [Pull 2880] (https://github.com/ReactiveX/RxJava/pull/2880) Use singleton reduction functions in count and countLong
* [Pull 2902] (https://github.com/ReactiveX/RxJava/pull/2902) Prevent ExceptionsTest from hanging when testing stack overflow
* [Pull 2897] (https://github.com/ReactiveX/RxJava/pull/2897) Fix for overlapping windows.
* [Pull 2904] (https://github.com/ReactiveX/RxJava/pull/2904) TakeLast - add request overflow check
* [Pull 2905] (https://github.com/ReactiveX/RxJava/pull/2905) Use singleton Operators where we can
* [Pull 2909] (https://github.com/ReactiveX/RxJava/pull/2909) Fix the drainer to check if the queue is empty before quitting.
* [Pull 2911] (https://github.com/ReactiveX/RxJava/pull/2911) OperatorPublish benchmark
* [Pull 2914] (https://github.com/ReactiveX/RxJava/pull/2914) Optimization - use OperatorTakeLastOne for takeLast(1)
* [Pull 2915] (https://github.com/ReactiveX/RxJava/pull/2915) Observable.ignoreElements - optimize
* [Pull 2883] (https://github.com/ReactiveX/RxJava/pull/2883) Proposal: standardized Subject state-peeking methods.
* [Pull 2901] (https://github.com/ReactiveX/RxJava/pull/2901) Operators toList and toSortedList now support backpressure
* [Pull 2921] (https://github.com/ReactiveX/RxJava/pull/2921) OperatorObserveOn - handle request overflow correctly
* [Pull 2882] (https://github.com/ReactiveX/RxJava/pull/2882) OperatorScan - don't call onNext after onError is called
* [Pull 2875] (https://github.com/ReactiveX/RxJava/pull/2875) Fix: NPE in requestFromChild method.
* [Pull 2814] (https://github.com/ReactiveX/RxJava/pull/2814) Operator Publish full rewrite
* [Pull 2820] (https://github.com/ReactiveX/RxJava/pull/2820) Backpressure for window(size)
* [Pull 2912] (https://github.com/ReactiveX/RxJava/pull/2912) Fix the Scheduler performance degradation

### Version 1.0.9 – April 9th 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.9%7C)) ###

* [Pull 2845] (https://github.com/ReactiveX/RxJava/pull/2845) Fix for repeat: wrong target of request 
* [Pull 2851] (https://github.com/ReactiveX/RxJava/pull/2851) Add 'request(Long.MAX_VALUE)' in 'onStart' to fix the backpressure issue of debounce
* [Pull 2852] (https://github.com/ReactiveX/RxJava/pull/2852) Change retryWhen to eagerly ignore an error'd source's subsequent events
* [Pull 2859] (https://github.com/ReactiveX/RxJava/pull/2859) OperatorDoOnRequest should unsubscribe from upstream
* [Pull 2854] (https://github.com/ReactiveX/RxJava/pull/2854) Fixes wrong request accounting in AbstractOnSubscribe
* [Pull 2860] (https://github.com/ReactiveX/RxJava/pull/2860) OperatorSingle should request exactly what it needs
* [Pull 2817] (https://github.com/ReactiveX/RxJava/pull/2817) Fix for non-deterministic: testOnBackpressureDropWithAction
* [Pull 2818] (https://github.com/ReactiveX/RxJava/pull/2818) Small fix for the getValue javadoc in AsyncSubject and BehaviorSubject
* [Pull 2823] (https://github.com/ReactiveX/RxJava/pull/2823) Enable maven central sync via bintray
* [Pull 2807] (https://github.com/ReactiveX/RxJava/pull/2807) Corrected all Java interfaces declarations
* [Pull 2632] (https://github.com/ReactiveX/RxJava/pull/2632) Implement hook to render specific types in OnNextValue
* [Pull 2837] (https://github.com/ReactiveX/RxJava/pull/2837) Fixed a non-deterministic test and a few scheduler leaks (in tests)
* [Pull 2825] (https://github.com/ReactiveX/RxJava/pull/2825) Fixed javadoc for Observable.repeat() method
* [Pull 2838] (https://github.com/ReactiveX/RxJava/pull/2838) Fix typo in OnSubscribe interface's Javadoc
* [Pull 2864] (https://github.com/ReactiveX/RxJava/pull/2864) IndexedRingBuffer.getInstance can infer type
* [Pull 2862] (https://github.com/ReactiveX/RxJava/pull/2862) Cleanup warnings in test source
* [Pull 2868] (https://github.com/ReactiveX/RxJava/pull/2868) Fixed reentrancy issue with the error producer. 
* [Pull 2866] (https://github.com/ReactiveX/RxJava/pull/2866) Use simpler naming in Action1, Func1 to assist IDEs

### Version 1.0.8 – March 7th 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.8%7C)) ###

* [Pull 2809] (https://github.com/ReactiveX/RxJava/pull/2809) Fixed takeUntil not unsubscribing from either of the observables in case of a terminal condition.
* [Pull 2804] (https://github.com/ReactiveX/RxJava/pull/2804) ObserveOn throughput enhancements
* [Pull 2767] (https://github.com/ReactiveX/RxJava/pull/2767) Optimized scalar observeOn/subscribeOn
* [Pull 2776] (https://github.com/ReactiveX/RxJava/pull/2776) Experimental: add new operator onBackpressureDrop(Action1 onDrop)
* [Pull 2788] (https://github.com/ReactiveX/RxJava/pull/2788) Fix the bug that 'publish' will cache items when no subscriber
* [Pull 2779] (https://github.com/ReactiveX/RxJava/pull/2779) OperatorMulticast.connect(connection) should not return null
* [Pull 2771] (https://github.com/ReactiveX/RxJava/pull/2771) OnSubscribeRange request overflow check
* [Pull 2770] (https://github.com/ReactiveX/RxJava/pull/2770) OperatorOnBackpressureDrop request overflow check
* [Pull 2769] (https://github.com/ReactiveX/RxJava/pull/2769) OperatorCombineLatest request overflow check

### Version 1.0.7 – February 21st 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.7%7C)) ###

This release includes some bug fixes along with a new operator and performance enhancements.

#### Experimental Operator

Note that these APIs [may still change or be removed altogether](https://github.com/ReactiveX/RxJava#experimental) since they are marked as `@Experimental`. 

##### withLatestFrom(Observable, Selector)

This allows combining all values from one `Observable` with the latest value from a second `Observable` at each `onNext`.

For example:

```java
Observable<Long> a = Observable.interval(1, TimeUnit.SECONDS);
Observable<Long> b = Observable.interval(250, TimeUnit.MILLISECONDS);


a.withLatestFrom(b, (x, y) -> new long[] { x, y })
        .toBlocking()
        .forEach(pair -> System.out.println("a: " + pair[0] + " b: " + pair[1]));
```

This outputs:

```
a: 0 b: 3
a: 1 b: 6
a: 2 b: 11
a: 3 b: 15
a: 4 b: 19
a: 5 b: 23
a: 6 b: 27
```


#### Changes

https://github.com/ReactiveX/RxJava/pull/2760 Operator: WithLatestFrom
https://github.com/ReactiveX/RxJava/pull/2762 Optimized isUnsubscribed check
https://github.com/ReactiveX/RxJava/pull/2759 Observable.using should use unsafeSubscribe and enable eager disposal
https://github.com/ReactiveX/RxJava/pull/2655 SwitchOnNext: fix upstream producer replacing the ops own producer

### Version 1.0.6 – February 11th 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.6%7C)) ###

This release adds an experimental operator and fixes several bugs. 

##### flatMap(maxConcurrent)

> Note that this API [may still change or be removed altogether](https://github.com/ReactiveX/RxJava#beta) since it is marked as `@Beta`. 

A `flatMap` overload was added that allows limiting concurrency, or the number of `Observable`s being merged .

This now means that these are the same, one using `merge` directly, the other using `flatMap` and passing in `10` as the `maxConcurrent`:

```java
Observable<Observable<Integer>> asyncWork = range(1, 1000000)
        .doOnNext(i -> System.out.println("Emitted Value: " + i))
        .map(item -> {
            return just(item)
                    .doOnNext(MergeMaxConcurrent::sleep)
                    .subscribeOn(Schedulers.io());
        });
merge(asyncWork, 10).toBlocking().forEach(v -> System.out.println("Received: " + v));
```

```java
range(1, 1000000)
        .doOnNext(i -> System.out.println("Emitted Value: " + i))
        .flatMap(item -> {
            return just(item)
                    .doOnNext(MergeMaxConcurrent::sleep)
                    .subscribeOn(Schedulers.io());
        }, 10)
        .toBlocking().forEach(v -> System.out.println("Received: " + v));
```

#### Changes

* [Pull 2627] (https://github.com/ReactiveX/RxJava/pull/2627) FlatMap overloads with maximum concurrency parameter
* [Pull 2648] (https://github.com/ReactiveX/RxJava/pull/2648) TakeWhile: don't unsubscribe downstream.
* [Pull 2580] (https://github.com/ReactiveX/RxJava/pull/2580) Allow configuring the maximum number of computation scheduler threads
* [Pull 2601] (https://github.com/ReactiveX/RxJava/pull/2601) Added common Exceptions.throwIfAny to throw a collection of exceptions 
* [Pull 2644] (https://github.com/ReactiveX/RxJava/pull/2644) Missing Unsafe class yields NoClassDefFoundError 
* [Pull 2642] (https://github.com/ReactiveX/RxJava/pull/2642) Fix a potential memory leak in schedulePeriodically 
* [Pull 2630] (https://github.com/ReactiveX/RxJava/pull/2630) Cast back Observer to Subscriber if passed to subscribe(Observer)
* [Pull 2622] (https://github.com/ReactiveX/RxJava/pull/2622) Changed Observable.empty() into a stateless constant observable.
* [Pull 2607] (https://github.com/ReactiveX/RxJava/pull/2607) OnSubscribeRefCount - improve comments


### Version 1.0.5 – February 3rd 2015 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.5%7C)) ###

This release includes many bug fixes along with a few new operators and enhancements.

#### Experimental Operators

This release adds a few experimental operators. 

Note that these APIs [may still change or be removed altogether](https://github.com/ReactiveX/RxJava#experimental) since they are marked as `@Experimental`. 

##### takeUntil(predicate)

This operator allows conditionally unsubscribing an `Observable` but inclusively emitting the final `onNext`. This differs from `takeWhile` which excludes the final `onNext`.

```java
// takeUntil(predicate) example
Observable.just(1, 2, 3, 4, 5, 6, 7)
        .doOnEach(System.out::println)
        .takeUntil(i -> i == 3)
        .forEach(System.out::println);

// takeWhile(predicate) example
Observable.just(1, 2, 3, 4, 5, 6, 7)
        .doOnEach(System.out::println)
        .takeWhile(i -> i <= 3)
        .forEach(System.out::println);
```

This outputs:

```
// takeUntil(predicate)
[rx.Notification@30e84925 OnNext 1]
1
[rx.Notification@30e84926 OnNext 2]
2
[rx.Notification@30e84927 OnNext 3]
3

// takeWhile(predicate)
[rx.Notification@30e84925 OnNext 1]
1
[rx.Notification@30e84926 OnNext 2]
2
[rx.Notification@30e84927 OnNext 3]
3
[rx.Notification@30e84928 OnNext 4]
```

Note how `takeWhile` produces 4 values and `takeUntil` produces 3.

##### switchIfEmpty

The new `switchIfEmpty` operator is a companion to `defaultIfEmpty` that switches to a different `Observable` if the primary `Observable` is empty.

```java
Observable.empty()
        .switchIfEmpty(Observable.just(1, 2, 3))
        .forEach(System.out::println);
```

#### Enhancements

##### merge(maxConcurrent) with backpressure

This release adds backpressure to `merge(maxConcurrent)` so that horizontal buffer bloat can also be controll with the `maxConcurrent` parameter.

This allows parallel execution such as the following to work with backpressure:

```java
public class MergeMaxConcurrent {

    public static void main(String[] args) {
        // define 1,000,000 async tasks
        Observable<Observable<Integer>> asyncWork = range(1, 1000000)
                      .doOnNext(i -> System.out.println("Value: " + i))
                .doOnRequest(r -> System.out.println("request1 " + r))
                .map(item -> {
                    return just(item)
                            // simulate slow IO or computation
                            .doOnNext(MergeMaxConcurrent::sleep)
                            .subscribeOn(Schedulers.io());
                })
                .doOnRequest(r -> System.out.println("request2 " + r));
                
        // allow 10 outstanding tasks at a time
        merge(asyncWork, 10).toBlocking().forEach(System.out::println);
    }

    public static void sleep(int value) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

In prior versions all 1,000,000 tasks are immediately emitted and queued for execution. As of this release it correctly allows 10 at a time to be emitted. 

#### Changes


* [Pull 2493] (https://github.com/ReactiveX/RxJava/pull/2493) Experimental Operator TakeUntil with predicate
* [Pull 2585] (https://github.com/ReactiveX/RxJava/pull/2585) Experimental Operator: switchIfEmpty
* [Pull 2470] (https://github.com/ReactiveX/RxJava/pull/2470) Experimental Subject state information methods & bounded ReplaySubject termination
* [Pull 2540] (https://github.com/ReactiveX/RxJava/pull/2540) Merge with max concurrency now supports backpressure.
* [Pull 2332] (https://github.com/ReactiveX/RxJava/pull/2332) Operator retry test fix attempt
* [Pull 2244] (https://github.com/ReactiveX/RxJava/pull/2244) OperatorTakeLast add check for isUnsubscribed to fast path
* [Pull 2469] (https://github.com/ReactiveX/RxJava/pull/2469) Remove the execute permission from source files
* [Pull 2455] (https://github.com/ReactiveX/RxJava/pull/2455) Fix for #2191 - OperatorMulticast fails to unsubscribe from source
* [Pull 2474] (https://github.com/ReactiveX/RxJava/pull/2474) MergeTest.testConcurrency timeout to let other tests run
* [Pull 2335] (https://github.com/ReactiveX/RxJava/pull/2335) A set of stateless operators that don't need to be instantiated
* [Pull 2447] (https://github.com/ReactiveX/RxJava/pull/2447) Fail early if a null subscription is added to a CompositeSubscription.
* [Pull 2475] (https://github.com/ReactiveX/RxJava/pull/2475) SynchronousQueue.clone fix
* [Pull 2477] (https://github.com/ReactiveX/RxJava/pull/2477) Backpressure tests fix0121
* [Pull 2476] (https://github.com/ReactiveX/RxJava/pull/2476) Fixed off-by-one error and value-drop in the window operator.
* [Pull 2478] (https://github.com/ReactiveX/RxJava/pull/2478) RefCountAsync: adjusted time values as 1 ms is unreliable
* [Pull 2238] (https://github.com/ReactiveX/RxJava/pull/2238) Fix the bug that cache doesn't unsubscribe the source Observable when the source is terminated
* [Pull 1840] (https://github.com/ReactiveX/RxJava/pull/1840) Unsubscribe when thread is interrupted
* [Pull 2471] (https://github.com/ReactiveX/RxJava/pull/2471) Fixes NPEs reported in ReactiveX#1702 by synchronizing queue. 
* [Pull 2482] (https://github.com/ReactiveX/RxJava/pull/2482) Merge: fixed hangs & missed scalar emissions
* [Pull 2547] (https://github.com/ReactiveX/RxJava/pull/2547) Warnings cleanup
* [Pull 2465] (https://github.com/ReactiveX/RxJava/pull/2465) ScheduledExecutorService: call purge periodically on JDK 6 to avoid cancelled task-retention
* [Pull 2591] (https://github.com/ReactiveX/RxJava/pull/2591) Changed the naming of the NewThreadWorker's system parameters
* [Pull 2543] (https://github.com/ReactiveX/RxJava/pull/2543) OperatorMerge handle request overflow
* [Pull 2548] (https://github.com/ReactiveX/RxJava/pull/2548) Subscriber.request should throw exception if negative request made
* [Pull 2550] (https://github.com/ReactiveX/RxJava/pull/2550) Subscriber.onStart requests should be additive (and check for overflow)
* [Pull 2553] (https://github.com/ReactiveX/RxJava/pull/2553) RxRingBuffer with synchronization
* [Pull 2565] (https://github.com/ReactiveX/RxJava/pull/2565) Obstruction detection in tests.
* [Pull 2563] (https://github.com/ReactiveX/RxJava/pull/2563) Retry backpressure test: split error conditions into separate test lines. 
* [Pull 2572] (https://github.com/ReactiveX/RxJava/pull/2572) Give more time to certain concurrency tests.
* [Pull 2559] (https://github.com/ReactiveX/RxJava/pull/2559) OnSubscribeFromIterable - add request overflow check
* [Pull 2574] (https://github.com/ReactiveX/RxJava/pull/2574) SizeEviction test needs to return false
* [Pull 2561] (https://github.com/ReactiveX/RxJava/pull/2561) Updating queue code from JCTools
* [Pull 2566] (https://github.com/ReactiveX/RxJava/pull/2566) CombineLatest: fixed concurrent requestUpTo yielding -1 requests
* [Pull 2552] (https://github.com/ReactiveX/RxJava/pull/2552) Publish: fixed incorrect subscriber requested accounting
* [Pull 2583] (https://github.com/ReactiveX/RxJava/pull/2583) Added perf tests for various container-like subscriptions
* [Pull 1955] (https://github.com/ReactiveX/RxJava/pull/1955) OnBackpressureXXX: support for common drain manager & fix for former concurrency bugs
* [Pull 2590] (https://github.com/ReactiveX/RxJava/pull/2590) Zip: fixed unbounded downstream requesting above Long.MAX_VALUE
* [Pull 2589] (https://github.com/ReactiveX/RxJava/pull/2589) Repeat/retry: fixed unbounded downstream requesting above Long.MAX_VALUE
* [Pull 2567] (https://github.com/ReactiveX/RxJava/pull/2567) RefCount: disconnect all if upstream terminates
* [Pull 2593] (https://github.com/ReactiveX/RxJava/pull/2593) Zip: emit onCompleted without waiting for request + avoid re-reading fields


### Version 1.0.4 – December 29th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.4%7C)) ###

* [Pull 2156] (https://github.com/ReactiveX/RxJava/pull/2156) Fix the issue that map may swallow fatal exceptions
* [Pull 1967] (https://github.com/ReactiveX/RxJava/pull/1967) Fix the issue that GroupBy may not call 'unsubscribe'
* [Pull 2052] (https://github.com/ReactiveX/RxJava/pull/2052) OperatorDoOnRequest.ParentSubscriber should be static class
* [Pull 2237] (https://github.com/ReactiveX/RxJava/pull/2237) Make Publish Operator Release RingBuffer
* [Pull 2053] (https://github.com/ReactiveX/RxJava/pull/2053) Fixed wrong bounded ReplaySubject use in test

### Version 1.0.3 – December 15th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.3%7C)) ###

* [Pull 1928] (https://github.com/ReactiveX/RxJava/pull/1928) Experimental: Add onBackpressureBuffer with capacity
* [Pull 1946] (https://github.com/ReactiveX/RxJava/pull/1946) Experimental: AbstractOnSubscribe to help build Observables one onNext at a time. 
* [Pull 1960] (https://github.com/ReactiveX/RxJava/pull/1960) Beta: doOnRequest
* [Pull 1965] (https://github.com/ReactiveX/RxJava/pull/1965) Fix the issue that Sample doesn't call 'unsubscribe' 
* [Pull 1966] (https://github.com/ReactiveX/RxJava/pull/1966) Fix NPE when the key is null in GroupBy
* [Pull 1964] (https://github.com/ReactiveX/RxJava/pull/1964) Handle 0 or negative request in Buffer
* [Pull 1957] (https://github.com/ReactiveX/RxJava/pull/1957) Fix 'request(0)' issue in Scan
* [Pull 1950] (https://github.com/ReactiveX/RxJava/pull/1950) Add "Subscriptions.unsubscribed" to fix the 'isUnsubscribed' issue
* [Pull 1938] (https://github.com/ReactiveX/RxJava/pull/1938) Any/All should not unsubscribe downstream.
* [Pull 1968] (https://github.com/ReactiveX/RxJava/pull/1968) Upgrade to Gradle 2.2
* [Pull 1961] (https://github.com/ReactiveX/RxJava/pull/1961) Remove Request Batching in Merge

* [Pull 1953] (https://github.com/ReactiveX/RxJava/pull/1953) Fixed timer cast-to-int crash causing incorrect benchmark.
* [Pull 1952] (https://github.com/ReactiveX/RxJava/pull/1952) Remove ActionSubscription
* [Pull 1951] (https://github.com/ReactiveX/RxJava/pull/1951) Remove extraneous request(n) and onCompleted() calls when unsubscribed.
* [Pull 1947] (https://github.com/ReactiveX/RxJava/pull/1947) Fixed first emission racing with pre and post subscription.
* [Pull 1937] (https://github.com/ReactiveX/RxJava/pull/1937) Scheduler.Worker to be finally unsubscribed to avoid interference
* [Pull 1926] (https://github.com/ReactiveX/RxJava/pull/1926) Move the codes out of the finally block
* [Pull 1922] (https://github.com/ReactiveX/RxJava/pull/1922) Set removeOnCancelPolicy on the threadpool if supported

### Version 1.0.2 – December 1st 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.2%7C)) ###

This release adds `@Beta` and `@Experimental` annotations to mark APIs that are not yet stable. 

An example of how this looks in the [Javadocs](http://reactivex.io/RxJava/javadoc/rx/Observable.html#onBackpressureBlock()) is:

```
@Experimental
public final Observable<T> onBackpressureBlock()
```

The lifecycle and stability of these are [documented in the README](https://github.com/ReactiveX/RxJava/blob/1.x/README.md#versioning) as follows:

> #### @Beta

> APIs marked with the `@Beta` annotation at the class or method level are subject to change. They can be modified in any way, or even removed, at any time. If your code is a library itself (i.e. it is used on the CLASSPATH of users outside your own control), you should not use beta APIs, unless you repackage them (e.g. using ProGuard, shading, etc).

> #### @Experimental

> APIs marked with the `@Experimental` annotation at the class or method level will almost certainly change. They can be modified in any way, or even removed, at any time. You should not use or rely on them in any production code. They are purely to allow broad testing and feedback. 

* [Pull 1905] (https://github.com/ReactiveX/RxJava/pull/1905) Beta & Experimental Annotations
* [Pull 1907] (https://github.com/ReactiveX/RxJava/pull/1907) Experimental: onBackpressureBlock
* [Pull 1903] (https://github.com/ReactiveX/RxJava/pull/1903) Fix TestScheduler Handling of Immediate vs Virtual Time
* [Pull 1898] (https://github.com/ReactiveX/RxJava/pull/1898) Scheduled action no interrupt
* [Pull 1904] (https://github.com/ReactiveX/RxJava/pull/1904) Fix the bug that Scan may request 0 when n is 1
* [Pull 1912] (https://github.com/ReactiveX/RxJava/pull/1912) Fixed retry without backpressure & test function to support bp

### Version 1.0.1 – November 28th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.1%7C)) ###

* [Pull 1893] (https://github.com/ReactiveX/RxJava/pull/1893) Fixed incorrect error merging.
* [Pull 1901] (https://github.com/ReactiveX/RxJava/pull/1901) Fixed redo & groupBy backpressure management

### Version 1.0.0 – November 18th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0%7C)) ###

After 2+ years of internal and open source development, 3600+ commits, 100+ releases, and with the help of 97 contributors RxJava has hit version 1.0.0.

Thank you @headinthebox @zsxwing @samuelgruetter @akarnokd @quidryan @DavidMGross @abersnaze @jmhofer @mairbek @mttkay @daveray @mattrjacobs @michaeldejong @MarioAriasC @johngmyers @pron @jbripley @davidmoten @gliptak @johnhmarks @jloisel @billyy @prabirshrestha @ragalie @abliss @dpsm @daschl @thegeez and the [many other contributors](https://github.com/ReactiveX/RxJava/graphs/contributors) and those who have reported bugs, tweeted, blogged or presented about RxJava.

The quality of this release could not have been achieved without all of your help. Thank you for your involvement and for building a community around this project. 


### JVM Language Adaptors & Subprojects

As of 1.0 the JVM language adapters and other subprojects no longer live under RxJava but have been separated out into their own projects with their own release cycles:

- [RxAndroid](https://github.com/ReactiveX/RxAndroid)
- [RxScala](https://github.com/ReactiveX/RxScala)
- [RxGroovy](https://github.com/ReactiveX/RxGroovy)
- [RxClojure](https://github.com/ReactiveX/RxClojure)
- [RxKotlin](https://github.com/ReactiveX/RxKotlin)
- [RxJRuby](https://github.com/ReactiveX/RxJRuby)
- [RxJavaReactiveStreams](https://github.com/ReactiveX/RxJavaReactiveStreams)
- [RxJavaString](https://github.com/ReactiveX/RxJavaString)
- [RxJavaGuava](https://github.com/ReactiveX/RxJavaGuava)
- [RxRoboVM](https://github.com/ReactiveX/RxRoboVM)
- [RxSwing](https://github.com/ReactiveX/RxSwing)
- [RxQuasar](https://github.com/ReactiveX/RxQuasar)
- [RxJavaJoins](https://github.com/ReactiveX/RxJavaJoins)
- [RxJavaComputationExpressions](https://github.com/ReactiveX/RxJavaComputationExpressions)
- [RxJavaAsyncUtil](https://github.com/ReactiveX/RxJavaAsyncUtil)
- [RxJavaDebug](https://github.com/ReactiveX/RxJavaDebug)
- [RxJavaMath](https://github.com/ReactiveX/RxJavaMath)
- [RxJavaFX](https://github.com/ReactiveX/RxJavaFX)
- [RxApacheHttp](https://github.com/ReactiveX/RxApacheHttp)


### Versioning

Version 1.x is now a stable API and will be supported for several years. 

Minor 1.x increments (such as 1.1, 1.2, etc) will occur when non-trivial new functionality is added or significant enhancements or bug fixes occur that may have behavioral changes that may affect some edge cases (such as dependence on behavior resulting from a bug). An example of an enhancement that would classify as this is adding reactive pull backpressure support to an operator that previously did not support it. This should be backwards compatible but does behave differently.

Patch 1.x.y increments (such as 1.0.0 -> 1.0.1, 1.3.1 -> 1.3.2, etc) will occur for bug fixes and trivial functionality (like adding a method overload).

### Roadmap and Known Issues

- [1.0.x milestone](https://github.com/ReactiveX/RxJava/issues?q=is%3Aopen+is%3Aissue+milestone%3A1.0.x) with known issues
- [1.1 milestone](https://github.com/ReactiveX/RxJava/issues?q=is%3Aopen+is%3Aissue+milestone%3A1.1) with additional support for reactive pull backpressure
- [1.x](https://github.com/ReactiveX/RxJava/issues?q=is%3Aopen+is%3Aissue+milestone%3A1.x) is a catch all for other items that may be pursued in 1.2, 1.3 and later versions.



### Change Log

- all deprecated methods and types from v0.20 and earlier are deleted
- now published to groupId `io.reactivex` instead of `com.netflix.rxjava`
- artifactId is now `rxjava` instead of `rxjava-core`

```
io.reactivex:rxjava:1.0.0
```

Following are specific changes from 0.20 to 1.0 to be aware of:

#### groupBy/groupByUntil

The `groupByUntil` operator was removed by collapsing its behavior into `groupBy`. Previously on `groupBy` when a child `GroupedObservable` was unsubscribed it would internally retain the state and ignore all future `onNext` for that key.

This matched behavior in Rx.Net but was found to be non-obvious and almost everyone using `groupBy` on long-lived streams actually wanted the behavior of `groupByUntil` where an unsubscribed `GroupedObservable` would clean up the resources and then if `onNext` for that key arrived again a new `GroupedObservable` would be emitted.

Adding backpressure (reactive pull) to `groupByUntil` was found to not work easily with its signatures so before 1.0 Final it was decided to collapse `groupBy` and `groupByUntil`. Further details on this can be found in [Pull Request 1727](https://github.com/ReactiveX/RxJava/pull/1727).

Here is an example of how `groupBy` now behaves when a child `GroupedObservable` is unsubscribed (using `take` here):

```java
// odd/even into lists of 10
Observable.range(1, 100)
        .groupBy(n -> n % 2 == 0)
        .flatMap(g -> {
            return g.take(10).toList();
        }).forEach(System.out::println);
```

```
[1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
[21, 23, 25, 27, 29, 31, 33, 35, 37, 39]
[22, 24, 26, 28, 30, 32, 34, 36, 38, 40]
[41, 43, 45, 47, 49, 51, 53, 55, 57, 59]
[42, 44, 46, 48, 50, 52, 54, 56, 58, 60]
[61, 63, 65, 67, 69, 71, 73, 75, 77, 79]
[62, 64, 66, 68, 70, 72, 74, 76, 78, 80]
[81, 83, 85, 87, 89, 91, 93, 95, 97, 99]
[82, 84, 86, 88, 90, 92, 94, 96, 98, 100]
```

Previously this would have only emitted 2 groups and ignored all subsequent values:

```
[1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
```

On a finite stream, similar behavior of the previous `groupBy` implementation that would filter can be achieved like this:

```java
//odd/even into lists of 10
Observable.range(1, 100)
        .groupBy(n -> n % 2 == 0)
        .flatMap(g -> {
            return g.filter(i -> i <= 20).toList();
        }).forEach(System.out::println);
```

```
[1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
```

That however does allow the stream to complete (which may not be wanted).

To unsubscribe here are some choices that get the same output but efficiently unsubscribe up so the source only emits 40 values:

```java
Observable.timer(0, 1, TimeUnit.MILLISECONDS)
        .groupBy(n -> n % 2 == 0)
        .flatMap(g -> {
            return g.take(10).toList();
        }).take(2).toBlocking().forEach(System.out::println);
```

or

```java
Observable.timer(0, 1, TimeUnit.MILLISECONDS)
        .take(20)
        .groupBy(n -> n % 2 == 0)
        .flatMap(g -> {
            return g.toList();
        }).toBlocking().forEach(System.out::println);
```        

These show that now `groupBy` composes like any other operator without the nuanced and hidden behavior of ignoring values after a child `GroupedObservable` is unsubscribed.

Uses of `groupByUntil` can now all be done by just using operators like `take`, `takeWhile` and `takeUntil` on the `GroupedObservable` directly, such as this:

```java
Observable.from(Arrays.asList("a", "b", "c", "a", "b", "c", "a", "b", "c", "a", "b", "c", "a", "b", "c", "a", "b", "c"))
        .groupBy(n -> n)
        .flatMap(g -> {
            return g.take(3).reduce((s, s2) -> s + s2);
        }).forEach(System.out::println);
```
```
aaa
bbb
ccc
aaa
bbb
ccc
```



#### retryWhen/repeatWhen

The `retryWhen` and `repeatWhen` method signatures both emitted a `Observable<Notification>` type which could be queried to represent either `onError` in the `retryWhen` case or `onCompleted` in the `repeatWhen` case. This was found to be confusing and unnecessary. The signatures were changed to emit `Observable<Throwable>` for `retryWhen` and `Observable<Void>` for `repeatWhen` to better signal the type of notification they are emitting without the need to then query the `Notification`.

The following contrived examples shows how the `Observable<Throwable>` is used to get the error that occurred when deciding to retry:

```java
    AtomicInteger count = new AtomicInteger();
    Observable.create((Subscriber<? super String> s) -> {
        if (count.getAndIncrement() == 0) {
            s.onError(new RuntimeException("always fails"));
        } else {
            s.onError(new IllegalArgumentException("user error"));
        }
    }).retryWhen(attempts -> {
        return attempts.flatMap(throwable -> {
            if (throwable instanceof IllegalArgumentException) {
                System.out.println("don't retry on IllegalArgumentException... allow failure");
                return Observable.error(throwable);
            } else {
                System.out.println(throwable + " => retry after 1 second");
                return Observable.timer(1, TimeUnit.SECONDS);
            }
        });
    })
    .toBlocking().forEach(System.out::println);
```


#### collect

The `collect` operator was changed to require a factory method for the initial value. This allows the `Observable` to be executed multiple times and get a new value (typically a mutable data structure) each time. Prior to this the `Observable` could only be subscribed to once since it would retain the original mutable data structure and keep mutating it.

```java
Observable.range(0, 10).collect(() -> new ArrayList<Integer>(), (list, i) -> {
    list.add(i);
}).forEach(System.out::println);
```

#### Removed Scheduler.parallelism

The `Scheduler.parallelism` method was no longer being used by anything so was removed. 

#### Removed Observable.parallel

The `parallel` operator was a failed experiment and almost all uses of it were wrong and led to confusion and often bad performance. Due to this it was removed. 

Here is example code to show approaches to adding concurrency:

```java
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class ParallelExecution {

    public static void main(String[] args) {
        System.out.println("------------ mergingAsync");
        mergingAsync();
        System.out.println("------------ mergingSync");
        mergingSync();
        System.out.println("------------ mergingSyncMadeAsync");
        mergingSyncMadeAsync();
        System.out.println("------------ flatMapExampleSync");
        flatMapExampleSync();
        System.out.println("------------ flatMapExampleAsync");
        flatMapExampleAsync();
        System.out.println("------------ flatMapBufferedExampleAsync");
        flatMapBufferedExampleAsync();
        System.out.println("------------ flatMapWindowedExampleAsync");
        flatMapWindowedExampleAsync();
        System.out.println("------------");
    }

    private static void mergingAsync() {
        Observable.merge(getDataAsync(1), getDataAsync(2))
                .toBlocking().forEach(System.out::println);
    }

    /**
     * Merging async Observables subscribes to all of them concurrently.
     */
    private static void mergingSync() {
        // here you'll see the delay as each is executed synchronously
        Observable.merge(getDataSync(1), getDataSync(2))
                .toBlocking().forEach(System.out::println);
    }

    /**
     * If the Observables are synchronous they can be made async with `subscribeOn`
     */
    private static void mergingSyncMadeAsync() {
        // if you have something synchronous and want to make it async, you can schedule it like this
        // so here we see both executed concurrently
        Observable.merge(
                getDataSync(1).subscribeOn(Schedulers.io()),
                getDataSync(2).subscribeOn(Schedulers.io())
                )
                .toBlocking().forEach(System.out::println);
    }

    /**
     * flatMap uses `merge` so any async Observables it returns will execute concurrently.
     */
    private static void flatMapExampleAsync() {
        Observable.range(0, 5).flatMap(i -> {
            return getDataAsync(i);
        }).toBlocking().forEach(System.out::println);
    }

    /**
     * If synchronous Observables are merged (via flatMap here) then it will behave like `concat`
     * and execute each Observable (getDataSync here) synchronously one after the other.
     */
    private static void flatMapExampleSync() {
        Observable.range(0, 5).flatMap(i -> {
            return getDataSync(i);
        }).toBlocking().forEach(System.out::println);
    }

    /**
     * If a single stream needs to be split across multiple CPUs it is generally more efficient to do it in batches.
     * 
     * The `buffer` operator can be used to batch into chunks that are then each processed on a separate thread.
     */
    private static void flatMapBufferedExampleAsync() {
        Observable.range(0, 5000).buffer(500).flatMap(i -> {
            return Observable.from(i).subscribeOn(Schedulers.computation()).map(item -> {
                // simulate computational work
                    try {
                        Thread.sleep(1);
                    } catch (Exception e) {
                    }
                    return item + " processed " + Thread.currentThread();
                });
        }).toBlocking().forEach(System.out::println);
    }

    /**
     * Or the `window` operator can be used instead of buffer to process them as a stream instead of buffered list.
     */
    private static void flatMapWindowedExampleAsync() {
        Observable.range(0, 5000).window(500).flatMap(work -> {
            return work.observeOn(Schedulers.computation()).map(item -> {
                // simulate computational work
                    try {
                        Thread.sleep(1);
                    } catch (Exception e) {
                    }
                    return item + " processed " + Thread.currentThread();
                });
        }).toBlocking().forEach(System.out::println);
    }

    // artificial representations of IO work
    static Observable<Integer> getDataAsync(int i) {
        return getDataSync(i).subscribeOn(Schedulers.io());
    }

    static Observable<Integer> getDataSync(int i) {
        return Observable.create((Subscriber<? super Integer> s) -> {
            // simulate latency
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                s.onNext(i);
                s.onCompleted();
            });
    }
}
```

#### Removed Observable.parallelMerge

Similar to the `parallel` operator `parallelMerge` was confusing and was a failed experiment so was removed.

#### Removed publishLast/initialValue

Removed `publishLast` since it can be done with `takeLast(1).publish()`.
Removed any method overload that took an initial value since the `startWith` operator already allows that generically.

#### Removed Utility Methods/Classes

A handful of utility classes and methods that are not core to the purpose of RxJava were removed. This was preferred over having to constantly make arbitrary decisions about what utility methods should be included versus not while balancing the other goal of keeping the library small and focused.

The changes can be seen [here](https://github.com/ReactiveX/RxJava/pull/1809/files) and [here](https://github.com/ReactiveX/RxJava/pull/1806/files)

#### Observable.compose

The `Transformer` signature used by `compose` was changed from:

```java
public static interface Transformer<T, R> extends Func1<Observable<? extends T>, Observable<? extends R>>
```

to 

```java
public static interface Transformer<T, R> extends Func1<Observable<T>, Observable<? extends R>>
```

This was done after finding issues with the generics that prevented it from easily being used.


#### Removed Observable.multicast

The `multicast` operator was removed in favor of directly using `publish()`, `replay()`, `share()` or `cache()`.
This was done because `multicast` could not be made to work with reactive pull backpressure due to its use of `Subject`. The `publish` and `replay` operators on the other hand can have their implementations changed to not use a `Subject` and then support backpressure. The `publish()` operator has since been updated to support backpressure.


#### Removed Observable.*withIndex

The `takeWhileWithIndex` and `skipWhileWithIndex` operators were removed since `zipWith(Observable.range(Integer.MAX_VALUE))` can be used on any `Observable` to get an index value. Any operator may want "withIndex" so it didn't make sense to have API clutter with some and certainly not all would have this variant added. 

#### Observable.longCount -> countLong

The `longCount` operator was renamed to `countLong` so it shows up alphabetically next to `count`.



#### Full List of Changes

* [Pull 1886] (https://github.com/ReactiveX/RxJava/pull/1886) Buffer with time and merge fix
* [Pull 1882] (https://github.com/ReactiveX/RxJava/pull/1882) Remove Unused Scheduler.parallelism
* [Pull 1884] (https://github.com/ReactiveX/RxJava/pull/1884) Fix Scan/Reduce/Collect Factory Ambiguity 
* [Pull 1866] (https://github.com/ReactiveX/RxJava/pull/1866) Fix memory leak in bounded ReplaySubject due to retaining the node index
* [Pull 1834] (https://github.com/ReactiveX/RxJava/pull/1834) Subject.toSerialized
* [Pull 1832] (https://github.com/ReactiveX/RxJava/pull/1832) Fix Take Early Unsubscription Causing Interrupts
* [Pull 1835] (https://github.com/ReactiveX/RxJava/pull/1835) Scan/Reduce with Seed Factory
* [Pull 1836] (https://github.com/ReactiveX/RxJava/pull/1836) Reduce Ring Buffer Default Sizes (and lower for Android)
* [Pull 1833] (https://github.com/ReactiveX/RxJava/pull/1833) Fix Thread Safety for Unsubscribe of Window
* [Pull 1827] (https://github.com/ReactiveX/RxJava/pull/1827) CacheThreadScheduler Evictor should Check Removal
* [Pull 1830] (https://github.com/ReactiveX/RxJava/pull/1830) Fix mergeDelayError Handling of Error in Parent Observable
* [Pull 1829] (https://github.com/ReactiveX/RxJava/pull/1829) Fix Window by Count Unsubscribe Behavior
* [Pull 1788] (https://github.com/ReactiveX/RxJava/pull/1788) Remove PublishLast/InitialValue
* [Pull 1796] (https://github.com/ReactiveX/RxJava/pull/1796) Improve TestSubject Javadoc
* [Pull 1803] (https://github.com/ReactiveX/RxJava/pull/1803) Print full classname (inner class support) and fix enum output
* [Pull 1802] (https://github.com/ReactiveX/RxJava/pull/1802) add hasObservers method to Subjects
* [Pull 1806] (https://github.com/ReactiveX/RxJava/pull/1806) Remove Unnecessary Utilities
* [Pull 1809] (https://github.com/ReactiveX/RxJava/pull/1809) Remove Utility Functions from Public API
* [Pull 1813] (https://github.com/ReactiveX/RxJava/pull/1813) Fix issue #1812 that zip may swallow requests
* [Pull 1817] (https://github.com/ReactiveX/RxJava/pull/1817) Fix Synchronous OnSubscribe Exception Skips Operators
* [Pull 1819] (https://github.com/ReactiveX/RxJava/pull/1819) Fix Concat Breaks with Double onCompleted
* [Pull 1771] (https://github.com/ReactiveX/RxJava/pull/1771) On error return backpressure
* [Pull 1776] (https://github.com/ReactiveX/RxJava/pull/1776) Observable.compose Generics
* [Pull 1778] (https://github.com/ReactiveX/RxJava/pull/1778) Change Transformer to Func1<Observable<T>, Observable<R>>
* [Pull 1775] (https://github.com/ReactiveX/RxJava/pull/1775) BlockingOperatorNextTest.testSingleSourceManyIterators fix
* [Pull 1784] (https://github.com/ReactiveX/RxJava/pull/1784) Publish with Backpressure
* [Pull 1786] (https://github.com/ReactiveX/RxJava/pull/1786) Remove Multicast
* [Pull 1787] (https://github.com/ReactiveX/RxJava/pull/1787) Remove *withIndex Operators
* [Pull 1789] (https://github.com/ReactiveX/RxJava/pull/1789) GroupedObservable.from/create
* [Pull 1793] (https://github.com/ReactiveX/RxJava/pull/1793) Take/Redo Unsubscribe
* [Pull 1767] (https://github.com/ReactiveX/RxJava/pull/1767) ExecutorScheduler delivers uncaught exceptions
* [Pull 1765] (https://github.com/ReactiveX/RxJava/pull/1765) backpressure support in onErrorResumeNext* operators
* [Pull 1766] (https://github.com/ReactiveX/RxJava/pull/1766) Unhandled errors go to UncaughtExceptionHandler
* [Pull 1755] (https://github.com/ReactiveX/RxJava/pull/1755) OnSubscribeRefCount with Synchronous Support
* [Pull 1750] (https://github.com/ReactiveX/RxJava/pull/1750) Fix NPE when iterable is null
* [Pull 1745] (https://github.com/ReactiveX/RxJava/pull/1745) SerializedSubject
* [Pull 1746] (https://github.com/ReactiveX/RxJava/pull/1746) Fatal System.err Logs on Unhandled Exceptions
* [Pull 1743] (https://github.com/ReactiveX/RxJava/pull/1743) Subject Error Handling
* [Pull 1742] (https://github.com/ReactiveX/RxJava/pull/1742) EmptyObserver and TestObserver
* [Pull 1740] (https://github.com/ReactiveX/RxJava/pull/1740) longCount -> countLong
* [Pull 1736] (https://github.com/ReactiveX/RxJava/pull/1736) Fix TrampolineScheduler NullPointerException
* [Pull 1738] (https://github.com/ReactiveX/RxJava/pull/1738) Delay Operator with Reactive Pull Backpressure
* [Pull 1739] (https://github.com/ReactiveX/RxJava/pull/1739) Fix Slow Non-deterministic Test
* [Pull 1731] (https://github.com/ReactiveX/RxJava/pull/1731) Remove Unused Code
* [Pull 1733] (https://github.com/ReactiveX/RxJava/pull/1733) Move To Proper Location
* [Pull 1747] (https://github.com/ReactiveX/RxJava/pull/1747) Cleanup: final and utility classes
* [Pull 1729] (https://github.com/ReactiveX/RxJava/pull/1729) CombineLatest: Request Up When Dropping Values
* [Pull 1728] (https://github.com/ReactiveX/RxJava/pull/1728) ObserveOn Error Propagation
* [Pull 1727] (https://github.com/ReactiveX/RxJava/pull/1727) Proposed groupBy/groupByUntil Changes
* [Pull 1726] (https://github.com/ReactiveX/RxJava/pull/1726) Fix Merge: backpressure + scalarValueQueue don't play nicely
* [Pull 1720] (https://github.com/ReactiveX/RxJava/pull/1720) Change repeatWhen and retryWhen signatures.
* [Pull 1719] (https://github.com/ReactiveX/RxJava/pull/1719) Fix Bug in the onBackpressure operators
* [Pull 1687] (https://github.com/ReactiveX/RxJava/pull/1687) Don't allocate an empty ArrayList for each Observable.empty call
* [Pull 1705] (https://github.com/ReactiveX/RxJava/pull/1705) Fix null-emitting combineLatest
* [Pull 1683] (https://github.com/ReactiveX/RxJava/pull/1683) ObserveOn Error Handling
* [Pull 1686] (https://github.com/ReactiveX/RxJava/pull/1686) Fix Rx serialization bug in takeUntil again and the concurrent issue in BufferUntilSubscriber
* [Pull 1701] (https://github.com/ReactiveX/RxJava/pull/1701) Fix the compose generics
* [Pull 1712] (https://github.com/ReactiveX/RxJava/pull/1712) Fixing regression in mergeDelayError
* [Pull 1716] (https://github.com/ReactiveX/RxJava/pull/1716) Remove Observable.Parallel
* [Pull 1667] (https://github.com/ReactiveX/RxJava/pull/1667) Fix the bug that Switch doesn't propagate 'unsubscribe'
* [Pull 1659] (https://github.com/ReactiveX/RxJava/pull/1659) OperatorScan should check for MAX_VALUE on request
* [Pull 1657] (https://github.com/ReactiveX/RxJava/pull/1657) Ignore furthur messages after entering terminate state
* [Pull 1669] (https://github.com/ReactiveX/RxJava/pull/1669) Error Handling Unsubscribe and Terminal State
* [Pull 1656] (https://github.com/ReactiveX/RxJava/pull/1656) Make TakeUntil obey Rx serialization contract
* [Pull 1652] (https://github.com/ReactiveX/RxJava/pull/1652) Operator Scan Backpressure Fix
* [Pull 1645] (https://github.com/ReactiveX/RxJava/pull/1645) Remove ParallelMerge


### Version 1.0.0-rc.12 – November 17th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.12%7C)) ###

* [Pull 1886] (https://github.com/ReactiveX/RxJava/pull/1886) Buffer with time and merge fix

### Version 1.0.0-rc.11 – November 15th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.11%7C)) ###

* [Pull 1882] (https://github.com/ReactiveX/RxJava/pull/1882) Remove Unused Scheduler.parallelism
* [Pull 1884] (https://github.com/ReactiveX/RxJava/pull/1884) Fix Scan/Reduce/Collect Factory Ambiguity 
* [Pull 1866] (https://github.com/ReactiveX/RxJava/pull/1866) Fix memory leak in bounded ReplaySubject due to retaining the node index

### Version 0.20.7 – November 11th 2014 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.7%22)) ###

* [Pull 1863] (https://github.com/ReactiveX/RxJava/pull/1863) Fix Concat Breaks with Double onCompleted

### Version 1.0.0-rc.10 – November 8th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.10%7C)) ###

* [Pull 1834] (https://github.com/ReactiveX/RxJava/pull/1834) Subject.toSerialized
* [Pull 1832] (https://github.com/ReactiveX/RxJava/pull/1832) Fix Take Early Unsubscription Causing Interrupts
* [Pull 1835] (https://github.com/ReactiveX/RxJava/pull/1835) Scan/Reduce with Seed Factory
* [Pull 1836] (https://github.com/ReactiveX/RxJava/pull/1836) Reduce Ring Buffer Default Sizes (and lower for Android)
* [Pull 1833] (https://github.com/ReactiveX/RxJava/pull/1833) Fix Thread Safety for Unsubscribe of Window
* [Pull 1827] (https://github.com/ReactiveX/RxJava/pull/1827) CacheThreadScheduler Evictor should Check Removal
* [Pull 1830] (https://github.com/ReactiveX/RxJava/pull/1830) Fix mergeDelayError Handling of Error in Parent Observable
* [Pull 1829] (https://github.com/ReactiveX/RxJava/pull/1829) Fix Window by Count Unsubscribe Behavior

### Version 1.0.0-rc.9 – November 2nd 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.9%7C)) ###

* [Pull 1788] (https://github.com/ReactiveX/RxJava/pull/1788) Remove PublishLast/InitialValue
* [Pull 1796] (https://github.com/ReactiveX/RxJava/pull/1796) Improve TestSubject Javadoc
* [Pull 1803] (https://github.com/ReactiveX/RxJava/pull/1803) Print full classname (inner class support) and fix enum output
* [Pull 1802] (https://github.com/ReactiveX/RxJava/pull/1802) add hasObservers method to Subjects
* [Pull 1806] (https://github.com/ReactiveX/RxJava/pull/1806) Remove Unnecessary Utilities
* [Pull 1809] (https://github.com/ReactiveX/RxJava/pull/1809) Remove Utility Functions from Public API
* [Pull 1813] (https://github.com/ReactiveX/RxJava/pull/1813) Fix issue #1812 that zip may swallow requests
* [Pull 1817] (https://github.com/ReactiveX/RxJava/pull/1817) Fix Synchronous OnSubscribe Exception Skips Operators
* [Pull 1819] (https://github.com/ReactiveX/RxJava/pull/1819) Fix Concat Breaks with Double onCompleted

### Version 1.0.0-rc.8 – October 23rd 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.8%7C)) ###

* [Pull 1771] (https://github.com/ReactiveX/RxJava/pull/1771) On error return backpressure
* [Pull 1776] (https://github.com/ReactiveX/RxJava/pull/1776) Observable.compose Generics
* [Pull 1778] (https://github.com/ReactiveX/RxJava/pull/1778) Change Transformer to Func1<Observable<T>, Observable<R>>
* [Pull 1775] (https://github.com/ReactiveX/RxJava/pull/1775) BlockingOperatorNextTest.testSingleSourceManyIterators fix
* [Pull 1784] (https://github.com/ReactiveX/RxJava/pull/1784) Publish with Backpressure
* [Pull 1786] (https://github.com/ReactiveX/RxJava/pull/1786) Remove Multicast
* [Pull 1787] (https://github.com/ReactiveX/RxJava/pull/1787) Remove *withIndex Operators
* [Pull 1789] (https://github.com/ReactiveX/RxJava/pull/1789) GroupedObservable.from/create
* [Pull 1793] (https://github.com/ReactiveX/RxJava/pull/1793) Take/Redo Unsubscribe

### Version 1.0.0-rc.7 – October 16th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.7%7C)) ###

* [Pull 1767] (https://github.com/ReactiveX/RxJava/pull/1767) ExecutorScheduler delivers uncaught exceptions
* [Pull 1765] (https://github.com/ReactiveX/RxJava/pull/1765) backpressure support in onErrorResumeNext* operators
* [Pull 1766] (https://github.com/ReactiveX/RxJava/pull/1766) Unhandled errors go to UncaughtExceptionHandler
* [Pull 1755] (https://github.com/ReactiveX/RxJava/pull/1755) OnSubscribeRefCount with Synchronous Support
* [Pull 1750] (https://github.com/ReactiveX/RxJava/pull/1750) Fix NPE when iterable is null

### Version 0.20.6 – October 15th 2014 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.6%22)) ###

* [Pull 1721] (https://github.com/ReactiveX/RxJava/pull/1721) Bug in the onBackpressure operators 
* [Pull 1695] (https://github.com/ReactiveX/RxJava/pull/1695) rewrite OnSubscribeRefCount to handle synchronous source
* [Pull 1761] (https://github.com/ReactiveX/RxJava/pull/1761) Fix null-emitting combineLatest

### Version 1.0.0-rc.6 – October 10th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.6%7C)) ###

This release is primarily bug fixes along with API cleanup by renaming `longCount` to `countLong` to be alphabetically sorted alongside `count`.

* [Pull 1745] (https://github.com/ReactiveX/RxJava/pull/1745) SerializedSubject
* [Pull 1746] (https://github.com/ReactiveX/RxJava/pull/1746) Fatal System.err Logs on Unhandled Exceptions
* [Pull 1743] (https://github.com/ReactiveX/RxJava/pull/1743) Subject Error Handling
* [Pull 1742] (https://github.com/ReactiveX/RxJava/pull/1742) EmptyObserver and TestObserver
* [Pull 1740] (https://github.com/ReactiveX/RxJava/pull/1740) longCount -> countLong
* [Pull 1736] (https://github.com/ReactiveX/RxJava/pull/1736) Fix TrampolineScheduler NullPointerException
* [Pull 1738] (https://github.com/ReactiveX/RxJava/pull/1738) Delay Operator with Reactive Pull Backpressure
* [Pull 1739] (https://github.com/ReactiveX/RxJava/pull/1739) Fix Slow Non-deterministic Test
* [Pull 1731] (https://github.com/ReactiveX/RxJava/pull/1731) Remove Unused Code
* [Pull 1733] (https://github.com/ReactiveX/RxJava/pull/1733) Move To Proper Location
* [Pull 1747] (https://github.com/ReactiveX/RxJava/pull/1747) Cleanup: final and utility classes

### Version 1.0.0-rc.5 – October 6th 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.5%7C)) ###

* [Pull 1729] (https://github.com/ReactiveX/RxJava/pull/1729) CombineLatest: Request Up When Dropping Values
* [Pull 1728] (https://github.com/ReactiveX/RxJava/pull/1728) ObserveOn Error Propagation
* [Pull 1727] (https://github.com/ReactiveX/RxJava/pull/1727) Proposed groupBy/groupByUntil Changes
* [Pull 1726] (https://github.com/ReactiveX/RxJava/pull/1726) Fix Merge: backpressure + scalarValueQueue don't play nicely
* [Pull 1720] (https://github.com/ReactiveX/RxJava/pull/1720) Change repeatWhen and retryWhen signatures.
* [Pull 1719] (https://github.com/ReactiveX/RxJava/pull/1719) Fix Bug in the onBackpressure operators

### groupBy/groupByUntil

The `groupByUntil` operator was removed by collapsing its behavior into `groupBy`. Previously on `groupBy` when a child `GroupedObservable` was unsubscribed it would internally retain the state and ignore all future `onNext` for that key.

This matched behavior in Rx.Net but was found to be non-obvious and almost everyone using `groupBy` on long-lived streams actually wanted the behavior of `groupByUntil` where an unsubscribed `GroupedObservable` would clean up the resources and then if `onNext` for that key arrived again a new `GroupedObservable` would be emitted.

Adding backpressure (reactive pull) to `groupByUntil` was found to not work easily with its signatures so before 1.0 Final it was decided to collapse `groupBy` and `groupByUntil`. Further details on this can be found in [Pull Request 1727](https://github.com/ReactiveX/RxJava/pull/1727).

Here is an example of how `groupBy` now behaves when a child `GroupedObservable` is unsubscribed (using `take` here):

```java
// odd/even into lists of 10
Observable.range(1, 100)
        .groupBy(n -> n % 2 == 0)
        .flatMap(g -> {
            return g.take(10).toList();
        }).forEach(System.out::println);
```

```
[1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
[21, 23, 25, 27, 29, 31, 33, 35, 37, 39]
[22, 24, 26, 28, 30, 32, 34, 36, 38, 40]
[41, 43, 45, 47, 49, 51, 53, 55, 57, 59]
[42, 44, 46, 48, 50, 52, 54, 56, 58, 60]
[61, 63, 65, 67, 69, 71, 73, 75, 77, 79]
[62, 64, 66, 68, 70, 72, 74, 76, 78, 80]
[81, 83, 85, 87, 89, 91, 93, 95, 97, 99]
[82, 84, 86, 88, 90, 92, 94, 96, 98, 100]
```

Previously this would have only emitted 2 groups and ignored all subsequent values:

```
[1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
```

On a finite stream, similar behavior of the previous `groupBy` implementation that would filter can be achieved like this:

```java
//odd/even into lists of 10
Observable.range(1, 100)
        .groupBy(n -> n % 2 == 0)
        .flatMap(g -> {
            return g.filter(i -> i <= 20).toList();
        }).forEach(System.out::println);
```

```
[1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
```

That however does allow the stream to complete (which may not be wanted).

To unsubscribe here are some choices that get the same output but efficiently unsubscribe up so the source only emits 40 values:

```java
Observable.timer(0, 1, TimeUnit.MILLISECONDS)
        .groupBy(n -> n % 2 == 0)
        .flatMap(g -> {
            return g.take(10).toList();
        }).take(2).toBlocking().forEach(System.out::println);
```

or

```java
Observable.timer(0, 1, TimeUnit.MILLISECONDS)
        .take(20)
        .groupBy(n -> n % 2 == 0)
        .flatMap(g -> {
            return g.toList();
        }).toBlocking().forEach(System.out::println);
```        

These show that now `groupBy` composes like any other operator without the nuanced and hidden behavior of ignoring values after a child `GroupedObservable` is unsubscribed.

Uses of `groupByUntil` can now all be done by just using operators like `take`, `takeWhile` and `takeUntil` on the `GroupedObservable` directly, such as this:

```java
Observable.from(Arrays.asList("a", "b", "c", "a", "b", "c", "a", "b", "c", "a", "b", "c", "a", "b", "c", "a", "b", "c"))
        .groupBy(n -> n)
        .flatMap(g -> {
            return g.take(3).reduce((s, s2) -> s + s2);
        }).forEach(System.out::println);
```
```
aaa
bbb
ccc
aaa
bbb
ccc
```



### retryWhen/repeatWhen

The `retryWhen` and `repeatWhen` method signatures both emitted a `Observable<Notification>` type which could be queried to represent either `onError` in the `retryWhen` case or `onCompleted` in the `repeatWhen` case. This was found to be confusing and unnecessary. The signatures were changed to emit `Observable<Throwable>` for `retryWhen` and `Observable<Void>` for `repeatWhen` to better signal the type of notification they are emitting without the need to then query the `Notification`.

The following contrived examples shows how the `Observable<Throwable>` is used to get the error that occurred when deciding to retry:

```java
    AtomicInteger count = new AtomicInteger();
    Observable.create((Subscriber<? super String> s) -> {
        if (count.getAndIncrement() == 0) {
            s.onError(new RuntimeException("always fails"));
        } else {
            s.onError(new IllegalArgumentException("user error"));
        }
    }).retryWhen(attempts -> {
        return attempts.flatMap(throwable -> {
            if (throwable instanceof IllegalArgumentException) {
                System.out.println("don't retry on IllegalArgumentException... allow failure");
                return Observable.error(throwable);
            } else {
                System.out.println(throwable + " => retry after 1 second");
                return Observable.timer(1, TimeUnit.SECONDS);
            }
        });
    })
    .toBlocking().forEach(System.out::println);
```

### Version 1.0.0-rc.4 – October 2nd 2014 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.4%7C)) ###

* [Pull 1687] (https://github.com/ReactiveX/RxJava/pull/1687) Don't allocate an empty ArrayList for each Observable.empty call
* [Pull 1705] (https://github.com/ReactiveX/RxJava/pull/1705) Fix null-emitting combineLatest
* [Pull 1683] (https://github.com/ReactiveX/RxJava/pull/1683) ObserveOn Error Handling
* [Pull 1686] (https://github.com/ReactiveX/RxJava/pull/1686) Fix Rx serialization bug in takeUntil again and the concurrent issue in BufferUntilSubscriber
* [Pull 1701] (https://github.com/ReactiveX/RxJava/pull/1701) Fix the compose generics
* [Pull 1712] (https://github.com/ReactiveX/RxJava/pull/1712) Fixing regression in mergeDelayError
* [Pull 1716] (https://github.com/ReactiveX/RxJava/pull/1716) Remove Observable.Parallel

### Version 0.20.5 – October 2nd 2014 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.5%22)) ###

* [Pull 1686] (https://github.com/ReactiveX/RxJava/pull/1686) Fix Rx serialization bug in takeUntil again and the concurrent issue in BufferUntilSubscriber
* [Pull 1701] (https://github.com/ReactiveX/RxJava/pull/1701) Fix the compose generics
* [Pull 1712] (https://github.com/ReactiveX/RxJava/pull/1712) Fixing regression in mergeDelayError
* [Pull 1715] (https://github.com/ReactiveX/RxJava/pull/1715) Deprecate Observable.Parallel

### Version 1.0.0-rc.3 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.3%7C)) ###

* Merging fixes from 0.20.4 into 1.x branch

### Version 0.20.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.4%22)) ###

* [Pull 1667] (https://github.com/ReactiveX/RxJava/pull/1667) Fix the bug that Switch doesn't propagate 'unsubscribe'
* [Pull 1659] (https://github.com/ReactiveX/RxJava/pull/1659) OperatorScan should check for MAX_VALUE on request
* [Pull 1657] (https://github.com/ReactiveX/RxJava/pull/1657) Ignore furthur messages after entering terminate state
* [Pull 1669] (https://github.com/ReactiveX/RxJava/pull/1669) Error Handling Unsubscribe and Terminal State
* [Pull 1656] (https://github.com/ReactiveX/RxJava/pull/1656) Make TakeUntil obey Rx serialization contract
* [Pull 1664] (https://github.com/ReactiveX/RxJava/pull/1664) StringObservable.split NPE fixes

### Version 1.0.0-rc.2 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.2%7C)) ###

* [Pull 1652] (https://github.com/ReactiveX/RxJava/pull/1652) Operator Scan Backpressure Fix
* [Pull 1645] (https://github.com/ReactiveX/RxJava/pull/1645) Remove ParallelMerge

### Version 0.20.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.3%22)) ###

* [Pull 1648] (https://github.com/ReactiveX/RxJava/pull/1648) Operator Scan Backpressure Fix
* [Pull 1651] (https://github.com/ReactiveX/RxJava/pull/1651) RxScala: Fix the problem that Subscriber.onStart isn't called
* [Pull 1641] (https://github.com/ReactiveX/RxJava/pull/1641) RxScala: Fix infinite recursive onStart call in Subscriber
* [Pull 1646] (https://github.com/ReactiveX/RxJava/pull/1646) Deprecate ParallelMerge

### Version 1.0.0-rc.1 ([Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxjava%7C1.0.0-rc.1%7C)) ###

The first release candidate for [1.0.0](https://github.com/ReactiveX/RxJava/issues/1001).

This is the same code as version 0.20.2 except:

- all deprecated methods and types are deleted
- now published to groupId `io.reactivex` instead of `com.netflix.rxjava`
- artifactId is now `rxjava` instead of `rxjava-core`

```
io.reactivex:rxjava:1.0.0-rc.1
```

- all sub-projects are separated into their own projects and no longer released along with RxJava

The artifacts can be found on maven Central at: http://repo1.maven.org/maven2/io/reactivex/rxjava/1.0.0-rc.1/


### Version 0.20.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.2%22)) ###

* [Pull 1637] (https://github.com/ReactiveX/RxJava/pull/1637) Optimize single BlockingObservable operations

### Version 0.20.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.1%22)) ###

* [Pull 1631] (https://github.com/ReactiveX/RxJava/pull/1631) Handle Fatal Exceptions in doOn* operators
* [Pull 1625] (https://github.com/ReactiveX/RxJava/pull/1625) RxScala: Mark superfluous from/empty methods with scheduler parameter as deprecated
* [Pull 1623] (https://github.com/ReactiveX/RxJava/pull/1623) RxScala: Add more operators to match RxJava
* [Pull 1632] (https://github.com/ReactiveX/RxJava/pull/1632) Composite Exception - Circular Reference Handling

### Version 0.20.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.0%22)) ###

RxJava 0.20.0 is a major release that adds "reactive pull" support for backpressure along with several other enhancements leading into the 1.0 release.

#### Reactive Pull for Backpressure

Solutions for backpressure was the major focus of this release. A "reactive pull" implementation was implemented. Documentation on this and other options for backpressure are found in the wiki: https://github.com/ReactiveX/RxJava/wiki/Backpressure

The reactive pull solution evolved out of [several prototypes and interaction with many people over the months](https://github.com/ReactiveX/RxJava/issues/1000). 

##### Signature Changes

A new type `Producer` has been added:

```java
public interface Producer {
    public void request(long n);
}
```

The `Subscriber` type now has these methods added:

```java
public abstract class Subscriber<T> implements Observer<T>, Subscription {
    public void onStart();
    protected final void request(long n);
    public final void setProducer(Producer producer);
}
```


##### Examples


This trivial example shows requesting values one at a time:

```java
Observable.from(1, 2, 3, 4).subscribe(new Subscriber<Integer>() {

    @Override
    public void onStart() {
        // on start this tells it to request 1
        // otherwise it defaults to request(Long.MAX_VALUE)
        request(1);
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
    }

    @Override
    public void onNext(Integer t) {
        System.out.println(t);
        // as each onNext is consumed, request another 
        // otherwise the Producer will not send more
        request(1);
    }

});
```

The [OnSubscribeFromIterable](https://github.com/ReactiveX/RxJava/blob/1.x/rxjava/src/main/java/rx/internal/operators/OnSubscribeFromIterable.java) operator shows how an `Iterable` is consumed with backpressure.

Some hi-lights (modified for simplicity rather than performance and completeness):

```java
public final class OnSubscribeFromIterable<T> implements OnSubscribe<T> {

    @Override
    public void call(final Subscriber<? super T> o) {
        final Iterator<? extends T> it = is.iterator();
		// instead of emitting directly to the Subscriber, it emits a Producer
        o.setProducer(new IterableProducer<T>(o, it));
    }
	
	private static final class IterableProducer<T> implements Producer {
	
        public void request(long n) {
            int _c = requested.getAndAdd(n);
            if (_c == 0) {
                while (it.hasNext()) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    T t = it.next();
                    o.onNext(t);
                    if (requested.decrementAndGet() == 0) {
                        // we're done emitting the number requested so return
                        return;
                    }
                }

                o.onCompleted();
            }

        }
	}
}
```

The [`observeOn` operator](https://github.com/ReactiveX/RxJava/blob/1.x/rxjava/src/main/java/rx/internal/operators/OperatorObserveOn.java#L85) is a sterotypical example of queuing on one side of a thread and draining on the other, now with backpressure.

```java
private static final class ObserveOnSubscriber<T> extends Subscriber<T> {
        @Override
        public void onStart() {
            // signal that this is an async operator capable of receiving this many
            request(RxRingBuffer.SIZE);
        }
		
        @Override
        public void onNext(final T t) {
            try {
				// enqueue
                queue.onNext(t);
            } catch (MissingBackpressureException e) {
				// fail if the upstream has not obeyed our backpressure requests
                onError(e);
                return;
            }
			// attempt to schedule draining if needed
            schedule();
        }
		
		// the scheduling polling will then drain the queue and invoke `request(n)` to request more after draining
}
```

Many use cases will be able to use `Observable.from`, `Observable.onBackpressureDrop` and `Observable.onBackpressureBuffer` to achieve "reactive pull backpressure" without manually implementing `Producer` logic. Also, it is optional to make an `Observable` support backpressure. It can remain completely reactive and just push events as it always has. Most uses of RxJava this works just fine. If backpressure is needed then it can be migrated to use a `Producer` or several other approaches to flow control exist such as `throttle`, `sample`, `debounce`, `window`, `buffer`, `onBackpressureBuffer`, and `onBackpressureDrop`.


The [wiki](https://github.com/ReactiveX/RxJava/wiki/Backpressure) provides further documentation.

##### Relation to Reactive Streams

Contributors to RxJava are involved in defining the [Reactive Streams](https://github.com/reactive-streams/reactive-streams/) spec. RxJava 1.0 is trying to comply with the semantic rules but is not attempting to comply with the type signatures. It will however have a separate module that acts as a bridge between the RxJava `Observable` and the Reactive Stream types.

The reasons for this are:

- Rx has `Observer.onCompleted` whereas Reactive Streams has `onComplete`. This is a massive breaking change to remove a "d".
- The RxJava `Subscription` is used also a "Closeable"/"Disposable" and it does not work well to make it now also be used for `request(n)`, hence the separate type `Producer` in RxJava. It was attempted to reuse `rx.Subscription` but it couldn't be done without massive breaking changes.
- Reactive Streams uses `onSubscribe(Subscription s)` whereas RxJava injects the `Subscription` as the `Subscriber`. Again, this change could not be done without major breaking changes.
- RxJava 1.0 needs to be backwards compatible with the major Rx contracts established during the 0.x roadmap.

Considering these things, the major semantics of `request(long n)` for backpressure are compatible and this will allow interop with a bridge between the interfaces. 



#### New Features

##### Compose/Transformer

The `compose` operator is similar to `lift` but allows custom operator implementations that are chaining `Observable` operators whereas `lift` is directly implementing the raw `Subscriber` logic.

Here is a trival example demonstrating how using `compose` is a better option than `lift` when existing `Observable` operators can be used to achieve the custom behavior.

```java
import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Subscriber;

public class ComposeExample {

    public static void main(String[] args) {
        Observable.just("hello").compose(appendWorldTransformer()).forEach(System.out::println);
        Observable.just("hello").lift(appendWorldOperator()).forEach(System.out::println);
    }

    // if existing operators can be used, compose with Transformer is ideal
    private static Transformer<? super String, String> appendWorldTransformer() {
        return o -> o.map(s -> s + " world!").finallyDo(() -> {
            System.out.println("  some side-effect");
        });
    }

    // whereas lift is more low level
    private static Operator<? super String, String> appendWorldOperator() {
        return new Operator<String, String>() {

            @Override
            public Subscriber<? super String> call(Subscriber<? super String> child) {
                return new Subscriber<String>(child) {

                    @Override
                    public void onCompleted() {
                        child.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(String t) {
                        child.onNext(t + " world!");
                        System.out.println("  some side-effect");
                    }

                };
            }

        };
    }
}
```

##### retryWhen/repeatWhen

New operators `retryWhen` and `repeatWhen` were added which offer support for more advanced recursion such as retry with exponential backoff.

Here is an example that increases delay between each retry:

```java
Observable.create((Subscriber<? super String> s) -> {
    System.out.println("subscribing");
    s.onError(new RuntimeException("always fails"));
}).retryWhen(attempts -> {
    return attempts.zipWith(Observable.range(1, 3), (n, i) -> i).flatMap(i -> {
        System.out.println("delay retry by " + i + " second(s)");
        return Observable.timer(i, TimeUnit.SECONDS);
    });
}).toBlocking().forEach(System.out::println);
```


#### Breaking Changes


The use of `Producer` has been added in such a way that it is optional and additive, but some operators that used to have unbounded queues are now bounded. This means that if a source `Observable` emits faster than the `Observer` can consume them, a `MissingBackpressureException` can be emitted via `onError`.

This semantic change can break existing code.

There are two ways of resolving this:

1) Modify the source `Observable` to use `Producer` and support backpressure.
2) Use newly added operators such as `onBackpressureBuffer` or `onBackpressureDrop` to choose a strategy for the source `Observable` of how to behave when it emits more data than the consuming `Observer` is capable of handling. Use of `onBackpressureBuffer` effectively returns it to having an unbounded buffer and behaving like version 0.19 or earlier.

Example:

```java
sourceObservable.onBackpressureBuffer().subscribe(slowConsumer);
```


#### Deprecations

Various methods, operators or classes have been deprecated and will be removed in 1.0. Primarily they have been done to remove ambiguity, remove nuanced functionality that is easy to use wrong, clear out superfluous methods and eliminate cruft that was added during the 0.x development process but has been replaced.

For example, `Observable.from(T)` was deprecated in favor of `Observable.just(T)` despite being a [painful breaking change](https://github.com/ReactiveX/RxJava/issues/1563) so as to solve ambiguity with `Observable.from(Iterable)`.

This means that the upgrade from 0.20 to 1.0 will be breaking. This is being done so that the 1.x version can be a long-lived stable API built upon as clean a foundation as possible. 

A stable API for RxJava is important because it is intended to be a foundational library that many projects will depend upon. The deprecations are intended to help this be achieved.

#### Future

The next release will be 1.0 (after a few release candidates). The RxJava project has been split up into many new top-level projects at https://github.com/ReactiveX so each of their release cycles and version strategies can be decoupled. 

The 1.x version is intended to be stable for many years and target Java 6, 7 and 8. The expected outcome is for a 2.x version to target Java 8+ but for RxJava 1.x and 2.x to co-exist and both be living, supported versions.


### Version 0.20.0-RC6 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.0-RC6%22)) ###

Further fixes and enhancements bringing us close to completing 0.20.0 and almost ready for 1.0.

A more major change in this release is the deprecation of `Observable.from(T)`. The full discussion can be seen in [#1563](https://github.com/ReactiveX/RxJava/issues/1563).

* [Pull 1575] (https://github.com/ReactiveX/RxJava/pull/1575) combineLatest with backpressure
* [Pull 1569] (https://github.com/ReactiveX/RxJava/pull/1569) Compose/Transform Covariance
* [Pull 1577] (https://github.com/ReactiveX/RxJava/pull/1577) Fix the compose covariance
* [Pull 1581] (https://github.com/ReactiveX/RxJava/pull/1581) zip(Iterable) -> zipWith(Iterable)
* [Pull 1582] (https://github.com/ReactiveX/RxJava/pull/1582) Deprecate GroupedObservable.from
* [Pull 1583] (https://github.com/ReactiveX/RxJava/pull/1583) Redo/Repeat Backpressure
* [Pull 1576] (https://github.com/ReactiveX/RxJava/pull/1576) from(T) -> just(T)
* [Pull 1545] (https://github.com/ReactiveX/RxJava/pull/1545) Make Android ViewObservable.input observe TextView instead of String

### Version 0.20.0-RC5 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.0-RC5%22)) ###

Version 0.20.0-RC5 updates `parallel`, `buffer(size)`, `switchOnNext`, `repeat`, and `retry` to support "reactive pull" backpressure. It adds a `groupBy` overload with an element selector, a new `compose` method as an alternative to `lift` for custom operators, fixes bugs and other general improvements.

There are still oustanding items being tracked for 0.20 that need to be completed for the final release.

* [Pull 1573] (https://github.com/ReactiveX/RxJava/pull/1573) Backpressure: parallel 
* [Pull 1572] (https://github.com/ReactiveX/RxJava/pull/1572) Remove Timeout in Blocking Iterator
* [Pull 1570] (https://github.com/ReactiveX/RxJava/pull/1570) RxClojure: Fix for mapcat
* [Pull 1568] (https://github.com/ReactiveX/RxJava/pull/1568) Compose/Transformer
* [Pull 1567] (https://github.com/ReactiveX/RxJava/pull/1567) groupBy with element selector
* [Pull 1565] (https://github.com/ReactiveX/RxJava/pull/1565) Fixing Kotlin Defer
* [Pull 1507] (https://github.com/ReactiveX/RxJava/pull/1507) BufferWithSize with Backpressure Support
* [Pull 1557] (https://github.com/ReactiveX/RxJava/pull/1557) SwitchOnNext with backpressure support
* [Pull 1562] (https://github.com/ReactiveX/RxJava/pull/1562) TakeLastTimed with backpressure support
* [Pull 1564] (https://github.com/ReactiveX/RxJava/pull/1564) RxScala: Fix errors in Completeness.scala and also improve it
* [Pull 1548] (https://github.com/ReactiveX/RxJava/pull/1548) Adding backpressure to OnSubscribeRedo
* [Pull 1559] (https://github.com/ReactiveX/RxJava/pull/1559) More consistent hooks for scheduler plugins
* [Pull 1561] (https://github.com/ReactiveX/RxJava/pull/1561) Remove Variance on Defer
* [Pull 1537] (https://github.com/ReactiveX/RxJava/pull/1537) recursive scheduling in RxScala
* [Pull 1560] (https://github.com/ReactiveX/RxJava/pull/1560) flatMap overloads
* [Pull 1558] (https://github.com/ReactiveX/RxJava/pull/1558) mergeMap generics
* [Pull 1552] (https://github.com/ReactiveX/RxJava/pull/1552) Fixing a bug and a potential for other concurrency issues
* [Pull 1555] (https://github.com/ReactiveX/RxJava/pull/1555) RxScala: Add retryWhen/repeatWhen methods

### Version 0.20.0-RC4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.0-RC4%22)) ###

Version 0.20.0-RC4 continues bug fixes and completing work related to "reactive pull" backpressure. This release updates `amb` and `concat` to connect the backpressure `request`.

Internal uses of `RxRingBuffer` migrated to using `SpmcArrayQueue` which significantly reduces object allocations. See [#1526](https://github.com/ReactiveX/RxJava/pull/1526) for details.

The `multicast` operators were updated to use a `Subject` factory so that `Observable` sequences can be reused. See [#1515](https://github.com/ReactiveX/RxJava/pull/1515) for details.


* [Pull 1534] (https://github.com/ReactiveX/RxJava/pull/1534) Concat Backpressure
* [Pull 1533] (https://github.com/ReactiveX/RxJava/pull/1533) Amb + Backpressure
* [Pull 1527] (https://github.com/ReactiveX/RxJava/pull/1527) Failing unit test for reduce, showing it does not implement backpressure correctly
* [Pull 1528] (https://github.com/ReactiveX/RxJava/pull/1528) Add operators to create Observables from BroadcastReceiver (rebased)
* [Pull 1523] (https://github.com/ReactiveX/RxJava/pull/1523) Fix issue #1522: takeLast
* [Pull 1530] (https://github.com/ReactiveX/RxJava/pull/1530) Fix the unbounded check for merge
* [Pull 1526] (https://github.com/ReactiveX/RxJava/pull/1526) Restore use of SpmcArrayQueue in RxRingBuffer
* [Pull 1468] (https://github.com/ReactiveX/RxJava/pull/1468) RxScala: Update CompletenessTest.scala
* [Pull 1515] (https://github.com/ReactiveX/RxJava/pull/1515) Support Subject Factory with Multicast
* [Pull 1518] (https://github.com/ReactiveX/RxJava/pull/1518) Fix typos in javadoc comments
* [Pull 1521] (https://github.com/ReactiveX/RxJava/pull/1521) Fix toIterator Exception Handling
* [Pull 1520] (https://github.com/ReactiveX/RxJava/pull/1520) Fix non-deterministic RxRingBuffer test 


### Version 0.20.0-RC3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.0-RC3%22)) ###

Version 0.20.0-RC3 preview release fixes several bugs related to backpressure and adds `retryWhen`, `repeatWhen` for more advanced recursion use cases like retry with exponential backoff.

This version passed the Netflix API production canary process. Please test this against your code to help us find any issues before we release 0.20.0.

* [Pull 1493] (https://github.com/ReactiveX/RxJava/pull/1493) retryWhen/repeatWhen
* [Pull 1494] (https://github.com/ReactiveX/RxJava/pull/1494) zipWith
* [Pull 1501] (https://github.com/ReactiveX/RxJava/pull/1501) blocking synchronous next
* [Pull 1498] (https://github.com/ReactiveX/RxJava/pull/1498) non-deterministic testUserSubscriberUsingRequestAsync
* [Pull 1497] (https://github.com/ReactiveX/RxJava/pull/1497) spsc ring buffer concurrency test
* [Pull 1496] (https://github.com/ReactiveX/RxJava/pull/1496) Change RxRingBuffer Queue Usage
* [Pull 1491] (https://github.com/ReactiveX/RxJava/pull/1491) Concat Outer Backpressure
* [Pull 1490] (https://github.com/ReactiveX/RxJava/pull/1490) non-deterministic timeouts on slow machines
* [Pull 1489] (https://github.com/ReactiveX/RxJava/pull/1489) Backpressure Fixes and Docs
* [Pull 1474] (https://github.com/ReactiveX/RxJava/pull/1474) Ignore backpressure for OperatorToObservableSortedList
* [Pull 1473] (https://github.com/ReactiveX/RxJava/pull/1473) OperatorAny needs to handle backpressure
* [Pull 1472] (https://github.com/ReactiveX/RxJava/pull/1472) Add test of backpressure to OperatorAll
* [Pull 1469] (https://github.com/ReactiveX/RxJava/pull/1469) ToList operator needs to ignore backpressure
* [Pull 1393] (https://github.com/ReactiveX/RxJava/pull/1393) Add cache(int capacity) to Observable
* [Pull 1431] (https://github.com/ReactiveX/RxJava/pull/1431) CompositeException fix for Android
* [Pull 1436] (https://github.com/ReactiveX/RxJava/pull/1436) Correct warnings


### Version 0.20.0-RC2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.0-RC2%22)) ###

Version 0.20.0-RC2 preview release adds support for backpressure to the `zip` operators, fixes bugs and removes the `Subscribe.onSetProducer` method.

This means signature changes are modified to be:

The new type `Producer` ->

```java
public interface Producer {
    public void request(long n);
}
```

New methods added to `Subscriber` ->

```java
public abstract class Subscriber<T> implements Observer<T>, Subscription {
	public void onStart();
	protected final void request(long n);
	public final void setProducer(Producer producer);
}
```


* [Pull 1448] (https://github.com/ReactiveX/RxJava/pull/1448) RxScala: Add Scala idiomatic methods
* [Pull 1446] (https://github.com/ReactiveX/RxJava/pull/1446) Zip with Backpressure Support
* [Pull 1454] (https://github.com/ReactiveX/RxJava/pull/1454) doOnEachObserver fix
* [Pull 1457] (https://github.com/ReactiveX/RxJava/pull/1457) MergeDelayError & OnErrorFlatMap w/ Merge
* [Pull 1458] (https://github.com/ReactiveX/RxJava/pull/1458) Remove Pivot Operator
* [Pull 1459] (https://github.com/ReactiveX/RxJava/pull/1459) Remove Subscriber.onSetProducer
* [Pull 1462] (https://github.com/ReactiveX/RxJava/pull/1462) Merge Perf Fix: Re-enable fast-path
* [Pull 1463] (https://github.com/ReactiveX/RxJava/pull/1463) Merge Bug: Missing Emissions

### Version 0.20.0-RC1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.20.0-RC1%22)) ###


Version 0.20.0-RC1 is a preview release that adds backpressure support to RxJava as per issue [#1000](https://github.com/ReactiveX/RxJava/issues/1000). It has been done in a way that is mostly additive and most existing code will not be affected by these additions. A section below on "Breaking Changes" will discuss use cases that do break and how to deal with them.

This release has been tested successfully in Netflix production canaries, but that does not exercise all use cases or operators, nor does it leverage the newly added backpressure functionality (though the backpressure code paths are used).


#### Outstanding Work

- The `zip` operator has not yet been upgraded to support backpressure. The work is almost done and it will be included in the next release.
- Not all operators have yet been reviewed for whether they need to be changed in any way. 
- Temporal operators (like `buffer`, `window`, `sample`, etc) need to be modified to disable backpressure upstream (using `request(Long.MAX_VALUE)`) and a decision made about how downstream backpressure requests will be supported.
- Ensure all code works on Android. New data structures rely on `sun.misc.Unsafe` but are conditionally used only when it is available. We need to ensure those conditions are working and the alternative implementations are adequate. The default buffer size of 1024 also needs to be reviewed for whether it is a correct default for all systems, or needs to be modified by environment (such as smaller for Android).
- Ensure use cases needing backpressure all work.

#### Signature Changes

A new type `Producer` has been added:

```java
public interface Producer {
    public void request(long n);
}
```

The `Subscriber` type now has these methods added:

```java
public abstract class Subscriber<T> implements Observer<T>, Subscription {
	public void onStart();
	public final void request(long n);
	public final void setProducer(Producer producer);
	protected Producer onSetProducer(Producer producer);
}
```


#### Examples


This trivial example shows requesting values one at a time:

```java
Observable.from(1, 2, 3, 4).subscribe(new Subscriber<Integer>() {

    @Override
    public void onStart() {
        request(1);
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
    }

    @Override
    public void onNext(Integer t) {
        request(1);
    }

});
```

The [OnSubscribeFromIterable](https://github.com/ReactiveX/RxJava/blob/master/rxjava-core/src/main/java/rx/internal/operators/OnSubscribeFromIterable.java) operator shows how an `Iterable` is consumed with backpressure.

Some hi-lights (modified for simplicity rather than performance and completeness):

```java
public final class OnSubscribeFromIterable<T> implements OnSubscribe<T> {

    @Override
    public void call(final Subscriber<? super T> o) {
        final Iterator<? extends T> it = is.iterator();
		// instead of emitting directly to the Subscriber, it emits a Producer
        o.setProducer(new IterableProducer<T>(o, it));
    }
	
	private static final class IterableProducer<T> implements Producer {
	
        public void request(long n) {
            int _c = requested.getAndAdd(n);
            if (_c == 0) {
                while (it.hasNext()) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    T t = it.next();
                    o.onNext(t);
                    if (requested.decrementAndGet() == 0) {
                        // we're done emitting the number requested so return
                        return;
                    }
                }

                o.onCompleted();
            }

        }
	}
}
```

The [`observeOn` operator](https://github.com/ReactiveX/RxJava/blob/master/rxjava-core/src/main/java/rx/internal/operators/OperatorObserveOn.java#L85) is a sterotypical example of queuing on one side of a thread and draining on the other, now with backpressure.

```java
private static final class ObserveOnSubscriber<T> extends Subscriber<T> {
        @Override
        public void onStart() {
            // signal that this is an async operator capable of receiving this many
            request(RxRingBuffer.SIZE);
        }
		
        @Override
        public void onNext(final T t) {
            try {
				// enqueue
                queue.onNext(t);
            } catch (MissingBackpressureException e) {
				// fail if the upstream has not obeyed our backpressure requests
                onError(e);
                return;
            }
			// attempt to schedule draining if needed
            schedule();
        }
		
		// the scheduling polling will then drain the queue and invoke `request(n)` to request more after draining
}
```


#### Breaking Changes

The use of `Producer` has been added in such a way that it is optional and additive, but some operators that used to have unbounded queues are now bounded. This means that if a source `Observable` emits faster than the `Observer` can consume them, a `MissingBackpressureException` can be emitted via `onError`.

This semantic change can break existing code.

There are two ways of resolving this:

1) Modify the source `Observable` to use `Producer` and support backpressure.
2) Use newly added operators such as `onBackpressureBuffer` or `onBackpressureDrop` to choose a strategy for the source `Observable` of how to behave when it emits more data than the consuming `Observer` is capable of handling. Use of `onBackpressureBuffer` effectively returns it to having an unbounded buffer and behaving like version 0.19 or earlier.

Example:

```java
sourceObservable.onBackpressureBuffer().subscribe(slowConsumer);
```

#### Relation to Reactive Streams

Contributors to RxJava are involved in defining the [Reactive Streams](https://github.com/reactive-streams/reactive-streams/) spec. RxJava 1.0 is trying to comply with the semantic rules but is not attempting to comply with the type signatures. It will however have a separate module that acts as a bridge between the RxJava `Observable` and the Reactive Stream types.

The reasons for this are:

- Rx has `Observer.onCompleted` whereas Reactive Streams has `onComplete`. This is a massive breaking change to remove a "d".
- The RxJava `Subscription` is used also a "Closeable"/"Disposable" and it does not work well to make it now also be used for `request(n)`, hence the separate type `Producer` in RxJava. It was attempted to reuse `rx.Subscription` but it couldn't be done without massive breaking changes.
- Reactive Streams uses `onSubscribe(Subscription s)` whereas RxJava injects the `Subscription` as the `Subscriber`. Again, this change could not be done without major breaking changes.
- RxJava 1.0 needs to be backwards compatible with the major Rx contracts established during the 0.x roadmap.
- Reactive Streams is not yet 1.0 and despite significant progress, it is a moving target. 

Considering these things, the major semantics of `request(long n)` for backpressure are compatible and this will allow interop with a bridge between the interfaces. As the Reactive Streams spec matures, RxJava 2.0 may choose to fully adopt the types in the future while RxJava 1.x retains the current signatures.


#### How to Help

First, please test this release against your existing code to help us determine if we have broken anything. 

Second, try to solve backpressure use cases and provide feedback on what works and what doesn't work.

Thank you!


### Version 0.19.6 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.19.6%22)) ###

Inclusion of 'rxjava-contrib:rxjava-scalaz' in release.

### Version 0.19.5 ###

Upload to Maven Central was corrupted so release is skipped.

### Version 0.19.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.19.4%22)) ###

* [Pull 1401] (https://github.com/ReactiveX/RxJava/pull/1401) OnError while emitting onNext value: object.toString
* [Pull 1409] (https://github.com/ReactiveX/RxJava/pull/1409) Avoiding OperatorObserveOn from calling subscriber.onNext(..) after unsubscribe
* [Pull 1406] (https://github.com/ReactiveX/RxJava/pull/1406) Kotlin M8
* [Pull 1400] (https://github.com/ReactiveX/RxJava/pull/1400) Internal Data Structures
* [Pull 1399] (https://github.com/ReactiveX/RxJava/pull/1399) Update Perf Tests
* [Pull 1396] (https://github.com/ReactiveX/RxJava/pull/1396) RxScala: Fix the compiler warnings
* [Pull 1397] (https://github.com/ReactiveX/RxJava/pull/1397) Adding the hooks unsafeSubscribe

### Version 0.19.3 ###

Upload to Maven Central was corrupted so release is skipped.

### Version 0.19.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.19.2%22)) ###

* [Pull 1388] (https://github.com/ReactiveX/RxJava/pull/1388) CompositeException stops mutating nested Exceptions
* [Pull 1387] (https://github.com/ReactiveX/RxJava/pull/1387) Upgrade to JMH 0.9
* [Pull 1297] (https://github.com/ReactiveX/RxJava/pull/1297) [RxScala] rxjava-scalaz: providing some type class instances
* [Pull 1332] (https://github.com/ReactiveX/RxJava/pull/1332) IOSSchedulers for RoboVM
* [Pull 1380] (https://github.com/ReactiveX/RxJava/pull/1380) Variety of Fixes
* [Pull 1379] (https://github.com/ReactiveX/RxJava/pull/1379) Parallel Operator Rewrite
* [Pull 1378] (https://github.com/ReactiveX/RxJava/pull/1378) BugFix: Pivot Concurrency
* [Pull 1376] (https://github.com/ReactiveX/RxJava/pull/1376) Revision of JMH Tests
* [Pull 1375] (https://github.com/ReactiveX/RxJava/pull/1375) RxScala: Add idiomatic toXXX methods
* [Pull 1367] (https://github.com/ReactiveX/RxJava/pull/1367) Fix the bug that 'flatMap' swallows OnErrorNotImplementedException
* [Pull 1374] (https://github.com/ReactiveX/RxJava/pull/1374) Fix head/tail false sharing issues 
* [Pull 1369] (https://github.com/ReactiveX/RxJava/pull/1369) DebugHook got miswired before
* [Pull 1361] (https://github.com/ReactiveX/RxJava/pull/1361) Fix a race condition if queued actions have been handled already
* [Pull 1336] (https://github.com/ReactiveX/RxJava/pull/1336) RxScala: Add the rest missing methods to BlockingObservable
* [Pull 1362] (https://github.com/ReactiveX/RxJava/pull/1362) RxScala: Fix #1340 and #1343
* [Pull 1359] (https://github.com/ReactiveX/RxJava/pull/1359) Fixed padding of the integer and node classes

### Version 0.19.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.19.1%22)) ###

* [Pull 1357] (https://github.com/ReactiveX/RxJava/pull/1357) MergeWith, ConcatWith, AmbWith
* [Pull 1345] (https://github.com/ReactiveX/RxJava/pull/1345) RxScala: Simplify doOnCompleted/Terminate, finallyDo callback usage
* [Pull 1337] (https://github.com/ReactiveX/RxJava/pull/1337) Make Future receive NoSuchElementException when the BlockingObservable is empty
* [Pull 1335] (https://github.com/ReactiveX/RxJava/pull/1335) RxAndroid: Bump build tools to 19.1 and android plugin to 0.11
* [Pull 1327] (https://github.com/ReactiveX/RxJava/pull/1327) Join patterns extension for 4..9 and N arity joins.
* [Pull 1321] (https://github.com/ReactiveX/RxJava/pull/1321) RxAndroid: Ensuring Runnables posted with delay to a Handler are removed when unsubcribed
* [Pull 1347] (https://github.com/ReactiveX/RxJava/pull/1347) Allow use of the returned subscription to cancel periodic scheduling 
* [Pull 1355] (https://github.com/ReactiveX/RxJava/pull/1355) Don't add the subscriber to the manager if it unsubscribed during the onStart call
* [Pull 1350] (https://github.com/ReactiveX/RxJava/pull/1350) Baseline Performance Tests
* [Pull 1316] (https://github.com/ReactiveX/RxJava/pull/1316) RxScala: Add the rest operators
* [Pull 1324] (https://github.com/ReactiveX/RxJava/pull/1324) TrampolineScheduler & Unsubscribe
* [Pull 1311] (https://github.com/ReactiveX/RxJava/pull/1311) Tiny integration test change


### Version 0.19.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.19.0%22)) ###

#### Performance and Object Allocation

Fairly significant object allocation improvements are included in this release which reduce GC pressure and improve performance.

Two pull requests (amongst several) with details are:

- https://github.com/ReactiveX/RxJava/pull/1281 Reduce Subscription Object Allocation
- https://github.com/ReactiveX/RxJava/pull/1246 Moved to atomic field updaters

With the following simple test code relative performance has increased as shown below:

```java
Observable<Integer> o = Observable.just(1);
o.map(i -> {
    return String.valueOf(i);
}).map(i -> {
    return Integer.parseInt(i);
}).subscribe(observer);
```


###### Rx 0.19

```
Run: 10 - 10,692,099 ops/sec 
Run: 11 - 10,617,627 ops/sec 
Run: 12 - 10,938,405 ops/sec 
Run: 13 - 10,917,388 ops/sec 
Run: 14 - 10,783,298 ops/sec 
```

###### Rx 0.18.4

```
Run: 11 - 8,493,506 ops/sec 
Run: 12 - 8,403,361 ops/sec 
Run: 13 - 8,400,537 ops/sec 
Run: 14 - 8,163,998 ops/sec 
```

###### Rx 0.17.6

```
Run: 10 - 4,930,966 ops/sec 
Run: 11 - 6,119,951 ops/sec 
Run: 12 - 7,062,146 ops/sec 
Run: 13 - 6,514,657 ops/sec 
Run: 14 - 6,369,426 ops/sec 
```

###### Rx 0.16.1

```
Run: 10 - 2,879,355 ops/sec 
Run: 11 - 3,236,245 ops/sec 
Run: 12 - 4,468,275 ops/sec 
Run: 13 - 3,237,293 ops/sec 
Run: 14 - 4,683,840 ops/sec 
```

Note that these numbers are relative as they depend on the JVM and hardware. 


#### Scala Changes

Many missing operators have been added to the RxScala APIs along with fixes and other maturation.


#### toBlockingObservable() -> toBlocking()

The `toBlockingObservable()` method has been deprecated in favor of `toBlocking()` for brevity and fit better with possible future additions such as `toParallel()` without always needing the `Observable` suffix.


#### forEach

`forEach` as added as an alias for `subscribe` to match the Java 8 naming convention.

This means code can now be written as:

```java
Observable.from(1, 2, 3).limit(2).forEach(System.out::println);
```

which is an alias of this:

```java
Observable.from(1, 2, 3).take(2).subscribe(System.out::println);
```

Since `forEach` exists on `BlockingObservable` as well, moving from non-blocking to blocking looks like this:

```java
// non-blocking
Observable.from(1, 2, 3).limit(2).forEach(System.out::println);
// blocking
Observable.from(1, 2, 3).limit(2).toBlocking().forEach(System.out::println);
```


#### Schedulers

Thread caching is restored to `Schedulers.io()` after being lost in v0.18.

A replacement for `ExecutorScheduler` (removed in 0.18) is accessible via `Schedulers.from(Executor e)` that wraps an `Executor` and complies with the Rx contract. 


#### ReplaySubject

All "replay" functionality now exists directly on the `ReplaySubject` rather than in an internal type. This means there are now several different `create` methods with the various overloads of size and time. 


#### Changelist

* [Pull 1165](https://github.com/ReactiveX/RxJava/pull/1165) RxScala: Add dropUntil, contains, repeat, doOnTerminate, startWith, publish variants
* [Pull 1183](https://github.com/ReactiveX/RxJava/pull/1183) NotificationLite.accept performance improvements
* [Pull 1177](https://github.com/ReactiveX/RxJava/pull/1177) GroupByUntil to use BufferUntilSubscriber
* [Pull 1182](https://github.com/ReactiveX/RxJava/pull/1182) Add facilities for creating Observables from JavaFX events and ObservableValues
* [Pull 1188](https://github.com/ReactiveX/RxJava/pull/1188) RxScala Schedulers changes
* [Pull 1175](https://github.com/ReactiveX/RxJava/pull/1175) Fixed synchronous ConnectableObservable.connect problem
* [Pull 1172](https://github.com/ReactiveX/RxJava/pull/1172) ObserveOn: Change to batch dequeue
* [Pull 1191](https://github.com/ReactiveX/RxJava/pull/1191) Fix attempt for OperatorPivotTest
* [Pull 1195](https://github.com/ReactiveX/RxJava/pull/1195) SwingScheduler: allow negative schedule
* [Pull 1178](https://github.com/ReactiveX/RxJava/pull/1178) Fix RxScala bug
* [Pull 1210](https://github.com/ReactiveX/RxJava/pull/1210) Add more operators to RxScala
* [Pull 1216](https://github.com/ReactiveX/RxJava/pull/1216) RxScala: Exposing PublishSubject
* [Pull 1208](https://github.com/ReactiveX/RxJava/pull/1208) OperatorToObservableList: use LinkedList to buffer the sequence’s items
* [Pull 1185](https://github.com/ReactiveX/RxJava/pull/1185) Behavior subject time gap fix 2 
* [Pull 1226](https://github.com/ReactiveX/RxJava/pull/1226) Fix bug in `zipWithIndex` and set `zip(that, selector)` public in RxScala
* [Pull 1224](https://github.com/ReactiveX/RxJava/pull/1224) Implement shorter toBlocking as shorter alias for toBlockingObservable.
* [Pull 1223](https://github.com/ReactiveX/RxJava/pull/1223) ReplaySubject enhancement with time and/or size bounds
* [Pull 1160](https://github.com/ReactiveX/RxJava/pull/1160) Add `replay` and `multicast` variants to RxScala
* [Pull 1229](https://github.com/ReactiveX/RxJava/pull/1229) Remove Ambiguous Subscribe Overloads with Scheduler
* [Pull 1232](https://github.com/ReactiveX/RxJava/pull/1232) Adopt Limit and ForEach Java 8 Naming Conventions
* [Pull 1233](https://github.com/ReactiveX/RxJava/pull/1233) Deprecate toBlockingObservable in favor of toBlocking
* [Pull 1237](https://github.com/ReactiveX/RxJava/pull/1237) SafeSubscriber memory reduction
* [Pull 1236](https://github.com/ReactiveX/RxJava/pull/1236) CompositeSubscription with atomic field updater
* [Pull 1243](https://github.com/ReactiveX/RxJava/pull/1243) Remove Subscription Wrapper from Observable.subscribe
* [Pull 1244](https://github.com/ReactiveX/RxJava/pull/1244) Observable.from(T) using Observable.just(T)
* [Pull 1239](https://github.com/ReactiveX/RxJava/pull/1239) RxScala: Update docs for "apply" and add an example
* [Pull 1248](https://github.com/ReactiveX/RxJava/pull/1248) Fixed testConcurrentOnNextFailsValidation 
* [Pull 1246](https://github.com/ReactiveX/RxJava/pull/1246) Moved to atomic field updaters.
* [Pull 1254](https://github.com/ReactiveX/RxJava/pull/1254) ZipIterable unsubscription fix
* [Pull 1247](https://github.com/ReactiveX/RxJava/pull/1247) Add zip(iterable, selector) to RxScala
* [Pull 1260](https://github.com/ReactiveX/RxJava/pull/1260) Fix the bug that BlockingObservable.singleOrDefault doesn't call unsubscribe
* [Pull 1269](https://github.com/ReactiveX/RxJava/pull/1269) Fix the bug that int overflow can bypass the range check
* [Pull 1272](https://github.com/ReactiveX/RxJava/pull/1272) ExecutorScheduler to wrap an Executor
* [Pull 1264](https://github.com/ReactiveX/RxJava/pull/1264) ObserveOn scheduled unsubscription
* [Pull 1271](https://github.com/ReactiveX/RxJava/pull/1271) Operator Retry with predicate
* [Pull 1265](https://github.com/ReactiveX/RxJava/pull/1265) Add more operators to RxScala
* [Pull 1281](https://github.com/ReactiveX/RxJava/pull/1281) Reduce Subscription Object Allocation 
* [Pull 1284](https://github.com/ReactiveX/RxJava/pull/1284) Lock-free, MPSC-queue
* [Pull 1288](https://github.com/ReactiveX/RxJava/pull/1288) Ensure StringObservable.from() does not perform unnecessary read
* [Pull 1286](https://github.com/ReactiveX/RxJava/pull/1286) Rename some Operator* classes to OnSubscribe*
* [Pull 1276](https://github.com/ReactiveX/RxJava/pull/1276) CachedThreadScheduler
* [Pull 1287](https://github.com/ReactiveX/RxJava/pull/1287) ReplaySubject remove replayState CHM and related SubjectObserver changes
* [Pull 1289](https://github.com/ReactiveX/RxJava/pull/1289) Schedulers.from(Executor)
* [Pull 1290](https://github.com/ReactiveX/RxJava/pull/1290) Upgrade to JMH 0.7.3
* [Pull 1293](https://github.com/ReactiveX/RxJava/pull/1293) Fix and Update JMH Perf Tests 
* [Pull 1291](https://github.com/ReactiveX/RxJava/pull/1291) Check unsubscribe within observable from future
* [Pull 1294](https://github.com/ReactiveX/RxJava/pull/1294) rx.operators -> rx.internal.operators
* [Pull 1295](https://github.com/ReactiveX/RxJava/pull/1295) Change `void accept` to `boolean accept`
* [Pull 1296](https://github.com/ReactiveX/RxJava/pull/1296) Move re-used internal Scheduler classes to their own package
* [Pull 1298](https://github.com/ReactiveX/RxJava/pull/1298) Remove Bad Perf Test
* [Pull 1301](https://github.com/ReactiveX/RxJava/pull/1301) RxScala: Add convenience method for adding unsubscription callback
* [Pull 1304](https://github.com/ReactiveX/RxJava/pull/1304) Add flatMap and concatMap to RxScala
* [Pull 1306](https://github.com/ReactiveX/RxJava/pull/1306) Hooked RxJavaPlugins errorHandler up within all operators that swallow onErrors
* [Pull 1309](https://github.com/ReactiveX/RxJava/pull/1309) Hide ChainedSubscription/SubscriptionList from Public API

### Version 0.18.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.18.4%22)) ###

This is a fix for `CompositeSubscription` object allocation problems. Details can be found in issue [#1204](https://github.com/ReactiveX/RxJava/issues/1204).

* [Pull 1283](https://github.com/ReactiveX/RxJava/pull/1283) Subscription object allocation fix

### Version 0.18.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.18.3%22)) ###

* [Pull 1161] (https://github.com/ReactiveX/RxJava/pull/1161) Removed use of deprecated API from tests & operators
* [Pull 1162] (https://github.com/ReactiveX/RxJava/pull/1162) fix to remove drift from schedulePeriodic
* [Pull 1159] (https://github.com/ReactiveX/RxJava/pull/1159) Rxscala improvement
* [Pull 1164] (https://github.com/ReactiveX/RxJava/pull/1164) JMH Perf Tests for Schedulers.computation
* [Pull 1158] (https://github.com/ReactiveX/RxJava/pull/1158) Scheduler correctness improvements.

### Version 0.18.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.18.2%22)) ###


* [Pull 1150] (https://github.com/ReactiveX/RxJava/pull/1150) Fix ReplaySubject Terminal State Race Condition
* [Pull 1144] (https://github.com/ReactiveX/RxJava/pull/1144) Operator Delay rebase & fixes
* [Pull 1142] (https://github.com/ReactiveX/RxJava/pull/1142) Update 'contains' signature to 'contains(Object)'
* [Pull 1134] (https://github.com/ReactiveX/RxJava/pull/1134) OperatorTakeLast
* [Pull 1135] (https://github.com/ReactiveX/RxJava/pull/1135) OperatorTakeUntil
* [Pull 1137] (https://github.com/ReactiveX/RxJava/pull/1137) Random fixes to operators multicast, sample, refCount
* [Pull 1138] (https://github.com/ReactiveX/RxJava/pull/1138) Operator Window and other changes
* [Pull 1131] (https://github.com/ReactiveX/RxJava/pull/1131) Operator TakeTimed
* [Pull 1130] (https://github.com/ReactiveX/RxJava/pull/1130) Operator Switch
* [Pull 1129] (https://github.com/ReactiveX/RxJava/pull/1129) Conditional statements contribution to Operator
* [Pull 1128] (https://github.com/ReactiveX/RxJava/pull/1128) Fix for SerializedObserverTest
* [Pull 1126] (https://github.com/ReactiveX/RxJava/pull/1126) Operator When
* [Pull 1125] (https://github.com/ReactiveX/RxJava/pull/1125) Operator contrib math
* [Pull 1124] (https://github.com/ReactiveX/RxJava/pull/1124) Add lift to rxscala
* [Pull 1122] (https://github.com/ReactiveX/RxJava/pull/1122) OperatorSkipUntil
* [Pull 1121] (https://github.com/ReactiveX/RxJava/pull/1121) OperatorSkipTimed
* [Pull 1120] (https://github.com/ReactiveX/RxJava/pull/1120) OperatorSequenceEqual
* [Pull 1119] (https://github.com/ReactiveX/RxJava/pull/1119) OperatorRefCount
* [Pull 1118] (https://github.com/ReactiveX/RxJava/pull/1118) Operator ParallelMerge
* [Pull 1117] (https://github.com/ReactiveX/RxJava/pull/1117) Operator OnExceptionResumeNextViaObservable
* [Pull 1115] (https://github.com/ReactiveX/RxJava/pull/1115) OperatorTakeWhile
* [Pull 1112] (https://github.com/ReactiveX/RxJava/pull/1112) OperatorThrottleFirst
* [Pull 1111] (https://github.com/ReactiveX/RxJava/pull/1111) OperatorTimeInterval
* [Pull 1110] (https://github.com/ReactiveX/RxJava/pull/1110) OperatorOnErrorReturn
* [Pull 1109] (https://github.com/ReactiveX/RxJava/pull/1109) OperatorOnErrorResumeNextViaObservable
* [Pull 1108] (https://github.com/ReactiveX/RxJava/pull/1108) OperatorMulticastAndReplay
* [Pull 1107] (https://github.com/ReactiveX/RxJava/pull/1107) Fix ReplaySubject's double termination problem.
* [Pull 1106] (https://github.com/ReactiveX/RxJava/pull/1106) OperatorMergeMaxConcurrent
* [Pull 1104] (https://github.com/ReactiveX/RxJava/pull/1104) Operator merge delay error
* [Pull 1103] (https://github.com/ReactiveX/RxJava/pull/1103) OperatorJoin
* [Pull 1101] (https://github.com/ReactiveX/RxJava/pull/1101) Operator async
* [Pull 1100] (https://github.com/ReactiveX/RxJava/pull/1100) OperatorUsing
* [Pull 1099] (https://github.com/ReactiveX/RxJava/pull/1099) OperatorToMap
* [Pull 1098] (https://github.com/ReactiveX/RxJava/pull/1098) OperatorTimerAndSample
* [Pull 1097] (https://github.com/ReactiveX/RxJava/pull/1097) OperatorToMultimap
* [Pull 1096] (https://github.com/ReactiveX/RxJava/pull/1096) OperatorGroupJoin
* [Pull 1095] (https://github.com/ReactiveX/RxJava/pull/1095) OperatorGroupByUntil
* [Pull 1094] (https://github.com/ReactiveX/RxJava/pull/1094) Operator debounce


### Version 0.18.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.18.1%22)) ###

* [Pull 1065] (https://github.com/ReactiveX/RxJava/pull/1065) Optimize OperatorSkipLastTimed
* [Pull 1073] (https://github.com/ReactiveX/RxJava/pull/1073) OperatorBuffer
* [Pull 1074] (https://github.com/ReactiveX/RxJava/pull/1074) OperatorConcat
* [Pull 1088] (https://github.com/ReactiveX/RxJava/pull/1088) OperatorToObservableFuture
* [Pull 1087] (https://github.com/ReactiveX/RxJava/pull/1087) OperatorMergeMap
* [Pull 1086] (https://github.com/ReactiveX/RxJava/pull/1086) OperatorFinallyDo
* [Pull 1085] (https://github.com/ReactiveX/RxJava/pull/1085) OperatorDistinctUntilChanged
* [Pull 1084] (https://github.com/ReactiveX/RxJava/pull/1084) OperatorDistinct
* [Pull 1083] (https://github.com/ReactiveX/RxJava/pull/1083) OperatorDematerialize
* [Pull 1081] (https://github.com/ReactiveX/RxJava/pull/1081) OperatorDefer
* [Pull 1080] (https://github.com/ReactiveX/RxJava/pull/1080) OperatorDefaultIfEmpty
* [Pull 1079] (https://github.com/ReactiveX/RxJava/pull/1079) OperatorCombineLatest
* [Pull 1074] (https://github.com/ReactiveX/RxJava/pull/1074) OperatorConcat
* [Pull 1073] (https://github.com/ReactiveX/RxJava/pull/1073) OperatorBuffer
* [Pull 1091] (https://github.com/ReactiveX/RxJava/pull/1091) Handle Thrown Errors with UnsafeSubscribe
* [Pull 1092] (https://github.com/ReactiveX/RxJava/pull/1092) Restore ObservableExecutionHook.onCreate

### Version 0.18.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.18.0%22)) ###

This release takes us a step closer to [1.0](https://github.com/ReactiveX/RxJava/issues/1001) by completing some of the remaining work on the roadmap.

##### Scheduler

The first is simplifying the [Scheduler API](https://github.com/ReactiveX/RxJava/issues/997). 

The Scheduler API is now simplified to this:

```java
class Scheduler {
    public abstract Worker createWorker(); 
    public int parallelism();
    public long now();

    public abstract static class Worker implements Subscription {
        public abstract Subscription schedule(Action0 action, long delayTime, TimeUnit unit);
        public abstract Subscription schedule(Action0 action);
        public Subscription schedulePeriodically(Action0 action, long initialDelay, long period, TimeUnit unit);
        public long now();
    }
}
```

This is a breaking change if you have a custom `Scheduler` implementation or use a `Scheduler` directly. If you only ever pass in a `Scheduler` via the `Schedulers` factory methods, this change does not affect you.

Additionally, the `ExecutionScheduler` was removed because a general threadpool does not meet the requirements of sequential execution for an `Observable`. It was replaced with `rx.schedulers.EventLoopScheduler` which is the new default for `Schedulers.computation()`. It is a pool of event loops.

##### rx.joins

The `rx.joins` package and associated `when`, `and` and `then` operators were moved out of rxjava-core into a new module rxjava-joins. This is done as the rx.joins API was not yet matured and is not going to happen before 1.0. It was determined low priority and not worth blocking a 1.0 release. If the API matures inside the separate module to the point where it makes sense to bring it back into the core it can be done in the 1.x series.

##### Deprecation Cleanup

This releases removes many of the classes and methods that have been deprecated in previous releases. Most of the removed functionality was migrated in previous releases to contrib modules such as rxjava-math, rxjava-async and rxjava-computation-expressions.

A handful of deprecated items still remain but can not yet be removed until all internal operators are finished migrating to using the `lift`/`Subscriber` design changes done in 0.17.0.


The full list of changes in 0.18.0:

* [Pull 1047] (https://github.com/ReactiveX/RxJava/pull/1047) Scheduler Simplification
* [Pull 1072] (https://github.com/ReactiveX/RxJava/pull/1072) Scheduler.Inner -> Scheduler.Worker
* [Pull 1053] (https://github.com/ReactiveX/RxJava/pull/1053) Deprecation Cleanup
* [Pull 1052] (https://github.com/ReactiveX/RxJava/pull/1052) Scheduler Cleanup
* [Pull 1048] (https://github.com/ReactiveX/RxJava/pull/1048) Remove ExecutorScheduler - New ComputationScheduler
* [Pull 1049] (https://github.com/ReactiveX/RxJava/pull/1049) Move rx.joins to rxjava-joins module
* [Pull 1068] (https://github.com/ReactiveX/RxJava/pull/1068) add synchronous test of resubscribe after error
* [Pull 1066] (https://github.com/ReactiveX/RxJava/pull/1066) CompositeSubscription fix
* [Pull 1071] (https://github.com/ReactiveX/RxJava/pull/1071) Manual Merge of AsObservable
* [Pull 1063] (https://github.com/ReactiveX/RxJava/pull/1063) Fix bugs in equals and hashCode of Timestamped
* [Pull 1070] (https://github.com/ReactiveX/RxJava/pull/1070) OperationAny -> OperatorAny
* [Pull 1069] (https://github.com/ReactiveX/RxJava/pull/1069) OperationAll -> OperatorAll
* [Pull 1058] (https://github.com/ReactiveX/RxJava/pull/1058) Typo in javadoc
* [Pull 1056] (https://github.com/ReactiveX/RxJava/pull/1056) Add drop(skip) and dropRight(skipLast) to rxscala
* [Pull 1057] (https://github.com/ReactiveX/RxJava/pull/1057) Fix: Retry in Scala adaptor is ambiguous
* [Pull 1055] (https://github.com/ReactiveX/RxJava/pull/1055) Fix: Missing Quasar instrumentation on Observable$2.call
* [Pull 1050] (https://github.com/ReactiveX/RxJava/pull/1050) Reimplement the 'SkipLast' operator 
* [Pull 967] (https://github.com/ReactiveX/RxJava/pull/967) Reimplement the 'single' operator



### Version 0.17.6 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.17.6%22)) ###

* [Pull 1031] (https://github.com/ReactiveX/RxJava/pull/1031) Fix NPE in SubjectSubscriptionManager
* [Pull 1030] (https://github.com/ReactiveX/RxJava/pull/1030) Benchmarking: Add JMH benchmark for ReplaySubject
* [Pull 1033] (https://github.com/ReactiveX/RxJava/pull/1033) isolate subscriber used for retries, cleanup tests
* [Pull 1021] (https://github.com/ReactiveX/RxJava/pull/1021) OperatorWeakBinding to not use WeakReferences anymore
* [Pull 1005] (https://github.com/ReactiveX/RxJava/pull/1005) add toMap from Java Observable
* [Pull 1040] (https://github.com/ReactiveX/RxJava/pull/1040) Fixed deadlock in Subjects + OperatorCache
* [Pull 1042] (https://github.com/ReactiveX/RxJava/pull/1042) Kotlin M7 and full compatibility with 0.17.0
* [Pull 1035] (https://github.com/ReactiveX/RxJava/pull/1035) Scala cleanup
* [Pull 1009] (https://github.com/ReactiveX/RxJava/pull/1009) Android - Adding a new RetainedFragment example
* [Pull 1020] (https://github.com/ReactiveX/RxJava/pull/1020) Upgrade Gradle wrapper for Android samples to Gradle 1.11
* [Pull 1038] (https://github.com/ReactiveX/RxJava/pull/1038) rxjava-android: parameterize OperatorViewClick by concrete view type

### Version 0.17.5 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.17.5%22)) ###

* [Pull 1010] (https://github.com/ReactiveX/RxJava/pull/1010) Observable.unsafeSubscribe
* [Pull 1015] (https://github.com/ReactiveX/RxJava/pull/1015) Remove Redundant protectivelyWrap Method
* [Pull 1019] (https://github.com/ReactiveX/RxJava/pull/1019) Fix: retry() never unsubscribes from source until operator completes 

### Version 0.17.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.17.4%22)) ###

* [Pull 990] (https://github.com/ReactiveX/RxJava/pull/990) Quasar Lightweight Threads/Fibers Contrib Module
* [Pull 1012] (https://github.com/ReactiveX/RxJava/pull/1012) SerializedObserver: Removed window between the two synchronized blocks


### Version 0.17.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.17.3%22)) ###

* [Pull 991] (https://github.com/ReactiveX/RxJava/pull/991) JMH Benchmark Build Config
* [Pull 993] (https://github.com/ReactiveX/RxJava/pull/993) JMH Perf Tests
* [Pull 995] (https://github.com/ReactiveX/RxJava/pull/995) Support Custom JMH Args
* [Pull 996] (https://github.com/ReactiveX/RxJava/pull/996) JMH Perfshadowjar
* [Pull 1003] (https://github.com/ReactiveX/RxJava/pull/1003) Func0 can transparently implement java.util.concurrent.Callable
* [Pull 999] (https://github.com/ReactiveX/RxJava/pull/999) New Implementation of SerializedObserver

### Version 0.17.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.17.2%22)) ###

* [Pull 963](https://github.com/ReactiveX/RxJava/pull/963) A more robust JMH benchmarking set-up 
* [Pull 964](https://github.com/ReactiveX/RxJava/pull/964) SubjectSubscriptionManager fix.
* [Pull 970](https://github.com/ReactiveX/RxJava/pull/970) Notifications for the allocation averse.
* [Pull 973](https://github.com/ReactiveX/RxJava/pull/973) Merge - Handle Bad Observables
* [Pull 974](https://github.com/ReactiveX/RxJava/pull/974) TestSubject, TestObserver and TestScheduler Improvements
* [Pull 975](https://github.com/ReactiveX/RxJava/pull/975) GroupBy & Time Gap Fixes
* [Pull 976](https://github.com/ReactiveX/RxJava/pull/976) parallel-merge unit test assertions
* [Pull 977](https://github.com/ReactiveX/RxJava/pull/977) Dematerialize - handle non-materialized terminal events
* [Pull 982](https://github.com/ReactiveX/RxJava/pull/982) Pivot Operator
* [Pull 984](https://github.com/ReactiveX/RxJava/pull/984) Tests and Javadoc for Pivot
* [Pull 966](https://github.com/ReactiveX/RxJava/pull/966) Reimplement the ElementAt operator and add it to rxjava-scala
* [Pull 965](https://github.com/ReactiveX/RxJava/pull/965) BugFix: Chain Subscription in TimeoutSubscriber and SerializedSubscriber
* [Pull 986](https://github.com/ReactiveX/RxJava/pull/986) Fix SynchronizedObserver.runConcurrencyTest
* [Pull 987](https://github.com/ReactiveX/RxJava/pull/987) Fix Non-Deterministic Pivot Test
* [Pull 988](https://github.com/ReactiveX/RxJava/pull/988) OnErrorFailedException


### Version 0.17.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.17.1%22)) ###

* [Pull 953](https://github.com/ReactiveX/RxJava/pull/953) Make ObserveOnTest.testNonBlockingOuterWhileBlockingOnNext deterministic
* [Pull 930](https://github.com/ReactiveX/RxJava/pull/930) Initial commit of the Android samples module
* [Pull 938](https://github.com/ReactiveX/RxJava/pull/938) OperatorWeakBinding (deprecates OperatorObserveFromAndroidComponent)
* [Pull 952](https://github.com/ReactiveX/RxJava/pull/952) rxjava-scala improvements and reimplemented the `amb` operator
* [Pull 955](https://github.com/ReactiveX/RxJava/pull/955) Fixed ReplaySubject leak
* [Pull 956](https://github.com/ReactiveX/RxJava/pull/956) Fixed byLine test to use line.separator system property instead of \n.
* [Pull 958](https://github.com/ReactiveX/RxJava/pull/958) OperatorSkipWhile
* [Pull 959](https://github.com/ReactiveX/RxJava/pull/959) OperationToFuture must throw CancellationException on get() if cancelled
* [Pull 928](https://github.com/ReactiveX/RxJava/pull/928) Fix deadlock in SubscribeOnBounded
* [Pull 960](https://github.com/ReactiveX/RxJava/pull/960) Unit test for "Cannot subscribe to a Retry observable once all subscribers unsubscribed"
* [Pull 962](https://github.com/ReactiveX/RxJava/pull/962) Migrate from SynchronizedObserver to SerializedObserver


### Version 0.17.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.17.0%22)) ###


Version 0.17.0 contains some significant signature changes that allow us to significantly improve handling of synchronous Observables and simplify Schedulers. Many of the changes have backwards compatible deprecated methods to ease the migration while some are breaking. 

The new signatures related to `Observable` in this release are:

```java
// A new create method takes `OnSubscribe` instead of `OnSubscribeFunc`
public final static <T> Observable<T> create(OnSubscribe<T> f)

// The new OnSubscribe type accepts a Subscriber instead of Observer and does not return a Subscription
public static interface OnSubscribe<T> extends Action1<Subscriber<? super T>>

// Subscriber is an Observer + Subscription
public abstract class Subscriber<T> implements Observer<T>, Subscription

// The main `subscribe` behavior receives a Subscriber instead of Observer
public final Subscription subscribe(Subscriber<? super T> subscriber)

// Subscribing with an Observer however is still appropriate
// and the Observer is automatically converted into a Subscriber
public final Subscription subscribe(Observer<? super T> observer)

// A new 'lift' function allows composing Operator implementations together
public <R> Observable<R> lift(final Operator<? extends R, ? super T> lift)
	
// The `Operator` used with `lift`
public interface Operator<R, T> extends Func1<Subscriber<? super R>, Subscriber<? super T>>

```

Also changed is the `Scheduler` interface which is much simpler:

```java
public abstract class Scheduler {
	public Subscription schedule(Action1<Scheduler.Inner> action);
    public Subscription schedule(Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit);
	public Subscription schedulePeriodically(Action1<Scheduler.Inner> action, long initialDelay, long period, TimeUnit unit);
	public final Subscription scheduleRecursive(final Action1<Recurse> action)
	public long now();
	public int degreeOfParallelism();
	
	public static class Inner implements Subscription {
		public abstract void schedule(Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit);
		public abstract void schedule(Action1<Scheduler.Inner> action);
		public long now();
	}
	
	public static final class Recurse {
		public final void schedule();
		public final void schedule(long delay, TimeUnit unit);
	}
}
```


This release applies many lessons learned over the past year and seeks to streamline the API before we hit 1.0.

As shown in the code above the changes fall into 2 major sections:

##### 1) Lift/Operator/OnSubscribe/Subscriber

Changes that allow unsubscribing from synchronous Observables without needing to add concurrency.

##### 2) Schedulers

Simplification of the `Scheduler` interface and make clearer the concept of "outer" and "inner" Schedulers for recursion.


#### Lift/Operator/OnSubscribe/Subscriber

New types `Subscriber` and `OnSubscribe` along with the new `lift` function have been added. The reasons and benefits are as follows:

##### 1) Synchronous Unsubscribe

RxJava versions up until 0.16.x are unable to unsubscribe from a synchronous Observable such as this:

```java
Observable<Integer> oi = Observable.create(new OnSubscribe<Integer>() {

    @Override
    public void call(Observer<? super Integer> Observer) {
        for (int i = 1; i < 1000000; i++) {
            subscriber.onNext(i);
        }
        subscriber.onCompleted();
    }
});
```

Subscribing to this `Observable` will always emit all 1,000,000 values even if unsubscribed such as via `oi.take(10)`.

Version 0.17.0 fixes this issue by injecting the `Subscription` into the `OnSubscribe` function to allow code like this:

```java
Observable<Integer> oi = Observable.create(new OnSubscribe<Integer>() {

    @Override
    public void call(Subscriber<? super Integer> subscriber) {
        // we now receive a Subscriber instead of Observer
        for (int i = 1; i < 1000000; i++) {
            // the OnSubscribe can now check for isUnsubscribed
            if (subscriber.isUnsubscribed()) {
                return;
            }
            subscriber.onNext(i);
        }
        subscriber.onCompleted();
    }

});
```

Subscribing to this will now correctly only emit 10 `onNext` and unsubscribe:

```java
// subscribe with an Observer
oi.take(10).subscribe(new Observer<Integer>() {

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(Integer t) {
        println("Received: " + t);
    }

})
```

Or the new `Subscriber` type can be used and the `Subscriber` itself can `unsubscribe`:

```java
// or subscribe with a Subscriber which supports unsubscribe
oi.subscribe(new Subscriber<Integer>() {

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(Integer t) {
        println("Received: " + t);
        if(t >= 10) {
            // a Subscriber can unsubscribe
            this.unsubscribe();
        }
    }

})
```


##### 2) Custom Operator Chaining

Because Java doesn't support extension methods, the only approach to applying custom operators without getting them added to `rx.Observable` is using static methods. This has meant code like this:

```java
MyCustomerOperators.operate(observable.map(...).filter(...).take(5)).map(...).subscribe()
```

In reality we want:

```java
observable.map(...).filter(...).take(5).myCustomOperator().map(...).subscribe()
```

Using the newly added `lift` we can get quite close to this:


```java
observable.map(...).filter(...).take(5).lift(MyCustomOperator.operate()).map(...).subscribe()
```

Here is how the proposed `lift` method looks if all operators were applied with it:

```java
Observable<String> os = OBSERVABLE_OF_INTEGERS.lift(TAKE_5).lift(MAP_INTEGER_TO_STRING);
```

Along with the `lift` function comes a new `Operator` signature:

```java
public interface Operator<R, T> extends Func1<Subscriber<? super R>, Subscriber<? super T>>
```

All operator implementations in the `rx.operators` package will over time be migrated to this new signature. 

NOTE: Operators that have not yet been migrated do not work with synchronous unsubscribe. 


##### 3) Simpler Operator Implementations

The `lift` operator injects the necessary `Observer` and `Subscription` instances (via the new `Subscriber` type) and eliminates (for most use cases) the need for manual subscription management. Because the `Subscription` is available in-scope there are no awkward coding patterns needed for creating a `Subscription`, closing over it and returning and taking into account synchronous vs asynchronous.

For example, the body of `fromIterable` is simply:

```java
public void call(Subscriber<? super T> o) {
    for (T i : is) {
        if (o.isUnsubscribed()) {
            return;
        }
        o.onNext(i);
    }
    o.onCompleted();
}
```

The `take` operator is:

```java
public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final CompositeSubscription parent = new CompositeSubscription();
        if (limit == 0) {
            child.onCompleted();
            parent.unsubscribe();
        }

        child.add(parent);
        return new Subscriber<T>(parent) {

            int count = 0;
            boolean completed = false;

            @Override
            public void onCompleted() {
                if (!completed) {
                    child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!completed) {
                    child.onError(e);
                }
            }

            @Override
            public void onNext(T i) {
                if (!isUnsubscribed()) {
                    child.onNext(i);
                    if (++count >= limit) {
                        completed = true;
                        child.onCompleted();
                        unsubscribe();
                    }
                }
            }

        };
    }
```


##### 4) Recursion/Loop Performance with Unsubscribe

The `fromIterable` use case is 20x faster when implemented as a loop instead of recursive scheduler (see https://github.com/ReactiveX/RxJava/commit/a18b8c1a572b7b9509b7a7fe1a5075ce93657771).

Several places we can remove recursive scheduling used originally for unsubscribe support and use a loop instead.





#### Schedulers


Schedulers were greatly simplified to a design based around `Action1<Inner>`. 

```java
public abstract class Scheduler {
	public Subscription schedule(Action1<Scheduler.Inner> action);
    public Subscription schedule(Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit);
	public Subscription schedulePeriodically(Action1<Scheduler.Inner> action, long initialDelay, long period, TimeUnit unit);
	public final Subscription scheduleRecursive(final Action1<Recurse> action)
	public long now();
	public int degreeOfParallelism();
	
	public static class Inner implements Subscription {
		public abstract void schedule(Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit);
		public abstract void schedule(Action1<Scheduler.Inner> action);
		public long now();
	}
	
	public static final class Recurse {
		public final void schedule();
		public final void schedule(long delay, TimeUnit unit);
	}
}
```

This design change originated from three findings:

1) It was very easy to cause memory leaks or inadvertent parallel execution since the distinction between outer and inner scheduling was not obvious.

To solve this the new design explicitly has the outer `Scheduler` and then `Scheduler.Inner` for recursion.

2) The passing of state is not useful since scheduling over network boundaries with this model does not work.

In this new design all state passing signatures have been removed. This was determined while implementing a `RemoteScheduler` that attempted to use `observeOn` to transition execution from one machine to another. This does not work because of the requirement for serializing/deserializing the state of the entire execution stack. Migration of work over the network has been bound to be better suited to explicit boundaries established by Subjects. Thus, the complications within the Schedulers are unnecessary.

3) The number of overloads with different ways of doing the same things were confusing.

This new design removes all but the essential and simplest methods.

4) A scheduled task could not do work in a loop and easily be unsubscribed which generally meant less efficient recursive scheduling. 

This new design applies similar principles as done with `lift`/`create`/`OnSubscribe`/`Subscriber` and injects the `Subscription` via the `Inner` interface so a running task can check `isUnsubscribed()`.



WIth this new design, the simplest execution of a single task is:

```java
Schedulers.newThread().schedule(new Action1<Inner>() {

    @Override
    public void call(Inner inner) {
        doWork();
    }

});
```

Recursion is easily invoked like this:

```java
Schedulers.newThread().scheduleRecursive(new Action1<Recurse>() {

    @Override
    public void call(Recurse recurse) {
        doWork();
        // recurse until unsubscribed (the schedule will do nothing if unsubscribed)
        recurse.schedule();
    }


});
```

or like this if the outer and inner actions need different behavior:

```java
Schedulers.newThread().schedule(new Action1<Inner>() {

    @Override
    public void call(Inner inner) {
        doWork();
        // recurse until unsubscribed (the schedule will do nothing if unsubscribed)
        inner.schedule(this);
    }

});
```

The use of `Action1<Inner>` on both the outer and inner levels makes it so recursion that refer to `this` and it works easily. 


Similar to the new `lift`/`create` pattern with `Subscriber` the `Inner` is also a `Subscription` so it allows efficient loops with `unsubscribe` support:

```java
Schedulers.newThread().schedule(new Action1<Inner>() {

    @Override
    public void call(Inner inner) {
        while(!inner.isUnsubscribed()) {
            doWork();
        }
    }

});
```

An action can now `unsubscribe`	the `Scheduler.Inner`:

```java
Schedulers.newThread().schedule(new Action1<Inner>() {

    @Override
    public void call(Inner inner) {
        while(!inner.isUnsubscribed()) {
            int i = doOtherWork();
            if(i > 100) {
                // an Action can cause the Scheduler to unsubscribe and stop
                inner.unsubscribe();
            }
        }
    }

});
```

Typically just stopping is sufficient:

```java
Schedulers.newThread().schedule(new Action1<Inner>() {

    @Override
    public void call(Inner inner) {
        int i = doOtherWork();
        if (i < 10) {
            // recurse until done 10
            inner.schedule(this);
        }
    }

});
```

but if other work in other tasks is being done and you want to unsubscribe conditionally you could:

```java
Schedulers.newThread().schedule(new Action1<Inner>() {

    @Override
    public void call(Inner inner) {
        int i = doOtherWork();
        if (i < 10) {
            // recurse until done 10
            inner.schedule(this);
        } else {
            inner.unsubscribe();
        }
    }

});
```

and the recursion can be delayed:

```java
Schedulers.newThread().schedule(new Action1<Inner>() {

    @Override
    public void call(Inner inner) {
        doWork();
        // recurse until unsubscribed ... but delay the recursion
        inner.schedule(this, 500, TimeUnit.MILLISECONDS);
    }

});
```

The same pattern works with the `Recurse` signature:

```java
Schedulers.newThread().scheduleRecursive(new Action1<Recurse>() {

    @Override
    public void call(Recurse recurse) {
        doWork();
        // recurse until unsubscribed (the schedule will do nothing if unsubscribed)
        recurse.schedule(500, TimeUnit.MILLISECONDS);
    }


});
```

The methods on the `Inner` never return a `Subscription` because they are always a single thread/event-loop/actor/etc and controlled by the `Subscription` returned by the initial `Scheduler.schedule` method. This is part of clarifying the contract.

Thus an `unsubscribe` controlled from the outside would be done like this:

```java
Subscription s = Schedulers.newThread().schedule(new Action1<Inner>() {

    @Override
    public void call(Inner inner) {
        while(!inner.isUnsubscribed()) {
            doWork();
        }
    }

});

// unsubscribe from outside
s.unsubscribe();
```



#### Migration Path

##### 1) Lift/OnSubscribe/Subscriber

The `lift` function will not be used by most and is additive so will not affect backwards compatibility. The `Subscriber` type is also additive and for most use cases does not need to be used directly, the `Observer` interface can continue being used.

The previous `create(OnSubscribeFunc f)` signature has been deprecated so code will work but now have warnings. Please begin migrating code as this will be deleted prior to the 1.0 release.

Code such as this:

```java
Observable.create(new OnSubscribeFunc<Integer>() {

    @Override
    public Subscription onSubscribe(Observer<? super Integer> o) {
        o.onNext(1);
        o.onCompleted();
        return Subscriptions.empty();
    }
});
```

should change to this:

```java
Observable.create(new OnSubscribe<Integer>() {

    @Override
    public void call(Subscriber<? super Integer> subscriber) {
        subscriber.onNext(1);
        subscriber.onCompleted();
    }
});
```

If concurrency was being injected to allow unsubscribe support:

```java
Observable.create(new OnSubscribeFunc<Integer>() {

    @Override
    public Subscription onSubscribe(final Observer<? super Integer> o) {
        final BooleanSubscription s = new BooleanSubscription();
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                int i = 0;
                while (!s.isUnsubscribed()) {
                    o.onNext(i++);
                }
            }

        });
        t.start();
        return s;
    }
});
```

you may no longer need it and can implement like this instead:

```java
Observable.create(new OnSubscribe<Integer>() {

    @Override
    public void call(Subscriber<? super Integer> subscriber) {
        int i = 0;
        while (!subscriber.isUnsubscribed()) {
            subscriber.onNext(i++);
        }
    }
});
```

or if the concurreny is still desired you can simplify the `Subscription` management:

```java
Observable.create(new OnSubscribe<Integer>() {

    @Override
    public void call(final Subscriber<? super Integer> subscriber) {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                int i = 0;
                while (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(i++);
                }
            }

        });
        t.start();
    }
});
```

or use `subscribeOn` which now works to make synchronous `Observables` async while supporting `unsubscribe` (this didn't work before):

```java
Observable.create(new OnSubscribe<Integer>() {

    @Override
    public void call(Subscriber<? super Integer> subscriber) {
        int i = 0;
        while (!subscriber.isUnsubscribed()) {
            subscriber.onNext(i++);
        }
    }
}).subscribeOn(Schedulers.newThread());
```



##### 2) Schedulers

Custom `Scheduler` implementations will need to be re-implemented and any direct use of the `Scheduler` interface will also need to be updated.


##### 3) Subscription

If you have custom `Subscription` implementations you will see they now need an `isUnsubscribed()` method.

You can either add this method, or wrap your function using `Subscriptions.create` and it will handle the `isUnsubscribed` behavior and execute your function when `unsubscribe()` is called.

It is recommended to use `Subscriptions.create` for most `Subscription` usage.



#### The Future...

We have most if not all operators from Rx.Net that we want or intend to port. We think we have got the `create`/`subscribe` signatures as we want and the `Subscription` and `Scheduler` interfaces are now clean. There is at least one more major topic related to back pressure that may result in signature change in a future release. Beyond that no further major signature changing work is expected prior to 1.0.

We still need to improve on some of the `Subject` implementations still, particularly `ReplaySubject`. We are beginning to focus after this release on cleaning up all of the operator implementations, stabilizing, fixing bugs and performance tuning.

As we get closer to 1.0 there will be a release that focused on deleting all deprecated methods so it is suggested to start migrating off of them.

We appreciate your usage, feedback and contributions and hope the library is creating value for you!


#### Pull Requests


* [Pull 767](https://github.com/ReactiveX/RxJava/pull/767) Zip fix for multiple onCompleted and moved unsubscribe outside the lock.
* [Pull 770](https://github.com/ReactiveX/RxJava/pull/770) Bind Operator
* [Pull 778](https://github.com/ReactiveX/RxJava/pull/778) Fix zip race condition 
* [Pull 784](https://github.com/ReactiveX/RxJava/pull/784) Lift and Observer+Subscription
* [Pull 793](https://github.com/ReactiveX/RxJava/pull/793) Observer + Subscriber
* [Pull 796](https://github.com/ReactiveX/RxJava/pull/796) Add Subscription.isUnsubscribed()
* [Pull 797](https://github.com/ReactiveX/RxJava/pull/797) Scheduler Outer/Inner [Preview]
* [Pull 805](https://github.com/ReactiveX/RxJava/pull/805) Fix CompositeException
* [Pull 785](https://github.com/ReactiveX/RxJava/pull/785) Reimplement Zip Operator Using Lift [Preview]
* [Pull 814](https://github.com/ReactiveX/RxJava/pull/814) RunAsync method for outputting multiple values
* [Pull 812](https://github.com/ReactiveX/RxJava/pull/812) Fixed OperationSubscribeOn so OperationConditionalsTest works again.
* [Pull 816](https://github.com/ReactiveX/RxJava/pull/816) One global onCompleted object
* [Pull 818](https://github.com/ReactiveX/RxJava/pull/818) CompositeSubscription memory reduction
* [Pull 817](https://github.com/ReactiveX/RxJava/pull/817) Scala Scheduler Bindings Fix
* [Pull 819](https://github.com/ReactiveX/RxJava/pull/819) CompositeSubscription performance increase
* [Pull 781](https://github.com/ReactiveX/RxJava/pull/781) Fixed buglet in join binding, simplified types
* [Pull 783](https://github.com/ReactiveX/RxJava/pull/783) Implement some Android UI related operators
* [Pull 821](https://github.com/ReactiveX/RxJava/pull/821) Update to use Subscriber/Subscriptions.create
* [Pull 826](https://github.com/ReactiveX/RxJava/pull/826) Return wrapped Subscription
* [Pull 824](https://github.com/ReactiveX/RxJava/pull/824) Set setDaemon on NewThreadScheduler
* [Pull 828](https://github.com/ReactiveX/RxJava/pull/828) Repeat Operator
* [Pull 827](https://github.com/ReactiveX/RxJava/pull/827) Fixed cut & paster error in io scheduler
* [Pull 833](https://github.com/ReactiveX/RxJava/pull/833) Take operator was breaking the unsubscribe chain
* [Pull 822](https://github.com/ReactiveX/RxJava/pull/822) Reimplement 'subscribeOn' using 'lift'
* [Pull 832](https://github.com/ReactiveX/RxJava/pull/832) Issue #831 Fix for OperationJoin race condition
* [Pull 834](https://github.com/ReactiveX/RxJava/pull/834) Update clojure for 0.17
* [Pull 839](https://github.com/ReactiveX/RxJava/pull/839) Error Handling: OnErrorNotImplemented and java.lang.Error
* [Pull 838](https://github.com/ReactiveX/RxJava/pull/838) Make Scala OnCompleted Notification an object
* [Pull 837](https://github.com/ReactiveX/RxJava/pull/837) Perf with JMH
* [Pull 841](https://github.com/ReactiveX/RxJava/pull/841) Range OnSubscribe
* [Pull 842](https://github.com/ReactiveX/RxJava/pull/842) Test Unsubscribe
* [Pull 845](https://github.com/ReactiveX/RxJava/pull/845) Fix problem with Subscription 
* [Pull 847](https://github.com/ReactiveX/RxJava/pull/847) Various Changes While Fixing GroupBy
* [Pull 849](https://github.com/ReactiveX/RxJava/pull/849) Add 'Fragment-Host' to rxjava-contrib modules for OSGi
* [Pull 851](https://github.com/ReactiveX/RxJava/pull/851) Reimplement the timeout operator and fix timeout bugs
* [Pull 846](https://github.com/ReactiveX/RxJava/pull/846) Added overloaded createRequest method that takes an HttpContext instance
* [Pull 777](https://github.com/ReactiveX/RxJava/pull/777) Fixed testSingleSourceManyIterators
* [Pull 852](https://github.com/ReactiveX/RxJava/pull/852) rxjava-debug
* [Pull 853](https://github.com/ReactiveX/RxJava/pull/853) StringObservable Update
* [Pull 763](https://github.com/ReactiveX/RxJava/pull/763) Added support for custom functions in combineLatest.
* [Pull 854](https://github.com/ReactiveX/RxJava/pull/854) The onCreate hook disappeared
* [Pull 857](https://github.com/ReactiveX/RxJava/pull/857) Change Lift to use rx.Observable.Operator 
* [Pull 859](https://github.com/ReactiveX/RxJava/pull/859) Add 'Fragment-Host' to rxjava-contrib/debug module for OSGi
* [Pull 860](https://github.com/ReactiveX/RxJava/pull/860) Fixing the generics for merge and lift
* [Pull 863](https://github.com/ReactiveX/RxJava/pull/863) Optimize SwingMouseEventSource.fromRelativeMouseMotion
* [Pull 862](https://github.com/ReactiveX/RxJava/pull/862) Update the timeout docs
* [Pull 790](https://github.com/ReactiveX/RxJava/pull/790) Convert to scan to use lift
* [Pull 866](https://github.com/ReactiveX/RxJava/pull/866) Update OperationScan to OperatorScan
* [Pull 870](https://github.com/ReactiveX/RxJava/pull/870) Add the selector variants of timeout in RxScala
* [Pull 874](https://github.com/ReactiveX/RxJava/pull/874) Update CompositeSubscriptionTest.java
* [Pull 869](https://github.com/ReactiveX/RxJava/pull/869) subscribeOn + groupBy
* [Pull 751](https://github.com/ReactiveX/RxJava/pull/751) Provide Observable.timestamp(Scheduler) to be used in the tests. 
* [Pull 878](https://github.com/ReactiveX/RxJava/pull/878) Scheduler.scheduleRecursive
* [Pull 877](https://github.com/ReactiveX/RxJava/pull/877) Correct synchronization guard in groupByUntil
* [Pull 880](https://github.com/ReactiveX/RxJava/pull/880) Force ViewObservable be subscribed and unsubscribed in the UI thread
* [Pull 887](https://github.com/ReactiveX/RxJava/pull/887) Remove Bad Filter Logic 
* [Pull 890](https://github.com/ReactiveX/RxJava/pull/890) Split SubscribeOn into SubscribeOn/UnsubscribeOn
* [Pull 891](https://github.com/ReactiveX/RxJava/pull/891) Eliminate rx.util.* dumping grounds
* [Pull 881](https://github.com/ReactiveX/RxJava/pull/881) Lift Performance
* [Pull 893](https://github.com/ReactiveX/RxJava/pull/893) Change Parallel to use Long instead of Int
* [Pull 894](https://github.com/ReactiveX/RxJava/pull/894) Synchronized Operator Check for isTerminated
* [Pull 885](https://github.com/ReactiveX/RxJava/pull/885) Fixed an issue with the from(Reader) added a bunch of unit tests.
* [Pull 896](https://github.com/ReactiveX/RxJava/pull/896) removing java 7 dep
* [Pull 883](https://github.com/ReactiveX/RxJava/pull/883) Make Subscriptions of SwingObservable thread-safe
* [Pull 895](https://github.com/ReactiveX/RxJava/pull/895) Rewrite OperationObserveFromAndroidComponent to OperatorObserveFromAndroid
* [Pull 892](https://github.com/ReactiveX/RxJava/pull/892) onErrorFlatMap + OnErrorThrowable
* [Pull 898](https://github.com/ReactiveX/RxJava/pull/898) Handle illegal errors thrown from plugin
* [Pull 901](https://github.com/ReactiveX/RxJava/pull/901) GroupBy Unit Test from #900
* [Pull 902](https://github.com/ReactiveX/RxJava/pull/902) Fixed NullPointerException that may happen on timeout
* [Pull 903](https://github.com/ReactiveX/RxJava/pull/903) Scheduler.Recurse fields should be private
* [Pull 904](https://github.com/ReactiveX/RxJava/pull/904) Merge: Unsubscribe Completed Inner Observables
* [Pull 905](https://github.com/ReactiveX/RxJava/pull/905) RxJavaSchedulers Plugin
* [Pull 909](https://github.com/ReactiveX/RxJava/pull/909) Scheduler Plugin Refactor 
* [Pull 910](https://github.com/ReactiveX/RxJava/pull/910) Remove groupBy with selector
* [Pull 918](https://github.com/ReactiveX/RxJava/pull/918) Operator: doOnTerminate
* [Pull 919](https://github.com/ReactiveX/RxJava/pull/919) BugFix: Zip Never Completes When Zero Observables
* [Pull 920](https://github.com/ReactiveX/RxJava/pull/920) Delete Deprecated onSubscribeStart That Doesn't Work
* [Pull 922](https://github.com/ReactiveX/RxJava/pull/922) Changes made while integrating it with our internal system
* [Pull 924](https://github.com/ReactiveX/RxJava/pull/924) Localized Operator Error Handling
* [Pull 925](https://github.com/ReactiveX/RxJava/pull/925) Rxjava clojure bindings final 
* [Pull 926](https://github.com/ReactiveX/RxJava/pull/926) TestSubscriber: Default onError and Terminal Latch Behavior
* [Pull 927](https://github.com/ReactiveX/RxJava/pull/927) TestSubscriber lastSeenThread 
* [Pull 936](https://github.com/ReactiveX/RxJava/pull/936) Skip fixed
* [Pull 942](https://github.com/ReactiveX/RxJava/pull/942) MathObservable
* [Pull 944](https://github.com/ReactiveX/RxJava/pull/944) OperationRetry -> OperatorRetry
* [Pull 945](https://github.com/ReactiveX/RxJava/pull/945) refactor the debug hooks before they become a breaking change.
* [Pull 934](https://github.com/ReactiveX/RxJava/pull/934) add Observable.startWith(Observable) method and unit test
* [Pull 929](https://github.com/ReactiveX/RxJava/pull/929) correct link to maven search
* [Pull 923](https://github.com/ReactiveX/RxJava/pull/923) Observable creation from Subscriber[T]=>Unit for Scala
* [Pull 931](https://github.com/ReactiveX/RxJava/pull/931) A number of improvements to OperatorObserveFromAndroidComponent 
* [Pull 950](https://github.com/ReactiveX/RxJava/pull/950) Add support for Eclipse PDE


### Version 0.16.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.16.1%22)) ###

* [Pull 730](https://github.com/ReactiveX/RxJava/pull/730) Improve Error Handling and Stacktraces When Unsubscribe Fails
* [Pull 720](https://github.com/ReactiveX/RxJava/pull/720) Added `Observable.timeout` wrappers to scala adapter
* [Pull 731](https://github.com/ReactiveX/RxJava/pull/731) Fix non-deterministic unit test
* [Pull 742](https://github.com/ReactiveX/RxJava/pull/742) Build with Gradle 1.10
* [Pull 718](https://github.com/ReactiveX/RxJava/pull/718) Merge overloads
* [Pull 733](https://github.com/ReactiveX/RxJava/pull/733) Buffer with Observable boundary
* [Pull 734](https://github.com/ReactiveX/RxJava/pull/734) Delay with subscription and item delaying observables
* [Pull 735](https://github.com/ReactiveX/RxJava/pull/735) Window with Observable boundary
* [Pull 736](https://github.com/ReactiveX/RxJava/pull/736) MergeMap with Iterable and resultSelector overloads
* [Pull 738](https://github.com/ReactiveX/RxJava/pull/738) Publish and PublishLast overloads
* [Pull 739](https://github.com/ReactiveX/RxJava/pull/739) Debounce with selector
* [Pull 740](https://github.com/ReactiveX/RxJava/pull/740) Timeout with selector overloads
* [Pull 745](https://github.com/ReactiveX/RxJava/pull/745) Fixed `switch` bug
* [Pull 741](https://github.com/ReactiveX/RxJava/pull/741) Zip with iterable, removed old aggregator version and updated tests
* [Pull 749](https://github.com/ReactiveX/RxJava/pull/749) Separated Android test code from source
* [Pull 732](https://github.com/ReactiveX/RxJava/pull/732) Ported groupByUntil function to scala-adapter


### Version 0.16.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.16.0%22)) ###


This is a significant release with the following changes:

- Refactor of Subjects and Subscriptions to non-blocking implementations 
- Many bug fixes, new operators and behavior changes to match Rx.Net. 
- Deprecation of some operators due to renaming or eliminating duplicates 
- The `rx.concurrency` package has been renamed to `rx.schedulers`. Existing classes still remain in `rx.concurrency` but are deprecated. Use of `rx.concurrency` should be migrated to `rx.schedulers` as these deprecated classes will be removed in a future release. 
- Breaking changes to Scala bindings. See [Release Notes](https://github.com/ReactiveX/RxJava/blob/master/language-adaptors/rxjava-scala/ReleaseNotes.md) for details.
- New modules: rxjava-string, rxjava-async-util and rxjava-computation-expressions for operators deemed not applicable to the core library. 

---

* [Pull 516](https://github.com/ReactiveX/RxJava/pull/516) rxjava-string module with StringObservable
* [Pull 533](https://github.com/ReactiveX/RxJava/pull/533) Operator: ToAsync
* [Pull 535](https://github.com/ReactiveX/RxJava/pull/535) Fix compilation errors due to referencing the Android support library directly
* [Pull 545](https://github.com/ReactiveX/RxJava/pull/545) Fixed Zip issue with infinite streams
* [Pull 539](https://github.com/ReactiveX/RxJava/pull/539) Zipping a finite and an infinite Observable
* [Pull 541](https://github.com/ReactiveX/RxJava/pull/541) Operator: SkipUntil
* [Pull 537](https://github.com/ReactiveX/RxJava/pull/537) Add scala adapters for doOnEach operator
* [Pull 560](https://github.com/ReactiveX/RxJava/pull/560) Add type variances for doOnEach actions
* [Pull 562](https://github.com/ReactiveX/RxJava/pull/562) Scala Adaptor Improvements
* [Pull 563](https://github.com/ReactiveX/RxJava/pull/563) Operator: GroupByUntil
* [Pull 561](https://github.com/ReactiveX/RxJava/pull/561) Revised Approach to Creating Observables in Scala
* [Pull 565](https://github.com/ReactiveX/RxJava/pull/565) Operator: GroupJoin v2
* [Pull 567](https://github.com/ReactiveX/RxJava/pull/567) Operator: Timestamp with Scheduler
* [Pull 568](https://github.com/ReactiveX/RxJava/pull/568) Use lock free strategy for several Subscription implementations
* [Pull 571](https://github.com/ReactiveX/RxJava/pull/571) Operator: Sample with Observable v2
* [Pull 572](https://github.com/ReactiveX/RxJava/pull/572) Multiple Subscriptions to ObserveOn
* [Pull 573](https://github.com/ReactiveX/RxJava/pull/573) Removed Opening and Closing historical artifacts
* [Pull 575](https://github.com/ReactiveX/RxJava/pull/575) Operator: SequenceEqual reimplementation
* [Pull 587](https://github.com/ReactiveX/RxJava/pull/587) Operator: LongCount
* [Pull 586](https://github.com/ReactiveX/RxJava/pull/586) Fix Concat to allow multiple observers
* [Pull 598](https://github.com/ReactiveX/RxJava/pull/598) New Scala Bindings
* [Pull 596](https://github.com/ReactiveX/RxJava/pull/596) Fix for buffer not stopping when unsubscribed
* [Pull 576](https://github.com/ReactiveX/RxJava/pull/576) Operators: Timer and Delay
* [Pull 593](https://github.com/ReactiveX/RxJava/pull/593) Lock-free subscriptions
* [Pull 599](https://github.com/ReactiveX/RxJava/pull/599) Refactor rx.concurrency to rx.schedulers
* [Pull 600](https://github.com/ReactiveX/RxJava/pull/600) BugFix: Replay Subject
* [Pull 594](https://github.com/ReactiveX/RxJava/pull/594) Operator: Start
* [Pull 604](https://github.com/ReactiveX/RxJava/pull/604) StringObservable.join
* [Pull 609](https://github.com/ReactiveX/RxJava/pull/609) Operation: Timer
* [Pull 612](https://github.com/ReactiveX/RxJava/pull/612) Operation: Replay (overloads)
* [Pull 628](https://github.com/ReactiveX/RxJava/pull/628) BugFix: MergeDelayError Synchronization
* [Pull 602](https://github.com/ReactiveX/RxJava/pull/602) BugFix: ObserveOn Subscription leak
* [Pull 631](https://github.com/ReactiveX/RxJava/pull/631) Make NewThreadScheduler create Daemon threads
* [Pull 651](https://github.com/ReactiveX/RxJava/pull/651) Subjects Refactor - Non-Blocking, Common Abstraction, Performance
* [Pull 661](https://github.com/ReactiveX/RxJava/pull/661) Subscriptions Rewrite
* [Pull 520](https://github.com/ReactiveX/RxJava/pull/520) BugFix: blocking/non-blocking `first`
* [Pull 621](https://github.com/ReactiveX/RxJava/pull/621) Scala: SerialSubscription & From
* [Pull 626](https://github.com/ReactiveX/RxJava/pull/626) BO.Latest, fixed: BO.next, BO.mostRecent, BO.toIterable
* [Pull 633](https://github.com/ReactiveX/RxJava/pull/633) BugFix: null in toList operator
* [Pull 635](https://github.com/ReactiveX/RxJava/pull/635) Conditional Operators
* [Pull 638](https://github.com/ReactiveX/RxJava/pull/638) Operations: DelaySubscription, TakeLast w/ time, TakeLastBuffer
* [Pull 659](https://github.com/ReactiveX/RxJava/pull/659) Missing fixes from the subject rewrite
* [Pull 688](https://github.com/ReactiveX/RxJava/pull/688) Fix SafeObserver handling of onComplete errors
* [Pull 690](https://github.com/ReactiveX/RxJava/pull/690) Fixed Scala bindings
* [Pull 693](https://github.com/ReactiveX/RxJava/pull/693) Kotlin M6.2
* [Pull 689](https://github.com/ReactiveX/RxJava/pull/689) Removed ObserverBase
* [Pull 664](https://github.com/ReactiveX/RxJava/pull/664) Operation: AsObservable
* [Pull 697](https://github.com/ReactiveX/RxJava/pull/697) Operations: Skip, SkipLast, Take with time
* [Pull 698](https://github.com/ReactiveX/RxJava/pull/698) Operations: Average, Sum
* [Pull 699](https://github.com/ReactiveX/RxJava/pull/699) Operation: Repeat
* [Pull 701](https://github.com/ReactiveX/RxJava/pull/701) Operation: Collect
* [Pull 707](https://github.com/ReactiveX/RxJava/pull/707) Module: rxjava-async-util
* [Pull 708](https://github.com/ReactiveX/RxJava/pull/708) BugFix: combineLatest
* [Pull 712](https://github.com/ReactiveX/RxJava/pull/712) Fix Scheduler Memory Leaks
* [Pull 714](https://github.com/ReactiveX/RxJava/pull/714) Module: rxjava-computation-expressions
* [Pull 715](https://github.com/ReactiveX/RxJava/pull/715) Add missing type hint to clojure example
* [Pull 717](https://github.com/ReactiveX/RxJava/pull/717) Scala: Added ConnectableObservable
* [Pull 723](https://github.com/ReactiveX/RxJava/pull/723) Deprecate multiple arity ‘from’
* [Pull 724](https://github.com/ReactiveX/RxJava/pull/724) Revert use of CurrentThreadScheduler for Observable.from
* [Pull 725](https://github.com/ReactiveX/RxJava/pull/725) Simpler computation/io naming for Schedulers
* [Pull 727](https://github.com/ReactiveX/RxJava/pull/727) ImmediateScheduler optimization for toObservableIterable


### Version 0.15.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.15.1%22)) ###

This release should be additive functionality and bug fixes.

* [Pull 510](https://github.com/ReactiveX/RxJava/pull/506) Operators: And, Then, When
* [Pull 514](https://github.com/ReactiveX/RxJava/pull/514) Operator: Join
* [Pull 525](https://github.com/ReactiveX/RxJava/pull/526) Operators: toMap/toMultiMap
* [Pull 510](https://github.com/ReactiveX/RxJava/pull/510) BugFix: Zip
* [Pull 512](https://github.com/ReactiveX/RxJava/pull/512) Scala Adaptor Details
* [Pull 512](https://github.com/ReactiveX/RxJava/pull/529) Scala fixes
* [Pull 508](https://github.com/ReactiveX/RxJava/pull/508) Empty subscribe 
* [Pull 522](https://github.com/ReactiveX/RxJava/pull/522) Unsubscribe from takeLast
* [Pull 525](https://github.com/ReactiveX/RxJava/pull/525) BugFix: Handling of Terminal State for Behavior/Publish Subjects

### Version 0.15.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.15.0%22)) ###

This release contains a refactor of the Scala Bindings by @headinthebox that results in some breaking changes. 
The previous solution ended up not working well in all cases for idiomatic Scala usage. Java/Scala interop has been changed and is no longer transparent so as to optimize for native Scala usage.
Read the [rxjava-scala README](https://github.com/ReactiveX/RxJava/tree/master/language-adaptors/rxjava-scala) for more information.

* [Pull 503](https://github.com/ReactiveX/RxJava/pull/503) New Scala Bindings
* [Pull 502](https://github.com/ReactiveX/RxJava/pull/502) Fix ObserveOn and add ParallelMerge Scheduler overload
* [Pull 499](https://github.com/ReactiveX/RxJava/pull/499) ObserveOn Refactor
* [Pull 492](https://github.com/ReactiveX/RxJava/pull/492) Implement the scheduler overloads for Range, From, StartWith
* [Pull 496](https://github.com/ReactiveX/RxJava/pull/496) Add contravariant for min and max

### Version 0.14.11 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.11%22)) ###

* [Pull 486](https://github.com/ReactiveX/RxJava/pull/486) BugFix: AsyncSubject
* [Pull 483](https://github.com/ReactiveX/RxJava/pull/483) Tweaks to DoOnEach and added DoOnError/DoOnCompleted

This has a very slight breaking change by removing one `doOnEach` overload. The version was not bumped from 0.14 to 0.15 as it is so minor and the offending method was just released in the previous version.

### Version 0.14.10 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.10%22)) ###

* [Pull 481](https://github.com/ReactiveX/RxJava/pull/481) Operator: Using
* [Pull 480](https://github.com/ReactiveX/RxJava/pull/480) BugFix: Emit an IllegalArgumentException instead of ArithmeticException if the observable is empty
* [Pull 479](https://github.com/ReactiveX/RxJava/pull/479) Operator: DoOnEach
* [Pull 478](https://github.com/ReactiveX/RxJava/pull/478) Operator: Min, MinBy, Max, MaxBy
* [Pull 463](https://github.com/ReactiveX/RxJava/pull/463) Add Timeout Overloads

### Version 0.14.9 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.9%22)) ###

* [Pull 477](https://github.com/ReactiveX/RxJava/pull/477) BugFix: CompositeSubscription
* [Pull 476](https://github.com/ReactiveX/RxJava/pull/476) BugFix: Don't emit null onComplete when no onNext received in AsyncSubject
* [Pull 474](https://github.com/ReactiveX/RxJava/pull/474) BugFix: Reduce an empty observable
* [Pull 474](https://github.com/ReactiveX/RxJava/pull/474) BugFix: non-deterministic unit test
* [Pull 472](https://github.com/ReactiveX/RxJava/pull/472) BugFix: Issue 431 Unsubscribe with Schedulers.newThread
* [Pull 470](https://github.com/ReactiveX/RxJava/pull/470) Operator: Last

### Version 0.14.8 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.8%22)) ###

* [Pull 460](https://github.com/ReactiveX/RxJava/pull/460) Operator: Amb
* [Pull 466](https://github.com/ReactiveX/RxJava/pull/466) Refactor Unit Tests

### Version 0.14.7 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.7%22)) ###

* [Pull 459](https://github.com/ReactiveX/RxJava/pull/459) Fix multiple unsubscribe behavior
* [Pull 458](https://github.com/ReactiveX/RxJava/pull/458) rxjava-android: OperationObserveFromAndroidComponent
* [Pull 453](https://github.com/ReactiveX/RxJava/pull/453) Fix error handling in map operator
* [Pull 450](https://github.com/ReactiveX/RxJava/pull/450) Operator: TimeInterval
* [Pull 452](https://github.com/ReactiveX/RxJava/pull/451) Scheduler Overload of Just/Return Operator
* [Pull 433](https://github.com/ReactiveX/RxJava/pull/433) Fixes: Next Operator
* [Commit d64a8c5](https://github.com/ReactiveX/RxJava/commit/d64a8c5f73d8d1a5de1861e0d20f12609b408880) Update rxjava-apache-http to Apache HttpAsyncClient 4.0 GA 

### Version 0.14.6 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.6%22)) ###

* [Pull 441](https://github.com/ReactiveX/RxJava/pull/441) Fixed the issue that 'take' does not call 'onError'
* [Pull 443](https://github.com/ReactiveX/RxJava/pull/443) OperationSwitch notify onComplete() too early.
* [Pull 434](https://github.com/ReactiveX/RxJava/pull/434) Timeout operator and SerialSubscription
* [Pull 447](https://github.com/ReactiveX/RxJava/pull/447) Caching the result of 'isInternalImplementation'

### Version 0.14.5 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.5%22)) ###

* [Pull 438](https://github.com/ReactiveX/RxJava/pull/438) Kotlin Language Adaptor

### Version 0.14.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.4%22)) ###

* [Issue 428](https://github.com/ReactiveX/RxJava/issues/428) Fix: buffer() using TimeAndSizeBasedChunks incorrectly forces thread into interrupted state
* [Pull 435](https://github.com/ReactiveX/RxJava/pull/435) rx-apache-http recognizes "Transfer-Encoding: chunked" as an HTTP stream
* [Pull 437](https://github.com/ReactiveX/RxJava/pull/437) Fixes: Scheduler and Merge


### Version 0.14.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.3%22)) ###

* [Pull 407](https://github.com/ReactiveX/RxJava/pull/407) Operator: RefCount
* [Pull 410](https://github.com/ReactiveX/RxJava/pull/410) Operator: Contains
* [Pull 411](https://github.com/ReactiveX/RxJava/pull/411) Unit Test fix: update counter before triggering latch 
* [Pull 413](https://github.com/ReactiveX/RxJava/pull/413) Fixed the issues of takeLast(items, 0) and null values
* [Pull 414](https://github.com/ReactiveX/RxJava/pull/414) Operator: SkipLast
* [Pull 415](https://github.com/ReactiveX/RxJava/pull/415) Operator: Empty with scheduler
* [Pull 416](https://github.com/ReactiveX/RxJava/pull/416) Operator: Throw with scheduler 
* [Pull 420](https://github.com/ReactiveX/RxJava/pull/420) Scala Adaptor Improvements
* [Pull 422](https://github.com/ReactiveX/RxJava/pull/422) JRuby function wrapping support
* [Pull 424](https://github.com/ReactiveX/RxJava/pull/424) Operator: IgnoreElements
* [Pull 426](https://github.com/ReactiveX/RxJava/pull/426) PublishSubject ReSubscribe for publish().refCount() Behavior

### Version 0.14.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.2%22)) ###

* [Pull 403](https://github.com/ReactiveX/RxJava/pull/403) Operators: Cast and OfType
* [Pull 401](https://github.com/ReactiveX/RxJava/pull/401) Operator: DefaultIfEmpty
* [Pull 409](https://github.com/ReactiveX/RxJava/pull/409) Operator: Synchronize with object

### Version 0.14.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.1%22)) ###

* [Pull 402](https://github.com/ReactiveX/RxJava/pull/402) rxjava-apache-http improvements

### Version 0.14.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.14.0%22)) ###

Further progress to the Scala adaptor and a handful of new operators.

Bump to 0.14.0 due to small breaking change to `distinct` operator removing overloaded methods with `Comparator`. These methods were added in 0.13.2 and determined to be incorrect.

This release also includes a new contrib module, [rxjava-apache-http](https://github.com/ReactiveX/RxJava/tree/master/rxjava-contrib/rxjava-apache-http) that provides an Observable API to the Apache HttpAsyncClient.

* [Pull 396](https://github.com/ReactiveX/RxJava/pull/396) Add missing methods to Scala Adaptor
* [Pull 390](https://github.com/ReactiveX/RxJava/pull/390) Operators: ElementAt and ElementAtOrDefault
* [Pull 398](https://github.com/ReactiveX/RxJava/pull/398) Operators: IsEmpty and Exists (instead of Any)
* [Pull 397](https://github.com/ReactiveX/RxJava/pull/397) Observable API for Apache HttpAsyncClient 4.0
* [Pull 400](https://github.com/ReactiveX/RxJava/pull/400) Removing `comparator` overloads of `distinct`

### Version 0.13.5

* Upload to Sonatype failed so version skipped

### Version 0.13.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.13.4%22)) ###

* [Pull 393](https://github.com/ReactiveX/RxJava/pull/393) Parallel Operator & ObserveOn/ScheduledObserver Fixes
* [Pull 394](https://github.com/ReactiveX/RxJava/pull/394) Change Interval and Sample default Scheduler
* [Pull 391](https://github.com/ReactiveX/RxJava/pull/391) Fix OSGI support for rxjava-scala

### Version 0.13.3

* Upload to Sonatype failed so version skipped

### Version 0.13.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.13.2%22)) ###

* [Pull 389](https://github.com/ReactiveX/RxJava/pull/389) Scala Adaptor Improvements
* [Pull 382](https://github.com/ReactiveX/RxJava/pull/382) Removing deprecated RxImplicits from rxjava-scala
* [Pull 381](https://github.com/ReactiveX/RxJava/pull/381) Operator: mapWithIndex
* [Pull 380](https://github.com/ReactiveX/RxJava/pull/380) Implemented `distinct` and `distinctUntilChanged` variants using a comparator
* [Pull 379](https://github.com/ReactiveX/RxJava/pull/379) Make `interval` work with multiple subscribers

### Version 0.13.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.13.1%22)) ###

This release includes a new Scala adaptor as part of the effort from issue https://github.com/ReactiveX/RxJava/issues/336 pursuing idiomatic Scala support.

* [Pull 376](https://github.com/ReactiveX/RxJava/pull/376) Idiomatic Scala Adaptor
* [Pull 375](https://github.com/ReactiveX/RxJava/pull/375) Operator: Distinct
* [Pull 374](https://github.com/ReactiveX/RxJava/pull/374) Operator: DistinctUntilChanged
* [Pull 373](https://github.com/ReactiveX/RxJava/pull/373) Fixes and Cleanup

### Version 0.13.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.13.0%22)) ###

This release has some minor changes related to varargs that could break backwards compatibility 
if directly passing arrays but for most this release should not be breaking.

* [Pull 354](https://github.com/ReactiveX/RxJava/pull/354) Operators: Count, Sum, Average
* [Pull 355](https://github.com/ReactiveX/RxJava/pull/355) Operators: skipWhile and skipWhileWithIndex
* [Pull 356](https://github.com/ReactiveX/RxJava/pull/356) Operator: Interval
* [Pull 357](https://github.com/ReactiveX/RxJava/pull/357) Operators: first and firstOrDefault
* [Pull 368](https://github.com/ReactiveX/RxJava/pull/368) Operators: Throttle and Debounce
* [Pull 371](https://github.com/ReactiveX/RxJava/pull/371) Operator: Retry
* [Pull 370](https://github.com/ReactiveX/RxJava/pull/370) Change zip method signature from Collection to Iterable
* [Pull 369](https://github.com/ReactiveX/RxJava/pull/369) Generics Improvements: co/contra-variance
* [Pull 361](https://github.com/ReactiveX/RxJava/pull/361) Remove use of varargs from API

### Version 0.12.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.12.2%22)) ###

* [Pull 352](https://github.com/ReactiveX/RxJava/pull/352) Groovy Language Adaptor: Add Func5-9 and N to the wrapper

### Version 0.12.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.12.1%22)) ###

* [Pull 350](https://github.com/ReactiveX/RxJava/pull/350) Swing module enhancements
* [Pull 351](https://github.com/ReactiveX/RxJava/pull/351) Fix Observable.window static/instance bug

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

* [Pull 343](https://github.com/ReactiveX/RxJava/pull/343) Covariant Support with super/extends and `OnSubscribeFunc` as type for `Observable.create`
* [Pull 337](https://github.com/ReactiveX/RxJava/pull/337) Operator: `window`
* [Pull 348](https://github.com/ReactiveX/RxJava/pull/348) Rename `switchDo` to `switchOnNext` (deprecate `switchDo` for eventual deletion)
* [Pull 348](https://github.com/ReactiveX/RxJava/pull/348) Delete `switchDo` instance method in preference for static
* [Pull 346](https://github.com/ReactiveX/RxJava/pull/346) Remove duplicate static methods from `BlockingObservable` 
* [Pull 346](https://github.com/ReactiveX/RxJava/pull/346) `BlockingObservable` no longer extends from `Observable`
* [Pull 345](https://github.com/ReactiveX/RxJava/pull/345) Remove unnecessary constructor from `Observable`

### Version 0.11.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.11.2%22)) ###

* [Commit ccf53e8]( https://github.com/ReactiveX/RxJava/commit/ccf53e84835d99136cce80a4c508bae787d5da45) Update to Scala 2.10.2

### Version 0.11.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.11.1%22)) ###

* [Pull 325](https://github.com/ReactiveX/RxJava/pull/325) Clojure: Preserve metadata on fn and action macros

### Version 0.11.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.11.0%22)) ###

This is a major refactor of rxjava-core and the language adaptors. 

Note that there are *breaking changes* in this release. Details are below.

After this refactor it is expected that the API will settle down and allow us to stabilize towards a 1.0 release.

* [Pull 332](https://github.com/ReactiveX/RxJava/pull/332) Refactor Core to be Statically Typed

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
  - Clojure adds a new macro ([NOTE: this requires code changes](https://github.com/ReactiveX/RxJava/tree/master/language-adaptors/rxjava-clojure#basic-usage))
  - JRuby has been temporarily disabled (discussing new implementation at https://github.com/ReactiveX/RxJava/issues/320)
- language supports continue to be additive
  - the rxjava-core will always be required and then whichever language modules are desired such as rxjava-scala, rxjava-clojure, rxjava-groovy are added to the classpath
- deletes deprecated methods
- deletes redundant static methods on `Observable` that cluttered the API and in some cases caused dynamic languages trouble choosing which method to invoke
- deletes redundant methods on `Scheduler` that gave dynamic languages a hard time choosing which method to invoke

The benefits of this are:

1) Everything is statically typed so compile-time checks for Java, Scala, etc work correctly
2) Method dispatch is now done via native Java bytecode using types rather than going via `Object` which then has to do a lookup in a map. Memoization helped with the performance but each method invocation still required looking in a map for the correct adaptor. With this approach the appropriate methods will be compiled into the `rx.Observable` class to correctly invoke the right adaptor without lookups. 
3) Interaction from each language should work as expected idiomatically for that language.

Further history on the various discussions and different attempts at solutions can be seen at https://github.com/ReactiveX/RxJava/pull/304, https://github.com/ReactiveX/RxJava/issues/204 and https://github.com/ReactiveX/RxJava/issues/208


### Version 0.10.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.10.1%22)) ###

A new contrib module for Android: https://github.com/ReactiveX/RxJava/tree/master/rxjava-contrib/rxjava-android

* [Pull 318](https://github.com/ReactiveX/RxJava/pull/318) rxjava-android module with Android Schedulers

### Version 0.10.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.10.0%22)) ###

This release includes a breaking change as it changes `onError(Exception)` to `onError(Throwable)`. This decision was made via discussion at https://github.com/ReactiveX/RxJava/issues/296.

Any statically-typed `Observer` implementations with `onError(Exception)` will need to be updated to `onError(Throwable)` when moving to this version.

* [Pull 312](https://github.com/ReactiveX/RxJava/pull/312) Fix for OperatorOnErrorResumeNextViaObservable and async Resume
* [Pull 314](https://github.com/ReactiveX/RxJava/pull/314) Map Error Handling
* [Pull 315](https://github.com/ReactiveX/RxJava/pull/315) Change onError(Exception) to onError(Throwable) - Issue #296

### Version 0.9.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.9.2%22)) ###

* [Pull 308](https://github.com/ReactiveX/RxJava/pull/308) Ensure now() is always updated in TestScheduler.advanceTo/By
* [Pull 281](https://github.com/ReactiveX/RxJava/pull/281) Operator: Buffer

### Version 0.9.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.9.1%22)) ###

* [Pull 303](https://github.com/ReactiveX/RxJava/pull/303) CombineLatest
* [Pull 290](https://github.com/ReactiveX/RxJava/pull/290) Zip overload with FuncN
* [Pull 302](https://github.com/ReactiveX/RxJava/pull/302) NPE fix when no package on class
* [Pull 284](https://github.com/ReactiveX/RxJava/pull/284) GroupBy fixes (items still [oustanding](https://github.com/ReactiveX/RxJava/issues/282))
* [Pull 288](https://github.com/ReactiveX/RxJava/pull/288) PublishSubject concurrent modification fixes
* [Issue 198](https://github.com/ReactiveX/RxJava/issues/198) Throw if no onError handler specified
* [Issue 278](https://github.com/ReactiveX/RxJava/issues/278) Subscribe argument validation
* Javadoc improvements and many new marble diagrams

### Version 0.9.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.9.0%22)) ###

This release includes breaking changes that move all blocking operators (such as `single`, `last`, `forEach`) to `BlockingObservable`.

This means `Observable` has only non-blocking operators on it. The blocking operators can now be accessed via `.toBlockingObservable()` or `BlockingObservable.from(observable)`.

Notes and link to the discussion of this change can be found at https://github.com/ReactiveX/RxJava/pull/272.

* [Pull 272](https://github.com/ReactiveX/RxJava/pull/272) Move blocking operators into BlockingObservable
* [Pull 273](https://github.com/ReactiveX/RxJava/pull/273) Fix Concat (make non-blocking)
* [Issue 13](https://github.com/ReactiveX/RxJava/issues/13) Operator: Switch
* [Pull 274](https://github.com/ReactiveX/RxJava/pull/274) Remove SLF4J dependency (RxJava is now a single jar with no dependencies)

### Version 0.8.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.4%22)) ###

* [Pull 269](https://github.com/ReactiveX/RxJava/pull/269) (Really) Fix concurrency bug in ScheduledObserver

### Version 0.8.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.3%22)) ###

* [Pull 268](https://github.com/ReactiveX/RxJava/pull/268) Fix concurrency bug in ScheduledObserver

### Version 0.8.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.2%22)) ###

* [Issue 74](https://github.com/ReactiveX/RxJava/issues/74) Operator: Sample
* [Issue 93](https://github.com/ReactiveX/RxJava/issues/93) Operator: Timestamp
* [Pull 253](https://github.com/ReactiveX/RxJava/pull/253) Fix multiple subscription bug on operation filter
* [Pull 254](https://github.com/ReactiveX/RxJava/pull/254) SwingScheduler (new rxjava-swing module)
* [Pull 256](https://github.com/ReactiveX/RxJava/pull/256) BehaviorSubject
* [Pull 257](https://github.com/ReactiveX/RxJava/pull/257) Improved scan, reduce, aggregate
* [Pull 262](https://github.com/ReactiveX/RxJava/pull/262) SwingObservable (new rxjava-swing module)
* [Pull 264](https://github.com/ReactiveX/RxJava/pull/263) Publish, Replay and Cache Operators
* 
### Version 0.8.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.1%22)) ###

* [Pull 250](https://github.com/ReactiveX/RxJava/pull/250) AsyncSubject
* [Pull 252](https://github.com/ReactiveX/RxJava/pull/252) ToFuture
* [Pull 246](https://github.com/ReactiveX/RxJava/pull/246) Scheduler.schedulePeriodically
* [Pull 247](https://github.com/ReactiveX/RxJava/pull/247) flatMap aliased to mapMany

### Version 0.8.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.8.0%22)) ###

This is a breaking (non-backwards compatible) release that updates the Scheduler implementation released in 0.7.0.

See https://github.com/ReactiveX/RxJava/issues/19 for background, discussion and status of Schedulers.

It is believed that the public signatures of Scheduler and related objects is now stabilized but ongoing feedback and review by the community could still result in changes.

* [Issue 19](https://github.com/ReactiveX/RxJava/issues/19) Schedulers improvements, changes and additions
* [Issue 202](https://github.com/ReactiveX/RxJava/issues/202) Fix Concat bugs
* [Issue 65](https://github.com/ReactiveX/RxJava/issues/65) Multicast
* [Pull 218](https://github.com/ReactiveX/RxJava/pull/218) ReplaySubject

### Version 0.7.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.7.0%22)) ###

This release adds the foundations of Rx Schedulers.

There are still open questions, portions not implemented and assuredly bugs and behavior we didn't understand and thus implemented wrong.

Please provide bug reports, pull requests or feedback to help us on the road to version 1.0 and get schedulers implemented correctly.

See https://github.com/ReactiveX/RxJava/issues/19#issuecomment-15979582 for some known open questions that we could use help answering.

* [Issue 19](https://github.com/ReactiveX/RxJava/issues/19) Schedulers

### Version 0.6.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.6.3%22)) ###

* [Pull 224](https://github.com/ReactiveX/RxJava/pull/224) RxJavaObservableExecutionHook

### Version 0.6.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.6.2%22)) ###

* [Issue 101](https://github.com/ReactiveX/RxJava/issues/101) Operator: Where (alias to filter)
* [Pull 197](https://github.com/ReactiveX/RxJava/pull/197) TakeWhile observables do not properly complete
* [Issue 21](https://github.com/ReactiveX/RxJava/issues/21) Operator: All
* [Pull 206](https://github.com/ReactiveX/RxJava/pull/206) Observable.toList breaks with multiple subscribers
* [Issue 29](https://github.com/ReactiveX/RxJava/issues/29) Operator: CombineLatest
* [Issue 211](https://github.com/ReactiveX/RxJava/issues/211) Remove use of JSR 305 and dependency on com.google.code.findbugs
* [Pull 212](https://github.com/ReactiveX/RxJava/pull/212) Operation take leaks errors
* [Pull 220](https://github.com/ReactiveX/RxJava/pull/220) TakeWhile protect calls to predicate
* [Pull 221](https://github.com/ReactiveX/RxJava/pull/221) Error Handling Improvements - User Provided Observers/Functions
* [Pull 201](https://github.com/ReactiveX/RxJava/pull/201) Synchronize Observer on OperationMerge
* [Issue 43](https://github.com/ReactiveX/RxJava/issues/43) Operator: Finally

### Version 0.6.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.6.1%22)) ###

* [Pull 190](https://github.com/ReactiveX/RxJava/pull/190) Fix generics issue with materialize() that prevented chaining

### Version 0.6.0 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.6.0%22)) ###

* [Issue 154](https://github.com/ReactiveX/RxJava/issues/154) Add OSGi manifest headers
* [Issue 173](https://github.com/ReactiveX/RxJava/issues/173) Subscription Utilities and Default Implementations
* [Pull 184](https://github.com/ReactiveX/RxJava/pull/184) Convert 'last' from non-blocking to blocking to match Rx.Net (see [Issue 57](https://github.com/ReactiveX/RxJava/issues/57))

*NOTE:* This is a version bump from 0.5 to 0.6 because [Issue 173](https://github.com/ReactiveX/RxJava/issues/173) and [Pull 184](https://github.com/ReactiveX/RxJava/pull/184) include breaking changes.

These changes are being done in the goal of matching the [Rx.Net](https://rx.codeplex.com) implementation so breaking changes will be made prior to 1.0 on 0.x releases if necessary.

It was found that the `last()` operator was implemented [incorrectly](https://github.com/ReactiveX/RxJava/issues/57) (non-blocking instead of blocking) so any use of `last()` on version 0.5.x should be changed to use `takeLast(1)`. Since the return type needed to change this could not be done via a deprecation.

Also [removed](https://github.com/ReactiveX/RxJava/issues/173) were the `Observable.createSubscription`/`Observable.noOpSubscription` methods which are now on the rx.subscriptions.Subscriptions utility class as `Subscriptions.create`/`Subscriptions.empty`. These methods could have been deprecated rather than removed but since another breaking change was being done they were just cleanly changed as part of the pre-1.0 process.


### Version 0.5.5 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.5%22)) ###

* [Issue 35](https://github.com/ReactiveX/RxJava/issues/35) Operator: Defer
* [Issue 37](https://github.com/ReactiveX/RxJava/issues/37) Operator: Dematerialize
* [Issue 50](https://github.com/ReactiveX/RxJava/issues/50) Operator: GetEnumerator (GetIterator)
* [Issue 64](https://github.com/ReactiveX/RxJava/issues/64) Operator: MostRecent
* [Issue 86](https://github.com/ReactiveX/RxJava/issues/86) Operator: TakeUntil

### Version 0.5.4 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.4%22)) ###

* [Issue 18](https://github.com/ReactiveX/RxJava/issues/18) Operator: ToIterable
* [Issue 58](https://github.com/ReactiveX/RxJava/issues/58) Operator: LastOrDefault
* [Issue 66](https://github.com/ReactiveX/RxJava/issues/66) Operator: Next
* [Issue 77](https://github.com/ReactiveX/RxJava/issues/77) Operator: Single and SingleOrDefault
* [Issue 164](https://github.com/ReactiveX/RxJava/issues/164) Range.createWithCount bugfix
* [Pull 161](https://github.com/ReactiveX/RxJava/pull/161) Build Status Badges and CI Integration

### Version 0.5.3 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.3%22)) ###

* [Issue 45](https://github.com/ReactiveX/RxJava/issues/45) Operator: ForEach
* [Issue 87](https://github.com/ReactiveX/RxJava/issues/87) Operator: TakeWhile
* [Pull 145](https://github.com/ReactiveX/RxJava/pull/145) IntelliJ IDEA support in Gradle build

### Version 0.5.2 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.2%22)) ###

* [Issue 68](https://github.com/ReactiveX/RxJava/issues/68) Operator: Range
* [Issue 76](https://github.com/ReactiveX/RxJava/issues/76) Operator: SequenceEqual
* [Issue 85](https://github.com/ReactiveX/RxJava/issues/85) Operator: TakeLast
* [Issue 139](https://github.com/ReactiveX/RxJava/issues/85) Plugin System
* [Issue 141](https://github.com/ReactiveX/RxJava/issues/85) Error Handler Plugin
* [Pull 134](https://github.com/ReactiveX/RxJava/pull/134) VideoExample in Clojure
* [Pull 135](https://github.com/ReactiveX/RxJava/pull/135) Idiomatic usage of import in ns macro in rx-examples.
* [Pull 136](https://github.com/ReactiveX/RxJava/pull/136) Add examples for jbundler and sbt

### Version 0.5.1 ([Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22%20AND%20v%3A%220.5.1%22)) ###

* variety of code cleanup commits
* [Pull 132](https://github.com/ReactiveX/RxJava/pull/132) Broke rxjava-examples mo
