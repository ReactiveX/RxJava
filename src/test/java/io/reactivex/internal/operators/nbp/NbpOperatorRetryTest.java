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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.Test;
import org.mockito.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.observables.nbp.NbpGroupedObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorRetryTest {

    @Test
    public void iterativeBackoff() {
        NbpSubscriber<String> consumer = TestHelper.mockNbpSubscriber();
        
        NbpObservable<String> producer = NbpObservable.create(new NbpOnSubscribe<String>() {

            private AtomicInteger count = new AtomicInteger(4);
            long last = System.currentTimeMillis();

            @Override
            public void accept(NbpSubscriber<? super String> t1) {
                t1.onSubscribe(EmptyDisposable.INSTANCE);
                System.out.println(count.get() + " @ " + String.valueOf(last - System.currentTimeMillis()));
                last = System.currentTimeMillis();
                if (count.getAndDecrement() == 0) {
                    t1.onNext("hello");
                    t1.onComplete();
                }
                else 
                    t1.onError(new RuntimeException());
            }
            
        });
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>(consumer);
        producer.retryWhen(new Function<NbpObservable<? extends Throwable>, NbpObservable<?>>() {

            @Override
            public NbpObservable<?> apply(NbpObservable<? extends Throwable> attempts) {
                // Worker w = Schedulers.computation().createWorker();
                return attempts
                    .map(new Function<Throwable, Tuple>() {
                        @Override
                        public Tuple apply(Throwable n) {
                            return new Tuple(new Long(1), n);
                        }})
                    .scan(new BiFunction<Tuple, Tuple, Tuple>(){
                        @Override
                        public Tuple apply(Tuple t, Tuple n) {
                            return new Tuple(t.count + n.count, n.n);
                        }})
                    .flatMap(new Function<Tuple, NbpObservable<Long>>() {
                        @Override
                        public NbpObservable<Long> apply(Tuple t) {
                            System.out.println("Retry # "+t.count);
                            return t.count > 20 ? 
                                NbpObservable.<Long>error(t.n) :
                                NbpObservable.timer(t.count *1L, TimeUnit.MILLISECONDS);
                    }});
            }
        }).subscribe(ts);
        ts.awaitTerminalEvent();
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
    public void testRetryIndefinitely() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        int NUM_RETRIES = 20;
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(NUM_RETRIES));
        origin.retry().unsafeSubscribe(new NbpTestSubscriber<>(NbpObserver));

        InOrder inOrder = inOrder(NbpObserver);
        // should show 3 attempts
        inOrder.verify(NbpObserver, times(NUM_RETRIES + 1)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(NbpObserver, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSchedulingNotificationHandler() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        int NUM_RETRIES = 2;
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(NUM_RETRIES));
        NbpTestSubscriber<String> NbpSubscriber = new NbpTestSubscriber<>(NbpObserver);
        origin.retryWhen(new Function<NbpObservable<? extends Throwable>, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(NbpObservable<? extends Throwable> t1) {
                return t1.observeOn(Schedulers.computation()).map(new Function<Throwable, Integer>() {
                    @Override
                    public Integer apply(Throwable t1) {
                        return 1;
                    }
                }).startWith(1);
            }
        })
        .doOnError(Throwable::printStackTrace)
        .subscribe(NbpSubscriber);

        NbpSubscriber.awaitTerminalEvent();
        InOrder inOrder = inOrder(NbpObserver);
        // should show 3 attempts
        inOrder.verify(NbpObserver, times(1 + NUM_RETRIES)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(NbpObserver, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testOnNextFromNotificationHandler() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        int NUM_RETRIES = 2;
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(NUM_RETRIES));
        origin.retryWhen(new Function<NbpObservable<? extends Throwable>, NbpObservable<?>>() {
            @Override
            public NbpObservable<?> apply(NbpObservable<? extends Throwable> t1) {
                return t1.map(new Function<Throwable, Integer>() {

                    @Override
                    public Integer apply(Throwable t1) {
                        return 0;
                    }
                }).startWith(0);
            }
        }).subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        // should show 3 attempts
        inOrder.verify(NbpObserver, times(NUM_RETRIES + 1)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(NbpObserver, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testOnCompletedFromNotificationHandler() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(1));
        NbpTestSubscriber<String> NbpSubscriber = new NbpTestSubscriber<>(NbpObserver);
        origin.retryWhen(new Function<NbpObservable<? extends Throwable>, NbpObservable<?>>() {
            @Override
            public NbpObservable<?> apply(NbpObservable<? extends Throwable> t1) {
                return NbpObservable.empty();
            }
        }).subscribe(NbpSubscriber);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver).onSubscribe((Disposable)notNull());
        inOrder.verify(NbpObserver, never()).onNext("beginningEveryTime");
        inOrder.verify(NbpObserver, never()).onNext("onSuccessOnly");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verify(NbpObserver, never()).onError(any(Exception.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testOnErrorFromNotificationHandler() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(2));
        origin.retryWhen(new Function<NbpObservable<? extends Throwable>, NbpObservable<?>>() {
            @Override
            public NbpObservable<?> apply(NbpObservable<? extends Throwable> t1) {
                return NbpObservable.error(new RuntimeException());
            }
        }).subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver).onSubscribe((Disposable)notNull());
        inOrder.verify(NbpObserver, never()).onNext("beginningEveryTime");
        inOrder.verify(NbpObserver, never()).onNext("onSuccessOnly");
        inOrder.verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, times(1)).onError(any(IllegalStateException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleSubscriptionOnFirst() throws Exception {
        final AtomicInteger inc = new AtomicInteger(0);
        NbpOnSubscribe<Integer> onSubscribe = new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(NbpSubscriber<? super Integer> NbpSubscriber) {
                NbpSubscriber.onSubscribe(EmptyDisposable.INSTANCE);
                final int emit = inc.incrementAndGet();
                NbpSubscriber.onNext(emit);
                NbpSubscriber.onComplete();
            }
        };

        int first = NbpObservable.create(onSubscribe)
                .retryWhen(new Function<NbpObservable<? extends Throwable>, NbpObservable<?>>() {
                    @Override
                    public NbpObservable<?> apply(NbpObservable<? extends Throwable> attempt) {
                        return attempt.zipWith(NbpObservable.just(1), new BiFunction<Throwable, Integer, Void>() {
                            @Override
                            public Void apply(Throwable o, Integer integer) {
                                return null;
                            }
                        });
                    }
                })
                .toBlocking()
                .first();

        assertEquals("NbpObserver did not receive the expected output", 1, first);
        assertEquals("Subscribe was not called once", 1, inc.get());
    }

    @Test
    public void testOriginFails() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(1));
        origin.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("beginningEveryTime");
        inOrder.verify(NbpObserver, times(1)).onError(any(RuntimeException.class));
        inOrder.verify(NbpObserver, never()).onNext("onSuccessOnly");
        inOrder.verify(NbpObserver, never()).onComplete();
    }

    @Test
    public void testRetryFail() {
        int NUM_RETRIES = 1;
        int NUM_FAILURES = 2;
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(NUM_FAILURES));
        origin.retry(NUM_RETRIES).subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        // should show 2 attempts (first time fail, second time (1st retry) fail)
        inOrder.verify(NbpObserver, times(1 + NUM_RETRIES)).onNext("beginningEveryTime");
        // should only retry once, fail again and emit onError
        inOrder.verify(NbpObserver, times(1)).onError(any(RuntimeException.class));
        // no success
        inOrder.verify(NbpObserver, never()).onNext("onSuccessOnly");
        inOrder.verify(NbpObserver, never()).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRetrySuccess() {
        int NUM_FAILURES = 1;
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(NUM_FAILURES));
        origin.retry(3).subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        // should show 3 attempts
        inOrder.verify(NbpObserver, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(NbpObserver, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testInfiniteRetry() {
        int NUM_FAILURES = 20;
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(NUM_FAILURES));
        origin.retry().subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        // should show 3 attempts
        inOrder.verify(NbpObserver, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(NbpObserver, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Checks in a simple and synchronous way that retry resubscribes
     * after error. This test fails against 0.16.1-0.17.4, hangs on 0.17.5 and
     * passes in 0.17.6 thanks to fix for issue #1027.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testRetrySubscribesAgainAfterError() {

        // record emitted values with this action
        Consumer<Integer> record = mock(Consumer.class);
        InOrder inOrder = inOrder(record);

        // always throw an exception with this action
        Consumer<Integer> throwException = mock(Consumer.class);
        doThrow(new RuntimeException()).when(throwException).accept(Mockito.anyInt());

        // create a retrying NbpObservable based on a NbpPublishSubject
        NbpPublishSubject<Integer> subject = NbpPublishSubject.create();
        subject
        // record item
        .doOnNext(record)
        // throw a RuntimeException
                .doOnNext(throwException)
                // retry on error
                .retry()
                // subscribe and ignore
                .subscribe();

        inOrder.verifyNoMoreInteractions();

        subject.onNext(1);
        inOrder.verify(record).accept(1);

        subject.onNext(2);
        inOrder.verify(record).accept(2);

        subject.onNext(3);
        inOrder.verify(record).accept(3);

        inOrder.verifyNoMoreInteractions();
    }

    public static class FuncWithErrors implements NbpOnSubscribe<String> {

        private final int numFailures;
        private final AtomicInteger count = new AtomicInteger(0);

        FuncWithErrors(int count) {
            this.numFailures = count;
        }

        @Override
        public void accept(final NbpSubscriber<? super String> o) {
            o.onSubscribe(EmptyDisposable.INSTANCE);
            o.onNext("beginningEveryTime");
            int i = count.getAndIncrement();
            if (i < numFailures) {
                o.onError(new RuntimeException("forced failure: " + (i + 1)));
            } else {
                o.onNext("onSuccessOnly");
                o.onComplete();
            }
        }
    }

    @Test
    public void testUnsubscribeFromRetry() {
        NbpPublishSubject<Integer> subject = NbpPublishSubject.create();
        final AtomicInteger count = new AtomicInteger(0);
        Disposable sub = subject.retry().subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer n) {
                count.incrementAndGet();
            }
        });
        subject.onNext(1);
        sub.dispose();
        subject.onNext(2);
        assertEquals(1, count.get());
    }

    @Test
    public void testRetryAllowsSubscriptionAfterAllSubscriptionsUnsubscribed() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);
        NbpOnSubscribe<String> onSubscribe = new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> s) {
                subsCount.incrementAndGet();
                s.onSubscribe(() -> {
                        subsCount.decrementAndGet();
                });
                
            }
        };
        NbpObservable<String> stream = NbpObservable.create(onSubscribe);
        NbpObservable<String> streamWithRetry = stream.retry();
        Disposable sub = streamWithRetry.subscribe();
        assertEquals(1, subsCount.get());
        sub.dispose();
        assertEquals(0, subsCount.get());
        streamWithRetry.subscribe();
        assertEquals(1, subsCount.get());
    }

    @Test
    public void testSourceObservableCallsUnsubscribe() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);

        final NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();

        NbpOnSubscribe<String> onSubscribe = new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> s) {
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

        NbpObservable.create(onSubscribe).retry(3).subscribe(ts);
        assertEquals(4, subsCount.get()); // 1 + 3 retries
    }

    @Test
    public void testSourceObservableRetry1() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);

        final NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();

        NbpOnSubscribe<String> onSubscribe = new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                subsCount.incrementAndGet();
                s.onError(new RuntimeException("failed"));
            }
        };

        NbpObservable.create(onSubscribe).retry(1).subscribe(ts);
        assertEquals(2, subsCount.get());
    }

    @Test
    public void testSourceObservableRetry0() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);

        final NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();

        NbpOnSubscribe<String> onSubscribe = new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                subsCount.incrementAndGet();
                s.onError(new RuntimeException("failed"));
            }
        };

        NbpObservable.create(onSubscribe).retry(0).subscribe(ts);
        assertEquals(1, subsCount.get());
    }

    static final class SlowObservable implements NbpOnSubscribe<Long> {

        final AtomicInteger efforts = new AtomicInteger(0);
        final AtomicInteger active = new AtomicInteger(0), maxActive = new AtomicInteger(0);
        final AtomicInteger nextBeforeFailure;

        private final int emitDelay;

        public SlowObservable(int emitDelay, int countNext) {
            this.emitDelay = emitDelay;
            this.nextBeforeFailure = new AtomicInteger(countNext);
        }

        @Override
        public void accept(final NbpSubscriber<? super Long> NbpSubscriber) {
            final AtomicBoolean terminate = new AtomicBoolean(false);
            NbpSubscriber.onSubscribe(() -> {
                    terminate.set(true);
                    active.decrementAndGet();
            });
            efforts.getAndIncrement();
            active.getAndIncrement();
            maxActive.set(Math.max(active.get(), maxActive.get()));
            final Thread thread = new Thread() {
                @Override
                public void run() {
                    long nr = 0;
                    try {
                        while (!terminate.get()) {
                            Thread.sleep(emitDelay);
                            if (nextBeforeFailure.getAndDecrement() > 0) {
                                NbpSubscriber.onNext(nr++);
                            } else {
                                NbpSubscriber.onError(new RuntimeException("expected-failed"));
                            }
                        }
                    } catch (InterruptedException t) {
                    }
                }
            };
            thread.start();
        }
    }

    /** NbpObserver for listener on seperate thread */
    static final class AsyncObserver<T> extends NbpObserver<T> {

        protected CountDownLatch latch = new CountDownLatch(1);

        protected NbpSubscriber<T> target;

        /** Wrap existing NbpObserver */
        public AsyncObserver(NbpSubscriber<T> target) {
            this.target = target;
        }

        /** Wait */
        public void await() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }
        }

        // NbpObserver implementation

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

    @Test(timeout = 10000)
    public void testUnsubscribeAfterError() {

        NbpSubscriber<Long> NbpObserver = TestHelper.mockNbpSubscriber();

        // NbpObservable that always fails after 100ms
        SlowObservable so = new SlowObservable(100, 0);
        NbpObservable<Long> o = NbpObservable.create(so).retry(5);

        AsyncObserver<Long> async = new AsyncObserver<>(NbpObserver);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(NbpObserver);
        // Should fail once
        inOrder.verify(NbpObserver, times(1)).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
        assertEquals("Only 1 active subscription", 1, so.maxActive.get());
    }

    @Test(timeout = 10000)
    public void testTimeoutWithRetry() {

        @SuppressWarnings("unchecked")
        NbpObserver<Long> NbpObserver = mock(NbpObserver.class);

        // NbpObservable that sends every 100ms (timeout fails instead)
        SlowObservable so = new SlowObservable(100, 10);
        NbpObservable<Long> o = NbpObservable.create(so).timeout(80, TimeUnit.MILLISECONDS).retry(5);

        AsyncObserver<Long> async = new AsyncObserver<>(NbpObserver);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(NbpObserver);
        // Should fail once
        inOrder.verify(NbpObserver, times(1)).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
    }
    
    @Test//(timeout = 15000)
    public void testRetryWithBackpressure() throws InterruptedException {
        final int NUM_LOOPS = 1;
        for (int j=0;j<NUM_LOOPS;j++) {
            final int NUM_RETRIES = Observable.bufferSize() * 2;
            for (int i = 0; i < 400; i++) {
                NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
                NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(NUM_RETRIES));
                NbpTestSubscriber<String> ts = new NbpTestSubscriber<>(NbpObserver);
                origin.retry().observeOn(Schedulers.computation()).unsafeSubscribe(ts);
                ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
                
                InOrder inOrder = inOrder(NbpObserver);
                // should have no errors
                verify(NbpObserver, never()).onError(any(Throwable.class));
                // should show NUM_RETRIES attempts
                inOrder.verify(NbpObserver, times(NUM_RETRIES + 1)).onNext("beginningEveryTime");
                // should have a single success
                inOrder.verify(NbpObserver, times(1)).onNext("onSuccessOnly");
                // should have a single successful onCompleted
                inOrder.verify(NbpObserver, times(1)).onComplete();
                inOrder.verifyNoMoreInteractions();
            }
        }
    }
    
    @Test//(timeout = 15000)
    public void testRetryWithBackpressureParallel() throws InterruptedException {
        final int NUM_LOOPS = 1;
        final int NUM_RETRIES = Observable.bufferSize() * 2;
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
                                NbpObservable<String> origin = NbpObservable.create(new FuncWithErrors(NUM_RETRIES));
                                NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
                                origin.retry()
                                .observeOn(Schedulers.computation()).unsafeSubscribe(ts);
                                ts.awaitTerminalEvent(2500, TimeUnit.MILLISECONDS);
                                List<String> onNextEvents = new ArrayList<>(ts.values());
                                if (onNextEvents.size() != NUM_RETRIES + 2) {
                                    for (Throwable t : ts.errors()) {
                                        onNextEvents.add(t.toString());
                                    }
                                    for (long err = ts.completions(); err != 0; err--) {
                                        onNextEvents.add("onCompleted");
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
    @Test//(timeout = 3000)
    public void testIssue1900() throws InterruptedException {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        final int NUM_MSG = 1034;
        final AtomicInteger count = new AtomicInteger();

        NbpObservable<String> origin = NbpObservable.range(0, NUM_MSG)
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
        .flatMap(new Function<NbpGroupedObservable<String,String>, NbpObservable<String>>() {
            @Override
            public NbpObservable<String> apply(NbpGroupedObservable<String, String> t1) {
                return t1.take(1);
            }
        })
        .unsafeSubscribe(new NbpTestSubscriber<>(NbpObserver));
        
        InOrder inOrder = inOrder(NbpObserver);
        // should show 3 attempts
        inOrder.verify(NbpObserver, times(NUM_MSG)).onNext(any(java.lang.String.class));
        //        // should have no errors
        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        // should have a single success
        //inOrder.verify(NbpObserver, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
    @Test//(timeout = 3000)
    public void testIssue1900SourceNotSupportingBackpressure() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        final int NUM_MSG = 1034;
        final AtomicInteger count = new AtomicInteger();

        NbpObservable<String> origin = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(NbpSubscriber<? super String> o) {
                o.onSubscribe(EmptyDisposable.INSTANCE);
                for(int i=0; i<NUM_MSG; i++) {
                    o.onNext("msg:" + count.incrementAndGet());
                }   
                o.onComplete();
            }
        });
        
        origin.retry()
        .groupBy(new Function<String, String>() {
            @Override
            public String apply(String t1) {
                return t1;
            }
        })
        .flatMap(new Function<NbpGroupedObservable<String,String>, NbpObservable<String>>() {
            @Override
            public NbpObservable<String> apply(NbpGroupedObservable<String, String> t1) {
                return t1.take(1);
            }
        })
        .unsafeSubscribe(new NbpTestSubscriber<>(NbpObserver));
        
        InOrder inOrder = inOrder(NbpObserver);
        // should show 3 attempts
        inOrder.verify(NbpObserver, times(NUM_MSG)).onNext(any(java.lang.String.class));
        //        // should have no errors
        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        // should have a single success
        //inOrder.verify(NbpObserver, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

}