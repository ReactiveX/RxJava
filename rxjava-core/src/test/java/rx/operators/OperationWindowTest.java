package rx.operators;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.Closing;
import rx.util.Closings;
import rx.util.Opening;
import rx.util.Openings;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static rx.operators.OperationWindow.window;

public class OperationWindowTest {

  private TestScheduler scheduler;

  @Before
  public void before() {
    scheduler = new TestScheduler();
  }

  private static <T> List<List<T>> toLists(Observable<Observable<T>> observable) {
    final List<T> list = new ArrayList<T>();
    final List<List<T>> lists = new ArrayList<List<T>>();

    observable.subscribe(new Action1<Observable<T>>() {
      @Override
      public void call(Observable<T> tObservable) {
        tObservable.subscribe(new Action1<T>() {
          @Override
          public void call(T t) {
            list.add(t);
          }
        });
        lists.add(new ArrayList<T>(list));
        list.clear();
      }
    });
    return lists;
  }

  @Test
  public void testNonOverlappingWindows() {
    Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
    Observable<Observable<String>> windowed = Observable.create(window(subject, 3));

    List<List<String>> windows = toLists(windowed);

    assertEquals(2, windows.size());
    assertEquals(list("one", "two", "three"), windows.get(0));
    assertEquals(list("four", "five"), windows.get(1));
  }

  @Test
  public void testSkipAndCountGaplessEindows() {
    Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
    Observable<Observable<String>> windowed = Observable.create(window(subject, 3, 3));

    List<List<String>> windows = toLists(windowed);

    assertEquals(2, windows.size());
    assertEquals(list("one", "two", "three"), windows.get(0));
    assertEquals(list("four", "five"), windows.get(1));
  }

  @Test
  public void testOverlappingWindows() {
    Observable<String> subject = Observable.from("zero", "one", "two", "three", "four", "five");
    Observable<Observable<String>> windowed = Observable.create(window(subject, 3, 1));

    List<List<String>> windows = toLists(windowed);

    assertEquals(6, windows.size());
    assertEquals(list("zero", "one", "two"), windows.get(0));
    assertEquals(list("one", "two", "three"), windows.get(1));
    assertEquals(list("two", "three", "four"), windows.get(2));
    assertEquals(list("three", "four", "five"), windows.get(3));
    assertEquals(list("four", "five"), windows.get(4));
    assertEquals(list("five"), windows.get(5));
  }

  @Test
  public void testSkipAndCountWindowsWithGaps() {
    Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
    Observable<Observable<String>> windowed = Observable.create(window(subject, 2, 3));

    List<List<String>> windows = toLists(windowed);

    assertEquals(2, windows.size());
    assertEquals(list("one", "two"), windows.get(0));
    assertEquals(list("four", "five"), windows.get(1));
  }

  @Test
  public void testTimedAndCount() {
    final List<String> list = new ArrayList<String>();
    final List<List<String>> lists = new ArrayList<List<String>>();

    Observable<String> source = Observable.create(new Observable.OnSubscribeFunc<String>() {
      @Override
      public Subscription onSubscribe(Observer<? super String> observer) {
        push(observer, "one", 10);
        push(observer, "two", 90);
        push(observer, "three", 110);
        push(observer, "four", 190);
        push(observer, "five", 210);
        complete(observer, 250);
        return Subscriptions.empty();
      }
    });

    Observable<Observable<String>> windowed = Observable.create(window(source, 100, TimeUnit.MILLISECONDS, 2, scheduler));
    windowed.subscribe(observeWindow(list, lists));

    scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS);
    assertEquals(1, lists.size());
    assertEquals(lists.get(0), list("one", "two"));

    scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS);
    assertEquals(2, lists.size());
    assertEquals(lists.get(1), list("three", "four"));

    scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
    assertEquals(3, lists.size());
    assertEquals(lists.get(2), list("five"));
  }

  @Test
  public void testTimed() {
    final List<String> list = new ArrayList<String>();
    final List<List<String>> lists = new ArrayList<List<String>>();

    Observable<String> source = Observable.create(new Observable.OnSubscribeFunc<String>() {
      @Override
      public Subscription onSubscribe(Observer<? super String> observer) {
        push(observer, "one", 98);
        push(observer, "two", 99);
        push(observer, "three", 100);
        push(observer, "four", 101);
        push(observer, "five", 102);
        complete(observer, 150);
        return Subscriptions.empty();
      }
    });

    Observable<Observable<String>> windowed = Observable.create(window(source, 100, TimeUnit.MILLISECONDS, scheduler));
    windowed.subscribe(observeWindow(list, lists));

    scheduler.advanceTimeTo(101, TimeUnit.MILLISECONDS);
    assertEquals(1, lists.size());
    assertEquals(lists.get(0), list("one", "two", "three"));

    scheduler.advanceTimeTo(201, TimeUnit.MILLISECONDS);
    assertEquals(2, lists.size());
    assertEquals(lists.get(1), list("four", "five"));
  }

  @Test
  public void testObservableBasedOpenerAndCloser() {
    final List<String> list = new ArrayList<String>();
    final List<List<String>> lists = new ArrayList<List<String>>();

    Observable<String> source = Observable.create(new Observable.OnSubscribeFunc<String>() {
      @Override
      public Subscription onSubscribe(Observer<? super String> observer) {
        push(observer, "one", 10);
        push(observer, "two", 60);
        push(observer, "three", 110);
        push(observer, "four", 160);
        push(observer, "five", 210);
        complete(observer, 500);
        return Subscriptions.empty();
      }
    });

    Observable<Opening> openings = Observable.create(new Observable.OnSubscribeFunc<Opening>() {
      @Override
      public Subscription onSubscribe(Observer<? super Opening> observer) {
        push(observer, Openings.create(), 50);
        push(observer, Openings.create(), 200);
        complete(observer, 250);
        return Subscriptions.empty();
      }
    });

    Func1<Opening, Observable<Closing>> closer = new Func1<Opening, Observable<Closing>>() {
      @Override
      public Observable<Closing> call(Opening opening) {
        return Observable.create(new Observable.OnSubscribeFunc<Closing>() {
          @Override
          public Subscription onSubscribe(Observer<? super Closing> observer) {
            push(observer, Closings.create(), 100);
            complete(observer, 101);
            return Subscriptions.empty();
          }
        });
      }
    };

    Observable<Observable<String>> windowed = Observable.create(window(source, openings, closer));
    windowed.subscribe(observeWindow(list, lists));

    scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
    assertEquals(2, lists.size());
    assertEquals(lists.get(0), list("two", "three"));
    assertEquals(lists.get(1), list("five"));
  }

  @Test
  public void testObservableBasedCloser() {
    final List<String> list = new ArrayList<String>();
    final List<List<String>> lists = new ArrayList<List<String>>();

    Observable<String> source = Observable.create(new Observable.OnSubscribeFunc<String>() {
      @Override
      public Subscription onSubscribe(Observer<? super String> observer) {
        push(observer, "one", 10);
        push(observer, "two", 60);
        push(observer, "three", 110);
        push(observer, "four", 160);
        push(observer, "five", 210);
        complete(observer, 250);
        return Subscriptions.empty();
      }
    });

    Func0<Observable<Closing>> closer = new Func0<Observable<Closing>>() {
      @Override
      public Observable<Closing> call() {
        return Observable.create(new Observable.OnSubscribeFunc<Closing>() {
          @Override
          public Subscription onSubscribe(Observer<? super Closing> observer) {
            push(observer, Closings.create(), 100);
            complete(observer, 101);
            return Subscriptions.empty();
          }
        });
      }
    };

    Observable<Observable<String>> windowed = Observable.create(window(source, closer));
    windowed.subscribe(observeWindow(list, lists));

    scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
    assertEquals(3, lists.size());
    assertEquals(lists.get(0), list("one", "two"));
    assertEquals(lists.get(1), list("three", "four"));
    assertEquals(lists.get(2), list("five"));
  }

  private List<String> list(String... args) {
    List<String> list = new ArrayList<String>();
    for (String arg : args) {
      list.add(arg);
    }
    return list;
  }

  private <T> void push(final Observer<T> observer, final T value, int delay) {
    scheduler.schedule(new Action0() {
      @Override
      public void call() {
        observer.onNext(value);
      }
    }, delay, TimeUnit.MILLISECONDS);
  }

  private void complete(final Observer<?> observer, int delay) {
    scheduler.schedule(new Action0() {
      @Override
      public void call() {
        observer.onCompleted();
      }
    }, delay, TimeUnit.MILLISECONDS);
  }

  private Action1<Observable<String>> observeWindow(final List<String> list, final List<List<String>> lists) {
    return new Action1<Observable<String>>() {
      @Override
      public void call(Observable<String> stringObservable) {
        stringObservable.subscribe(new Observer<String>() {
          @Override
          public void onCompleted() {
            lists.add(new ArrayList<String>(list));
            list.clear();
          }

          @Override
          public void onError(Throwable e) {
            fail(e.getMessage());
          }

          @Override
          public void onNext(String args) {
            list.add(args);
          }
        });
      }
    };
  }
}
