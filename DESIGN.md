## RxJava v2 Design

This document explains the terminology, principles, contracts, and other aspects of the design of RxJava v2.
Its intended audience is the implementers of the library.

### Terminology & Definitions

##### Interactive

Producer obeys consumer-driven flow control.
Consumer manages capacity by requesting data.


##### Reactive

Producer is in charge. Consumer has to do whatever it needs to keep up.


##### Hot

When used to refer to a data source (such as an `Observable`), it means it does not have side-effects when subscribed to.

For example, an `Observable` of mouse events. Subscribing to that `Observable` does not cause the mouse events, but starts receiving them.

(Note: Yes, there are *some* side-effects of adding a listener, but they are inconsequential as far as the 'hot' usage is concerned).


##### Cold

When used to refer to a data source (such as an `Observable`), it means it has side-effects when subscribed to.

For example, an `Observable` of data from a remote API (such as an RPC call). Each time that `Observable` is subscribed to causes a new network call to occur.


##### Reactive/Push

Producer is in charge. Consumer has to do whatever it needs to keep up.

Examples:

- `Observable` (RxJS, Rx.Net, RxJava v1.x without backpressure, RxJava v2)
- Callbacks (the producer calls the function at its convenience)
- IRQ, mouse events, IO interrupts
- 2.x `Flowable` (with `request(n)` credit always granted faster or in larger quantity than producer)
- Reactive Streams `Publisher` (with `request(n)` credit always granted faster or in larger quantity than producer)
- Java 9 `Flow.Publisher` (with `request(n)` credit always granted faster than or in larger quantity producer)


##### Synchronous Interactive/Pull

Consumer is in charge. Producer has to do whatever it needs to keep up.

Examples:

- `Iterable`
- 2.x/1.x `Observable` (without concurrency, producer and consumer on the same thread)
- 2.x `Flowable` (without concurrency, producer and consumer on the same thread)
- Reactive Streams `Publisher` (without concurrency, producer and consumer on the same thread)
- Java 9 `Flow.Publisher` (without concurrency, producer and consumer on the same thread)


##### Async Pull (Async Interactive)

Consumer requests data when it wishes, and the data is then pushed when the producer wishes to. 

Examples:

- `Future` & `Promise`
- `Single` (lazy `Future`)
- 2.x `Flowable`
- Reactive Streams `Publisher`
- Java 9 `Flow.Publisher`
- 1.x `Observable` (with backpressure)
- `AsyncEnumerable`/`AsyncIterable`

There is an overhead (performance and mental) for achieving this, which is why we also have the 2.x `Observable` without backpressure.


##### Flow Control

Flow control is any mitigation strategies that a consumer applies to reduce the flow of data.

Examples:

- Controlling the production of data, such as with `Iterator.next` or `Subscription.request(n)`
- Preventing the delivery of data, such as buffer, drop, sample/throttle, and debounce.


##### Eager

Containing object immediately start work when it is created.

Examples: 

- A `Future` once created has work being performed and represents the eventual value of that work. It can not be deferred once created.


##### Lazy

Containing object does nothing until it is subscribed to or otherwise started.

Examples:

- `Observable.create` does not start any work until `Observable.subscribe` starts the work.


### RxJava & Related Types

##### Observable

Stream that supports async and synchronous push. It does *not* support interactive flow control (`request(n)`).

Usable for:

- sync or async
- push
- 0, 1, many or infinite items

Flow control support:

- buffering, sampling, throttling, windowing, dropping, etc
- temporal and count-based strategies

*Type Signature*

```java
class Observable<T> {
  void subscribe(Observer<T> observer);

  interface Observer<T> {
    void onNext(T t);
    void onError(Throwable t);
    void onComplete();
    void onSubscribe(Disposable d);
  }
}
```

The rule for using this type signature is:

> onSubscribe onNext* (onError | onComplete)?


##### Flowable

Stream that supports async and synchronous push and pull. It supports interactive flow control (`request(n)`).

Usable for:

- pull sources
- push Observables with backpressure strategy (ie. `Observable.toFlowable(onBackpressureStrategy)`)
- sync or async
- 0, 1, many or infinite items

