## RxJava v2 Design

Terminology, principles, contracts, and other aspects of the design of RxJava v2.

### Terminology & Definitions

##### Hot

When used to refer to a data source (such as an `Observable`), it means it does not have side-effects when subscribed to.

For example, an `Observable` of mouse events. Subscribing to that `Observable` does not cause the mouse events, but starts receiving them.

(Note: Yes, there are *some* side-effects of adding a listener, but they are inconsequential as far as the 'hot' usage is concerned).

##### Cold

When used to refer to a data source (such as an `Observable`), it means it has side-effects when subscribed to.

For example, an `Observable` of data from a remote API (such as an RPC call). Each time that `Observable` is subscribed to causes a new network call to occur.

##### Reactive

Producer is in charge. Consumer has to do whatever it needs to keep up.

##### Interactive

Consumer is in charge. Producer has to do whatever it needs to keep up.

##### Push

Producer emits when it wishes to. Related to "reactive". Callbacks are an instance of push.

##### Pull

Consumer requests data when it wishes to. Related to "interactive". An `Iterable` is an instance of pull.

##### Async Pull

Consumer requests data when it wishes, and the data is then pushed when the producer wishes to. The Reactive Streams `Publisher` is an instance of "async pull", as is the 'AsyncEnumerable' in .Net.

### RxJava & Related Types

##### Observable

Stream that supports async and synchronous push. It does not support interactive flow control (`request(n)`).

Usable for:

- hot and cold sources
- sync or async
- push
- 0, 1, many or infinite items

Flow control support:

- buffering, sampling, throttling, windowing, dropping, etc
- temporal and count-based strategies

##### Flowable

Stream that supports async and synchronous push and pull. It supports interactive flow control (`request(n)`).

Usable for:

- hot and cold sources
- sync or async
- push
- pull
- 0, 1, many or infinite items

Flow control support:

- buffering, sampling, throttling, windowing, dropping, etc
- temporal and count-based strategies
- `request(n)` consumer demand signal
  - for pull-based sources, this allows batched "async pull"
  - for push-based sources, this allows backpressure signals to conditionally apply strategies (i.e. drop, buffer, sample, fail, etc)

##### Observer

Consumer of events without flow control.

##### Publisher

[Reactive Streams producer](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#1-publisher-code) of data

##### Subscriber

[Reactive Streams consumer](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#2-subscriber-code) of data.

##### Subscription

[Reactive Streams state](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#3-subscription-code) of subscription supporting flow control and cancellation.

##### Processor

[Reactive Streams operator](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#4processor-code) for defining behavior between `Publisher` and `Subscriber`. It must obey the contracts of `Publisher` and `Subscriber`, meaning it is sequential, serialized, and must obey `request(n)` flow control.

##### Subject

A "hot" data source that allows a producer to emit events and consumers to receive events in a multicast manner.

It is "hot" because consumers subscribing to it does not cause side-effects, or affect the data flow in any way. It is push and reactive because the producer is fully in charge.

##### Disposable

A type representing work that can be cancelled or disposed.


