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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableWindowWithSizeTest extends RxJavaTest {

    private static <T> List<List<T>> toLists(Flowable<Flowable<T>> observables) {

        return observables.flatMapSingle(new Function<Flowable<T>, SingleSource<List<T>>>() {
            @Override
            public SingleSource<List<T>> apply(Flowable<T> w) throws Throwable {
                return w.toList();
            }
        }).toList().blockingGet();
    }

    @Test
    public void nonOverlappingWindows() {
        Flowable<String> subject = Flowable.just("one", "two", "three", "four", "five");
        Flowable<Flowable<String>> windowed = subject.window(3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void skipAndCountGaplessWindows() {
        Flowable<String> subject = Flowable.just("one", "two", "three", "four", "five");
        Flowable<Flowable<String>> windowed = subject.window(3, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void overlappingWindows() {
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
    public void skipAndCountWindowsWithGaps() {
        Flowable<String> subject = Flowable.just("one", "two", "three", "four", "five");
        Flowable<Flowable<String>> windowed = subject.window(2, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void windowUnsubscribeNonOverlapping() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        final AtomicInteger count = new AtomicInteger();
        Flowable.merge(Flowable.range(1, 10000).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                count.incrementAndGet();
            }

        }).window(5).take(2))
        .subscribe(ts);

        ts.awaitDone(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        //        System.out.println(ts.getOnNextEvents());
        assertEquals(10, count.get());
    }

    @Test
    public void windowUnsubscribeNonOverlappingAsyncSource() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        ts.awaitDone(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // make sure we don't emit all values ... the unsubscribe should propagate
        assertTrue(count.get() < 100000);
    }

    @Test
    public void windowUnsubscribeOverlapping() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        final AtomicInteger count = new AtomicInteger();
        Flowable.merge(Flowable.range(1, 10000).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                count.incrementAndGet();
            }

        }).window(5, 4).take(2)).subscribe(ts);
        ts.awaitDone(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        //        System.out.println(ts.getOnNextEvents());
        ts.assertValues(1, 2, 3, 4, 5, 5, 6, 7, 8, 9);
        assertEquals(9, count.get());
    }

    @Test
    public void windowUnsubscribeOverlappingAsyncSource() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        ts.awaitDone(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 5, 6, 7, 8, 9);
        // make sure we don't emit all values ... the unsubscribe should propagate
        // assertTrue(count.get() < 100000); // disabled: a small hiccup in the consumption may allow the source to run to completion
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    @Test
    public void backpressureOuter() {
        Flowable<Flowable<Integer>> source = Flowable.range(1, 10).window(3);

        final List<Integer> list = new ArrayList<>();

        final Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

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
                        subscriber.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), list);

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete(); // 1 inner
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
    public void takeFlatMapCompletes() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final int indicator = 999999999;

        hotStream()
        .window(10)
        .take(2)
        .flatMap(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> w) {
                return w.startWithItem(indicator);
            }
        }).subscribe(ts);

        ts.awaitDone(2, TimeUnit.SECONDS);
        ts.assertComplete();
        ts.assertValueCount(22);
    }

    @Test
    public void backpressureOuterInexact() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<>(0L);

        Flowable.range(1, 5)
        .window(2, 1)
        .map(new Function<Flowable<Integer>, Flowable<List<Integer>>>() {
            @Override
            public Flowable<List<Integer>> apply(Flowable<Integer> t) {
                return t.toList().toFlowable();
            }
        })
        .concatMapEager(new Function<Flowable<List<Integer>>, Publisher<List<Integer>>>() {
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
            public Flowable<Flowable<Object>> apply(Flowable<Object> f) throws Exception {
                return f.window(1);
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Flowable<Object>>>() {
            @Override
            public Flowable<Flowable<Object>> apply(Flowable<Object> f) throws Exception {
                return f.window(2, 1);
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Flowable<Object>>>() {
            @Override
            public Flowable<Flowable<Object>> apply(Flowable<Object> f) throws Exception {
                return f.window(1, 2);
            }
        });
    }

    @Test
    public void errorExact() {
        Flowable.error(new TestException())
        .window(1)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorSkip() {
        Flowable.error(new TestException())
        .window(1, 2)
        .test()
        .assertFailure(TestException.class);
    }

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

    @Test
    public void cancellingWindowCancelsUpstreamSize() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.window(10)
        .take(1)
        .flatMap(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> w) throws Throwable {
                return w.take(1);
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts
        .assertResult(1);

        assertFalse("Processor still has subscribers!", pp.hasSubscribers());
    }

    @Test
    public void windowAbandonmentCancelsUpstreamSize() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final AtomicReference<Flowable<Integer>> inner = new AtomicReference<>();

        TestSubscriber<Flowable<Integer>> ts = pp.window(10)
        .take(1)
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> v) throws Throwable {
                inner.set(v);
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertFalse("Processor still has subscribers!", pp.hasSubscribers());

        inner.get().test().assertResult(1);
    }

    @Test
    public void cancellingWindowCancelsUpstreamSkip() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.window(5, 10)
        .take(1)
        .flatMap(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> w) throws Throwable {
                return w.take(1);
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts
        .assertResult(1);

        assertFalse("Processor still has subscribers!", pp.hasSubscribers());
    }

    @Test
    public void windowAbandonmentCancelsUpstreamSkip() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final AtomicReference<Flowable<Integer>> inner = new AtomicReference<>();

        TestSubscriber<Flowable<Integer>> ts = pp.window(5, 10)
        .take(1)
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> v) throws Throwable {
                inner.set(v);
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertFalse("Processor still has subscribers!", pp.hasSubscribers());

        inner.get().test().assertResult(1);
    }

    @Test
    public void cancellingWindowCancelsUpstreamOverlap() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.window(5, 3)
        .take(1)
        .flatMap(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> w) throws Throwable {
                return w.take(1);
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts
        .assertResult(1);

        assertFalse("Processor still has subscribers!", pp.hasSubscribers());
    }

    @Test
    public void windowAbandonmentCancelsUpstreamOverlap() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final AtomicReference<Flowable<Integer>> inner = new AtomicReference<>();

        TestSubscriber<Flowable<Integer>> ts = pp.window(5, 3)
        .take(1)
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> v) throws Throwable {
                inner.set(v);
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertFalse("Processor still has subscribers!", pp.hasSubscribers());

        inner.get().test().assertResult(1);
    }
}