Flow control support:

- buffering, sampling, throttling, windowing, dropping, etc
- temporal and count-based strategies
- `request(n)` consumer demand signal
	- for pull-based sources, this allows batched "async pull"
	- for push-based sources, this allows backpressure signals to conditionally apply strategies (i.e. drop, first, buffer, sample, fail, etc)

You get a flowable from:

- Converting a Observable with a backpressure strategy
- Create from sync/async OnSubscribe API (which participate in backpressure semantics)

*Type Signature*

```java
class Flowable<T> implements Flow.Publisher<T>, io.reactivestreams.Publisher<T> {
  void subscribe(Subscriber<T> subscriber);

  interface Subscriber<T> implements Flow.Subscriber<T>, io.reactivestreams.Subscriber<T> {
    void onNext(T t);
    void onError(Throwable t);
    void onComplete();
    void onSubscribe(Flowable.Subscription d);
  }

  interface Subscription implements Flow.Subscription, io.reactivestreams.Subscription {
    void cancel();
    void request(long n);
  }
}
```

*NOTE: To support `Flow.Publisher` in Java 9+ without breaking Java 7+ compatibility, we want to use the [multi-release jar file support](http://openjdk.java.net/jeps/238).*

The rule for using this type signature is:

> onSubscribe onNext* (onError | onComplete)?


##### Single

Lazy representation of a single response (lazy equivalent of `Future`/`Promise`).

Usable for:

- pull sources
- push sources being windowed or flow controlled (such as `window(1)` or `take(1)`)
- sync or async
- 1 item

Flow control:

- Not applicable (don't subscribe if the single response is not wanted)

*Type Signature*

```java
class Single<T> {
  void subscribe(Single.Subscriber<T> subscriber);

  interface Subscriber<T> {
    void onSuccess(T t);
    void onError(Throwable t);
    void onSubscribe(Disposable d);
  }
}
```

> onSubscribe (onError | onSuccess)?


##### Completable

Lazy representation of a unit of work that can complete or fail

- Semantic equivalent of `Observable.empty().doOnSubscribe()`. 
- Alternative for scenarios often represented with types such as `Single<Void>` or `Observable<Void>`.

Usable for:

- sync or async
- 0 items

*Type Signature*

```java
class Completable {
  void subscribe(Completable.Subscriber subscriber);

  interface Subscriber {
    void onComplete();
    void onError(Throwable t);
    void onSubscribe(Disposable d);
  }
}
```

> onSubscribe (onError | onComplete)?

##### Observer

Reactive consumer of events (without consumer-driven flow control). Involved in subscription lifecycle to allow unsubscription.

##### Publisher

Interactive producer of events (with flow control). Implemented by `Flowable`.

[Reactive Streams producer](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#1-publisher-code) of data.

##### Subscriber

Interactive consumer of events (with consumer-driven flow control). Involved in subscription lifecycle to allow unsubscription.

[Reactive Streams consumer](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#2-subscriber-code) of data.

##### Subscription

[Reactive Streams state](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#3-subscription-code) of subscription supporting flow control and cancellation.

`Disposable` is a similar type used for lifecycle management on the `Observable` type without interactive flow control. 

##### Processor

[Reactive Streams operator](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#4processor-code) for defining behavior between `Publisher` and `Subscriber`. It must obey the contracts of `Publisher` and `Subscriber`, meaning it is sequential, serialized, and must obey `request(n)` flow control.

##### Subject

A "hot", push-based data source that allows a producer to emit events to it and consumers to subscribe to events in a multicast manner. It is "hot" because consumers subscribing to it does not cause side-effects, or affect the data flow in any way. It is push-based and reactive because the producer is fully in charge.

A `Subject` is used to decouple unsubscription. Termination is fully in the control of the producer. `onError` and `onComplete` are still terminal events.
`Subject`s are stateful and retain their terminal state (for replaying to all/future subscribers).

Relation to Reactive Streams:

- It can not implement Reactive Streams `Publisher` unless it is created with a default consumer-driven flow control strategy.
- It can not implement `Processor` since a `Processor` must compose `request(n)` which can not be done with multicasting or push.

Here is an approach to converting from a `Subject` to Reactive Streams types by adding a default flow control strategy:

```java
Subject s = PublishSubject.create();
// convert to Publisher with backpressure strategy
Publisher p = s.toPublisher(onBackpressureStrategy);

// now the request(n) semantics are handled by default
p.subscribe(subscriber1); 
p.subscribe(subscriber2);
```

In this example, `subscriber1` and `subscriber2` can consume at different rates, `request(n)` will propagate to the provided  `onBackpressureStrategy`, not the original `Subject` which can't propagate `request(n)` upstream.


##### Disposable

A type representing work that can be cancelled or disposed.

Examples:

- An `Observable.subscribe` passes a `Disposable` to the `Observable.onSubscribe` to allow the `Observer` to dispose of the subscription. 
- A `Scheduler` returns a `Disposable` that you use for disposing of the `Scheduler`.

`Subscription` is a similar type used for lifecycle management on the `Flowable` type with interactive flow control.

##### Operator

An operator follows a specific lifecycle (union of the producer/consumer contract).

- It must propagate the `subscribe` event upstream (to the producer).
- It must obey the RxJava contract (serialize all events, `onError`/`onComplete` are terminal).
- If it has resources to cleanup it is responsible for watching `onError`, `onComplete`, and `cancel/dispose`, and doing the necessary cleanup.
- It must propagate the `cancel/dispose` upstream.

In the addition of the previous rules, an operator for `Flowable`:

- It must propagate/negotiate the `request(n)` event.


### Creation

Creation of the various types should be exposed through factory methods that provide safe construction.

```java
Flowable.create(SyncGenerator generator)

Flowable.create(AsyncGenerator generator)

Observable<T>.create(OnSubscribe<Observer<T>> onSubscribe)

Single<T>.create(OnSubscribe<Single.Subscriber<T>> onSubscribe)

Completable<T>.create(OnSubscribe<Completable.Subscriber<T>> onSubscribe)
```

### Terminal behavior

A producer can terminate a stream by emitting `onComplete` or `onError`. A consumer can terminate a stream by calling `cancel`/`dispose`.

Any resource cleanup of the source or operators must account for any of these three termination events. In other words, if an operator needs cleanup, then it should register the cleanup callback with `cancel`/`dispose`, `onError` and `onComplete`.

The final `subscribe` will *not* invoke `cancel`/`dispose` after receiving an `onComplete` or `onError`.

### JVM target and source compatibility

The 2.x version will target JDK6+ but the source will be using some of the Java 8 features.
We'll use the retrolambda project to generate JDK6 Byte-code from the Java 8 sources.
The only Java 8 features we intend to use are: lambda expressions, method references and try-with-resources statements.
Default methods may be considered if retrolambda support reach an acceptable level of maturity.

It still up to discussion to know how we will generate multiple targets?
Options are:

- two artifcat ids: rxjava-jdk6, rxjava-jdk8
- classifiers: jdk6, jdk8

The intent is to let Android users consume the new version of RxJava.
(Not sure if JDK6 is the appropriate requirement, JDK7+ is maybe sufficient).

##### Custom Observable, Single, Completable, or Flowable (To be confirmed)

An implementation of an Observable which can be subscribed to with an `Observer`. Custom (`Observable`|`Single`|`Completable`|`Flowable`) classes would implement the interface `Consumable<S>` where the generic type `S` is a `Flowable.Subscriber<T>`, `Observable.Observer<T>`, `Completable.Subscriber<T>`, or a `Single.Subscriber<T>` depending on which semantics the custom class will follow. This choice would also affect the kinds of observables the custom class could interop with. For instance, `Flowable#merge` could operate over the standard `Flowable` or any custom class which implements `Consumable<Flowable.Subscriber<T>>`. 

For more information see the proof of concept project [Consumable](https://github.com/stealthcode/Consumable). 

### Fusion (To be confirmed)

We intend to enable operator fusion, but we don't have any specification yet. Nothing we do here should prevent the implementation of fusion.
