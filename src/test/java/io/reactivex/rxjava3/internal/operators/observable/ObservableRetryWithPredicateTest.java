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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableRetryWithPredicateTest extends RxJavaTest {
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
    public void withNothingToRetry() {
        Observable<Integer> source = Observable.range(0, 3);

        Observer<Integer> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.retry(retryTwice).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void retryTwice() {
        Observable<Integer> source = Observable.unsafeCreate(new ObservableSource<Integer>() {
            int count;
            @Override
            public void subscribe(Observer<? super Integer> t1) {
                t1.onSubscribe(Disposable.empty());
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

        Observer<Integer> o = TestHelper.mockObserver();
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
    public void retryTwiceAndGiveUp() {
        Observable<Integer> source = Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> t1) {
                t1.onSubscribe(Disposable.empty());
                t1.onNext(0);
                t1.onNext(1);
                t1.onError(new TestException());
            }
        });

        Observer<Integer> o = TestHelper.mockObserver();
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
    public void retryOnSpecificException() {
        Observable<Integer> source = Observable.unsafeCreate(new ObservableSource<Integer>() {
            int count;
            @Override
            public void subscribe(Observer<? super Integer> t1) {
                t1.onSubscribe(Disposable.empty());
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

        Observer<Integer> o = TestHelper.mockObserver();
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
    public void retryOnSpecificExceptionAndNotOther() {
        final IOException ioe = new IOException();
        final TestException te = new TestException();
        Observable<Integer> source = Observable.unsafeCreate(new ObservableSource<Integer>() {
            int count;
            @Override
            public void subscribe(Observer<? super Integer> t1) {
                t1.onSubscribe(Disposable.empty());
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

        Observer<Integer> o = TestHelper.mockObserver();
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
    public void unsubscribeFromRetry() {
        PublishSubject<Integer> subject = PublishSubject.create();
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

    @Test
    public void unsubscribeAfterError() {

        Observer<Long> observer = TestHelper.mockObserver();

        // Observable that always fails after 100ms
        ObservableRetryTest.SlowObservable so = new ObservableRetryTest.SlowObservable(100, 0, "testUnsubscribeAfterError");
        Observable<Long> o = Observable
                .unsafeCreate(so)
                .retry(retry5);

        ObservableRetryTest.AsyncObserver<Long> async = new ObservableRetryTest.AsyncObserver<>(observer);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(observer);
        // Should fail once
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
        assertEquals("Only 1 active subscription", 1, so.maxActive.get());
    }

    @Test
    public void timeoutWithRetry() {

        Observer<Long> observer = TestHelper.mockObserver();

        // Observable that sends every 100ms (timeout fails instead)
        ObservableRetryTest.SlowObservable so = new ObservableRetryTest.SlowObservable(100, 10, "testTimeoutWithRetry");
        Observable<Long> o = Observable
                .unsafeCreate(so)
                .timeout(80, TimeUnit.MILLISECONDS)
                .retry(retry5);

        ObservableRetryTest.AsyncObserver<Long> async = new ObservableRetryTest.AsyncObserver<>(observer);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(observer);
        // Should fail once
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
    }

    @Test
    public void issue2826() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        final RuntimeException e = new RuntimeException("You shall not pass");
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                c.incrementAndGet();
                throw e;
            }
        }).retry(retry5).subscribe(to);

        to.assertTerminated();
        assertEquals(6, c.get());
        assertEquals(Collections.singletonList(e), to.errors());
    }

    @Test
    public void justAndRetry() throws Exception {
        final AtomicBoolean throwException = new AtomicBoolean(true);
        int value = Observable.just(1).map(new Function<Integer, Integer>() {
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
    public void issue3008RetryWithPredicate() {
        final List<Long> list = new CopyOnWriteArrayList<>();
        final AtomicBoolean isFirst = new AtomicBoolean(true);
        Observable.<Long> just(1L, 2L, 3L).map(new Function<Long, Long>() {
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
        assertEquals(Arrays.asList(1L, 1L, 2L, 3L), list);
    }

    @Test
    public void issue3008RetryInfinite() {
        final List<Long> list = new CopyOnWriteArrayList<>();
        final AtomicBoolean isFirst = new AtomicBoolean(true);
        Observable.<Long> just(1L, 2L, 3L).map(new Function<Long, Long>() {
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
        assertEquals(Arrays.asList(1L, 1L, 2L, 3L), list);
    }

    @Test
    public void predicateThrows() {

        TestObserverEx<Object> to = Observable.error(new TestException("Outer"))
        .retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .to(TestHelper.testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void dontRetry() {
        Observable.error(new TestException("Outer"))
        .retry(Functions.alwaysFalse())
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "Outer");
    }

    @Test
    public void retryDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = ps.retry(Functions.alwaysTrue()).test();

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
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }

    @Test
    public void bipredicateThrows() {

        TestObserverEx<Object> to = Observable.error(new TestException("Outer"))
        .retry(new BiPredicate<Integer, Throwable>() {
            @Override
            public boolean test(Integer n, Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .to(TestHelper.testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void retryBiPredicateDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = ps.retry(new BiPredicate<Object, Object>() {
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
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }
}
