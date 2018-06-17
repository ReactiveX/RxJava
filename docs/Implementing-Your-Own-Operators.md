You can implement your own Observable operators. This page shows you how.

If your operator is designed to *originate* an Observable, rather than to transform or react to a source Observable, use the [`create( )`](http://reactivex.io/documentation/operators/create.html) method rather than trying to implement `Observable` manually.  Otherwise, you can create a custom operator by following the instructions on this page.

If your operator is designed to act on the individual items emitted by a source Observable, follow the instructions under [_Sequence Operators_](Implementing-Your-Own-Operators#sequence-operators) below. If your operator is designed to transform the source Observable as a whole (for instance, by applying a particular set of existing RxJava operators to it) follow the instructions under [_Transformational Operators_](Implementing-Your-Own-Operators#transformational-operators) below.

(**Note:** in Xtend, a Groovy-like language, you can implement your operators as _extension methods_ and can thereby chain them directly without using the methods described on this page. See [RxJava and Xtend](http://mnmlst-dvlpr.blogspot.de/2014/07/rxjava-and-xtend.html) for details.)

# Sequence Operators

The following example shows how you can use the `lift( )` operator to chain your custom operator (in this example: `myOperator`) alongside standard RxJava operators like `ofType` and `map`:
```groovy
fooObservable = barObservable.ofType(Integer).map({it*2}).lift(new myOperator<T>()).map({"transformed by myOperator: " + it});
```
The following section shows how you form the scaffolding of your operator so that it will work correctly with `lift( )`.

## Implementing Your Operator

Define your operator as a public class that implements the [`Operator`](http://reactivex.io/RxJava/javadoc/rx/Observable.Operator.html) interface, like so:
```java
public class myOperator<T> implements Operator<T> {
  public myOperator( /* any necessary params here */ ) {
    /* any necessary initialization here */
  }

  @Override
  public Subscriber<? super T> call(final Subscriber<? super T> s) {
    return new Subscriber<t>(s) {
      @Override
      public void onCompleted() {
        /* add your own onCompleted behavior here, or just pass the completed notification through: */
        if(!s.isUnsubscribed()) {
          s.onCompleted();
        }
      }

      @Override
      public void onError(Throwable t) {
        /* add your own onError behavior here, or just pass the error notification through: */
        if(!s.isUnsubscribed()) {
          s.onError(t);
        }
      }

      @Override
      public void onNext(T item) {
        /* this example performs some sort of operation on each incoming item and emits the results */
        if(!s.isUnsubscribed()) {
          transformedItem = myOperatorTransformOperation(item);
          s.onNext(transformedItem);
        }
      }
    };
  }
}
``` 

# Transformational Operators

The following example shows how you can use the `compose( )` operator to chain your custom operator (in this example, an operator called `myTransformer` that transforms an Observable that emits Integers into one that emits Strings) alongside standard RxJava operators like `ofType` and `map`:
```groovy
fooObservable = barObservable.ofType(Integer).map({it*2}).compose(new myTransformer<Integer,String>()).map({"transformed by myOperator: " + it});
```
The following section shows how you form the scaffolding of your operator so that it will work correctly with `compose( )`.

## Implementing Your Transformer

Define your transforming function as a public class that implements the [`Transformer`](http://reactivex.io/RxJava/javadoc/rx/Observable.Transformer.html) interface, like so:

````java
public class myTransformer<Integer,String> implements Transformer<Integer,String> {
  public myTransformer( /* any necessary params here */ ) {
    /* any necessary initialization here */
  }

  @Override
  public Observable<String> call(Observable<Integer> source) {
    /* 
     * this simple example Transformer applies map() to the source Observable
     * in order to transform the "source" observable from one that emits
     * integers to one that emits string representations of those integers.
     */
    return source.map( new Func1<Integer,String>() {
      @Override
      public String call(Integer t1) {
        return String.valueOf(t1);
      }
    } );
  }
}
````

## See also

* [&ldquo;Don&#8217;t break the chain: use RxJava&#8217;s compose() operator&rdquo;](http://blog.danlew.net/2015/03/02/dont-break-the-chain/) by Dan Lew

# Other Considerations

* Your sequence operator may want to check [its Subscriber&#8217;s `isUnsubscribed( )` status](Observable#unsubscribing) before it emits any item to (or sends any notification to) the Subscriber. There&#8217;s no need to waste time generating items that no Subscriber is interested in seeing.
* Take care that your sequence operator obeys the core tenets of the Observable contract:
  * It may call a Subscriber&#8217;s [`onNext( )`](Observable#onnext-oncompleted-and-onerror) method any number of times, but these calls must be non-overlapping.
  * It may call either a Subscriber&#8217;s [`onCompleted( )`](Observable#onnext-oncompleted-and-onerror) or [`onError( )`](Observable#onnext-oncompleted-and-onerror) method, but not both, exactly once, and it may not subsequently call a Subscriber&#8217;s [`onNext( )`](Observable#onnext-oncompleted-and-onerror) method.
  * If you are unable to guarantee that your operator conforms to the above two tenets, you can add the [`serialize( )`](Observable-Utility-Operators#serialize) operator to it, which will force the correct behavior.
* Keep an eye on [Issue #1962](https://github.com/ReactiveX/RxJava/issues/1962) &mdash; there are plans to create a test scaffold that you can use to write tests which verify that your new operator conforms to the Observable contract.
* Do not block within your operator.
* When possible, you should compose new operators by combining existing operators, rather than implementing them with new code. RxJava itself does this with some of its standard operators, for example:
  * [`first( )`](http://reactivex.io/documentation/operators/first.html) is defined as <tt>[take(1)](http://reactivex.io/documentation/operators/take.html).[single( )](http://reactivex.io/documentation/operators/first.html)</tt>
  * [`ignoreElements( )`](http://reactivex.io/documentation/operators/ignoreelements.html) is defined as <tt>[filter(alwaysFalse( ))](http://reactivex.io/documentation/operators/filter.html)</tt>
  * [`reduce(a)`](http://reactivex.io/documentation/operators/reduce.html) is defined as <tt>[scan(a)](http://reactivex.io/documentation/operators/scan.html).[last( )](http://reactivex.io/documentation/operators/last.html)</tt>
* If your operator uses functions or lambdas that are passed in as parameters (predicates, for instance), note that these may be sources of exceptions, and be prepared to catch these and notify subscribers via `onError( )` calls.
  * Some exceptions are considered &ldquo;fatal&rdquo; and for them there&#8217;s no point in trying to call `onError( )` because that will either be futile or will just compound the problem. You can use the `Exceptions.throwIfFatal(throwable)` method to filter out such fatal exceptions and rethrow them rather than try to notify about them.
* In general, notify subscribers of error conditions immediately, rather than making an effort to emit more items first.
* Be aware that &ldquo;<code>null</code>&rdquo; is a valid item that may be emitted by an Observable. A frequent source of bugs is to test some variable meant to hold an emitted item against <code>null</code> as a substitute for testing whether or not an item was emitted. An emission of &ldquo;<code>null</code>&rdquo; is still an emission and is not the same as not emitting anything.
* It can be tricky to make your operator behave well in *backpressure* scenarios. See [Advanced RxJava](http://akarnokd.blogspot.hu/), a blog from Dávid Karnok, for a good discussion of the factors at play and how to deal with them.