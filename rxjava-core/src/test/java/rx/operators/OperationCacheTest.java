package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.util.functions.Action1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static rx.operators.OperationCache.cache;

public class OperationCacheTest {

  @Test
  public void testCache() throws InterruptedException {
    final AtomicInteger counter = new AtomicInteger();
    Observable<String> o = Observable.create(cache(Observable.create(new Observable.OnSubscribeFunc<String>() {

      @Override
      public Subscription onSubscribe(final Observer<? super String> observer) {
        final BooleanSubscription subscription = new BooleanSubscription();
        new Thread(new Runnable() {

          @Override
          public void run() {
            counter.incrementAndGet();
            System.out.println("published observable being executed");
            observer.onNext("one");
            observer.onCompleted();
          }
        }).start();
        return subscription;
      }
    })));

    // we then expect the following 2 subscriptions to get that same value
    final CountDownLatch latch = new CountDownLatch(2);

    // subscribe once
    o.subscribe(new Action1<String>() {

      @Override
      public void call(String v) {
        assertEquals("one", v);
        System.out.println("v: " + v);
        latch.countDown();
      }
    });

    // subscribe again
    o.subscribe(new Action1<String>() {

      @Override
      public void call(String v) {
        assertEquals("one", v);
        System.out.println("v: " + v);
        latch.countDown();
      }
    });

    if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
      fail("subscriptions did not receive values");
    }
    assertEquals(1, counter.get());
  }
}
