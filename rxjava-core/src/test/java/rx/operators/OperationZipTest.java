package rx.operators;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static rx.operators.OperationZip.Aggregator;
import static rx.operators.OperationZip.ZipObserver;
import static rx.operators.OperationZip.zip;

public class OperationZipTest {

  @SuppressWarnings("unchecked")
  @Test
  public void testCollectionSizeDifferentThanFunction() {
    FuncN<String> zipr = Functions.fromFunc(getConcatStringIntegerIntArrayZipr());
    //Func3<String, Integer, int[], String>

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);

    @SuppressWarnings("rawtypes")
    Collection ws = java.util.Collections.singleton(Observable.from("one", "two"));
    Observable<String> w = Observable.create(zip(ws, zipr));
    w.subscribe(aObserver);

    verify(aObserver, times(1)).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    verify(aObserver, never()).onNext(any(String.class));
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testZippingDifferentLengthObservableSequences1() {
    Observer<String> w = mock(Observer.class);

    TestObservable w1 = new TestObservable();
    TestObservable w2 = new TestObservable();
    TestObservable w3 = new TestObservable();

    Observable<String> zipW = Observable.create(zip(Observable.create(w1), Observable.create(w2), Observable.create(w3), getConcat3StringsZipr()));
    zipW.subscribe(w);

            /* simulate sending data */
    // once for w1
    w1.observer.onNext("1a");
    w1.observer.onCompleted();
    // twice for w2
    w2.observer.onNext("2a");
    w2.observer.onNext("2b");
    w2.observer.onCompleted();
    // 4 times for w3
    w3.observer.onNext("3a");
    w3.observer.onNext("3b");
    w3.observer.onNext("3c");
    w3.observer.onNext("3d");
    w3.observer.onCompleted();

            /* we should have been called 1 time on the Observer */
    InOrder inOrder = inOrder(w);
    inOrder.verify(w).onNext("1a2a3a");

    inOrder.verify(w, times(1)).onCompleted();
  }

  @Test
  public void testZippingDifferentLengthObservableSequences2() {
    @SuppressWarnings("unchecked")
    Observer<String> w = mock(Observer.class);

    TestObservable w1 = new TestObservable();
    TestObservable w2 = new TestObservable();
    TestObservable w3 = new TestObservable();

    Observable<String> zipW = Observable.create(zip(Observable.create(w1), Observable.create(w2), Observable.create(w3), getConcat3StringsZipr()));
    zipW.subscribe(w);

            /* simulate sending data */
    // 4 times for w1
    w1.observer.onNext("1a");
    w1.observer.onNext("1b");
    w1.observer.onNext("1c");
    w1.observer.onNext("1d");
    w1.observer.onCompleted();
    // twice for w2
    w2.observer.onNext("2a");
    w2.observer.onNext("2b");
    w2.observer.onCompleted();
    // 1 times for w3
    w3.observer.onNext("3a");
    w3.observer.onCompleted();

            /* we should have been called 1 time on the Observer */
    InOrder inOrder = inOrder(w);
    inOrder.verify(w).onNext("1a2a3a");

    inOrder.verify(w, times(1)).onCompleted();

  }

  /**
   * Testing internal private logic due to the complexity so I want to use TDD to test as a I build it rather than relying purely on the overall functionality expected by the public methods.
   */
  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testAggregatorSimple() {
    FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
    Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);
    a.onSubscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
    ZipObserver<String, String> r1 = mock(ZipObserver.class);
    ZipObserver<String, String> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
    a.addObserver(r1);
    a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
    a.next(r1, "hello");
    a.next(r2, "world");

    InOrder inOrder = inOrder(aObserver);

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    inOrder.verify(aObserver, times(1)).onNext("helloworld");

    a.next(r1, "hello ");
    a.next(r2, "again");

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    inOrder.verify(aObserver, times(1)).onNext("hello again");

    a.complete(r1);
    a.complete(r2);

    inOrder.verify(aObserver, never()).onNext(anyString());
    verify(aObserver, times(1)).onCompleted();
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testAggregatorDifferentSizedResultsWithOnComplete() {
    FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
    Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);
    a.onSubscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
    ZipObserver<String, String> r1 = mock(ZipObserver.class);
    ZipObserver<String, String> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
    a.addObserver(r1);
    a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
    a.next(r1, "hello");
    a.next(r2, "world");
    a.complete(r2);

    InOrder inOrder = inOrder(aObserver);

    inOrder.verify(aObserver, never()).onError(any(Throwable.class));
    inOrder.verify(aObserver, never()).onCompleted();
    inOrder.verify(aObserver, times(1)).onNext("helloworld");

