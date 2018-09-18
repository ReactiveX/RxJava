This page will present some elementary RxJava puzzles and walk through some solutions (using the Groovy language implementation of RxJava) as a way of introducing you to some of the RxJava operators.

# Project Euler problem #1

There used to be a site called "Project Euler" that presented a series of mathematical computing conundrums (some fairly easy, others quite baffling) and challenged people to solve them. The first one was a sort of warm-up exercise:

> If we list all the natural numbers below 10 that are multiples of 3 or 5, we get 3, 5, 6 and 9. The sum of these multiples is 23. Find the sum of all the multiples of 3 or 5 below 1000.

There are several ways we could go about this with RxJava.  We might, for instance, begin by going through all of the natural numbers below 1000 with [`range`](Creating-Observables#range) and then [`filter`](Filtering-Observables#filter) out those that are not a multiple either of 3 or of 5:
````groovy
def threesAndFives = Observable.range(1,999).filter({ !((it % 3) && (it % 5)) });
````
Or, we could generate two Observable sequences, one containing the multiples of three and the other containing the multiples of five (by [`map`](https://github.com/Netflix/RxJava/wiki/Transforming-Observables#map)ping each value onto its appropriate multiple), making sure to only generating new multiples while they are less than 1000 (the [`takeWhile`](Conditional-and-Boolean-Operators#takewhile-and-takewhilewithindex) operator will help here), and then [`merge`](Combining-Observables#merge) these sets:
````groovy
def threes = Observable.range(1,999).map({it*3}).takeWhile({it<1000});
def fives = Observable.range(1,999).map({it*5}).takeWhile({it<1000});
def threesAndFives = Observable.merge(threes, fives).distinct();
````
Don't forget the [`distinct`](Filtering-Observables#distinct) operator here, otherwise merge will duplicate numbers like 15 that are multiples of both 5 and 3.

Next, we want to sum up the numbers in the resulting sequence. If you have installed the optional `rxjava-math` module, this is elementary: just use an operator like [`sumInteger` or `sumLong`](Mathematical-and-Aggregate-Operators#suminteger-sumlong-sumfloat-and-sumdouble) on the `threesAndFives` Observable. But what if you don't have this module? How could you use standard RxJava operators to sum up a sequence and emit that sum?

There are a number of operators that reduce a sequence emitted by a source Observable to a single value emitted by the resulting Observable. Most of the ones that are not in the `rxjava-math` module emit boolean evaluations of the sequence; we want something that can emit a number. The [`reduce`](Mathematical-and-Aggregate-Operators#reduce) operator will do the job:
````groovy
def summer = threesAndFives.reduce(0, { a, b -> a+b });
````
Here is how `reduce` gets the job done. It starts with 0 as a seed. Then, with each item that `threesAndFives` emits, it calls the closure `{ a, b -> a+b }`, passing it the current seed value as `a` and the emission as `b`. The closure adds these together and returns that sum, and `reduce` uses this returned value to overwrite its seed. When `threesAndFives` completes, `reduce` emits the final value returned from the closure as its sole emission:
<table>
 <thead>
  <tr><th>iteration</th><th>seed</th><th>emission</th><th>reduce</th></tr>
 </thead>
 <tbody>
  <tr><td>1</td><td>0</td><td>3</td><td>3</td></tr>
  <tr><td>2</td><td>3</td><td>5</td><td>8</td></tr>
  <tr><td>3</td><td>8</td><td>6</td><td>14</td></tr>
  <tr><td colspan="4"><center>&hellip;</center></td></tr>
  <tr><td>466</td><td>232169</td><td>999</td><td>233168</td></tr>
 </tbody>
</table>
Finally, we want to see the result. This means we must [subscribe](Observable#onnext-oncompleted-and-onerror) to the Observable we have constructed:
````groovy
summer.subscribe({println(it);});
````

# Generate the Fibonacci Sequence

How could you create an Observable that emits [the Fibonacci sequence](http://en.wikipedia.org/wiki/Fibonacci_number)?

The most direct way would be to use the [`create`](Creating-Observables#wiki-create) operator to make an Observable "from scratch," and then use a traditional loop within the closure you pass to that operator to generate the sequence. Something like this:
````groovy
def fibonacci = Observable.create({ observer ->
  def f1=0; f2=1, f=1;
  while(!observer.isUnsubscribed() {
    observer.onNext(f);
    f  = f1+f2;
    f1 = f2;
    f2 = f;
  };
});
````
But this is a little too much like ordinary linear programming. Is there some way we can instead create this sequence by composing together existing Observable operators?

Here's an option that does this:
````
def fibonacci = Observable.from(0).repeat().scan([0,1], { a,b -> [a[1], a[0]+a[1]] }).map({it[1]});
````
It's a little [janky](http://www.urbandictionary.com/define.php?term=janky). Let's walk through it:

The `Observable.from(0).repeat()` creates an Observable that just emits a series of zeroes. This just serves as grist for the mill to keep [`scan`](Transforming-Observables#scan) operating. The way `scan` usually behaves is that it operates on the emissions from an Observable, one at a time, accumulating the result of operations on each emission in some sort of register, which it emits as its own emissions. The way we're using it here, it ignores the emissions from the source Observable entirely, and simply uses these emissions as an excuse to transform and emit its register. That register gets `[0,1]` as a seed, and with each iteration changes the register from `[a,b]` to `[b,a+b]` and then emits this register.

This has the effect of emitting the following sequence of items: `[0,1], [1,1], [1,2], [2,3], [3,5], [5,8]...`

The second item in this array describes the Fibonacci sequence. We can use `map` to reduce the sequence to just that item.

To print out a portion of this sequence (using either method), you would use code like the following:
````groovy
fibonnaci.take(15).subscribe({println(it)})];
````

Is there a less-janky way to do this? The [`generate`](https://github.com/Netflix/RxJava/wiki/Phantom-Operators#generate-and-generateabsolutetime) operator would avoid the silliness of creating an Observable that does nothing but turn the crank of `seed`, but this operator is not yet part of RxJava.  Perhaps you can think of a more elegant solution?