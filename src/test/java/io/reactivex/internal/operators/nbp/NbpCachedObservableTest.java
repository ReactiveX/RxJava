/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpCachedObservableTest {
    @Test
    public void testColdReplayNoBackpressure() {
        NbpCachedObservable<Integer> source = NbpCachedObservable.from(NbpObservable.range(0, 1000));
        
        assertFalse("Source is connected!", source.isConnected());
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.subscribe(ts);

        assertTrue("Source is not connected!", source.isConnected());
        assertFalse("Subscribers retained!", source.hasObservers());
        
        ts.assertNoErrors();
        ts.assertTerminated();
        List<Integer> onNextEvents = ts.values();
        assertEquals(1000, onNextEvents.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
    }
    
    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        NbpObservable<String> o = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(final NbpSubscriber<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published NbpObservable being executed");
                        NbpObserver.onNext("one");
                        NbpObserver.onComplete();
                    }
                }).start();
            }
        }).cache();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(v -> {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
        });

        // subscribe again
        o.subscribe(v -> {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void testUnsubscribeSource() {
        Runnable unsubscribe = mock(Runnable.class);
        NbpObservable<Integer> o = NbpObservable.just(1).doOnCancel(unsubscribe).cache();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        verify(unsubscribe, times(1)).run();
    }
    
    @Test
    public void testTake() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();

        NbpCachedObservable<Integer> cached = NbpCachedObservable.from(NbpObservable.range(1, 100));
        cached.take(10).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
//        ts.assertUnsubscribed(); // FIXME no longer valid 
        assertFalse(cached.hasObservers());
    }
    
    @Test
    public void testAsync() {
        NbpObservable<Integer> source = NbpObservable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            NbpTestSubscriber<Integer> ts1 = new NbpTestSubscriber<>();
            
            NbpCachedObservable<Integer> cached = NbpCachedObservable.from(source);
            
            cached.observeOn(Schedulers.computation()).subscribe(ts1);
            
            ts1.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertComplete();
            assertEquals(10000, ts1.values().size());
            
            NbpTestSubscriber<Integer> ts2 = new NbpTestSubscriber<>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);
            
            ts2.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertComplete();
            assertEquals(10000, ts2.values().size());
        }
    }
    @Test
    public void testAsyncComeAndGo() {
        NbpObservable<Long> source = NbpObservable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        NbpCachedObservable<Long> cached = NbpCachedObservable.from(source);
        
        NbpObservable<Long> output = cached.observeOn(Schedulers.computation());
        
        List<NbpTestSubscriber<Long>> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            NbpTestSubscriber<Long> ts = new NbpTestSubscriber<>();
            list.add(ts);
            output.skip(i * 10).take(10).subscribe(ts);
        }

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (NbpTestSubscriber<Long> ts : list) {
            ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertComplete();
            
            for (int i = j * 10; i < j * 10 + 10; i++) {
                expected.set(i - j * 10, (long)i);
            }
            
            ts.assertValueSequence(expected);
            
            j++;
        }
    }
    
    @Test
    public void testNoMissingBackpressureException() {
        final int m = 4 * 1000 * 1000;
        NbpObservable<Integer> firehose = NbpObservable.create(new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(NbpSubscriber<? super Integer> t) {
                t.onSubscribe(EmptyDisposable.INSTANCE);
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onComplete();
            }
        });
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        firehose.cache().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);
        
        ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertComplete();
        
        assertEquals(100, ts.values().size());
    }
    
    @Test
    public void testValuesAndThenError() {
        NbpObservable<Integer> source = NbpObservable.range(1, 10)
                .concatWith(NbpObservable.<Integer>error(new TestException()))
                .cache();
        
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        source.subscribe(ts);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNotComplete();
        ts.assertError(TestException.class);
        
        NbpTestSubscriber<Integer> ts2 = new NbpTestSubscriber<>();
        source.subscribe(ts2);
        
        ts2.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts2.assertNotComplete();
        ts2.assertError(TestException.class);
    }
    
    @Test
    public void unsafeChildThrows() {
        final AtomicInteger count = new AtomicInteger();
        
        NbpObservable<Integer> source = NbpObservable.range(1, 100)
        .doOnNext(t -> count.getAndIncrement())
        .cache();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };
        
        source.unsafeSubscribe(ts);
        
        Assert.assertEquals(100, count.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }
}