This page shows operators with which you can transform items that are emitted by an Observable.

* [**`map( )`**](http://reactivex.io/documentation/operators/map.html) — transform the items emitted by an Observable by applying a function to each of them
* [**`flatMap( )`, `concatMap( )`, and `flatMapIterable( )`**](http://reactivex.io/documentation/operators/flatmap.html) — transform the items emitted by an Observable into Observables (or Iterables), then flatten this into a single Observable
* [**`switchMap( )`**](http://reactivex.io/documentation/operators/flatmap.html) — transform the items emitted by an Observable into Observables, and mirror those items emitted by the most-recently transformed Observable
* [**`scan( )`**](http://reactivex.io/documentation/operators/scan.html) — apply a function to each item emitted by an Observable, sequentially, and emit each successive value
* [**`groupBy( )`**](http://reactivex.io/documentation/operators/groupby.html) — divide an Observable into a set of Observables that emit groups of items from the original Observable, organized by key
* [**`buffer( )`**](http://reactivex.io/documentation/operators/buffer.html) — periodically gather items from an Observable into bundles and emit these bundles rather than emitting the items one at a time 
* [**`window( )`**](http://reactivex.io/documentation/operators/window.html) — periodically subdivide items from an Observable into Observable windows and emit these windows rather than emitting the items one at a time 
* [**`cast( )`**](http://reactivex.io/documentation/operators/map.html) — cast all items from the source Observable into a particular type before reemitting them