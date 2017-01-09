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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.*;

public class FlowableRetryWithPredicateTest {
    BiPredicate<Integer, Throwable> retryTwice = new BiPredicate<Integer, Throwable>() {
        @Override
        public boolean test(Integer t1, Throwable t2) {
            return t1 <= 2;
        }
    };
    BiPredicate<Integer, Throwable> retry5 = new BiPredicate<Integer, Throwable>() {
        @Override
        public boolean test(Integer t1, Throwable t2) {
            return t1 <= 5;
        }
    };
    BiPredicate<Integer, Throwable> retryOnTestException = new BiPredicate<Integer, Throwable>() {
        @Override
        public boolean test(Integer t1, Throwable t2) {
            return t2 instanceof IOException;
        }
    };
    @Test
    public void testWithNothingToRetry() {
        Flowable<Integer> source = Flowable.range(0, 3);

        Subscriber<Integer> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.retry(retryTwice).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testRetryTwice() {
        Flowable<Integer> source = Flowable.unsafeCreate(new Publisher<Integer>() {
            int count;
            @Override
            public void subscribe(Subscriber<? super Integer> t1) {
                t1.onSubscribe(new BooleanSubscription());
                count++;
                t1.onNext(0);
                t1.onNext(1);
                if (count == 1) {
                    t1.onError(new TestException());
                    return;
                }
                t1.onNext(2);
                t1.onNext(3);
                t1.onComplete();
            }
        });

        @SuppressWarnings("unchecked")
        DefaultSubscriber<Integer> o = mock(DefaultSubscriber.class);
        InOrder inOrder = inOrder(o);

        source.retry(retryTwice).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

    }
    @Test
    public void testRetryTwiceAndGiveUp() {
        Flowable<Integer> source = Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> t1) {
                t1.onSubscribe(new BooleanSubscription());
                t1.onNext(0);
                t1.onNext(1);
                t1.onError(new TestException());
            }
        });

        @SuppressWarnings("unchecked")
        DefaultSubscriber<Integer> o = mock(DefaultSubscriber.class);
        InOrder inOrder = inOrder(o);

        source.retry(retryTwice).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();

    }
    @Test
    public void testRetryOnSpecificException() {
        Flowable<Integer> source = Flowable.unsafeCreate(new Publisher<Integer>() {
            int count;
            @Override
            public void subscribe(Subscriber<? super Integer> t1) {
                t1.onSubscribe(new BooleanSubscription());
                count++;
                t1.onNext(0);
                t1.onNext(1);
                if (count == 1) {
                    t1.onError(new IOException());
                    return;
                }
                t1.onNext(2);
                t1.onNext(3);
                t1.onComplete();
            }
        });

        @SuppressWarnings("unchecked")
        DefaultSubscriber<Integer> o = mock(DefaultSubscriber.class);
        InOrder inOrder = inOrder(o);

        source.retry(retryOnTestException).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testRetryOnSpecificExceptionAndNotOther() {
        final IOException ioe = new IOException();
        final TestException te = new TestException();
        Flowable<Integer> source = Flowable.unsafeCreate(new Publisher<Integer>() {
            int count;
            @Override
            public void subscribe(Subscriber<? super Integer> t1) {
                t1.onSubscribe(new BooleanSubscription());
                count++;
                t1.onNext(0);
                t1.onNext(1);
                if (count == 1) {
                    t1.onError(ioe);
                    return;
                }
                t1.onNext(2);
                t1.onNext(3);
                t1.onError(te);
            }
        });

        @SuppressWarnings("unchecked")
        DefaultSubscriber<Integer> o = mock(DefaultSubscriber.class);
        InOrder inOrder = inOrder(o);

        source.retry(retryOnTestException).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onError(te);
        verify(o, never()).onError(ioe);
        verify(o, never()).onComplete();
    }

    @Test
    public void testUnsubscribeFromRetry() {
        PublishProcessor<Integer> subject = PublishProcessor.create();
        final AtomicInteger count = new AtomicInteger(0);
        Disposable sub = subject.retry(retryTwice).subscribe(new Consumer<Integer>() {
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

    @Test(timeout = 10000)
    public void testUnsubscribeAfterError() {

        Subscriber<Long> observer = TestHelper.mockSubscriber();

        // Flowable that always fails after 100ms
        FlowableRetryTest.SlowFlowable so = new FlowableRetryTest.SlowFlowable(100, 0);
        Flowable<Long> o = Flowable
                .unsafeCreate(so)
                .retry(retry5);

        FlowableRetryTest.AsyncObserver<Long> async = new FlowableRetryTest.AsyncObserver<Long>(observer);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(observer);
        // Should fail once
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
        assertEquals("Only 1 active subscription", 1, so.maxActive.get());
    }

    @Test(timeout = 10000)
    public void testTimeoutWithRetry() {

        Subscriber<Long> observer = TestHelper.mockSubscriber();

        // Flowable that sends every 100ms (timeout fails instead)
        FlowableRetryTest.SlowFlowable so = new FlowableRetryTest.SlowFlowable(100, 10);
        Flowable<Long> o = Flowable
                .unsafeCreate(so)
                .timeout(80, TimeUnit.MILLISECONDS)
                .retry(retry5);

        FlowableRetryTest.AsyncObserver<Long> async = new FlowableRetryTest.AsyncObserver<Long>(observer);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(observer);
        // Should fail once
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
    }

    @Test
    public void testIssue2826() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final RuntimeException e = new RuntimeException("You shall not pass");
        final AtomicInteger c = new AtomicInteger();
        Flowable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                c.incrementAndGet();
                throw e;
            }
        }).retry(retry5).subscribe(ts);

        ts.assertTerminated();
        assertEquals(6, c.get());
        assertEquals(Collections.singletonList(e), ts.errors());
    }
    @Test
    public void testJustAndRetry() throws Exception {
        final AtomicBoolean throwException = new AtomicBoolean(true);
        int value = Flowable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                if (throwException.compareAndSet(true, false)) {
                    throw new TestException();
                }
                return t1;
            }
        }).retry(1).blockingSingle();

        assertEquals(1, value);
    }

    @Test
    public void testIssue3008RetryWithPredicate() {
        final List<Long> list = new CopyOnWriteArrayList<Long>();
        final AtomicBoolean isFirst = new AtomicBoolean(true);
        Flowable.<Long> just(1L, 2L, 3L).map(new Function<Long, Long>() {
            @Override
            public Long apply(Long x) {
                System.out.println("map " + x);
                if (x == 2 && isFirst.getAndSet(false)) {
                    throw new RuntimeException("retryable error");
                }
                return x;
            }})
        .retry(new BiPredicate<Integer, Throwable>() {
            @Override
            public boolean test(Integer t1, Throwable t2) {
                return true;
            }})
        .forEach(new Consumer<Long>() {

            @Override
            public void accept(Long t) {
                System.out.println(t);
                list.add(t);
            }});
        assertEquals(Arrays.asList(1L,1L,2L,3L), list);
    }

    @Test
    public void testIssue3008RetryInfinite() {
        final List<Long> list = new CopyOnWriteArrayList<Long>();
        final AtomicBoolean isFirst = new AtomicBoolean(true);
        Flowable.<Long> just(1L, 2L, 3L).map(new Function<Long, Long>() {
            @Override
            public Long apply(Long x) {
                System.out.println("map " + x);
                if (x == 2 && isFirst.getAndSet(false)) {
                    throw new RuntimeException("retryable error");
                }
                return x;
            }})
        .retry()
        .forEach(new Consumer<Long>() {

            @Override
            public void accept(Long t) {
                System.out.println(t);
                list.add(t);
            }});
        assertEquals(Arrays.asList(1L,1L,2L,3L), list);
    }

    @Test
    public void testBackpressure() {
        final List<Long> requests = new ArrayList<Long>();

        Flowable<Integer> source = Flowable
                .just(1)
                .concatWith(Flowable.<Integer>error(new TestException()))
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t) {
                        requests.add(t);
                    }
                });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(3L);
        source
        .retry(new BiPredicate<Integer, Throwable>() {
            @Override
            public boolean test(Integer t1, Throwable t2) {
                return t1 < 4; // FIXME was 3 in 1.x for some reason
            }
        }).subscribe(ts);

        assertEquals(Arrays.asList(3L, 2L, 1L), requests);
        ts.assertValues(1, 1, 1);
        ts.assertNotComplete();
        ts.assertNoErrors();
    }

    @Test
    public void predicateThrows() {

        TestSubscriber<Object> to = Flowable.error(new TestException("Outer"))
        .retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void dontRetry() {
        Flowable.error(new TestException("Outer"))
        .retry(Functions.alwaysFalse())
        .test()
        .assertFailureAndMessage(TestException.class, "Outer");
    }

    @Test
    public void retryDisposeRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps = PublishProcessor.create();

            final TestSubscriber<Integer> to = ps.retry(Functions.alwaysTrue()).test();

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onError(ex);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            to.assertEmpty();
        }
    }

    @Test
    public void bipredicateThrows() {

        TestSubscriber<Object> to = Flowable.error(new TestException("Outer"))
        .retry(new BiPredicate<Integer, Throwable>() {
            @Override
            public boolean test(Integer n, Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void retryBiPredicateDisposeRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps = PublishProcessor.create();

            final TestSubscriber<Integer> to = ps.retry(new BiPredicate<Object, Object>() {
                @Override
                public boolean test(Object t1, Object t2) throws Exception {
                    return true;
                }
            }).test();

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onError(ex);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            to.assertEmpty();
        }
    }
}
