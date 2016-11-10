package rx.doppl.memory;
import android.support.annotation.NonNull;

import com.google.j2objc.annotations.AutoreleasePool;
import com.google.j2objc.annotations.Weak;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.internal.operators.CachedObservable;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by kgalligan on 11/7/16.
 */

public class SubscriberAutomaticRemovalTest
{
    /*@Test
    public void testCompleteUnsubscribe()
    {
        Observable<Long> longObservable = basicLongObservable(true);

        longObservable.subscribe(new Action1<Long>()
        {
            @Override
            public void call(Long aLong)
            {
                System.out.println("Hiya! "+ aLong);
            }
        });
    }

    @Test
    public void testSubscriptionOnComplete()
    {
        Observable<Long> longObservable = basicLongObservable(true);

        TestSubscriber<Long> ts = new TestSubscriber<>();

        longObservable.subscribe(ts);
        assertEquals(ts.getValueCount(), 4);
        ts.assertCompleted();
    }

    @Test
    public void testSubscriptionNoOnComplete()
    {
        Observable<Long> longObservable = basicLongObservable(false);

        TestSubscriber<Long> ts = new TestSubscriber<>();

        longObservable.subscribe(ts);

        assertEquals(ts.getValueCount(), 4);
        ts.assertNotCompleted();
    }*/


/*@Test
    public void testSingleProducer()
    {
        AtomicInteger refCount = new AtomicInteger();
        innerTestSingleProducer(refCount);
        assertEquals(0, refCount.get());
    }

    @AutoreleasePool
    private void innerTestSingleProducer(AtomicInteger refCount)
    {
        TestSubscriber<Long> ts = new RefCountTestSubscriber(refCount);
        assertEquals(1, refCount.get());
        SingleProducer<Long> singleProducer = new SingleProducer<>(ts, 123l);
        ts.setProducer(singleProducer);

        ts.assertValueCount(1);
    }*/

    /*@NonNull
    private Observable<Long> basicLongObservable(boolean callOnComplete)
    {
        return Observable.create(new Observable.OnSubscribe<Long>()
        {
            @Override
            public void call(Subscriber<? super Long> subscriber)
            {
                subscriber.onNext(1l);
                subscriber.onNext(3l);
                subscriber.onNext(5l);
                subscriber.onNext(5l);
                if(callOnComplete)
                    subscriber.onCompleted();
            }
        });
    }*/


    @Test
    public void testTake()
    {
        AtomicInteger refCount = new AtomicInteger();
        innerTestTake(refCount);
        assertEquals(0, refCount.get());
    }

    @AutoreleasePool
    private void innerTestTake(AtomicInteger refCount)
    {
        Observable<Integer> observable = Observable.create(new Observable.OnSubscribe<Integer>()
        {

            @Override
            public void call(Subscriber<? super Integer> subscriber)
            {
                subscriber.setProducer(new EasyProducer(subscriber));
            }
        });

        TestSubscriber<Integer> ts = new RefCountTestSubscriber(refCount);
        assertEquals(1, refCount.get());
        observable
                .take(3)
                .subscribe(ts);

        assertEquals(ts.getValueCount(), 3);
        ts.assertCompleted();
    }

    static class EasyProducer implements Producer
    {
        @Weak
        final Subscriber<? super Integer> s;
        int i =0;

        EasyProducer(Subscriber<? super Integer> s)
        {
            this.s = s;
        }

        @Override
        public void request(long n)
        {
            if(!s.isUnsubscribed())
            {
                for(i = 0; i < n; i++)
                {
                    s.onNext(i);
                }
            }
        }
    }

    private static class RefCountTestSubscriber extends TestSubscriber<Integer>
    {
        final AtomicInteger refCount;

        private RefCountTestSubscriber(AtomicInteger refCount)
        {
            this.refCount = refCount;
            refCount.incrementAndGet();
        }

        @Override
        protected void finalize() throws Throwable
        {
            super.finalize();
            refCount.decrementAndGet();
        }
    }

    @Test
    public void testObserveOn() {
        AtomicInteger refCount = new AtomicInteger();
        innerTestObserveOn(refCount);
        assertEquals(0, refCount.get());
    }

    @AutoreleasePool
    public void innerTestObserveOn(AtomicInteger refCount) {
        int NUM = (int) (RxRingBuffer.SIZE * 2.1);
        AtomicInteger c = new AtomicInteger();
        TestSubscriber<Integer> ts = new RefCountTestSubscriber(refCount);
        incrementingIntegers(c)
                .observeOn(Schedulers.computation())
                .take(NUM)
                .subscribe(ts);

        try
        {
            Thread.sleep(3000);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println("testObserveOn => Received: " + ts.getOnNextEvents().size() + "  Emitted: " + c.get());
        assertEquals(NUM, ts.getOnNextEvents().size());
        assertTrue(c.get() < RxRingBuffer.SIZE * 4);
    }

    private static Observable<Integer> incrementingIntegers(final AtomicInteger counter) {

        return Observable.create(new Observable.OnSubscribe<Integer>() {

            final AtomicLong requested = new AtomicLong();

            @Override
            public void call(final Subscriber<? super Integer> s) {
                s.setProducer(new BPTPRoducer(counter, requested, s));
            }

        });
    }



    static class BPTPRoducer implements Producer
    {
        final AtomicInteger counter;
        final AtomicLong requested;
        @Weak
        final Subscriber<? super Integer> s;
        int i = 0;

        BPTPRoducer(AtomicInteger counter, AtomicLong requested, Subscriber<? super Integer> s)
        {
            this.counter = counter;
            this.requested = requested;
            this.s = s;
        }

        @Override
        public void request(long n) {
            System.out.println("BPTPRoducer: "+ n);
            if (n == 0) {
                // nothing to do
                return;
            }
            long _c = requested.getAndAdd(n);
            if (_c == 0) {
                while (!s.isUnsubscribed()) {
                    counter.incrementAndGet();
                    s.onNext(i++);
                    if (requested.decrementAndGet() == 0) {
                        // we're done emitting the number requested so return
                        return;
                    }
                }
            }
        }
    }


    @Test
    public void testAsyncComeAndGo() {
        Observable<Long> source = Observable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        CachedObservable<Long> cached = CachedObservable.from(source);

        Observable<Long> output = cached.observeOn(Schedulers.computation());

        List<TestSubscriber<Long>> list = new ArrayList<TestSubscriber<Long>>(100);
        for (int i = 0; i < 1; i++) {
            TestSubscriber<Long> ts = new TestSubscriber<Long>();
            list.add(ts);
            output.skip(i * 10).take(10).subscribe(ts);
        }

        List<Long> expected = new ArrayList<Long>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (TestSubscriber<Long> ts : list) {
            ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertTerminalEvent();

            for (int i = j * 10; i < j * 10 + 10; i++) {
                expected.set(i - j * 10, (long)i);
            }

            ts.assertReceivedOnNext(expected);

            j++;
        }
    }
}
