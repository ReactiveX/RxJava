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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.Observer;
import rx.functions.*;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorWindowWithSizeTest {

    private static <T> List<List<T>> toLists(Observable<Observable<T>> observables) {

        final List<List<T>> lists = new ArrayList<List<T>>();
        Observable.concat(observables.map(new Func1<Observable<T>, Observable<List<T>>>() {
            @Override
            public Observable<List<T>> call(Observable<T> xs) {
                return xs.toList();
            }
        }))
                .toBlocking()
                .forEach(new Action1<List<T>>() {
                    @Override
                    public void call(List<T> xs) {
                        lists.add(xs);
                    }
                });
        return lists;
    }

    @Test
    public void testNonOverlappingWindows() {
        Observable<String> subject = Observable.just("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testSkipAndCountGaplessWindows() {
        Observable<String> subject = Observable.just("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(3, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testOverlappingWindows() {
        Observable<String> subject = Observable.from(new String[] { "zero", "one", "two", "three", "four", "five" });
        Observable<Observable<String>> windowed = subject.window(3, 1);

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
        Observable<String> subject = Observable.just("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(2, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testWindowUnsubscribeNonOverlapping() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 10000).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer t1) {
                count.incrementAndGet();
            }

        }).window(5).take(2)).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        //        System.out.println(ts.getOnNextEvents());
        assertEquals(10, count.get());
    }

    @Test
    public void testWindowUnsubscribeNonOverlappingAsyncSource() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 100000)
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        count.incrementAndGet();
                    }

                })
                .observeOn(Schedulers.computation())
                .window(5)
                .take(2))
                .subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        // make sure we don't emit all values ... the unsubscribe should propagate
        assertTrue(count.get() < 100000);
    }

    @Test
    public void testWindowUnsubscribeOverlapping() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 10000).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer t1) {
                count.incrementAndGet();
            }

        }).window(5, 4).take(2)).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        //        System.out.println(ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 5, 6, 7, 8, 9));
        assertEquals(9, count.get());
    }

    @Test
    public void testWindowUnsubscribeOverlappingAsyncSource() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 100000)
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        count.incrementAndGet();
                    }

                })
                .observeOn(Schedulers.computation())
                .window(5, 4)
                .take(2))
                .subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 5, 6, 7, 8, 9));
        // make sure we don't emit all values ... the unsubscribe should propagate
        assertTrue(count.get() < 100000);
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<String>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }
    
    @Test
    public void testBackpressureOuter() {
        Observable<Observable<Integer>> source = Observable.range(1, 10).window(3);
        
        final List<Integer> list = new ArrayList<Integer>();
        
        @SuppressWarnings("unchecked")
        final Observer<Integer> o = mock(Observer.class);
        
        source.subscribe(new Subscriber<Observable<Integer>>() {
            @Override
            public void onStart() {
                request(1);
            }
            @Override
            public void onNext(Observable<Integer> t) {
                t.subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer t) {
                        list.add(t);
                    }
                    @Override
                    public void onError(Throwable e) {
                        o.onError(e);
                    }
                    @Override
                    public void onCompleted() {
                        o.onCompleted();
                    }
                });
            }
            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }
            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        });
        
        assertEquals(Arrays.asList(1, 2, 3), list);
        
        verify(o, never()).onError(any(Throwable.class));
        verify(o, times(1)).onCompleted(); // 1 inner
    }

    public static Observable<Integer> hotStream() {
        return Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> s) {
                while (!s.isUnsubscribed()) {
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
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final int indicator = 999999999;
        
        hotStream()
        .window(10)
        .take(2)
        .flatMap(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> w) {
                return w.startWith(indicator);
            }
        }).subscribe(ts);
        
        ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
        ts.assertCompleted();
        Assert.assertFalse(ts.getOnNextEvents().isEmpty());
    }
    
    @Ignore("Requires #3678")
    @Test
    @SuppressWarnings("unchecked")
    public void testBackpressureOuterInexact() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0);
        
        Observable.range(1, 5).window(2, 1)
        .map(new Func1<Observable<Integer>, Observable<List<Integer>>>() {
            @Override
            public Observable<List<Integer>> call(Observable<Integer> t) {
                return t.toList();
            }
        }).concatMap(UtilityFunctions.<Observable<List<Integer>>>identity())
        .subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotCompleted();
        
        ts.requestMore(2);

        ts.assertValues(Arrays.asList(1, 2), Arrays.asList(2, 3));
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(5);

        System.out.println(ts.getOnNextEvents());
        
        ts.assertValues(Arrays.asList(1, 2), Arrays.asList(2, 3),
                Arrays.asList(3, 4), Arrays.asList(4, 5), Arrays.asList(5));
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void testBackpressureOuterOverlap() {
        Observable<Observable<Integer>> source = Observable.range(1, 10).window(3, 1);
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create(0L);
        
        source.subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValueCount(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(7);
        
        ts.assertValueCount(8);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(3);

        ts.assertValueCount(10);
        ts.assertCompleted();
        ts.assertNoErrors();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCountInvalid() {
        Observable.range(1, 10).window(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSkipInvalid() {
        Observable.range(1, 10).window(3, 0);
    }
    @Test
    public void testTake1Overlapping() {
        Observable<Observable<Integer>> source = Observable.range(1, 10).window(3, 1).take(1);
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create(0L);

        source.subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(2);
        
        ts.assertValueCount(1);
        ts.assertCompleted();
        ts.assertNoErrors();

        TestSubscriber<Integer> ts1 = TestSubscriber.create();
        
        ts.getOnNextEvents().get(0).subscribe(ts1);
        
        ts1.assertValues(1, 2, 3);
        ts1.assertCompleted();
        ts1.assertNoErrors();
    }
}