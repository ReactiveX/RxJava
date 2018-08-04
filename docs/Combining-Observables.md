This section explains operators you can use to combine multiple Observables.

# Outline

- [`combineLatest`](#combineLatest)
- [`join` and `groupJoin`](#joins)
- [`merge`](#merge)
- [`mergeDelayError`](#mergeDelayError)
- [`rxjava-joins`](#rxjava-joins)
- [`startWith`](#startWith)
- [`switchOnNext`](#switchOnNext)
- [`zip`](#zip)

## startWith

Emit a specified sequence of items before beginning to emit the items from the Observable.

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/startwith.html](http://reactivex.io/documentation/operators/startwith.html)

#### startWith Example

```java
Observable<String> names = Observable.just("Spock", "McCoy");
names.startWith("Kirk").subscribe(item -> System.out.println(item));

// prints Kirk, Spock, McCoy
```

## merge

Combines multiple Observables into one. 


### merge

Combines multiple Observables into one. Any `onError` notifications passed from any of the source observables will immediately be passed through to through to the observers and will terminate the merged `Observable`.

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/merge.html](http://reactivex.io/documentation/operators/merge.html)

#### merge Example

```java
Observable.just(1, 2, 3)
    .mergeWith(Observable.just(4, 5, 6))
    .subscribe(item -> System.out.println(item));
```

### mergeDelayError

Combines multiple Observables into one. Any `onError` notifications passed from any of the source observables will be withheld until all merged Observables complete, and only then will be passed along to the observers.

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/merge.html](http://reactivex.io/documentation/operators/merge.html)

#### mergeDelayError Example

```java
Observable<String> observable1 = Observable.error(new IllegalArgumentException(""));
Observable<String> observable2 = Observable.just("Four", "Five", "Six");
Observable.mergeDelayError(observable1, observable2)
        .subscribe(item -> System.out.println(item));

// emits 4, 5, 6 and then the IllegalArgumentException
```

## zip

Combines sets of items emitted by two or more Observables together via a specified function and emit items based on the results of this function.

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/zip.html](http://reactivex.io/documentation/operators/zip.html)

#### zip Example

```java
Observable<String> firstNames = Observable.just("James", "Jean-Luc", "Benjamin");
Observable<String> lastNames = Observable.just("Kirk", "Picard", "Sisko");
firstNames.zipWith(lastNames, (first, last) -> first + " " + last)
    .subscribe(item -> System.out.println(item));

// prints James Kirk, Jean-Luc Picard, Benjamin Sisko
```

## combineLatest

When an item is emitted by either of two Observables, combine the latest item emitted by each Observable via a specified function and emit items based on the results of this function.

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/combinelatest.html](http://reactivex.io/documentation/operators/combinelatest.html)

#### combineLatest Example

```java
Observable<Long> newsRefreshes = Observable.interval(100, TimeUnit.MILLISECONDS);
Observable<Long> weatherRefreshes = Observable.interval(50, TimeUnit.MILLISECONDS);
Observable.combineLatest(newsRefreshes, weatherRefreshes,
    (newsRefreshTimes, weatherRefreshTimes) ->
        "Refreshed news " + newsRefreshTimes + " times and weather " + weatherRefreshTimes)
    .subscribe(item -> System.out.println(item));

// prints:
// Refreshed news 0 times and weather 0
// Refreshed news 0 times and weather 1
// Refreshed news 0 times and weather 2
// Refreshed news 1 times and weather 2
// Refreshed news 1 times and weather 3
// Refreshed news 1 times and weather 4
// Refreshed news 2 times and weather 4
// Refreshed news 2 times and weather 5
// ...
```

## switchOnNext

Convert an Observable that emits Observables into a single Observable that emits the items emitted by the most-recently emitted of those Observables.

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/switch.html](http://reactivex.io/documentation/operators/switch.html)

#### switchOnNext Example

```java
Observable<Observable<Long>> timeIntervals = Observable.interval(1, TimeUnit.MILLISECONDS)
  .map(ticks -> Observable.just(ticks));
Observable.switchOnNext(timeIntervals)
    .subscribe(item -> System.out.println(item));

// prints 0, 1, 2, ... 
```


## joins

* [**`join( )` and `groupJoin( )`**](http://reactivex.io/documentation/operators/join.html) — combine the items emitted by two Observables whenever one item from one Observable falls within a window of duration specified by an item emitted by the other Observable

## rxjava-joins

* (`rxjava-joins`) [**`and( )`, `then( )`, and `when( )`**](http://reactivex.io/documentation/operators/and-then-when.html) — combine sets of items emitted by two or more Observables by means of `Pattern` and `Plan` intermediaries

> (`rxjava-joins`) — indicates that this operator is currently part of the optional `rxjava-joins` package under `rxjava-contrib` and is not included with the standard RxJava set of operators
