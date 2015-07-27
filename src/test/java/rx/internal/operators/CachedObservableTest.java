/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class CachedObservableTest {
    @Test
    public void testColdReplayNoBackpressure() {
        CachedObservable<Integer> source = CachedObservable.from(Observable.range(0, 1000));
        
        assertFalse("Source is connected!", source.isConnected());
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.subscribe(ts);

        assertTrue("Source is not connected!", source.isConnected());
        assertFalse("Subscribers retained!", source.hasObservers());
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        List<Integer> onNextEvents = ts.getOnNextEvents();
        assertEquals(1000, onNextEvents.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
    }
    @Test
    public void testColdReplayBackpressure() {
        CachedObservable<Integer> source = CachedObservable.from(Observable.range(0, 1000));
        
        assertFalse("Source is connected!", source.isConnected());
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.requestMore(10);
        
        source.subscribe(ts);

        assertTrue("Source is not connected!", source.isConnected());
        assertTrue("Subscribers not retained!", source.hasObservers());
        
        ts.assertNoErrors();
        assertTrue(ts.getOnCompletedEvents().isEmpty());
        List<Integer> onNextEvents = ts.getOnNextEvents();
        assertEquals(10, onNextEvents.size());

        for (int i = 0; i < 10; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
        
        ts.unsubscribe();
        assertFalse("Subscribers retained!", source.hasObservers());
    }
    
    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Observable<String> o = Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(final Subscriber<? super String> observer) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published observable being executed");
                        observer.onNext("one");
                        observer.onCompleted();
                    }
                }).start();
            }
        }).cache();

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

    @Test
    public void testUnsubscribeSource() {
        Action0 unsubscribe = mock(Action0.class);
        Observable<Integer> o = Observable.just(1).doOnUnsubscribe(unsubscribe).cache();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        verify(unsubscribe, times(1)).call();
    }
    
    @Test
    public void testTake() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        CachedObservable<Integer> cached = CachedObservable.from(Observable.range(1, 100));
        cached.take(10).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        ts.assertUnsubscribed();
        assertFalse(cached.hasObservers());
    }
    
    @Test
    public void testAsync() {
        Observable<Integer> source = Observable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
            
            CachedObservable<Integer> cached = CachedObservable.from(source);
            
            cached.observeOn(Schedulers.computation()).subscribe(ts1);
            
            ts1.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertTerminalEvent();
            assertEquals(10000, ts1.getOnNextEvents().size());
            
            TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);
            
            ts2.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertTerminalEvent();
            assertEquals(10000, ts2.getOnNextEvents().size());
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
        for (int i = 0; i < 100; i++) {
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
    
    @Test
    public void testNoMissingBackpressureException() {
        final int m = 4 * 1000 * 1000;
        Observable<Integer> firehose = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t) {
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onCompleted();
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        firehose.cache().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);
        
        ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        
        assertEquals(100, ts.getOnNextEvents().size());
    }
    
    @Test
    public void testValuesAndThenError() {
        Observable<Integer> source = Observable.range(1, 10)
                .concatWith(Observable.<Integer>error(new TestException()))
                .cache();
        
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        source.subscribe(ts);
        
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Assert.assertTrue(ts.getOnCompletedEvents().isEmpty());
        Assert.assertEquals(1, ts.getOnErrorEvents().size());
        
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();
        source.subscribe(ts2);
        
        ts2.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Assert.assertTrue(ts2.getOnCompletedEvents().isEmpty());
        Assert.assertEquals(1, ts2.getOnErrorEvents().size());
    }
    
    @Test
    public void unsafeChildThrows() {
        final AtomicInteger count = new AtomicInteger();
        
        Observable<Integer> source = Observable.range(1, 100)
        .doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        })
        .cache();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };
        
        source.unsafeSubscribe(ts);
        
        Assert.assertEquals(100, count.get());

        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
}