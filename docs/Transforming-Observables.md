This page shows operators with which you can transform items that are emitted by reactive sources, such as `Observable`s.

# Outline

- [`buffer`](#buffer)
- [`cast`](#cast)
- [`concatMap`](#concatmap)
- [`flatMap`](#flatmap)
- [`flatMapIterable`](#flatmapiterable)
- [`groupBy`](#groupby)
- [`map`](#map)
- [`scan`](#scan)
- [`switchMap`](#switchmap)
- [`window`](#window)

## buffer

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/buffer.html](http://reactivex.io/documentation/operators/buffer.html)

Collects the items emitted by a reactive source into buffers, and emits these buffers.

### buffer example

```java
Observable.range(0, 10)
    .buffer(4)
    .subscribe((List<Integer> buffer) -> System.out.println(buffer));

// prints:
// [0, 1, 2, 3]
// [4, 5, 6, 7]
// [8, 9]
```

## cast

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/map.html](http://reactivex.io/documentation/operators/map.html)

Converts each item emitted by a reactive source to the specified type, and emits these items.

### cast example

```java
Observable<Number> numbers = Observable.just(1, 4.0, 3f, 7, 12, 4.6, 5);

numbers.filter((Number x) -> Integer.class.isInstance(x))
    .cast(Integer.class)
    .subscribe((Integer x) -> System.out.println(x));

// prints:
// 1
// 7
// 12
// 5
```

## concatMap

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/flatmap.html](http://reactivex.io/documentation/operators/flatmap.html)

Applies the given `io.reactivex.functions.Function` to each item emitted by a reactive source, where that function returns a reactive source, and emits the items that result from concatenating the results of these function applications.

### concatMap example

```java
Observable.range(0, 5)
    .concatMap(i -> {
        long delay = Math.round(Math.random() * 2);

        return Observable.timer(delay, TimeUnit.SECONDS).map(n -> i);
    })
    .blockingSubscribe(System.out::print);

// prints 01234
```

## flatMap

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/flatmap.html](http://reactivex.io/documentation/operators/flatmap.html)

Applies the given `io.reactivex.functions.Function` to each item emitted by a reactive source, where that function returns a reactive source, and emits the items that result from merging the results of these function applications.

### flatMap example

```java
Observable.just("A", "B", "C")
    .flatMap(a -> {
        return Observable.intervalRange(1, 3, 0, 1, TimeUnit.SECONDS)
                .map(b -> '(' + a + ", " + b + ')');
    })
    .blockingSubscribe(System.out::println);

// prints (not necessarily in this order):
// (A, 1)
// (C, 1)
// (B, 1)
// (A, 2)
// (C, 2)
// (B, 2)
// (A, 3)
// (C, 3)
// (B, 3)
```

## flatMapIterable

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/flatmap.html](http://reactivex.io/documentation/operators/flatmap.html)

Applies the given `io.reactivex.functions.Function` to each item emitted by a reactive source, where that function returns a `java.lang.Iterable`, and emits the items that result from merging the results of these function applications.

### flatMapIterable example

```java
Observable.just(1, 2, 3, 4)
    .flatMapIterable(x -> {
        switch (x % 4) {
            case 1:
                return List.of("A");
            case 2:
                return List.of("B", "B");
            case 3:
                return List.of("C", "C", "C");
            default:
                return List.of();
        }
    })
    .subscribe(System.out::println);

// prints:
// A
// B
// B
// C
// C
// C
```

## groupBy

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/groupby.html](http://reactivex.io/documentation/operators/groupby.html)

Groups the items emitted by a reactive source according to a specified criterion, and emits these grouped items as a `GroupedObservable` or `GroupedFlowable`.

### groupBy example

```java
Observable<String> animals = Observable.just(
    "Tiger", "Elephant", "Cat", "Chameleon", "Frog", "Fish", "Turtle", "Flamingo");

animals.groupBy(animal -> animal.charAt(0), String::toUpperCase)
    .concatMapSingle(Observable::toList)
    .subscribe(System.out::println);

// prints:
// [TIGER, TURTLE]
// [ELEPHANT]
// [CAT, CHAMELEON]
// [FROG, FISH, FLAMINGO]
```

## map

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/map.html](http://reactivex.io/documentation/operators/map.html)

Applies the given `io.reactivex.functions.Function` to each item emitted by a reactive source and emits the results of these function applications.

### map example

```java
Observable.just(1, 2, 3)
    .map(x -> x * x)
    .subscribe(System.out::println);

// prints:
// 1
// 4
// 9
```

## scan

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/scan.html](http://reactivex.io/documentation/operators/scan.html)

Applies the given `io.reactivex.functions.BiFunction` to a seed value and the first item emitted by a reactive source, then feeds the result of that function application along with the second item emitted by the reactive source into the same function, and so on until all items have been emitted by the reactive source, emitting each intermediate result.

### scan example

```java
Observable.just(5, 3, 8, 1, 7)
    .scan(0, (partialSum, x) -> partialSum + x)
    .subscribe(System.out::println);

// prints:
// 0
// 5
// 8
// 16
// 17
// 24
```

## switchMap

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/flatmap.html](http://reactivex.io/documentation/operators/flatmap.html)

Applies the given `io.reactivex.functions.Function` to each item emitted by a reactive source, where that function returns a reactive source, and emits the items emitted by the most recently projected of these reactive sources.

### switchMap example

```java
Observable.interval(0, 1, TimeUnit.SECONDS)
    .switchMap(x -> {
        return Observable.interval(0, 750, TimeUnit.MILLISECONDS)
                .map(y -> x);
    })
    .takeWhile(x -> x < 3)
    .blockingSubscribe(System.out::print);

// prints 001122
```

## window

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX documentation:** [http://reactivex.io/documentation/operators/window.html](http://reactivex.io/documentation/operators/window.html)

Collects the items emitted by a reactive source into windows, and emits these windows as a `Flowable` or `Observable`.

### window example

```java
Observable.range(1, 10)

    // Create windows containing at most 2 items, and skip 3 items before starting a new window.
    .window(2, 3)
    .flatMapSingle(window -> {
        return window.map(String::valueOf)
                .reduce(new StringJoiner(", ", "[", "]"), StringJoiner::add);
    })
    .subscribe(System.out::println);

// prints:
// [1, 2]
// [4, 5]
// [7, 8]
// [10]
```
