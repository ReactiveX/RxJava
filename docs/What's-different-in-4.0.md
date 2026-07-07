# Introduction

RxJava 4.0 is the next major, significantly upgraded version of the most influential Reactive Programming
library on the Java JVM ecosystem.

Thanks to the upgraded Java 26 baseline, the code and usage is now far more convenient thanks to the language,
runtime and over 6 years of general computer science advancements.

We can now leverage ***records***, ***virtual threads***, ***collection framework enhancements***,
***patterns*** and the ***var*** keyword.

We also aimed at reducing and rationalizing the API surface of the library, which resulted in many overloads
getting coalesced into so-called ***configuration records*** where the overload pyramids can be hidden and
expanded upon demand.

This document describes the detailed changes from 3.1 to 4.0 in terms of API surface, behavior, notable internal
changes, new types and new ways of using both standard Java feautures with RxJava and the library itself.

## Quick migration guide

1. Replace `io.reactivex.rxjava3` with `io.reactivex.rxjava4` in imports.
2. Replace `io.reactivex.rxjava3.flowables` with `io.reactivex.rxjava4.core` in imports.
3. Replace `io.reactivex.rxjava3.observables` with `io.reactivex.rxjava4.core` in imports.
4. In missing method cases, look for an overload with a `XxxConfig` in its name, and wrap your arguments with it.
7. Some types no longer implement `Disposable` so look for a `.asDisposable()` methods.
5. Decide what scheduler to use instead of `Schedulers.io()`, i.e., `cached()` or `virtual()`.
6. Replace `rx3.` with `rxjava4.` in properties references.

# Enhancements

## API

### Virtual threads

Java 21, released September 2023, standardized the ***virtual thread*** infrastructure that was in development in
various forms since 2017.

Primary aim was to provide a worry-free blocking concurrency where people can freely wait on blocking calls while not
bogging down the scarce amount platform threads. No more cached executors, just have as many virtual threaded executor
as you want and they manage the blocking waiting by janking out the execution context of the runnable code and janking
back in some other readily runnable code. 

The secondary aim was political; make RxJava obsolete. Who want's declarative coordination/concurrency when you can just
sequentially block on sequential code, right?

