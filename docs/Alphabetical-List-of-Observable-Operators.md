* **`aggregate( )`** — _see [**`reduce( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#reduce)_
* [**`all( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#boolean-operators) — determine whether all items emitted by an Observable meet some criteria
* [**`amb( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — given two or more source Observables, emits all of the items from the first of these Observables to emit an item
* **`ambWith( )`** — _instance version of [**`amb( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators)_
* [**`and( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#and-then-and-when) — combine the emissions from two or more source Observables into a `Pattern` (`rxjava-joins`)
* **`apply( )`** (scala) — _see [**`create( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#create)_
* **`asObservable( )`** (kotlin) — _see [**`from( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#from) (et al.)_
* [**`asyncAction( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators#toasync-or-asyncaction-or-asyncfunc) — convert an Action into an Observable that executes the Action and emits its return value (`rxjava-async`)
* [**`asyncFunc( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators#toasync-or-asyncaction-or-asyncfunc) — convert a function into an Observable that executes the function and emits its return value (`rxjava-async`)
* [**`averageDouble( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#averagedouble) — calculates the average of Doubles emitted by an Observable and emits this average (`rxjava-math`)
* [**`averageFloat( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#averagefloat) — calculates the average of Floats emitted by an Observable and emits this average (`rxjava-math`)
* [**`averageInteger( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators) — calculates the average of Integers emitted by an Observable and emits this average (`rxjava-math`)
* [**`averageLong( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators) — calculates the average of Longs emitted by an Observable and emits this average (`rxjava-math`)
* **`blocking( )`** (clojure) — _see [**`toBlocking( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators)_
* [**`buffer( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#buffer) — periodically gather items from an Observable into bundles and emit these bundles rather than emitting the items one at a time
* [**`byLine( )`**](https://github.com/ReactiveX/RxJava/wiki/String-Observables) (`StringObservable`) — converts an Observable of Strings into an Observable of Lines by treating the source sequence as a stream and splitting it on line-endings
* [**`cache( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — remember the sequence of items emitted by the Observable and emit the same sequence to future Subscribers
* [**`cast( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#cast) — cast all items from the source Observable into a particular type before reemitting them
* **`catch( )`** (clojure) — _see [**`onErrorResumeNext( )`**](https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators#onerrorresumenext)_
* [**`chunkify( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#chunkify) — returns an iterable that periodically returns a list of items emitted by the source Observable since the last list (⁇)
* [**`collect( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#collect) — collects items emitted by the source Observable into a single mutable data structure and returns an Observable that emits this structure
* [**`combineLatest( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#combinelatest) — when an item is emitted by either of two Observables, combine the latest item emitted by each Observable via a specified function and emit items based on the results of this function
* **`combineLatestWith( )`** (scala) — _instance version of [**`combineLatest( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#combinelatest)_
* [**`concat( )`**](http://reactivex.io/documentation/operators/concat.html) — concatenate two or more Observables sequentially
* [**`concatMap( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#concatmap) — transform the items emitted by an Observable into Observables, then flatten this into a single Observable, without interleaving
* **`concatWith( )`** — _instance version of [**`concat( )`**](http://reactivex.io/documentation/operators/concat.html)_
* [**`connect( )`**](https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators) — instructs a Connectable Observable to begin emitting items
* **`cons( )`** (clojure) — _see [**`concat( )`**](http://reactivex.io/documentation/operators/concat.html)_
* [**`contains( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#boolean-operators) — determine whether an Observable emits a particular item or not
* [**`count( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#count) — counts the number of items emitted by an Observable and emits this count
* [**`countLong( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#count) — counts the number of items emitted by an Observable and emits this count
* [**`create( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#create) — create an Observable from scratch by means of a function
* **`cycle( )`** (clojure) — _see [**`repeat( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables)_
* [**`debounce( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#debounce) — only emit an item from the source Observable after a particular timespan has passed without the Observable emitting any other items
* [**`decode( )`**](https://github.com/ReactiveX/RxJava/wiki/String-Observables) (`StringObservable`) — convert a stream of multibyte characters into an Observable that emits byte arrays that respect character boundaries
* [**`defaultIfEmpty( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — emit items from the source Observable, or emit a default item if the source Observable completes after emitting no items
* [**`defer( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#defer) — do not create the Observable until a Subscriber subscribes; create a fresh Observable on each subscription
* [**`deferFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) — convert a Future that returns an Observable into an Observable, but do not attempt to get the Observable that the Future returns until a Subscriber subscribes (`rxjava-async`)
* [**`deferCancellableFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#fromcancellablefuture-startcancellablefuture-and-defercancellablefuture) — convert a Future that returns an Observable into an Observable in a way that monitors the subscription status of the Observable to determine whether to halt work on the Future, but do not attempt to get the returned Observable until a Subscriber subscribes (⁇)(`rxjava-async`)
* [**`delay( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — shift the emissions from an Observable forward in time by a specified amount
* [**`dematerialize( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — convert a materialized Observable back into its non-materialized form
* [**`distinct( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#distinct) — suppress duplicate items emitted by the source Observable
* [**`distinctUntilChanged( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#distinctuntilchanged) — suppress duplicate consecutive items emitted by the source Observable
* **`do( )`** (clojure) — _see [**`doOnEach( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators)_
* [**`doOnCompleted( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — register an action to take when an Observable completes successfully
* [**`doOnEach( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — register an action to take whenever an Observable emits an item
* [**`doOnError( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — register an action to take when an Observable completes with an error
* **`doOnNext( )`** — _see [**`doOnEach( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators)_
* **`doOnRequest( )`** — register an action to take when items are requested from an Observable via reactive-pull backpressure (⁇)
* [**`doOnSubscribe( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — register an action to take when an observer subscribes to an Observable
* [**`doOnTerminate( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — register an action to take when an Observable completes, either successfully or with an error
* [**`doOnUnsubscribe( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — register an action to take when an observer unsubscribes from an Observable
* [**`doWhile( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators) — emit the source Observable's sequence, and then repeat the sequence as long as a condition remains true (`contrib-computation-expressions`)
* **`drop( )`** (scala/clojure) — _see [**`skip( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#skip)_
* **`dropRight( )`** (scala) — _see [**`skipLast( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#skiplast)_
* **`dropUntil( )`** (scala) — _see [**`skipUntil( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators)_
* **`dropWhile( )`** (scala) — _see [**`skipWhile( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators)_
* **`drop-while( )`** (clojure) — _see [**`skipWhile( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#skipwhile)_
* [**`elementAt( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#elementat) — emit item _n_ emitted by the source Observable
* [**`elementAtOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) — emit item _n_ emitted by the source Observable, or a default item if the source Observable emits fewer than _n_ items
* [**`empty( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#empty) — create an Observable that emits nothing and then completes
* [**`encode( )`**](https://github.com/ReactiveX/RxJava/wiki/String-Observables) (`StringObservable`) — transform an Observable that emits strings into an Observable that emits byte arrays that respect character boundaries of multibyte characters in the original strings
* [**`error( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#error) — create an Observable that emits nothing and then signals an error
* **`every( )`** (clojure) — _see [**`all( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#boolean-operators)_
* [**`exists( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#boolean-operators) — determine whether an Observable emits any items or not
* [**`filter( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#filter) — filter items emitted by an Observable
* **`finally( )`** (clojure) — _see [**`finallyDo( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators)_
* **`filterNot( )`** (scala) — _see [**`filter( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#filter)_
* [**`finallyDo( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — register an action to take when an Observable completes
* [**`first( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#first) (`Observable`) — emit only the first item emitted by an Observable, or the first item that meets some condition
* [**`first( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`) — emit only the first item emitted by an Observable, or the first item that meets some condition
* [**`firstOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) (`Observable`) — emit only the first item emitted by an Observable, or the first item that meets some condition, or a default value if the source Observable is empty
* [**`firstOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`) — emit only the first item emitted by an Observable, or the first item that meets some condition, or a default value if the source Observable is empty
* **`firstOrElse( )`** (scala) — _see [**`firstOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) or [**`firstOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`)_
* [**`flatMap( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#flatmap) — transform the items emitted by an Observable into Observables, then flatten this into a single Observable
* [**`flatMapIterable( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#flatmapiterable) — create Iterables corresponding to each emission from a source Observable and merge the results into a single Observable
* **`flatMapIterableWith( )`** (scala) — _instance version of [**`flatMapIterable( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#flatmapiterable)_
* **`flatMapWith( )`** (scala) — _instance version of [**`flatmap( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#flatmap)_
* **`flatten( )`** (scala) — _see [**`merge( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#merge)_
* **`flattenDelayError( )`** (scala) — _see [**`mergeDelayError( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#mergedelayerror)_
* **`foldLeft( )`** (scala) — _see [**`reduce( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#reduce)_
* **`forall( )`** (scala) — _see [**`all( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#boolean-operators)_
* **`forEach( )`** (`Observable`) — _see [**`subscribe( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable)_
* [**`forEach( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`) — invoke a function on each item emitted by the Observable; block until the Observable completes
* [**`forEachFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) (`Async`) — pass Subscriber methods to an Observable but also have it behave like a Future that blocks until it completes (`rxjava-async`)
* [**`forEachFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#foreachfuture) (`BlockingObservable`)— create a futureTask that will invoke a specified function on each item emitted by an Observable (⁇)
* [**`forIterable( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#foriterable) — apply a function to the elements of an Iterable to create Observables which are then concatenated (⁇)
* [**`from( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#from) — convert an Iterable, a Future, or an Array into an Observable
* [**`from( )`**](https://github.com/ReactiveX/RxJava/wiki/String-Observables) (`StringObservable`) — convert a stream of characters or a Reader into an Observable that emits byte arrays or Strings
* [**`fromAction( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) — convert an Action into an Observable that invokes the action and emits its result when a Subscriber subscribes (`rxjava-async`)
* [**`fromCallable( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) — convert a Callable into an Observable that invokes the callable and emits its result or exception when a Subscriber subscribes (`rxjava-async`)
* [**`fromCancellableFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#fromcancellablefuture-startcancellablefuture-and-defercancellablefuture) — convert a Future into an Observable in a way that monitors the subscription status of the Observable to determine whether to halt work on the Future, but do not attempt to get the Future's value until a Subscriber subscribes (⁇)(`rxjava-async`)
* **`fromFunc0( )`** — _see [**`fromCallable( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) (`rxjava-async`)_
* [**`fromFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#fromfuture) — convert a Future into an Observable, but do not attempt to get the Future's value until a Subscriber subscribes (⁇)
* [**`fromRunnable( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators#fromrunnable) — convert a Runnable into an Observable that invokes the runable and emits its result when a Subscriber subscribes (`rxjava-async`)
* [**`generate( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#generate-and-generateabsolutetime) — create an Observable that emits a sequence of items as generated by a function of your choosing (⁇)
* [**`generateAbsoluteTime( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#generate-and-generateabsolutetime) — create an Observable that emits a sequence of items as generated by a function of your choosing, with each item emitted at an item-specific time (⁇)
* **`generator( )`** (clojure) — _see [**`generate( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#generate-and-generateabsolutetime)_
* [**`getIterator( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) — convert the sequence emitted by the Observable into an Iterator
* [**`groupBy( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#groupby) — divide an Observable into a set of Observables that emit groups of items from the original Observable, organized by key
* **`group-by( )`** (clojure) — _see [**`groupBy( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#groupby)_
* [**`groupByUntil( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators) — a variant of the [`groupBy( )`](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#groupby) operator that closes any open GroupedObservable upon a signal from another Observable (⁇)
* [**`groupJoin( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#joins) — combine the items emitted by two Observables whenever one item from one Observable falls within a window of duration specified by an item emitted by the other Observable
* **`head( )`** (scala) — _see [**`first( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`)_
* **`headOption( )`** (scala) — _see [**`firstOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) or [**`firstOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`)_
* **`headOrElse( )`** (scala) — _see [**`firstOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) or [**`firstOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`)_
* [**`ifThen( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — only emit the source Observable's sequence if a condition is true, otherwise emit an empty or default sequence (`contrib-computation-expressions`)
* [**`ignoreElements( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#ignoreelements) — discard the items emitted by the source Observable and only pass through the error or completed notification
* [**`interval( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#interval) — create an Observable that emits a sequence of integers spaced by a given time interval
* **`into( )`** (clojure) — _see [**`reduce( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#reduce)_
* [**`isEmpty( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#boolean-operators) — determine whether an Observable emits any items or not
* **`items( )`** (scala) — _see [**`just( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#just)_
* [**`join( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#joins) — combine the items emitted by two Observables whenever one item from one Observable falls within a window of duration specified by an item emitted by the other Observable
* [**`join( )`**](https://github.com/ReactiveX/RxJava/wiki/String-Observables) (`StringObservable`) — converts an Observable that emits a sequence of strings into an Observable that emits a single string that concatenates them all, separating them by a specified string
* [**`just( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#just) — convert an object into an Observable that emits that object
* [**`last( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`) — block until the Observable completes, then return the last item emitted by the Observable
* [**`last( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#last) (`Observable`) — emit only the last item emitted by the source Observable
* **`lastOption( )`** (scala) — _see [**`lastOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) or [**`lastOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`)_
* [**`lastOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`) — block until the Observable completes, then return the last item emitted by the Observable or a default item if there is no last item
* [**`lastOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) (`Observable`) — emit only the last item emitted by an Observable, or a default value if the source Observable is empty
* **`lastOrElse( )`** (scala) — _see [**`lastOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) or [**`lastOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`)_
* [**`latest( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) — returns an iterable that blocks until or unless the Observable emits an item that has not been returned by the iterable, then returns the latest such item
* **`length( )`** (scala) — _see [**`count( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#count)_
* **`limit( )`** — _see [**`take( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#take)_
* **`longCount( )`** (scala) — _see [**`countLong( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators)_
* [**`map( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#map) — transform the items emitted by an Observable by applying a function to each of them
* **`mapcat( )`** (clojure) — _see [**`concatMap( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#concatmap)_
* **`mapMany( )`** — _see: [**`flatMap( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#flatmap)_
* [**`materialize( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — convert an Observable into a list of Notifications
* [**`max( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#max) — emits the maximum value emitted by a source Observable (`rxjava-math`)
* [**`maxBy( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators) — emits the item emitted by the source Observable that has the maximum key value (`rxjava-math`)
* [**`merge( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#merge) — combine multiple Observables into one
* [**`mergeDelayError( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#mergedelayerror) — combine multiple Observables into one, allowing error-free Observables to continue before propagating errors
* **`merge-delay-error( )`** (clojure) — _see [**`mergeDelayError( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#mergedelayerror)_
* **`mergeMap( )`** * — _see: [**`flatMap( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#flatmap)_
* **`mergeMapIterable( )`** — _see: [**`flatMapIterable( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#flatmapiterable)_
* **`mergeWith( )`** — _instance version of [**`merge( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#merge)_
* [**`min( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#min) — emits the minimum value emitted by a source Observable (`rxjava-math`)
* [**`minBy( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators) — emits the item emitted by the source Observable that has the minimum key value (`rxjava-math`)
* [**`mostRecent( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) — returns an iterable that always returns the item most recently emitted by the Observable
* [**`multicast( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#multicast) — represents an Observable as a Connectable Observable
* [**`never( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#never) — create an Observable that emits nothing at all
* [**`next( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) — returns an iterable that blocks until the Observable emits another item, then returns that item
* **`nonEmpty( )`** (scala) — _see [**`isEmpty( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#boolean-operators)_
* **`nth( )`** (clojure) — _see [**`elementAt( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#elementat) and [**`elementAtOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables)_
* [**`observeOn( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — specify on which Scheduler a Subscriber should observe the Observable
* [**`ofType( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#oftype) — emit only those items from the source Observable that are of a particular class
* [**`onBackpressureBlock( )`**](https://github.com/ReactiveX/RxJava/wiki/Backpressure#reactive-pull-backpressure-isnt-magic) — block the Observable's thread until the Observer is ready to accept more items from the Observable (⁇)
* [**`onBackpressureBuffer( )`**](https://github.com/ReactiveX/RxJava/wiki/Backpressure#reactive-pull-backpressure-isnt-magic) — maintain a buffer of all emissions from the source Observable and emit them to downstream Subscribers according to the requests they generate
* [**`onBackpressureDrop( )`**](https://github.com/ReactiveX/RxJava/wiki/Backpressure#reactive-pull-backpressure-isnt-magic) — drop emissions from the source Observable unless there is a pending request from a downstream Subscriber, in which case emit enough items to fulfill the request
* [**`onErrorFlatMap( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#onerrorflatmap) — instructs an Observable to emit a sequence of items whenever it encounters an error (⁇)
* [**`onErrorResumeNext( )`**](https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators#onerrorresumenext) — instructs an Observable to emit a sequence of items if it encounters an error
* [**`onErrorReturn( )`**](https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators#onerrorreturn) — instructs an Observable to emit a particular item when it encounters an error
* [**`onExceptionResumeNext( )`**](https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators#onexceptionresumenext) — instructs an Observable to continue emitting items after it encounters an exception (but not another variety of throwable)
* **`orElse( )`** (scala) — _see [**`defaultIfEmpty( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators)_
* [**`parallel( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#parallel) — split the work done on the emissions from an Observable into multiple Observables each operating on its own parallel thread (⁇)
* [**`parallelMerge( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#parallelmerge) — combine multiple Observables into smaller number of Observables (⁇)
* [**`pivot( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#pivot) — combine multiple sets of grouped observables so that they are arranged primarily by group rather than by set (⁇)
* [**`publish( )`**](https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators) — represents an Observable as a Connectable Observable
* [**`publishLast( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#publishlast) — represent an Observable as a Connectable Observable that emits only the last item emitted by the source Observable (⁇)
* [**`range( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#range) — create an Observable that emits a range of sequential integers
* [**`reduce( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#reduce) — apply a function to each emitted item, sequentially, and emit only the final accumulated value
* **`reductions( )`** (clojure) — _see [**`scan( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#scan)_
* [**`refCount( )`**](https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators) — makes a Connectable Observable behave like an ordinary Observable
* [**`repeat( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables) — create an Observable that emits a particular item or sequence of items repeatedly
* [**`repeatWhen( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#repeatwhen) — create an Observable that emits a particular item or sequence of items repeatedly, depending on the emissions of a second Observable
* [**`replay( )`**](https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators#observablereplay) — ensures that all Subscribers see the same sequence of emitted items, even if they subscribe after the Observable begins emitting the items
* **`rest( )`** (clojure) — _see [**`next( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#next)_
* **`return( )`** (clojure) — _see [**`just( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#just)_
* [**`retry( )`**](https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators#retry) — if a source Observable emits an error, resubscribe to it in the hopes that it will complete without error
* [**`retrywhen( )`**](https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators#retrywhen) — if a source Observable emits an error, pass that error to another Observable to determine whether to resubscribe to the source
* [**`runAsync( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) — returns a `StoppableObservable` that emits multiple actions as generated by a specified Action on a Scheduler (`rxjava-async`)
* [**`sample( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#sample) — emit the most recent items emitted by an Observable within periodic time intervals
* [**`scan( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#scan) — apply a function to each item emitted by an Observable, sequentially, and emit each successive value
* **`seq( )`** (clojure) — _see [**`getIterator( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators)_
* [**`sequenceEqual( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators) — test the equality of sequences emitted by two Observables
* **`sequenceEqualWith( )`** (scala) — _instance version of [**`sequenceEqual( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators)_
* [**`serialize( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators#serialize) — force an Observable to make serialized calls and to be well-behaved
* **`share( )`** — _see [**`refCount( )`**](https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators)_
* [**`single( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`) — if the source Observable completes after emitting a single item, return that item, otherwise throw an exception
* [**`single( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) (`Observable`) — if the source Observable completes after emitting a single item, emit that item, otherwise notify of an exception
* **`singleOption( )`** (scala) — _see [**`singleOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault) (`BlockingObservable`)_
* [**`singleOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) (`BlockingObservable`) — if the source Observable completes after emitting a single item, return that item, otherwise return a default item
* [**`singleOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) (`Observable`) — if the source Observable completes after emitting a single item, emit that item, otherwise emit a default item
* **`singleOrElse( )`** (scala) — _see [**`singleOrDefault( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators)_
* **`size( )`** (scala) — _see [**`count( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#count)_
* [**`skip( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#skip) — ignore the first _n_ items emitted by an Observable
* [**`skipLast( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#skiplast) — ignore the last _n_ items emitted by an Observable
* [**`skipUntil( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — discard items emitted by a source Observable until a second Observable emits an item, then emit the remainder of the source Observable's items
* [**`skipWhile( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — discard items emitted by an Observable until a specified condition is false, then emit the remainder
* **`sliding( )`** (scala) — _see [**`window( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#window)_
* **`slidingBuffer( )`** (scala) — _see [**`buffer( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#buffer)_
* [**`split( )`**](https://github.com/ReactiveX/RxJava/wiki/String-Observables) (`StringObservable`) — converts an Observable of Strings into an Observable of Strings that treats the source sequence as a stream and splits it on a specified regex boundary
* [**`start( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) — create an Observable that emits the return value of a function (`rxjava-async`)
* [**`startCancellableFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Phantom-Operators#fromcancellablefuture-startcancellablefuture-and-defercancellablefuture) — convert a function that returns Future into an Observable that emits that Future's return value in a way that monitors the subscription status of the Observable to determine whether to halt work on the Future (⁇)(`rxjava-async`)
* [**`startFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) — convert a function that returns Future into an Observable that emits that Future's return value (`rxjava-async`)
* [**`startWith( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#startwith) — emit a specified sequence of items before beginning to emit the items from the Observable
* [**`stringConcat( )`**](https://github.com/ReactiveX/RxJava/wiki/String-Observables) (`StringObservable`) — converts an Observable that emits a sequence of strings into an Observable that emits a single string that concatenates them all
* [**`subscribeOn( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — specify which Scheduler an Observable should use when its subscription is invoked
* [**`sumDouble( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#sumdouble) — adds the Doubles emitted by an Observable and emits this sum (`rxjava-math`)
* [**`sumFloat( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#sumfloat) — adds the Floats emitted by an Observable and emits this sum (`rxjava-math`)
* [**`sumInt( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#sumint) — adds the Integers emitted by an Observable and emits this sum (`rxjava-math`)
* [**`sumLong( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#sumlong) — adds the Longs emitted by an Observable and emits this sum (`rxjava-math`)
* **`switch( )`** (scala) — _see [**`switchOnNext( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#switchonnext)_
* [**`switchCase( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — emit the sequence from a particular Observable based on the results of an evaluation (`contrib-computation-expressions`)
* [**`switchMap( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#switchmap) — transform the items emitted by an Observable into Observables, and mirror those items emitted by the most-recently transformed Observable
* [**`switchOnNext( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#switchonnext) — convert an Observable that emits Observables into a single Observable that emits the items emitted by the most-recently emitted of those Observables
* **`synchronize( )`** — _see [**`serialize( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators)_
* [**`take( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#take) — emit only the first _n_ items emitted by an Observable
* [**`takeFirst( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) — emit only the first item emitted by an Observable, or the first item that meets some condition
* [**`takeLast( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#takelast) — only emit the last _n_ items emitted by an Observable
* [**`takeLastBuffer( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables) — emit the last _n_ items emitted by an Observable, as a single list item
* **`takeRight( )`** (scala) — _see [**`last( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#last) (`Observable`) or [**`takeLast( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#takelast)_
* [**`takeUntil( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — emits the items from the source Observable until a second Observable emits an item
* [**`takeWhile( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — emit items emitted by an Observable as long as a specified condition is true, then skip the remainder
* **`take-while( )`** (clojure) — _see [**`takeWhile( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators)_
* [**`then( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#rxjava-joins) — transform a series of `Pattern` objects via a `Plan` template (`rxjava-joins`)
* [**`throttleFirst( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#throttlefirst) — emit the first items emitted by an Observable within periodic time intervals
* [**`throttleLast( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#throttlelast) — emit the most recent items emitted by an Observable within periodic time intervals
* [**`throttleWithTimeout( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#throttlewithtimeout) — only emit an item from the source Observable after a particular timespan has passed without the Observable emitting any other items
* **`throw( )`** (clojure) — _see [**`error( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#error)_
* [**`timeInterval( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — emit the time lapsed between consecutive emissions of a source Observable
* [**`timeout( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#timeout) — emit items from a source Observable, but issue an exception if no item is emitted in a specified timespan
* [**`timer( )`**](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables#timer) — create an Observable that emits a single item after a given delay
* [**`timestamp( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — attach a timestamp to every item emitted by an Observable
* [**`toAsync( )`**](https://github.com/ReactiveX/RxJava/wiki/Async-Operators) — convert a function or Action into an Observable that executes the function and emits its return value (`rxjava-async`)
* [**`toBlocking( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) — transform an Observable into a BlockingObservable
* **`toBlockingObservable( )`** - _see [**`toBlocking( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators)_
* [**`toFuture( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) — convert the Observable into a Future
* [**`toIterable( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators) — convert the sequence emitted by the Observable into an Iterable
* **`toIterator( )`** — _see [**`getIterator( )`**](https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators)_
* [**`toList( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#tolist) — collect all items from an Observable and emit them as a single List
* [**`toMap( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#tomap) — convert the sequence of items emitted by an Observable into a map keyed by a specified key function
* [**`toMultimap( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#tomultimap) — convert the sequence of items emitted by an Observable into an ArrayList that is also a map keyed by a specified key function
* **`toSeq( )`** (scala) — _see [**`toList( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#tolist)_
* [**`toSortedList( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators#tosortedlist) — collect all items from an Observable and emit them as a single, sorted List
* **`tumbling( )`** (scala) — _see [**`window( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#window)_
* **`tumblingBuffer( )`** (scala) — _see [**`buffer( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#buffer)_
* [**`using( )`**](https://github.com/ReactiveX/RxJava/wiki/Observable-Utility-Operators) — create a disposable resource that has the same lifespan as an Observable
* [**`when( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#rxjava-joins) — convert a series of `Plan` objects into an Observable (`rxjava-joins`)
* **`where( )`** — _see: [**`filter( )`**](https://github.com/ReactiveX/RxJava/wiki/Filtering-Observables#filter)_
* [**`whileDo( )`**](https://github.com/ReactiveX/RxJava/wiki/Conditional-and-Boolean-Operators#conditional-operators) — if a condition is true, emit the source Observable's sequence and then repeat the sequence as long as the condition remains true (`contrib-computation-expressions`)
* [**`window( )`**](https://github.com/ReactiveX/RxJava/wiki/Transforming-Observables#window) — periodically subdivide items from an Observable into Observable windows and emit these windows rather than emitting the items one at a time
* [**`zip( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#zip) — combine sets of items emitted by two or more Observables together via a specified function and emit items based on the results of this function
* **`zipWith( )`** — _instance version of [**`zip( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#zip)_
* **`zipWithIndex( )`** (scala) — _see [**`zip( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#zip)_
* **`++`** (scala) — _see [**`concat( )`**](https://github.com/ReactiveX/RxJava/wiki/Mathematical-and-Aggregate-Operators)_
* **`+:`** (scala) — _see [**`startWith( )`**](https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#startwith)_

(⁇) — this proposed operator is not part of RxJava 1.0
