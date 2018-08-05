This page shows operators that perform mathematical or other operations over an entire sequence of items emitted by an Observable. Because these operations must wait for the source Observable to complete emitting items before they can construct their own emissions (and must usually buffer these items), these operators are dangerous to use on Observables that may have very long or infinite sequences.

# Outline

- [Mathematical Operators](#mathematical-operators)
  - [`averageDouble`](#averagedouble)
  - [`averageFloat`](#averagefloat)
  - [`max`](#max)
  - [`min`](#min)
  - [`sumDouble`](#sumdouble)
  - [`sumFloat`](#sumfloat)
  - [`sumInt`](#sumint)
  - [`sumLong`](#sumlong)
- [Other Aggregate Operators](#other-aggregate-operators)
  - [`count`](#count)
  - [`reduce` and `reduceWith`](#reduce-and-reducewith)
  - [`collect` and `collectInto`](#collect-and-collectinto)
  - [`toList` and `toSortedList`](#tolist-and-tosortedlist)
  - [`toMap`](#tomap)
  - [`toMultimap`](#tomultimap)

## Mathematical Operators

> The operators in this section are part of the [`RxJava2Extensions`](https://github.com/akarnokd/RxJava2Extensions) project. You have to add the
`rxjava2-extensions` module as a dependency to your project. It can be found
at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.akarnokd%22).

### averageDouble

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/average.html](http://reactivex.io/documentation/operators/average.html)

Calculates the average of Numbers emitted by an Observable and emits this average as a Double.

### averageFloat

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/average.html](http://reactivex.io/documentation/operators/average.html)

Calculates the average of Numbers emitted by an Observable and emits this average as a Float.

### max

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/max.html](http://reactivex.io/documentation/operators/max.html)

Emits the maximum value emitted by a source Observable.

### min

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/min.html](http://reactivex.io/documentation/operators/min.html)

Emits the minimum value emitted by a source Observable.

### sumDouble

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/sum.html](http://reactivex.io/documentation/operators/sum.html)

Adds the Doubles emitted by an Observable and emits this sum.

### sumFloat

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/sum.html](http://reactivex.io/documentation/operators/sum.html)

Adds the Floats emitted by an Observable and emits this sum.

### sumInt

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/sum.html](http://reactivex.io/documentation/operators/sum.html)

Adds the Integers emitted by an Observable and emits this sum.

### sumLong

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/sum.html](http://reactivex.io/documentation/operators/sum.html)

Adds the Integers emitted by an Observable and emits this sum.

## Other Aggregate Operators

### count

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/count.html](http://reactivex.io/documentation/operators/count.html)


Counts the number of items emitted by an Observable and emits this count.

### reduce and reduceWith

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/reduce.html](http://reactivex.io/documentation/operators/reduce.html)

Apply a function to each emitted item, sequentially, and emit only the final accumulated value.

### collect and collectInto

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/reduce.html](http://reactivex.io/documentation/operators/reduce.html)

Collect items emitted by the source Observable into a single mutable data structure and return an Observable that emits this structure.

### toList and toSortedList

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/to.html](http://reactivex.io/documentation/operators/to.html)

Collect all items from an Observable and emit them as a single List.

### toMap

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/to.html](http://reactivex.io/documentation/operators/to.html)

Convert the sequence of items emitted by an Observable into a map keyed by a specified key function.

### toMultimap

**Available in:** ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Flowable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png) `Observable`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Maybe`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Single`, ![image](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png) `Completable`

**ReactiveX doumentation:** [http://reactivex.io/documentation/operators/to.html](http://reactivex.io/documentation/operators/to.html)

Convert the sequence of items emitted by an Observable into an ArrayList that is also a map keyed by a specified key function.
