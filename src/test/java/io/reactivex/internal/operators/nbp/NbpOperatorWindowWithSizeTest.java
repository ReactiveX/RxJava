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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.BooleanDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorWindowWithSizeTest {

    private static <T> List<List<T>> toLists(NbpObservable<NbpObservable<T>> observables) {

        final List<List<T>> lists = new ArrayList<>();
        NbpObservable.concat(observables.map(new Function<NbpObservable<T>, NbpObservable<List<T>>>() {
            @Override
            public NbpObservable<List<T>> apply(NbpObservable<T> xs) {
                return xs.toList();
            }
        }))
                .toBlocking()
                .forEach(new Consumer<List<T>>() {
                    @Override
                    public void accept(List<T> xs) {
                        lists.add(xs);
                    }
                });
        return lists;
    }

    @Test
    public void testNonOverlappingWindows() {
        NbpObservable<String> subject = NbpObservable.just("one", "two", "three", "four", "five");
        NbpObservable<NbpObservable<String>> windowed = subject.window(3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testSkipAndCountGaplessWindows() {
        NbpObservable<String> subject = NbpObservable.just("one", "two", "three", "four", "five");
        NbpObservable<NbpObservable<String>> windowed = subject.window(3, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testOverlappingWindows() {
        NbpObservable<String> subject = NbpObservable.fromArray(new String[] { "zero", "one", "two", "three", "four", "five" });
        NbpObservable<NbpObservable<String>> windowed = subject.window(3, 1);

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
        NbpObservable<String> subject = NbpObservable.just("one", "two", "three", "four", "five");
        NbpObservable<NbpObservable<String>> windowed = subject.window(2, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testWindowUnsubscribeNonOverlapping() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        final AtomicInteger count = new AtomicInteger();
        NbpObservable.merge(NbpObservable.range(1, 10000).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                count.incrementAndGet();
            }

        }).window(5).take(2)).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        //        System.out.println(ts.getOnNextEvents());
        assertEquals(10, count.get());
    }

    @Test
    public void testWindowUnsubscribeNonOverlappingAsyncSource() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        final AtomicInteger count = new AtomicInteger();
        NbpObservable.merge(NbpObservable.range(1, 100000)
                .doOnNext(new Consumer<Integer>() {

                    @Override
                    public void accept(Integer t1) {
                        count.incrementAndGet();
                    }

                })
                .observeOn(Schedulers.computation())
                .window(5)
                .take(2))
                .subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // make sure we don't emit all values ... the unsubscribe should propagate
        assertTrue(count.get() < 100000);
    }

    @Test
    public void testWindowUnsubscribeOverlapping() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        final AtomicInteger count = new AtomicInteger();
        NbpObservable.merge(NbpObservable.range(1, 10000).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                count.incrementAndGet();
            }

        }).window(5, 4).take(2)).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        //        System.out.println(ts.getOnNextEvents());
        ts.assertValues(1, 2, 3, 4, 5, 5, 6, 7, 8, 9);
        assertEquals(9, count.get());
    }

    @Test
    public void testWindowUnsubscribeOverlappingAsyncSource() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        final AtomicInteger count = new AtomicInteger();
        NbpObservable.merge(NbpObservable.range(1, 100000)
                .doOnNext(new Consumer<Integer>() {

                    @Override
                    public void accept(Integer t1) {
                        count.incrementAndGet();
                    }

                })
                .observeOn(Schedulers.computation())
                .window(5, 4)
                .take(2))
                .subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 5, 6, 7, 8, 9);
        // make sure we don't emit all values ... the unsubscribe should propagate
        assertTrue(count.get() < 100000);
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }
    

    public static NbpObservable<Integer> hotStream() {
        return NbpObservable.create(new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(NbpSubscriber<? super Integer> s) {
                BooleanDisposable bs = new BooleanDisposable();
                s.onSubscribe(bs);
                while (!bs.isDisposed()) {
                    // burst some number of items
                    for (int i = 0; i < Math.random() * 20; i++) {
                        s.onNext(i);
                    }
                    try {
                        // sleep for a random amount of time
                        // NOTE: Only using Thread.sleep here as an artificial demo.
                        Thread.sleep((long) (Math.random() * 200));
                    } catch (Exception e) {
                        // do nothing
                    }
                }
                System.out.println("Hot done.");
            }
        }).subscribeOn(Schedulers.newThread()); // use newThread since we are using sleep to block
    }
    
    @Test
    public void testTakeFlatMapCompletes() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        final int indicator = 999999999;
        
        hotStream()
        .window(10)
        .take(2)
        .flatMap(new Function<NbpObservable<Integer>, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(NbpObservable<Integer> w) {
                return w.startWith(indicator);
            }
        }).subscribe(ts);
        
        ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
        ts.assertComplete();
        ts.assertValueCount(22);
    }
}