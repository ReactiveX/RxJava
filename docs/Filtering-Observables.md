This page shows operators you can use to filter and select items emitted by Observables.

* [**`filter( )`**](http://reactivex.io/documentation/operators/filter.html) — filter items emitted by an Observable
* [**`takeLast( )`**](http://reactivex.io/documentation/operators/takelast.html) — only emit the last _n_ items emitted by an Observable
* [**`last( )`**](http://reactivex.io/documentation/operators/last.html) — emit only the last item emitted by an Observable
* [**`lastOrDefault( )`**](http://reactivex.io/documentation/operators/last.html) — emit only the last item emitted by an Observable, or a default value if the source Observable is empty
* [**`takeLastBuffer( )`**](http://reactivex.io/documentation/operators/takelast.html) — emit the last _n_ items emitted by an Observable, as a single list item
* [**`skip( )`**](http://reactivex.io/documentation/operators/skip.html) — ignore the first _n_ items emitted by an Observable
* [**`skipLast( )`**](http://reactivex.io/documentation/operators/skiplast.html) — ignore the last _n_ items emitted by an Observable
* [**`take( )`**](http://reactivex.io/documentation/operators/take.html) — emit only the first _n_ items emitted by an Observable
* [**`first( )` and `takeFirst( )`**](http://reactivex.io/documentation/operators/first.html) — emit only the first item emitted by an Observable, or the first item that meets some condition
* [**`firstOrDefault( )`**](http://reactivex.io/documentation/operators/first.html) — emit only the first item emitted by an Observable, or the first item that meets some condition, or a default value if the source Observable is empty
* [**`elementAt( )`**](http://reactivex.io/documentation/operators/elementat.html) — emit item _n_ emitted by the source Observable
* [**`elementAtOrDefault( )`**](http://reactivex.io/documentation/operators/elementat.html) — emit item _n_ emitted by the source Observable, or a default item if the source Observable emits fewer than _n_ items
* [**`sample( )` or `throttleLast( )`**](http://reactivex.io/documentation/operators/sample.html) — emit the most recent items emitted by an Observable within periodic time intervals
* [**`throttleFirst( )`**](http://reactivex.io/documentation/operators/sample.html) — emit the first items emitted by an Observable within periodic time intervals
* [**`throttleWithTimeout( )` or `debounce( )`**](http://reactivex.io/documentation/operators/debounce.html) — only emit an item from the source Observable after a particular timespan has passed without the Observable emitting any other items
* [**`timeout( )`**](http://reactivex.io/documentation/operators/timeout.html) — emit items from a source Observable, but issue an exception if no item is emitted in a specified timespan
* [**`distinct( )`**](http://reactivex.io/documentation/operators/distinct.html) — suppress duplicate items emitted by the source Observable
* [**`distinctUntilChanged( )`**](http://reactivex.io/documentation/operators/distinct.html) — suppress duplicate consecutive items emitted by the source Observable
* [**`ofType( )`**](http://reactivex.io/documentation/operators/filter.html) — emit only those items from the source Observable that are of a particular class
* [**`ignoreElements( )`**](http://reactivex.io/documentation/operators/ignoreelements.html) — discard the items emitted by the source Observable and only pass through the error or completed notification
