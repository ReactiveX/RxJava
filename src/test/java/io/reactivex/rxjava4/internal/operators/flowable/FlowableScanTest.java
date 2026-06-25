/*
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

package io.reactivex.rxjava4.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.flowable.*;
import io.reactivex.rxjava4.flowable.FlowableEventStream.Event;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.subscribers.*;
import io.reactivex.rxjava4.testsupport.*;

public class FlowableScanTest extends RxJavaTest {

    @Test
    public void scanIntegersWithInitialValue() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<Integer> flowable = Flowable.just(1, 2, 3);

        Flowable<String> m = flowable.scan("", (s, n) -> s + n.toString());
        m.subscribe(subscriber);

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onNext("");
        verify(subscriber, times(1)).onNext("1");
        verify(subscriber, times(1)).onNext("12");
        verify(subscriber, times(1)).onNext("123");
        verify(subscriber, times(4)).onNext(anyString());
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void scanIntegersWithoutInitialValue() {
        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        Flowable<Integer> flowable = Flowable.just(1, 2, 3);

        Flowable<Integer> m = flowable.scan(Integer::sum);
        m.subscribe(subscriber);

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, never()).onNext(0);
        verify(subscriber, times(1)).onNext(1);
        verify(subscriber, times(1)).onNext(3);
        verify(subscriber, times(1)).onNext(6);
        verify(subscriber, times(3)).onNext(anyInt());
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void scanIntegersWithoutInitialValueAndOnlyOneValue() {
        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        Flowable<Integer> flowable = Flowable.just(1);

        Flowable<Integer> m = flowable.scan(Integer::sum);
        m.subscribe(subscriber);

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, never()).onNext(0);
        verify(subscriber, times(1)).onNext(1);
        verify(subscriber, times(1)).onNext(anyInt());
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void shouldNotEmitUntilAfterSubscription() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.range(1, 100)
        .scan(0, Integer::sum)
        .filter(t1 -> t1 > 0)
        .subscribe(ts);

        assertEquals(100, ts.values().size());
    }

    @Test
    public void backpressureWithInitialValue() {
        final AtomicInteger count = new AtomicInteger();
        Flowable.range(1, 100)
                .scan(0, Integer::sum)
                .subscribe(new DefaultSubscriber<Integer>() /* NFI */ {

                    @Override
                    public void onStart() {
                        request(10);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Assert.fail(e.getMessage());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                    }

                });

        // we only expect to receive 10 since we request(10)
        assertEquals(10, count.get());
    }

    @Test
    public void backpressureWithoutInitialValue() {
        final AtomicInteger count = new AtomicInteger();
        Flowable.range(1, 100)
                .scan(Integer::sum)
                .subscribe(new DefaultSubscriber<Integer>() /* NFI */ {

                    @Override
                    public void onStart() {
                        request(10);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Assert.fail(e.getMessage());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                    }

                });

        // we only expect to receive 10 since we request(10)
        assertEquals(10, count.get());
    }

    @Test
    public void noBackpressureWithInitialValue() {
        final AtomicInteger count = new AtomicInteger();
        Flowable.range(1, 100)
                .scan(0, Integer::sum)
                .subscribe(new DefaultSubscriber<Integer>() /* NFI */ {

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Assert.fail(e.getMessage());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                    }

                });

        // we only expect to receive 101 as we'll receive all 100 + the initial value
        assertEquals(101, count.get());
    }

    /**
     * This uses the public API collect which uses scan under the covers.
     */
    @Test
    public void seedFactory() {
        Single<List<Integer>> o = Flowable.range(1, 10)
                .collect(ArrayList::new, List::add);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.blockingGet());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.blockingGet());
    }

    /**
     * This uses the public API collect which uses scan under the covers.
     */
    @Test
    public void seedFactoryFlowable() {
        Flowable<List<Integer>> f = Flowable.range(1, 10)
                .<List<Integer>>collect(ArrayList::new, List::add)
                .toFlowable()
                .takeLast(1);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), f.blockingSingle());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), f.blockingSingle());
    }

    @Test
    public void scanWithRequestOne() {
        Flowable<Integer> f = Flowable.just(1, 2)
        .scan(0, Integer::sum)
        .take(1);

        TestSubscriberEx<Integer> subscriber = new TestSubscriberEx<>();
        f.subscribe(subscriber);
        subscriber.assertValue(0);
        subscriber.assertTerminated();
        subscriber.assertNoErrors();
    }

    /**
     * Turns the Subscription methods into lambda callbacks.
     * @param <T> the element type of the subscriber
     */
    static class SubscriptionDelegate<T, U> implements Subscription {

        @NonNull Subscriber<? super T> subscriber;
        @NonNull Consumer3<Subscriber<? super T>, Long, U> onRequest;
        @NonNull BiConsumer<Subscriber<? super T>, U> onCancel;
        @Nullable U data;

        SubscriptionDelegate(
                @NonNull Subscriber<? super T> subscriber,
                @NonNull Consumer3<Subscriber<? super T>, Long, U> onRequest,
                @NonNull BiConsumer<Subscriber<? super T>, U> onCancel,
                @Nullable U data
                ) {
            this.subscriber = subscriber;
            this.onRequest = onRequest;
            this.onCancel = onCancel;
            this.data = data;
        }

        @Override
        public void request(long n) {
            try {
                onRequest.accept(subscriber, n, data);
            } catch(Throwable ex) {
                throw Exceptions.propagate(ex);
            }
        }

            @Override
        public void cancel() {
            try {
                onCancel.accept(subscriber, data);
            } catch(Throwable ex) {
                throw Exceptions.propagate(ex);
            }
        }
    }

    @Test
    public void scanShouldNotRequestZero() {
        final AtomicReference<Subscription> producer = new AtomicReference<>();
        Flowable<Integer> f = Flowable.unsafeCreate((Publisher<Integer>) subscriber -> {

            var requested = new AtomicBoolean(false);

            var subber = new SubscriptionDelegate<Integer, AtomicBoolean>(subscriber, (sub, _, data) -> {
                    if (data.compareAndSet(false, true)) {
                        sub.onNext(1);
                        sub.onComplete();
                    }
                }, (_, _) -> { },
                requested
            );

            Subscription p = spy(subber);
            producer.set(p);
            subscriber.onSubscribe(p);
        }).scan(100, Integer::sum);

        f.subscribe(new TestSubscriber<Integer>(1L) /* NFI */ {

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });

        verify(producer.get(), never()).request(0);
        verify(producer.get(), times(1)).request(Flowable.bufferSize() - 1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().scan((a, _) -> a));

        TestHelper.checkDisposed(PublishProcessor.<Integer>create()
                .scan(0, Integer::sum));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable((Function<Flowable<Object>, Flowable<Object>>) f -> f.scan((a, _) -> a));

        TestHelper.checkDoubleOnSubscribeFlowable((Function<Flowable<Object>, Flowable<Object>>) f ->
        f.scan(0, (a, _) -> a));
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .scan((a, _) -> a)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void neverSource() {
        Flowable.<Integer>never()
        .scan(0, Integer::sum)
        .test()
        .assertValue(0)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void unsubscribeScan() {

        FlowableEventStream.getEventStream("HTTP-ClusterB", 20)
        .scan(new HashMap<>(), (BiFunction<HashMap<String, String>, Event, HashMap<String, String>>) (accum, perInstanceEvent) -> {
            accum.put("instance", perInstanceEvent.instanceId);
            return accum;
        })
        .take(10)
        .blockingForEach(System.out::println);
    }

    @Test
    public void scanWithSeedDoesNotEmitErrorTwiceIfScanFunctionThrows() {
        final List<Throwable> list = new CopyOnWriteArrayList<>();
        Consumer<Throwable> errorConsumer = list::add;
        try {
            RxJavaPlugins.setErrorHandler(errorConsumer);
            final RuntimeException e = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();
            Burst.items(1).error(e2)
            .scan(0, throwingBiFunction(e))
            .test()
            .assertValues(0)
            .assertError(e);

            assertEquals("" + list, 1, list.size());
            assertTrue("" + list, list.get(0) instanceof UndeliverableException);
            assertEquals(e2, list.get(0).getCause());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void scanWithSeedDoesNotEmitTerminalEventTwiceIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create()
        .scan(0, throwingBiFunction(e))
        .test()
        .assertValue(0)
        .assertError(e);
    }

    @Test
    public void scanWithSeedDoesNotProcessOnNextAfterTerminalEventIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        final AtomicInteger count = new AtomicInteger();
        Burst.items(1, 2).create().scan(0, (_, _) -> {
            count.incrementAndGet();
            throw e;
        })
        .test()
        .assertValues(0)
        .assertError(e);
        assertEquals(1, count.get());
    }

    @Test
    public void scanWithSeedCompletesNormally() {
        Flowable.just(1, 2, 3).scan(0, SUM)
        .test()
        .assertValues(0, 1, 3, 6)
        .assertComplete();
    }

    @Test
    public void scanWithSeedWhenScanSeedProviderThrows() {
        final RuntimeException e = new RuntimeException();
        Flowable.just(1, 2, 3).scanWith(throwingSupplier(e),
            SUM)
        .test()
        .assertError(e)
        .assertNoValues();
    }

    @Test
    public void scanNoSeed() {
        Flowable.just(1, 2, 3)
        .scan(SUM)
        .test()
        .assertValues(1, 3, 6)
        .assertComplete();
    }

    @Test
    public void scanNoSeedDoesNotEmitErrorTwiceIfScanFunctionThrows() {
        final List<Throwable> list = new CopyOnWriteArrayList<>();
        Consumer<Throwable> errorConsumer = list::add;
        try {
            RxJavaPlugins.setErrorHandler(errorConsumer);
            final RuntimeException e = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();
            Burst.items(1, 2).error(e2)
            .scan(throwingBiFunction(e))
            .test()
            .assertValue(1)
            .assertError(e);

            assertEquals("" + list, 1, list.size());
            assertTrue("" + list, list.get(0) instanceof UndeliverableException);
            assertEquals(e2, list.get(0).getCause());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void scanNoSeedDoesNotEmitTerminalEventTwiceIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        Burst.items(1, 2).create()
        .scan(throwingBiFunction(e))
        .test()
        .assertValue(1)
        .assertError(e);
    }

    @Test
    public void scanNoSeedDoesNotProcessOnNextAfterTerminalEventIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        final AtomicInteger count = new AtomicInteger();
        Burst.items(1, 2, 3).create().scan((_, _) -> {
            count.incrementAndGet();
            throw e;
        })
        .test()
        .assertValue(1)
        .assertError(e);
        assertEquals(1, count.get());
    }

    private static BiFunction<Integer, Integer, Integer> throwingBiFunction(final RuntimeException e) {
        return (_, _) -> {
            throw e;
        };
    }

    private static final BiFunction<Integer, Integer, Integer> SUM = Integer::sum;

    private static Supplier<Integer> throwingSupplier(final RuntimeException e) {
        return () -> {
            throw e;
        };
    }

    @Test
    public void scanEmptyBackpressured() {
        Flowable.<Integer>empty()
        .scan(0, SUM)
        .test(1)
        .assertResult(0);
    }

    @Test
    public void scanErrorBackpressured() {
        Flowable.<Integer>error(new TestException())
        .scan(0, SUM)
        .test(0)
        .assertFailure(TestException.class);
    }

    @Test
    public void scanTake() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() /* NFI */ {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                onComplete();
                cancel();
            }
        };

        Flowable.range(1, 10)
        .scan(0, SUM)
        .subscribe(ts)
        ;

        ts.assertResult(0);
    }

    @Test
    public void scanLong() {
        int n = 2 * Flowable.bufferSize();

        for (int b = 1; b <= n; b *= 2) {
            List<Integer> list = Flowable.range(1, n)
            .scan(0, (_, b1) -> b1)
            .rebatchRequests(b)
            .toList()
            .blockingGet();

            for (int i = 0; i <= n; i++) {
                assertEquals(i, list.get(i).intValue());
            }
        }
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.<Integer>never().scanWith(() -> 1, Integer::sum));
    }

    @Test
    public void drainMoreWork() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.scanWith(() -> 0, Integer::sum)
        .doOnNext(v -> {
            if (v == 1) {
                pp.onNext(2);
                pp.onComplete();
            }
        })
        .test();

        pp.onNext(1);

        ts.assertResult(0, 1, 3);
    }
}
