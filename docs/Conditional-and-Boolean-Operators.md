This section explains operators with which you conditionally emit or transform Observables, or can do boolean evaluations of them:

### Conditional Operators
* [**`amb( )`**](http://reactivex.io/documentation/operators/amb.html) — given two or more source Observables, emits all of the items from the first of these Observables to emit an item
* [**`defaultIfEmpty( )`**](http://reactivex.io/documentation/operators/defaultifempty.html) — emit items from the source Observable, or emit a default item if the source Observable completes after emitting no items
* (`rxjava-computation-expressions`) [**`doWhile( )`**](http://reactivex.io/documentation/operators/repeat.html) — emit the source Observable's sequence, and then repeat the sequence as long as a condition remains true
* (`rxjava-computation-expressions`) [**`ifThen( )`**](http://reactivex.io/documentation/operators/defer.html) — only emit the source Observable's sequence if a condition is true, otherwise emit an empty or default sequence
* [**`skipUntil( )`**](http://reactivex.io/documentation/operators/skipuntil.html) — discard items emitted by a source Observable until a second Observable emits an item, then emit the remainder of the source Observable's items
* [**`skipWhile( )`**](http://reactivex.io/documentation/operators/skipwhile.html) — discard items emitted by an Observable until a specified condition is false, then emit the remainder
* (`rxjava-computation-expressions`) [**`switchCase( )`**](http://reactivex.io/documentation/operators/defer.html) — emit the sequence from a particular Observable based on the results of an evaluation
* [**`takeUntil( )`**](http://reactivex.io/documentation/operators/takeuntil.html) — emits the items from the source Observable until a second Observable emits an item or issues a notification
* [**`takeWhile( )` and `takeWhileWithIndex( )`**](http://reactivex.io/documentation/operators/takewhile.html) — emit items emitted by an Observable as long as a specified condition is true, then skip the remainder
* (`rxjava-computation-expressions`) [**`whileDo( )`**](http://reactivex.io/documentation/operators/repeat.html) — if a condition is true, emit the source Observable's sequence and then repeat the sequence as long as the condition remains true

> (`rxjava-computation-expressions`) — indicates that this operator is currently part of the optional `rxjava-computation-expressions` package under `rxjava-contrib` and is not included with the standard RxJava set of operators

### Boolean Operators
* [**`all( )`**](http://reactivex.io/documentation/operators/all.html) — determine whether all items emitted by an Observable meet some criteria
* [**`contains( )`**](http://reactivex.io/documentation/operators/contains.html) — determine whether an Observable emits a particular item or not
* [**`exists( )` and `isEmpty( )`**](http://reactivex.io/documentation/operators/contains.html) — determine whether an Observable emits any items or not
* [**`sequenceEqual( )`**](http://reactivex.io/documentation/operators/sequenceequal.html) — test the equality of the sequences emitted by two Observables
