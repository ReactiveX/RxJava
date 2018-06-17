This page shows operators that perform mathematical or other operations over an entire sequence of items emitted by an Observable. Because these operations must wait for the source Observable to complete emitting items before they can construct their own emissions (and must usually buffer these items), these operators are dangerous to use on Observables that may have very long or infinite sequences.

#### Operators in the `rxjava-math` module
* [**`averageInteger( )`**](http://reactivex.io/documentation/operators/average.html) — calculates the average of Integers emitted by an Observable and emits this average
* [**`averageLong( )`**](http://reactivex.io/documentation/operators/average.html) — calculates the average of Longs emitted by an Observable and emits this average
* [**`averageFloat( )`**](http://reactivex.io/documentation/operators/average.html) — calculates the average of Floats emitted by an Observable and emits this average
* [**`averageDouble( )`**](http://reactivex.io/documentation/operators/average.html) — calculates the average of Doubles emitted by an Observable and emits this average
* [**`max( )`**](http://reactivex.io/documentation/operators/max.html) — emits the maximum value emitted by a source Observable
* [**`maxBy( )`**](http://reactivex.io/documentation/operators/max.html) — emits the item emitted by the source Observable that has the maximum key value
* [**`min( )`**](http://reactivex.io/documentation/operators/min.html) — emits the minimum value emitted by a source Observable
* [**`minBy( )`**](http://reactivex.io/documentation/operators/min.html) — emits the item emitted by the source Observable that has the minimum key value
* [**`sumInteger( )`**](http://reactivex.io/documentation/operators/sum.html) — adds the Integers emitted by an Observable and emits this sum
* [**`sumLong( )`**](http://reactivex.io/documentation/operators/sum.html) — adds the Longs emitted by an Observable and emits this sum
* [**`sumFloat( )`**](http://reactivex.io/documentation/operators/sum.html) — adds the Floats emitted by an Observable and emits this sum
* [**`sumDouble( )`**](http://reactivex.io/documentation/operators/sum.html) — adds the Doubles emitted by an Observable and emits this sum

#### Other Aggregate Operators
* [**`concat( )`**](http://reactivex.io/documentation/operators/concat.html) — concatenate two or more Observables sequentially
* [**`count( )` and `countLong( )`**](http://reactivex.io/documentation/operators/count.html) — counts the number of items emitted by an Observable and emits this count
* [**`reduce( )`**](http://reactivex.io/documentation/operators/reduce.html) — apply a function to each emitted item, sequentially, and emit only the final accumulated value
* [**`collect( )`**](http://reactivex.io/documentation/operators/reduce.html) — collect items emitted by the source Observable into a single mutable data structure and return an Observable that emits this structure
* [**`toList( )`**](http://reactivex.io/documentation/operators/to.html) — collect all items from an Observable and emit them as a single List
* [**`toSortedList( )`**](http://reactivex.io/documentation/operators/to.html) — collect all items from an Observable and emit them as a single, sorted List
* [**`toMap( )`**](http://reactivex.io/documentation/operators/to.html) — convert the sequence of items emitted by an Observable into a map keyed by a specified key function
* [**`toMultiMap( )`**](http://reactivex.io/documentation/operators/to.html) — convert the sequence of items emitted by an Observable into an ArrayList that is also a map keyed by a specified key function