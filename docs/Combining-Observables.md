This section explains operators you can use to combine multiple Observables.

* [**`startWith( )`**](http://reactivex.io/documentation/operators/startwith.html) — emit a specified sequence of items before beginning to emit the items from the Observable
* [**`merge( )`**](http://reactivex.io/documentation/operators/merge.html) — combine multiple Observables into one
* [**`mergeDelayError( )`**](http://reactivex.io/documentation/operators/merge.html) — combine multiple Observables into one, allowing error-free Observables to continue before propagating errors
* [**`zip( )`**](http://reactivex.io/documentation/operators/zip.html) — combine sets of items emitted by two or more Observables together via a specified function and emit items based on the results of this function
* (`rxjava-joins`) [**`and( )`, `then( )`, and `when( )`**](http://reactivex.io/documentation/operators/and-then-when.html) — combine sets of items emitted by two or more Observables by means of `Pattern` and `Plan` intermediaries
* [**`combineLatest( )`**](http://reactivex.io/documentation/operators/combinelatest.html) — when an item is emitted by either of two Observables, combine the latest item emitted by each Observable via a specified function and emit items based on the results of this function
* [**`join( )` and `groupJoin( )`**](http://reactivex.io/documentation/operators/join.html) — combine the items emitted by two Observables whenever one item from one Observable falls within a window of duration specified by an item emitted by the other Observable
* [**`switchOnNext( )`**](http://reactivex.io/documentation/operators/switch.html) — convert an Observable that emits Observables into a single Observable that emits the items emitted by the most-recently emitted of those Observables

> (`rxjava-joins`) — indicates that this operator is currently part of the optional `rxjava-joins` package under `rxjava-contrib` and is not included with the standard RxJava set of operators