Ooops, they forgot coordination. So now they are working on [Structured Concurrency](https://openjdk.org/jeps/533). As of writing this (2026.07.07.), they are in the seventh preview via JDK 27. Not bad, but not production ready. What is it for? In RxJava terms, they are single-element `amb`s and `zip`s and `merge`s. But as the API currently stands, you'll sweat blood working out the proper coordination algorithms.

What did RxJava do? Introduced a bunch of methods and infrastructure to capture virtual threaded executors or use
them directly in new operators.

#### Schedulers.virtual()

There is now a new standard `Scheduler` named `virtual` that is backed by the java standard
`Executors.newVirtualThreadPerTaskExecutor`.

That's it. All the huff and puff, and we just assimilated them into a `Scheduler` implementation.

```java
Flowable.fromCallable(() -> {
    return yourBlockingCallHere();
})
.subscribeOn(Schedulers.virtual())
.subscribe(IO::println);
```

You can now run thousands of web or database calls via blocking APIs and not exhaust the system resources.

Since virtual threads are best for IO-heavy work, the `Schedulers.io()` has been deprecated and is routed to the now
renamed `Schedulers.cached()` scheduler.

Related: [#7763](https://github.com/ReactiveX/RxJava/issues/7763), [8051](https://github.com/ReactiveX/RxJava/pull/8051)

#### Flowable.virtualCreate and Flowable.virtualTransform

The main appeal of virtual threads was, to some people, using it to perform blocking backpressure.

Instead of a dance of `Subscription.request`, serialization, trampolining, resumptions, people could
just write code that emits the item, and it just auto-blocks when the downstream is not ready.

Unfortunately, using a virtual scheduler on a create-like operator doesn't work. These can have blocking calls cheaply
on a virtual scheduler, but the backpressure infrastucture is still non-blocking. So they would still overflow or drop items.

Also they are so established changing them would result in massive headache trying to track down all the issues and hangs in codebases.

Therefore, two new methods have been introduced that ensure

- their body code can block freely,
- they use blocking backpressure to halt their body emissions,
- can be configured to run on virtual threads or on any classical schedulers of RxJava - with the caveat of real-blocking costs

##### virtualCreate

The first one is to replace `create` or most generator methods via virtualized blocking and sequential looking code:

```java
Flowable.virtualCreate(emitter -> {
    emiter.emit(1);
    emiter.emit(2);
    emiter.emit(3); // <---------- due to lack of requests, this will block here
    emiter.emit(4);
})
.subscribeWith(new TestScheduler<>(2))
.awaitCount(2)
.assertValues(1, 2)
.assertNoErrors()
.assertNotComplete();
```

##### virtualTransform
TODO

#### Virtual callback interfaces

It is worth detailing them here because they are functional interfaces with often many arguments not trivial
unless looking at their specs.

##### VirtualEmitter

TBD

##### VirtualTransformer

TBD

##### VirtualGenerator

TBD

#### Virtual debugging

Unfortunately, some IDEs (Eclipse) still can't properly debug virtual threaded code. Single stepping and breakpoints simply do not work or hang the IDE.
So much for sequential looking code that is easy to debug.

But what works is using the various `Scheduler` or `ExecutorService` overloads and running the problem code on a traditional scheduler! This comes in handy with unit tests or debugging the new [Streamable](#streamable) operators.

```java
try (var executor = Executors.newCachedThreadPool()) {
    Streamable.create(emitter -> {
        emiter.emit(1);
        emiter.emit(2);
        emiter.emit(3); // <---------- due to lack of requests, this will block here
        emiter.emit(4);
    }, executor) // <------ for debugging purposes
    .subscribeWith(new TestScheduler<>(2), executor) // <------ for debugging purposes
    .awaitCount(2)
    .assertValues(1, 2)
    .assertNoErrors()
    .assertNotComplete();
}
```

Here, you can now single step through the various internals and not glitch out your IDE.

### New schedulers

Apart from `Schedulers.virtual`, there are now two additional standard `Scheduler` implementations and one extra wrapper for creating one
from an `Executor`/`ExecutorService`/`ScheduledExecutorService`. Plus a rename.

#### Shared scheduler

TBD

#### Blocking scheduler

TBD

#### Deferred Executor-based scheduler

TBD

#### Cached scheduler

This is the traditional `Schedulers.io()` renamed to `Schedulers.cached()` to better signal the intent and distinguish
it from the `Schedulers.virtual()` when deciding where to do blocking runs.

It was decided not to delete `Schedulers.io()` but only deprecate it, because codebases are riddled with its use.
There will be many compilation errors after a library swap, let's not add to it via a missing method and no obvious
replacement.

To ensure no-surprise compatibility, `Schedulers.io()` internally delegates to `Schedulers.cached()`.

Due to the rename and for consistency, the system properties for this scheduler have been renamed:

`rx3.io*` to `rxjava4.cached*`

- `rxjava4.cached-keep-alive-time`
- `rxjava4.cached-priority`
- `rxjava4.cached-scheduled-release`


### ErrorMode

Previously an internal type, now public to help with the new way of using [Configuration records](#configuration-records)
and handling errors beyond the typical delay or don't delay them in an unified manner.

It has three modes:
- `IMMEDIATE` - the operator should signal the error as soon as possible
- `BOUNDARY` - delay the error until a boundary change, most commonly with `concat`-style coordination
- `END` - delay the error till the very end and then emit all errors accumulated, usually in a `CompositeException`.

Some operators have no meaning for `BOUNDARY` mode and these treat the mode as if `END` was requested.

Related: [#8175](https://github.com/ReactiveX/RxJava/pull/8175)

### TestSubscriber and TestObserver

They both received new methods to have them work more easily with the almost always async running
`Streamable` sequences.

#### assertValueSet

TBD

#### awaitOnSubscribe

TBD

#### awaitCount with times

TBD

#### asDisposable

The test consumers no longer implement `Disposable` directly as it ended up causing a lot of ambiguities
when working with them in code. They can be cancelled manually still, but you cannot add them
to a disposable container.

```java
var cd = new CompositeDisposable();
var ts = new TestSubscriber<>();

// cd.add(ts) // <----------- no longer works

cd.add(ts.asDisposable());
```

#### errors

TBD

#### timeouts

##### isTimeout

TBD

##### clearTimeout

TBD

##### assertTimeout

TBD

##### assertNoTimeout

TBD

#### toString

When calling `toString`, it will now generate a textual representation of the internal state of the consumer,
including counts of events and tag.

```java
var ts = new TestSubscriber<>();
ts.withTag("Tag");

IO.println(ts);
```

results in

```
TestSubscriber (latch = 0, values = 0, errors = 0, completions = 0, tag = Tag)
```

### DisposableContainer

TBD

## System properties

RxJava had a few system properties to control some aspects globally within a JVM instance. These usually run under the name prefix `rx.`, `rx2.` or `rx3.`.

To avoid ambiguities in the future, the new prefix is now on `rxjava4.`, i.e., the library name fully written out.

Examples:
- `rxjava4.buffer-size`
- `rxjava4.cached-priority`

## Configuration records

TBD

# Streamable

## Introduction

Now that we have worry-free blocking and `CompletableFuture.join()` we can build a whole new reactive type with it.

Enter the new `Streamable` interface with a matching fluent API to `Flowable`, along with a couple of other types

No more state machines, request coordination, just pure for loops and waits.

Except no, because Java doesn't have

- `async`/`await`, 
- `await foreach`,
- `yield return`,
- `async X method()`, either single value or streaming value,
- generally no compiler built state machines.

So we needed to build most state machinery ourselves.

But wait, there is more!

One of the critique against reactive programming was that it mandates colored methods; the reactive types sneak into parameters and
return types, in a cascading manner.

But virtual threads and structured concurrency makes it go away.

Except no. Because we don't have compiler generated state machinery support for `CompletionStage`s or `Iterator`s, we have
to rely on blocking which mandates we ensure code runs on virtual threads for the cheap.

Okay, but there is no global virtual pool like there is a global `ForkJoinPool.commonPool()`!

So you now have to either
- have a global `Executors.newVirtualThreadPerTaskExecutor()` hanging around,
- inject one under the hood in every operator,
- what's more common is to have an `ExecutorService` parameter splattered around for injecting one.

Because in practice, especially in testing, you need as much control as you can muster and not let operators
wander around. Plus virtual debugging is infeasible and sometimes even broken. 

See [Virtual Debugging](#virtual-debugging) for more rant.

**So yeah, many-many hand crafted state machines with the occasional `join` call, and many-many `CompletionException` wrappings.**

Related: [8048](https://github.com/ReactiveX/RxJava/pull/8048), [Tag: Streamable](https://github.com/ReactiveX/RxJava/pulls?q=label%3AStreamable)

## API

### Streamable

### Streamer

### StreamProcessor

### StreamSink

### StreamableConverter

### StreamableOperator

## Custom operators

LMAO no.

# Refactorings

## Moved types

### ConnectableX and GroupedX

These have been moved from the `flowables` and `observables` package for two reasons:
- they were the only inhabitants there so wasted package,
- having them next to the *core* API and fellow supertypes makes it easier to discover.

Affects the following types:
- `ConnectableFlowable`
- `GroupedFlowable`
- `ConnectableObsevable`
- `GroupedObservable`

So when migrating, don't forget to organize imports and the new packages should be picked up correctly.

Otherwise, adjust manually:

- Replace `io.reactivex.rxjava3.flowables` with `io.reactivex.rxjava4.core` in imports.
- Replace `io.reactivex.rxjava3.observables` with `io.reactivex.rxjava4.core` in imports.

Related: [#8215](https://github.com/ReactiveX/RxJava/pull/8215)

# Removals

## Scheduler.when

This was one of the operators that created a cycle between the `Flowable`s relying on schedulers and
`Scheduler`s relying on `Flowable`s.

As a longtime maintainer, I had never any intuition for it. My best guess is that the inventor wanted
to throttle actions scheduled via declarative manners for a very niche use case, which was later most
likely been replaced by Kotlin coroutines or other means.

So I decided to ditch it. If you need it, you can find the source code memorized in the related PR below.

Related: [#8153](https://github.com/ReactiveX/RxJava/pull/8153)

# Behavior changes

TBD