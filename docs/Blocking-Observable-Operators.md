This section explains the [`BlockingObservable`](http://reactivex.io/RxJava/javadoc/rx/observables/BlockingObservable.html) subclass. A Blocking Observable extends the ordinary Observable class by providing a set of operators on the items emitted by the Observable that block.

To transform an `Observable` into a `BlockingObservable`, use the [`Observable.toBlocking( )`](http://reactivex.io/RxJava/javadoc/rx/Observable.html#toBlocking()) method or the [`BlockingObservable.from( )`](http://reactivex.io/RxJava/javadoc/rx/observables/BlockingObservable.html#from(rx.Observable)) method.

* [**`forEach( )`**](http://reactivex.io/documentation/operators/subscribe.html) — invoke a function on each item emitted by the Observable; block until the Observable completes
* [**`first( )`**](http://reactivex.io/documentation/operators/first.html) — block until the Observable emits an item, then return the first item emitted by the Observable
* [**`firstOrDefault( )`**](http://reactivex.io/documentation/operators/first.html) — block until the Observable emits an item or completes, then return the first item emitted by the Observable or a default item if the Observable did not emit an item
* [**`last( )`**](http://reactivex.io/documentation/operators/last.html) — block until the Observable completes, then return the last item emitted by the Observable
* [**`lastOrDefault( )`**](http://reactivex.io/documentation/operators/last.html) — block until the Observable completes, then return the last item emitted by the Observable or a default item if there is no last item
* [**`mostRecent( )`**](http://reactivex.io/documentation/operators/first.html) — returns an iterable that always returns the item most recently emitted by the Observable
* [**`next( )`**](http://reactivex.io/documentation/operators/takelast.html) — returns an iterable that blocks until the Observable emits another item, then returns that item
* [**`latest( )`**](http://reactivex.io/documentation/operators/first.html) — returns an iterable that blocks until or unless the Observable emits an item that has not been returned by the iterable, then returns that item
* [**`single( )`**](http://reactivex.io/documentation/operators/first.html) — if the Observable completes after emitting a single item, return that item, otherwise throw an exception
* [**`singleOrDefault( )`**](http://reactivex.io/documentation/operators/first.html) — if the Observable completes after emitting a single item, return that item, otherwise return a default item
* [**`toFuture( )`**](http://reactivex.io/documentation/operators/to.html) — convert the Observable into a Future
* [**`toIterable( )`**](http://reactivex.io/documentation/operators/to.html) — convert the sequence emitted by the Observable into an Iterable
* [**`getIterator( )`**](http://reactivex.io/documentation/operators/to.html) — convert the sequence emitted by the Observable into an Iterator

> This documentation accompanies its explanations with a modified form of "marble diagrams." Here is how these marble diagrams represent Blocking Observables:

<img src="/ReactiveX/RxJava/wiki/images/rx-operators/B.legend.png" width="640" height="301" />

#### see also:
* javadoc: <a href="http://reactivex.io/RxJava/javadoc/rx/observables/BlockingObservable.html">`BlockingObservable`</a>
* javadoc: <a href="http://reactivex.io/RxJava/javadoc/rx/Observable.html#toBlocking()">`toBlocking()`</a>
* javadoc: <a href="http://reactivex.io/RxJava/javadoc/rx/observables/BlockingObservable.html#from(rx.Observable)">`BlockingObservable.from()`</a>

## Appendix: similar blocking and non-blocking operators

<table>
 <thead>
  <tr><th rowspan="2">operator</th><th colspan="3">result when it acts on</th><th rowspan="2">equivalent in Rx.NET</th></tr>
  <tr><th>Observable that emits multiple items</th><th>Observable that emits one item</th><th>Observable that emits no items</th></tr>
 </thead>
 <tbody>
  <tr><td><code>Observable.first</code></td><td>the first item</td><td>the single item</td><td><i>NoSuchElement</i></td><td><code>firstAsync</code></td></tr>
  <tr><td><code>BlockingObservable.first</code></td><td>the first item</td><td>the single item</td><td><i>NoSuchElement</i></td><td><code>first</code></td></tr>
  <tr><td><code>Observable.firstOrDefault</code></td><td>the first item</td><td>the single item</td><td>the default item</td><td><code>firstOrDefaultAsync</code></td></tr>
  <tr><td><code>BlockingObservable.firstOrDefault</code></td><td>the first item</td><td>the single item</td><td>the default item</td><td><code>firstOrDefault</code></td></tr>
  <tr><td><code>Observable.last</code></td><td>the last item</td><td>the single item</td><td><i>NoSuchElement</i></td><td><code>lastAsync</code></td></tr>
  <tr><td><code>BlockingObservable.last</code></td><td>the last item</td><td>the single item</td><td><i>NoSuchElement</i></td><td><code>last</code></td></tr>
  <tr><td><code>Observable.lastOrDefault</code></td><td>the last item</td><td>the single item</td><td>the default item</td><td><code>lastOrDefaultAsync</code></td></tr>
  <tr><td><code>BlockingObservable.lastOrDefault</code></td><td>the last item</td><td>the single item</td><td>the default item</td><td><code>lastOrDefault</code></td></tr>
  <tr><td><code>Observable.single</code></td><td><i>Illegal Argument</i></td><td>the single item</td><td><i>NoSuchElement</i></td><td><code>singleAsync</code></td></tr>
  <tr><td><code>BlockingObservable.single</code></td><td><i>Illegal Argument</i></td><td>the single item</td><td><i>NoSuchElement</i></td><td><code>single</code></td></tr>
  <tr><td><code>Observable.singleOrDefault</code></td><td><i>Illegal Argument</i></td><td>the single item</td><td>the default item</td><td><code>singleOrDefaultAsync</code></td></tr>
  <tr><td><code>BlockingObservable.singleOrDefault</code></td><td><i>Illegal Argument</i></td><td>the single item</td><td>the default item</td><td><code>singleOrDefault</code></td></tr>
 </tbody>
</table>