    a.next(r1, "hi");
    a.complete(r1);

    inOrder.verify(aObserver, never()).onError(any(Throwable.class));
    inOrder.verify(aObserver, times(1)).onCompleted();
    inOrder.verify(aObserver, never()).onNext(anyString());
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testAggregateMultipleTypes() {
    FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
    Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);
    a.onSubscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
    ZipObserver<String, String> r1 = mock(ZipObserver.class);
    ZipObserver<String, Integer> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
    a.addObserver(r1);
    a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
    a.next(r1, "hello");
    a.next(r2, "world");
    a.complete(r2);

    InOrder inOrder = inOrder(aObserver);

    inOrder.verify(aObserver, never()).onError(any(Throwable.class));
    inOrder.verify(aObserver, never()).onCompleted();
    inOrder.verify(aObserver, times(1)).onNext("helloworld");

    a.next(r1, "hi");
    a.complete(r1);

    inOrder.verify(aObserver, never()).onError(any(Throwable.class));
    inOrder.verify(aObserver, times(1)).onCompleted();
    inOrder.verify(aObserver, never()).onNext(anyString());
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testAggregate3Types() {
    FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
    Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);
    a.onSubscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
    ZipObserver<String, String> r1 = mock(ZipObserver.class);
    ZipObserver<String, Integer> r2 = mock(ZipObserver.class);
    ZipObserver<String, int[]> r3 = mock(ZipObserver.class);

            /* pretend we're starting up */
    a.addObserver(r1);
    a.addObserver(r2);
    a.addObserver(r3);

            /* simulate the Observables pushing data into the aggregator */
    a.next(r1, "hello");
    a.next(r2, 2);
    a.next(r3, new int[] { 5, 6, 7 });

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    verify(aObserver, times(1)).onNext("hello2[5, 6, 7]");
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testAggregatorsWithDifferentSizesAndTiming() {
    FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
    Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);
    a.onSubscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
    ZipObserver<String, String> r1 = mock(ZipObserver.class);
    ZipObserver<String, String> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
    a.addObserver(r1);
    a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
    a.next(r1, "one");
    a.next(r1, "two");
    a.next(r1, "three");
    a.next(r2, "A");

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    verify(aObserver, times(1)).onNext("oneA");

    a.next(r1, "four");
    a.complete(r1);
    a.next(r2, "B");
    verify(aObserver, times(1)).onNext("twoB");
    a.next(r2, "C");
    verify(aObserver, times(1)).onNext("threeC");
    a.next(r2, "D");
    verify(aObserver, times(1)).onNext("fourD");
    a.next(r2, "E");
    verify(aObserver, never()).onNext("E");
    a.complete(r2);

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testAggregatorError() {
    FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
    Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);
    a.onSubscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
    ZipObserver<String, String> r1 = mock(ZipObserver.class);
    ZipObserver<String, String> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
    a.addObserver(r1);
    a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
    a.next(r1, "hello");
    a.next(r2, "world");

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    verify(aObserver, times(1)).onNext("helloworld");

    a.error(r1, new RuntimeException(""));
    a.next(r1, "hello");
    a.next(r2, "again");

    verify(aObserver, times(1)).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    // we don't want to be called again after an error
    verify(aObserver, times(0)).onNext("helloagain");
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testAggregatorUnsubscribe() {
    FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
    Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);
    Subscription subscription = a.onSubscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
    ZipObserver<String, String> r1 = mock(ZipObserver.class);
    ZipObserver<String, String> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
    a.addObserver(r1);
    a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
    a.next(r1, "hello");
    a.next(r2, "world");

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    verify(aObserver, times(1)).onNext("helloworld");

    subscription.unsubscribe();
    a.next(r1, "hello");
    a.next(r2, "again");

    verify(aObserver, times(0)).onError(any(Throwable.class));
    verify(aObserver, never()).onCompleted();
    // we don't want to be called again after an error
    verify(aObserver, times(0)).onNext("helloagain");
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testAggregatorEarlyCompletion() {
    FuncN<String> zipr = getConcatZipr();
            /* create the aggregator which will execute the zip function when all Observables provide values */
    Aggregator<String> a = new Aggregator<String>(zipr);

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);
    a.onSubscribe(aObserver);

            /* mock the Observable Observers that are 'pushing' data for us */
    ZipObserver<String, String> r1 = mock(ZipObserver.class);
    ZipObserver<String, String> r2 = mock(ZipObserver.class);

            /* pretend we're starting up */
    a.addObserver(r1);
    a.addObserver(r2);

