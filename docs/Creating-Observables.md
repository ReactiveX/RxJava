This page shows methods that create Observables.

* [**`just( )`**](http://reactivex.io/documentation/operators/just.html) — convert an object or several objects into an Observable that emits that object or those objects
* [**`from( )`**](http://reactivex.io/documentation/operators/from.html) — convert an Iterable, a Future, or an Array into an Observable
* [**`create( )`**](http://reactivex.io/documentation/operators/create.html) — **advanced use only!** create an Observable from scratch by means of a function, consider `fromEmitter` instead
* [**`fromEmitter()`**](http://reactivex.io/RxJava/javadoc/rx/Observable.html#fromEmitter(rx.functions.Action1,%20rx.AsyncEmitter.BackpressureMode)) — create safe, backpressure-enabled, unsubscription-supporting Observable via a function and push events.
* [**`defer( )`**](http://reactivex.io/documentation/operators/defer.html) — do not create the Observable until a Subscriber subscribes; create a fresh Observable on each subscription
* [**`range( )`**](http://reactivex.io/documentation/operators/range.html) — create an Observable that emits a range of sequential integers
* [**`interval( )`**](http://reactivex.io/documentation/operators/interval.html) — create an Observable that emits a sequence of integers spaced by a given time interval
* [**`timer( )`**](http://reactivex.io/documentation/operators/timer.html) — create an Observable that emits a single item after a given delay
* [**`empty( )`**](http://reactivex.io/documentation/operators/empty-never-throw.html) — create an Observable that emits nothing and then completes
* [**`error( )`**](http://reactivex.io/documentation/operators/empty-never-throw.html) — create an Observable that emits nothing and then signals an error
* [**`never( )`**](http://reactivex.io/documentation/operators/empty-never-throw.html) — create an Observable that emits nothing at all
