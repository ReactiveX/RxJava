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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.flowables.GroupedFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableRetryTest extends RxJavaTest {

    @Test
    public void iterativeBackoff() {
        Subscriber<String> consumer = TestHelper.mockSubscriber();

        Flowable<String> producer = Flowable.unsafeCreate(new Publisher<String>() {

            private AtomicInteger count = new AtomicInteger(4);
            long last = System.currentTimeMillis();

            @Override
            public void subscribe(Subscriber<? super String> t1) {
                t1.onSubscribe(new BooleanSubscription());
                System.out.println(count.get() + " @ " + String.valueOf(last - System.currentTimeMillis()));
                last = System.currentTimeMillis();
                if (count.getAndDecrement() == 0) {
                    t1.onNext("hello");
                    t1.onComplete();
                } else {
                    t1.onError(new RuntimeException());
                }
            }

        });
        TestSubscriber<String> ts = new TestSubscriber<>(consumer);
        producer.retryWhen(new Function<Flowable<? extends Throwable>, Flowable<Object>>() {

            @Override
            public Flowable<Object> apply(Flowable<? extends Throwable> attempts) {
                // Worker w = Schedulers.computation().createWorker();
                return attempts
                    .map(new Function<Throwable, Tuple>() {
                        @Override
                        public Tuple apply(Throwable n) {
                            return new Tuple(new Long(1), n);
                        }})
                    .scan(new BiFunction<Tuple, Tuple, Tuple>() {
                        @Override
                        public Tuple apply(Tuple t, Tuple n) {
                            return new Tuple(t.count + n.count, n.n);
                        }})
                    .flatMap(new Function<Tuple, Flowable<Object>>() {
                        @Override
                        public Flowable<Object> apply(Tuple t) {
                            System.out.println("Retry # " + t.count);
                            return t.count > 20 ?
                                Flowable.<Object>error(t.n) :
                                Flowable.timer(t.count * 1L, TimeUnit.MILLISECONDS)
                                .cast(Object.class);
                    }});
            }
        }).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();

        InOrder inOrder = inOrder(consumer);
        inOrder.verify(consumer, never()).onError(any(Throwable.class));
        inOrder.verify(consumer, times(1)).onNext("hello");
        inOrder.verify(consumer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();

    }

    public static class Tuple {
        Long count;
        Throwable n;

        Tuple(Long c, Throwable n) {
            count = c;
            this.n = n;
        }
    }

    @Test
    public void retryIndefinitely() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        int numRetries = 20;
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(numRetries));
        origin.retry().subscribe(new TestSubscriber<>(subscriber));

        InOrder inOrder = inOrder(subscriber);
        // should show 3 attempts
        inOrder.verify(subscriber, times(numRetries + 1)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(subscriber, times(1)).onNext("onSuccessOnly");
        // should have a single successful onComplete
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void schedulingNotificationHandler() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        int numRetries = 2;
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(numRetries));
        TestSubscriber<String> ts = new TestSubscriber<>(subscriber);
        origin.retryWhen(new Function<Flowable<? extends Throwable>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<? extends Throwable> t1) {
                return t1.observeOn(Schedulers.computation()).map(new Function<Throwable, Integer>() {
                    @Override
                    public Integer apply(Throwable t1) {
                        return 1;
                    }
                }).startWithItem(1).cast(Object.class);
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                e.printStackTrace();
            }
        })
        .subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
        InOrder inOrder = inOrder(subscriber);
        // should show 3 attempts
        inOrder.verify(subscriber, times(1 + numRetries)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(subscriber, times(1)).onNext("onSuccessOnly");
        // should have a single successful onComplete
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void onNextFromNotificationHandler() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        int numRetries = 2;
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(numRetries));
        origin.retryWhen(new Function<Flowable<? extends Throwable>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<? extends Throwable> t1) {
                return t1.map(new Function<Throwable, Integer>() {

                    @Override
                    public Integer apply(Throwable t1) {
                        return 0;
                    }
                }).startWithItem(0).cast(Object.class);
            }
        }).subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        // should show 3 attempts
        inOrder.verify(subscriber, times(numRetries + 1)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(subscriber, times(1)).onNext("onSuccessOnly");
        // should have a single successful onComplete
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void onCompletedFromNotificationHandler() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(1));
        TestSubscriber<String> ts = new TestSubscriber<>(subscriber);
        origin.retryWhen(new Function<Flowable<? extends Throwable>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<? extends Throwable> t1) {
                return Flowable.empty();
            }
        }).subscribe(ts);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onSubscribe((Subscription)notNull());
        inOrder.verify(subscriber, never()).onNext("beginningEveryTime");
        inOrder.verify(subscriber, never()).onNext("onSuccessOnly");
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verify(subscriber, never()).onError(any(Exception.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void onErrorFromNotificationHandler() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(2));
        origin.retryWhen(new Function<Flowable<? extends Throwable>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<? extends Throwable> t1) {
                return Flowable.error(new RuntimeException());
            }
        }).subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onSubscribe((Subscription)notNull());
        inOrder.verify(subscriber, never()).onNext("beginningEveryTime");
        inOrder.verify(subscriber, never()).onNext("onSuccessOnly");
        inOrder.verify(subscriber, never()).onComplete();
        inOrder.verify(subscriber, times(1)).onError(any(RuntimeException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleSubscriptionOnFirst() throws Exception {
        final AtomicInteger inc = new AtomicInteger(0);
        Publisher<Integer> onSubscribe = new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                final int emit = inc.incrementAndGet();
                subscriber.onNext(emit);
                subscriber.onComplete();
            }
        };

        int first = Flowable.unsafeCreate(onSubscribe)
                .retryWhen(new Function<Flowable<? extends Throwable>, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> apply(Flowable<? extends Throwable> attempt) {
                        return attempt.zipWith(Flowable.just(1), new BiFunction<Throwable, Integer, Object>() {
                            @Override
                            public Object apply(Throwable o, Integer integer) {
                                return 0;
                            }
                        });
                    }
                })
                .blockingFirst();

        assertEquals("Observer did not receive the expected output", 1, first);
        assertEquals("Subscribe was not called once", 1, inc.get());
    }

    @Test
    public void originFails() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(1));
        origin.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext("beginningEveryTime");
        inOrder.verify(subscriber, times(1)).onError(any(RuntimeException.class));
        inOrder.verify(subscriber, never()).onNext("onSuccessOnly");
        inOrder.verify(subscriber, never()).onComplete();
    }

    @Test
    public void retryFail() {
        int numRetries = 1;
        int numFailures = 2;
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(numFailures));
        origin.retry(numRetries).subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        // should show 2 attempts (first time fail, second time (1st retry) fail)
        inOrder.verify(subscriber, times(1 + numRetries)).onNext("beginningEveryTime");
        // should only retry once, fail again and emit onError
        inOrder.verify(subscriber, times(1)).onError(any(RuntimeException.class));
        // no success
        inOrder.verify(subscriber, never()).onNext("onSuccessOnly");
        inOrder.verify(subscriber, never()).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void retrySuccess() {
        int numFailures = 1;
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(numFailures));
        origin.retry(3).subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        // should show 3 attempts
        inOrder.verify(subscriber, times(1 + numFailures)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(subscriber, times(1)).onNext("onSuccessOnly");
        // should have a single successful onComplete
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void infiniteRetry() {
        int numFailures = 20;
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(numFailures));
        origin.retry().subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        // should show 3 attempts
        inOrder.verify(subscriber, times(1 + numFailures)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(subscriber, times(1)).onNext("onSuccessOnly");
        // should have a single successful onComplete
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    /*
     * Checks in a simple and synchronous way that retry resubscribes
     * after error. This test fails against 0.16.1-0.17.4, hangs on 0.17.5 and
     * passes in 0.17.6 thanks to fix for issue #1027.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void retrySubscribesAgainAfterError() throws Throwable {

        // record emitted values with this action
        Consumer<Integer> record = mock(Consumer.class);
        InOrder inOrder = inOrder(record);

        // always throw an exception with this action
        Consumer<Integer> throwException = mock(Consumer.class);
        doThrow(new RuntimeException()).when(throwException).accept(Mockito.anyInt());

        // create a retrying Flowable based on a PublishProcessor
        PublishProcessor<Integer> processor = PublishProcessor.create();
        processor
        // record item
        .doOnNext(record)
        // throw a RuntimeException
                .doOnNext(throwException)
                // retry on error
                .retry()
                // subscribe and ignore
                .subscribe();

        inOrder.verifyNoMoreInteractions();

        processor.onNext(1);
        inOrder.verify(record).accept(1);

        processor.onNext(2);
        inOrder.verify(record).accept(2);

        processor.onNext(3);
        inOrder.verify(record).accept(3);

        inOrder.verifyNoMoreInteractions();
    }

    public static class FuncWithErrors implements Publisher<String> {

        private final int numFailures;
        private final AtomicInteger count = new AtomicInteger(0);

        FuncWithErrors(int count) {
            this.numFailures = count;
        }

        @Override
        public void subscribe(final Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                final AtomicLong req = new AtomicLong();
                // 0 = not set, 1 = fast path, 2 = backpressure
                final AtomicInteger path = new AtomicInteger(0);
                volatile boolean done;

                @Override
                public void request(long n) {
                    if (n == Long.MAX_VALUE && path.compareAndSet(0, 1)) {
                        subscriber.onNext("beginningEveryTime");
                        int i = count.getAndIncrement();
                        if (i < numFailures) {
                            subscriber.onError(new RuntimeException("forced failure: " + (i + 1)));
                        } else {
                            subscriber.onNext("onSuccessOnly");
                            subscriber.onComplete();
                        }
                        return;
                    }
                    if (n > 0 && req.getAndAdd(n) == 0 && (path.get() == 2 || path.compareAndSet(0, 2)) && !done) {
                        int i = count.getAndIncrement();
                        if (i < numFailures) {
                            subscriber.onNext("beginningEveryTime");
                            subscriber.onError(new RuntimeException("forced failure: " + (i + 1)));
                            done = true;
                        } else {
                            do {
                                if (i == numFailures) {
                                    subscriber.onNext("beginningEveryTime");
                                } else
                                if (i > numFailures) {
                                    subscriber.onNext("onSuccessOnly");
                                    subscriber.onComplete();
                                    done = true;
                                    break;
                                }
                                i = count.getAndIncrement();
                            } while (req.decrementAndGet() > 0);
                        }
                    }
                }

                @Override
                public void cancel() {
                    // TODO Auto-generated method stub

                }
            });
        }
    }

    @Test
    public void unsubscribeFromRetry() {
        PublishProcessor<Integer> processor = PublishProcessor.create();
        final AtomicInteger count = new AtomicInteger(0);
        Disposable sub = processor.retry().subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer n) {
                count.incrementAndGet();
            }
        });
        processor.onNext(1);
        sub.dispose();
        processor.onNext(2);
        assertEquals(1, count.get());
    }

    @Test
    public void retryAllowsSubscriptionAfterAllSubscriptionsUnsubscribed() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);
        Publisher<String> onSubscribe = new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> s) {
                subsCount.incrementAndGet();
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {

                    }

                    @Override
                    public void cancel() {
                        subsCount.decrementAndGet();
                    }
                });

            }
        };
        Flowable<String> stream = Flowable.unsafeCreate(onSubscribe);
        Flowable<String> streamWithRetry = stream.retry();
        Disposable sub = streamWithRetry.subscribe();
        assertEquals(1, subsCount.get());
        sub.dispose();
        assertEquals(0, subsCount.get());
        streamWithRetry.subscribe();
        assertEquals(1, subsCount.get());
    }

    @Test
    public void sourceFlowableCallsUnsubscribe() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);

        final TestSubscriber<String> ts = new TestSubscriber<>();

        Publisher<String> onSubscribe = new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> s) {
                BooleanSubscription bs = new BooleanSubscription();
                // if isUnsubscribed is true that means we have a bug such as
                // https://github.com/ReactiveX/RxJava/issues/1024
                if (!bs.isCancelled()) {
                    subsCount.incrementAndGet();
                    s.onError(new RuntimeException("failed"));
                    // it unsubscribes the child directly
                    // this simulates various error/completion scenarios that could occur
                    // or just a source that proactively triggers cleanup
                    // FIXME can't unsubscribe child
//                    s.unsubscribe();
                    bs.cancel();
                } else {
                    s.onError(new RuntimeException());
                }
            }
        };

        Flowable.unsafeCreate(onSubscribe).retry(3).subscribe(ts);
        assertEquals(4, subsCount.get()); // 1 + 3 retries
    }

    @Test
    public void sourceFlowableRetry1() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);

        final TestSubscriber<String> ts = new TestSubscriber<>();

        Publisher<String> onSubscribe = new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> s) {
                s.onSubscribe(new BooleanSubscription());
                subsCount.incrementAndGet();
                s.onError(new RuntimeException("failed"));
            }
        };

        Flowable.unsafeCreate(onSubscribe).retry(1).subscribe(ts);
        assertEquals(2, subsCount.get());
    }

    @Test
    public void sourceFlowableRetry0() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);

        final TestSubscriber<String> ts = new TestSubscriber<>();

        Publisher<String> onSubscribe = new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> s) {
                s.onSubscribe(new BooleanSubscription());
                subsCount.incrementAndGet();
                s.onError(new RuntimeException("failed"));
            }
        };

        Flowable.unsafeCreate(onSubscribe).retry(0).subscribe(ts);
        assertEquals(1, subsCount.get());
    }

    static final class SlowFlowable implements Publisher<Long> {

        final AtomicInteger efforts = new AtomicInteger(0);
        final AtomicInteger active = new AtomicInteger(0);
        final AtomicInteger maxActive = new AtomicInteger(0);
        final AtomicInteger nextBeforeFailure;
        final String context;

        private final int emitDelay;

        SlowFlowable(int emitDelay, int countNext, String context) {
            this.emitDelay = emitDelay;
            this.nextBeforeFailure = new AtomicInteger(countNext);
            this.context = context;
        }

        @Override
        public void subscribe(final Subscriber<? super Long> subscriber) {
            final AtomicBoolean terminate = new AtomicBoolean(false);
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void cancel() {
                    terminate.set(true);
                    active.decrementAndGet();
                }
            });
            efforts.getAndIncrement();
            active.getAndIncrement();
            maxActive.set(Math.max(active.get(), maxActive.get()));
            final Thread thread = new Thread(context) {
                @Override
                public void run() {
                    long nr = 0;
                    try {
                        while (!terminate.get()) {
                            Thread.sleep(emitDelay);
                            if (nextBeforeFailure.getAndDecrement() > 0) {
                                subscriber.onNext(nr++);
                            } else {
                                active.decrementAndGet();
                                subscriber.onError(new RuntimeException("expected-failed"));
                                break;
                            }
                        }
                    } catch (InterruptedException t) {
                    }
                }
            };
            thread.start();
        }
    }

    /** Observer for listener on seperate thread. */
    static final class AsyncSubscriber<T> extends DefaultSubscriber<T> {

        protected CountDownLatch latch = new CountDownLatch(1);

        protected Subscriber<T> target;

        /**
         * Wrap existing Observer.
         * @param target the target subscriber
         */
        AsyncSubscriber(Subscriber<T> target) {
            this.target = target;
        }

        /** Wait. */
        public void await() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }
        }

        // Observer implementation

        @Override
        public void onComplete() {
            target.onComplete();
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            target.onError(t);
            latch.countDown();
        }

        @Override
        public void onNext(T v) {
            target.onNext(v);
        }
    }

    @Test
    public void unsubscribeAfterError() {

        Subscriber<Long> subscriber = TestHelper.mockSubscriber();

        // Flowable that always fails after 100ms
        SlowFlowable so = new SlowFlowable(100, 0, "testUnsubscribeAfterError");
        Flowable<Long> f = Flowable.unsafeCreate(so).retry(5);

        AsyncSubscriber<Long> async = new AsyncSubscriber<>(subscriber);

        f.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(subscriber);
        // Should fail once
        inOrder.verify(subscriber, times(1)).onError(any(Throwable.class));
        inOrder.verify(subscriber, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
        assertEquals("Only 1 active subscription", 1, so.maxActive.get());
    }

    @Test
    public void timeoutWithRetry() {

        Subscriber<Long> subscriber = TestHelper.mockSubscriber();

        // Flowable that sends every 100ms (timeout fails instead)
        SlowFlowable sf = new SlowFlowable(100, 10, "testTimeoutWithRetry");
        Flowable<Long> f = Flowable.unsafeCreate(sf).timeout(80, TimeUnit.MILLISECONDS).retry(5);

        AsyncSubscriber<Long> async = new AsyncSubscriber<>(subscriber);

        f.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(subscriber);
        // Should fail once
        inOrder.verify(subscriber, times(1)).onError(any(Throwable.class));
        inOrder.verify(subscriber, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, sf.efforts.get());
    }

    @Test
    public void retryWithBackpressure() throws InterruptedException {
        final int NUM_LOOPS = 1;
        for (int j = 0; j < NUM_LOOPS; j++) {
            final int numRetries = Flowable.bufferSize() * 2;
            for (int i = 0; i < 400; i++) {
                Subscriber<String> subscriber = TestHelper.mockSubscriber();
                Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(numRetries));
                TestSubscriberEx<String> ts = new TestSubscriberEx<>(subscriber);
                origin.retry().observeOn(Schedulers.computation()).subscribe(ts);
                ts.awaitDone(5, TimeUnit.SECONDS);

                InOrder inOrder = inOrder(subscriber);
                // should have no errors
                verify(subscriber, never()).onError(any(Throwable.class));
                // should show numRetries attempts
                inOrder.verify(subscriber, times(numRetries + 1)).onNext("beginningEveryTime");
                // should have a single success
                inOrder.verify(subscriber, times(1)).onNext("onSuccessOnly");
                // should have a single successful onComplete
                inOrder.verify(subscriber, times(1)).onComplete();
                inOrder.verifyNoMoreInteractions();
            }
        }
    }

    @Test
    public void retryWithBackpressureParallel() throws InterruptedException {
        final int NUM_LOOPS = 1;
        final int numRetries = Flowable.bufferSize() * 2;
        int ncpu = Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(Math.max(ncpu / 2, 2));
        try {
            for (int r = 0; r < NUM_LOOPS; r++) {
                if (r % 10 == 0) {
                    System.out.println("testRetryWithBackpressureParallelLoop -> " + r);
                }

                final AtomicInteger timeouts = new AtomicInteger();
                final Map<Integer, List<String>> data = new ConcurrentHashMap<>();

                int m = 5000;
                final CountDownLatch cdl = new CountDownLatch(m);
                for (int i = 0; i < m; i++) {
                    final int j = i;
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            final AtomicInteger nexts = new AtomicInteger();
                            try {
                                Flowable<String> origin = Flowable.unsafeCreate(new FuncWithErrors(numRetries));
                                TestSubscriberEx<String> ts = new TestSubscriberEx<>();
                                origin.retry()
                                .observeOn(Schedulers.computation()).subscribe(ts);
                                ts.awaitDone(2500, TimeUnit.MILLISECONDS);
                                List<String> onNextEvents = new ArrayList<>(ts.values());
                                if (onNextEvents.size() != numRetries + 2) {
                                    for (Throwable t : ts.errors()) {
                                        onNextEvents.add(t.toString());
                                    }
                                    for (long err = ts.completions(); err != 0; err--) {
                                        onNextEvents.add("onComplete");
                                    }
                                    data.put(j, onNextEvents);
                                }
                            } catch (Throwable t) {
                                timeouts.incrementAndGet();
                                System.out.println(j + " | " + cdl.getCount() + " !!! " + nexts.get());
                            }
                            cdl.countDown();
                        }
                    });
                }
                cdl.await();
                assertEquals(0, timeouts.get());
                if (data.size() > 0) {
                    fail("Data content mismatch: " + allSequenceFrequency(data));
                }
            }
        } finally {
            exec.shutdown();
        }
    }
    static <T> StringBuilder allSequenceFrequency(Map<Integer, List<T>> its) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<Integer, List<T>> e : its.entrySet()) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(e.getKey()).append("={");
            b.append(sequenceFrequency(e.getValue()));
            b.append("}");
        }
        return b;
    }
    static <T> StringBuilder sequenceFrequency(Iterable<T> it) {
        StringBuilder sb = new StringBuilder();

        Object prev = null;
        int cnt = 0;

        for (Object curr : it) {
            if (sb.length() > 0) {
                if (!curr.equals(prev)) {
                    if (cnt > 1) {
                        sb.append(" x ").append(cnt);
                        cnt = 1;
                    }
                    sb.append(", ");
                    sb.append(curr);
                } else {
                    cnt++;
                }
            } else {
                sb.append(curr);
                cnt++;
            }
            prev = curr;
        }
        if (cnt > 1) {
            sb.append(" x ").append(cnt);
        }

        return sb;
    }

    @Test
    public void issue1900() throws InterruptedException {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        final int NUM_MSG = 1034;
        final AtomicInteger count = new AtomicInteger();

        Flowable<String> origin = Flowable.range(0, NUM_MSG)
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer t1) {
                        return "msg: " + count.incrementAndGet();
                    }
                });

        origin.retry()
        .groupBy(new Function<String, String>() {
            @Override
            public String apply(String t1) {
                return t1;
            }
        })
        .flatMap(new Function<GroupedFlowable<String, String>, Flowable<String>>() {
            @Override
            public Flowable<String> apply(GroupedFlowable<String, String> t1) {
                return t1.take(1);
            }
        }, NUM_MSG) // Must request as many groups as groupBy produces to avoid MBE
        .subscribe(new TestSubscriber<>(subscriber));

        InOrder inOrder = inOrder(subscriber);
        // should show 3 attempts
        inOrder.verify(subscriber, times(NUM_MSG)).onNext(any(java.lang.String.class));
        //        // should have no errors
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        // should have a single success
        //inOrder.verify(observer, times(1)).onNext("onSuccessOnly");
        // should have a single successful onComplete
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void issue1900SourceNotSupportingBackpressure() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        final int NUM_MSG = 1034;
        final AtomicInteger count = new AtomicInteger();

        Flowable<String> origin = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < NUM_MSG; i++) {
                    subscriber.onNext("msg:" + count.incrementAndGet());
                }
                subscriber.onComplete();
            }
        });

        origin.retry()
        .groupBy(new Function<String, String>() {
            @Override
            public String apply(String t1) {
                return t1;
            }
        })
        .flatMap(new Function<GroupedFlowable<String, String>, Flowable<String>>() {
            @Override
            public Flowable<String> apply(GroupedFlowable<String, String> t1) {
                return t1.take(1);
            }
        })
        .subscribe(new TestSubscriber<>(subscriber));

        InOrder inOrder = inOrder(subscriber);
        // should show 3 attempts
        inOrder.verify(subscriber, times(NUM_MSG)).onNext(any(java.lang.String.class));
        //        // should have no errors
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        // should have a single success
        //inOrder.verify(observer, times(1)).onNext("onSuccessOnly");
        // should have a single successful onComplete
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void retryWhenDefaultScheduler() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1)
        .concatWith(Flowable.<Integer>error(new TestException()))
        .retryWhen((Function)new Function<Flowable, Flowable>() {
            @Override
            public Flowable apply(Flowable f) {
                return f.take(2);
            }
        }).subscribe(ts);

        ts.assertValues(1, 1);
        ts.assertNoErrors();
        ts.assertComplete();

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void retryWhenTrampolineScheduler() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1)
        .concatWith(Flowable.<Integer>error(new TestException()))
        .subscribeOn(Schedulers.trampoline())
        .retryWhen((Function)new Function<Flowable, Flowable>() {
            @Override
            public Flowable apply(Flowable f) {
                return f.take(2);
            }
        }).subscribe(ts);

        ts.assertValues(1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void retryPredicate() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable v) throws Exception {
                return true;
            }
        })
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void retryLongPredicateInvalid() {
        try {
            Flowable.just(1).retry(-99, new Predicate<Throwable>() {
                @Override
                public boolean test(Throwable e) throws Exception {
                    return true;
                }
            });
            fail("Should have thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("times >= 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void retryUntil() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .retryUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        })
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void shouldDisposeInnerFlowable() {
      final PublishProcessor<Object> processor = PublishProcessor.create();
      final Disposable disposable = Flowable.error(new RuntimeException("Leak"))
          .retryWhen(new Function<Flowable<Throwable>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Throwable> errors) throws Exception {
                return errors.switchMap(new Function<Throwable, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> apply(Throwable ignore) throws Exception {
                        return processor;
                    }
                });
            }
        })
          .subscribe();

      assertTrue(processor.hasSubscribers());
      disposable.dispose();
      assertFalse(processor.hasSubscribers());
    }

    @Test
    public void noCancelPreviousRetry() {
        final AtomicInteger counter = new AtomicInteger();

        final AtomicInteger times = new AtomicInteger();

        Flowable<Integer> source = Flowable.defer(new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() throws Exception {
                if (times.getAndIncrement() < 4) {
                    return Flowable.error(new TestException());
                }
                return Flowable.just(1);
            }
        })
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        source.retry(5)
        .test()
        .assertResult(1);

        assertEquals(0, counter.get());
    }

    @Test
    public void noCancelPreviousRetryWhile() {
        final AtomicInteger counter = new AtomicInteger();

        final AtomicInteger times = new AtomicInteger();

        Flowable<Integer> source = Flowable.defer(new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() throws Exception {
                if (times.getAndIncrement() < 4) {
                    return Flowable.error(new TestException());
                }
                return Flowable.just(1);
            }
        })
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        source.retry(5, Functions.alwaysTrue())
        .test()
        .assertResult(1);

        assertEquals(0, counter.get());
    }

    @Test
    public void noCancelPreviousRetryWhile2() {
        final AtomicInteger counter = new AtomicInteger();

        final AtomicInteger times = new AtomicInteger();

        Flowable<Integer> source = Flowable.defer(new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() throws Exception {
                if (times.getAndIncrement() < 4) {
                    return Flowable.error(new TestException());
                }
                return Flowable.just(1);
            }
        })
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        source.retry(new BiPredicate<Integer, Throwable>() {
            @Override
            public boolean test(Integer a, Throwable b) throws Exception {
                return a < 5;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(0, counter.get());
    }

    @Test
    public void noCancelPreviousRetryUntil() {
        final AtomicInteger counter = new AtomicInteger();

        final AtomicInteger times = new AtomicInteger();

        Flowable<Integer> source = Flowable.defer(new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() throws Exception {
                if (times.getAndIncrement() < 4) {
                    return Flowable.error(new TestException());
                }
                return Flowable.just(1);
            }
        })
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        source.retryUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(0, counter.get());
    }

    @Test
    public void noCancelPreviousRepeatWhen() {
        final AtomicInteger counter = new AtomicInteger();

        final AtomicInteger times = new AtomicInteger();

        Flowable<Integer> source = Flowable.defer(new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() throws Exception {
                if (times.get() < 4) {
                    return Flowable.error(new TestException());
                }
                return Flowable.just(1);
            }
        }).doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        source.retryWhen(new Function<Flowable<Throwable>, Flowable<?>>() {
            @Override
            public Flowable<?> apply(Flowable<Throwable> e) throws Exception {
                return e.takeWhile(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return times.getAndIncrement() < 4;
                    }
                });
            }
        })
        .test()
        .assertResult(1);

        assertEquals(0, counter.get());
    }

    @Test
    public void noCancelPreviousRepeatWhen2() {
        final AtomicInteger counter = new AtomicInteger();

        final AtomicInteger times = new AtomicInteger();

        Flowable<Integer> source = Flowable.<Integer>error(new TestException())
                .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        source.retryWhen(new Function<Flowable<Throwable>, Flowable<?>>() {
            @Override
            public Flowable<?> apply(Flowable<Throwable> e) throws Exception {
                return e.takeWhile(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return times.getAndIncrement() < 4;
                    }
                });
            }
        })
        .test()
        .assertResult();

        assertEquals(0, counter.get());
    }

    @Test
    public void repeatFloodNoSubscriptionError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        final TestException error = new TestException();

        try {
            final PublishProcessor<Integer> source = PublishProcessor.create();
            final PublishProcessor<Integer> signaller = PublishProcessor.create();

            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

                TestSubscriber<Integer> ts = source.take(1)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        throw error;
                    }
                })
                .retryWhen(new Function<Flowable<Throwable>, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Flowable<Throwable> v)
                            throws Exception {
                        return signaller;
                    }
                }).test();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                            source.onNext(1);
                        }
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                            signaller.offer(1);
                        }
                    }
                };

                TestHelper.race(r1, r2);

                ts.cancel();
            }

            if (!errors.isEmpty()) {
                for (Throwable e : errors) {
                    e.printStackTrace();
                }
                fail(errors + "");
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