            /* simulate the Observables pushing data into the aggregator */
    a.next(r1, "one");
    a.next(r1, "two");
    a.complete(r1);
    a.next(r2, "A");

    InOrder inOrder = inOrder(aObserver);

    inOrder.verify(aObserver, never()).onError(any(Throwable.class));
    inOrder.verify(aObserver, never()).onCompleted();
    inOrder.verify(aObserver, times(1)).onNext("oneA");

    a.complete(r2);

    inOrder.verify(aObserver, never()).onError(any(Throwable.class));
    inOrder.verify(aObserver, times(1)).onCompleted();
    inOrder.verify(aObserver, never()).onNext(anyString());
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testZip2Types() {
    Func2<String, Integer, String> zipr = getConcatStringIntegerZipr();

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);

    Observable<String> w = Observable.create(zip(Observable.from("one", "two"), Observable.from(2, 3, 4), zipr));
    w.subscribe(aObserver);

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
    verify(aObserver, times(1)).onNext("one2");
    verify(aObserver, times(1)).onNext("two3");
    verify(aObserver, never()).onNext("4");
  }

  @SuppressWarnings("unchecked")
        /* mock calls don't do generics */
  @Test
  public void testZip3Types() {
    Func3<String, Integer, int[], String> zipr = getConcatStringIntegerIntArrayZipr();

            /* define a Observer to receive aggregated events */
    Observer<String> aObserver = mock(Observer.class);

    Observable<String> w = Observable.create(zip(Observable.from("one", "two"), Observable.from(2), Observable.from(new int[] { 4, 5, 6 }), zipr));
    w.subscribe(aObserver);

    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
    verify(aObserver, times(1)).onNext("one2[4, 5, 6]");
    verify(aObserver, never()).onNext("two");
  }

  @Test
  public void testOnNextExceptionInvokesOnError() {
    Func2<Integer, Integer, Integer> zipr = getDivideZipr();

    @SuppressWarnings("unchecked")
    Observer<Integer> aObserver = mock(Observer.class);

    Observable<Integer> w = Observable.create(zip(Observable.from(10, 20, 30), Observable.from(0, 1, 2), zipr));
    w.subscribe(aObserver);

    verify(aObserver, times(1)).onError(any(Throwable.class));
  }

  private Func2<Integer, Integer, Integer> getDivideZipr() {
    Func2<Integer, Integer, Integer> zipr = new Func2<Integer, Integer, Integer>() {

      @Override
      public Integer call(Integer i1, Integer i2) {
        return i1 / i2;
      }

    };
    return zipr;
  }

  private Func3<String, String, String, String> getConcat3StringsZipr() {
    Func3<String, String, String, String> zipr = new Func3<String, String, String, String>() {

      @Override
      public String call(String a1, String a2, String a3) {
        if (a1 == null) {
          a1 = "";
        }
        if (a2 == null) {
          a2 = "";
        }
        if (a3 == null) {
          a3 = "";
        }
        return a1 + a2 + a3;
      }

    };
    return zipr;
  }

  private FuncN<String> getConcatZipr() {
    FuncN<String> zipr = new FuncN<String>() {

      @Override
      public String call(Object... args) {
        String returnValue = "";
        for (Object o : args) {
          if (o != null) {
            returnValue += getStringValue(o);
          }
        }
        System.out.println("returning: " + returnValue);
        return returnValue;
      }

    };
    return zipr;
  }

  private Func2<String, Integer, String> getConcatStringIntegerZipr() {
    Func2<String, Integer, String> zipr = new Func2<String, Integer, String>() {

      @Override
      public String call(String s, Integer i) {
        return getStringValue(s) + getStringValue(i);
      }

    };
    return zipr;
  }

  private Func3<String, Integer, int[], String> getConcatStringIntegerIntArrayZipr() {
    Func3<String, Integer, int[], String> zipr = new Func3<String, Integer, int[], String>() {

      @Override
      public String call(String s, Integer i, int[] iArray) {
        return getStringValue(s) + getStringValue(i) + getStringValue(iArray);
      }

    };
    return zipr;
  }

  private static String getStringValue(Object o) {
    if (o == null) {
      return "";
    } else {
      if (o instanceof int[]) {
        return Arrays.toString((int[]) o);
      } else {
        return String.valueOf(o);
      }
    }
  }

  private static class TestObservable implements Observable.OnSubscribeFunc<String> {

    Observer<? super String> observer;

    @Override
    public Subscription onSubscribe(Observer<? super String> Observer) {
      // just store the variable where it can be accessed so we can manually trigger it
      this.observer = Observer;
      return Subscriptions.empty();
    }

  }
}
