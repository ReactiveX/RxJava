# RxJava Releases #

Version 1.x can be found at https://github.com/ReactiveX/RxJava/blob/1.x/CHANGES.md

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
