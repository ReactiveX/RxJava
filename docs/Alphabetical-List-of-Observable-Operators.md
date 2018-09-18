* **`aggregate( )`** — _see [**`reduce( )`**](Mathematical-and-Aggregate-Operators#reduce)_
* [**`all( )`**](Conditional-and-Boolean-Operators#all) — determine whether all items emitted by an Observable meet some criteria
* [**`amb( )`**](Conditional-and-Boolean-Operators#amb) — given two or more source Observables, emits all of the items from the first of these Observables to emit an item
* **`ambWith( )`** — _instance version of [**`amb( )`**](Conditional-and-Boolean-Operators#amb)_
* [**`and( )`**](Combining-Observables#and-then-and-when) — combine the emissions from two or more source Observables into a `Pattern` (`rxjava-joins`)
* **`apply( )`** (scala) — _see [**`create( )`**](Creating-Observables#create)_
* **`asObservable( )`** (kotlin) — _see [**`from( )`**](Creating-Observables#from) (et al.)_
* [**`asyncAction( )`**](Async-Operators#toasync-or-asyncaction-or-asyncfunc) — convert an Action into an Observable that executes the Action and emits its return value (`rxjava-async`)
* [**`asyncFunc( )`**](Async-Operators#toasync-or-asyncaction-or-asyncfunc) — convert a function into an Observable that executes the function and emits its return value (`rxjava-async`)
* [**`averageDouble( )`**](Mathematical-and-Aggregate-Operators#averageinteger-averagelong-averagefloat-and-averagedouble) — calculates the average of Doubles emitted by an Observable and emits this average (`rxjava-math`)
* [**`averageFloat( )`**](Mathematical-and-Aggregate-Operators#averageinteger-averagelong-averagefloat-and-averagedouble) — calculates the average of Floats emitted by an Observable and emits this average (`rxjava-math`)
* [**`averageInteger( )`**](Mathematical-and-Aggregate-Operators#averageinteger-averagelong-averagefloat-and-averagedouble) — calculates the average of Integers emitted by an Observable and emits this average (`rxjava-math`)
* [**`averageLong( )`**](Mathematical-and-Aggregate-Operators#averageinteger-averagelong-averagefloat-and-averagedouble) — calculates the average of Longs emitted by an Observable and emits this average (`rxjava-math`)
* **`blocking( )`** (clojure) — _see [**`toBlocking( )`**](Blocking-Observable-Operators)_
* [**`buffer( )`**](Transforming-Observables#buffer) — periodically gather items from an Observable into bundles and emit these bundles rather than emitting the items one at a time
* [**`byLine( )`**](String-Observables#byline) (`StringObservable`) — converts an Observable of Strings into an Observable of Lines by treating the source sequence as a stream and splitting it on line-endings
* [**`cache( )`**](Observable-Utility-Operators#cache) — remember the sequence of items emitted by the Observable and emit the same sequence to future Subscribers
* [**`cast( )`**](Transforming-Observables#cast) — cast all items from the source Observable into a particular type before reemitting them
* **`catch( )`** (clojure) — _see [**`onErrorResumeNext( )`**](Error-Handling-Operators#onerrorresumenext)_
* [**`chunkify( )`**](Phantom-Operators#chunkify) — returns an iterable that periodically returns a list of items emitted by the source Observable since the last list (⁇)
* [**`collect( )`**](Mathematical-and-Aggregate-Operators#collect) — collects items emitted by the source Observable into a single mutable data structure and returns an Observable that emits this structure
* [**`combineLatest( )`**](Combining-Observables#combinelatest) — when an item is emitted by either of two Observables, combine the latest item emitted by each Observable via a specified function and emit items based on the results of this function
* **`combineLatestWith( )`** (scala) — _instance version of [**`combineLatest( )`**](Combining-Observables#combinelatest)_
* [**`concat( )`**](Mathematical-and-Aggregate-Operators#concat) — concatenate two or more Observables sequentially
* [**`concatMap( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable) — transform the items emitted by an Observable into Observables, then flatten this into a single Observable, without interleaving
* **`concatWith( )`** — _instance version of [**`concat( )`**](Mathematical-and-Aggregate-Operators#concat)_
* [**`connect( )`**](Connectable-Observable-Operators#connectableobservableconnect) — instructs a Connectable Observable to begin emitting items
* **`cons( )`** (clojure) — _see [**`concat( )`**](Mathematical-and-Aggregate-Operators#concat)_
* [**`contains( )`**](Conditional-and-Boolean-Operators#contains) — determine whether an Observable emits a particular item or not
* [**`count( )`**](Mathematical-and-Aggregate-Operators#count-and-countlong) — counts the number of items emitted by an Observable and emits this count
* [**`countLong( )`**](Mathematical-and-Aggregate-Operators#count-and-countlong) — counts the number of items emitted by an Observable and emits this count
* [**`create( )`**](Creating-Observables#create) — create an Observable from scratch by means of a function
* **`cycle( )`** (clojure) — _see [**`repeat( )`**](Creating-Observables#repeat)_
* [**`debounce( )`**](Filtering-Observables#throttlewithtimeout-or-debounce) — only emit an item from the source Observable after a particular timespan has passed without the Observable emitting any other items
* [**`decode( )`**](String-Observables#decode) (`StringObservable`) — convert a stream of multibyte characters into an Observable that emits byte arrays that respect character boundaries
* [**`defaultIfEmpty( )`**](Conditional-and-Boolean-Operators#defaultifempty) — emit items from the source Observable, or emit a default item if the source Observable completes after emitting no items
* [**`defer( )`**](Creating-Observables#defer) — do not create the Observable until a Subscriber subscribes; create a fresh Observable on each subscription
* [**`deferFuture( )`**](Async-Operators#deferfuture) — convert a Future that returns an Observable into an Observable, but do not attempt to get the Observable that the Future returns until a Subscriber subscribes (`rxjava-async`)
* [**`deferCancellableFuture( )`**](Phantom-Operators#fromcancellablefuture-startcancellablefuture-and-defercancellablefuture-) — convert a Future that returns an Observable into an Observable in a way that monitors the subscription status of the Observable to determine whether to halt work on the Future, but do not attempt to get the returned Observable until a Subscriber subscribes (⁇)(`rxjava-async`)
* [**`delay( )`**](Observable-Utility-Operators#delay) — shift the emissions from an Observable forward in time by a specified amount
* [**`dematerialize( )`**](Observable-Utility-Operators#dematerialize) — convert a materialized Observable back into its non-materialized form
* [**`distinct( )`**](Filtering-Observables#distinct) — suppress duplicate items emitted by the source Observable
* [**`distinctUntilChanged( )`**](Filtering-Observables#distinctuntilchanged) — suppress duplicate consecutive items emitted by the source Observable
* **`do( )`** (clojure) — _see [**`doOnEach( )`**](Observable-Utility-Operators#dooneach)_
* [**`doOnCompleted( )`**](Observable-Utility-Operators#dooncompleted) — register an action to take when an Observable completes successfully
* [**`doOnEach( )`**](Observable-Utility-Operators#dooneach) — register an action to take whenever an Observable emits an item
* [**`doOnError( )`**](Observable-Utility-Operators#doonerror) — register an action to take when an Observable completes with an error
* **`doOnNext( )`** — _see [**`doOnEach( )`**](Observable-Utility-Operators#dooneach)_
* **`doOnRequest( )`** — register an action to take when items are requested from an Observable via reactive-pull backpressure (⁇)
* [**`doOnSubscribe( )`**](Observable-Utility-Operators#doonsubscribe) — register an action to take when an observer subscribes to an Observable
* [**`doOnTerminate( )`**](Observable-Utility-Operators#doonterminate) — register an action to take when an Observable completes, either successfully or with an error
* [**`doOnUnsubscribe( )`**](Observable-Utility-Operators#doonunsubscribe) — register an action to take when an observer unsubscribes from an Observable
* [**`doWhile( )`**](Conditional-and-Boolean-Operators#dowhile) — emit the source Observable's sequence, and then repeat the sequence as long as a condition remains true (`contrib-computation-expressions`)
* **`drop( )`** (scala/clojure) — _see [**`skip( )`**](Filtering-Observables#skip)_
* **`dropRight( )`** (scala) — _see [**`skipLast( )`**](Filtering-Observables#skiplast)_
* **`dropUntil( )`** (scala) — _see [**`skipUntil( )`**](Conditional-and-Boolean-Operators#skipuntil)_
* **`dropWhile( )`** (scala) — _see [**`skipWhile( )`**](Conditional-and-Boolean-Operators#skipwhile)_
* **`drop-while( )`** (clojure) — _see [**`skipWhile( )`**](Conditional-and-Boolean-Operators#skipwhile)_
* [**`elementAt( )`**](Filtering-Observables#elementat) — emit item _n_ emitted by the source Observable
* [**`elementAtOrDefault( )`**](Filtering-Observables#elementatordefault) — emit item _n_ emitted by the source Observable, or a default item if the source Observable emits fewer than _n_ items
* [**`empty( )`**](Creating-Observables#empty-error-and-never) — create an Observable that emits nothing and then completes
* [**`encode( )`**](String-Observables#encode) (`StringObservable`) — transform an Observable that emits strings into an Observable that emits byte arrays that respect character boundaries of multibyte characters in the original strings
* [**`error( )`**](Creating-Observables#empty-error-and-never) — create an Observable that emits nothing and then signals an error
* **`every( )`** (clojure) — _see [**`all( )`**](Conditional-and-Boolean-Operators#all)_
* [**`exists( )`**](Conditional-and-Boolean-Operators#exists-and-isempty) — determine whether an Observable emits any items or not
* [**`filter( )`**](Filtering-Observables#filter) — filter items emitted by an Observable
* **`finally( )`** (clojure) — _see [**`finallyDo( )`**](Observable-Utility-Operators#finallydo)_
* **`filterNot( )`** (scala) — _see [**`filter( )`**](Filtering-Observables#filter)_
* [**`finallyDo( )`**](Observable-Utility-Operators#finallydo) — register an action to take when an Observable completes
* [**`first( )`**](Filtering-Observables#first-and-takefirst) (`Observable`) — emit only the first item emitted by an Observable, or the first item that meets some condition
* [**`first( )`**](Blocking-Observable-Operators#first-and-firstordefault) (`BlockingObservable`) — emit only the first item emitted by an Observable, or the first item that meets some condition
* [**`firstOrDefault( )`**](Filtering-Observables#firstordefault) (`Observable`) — emit only the first item emitted by an Observable, or the first item that meets some condition, or a default value if the source Observable is empty
* [**`firstOrDefault( )`**](Blocking-Observable-Operators#first-and-firstordefault) (`BlockingObservable`) — emit only the first item emitted by an Observable, or the first item that meets some condition, or a default value if the source Observable is empty
* **`firstOrElse( )`** (scala) — _see [**`firstOrDefault( )`**](Filtering-Observables#firstordefault) or [**`firstOrDefault( )`**](Blocking-Observable-Operators#first-and-firstordefault) (`BlockingObservable`)_
* [**`flatMap( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable) — transform the items emitted by an Observable into Observables, then flatten this into a single Observable
* [**`flatMapIterable( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable) — create Iterables corresponding to each emission from a source Observable and merge the results into a single Observable
* **`flatMapIterableWith( )`** (scala) — _instance version of [**`flatMapIterable( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable)_
* **`flatMapWith( )`** (scala) — _instance version of [**`flatmap( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable)_
* **`flatten( )`** (scala) — _see [**`merge( )`**](Combining-Observables#merge)_
* **`flattenDelayError( )`** (scala) — _see [**`mergeDelayError( )`**](Combining-Observables#mergedelayerror)_
* **`foldLeft( )`** (scala) — _see [**`reduce( )`**](Mathematical-and-Aggregate-Operators#reduce)_
* **`forall( )`** (scala) — _see [**`all( )`**](Conditional-and-Boolean-Operators#all)_
* **`forEach( )`** (`Observable`) — _see [**`subscribe( )`**](Observable#onnext-oncompleted-and-onerror)_
* [**`forEach( )`**](Blocking-Observable-Operators#foreach) (`BlockingObservable`) — invoke a function on each item emitted by the Observable; block until the Observable completes
* [**`forEachFuture( )`**](Async-Operators#foreachfuture) (`Async`) — pass Subscriber methods to an Observable but also have it behave like a Future that blocks until it completes (`rxjava-async`)
* [**`forEachFuture( )`**](Phantom-Operators#foreachfuture) (`BlockingObservable`)— create a futureTask that will invoke a specified function on each item emitted by an Observable (⁇)
* [**`forIterable( )`**](Phantom-Operators#foriterable) — apply a function to the elements of an Iterable to create Observables which are then concatenated (⁇)
* [**`from( )`**](Creating-Observables#from) — convert an Iterable, a Future, or an Array into an Observable
* [**`from( )`**](String-Observables#from) (`StringObservable`) — convert a stream of characters or a Reader into an Observable that emits byte arrays or Strings
* [**`fromAction( )`**](Async-Operators#fromaction) — convert an Action into an Observable that invokes the action and emits its result when a Subscriber subscribes (`rxjava-async`)
* [**`fromCallable( )`**](Async-Operators#fromcallable) — convert a Callable into an Observable that invokes the callable and emits its result or exception when a Subscriber subscribes (`rxjava-async`)
* [**`fromCancellableFuture( )`**](Phantom-Operators#fromcancellablefuture-startcancellablefuture-and-defercancellablefuture-) — convert a Future into an Observable in a way that monitors the subscription status of the Observable to determine whether to halt work on the Future, but do not attempt to get the Future's value until a Subscriber subscribes (⁇)(`rxjava-async`)
* **`fromFunc0( )`** — _see [**`fromCallable( )`**](Async-Operators#fromcallable) (`rxjava-async`)_
* [**`fromFuture( )`**](Phantom-Operators#fromfuture) — convert a Future into an Observable, but do not attempt to get the Future's value until a Subscriber subscribes (⁇)
* [**`fromRunnable( )`**](Async-Operators#fromrunnable) — convert a Runnable into an Observable that invokes the runable and emits its result when a Subscriber subscribes (`rxjava-async`)
* [**`generate( )`**](Phantom-Operators#generate-and-generateabsolutetime) — create an Observable that emits a sequence of items as generated by a function of your choosing (⁇)
* [**`generateAbsoluteTime( )`**](Phantom-Operators#generate-and-generateabsolutetime) — create an Observable that emits a sequence of items as generated by a function of your choosing, with each item emitted at an item-specific time (⁇)
* **`generator( )`** (clojure) — _see [**`generate( )`**](Phantom-Operators#generate-and-generateabsolutetime)_
* [**`getIterator( )`**](Blocking-Observable-Operators#transformations-tofuture-toiterable-and-getiterator) — convert the sequence emitted by the Observable into an Iterator
* [**`groupBy( )`**](Transforming-Observables#groupby) — divide an Observable into a set of Observables that emit groups of items from the original Observable, organized by key
* **`group-by( )`** (clojure) — _see [**`groupBy( )`**](Transforming-Observables#groupby)_
* [**`groupByUntil( )`**](Phantom-Operators#groupbyuntil) — a variant of the [`groupBy( )`](Transforming-Observables#groupby) operator that closes any open GroupedObservable upon a signal from another Observable (⁇)
* [**`groupJoin( )`**](Combining-Observables#join-and-groupjoin) — combine the items emitted by two Observables whenever one item from one Observable falls within a window of duration specified by an item emitted by the other Observable
* **`head( )`** (scala) — _see [**`first( )`**](Blocking-Observable-Operators#first-and-firstordefault) (`BlockingObservable`)_
* **`headOption( )`** (scala) — _see [**`firstOrDefault( )`**](Filtering-Observables#firstordefault) or [**`firstOrDefault( )`**](Blocking-Observable-Operators#first-and-firstordefault) (`BlockingObservable`)_
* **`headOrElse( )`** (scala) — _see [**`firstOrDefault( )`**](Filtering-Observables#firstordefault) or [**`firstOrDefault( )`**](Blocking-Observable-Operators#first-and-firstordefault) (`BlockingObservable`)_
* [**`ifThen( )`**](Conditional-and-Boolean-Operators#ifthen) — only emit the source Observable's sequence if a condition is true, otherwise emit an empty or default sequence (`contrib-computation-expressions`)
* [**`ignoreElements( )`**](Filtering-Observables#ignoreelements) — discard the items emitted by the source Observable and only pass through the error or completed notification
* [**`interval( )`**](Creating-Observables#interval) — create an Observable that emits a sequence of integers spaced by a given time interval
* **`into( )`** (clojure) — _see [**`reduce( )`**](Mathematical-and-Aggregate-Operators#reduce)_
* [**`isEmpty( )`**](Conditional-and-Boolean-Operators#exists-and-isempty) — determine whether an Observable emits any items or not
* **`items( )`** (scala) — _see [**`just( )`**](Creating-Observables#just)_
* [**`join( )`**](Combining-Observables#join-and-groupjoin) — combine the items emitted by two Observables whenever one item from one Observable falls within a window of duration specified by an item emitted by the other Observable
* [**`join( )`**](String-Observables#join) (`StringObservable`) — converts an Observable that emits a sequence of strings into an Observable that emits a single string that concatenates them all, separating them by a specified string
* [**`just( )`**](Creating-Observables#just) — convert an object into an Observable that emits that object
* [**`last( )`**](Blocking-Observable-Operators#last-and-lastordefault) (`BlockingObservable`) — block until the Observable completes, then return the last item emitted by the Observable
* [**`last( )`**](Filtering-Observables#last) (`Observable`) — emit only the last item emitted by the source Observable
* **`lastOption( )`** (scala) — _see [**`lastOrDefault( )`**](Filtering-Observables#lastOrDefault) or [**`lastOrDefault( )`**](Blocking-Observable-Operators#last-and-lastordefault) (`BlockingObservable`)_
* [**`lastOrDefault( )`**](Blocking-Observable-Operators#last-and-lastordefault) (`BlockingObservable`) — block until the Observable completes, then return the last item emitted by the Observable or a default item if there is no last item
* [**`lastOrDefault( )`**](Filtering-Observables#lastOrDefault) (`Observable`) — emit only the last item emitted by an Observable, or a default value if the source Observable is empty
* **`lastOrElse( )`** (scala) — _see [**`lastOrDefault( )`**](Filtering-Observables#lastOrDefault) or [**`lastOrDefault( )`**](Blocking-Observable-Operators#last-and-lastordefault) (`BlockingObservable`)_
* [**`latest( )`**](Blocking-Observable-Operators#latest) — returns an iterable that blocks until or unless the Observable emits an item that has not been returned by the iterable, then returns the latest such item
* **`length( )`** (scala) — _see [**`count( )`**](Mathematical-and-Aggregate-Operators#count-and-countlong)_
* **`limit( )`** — _see [**`take( )`**](Filtering-Observables#take)_
* **`longCount( )`** (scala) — _see [**`countLong( )`**](Mathematical-and-Aggregate-Operators#count-and-countlong)_
* [**`map( )`**](Transforming-Observables#map) — transform the items emitted by an Observable by applying a function to each of them
* **`mapcat( )`** (clojure) — _see [**`concatMap( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable)_
* **`mapMany( )`** — _see: [**`flatMap( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable)_
* [**`materialize( )`**](Observable-Utility-Operators#materialize) — convert an Observable into a list of Notifications
* [**`max( )`**](Mathematical-and-Aggregate-Operators#max) — emits the maximum value emitted by a source Observable (`rxjava-math`)
* [**`maxBy( )`**](Mathematical-and-Aggregate-Operators#maxby) — emits the item emitted by the source Observable that has the maximum key value (`rxjava-math`)
* [**`merge( )`**](Combining-Observables#merge) — combine multiple Observables into one
* [**`mergeDelayError( )`**](Combining-Observables#mergedelayerror) — combine multiple Observables into one, allowing error-free Observables to continue before propagating errors
* **`merge-delay-error( )`** (clojure) — _see [**`mergeDelayError( )`**](Combining-Observables#mergedelayerror)_
* **`mergeMap( )`** * — _see: [**`flatMap( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable)_
* **`mergeMapIterable( )`** — _see: [**`flatMapIterable( )`**](Transforming-Observables#flatmap-concatmap-and-flatmapiterable)_
* **`mergeWith( )`** — _instance version of [**`merge( )`**](Combining-Observables#merge)_
* [**`min( )`**](Mathematical-and-Aggregate-Operators#min) — emits the minimum value emitted by a source Observable (`rxjava-math`)
* [**`minBy( )`**](Mathematical-and-Aggregate-Operators#minby) — emits the item emitted by the source Observable that has the minimum key value (`rxjava-math`)
* [**`mostRecent( )`**](Blocking-Observable-Operators#mostrecent) — returns an iterable that always returns the item most recently emitted by the Observable
* [**`multicast( )`**](Phantom-Operators#multicast) — represents an Observable as a Connectable Observable
* [**`never( )`**](Creating-Observables#empty-error-and-never) — create an Observable that emits nothing at all
* [**`next( )`**](Blocking-Observable-Operators#next) — returns an iterable that blocks until the Observable emits another item, then returns that item
* **`nonEmpty( )`** (scala) — _see [**`isEmpty( )`**](Conditional-and-Boolean-Operators#exists-and-isempty)_
* **`nth( )`** (clojure) — _see [**`elementAt( )`**](Filtering-Observables#elementat) and [**`elementAtOrDefault( )`**](Filtering-Observables#elementatordefault)_
* [**`observeOn( )`**](Observable-Utility-Operators#observeon) — specify on which Scheduler a Subscriber should observe the Observable
* [**`ofType( )`**](Filtering-Observables#oftype) — emit only those items from the source Observable that are of a particular class
* [**`onBackpressureBlock( )`**](Backpressure) — block the Observable's thread until the Observer is ready to accept more items from the Observable (⁇)
* [**`onBackpressureBuffer( )`**](Backpressure) — maintain a buffer of all emissions from the source Observable and emit them to downstream Subscribers according to the requests they generate
* [**`onBackpressureDrop( )`**](Backpressure) — drop emissions from the source Observable unless there is a pending request from a downstream Subscriber, in which case emit enough items to fulfill the request
* [**`onErrorFlatMap( )`**](Phantom-Operators#onerrorflatmap) — instructs an Observable to emit a sequence of items whenever it encounters an error (⁇)
* [**`onErrorResumeNext( )`**](Error-Handling-Operators#onerrorresumenext) — instructs an Observable to emit a sequence of items if it encounters an error
* [**`onErrorReturn( )`**](Error-Handling-Operators#onerrorreturn) — instructs an Observable to emit a particular item when it encounters an error
* [**`onExceptionResumeNext( )`**](Error-Handling-Operators#onexceptionresumenext) — instructs an Observable to continue emitting items after it encounters an exception (but not another variety of throwable)
* **`orElse( )`** (scala) — _see [**`defaultIfEmpty( )`**](Conditional-and-Boolean-Operators#defaultifempty)_
* [**`parallel( )`**](Phantom-Operators#parallel) — split the work done on the emissions from an Observable into multiple Observables each operating on its own parallel thread (⁇)
* [**`parallelMerge( )`**](Phantom-Operators#parallelmerge) — combine multiple Observables into smaller number of Observables (⁇)
* [**`pivot( )`**](Phantom-Operators#pivot) — combine multiple sets of grouped observables so that they are arranged primarily by group rather than by set (⁇)
* [**`publish( )`**](Connectable-Observable-Operators#observablepublish) — represents an Observable as a Connectable Observable
* [**`publishLast( )`**](Phantom-Operators#publishlast) — represent an Observable as a Connectable Observable that emits only the last item emitted by the source Observable (⁇)
* [**`range( )`**](Creating-Observables#range) — create an Observable that emits a range of sequential integers
* [**`reduce( )`**](Mathematical-and-Aggregate-Operators#reduce) — apply a function to each emitted item, sequentially, and emit only the final accumulated value
* **`reductions( )`** (clojure) — _see [**`scan( )`**](Transforming-Observables#scan)_
* [**`refCount( )`**](Connectable-Observable-Operators#connectableobservablerefcount) — makes a Connectable Observable behave like an ordinary Observable
* [**`repeat( )`**](Creating-Observables#repeat) — create an Observable that emits a particular item or sequence of items repeatedly
* [**`repeatWhen( )`**](Creating-Observables#repeatwhen) — create an Observable that emits a particular item or sequence of items repeatedly, depending on the emissions of a second Observable
* [**`replay( )`**](Connectable-Observable-Operators#observablereplay) — ensures that all Subscribers see the same sequence of emitted items, even if they subscribe after the Observable begins emitting the items
* **`rest( )`** (clojure) — _see [**`next( )`**](Blocking-Observable-Operators#next)_
* **`return( )`** (clojure) — _see [**`just( )`**](Creating-Observables#just)_
* [**`retry( )`**](Error-Handling-Operators#retry) — if a source Observable emits an error, resubscribe to it in the hopes that it will complete without error
* [**`retrywhen( )`**](Error-Handling-Operators#retrywhen) — if a source Observable emits an error, pass that error to another Observable to determine whether to resubscribe to the source
* [**`runAsync( )`**](Async-Operators#runasync) — returns a `StoppableObservable` that emits multiple actions as generated by a specified Action on a Scheduler (`rxjava-async`)
* [**`sample( )`**](Filtering-Observables#sample-or-throttlelast) — emit the most recent items emitted by an Observable within periodic time intervals
* [**`scan( )`**](Transforming-Observables#scan) — apply a function to each item emitted by an Observable, sequentially, and emit each successive value
* **`seq( )`** (clojure) — _see [**`getIterator( )`**](Blocking-Observable-Operators#transformations-tofuture-toiterable-and-getiterator)_
* [**`sequenceEqual( )`**](Conditional-and-Boolean-Operators#sequenceequal) — test the equality of sequences emitted by two Observables
* **`sequenceEqualWith( )`** (scala) — _instance version of [**`sequenceEqual( )`**](Conditional-and-Boolean-Operators#sequenceequal)_
* [**`serialize( )`**](Observable-Utility-Operators#serialize) — force an Observable to make serialized calls and to be well-behaved
* **`share( )`** — _see [**`refCount( )`**](Connectable-Observable-Operators#connectableobservablerefcount)_
* [**`single( )`**](Blocking-Observable-Operators#single-and-singleordefault) (`BlockingObservable`) — if the source Observable completes after emitting a single item, return that item, otherwise throw an exception
* [**`single( )`**](Observable-Utility-Operators#single-and-singleordefault) (`Observable`) — if the source Observable completes after emitting a single item, emit that item, otherwise notify of an exception
* **`singleOption( )`** (scala) — _see [**`singleOrDefault( )`**](Blocking-Observable-Operators#single-and-singleordefault) (`BlockingObservable`)_
* [**`singleOrDefault( )`**](Blocking-Observable-Operators#single-and-singleordefault) (`BlockingObservable`) — if the source Observable completes after emitting a single item, return that item, otherwise return a default item
* [**`singleOrDefault( )`**](Observable-Utility-Operators#single-and-singleordefault) (`Observable`) — if the source Observable completes after emitting a single item, emit that item, otherwise emit a default item
* **`singleOrElse( )`** (scala) — _see [**`singleOrDefault( )`**](Observable-Utility-Operators#single-and-singleordefault)_
* **`size( )`** (scala) — _see [**`count( )`**](Mathematical-and-Aggregate-Operators#count-and-countlong)_
* [**`skip( )`**](Filtering-Observables#skip) — ignore the first _n_ items emitted by an Observable
* [**`skipLast( )`**](Filtering-Observables#skiplast) — ignore the last _n_ items emitted by an Observable
* [**`skipUntil( )`**](Conditional-and-Boolean-Operators#skipuntil) — discard items emitted by a source Observable until a second Observable emits an item, then emit the remainder of the source Observable's items
* [**`skipWhile( )`**](Conditional-and-Boolean-Operators#skipwhile) — discard items emitted by an Observable until a specified condition is false, then emit the remainder
* **`sliding( )`** (scala) — _see [**`window( )`**](Transforming-Observables#window)_
* **`slidingBuffer( )`** (scala) — _see [**`buffer( )`**](Transforming-Observables#buffer)_
* [**`split( )`**](String-Observables#split) (`StringObservable`) — converts an Observable of Strings into an Observable of Strings that treats the source sequence as a stream and splits it on a specified regex boundary
* [**`start( )`**](Async-Operators#start) — create an Observable that emits the return value of a function (`rxjava-async`)
* [**`startCancellableFuture( )`**](Phantom-Operators#fromcancellablefuture-startcancellablefuture-and-defercancellablefuture-) — convert a function that returns Future into an Observable that emits that Future's return value in a way that monitors the subscription status of the Observable to determine whether to halt work on the Future (⁇)(`rxjava-async`)
* [**`startFuture( )`**](Async-Operators#startfuture) — convert a function that returns Future into an Observable that emits that Future's return value (`rxjava-async`)
* [**`startWith( )`**](Combining-Observables#startwith) — emit a specified sequence of items before beginning to emit the items from the Observable
* [**`stringConcat( )`**](String-Observables#stringconcat) (`StringObservable`) — converts an Observable that emits a sequence of strings into an Observable that emits a single string that concatenates them all
* [**`subscribeOn( )`**](Observable-Utility-Operators#subscribeon) — specify which Scheduler an Observable should use when its subscription is invoked
* [**`sumDouble( )`**](Mathematical-and-Aggregate-Operators#suminteger-sumlong-sumfloat-and-sumdouble) — adds the Doubles emitted by an Observable and emits this sum (`rxjava-math`)
* [**`sumFloat( )`**](Mathematical-and-Aggregate-Operators#suminteger-sumlong-sumfloat-and-sumdouble) — adds the Floats emitted by an Observable and emits this sum (`rxjava-math`)
* [**`sumInteger( )`**](Mathematical-and-Aggregate-Operators#suminteger-sumlong-sumfloat-and-sumdouble) — adds the Integers emitted by an Observable and emits this sum (`rxjava-math`)
* [**`sumLong( )`**](Mathematical-and-Aggregate-Operators#suminteger-sumlong-sumfloat-and-sumdouble) — adds the Longs emitted by an Observable and emits this sum (`rxjava-math`)
* **`switch( )`** (scala) — _see [**`switchOnNext( )`**](Combining-Observables#switchonnext)_
* [**`switchCase( )`**](Conditional-and-Boolean-Operators#switchcase) — emit the sequence from a particular Observable based on the results of an evaluation (`contrib-computation-expressions`)
* [**`switchMap( )`**](Transforming-Observables#switchmap) — transform the items emitted by an Observable into Observables, and mirror those items emitted by the most-recently transformed Observable
* [**`switchOnNext( )`**](Combining-Observables#switchonnext) — convert an Observable that emits Observables into a single Observable that emits the items emitted by the most-recently emitted of those Observables
* **`synchronize( )`** — _see [**`serialize( )`**](Observable-Utility-Operators#serialize)_
* [**`take( )`**](Filtering-Observables#take) — emit only the first _n_ items emitted by an Observable
* [**`takeFirst( )`**](Filtering-Observables#first-and-takefirst) — emit only the first item emitted by an Observable, or the first item that meets some condition
* [**`takeLast( )`**](Filtering-Observables#takelast) — only emit the last _n_ items emitted by an Observable
* [**`takeLastBuffer( )`**](Filtering-Observables#takelastbuffer) — emit the last _n_ items emitted by an Observable, as a single list item
* **`takeRight( )`** (scala) — _see [**`last( )`**](Filtering-Observables#last) (`Observable`) or [**`takeLast( )`**](Filtering-Observables#takelast)_
* [**`takeUntil( )`**](Conditional-and-Boolean-Operators#takeuntil) — emits the items from the source Observable until a second Observable emits an item
* [**`takeWhile( )`**](Conditional-and-Boolean-Operators#takewhile) — emit items emitted by an Observable as long as a specified condition is true, then skip the remainder
* **`take-while( )`** (clojure) — _see [**`takeWhile( )`**](Conditional-and-Boolean-Operators#takewhile)_
* [**`then( )`**](Combining-Observables#and-then-and-when) — transform a series of `Pattern` objects via a `Plan` template (`rxjava-joins`)
* [**`throttleFirst( )`**](Filtering-Observables#throttlefirst) — emit the first items emitted by an Observable within periodic time intervals
* [**`throttleLast( )`**](Filtering-Observables#sample-or-throttlelast) — emit the most recent items emitted by an Observable within periodic time intervals
* [**`throttleWithTimeout( )`**](Filtering-Observables#throttlewithtimeout-or-debounce) — only emit an item from the source Observable after a particular timespan has passed without the Observable emitting any other items
* **`throw( )`** (clojure) — _see [**`error( )`**](Creating-Observables#empty-error-and-never)_
* [**`timeInterval( )`**](Observable-Utility-Operators#timeinterval) — emit the time lapsed between consecutive emissions of a source Observable
* [**`timeout( )`**](Filtering-Observables#timeout) — emit items from a source Observable, but issue an exception if no item is emitted in a specified timespan
* [**`timer( )`**](Creating-Observables#timer) — create an Observable that emits a single item after a given delay
* [**`timestamp( )`**](Observable-Utility-Operators#timestamp) — attach a timestamp to every item emitted by an Observable
* [**`toAsync( )`**](Async-Operators#toasync-or-asyncaction-or-asyncfunc) — convert a function or Action into an Observable that executes the function and emits its return value (`rxjava-async`)
* [**`toBlocking( )`**](Blocking-Observable-Operators) — transform an Observable into a BlockingObservable
* **`toBlockingObservable( )`** - _see [**`toBlocking( )`**](Blocking-Observable-Operators)_
* [**`toFuture( )`**](Blocking-Observable-Operators#transformations-tofuture-toiterable-and-getiterator) — convert the Observable into a Future
* [**`toIterable( )`**](Blocking-Observable-Operators#transformations-tofuture-toiterable-and-getiterator) — convert the sequence emitted by the Observable into an Iterable
* **`toIterator( )`** — _see [**`getIterator( )`**](Blocking-Observable-Operators#transformations-tofuture-toiterable-and-getiterator)_
* [**`toList( )`**](Mathematical-and-Aggregate-Operators#tolist) — collect all items from an Observable and emit them as a single List
* [**`toMap( )`**](Mathematical-and-Aggregate-Operators#tomap-and-tomultimap) — convert the sequence of items emitted by an Observable into a map keyed by a specified key function
* [**`toMultimap( )`**](Mathematical-and-Aggregate-Operators#tomap-and-tomultimap) — convert the sequence of items emitted by an Observable into an ArrayList that is also a map keyed by a specified key function
* **`toSeq( )`** (scala) — _see [**`toList( )`**](Mathematical-and-Aggregate-Operators#tolist)_
* [**`toSortedList( )`**](Mathematical-and-Aggregate-Operators#tosortedlist) — collect all items from an Observable and emit them as a single, sorted List
* **`tumbling( )`** (scala) — _see [**`window( )`**](Transforming-Observables#window)_
* **`tumblingBuffer( )`** (scala) — _see [**`buffer( )`**](Transforming-Observables#buffer)_
* [**`using( )`**](Observable-Utility-Operators#using) — create a disposable resource that has the same lifespan as an Observable
* [**`when( )`**](Combining-Observables#and-then-and-when) — convert a series of `Plan` objects into an Observable (`rxjava-joins`)
* **`where( )`** — _see: [**`filter( )`**](Filtering-Observables#filter)_
* [**`whileDo( )`**](Conditional-and-Boolean-Operators#whiledo) — if a condition is true, emit the source Observable's sequence and then repeat the sequence as long as the condition remains true (`contrib-computation-expressions`)
* [**`window( )`**](Transforming-Observables#window) — periodically subdivide items from an Observable into Observable windows and emit these windows rather than emitting the items one at a time
* [**`zip( )`**](Combining-Observables#zip) — combine sets of items emitted by two or more Observables together via a specified function and emit items based on the results of this function
* **`zipWith( )`** — _instance version of [**`zip( )`**](Combining-Observables#zip)_
* **`zipWithIndex( )`** (scala) — _see [**`zip( )`**](Combining-Observables#zip)_
* **`++`** (scala) — _see [**`concat( )`**](Mathematical-and-Aggregate-Operators#concat)_
* **`+:`** (scala) — _see [**`startWith( )`**](Combining-Observables#startwith)_

(⁇) — this proposed operator is not part of RxJava 1.0