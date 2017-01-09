/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.*;

public class FlowableWindowWithSizeTest {

    private static <T> List<List<T>> toLists(Flowable<Flowable<T>> observables) {

        final List<List<T>> lists = new ArrayList<List<T>>();
        Flowable.concat(observables.map(new Function<Flowable<T>, Flowable<List<T>>>() {
            @Override
            public Flowable<List<T>> apply(Flowable<T> xs) {
                return xs.toList().toFlowable();
            }
        }))
                .blockingForEach(new Consumer<List<T>>() {
                    @Override
                    public void accept(List<T> xs) {
                        lists.add(xs);
                    }
                });
        return lists;
    }

    @Test
    public void testNonOverlappingWindows() {
        Flowable<String> subject = Flowable.just("one", "two", "three", "four", "five");
        Flowable<Flowable<String>> windowed = subject.window(3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testSkipAndCountGaplessWindows() {
        Flowable<String> subject = Flowable.just("one", "two", "three", "four", "five");
        Flowable<Flowable<String>> windowed = subject.window(3, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testOverlappingWindows() {
        Flowable<String> subject = Flowable.fromArray(new String[] { "zero", "one", "two", "three", "four", "five" });
        Flowable<Flowable<String>> windowed = subject.window(3, 1);

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
        Flowable<String> subject = Flowable.just("one", "two", "three", "four", "five");
        Flowable<Flowable<String>> windowed = subject.window(2, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testWindowUnsubscribeNonOverlapping() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Flowable.merge(Flowable.range(1, 10000).doOnNext(new Consumer<Integer>() {

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
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Flowable.merge(Flowable.range(1, 100000)
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
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Flowable.merge(Flowable.range(1, 10000).doOnNext(new Consumer<Integer>() {

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
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Flowable.merge(Flowable.range(1, 100000)
                .doOnNext(new Consumer<Integer>() {

                    @Override
                    public void accept(Integer t1) {
                        count.incrementAndGet();
                    }

                })
                .observeOn(Schedulers.computation())
                .window(5, 4)
                .take(2), 128)
                .subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 5, 6, 7, 8, 9);
        // make sure we don't emit all values ... the unsubscribe should propagate
        // assertTrue(count.get() < 100000); // disabled: a small hiccup in the consumption may allow the source to run to completion
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
        Flowable<Flowable<Integer>> source = Flowable.range(1, 10).window(3);

        final List<Integer> list = new ArrayList<Integer>();

        final Subscriber<Integer> o = TestHelper.mockSubscriber();

        source.subscribe(new DefaultSubscriber<Flowable<Integer>>() {
            @Override
            public void onStart() {
                request(1);
            }
            @Override
            public void onNext(Flowable<Integer> t) {
                t.subscribe(new DefaultSubscriber<Integer>() {
                    @Override
                    public void onNext(Integer t) {
                        list.add(t);
                    }
                    @Override
                    public void onError(Throwable e) {
                        o.onError(e);
                    }
                    @Override
                    public void onComplete() {
                        o.onComplete();
                    }
                });
            }
            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }
            @Override
            public void onComplete() {
                o.onComplete();
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), list);

        verify(o, never()).onError(any(Throwable.class));
        verify(o, times(1)).onComplete(); // 1 inner
    }

    public static Flowable<Integer> hotStream() {
        return Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                BooleanSubscription bs = new BooleanSubscription();
                s.onSubscribe(bs);
                while (!bs.isCancelled()) {
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
        .flatMap(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> w) {
                return w.startWith(indicator);
            }
        }).subscribe(ts);

        ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
        ts.assertComplete();
        ts.assertValueCount(22);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBackpressureOuterInexact() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0L);

        Flowable.range(1, 5)
        .window(2, 1)
        .map(new Function<Flowable<Integer>, Flowable<List<Integer>>>() {
            @Override
            public Flowable<List<Integer>> apply(Flowable<Integer> t) {
                return t.toList().toFlowable();
            }
        })
        .concatMap(new Function<Flowable<List<Integer>>, Publisher<List<Integer>>>() {
            @Override
            public Publisher<List<Integer>> apply(Flowable<List<Integer>> v) {
                return v;
            }
        })
        .subscribe(ts);

        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotComplete();

        ts.request(2);

        ts.assertValues(Arrays.asList(1, 2), Arrays.asList(2, 3));
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(5);

        System.out.println(ts.values());

        ts.assertValues(Arrays.asList(1, 2), Arrays.asList(2, 3),
                Arrays.asList(3, 4), Arrays.asList(4, 5), Arrays.asList(5));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().window(1));

        TestHelper.checkDisposed(PublishProcessor.create().window(2, 1));

        TestHelper.checkDisposed(PublishProcessor.create().window(1, 2));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Flowable<Object>>>() {
            @Override
            public Flowable<Flowable<Object>> apply(Flowable<Object> o) throws Exception {
                return o.window(1);
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Flowable<Object>>>() {
            @Override
            public Flowable<Flowable<Object>> apply(Flowable<Object> o) throws Exception {
                return o.window(2, 1);
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Flowable<Object>>>() {
            @Override
            public Flowable<Flowable<Object>> apply(Flowable<Object> o) throws Exception {
                return o.window(1, 2);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorExact() {
        Flowable.error(new TestException())
        .window(1)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorSkip() {
        Flowable.error(new TestException())
        .window(1, 2)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorOverlap() {
        Flowable.error(new TestException())
        .window(2, 1)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorExactInner() {
        @SuppressWarnings("rawtypes")
        final TestSubscriber[] to = { null };
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .window(2)
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> w) throws Exception {
                to[0] = w.test();
            }
        })
        .test()
        .assertError(TestException.class);

        to[0].assertFailure(TestException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorSkipInner() {
        @SuppressWarnings("rawtypes")
        final TestSubscriber[] to = { null };
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .window(2, 3)
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> w) throws Exception {
                to[0] = w.test();
            }
        })
        .test()
        .assertError(TestException.class);

        to[0].assertFailure(TestException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorOverlapInner() {
        @SuppressWarnings("rawtypes")
        final TestSubscriber[] to = { null };
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .window(3, 2)
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> w) throws Exception {
                to[0] = w.test();
            }
        })
        .test()
        .assertError(TestException.class);

        to[0].assertFailure(TestException.class, 1);
    }
}
