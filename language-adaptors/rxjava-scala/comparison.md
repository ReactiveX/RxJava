
## Comparison of Scala Observable and Java Observable
   
Note: 
*    This table contains both static methods and instance methods.
*    If a signature is too long, move your mouse over it to get the full signature.

   
| Java Method | Scala Method |
|-------------|--------------|
| `aggregate(R, Func2<R, ? super T, R>)` | `fold(R)((R, T) => R)` |
| `aggregate(Func2<T, T, T>)` | `reduce((U, U) => U)` |
| `all(Func1<? super T, Boolean>)` | `forall(T => Boolean)` |
| `average(Observable<Integer>)`<br/>`averageDoubles(Observable<Double>)`<br/>`averageFloats(Observable<Float>)`<br/>`averageLongs(Observable<Long>)` | We can't have a general average method because Scala's `Numeric` does not have scalar multiplication (we would need to calculate `(1.0/numberOfElements)*sum`). You can use `fold` instead to accumulate `sum` and `numberOfElements` and divide at the end. |
| `buffer(Int, Int)` | `buffer(Int, Int)` |
| `buffer(Long, TimeUnit, Int)` | `buffer(Duration, Int)` |
| `buffer(Long, Long, TimeUnit, Scheduler)` | `buffer(Duration, Duration, Scheduler)` |
| `buffer(Long, Long, TimeUnit)` | `buffer(Duration, Duration)` |
| <span title="buffer(Observable&lt;? extends Opening&gt;, Func1&lt;Opening, ? extends Observable&lt;? extends Closing&gt;&gt;)"><code>buffer(...)</code></span> | `buffer(Observable[Opening], Opening => Observable[Closing])` |
| <span title="buffer(Func0&lt;? extends Observable&lt;? extends Closing&gt;&gt;)"><code>buffer(...)</code></span> | `buffer(() => Observable[Closing])` |
| `buffer(Long, TimeUnit)` | `buffer(Duration)` |
| `buffer(Long, TimeUnit, Int, Scheduler)` | `buffer(Duration, Int, Scheduler)` |
| `buffer(Int)` | `buffer(Int)` |
| `buffer(Long, TimeUnit, Scheduler)` | `buffer(Duration, Scheduler)` |
| `cache()` | `cache` |
| <span title="combineLatest(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Func2&lt;? super T1, ? super T2, ? extends R&gt;)"><code>combineLatest(...)</code></span> | `combineLatest(Observable[U])` |
| <span title="combineLatest(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Func3&lt;? super T1, ? super T2, ? super T3, ? extends R&gt;)"><code>combineLatest(...)</code></span><br/><span title="combineLatest(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Func4&lt;? super T1, ? super T2, ? super T3, ? super T4, ? extends R&gt;)"><code>combineLatest(...)</code></span><br/><span title="combineLatest(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Func5&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R&gt;)"><code>combineLatest(...)</code></span><br/><span title="combineLatest(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Observable&lt;? extends T6&gt;, Func6&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R&gt;)"><code>combineLatest(...)</code></span><br/><span title="combineLatest(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Observable&lt;? extends T6&gt;, Observable&lt;? extends T7&gt;, Func7&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R&gt;)"><code>combineLatest(...)</code></span><br/><span title="combineLatest(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Observable&lt;? extends T6&gt;, Observable&lt;? extends T7&gt;, Observable&lt;? extends T8&gt;, Func8&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R&gt;)"><code>combineLatest(...)</code></span><br/><span title="combineLatest(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Observable&lt;? extends T6&gt;, Observable&lt;? extends T7&gt;, Observable&lt;? extends T8&gt;, Observable&lt;? extends T9&gt;, Func9&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R&gt;)"><code>combineLatest(...)</code></span> | If C# doesn't need it, Scala doesn't need it either ;-) |
| <span title="concat(Observable&lt;? extends Observable&lt;? extends T&gt;&gt;)"><code>concat(...)</code></span> | `concat(<:<[Observable[T], Observable[Observable[U]]])` |
| <span title="concat(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>concat(...)</code></span><br/><span title="concat(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>concat(...)</code></span><br/><span title="concat(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>concat(...)</code></span><br/><span title="concat(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>concat(...)</code></span><br/><span title="concat(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>concat(...)</code></span><br/><span title="concat(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>concat(...)</code></span><br/><span title="concat(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>concat(...)</code></span><br/><span title="concat(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>concat(...)</code></span> | unnecessary because we can use `++` instead or `Observable(o1, o2, ...).concat` |
| `count()` | `length` |
| `create(OnSubscribeFunc<T>)` | `apply(Observer[T] => Subscription)` |
| `debounce(Long, TimeUnit)` | `debounce(Duration)` |
| `debounce(Long, TimeUnit, Scheduler)` | `debounce(Duration, Scheduler)` |
| `defer(Func0<? extends Observable<? extends T>>)` | `defer(=> Observable[T])` |
| `dematerialize()` | `dematerialize(<:<[T, Notification[U]])` |
| `distinct(Comparator<T>)`<br/><span title="distinct(Func1&lt;? super T, ? extends U&gt;, Comparator&lt;U&gt;)"><code>distinct(...)</code></span> | **TODO: missing** |
| `distinct(Func1<? super T, ? extends U>)` | `distinct(T => U)` |
| `distinct()` | `distinct` |
| `distinctUntilChanged()` | `distinctUntilChanged` |
| `distinctUntilChanged(Comparator<T>)`<br/><span title="distinctUntilChanged(Func1&lt;? super T, ? extends U&gt;, Comparator&lt;U&gt;)"><code>distinctUntilChanged(...)</code></span> | **TODO: missing** |
| <span title="distinctUntilChanged(Func1&lt;? super T, ? extends U&gt;)"><code>distinctUntilChanged(...)</code></span> | `distinctUntilChanged(T => U)` |
| `empty()` | `apply(T*)` |
| `error(Throwable)` | `apply(Throwable)` |
| `filter(Func1<? super T, Boolean>)` | `filter(T => Boolean)` |
| `finallyDo(Action0)` | `finallyDo(() => Unit)` |
| `first()` | `first` |
| `first(Func1<? super T, Boolean>)` | use `.filter(condition).first` |
| `firstOrDefault(Func1<? super T, Boolean>, T)` | use `.filter(condition).firstOrElse(default)` |
| `firstOrDefault(T)` | `firstOrElse(=> U)` |
| <span title="flatMap(Func1&lt;? super T, ? extends Observable&lt;? extends R&gt;&gt;)"><code>flatMap(...)</code></span> | `flatMap(T => Observable[R])` |
| `from(Future<? extends T>, Long, TimeUnit)` | `apply(Future[T], Duration)` |
| `from(Future<? extends T>)` | `apply(Future[T])` |
| `from(Future<? extends T>, Scheduler)` | `apply(Future[T], Scheduler)` |
| `from(T[])`<br/>`from(Iterable<? extends T>)`<br/>`from(T)`<br/>`from(T, T)`<br/>`from(T, T, T)`<br/>`from(T, T, T, T)`<br/>`from(T, T, T, T, T)`<br/>`from(T, T, T, T, T, T)`<br/>`from(T, T, T, T, T, T, T)`<br/>`from(T, T, T, T, T, T, T, T)`<br/>`from(T, T, T, T, T, T, T, T, T)`<br/>`from(T, T, T, T, T, T, T, T, T, T)` | `apply(T*)` |
| `groupBy(Func1<? super T, ? extends K>)` | `groupBy(T => K)` |
| <span title="groupBy(Func1&lt;? super T, ? extends K&gt;, Func1&lt;? super T, ? extends R&gt;)"><code>groupBy(...)</code></span> | use `groupBy` and `map` |
| `interval(Long, TimeUnit)` | `interval(Duration)` |
| `interval(Long, TimeUnit, Scheduler)` | `interval(Duration, Scheduler)` |
| `just(T)` | `just(T)` |
| `map(Func1<? super T, ? extends R>)` | `map(T => R)` |
| <span title="mapMany(Func1&lt;? super T, ? extends Observable&lt;? extends R&gt;&gt;)"><code>mapMany(...)</code></span> | `flatMap(T => Observable[R])` |
| <span title="mapWithIndex(Func2&lt;? super T, Integer, ? extends R&gt;)"><code>mapWithIndex(...)</code></span> | combine `zipWithIndex` with `map` or with a for comprehension |
| `materialize()` | `materialize` |
| <span title="merge(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>merge(...)</code></span><br/><span title="merge(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>merge(...)</code></span><br/><span title="merge(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>merge(...)</code></span><br/><span title="merge(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>merge(...)</code></span><br/><span title="merge(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>merge(...)</code></span><br/><span title="merge(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>merge(...)</code></span><br/><span title="merge(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>merge(...)</code></span> | unnecessary because we can use `Observable(o1, o2, ...).flatten` instead |
| <span title="merge(Observable&lt;? extends Observable&lt;? extends T&gt;&gt;)"><code>merge(...)</code></span> | `flatten(<:<[Observable[T], Observable[Observable[U]]])` |
| <span title="merge(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>merge(...)</code></span> | `merge(Observable[U])` |
| <span title="mergeDelayError(Observable&lt;? extends Observable&lt;? extends T&gt;&gt;)"><code>mergeDelayError(...)</code></span> | `flattenDelayError(<:<[Observable[T], Observable[Observable[U]]])` |
| <span title="mergeDelayError(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>mergeDelayError(...)</code></span><br/><span title="mergeDelayError(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>mergeDelayError(...)</code></span><br/><span title="mergeDelayError(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>mergeDelayError(...)</code></span><br/><span title="mergeDelayError(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>mergeDelayError(...)</code></span><br/><span title="mergeDelayError(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>mergeDelayError(...)</code></span><br/><span title="mergeDelayError(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>mergeDelayError(...)</code></span><br/><span title="mergeDelayError(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>mergeDelayError(...)</code></span> | unnecessary because we can use `Observable(o1, o2, ...).flattenDelayError` instead |
| <span title="mergeDelayError(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>mergeDelayError(...)</code></span> | `mergeDelayError(Observable[U])` |
| `multicast(Subject<? super T, ? extends R>)` | `multicast(Subject[T, R])` |
| `never()` | `never` |
| `observeOn(Scheduler)` | `observeOn(Scheduler)` |
| <span title="onErrorResumeNext(Func1&lt;Throwable, ? extends Observable&lt;? extends T&gt;&gt;)"><code>onErrorResumeNext(...)</code></span> | `onErrorResumeNext(Throwable => Observable[U])` |
| `onErrorResumeNext(Observable<? extends T>)` | `onErrorResumeNext(Observable[U])` |
| `onErrorReturn(Func1<Throwable, ? extends T>)` | `onErrorReturn(Throwable => U)` |
| `onExceptionResumeNext(Observable<? extends T>)` | `onExceptionResumeNext(Observable[U])` |
| `parallel(Func1<Observable<T>, Observable<R>>)` | `parallel(Observable[T] => Observable[R])` |
| <span title="parallel(Func1&lt;Observable&lt;T&gt;, Observable&lt;R&gt;&gt;, Scheduler)"><code>parallel(...)</code></span> | `parallel(Observable[T] => Observable[R], Scheduler)` |
| `publish()` | `publish` |
| `range(Int, Int)` | `apply(Range)` |
| `reduce(Func2<T, T, T>)` | `reduce((U, U) => U)` |
| `reduce(R, Func2<R, ? super T, R>)` | `fold(R)((R, T) => R)` |
| `replay()` | `replay` |
| `retry(Int)` | `retry(Int)` |
| `retry()` | `retry` |
| `sample(Long, TimeUnit)` | `sample(Duration)` |
| `sample(Long, TimeUnit, Scheduler)` | `sample(Duration, Scheduler)` |
| `scan(Func2<T, T, T>)` | considered unnecessary in Scala land |
| `scan(R, Func2<R, ? super T, R>)` | `scan(R)((R, T) => R)` |
| <span title="sequenceEqual(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;, Func2&lt;? super T, ? super T, Boolean&gt;)"><code>sequenceEqual(...)</code></span> | use `(first zip second) map (p => equality(p._1, p._2))` |
| <span title="sequenceEqual(Observable&lt;? extends T&gt;, Observable&lt;? extends T&gt;)"><code>sequenceEqual(...)</code></span> | use `(first zip second) map (p => p._1 == p._2)` |
| `skip(Int)` | `drop(Int)` |
| `skipWhile(Func1<? super T, Boolean>)` | `dropWhile(T => Boolean)` |
| <span title="skipWhileWithIndex(Func2&lt;? super T, Integer, Boolean&gt;)"><code>skipWhileWithIndex(...)</code></span> | considered unnecessary in Scala land |
| `startWith(Iterable<T>)`<br/>`startWith(T)`<br/>`startWith(T, T)`<br/>`startWith(T, T, T)`<br/>`startWith(T, T, T, T)`<br/>`startWith(T, T, T, T, T)`<br/>`startWith(T, T, T, T, T, T)`<br/>`startWith(T, T, T, T, T, T, T)`<br/>`startWith(T, T, T, T, T, T, T, T)`<br/>`startWith(T, T, T, T, T, T, T, T, T)` | unnecessary because we can just use `++` instead |
| `subscribe(Observer<? super T>, Scheduler)` | `subscribe(Observer[T], Scheduler)` |
| `subscribe(Action1<? super T>, Action1<Throwable>)` | `subscribe(T => Unit, Throwable => Unit)` |
| <span title="subscribe(Action1&lt;? super T&gt;, Action1&lt;Throwable&gt;, Action0)"><code>subscribe(...)</code></span> | `subscribe(T => Unit, Throwable => Unit, () => Unit)` |
| <span title="subscribe(Action1&lt;? super T&gt;, Action1&lt;Throwable&gt;, Action0, Scheduler)"><code>subscribe(...)</code></span> | `subscribe(T => Unit, Throwable => Unit, () => Unit, Scheduler)` |
| `subscribe(Action1<? super T>)` | `subscribe(T => Unit)` |
| <span title="subscribe(Action1&lt;? super T&gt;, Action1&lt;Throwable&gt;, Scheduler)"><code>subscribe(...)</code></span> | `subscribe(T => Unit, Throwable => Unit, Scheduler)` |
| `subscribe(Action1<? super T>, Scheduler)` | `subscribe(T => Unit, Scheduler)` |
| `subscribe(Observer<? super T>)` | `subscribe(Observer[T])` |
| `subscribeOn(Scheduler)` | `subscribeOn(Scheduler)` |
| `sum(Observable<Integer>)` | `sum(Numeric[U])` |
| `sumDoubles(Observable<Double>)` | `sum(Numeric[U])` |
| `sumFloats(Observable<Float>)` | `sum(Numeric[U])` |
| `sumLongs(Observable<Long>)` | `sum(Numeric[U])` |
| <span title="switchDo(Observable&lt;? extends Observable&lt;? extends T&gt;&gt;)"><code>switchDo(...)</code></span> | deprecated in RxJava |
| <span title="switchOnNext(Observable&lt;? extends Observable&lt;? extends T&gt;&gt;)"><code>switchOnNext(...)</code></span> | `switch(<:<[Observable[T], Observable[Observable[U]]])` |
| `synchronize()`<br/>`synchronize(Observable<T>)` | `synchronize` |
| `take(Int)` | `take(Int)` |
| `takeFirst(Func1<? super T, Boolean>)` | use `.filter(condition).first` |
| `takeFirst()` | `first` |
| `takeLast(Int)` | `takeRight(Int)` |
| `takeUntil(Observable<? extends E>)` | `takeUntil(Observable[E])` |
| `takeWhile(Func1<? super T, Boolean>)` | `takeWhile(T => Boolean)` |
| <span title="takeWhileWithIndex(Func2&lt;? super T, ? super Integer, Boolean&gt;)"><code>takeWhileWithIndex(...)</code></span> | `takeWhileWithIndex((T, Integer) => Boolean)` |
| `throttleFirst(Long, TimeUnit)` | `throttleFirst(Duration)` |
| `throttleFirst(Long, TimeUnit, Scheduler)` | `throttleFirst(Duration, Scheduler)` |
| `throttleLast(Long, TimeUnit)` | `throttleLast(Duration)` |
| `throttleLast(Long, TimeUnit, Scheduler)` | `throttleLast(Duration, Scheduler)` |
| `throttleWithTimeout(Long, TimeUnit, Scheduler)` | `throttleWithTimeout(Duration, Scheduler)` |
| `throttleWithTimeout(Long, TimeUnit)` | `throttleWithTimeout(Duration)` |
| `timestamp()` | `timestamp` |
| `toBlockingObservable()` | `toBlockingObservable` |
| `toList()` | `toSeq` |
| `toSortedList(Func2<? super T, ? super T, Integer>)` | Sorting is already done in Scala's collection library, use `.toSeq.map(_.sortWith(f))` |
| `toSortedList()` | Sorting is already done in Scala's collection library, use `.toSeq.map(_.sorted)` |
| `where(Func1<? super T, Boolean>)` | `filter(T => Boolean)` |
| `window(Long, TimeUnit)` | `window(Duration)` |
| `window(Long, TimeUnit, Int)` | `window(Duration, Int)` |
| `window(Long, Long, TimeUnit, Scheduler)` | `window(Duration, Duration, Scheduler)` |
| `window(Int)` | `window(Int)` |
| `window(Int, Int)` | `window(Int, Int)` |
| `window(Long, Long, TimeUnit)` | `window(Duration, Duration)` |
| <span title="window(Observable&lt;? extends Opening&gt;, Func1&lt;Opening, ? extends Observable&lt;? extends Closing&gt;&gt;)"><code>window(...)</code></span> | `window(Observable[Opening], Opening => Observable[Closing])` |
| <span title="window(Func0&lt;? extends Observable&lt;? extends Closing&gt;&gt;)"><code>window(...)</code></span> | `window(() => Observable[Closing])` |
| `window(Long, TimeUnit, Int, Scheduler)` | `window(Duration, Int, Scheduler)` |
| `window(Long, TimeUnit, Scheduler)` | `window(Duration, Scheduler)` |
| <span title="zip(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Func2&lt;? super T1, ? super T2, ? extends R&gt;)"><code>zip(...)</code></span> | use instance method `zip` and `map` |
| <span title="zip(Iterable&lt;? extends Observable&lt;_&gt;&gt;, FuncN&lt;? extends R&gt;)"><code>zip(...)</code></span><br/><span title="zip(Observable&lt;? extends Observable&lt;_&gt;&gt;, FuncN&lt;? extends R&gt;)"><code>zip(...)</code></span> | use `zip` in companion object and `map` |
| <span title="zip(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Func3&lt;? super T1, ? super T2, ? super T3, ? extends R&gt;)"><code>zip(...)</code></span><br/><span title="zip(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Func4&lt;? super T1, ? super T2, ? super T3, ? super T4, ? extends R&gt;)"><code>zip(...)</code></span><br/><span title="zip(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Func5&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R&gt;)"><code>zip(...)</code></span><br/><span title="zip(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Observable&lt;? extends T6&gt;, Func6&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R&gt;)"><code>zip(...)</code></span><br/><span title="zip(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Observable&lt;? extends T6&gt;, Observable&lt;? extends T7&gt;, Func7&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R&gt;)"><code>zip(...)</code></span><br/><span title="zip(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Observable&lt;? extends T6&gt;, Observable&lt;? extends T7&gt;, Observable&lt;? extends T8&gt;, Func8&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R&gt;)"><code>zip(...)</code></span><br/><span title="zip(Observable&lt;? extends T1&gt;, Observable&lt;? extends T2&gt;, Observable&lt;? extends T3&gt;, Observable&lt;? extends T4&gt;, Observable&lt;? extends T5&gt;, Observable&lt;? extends T6&gt;, Observable&lt;? extends T7&gt;, Observable&lt;? extends T8&gt;, Observable&lt;? extends T9&gt;, Func9&lt;? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R&gt;)"><code>zip(...)</code></span> | considered unnecessary in Scala land |

This table was generated on Fri Sep 20 17:38:03 CEST 2013.
**Do not edit**. Instead, edit `rx.lang.scala.CompletenessTest`.
