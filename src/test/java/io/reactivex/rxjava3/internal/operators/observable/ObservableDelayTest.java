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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableDelayTest extends RxJavaTest {
    private Observer<Long> observer;
    private Observer<Long> observer2;

    private TestScheduler scheduler;

    @Before
    public void before() {
        observer = TestHelper.mockObserver();
        observer2 = TestHelper.mockObserver();

        scheduler = new TestScheduler();
    }

    @Test
    public void delay() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void longDelay() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = source.delay(5L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(5999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(6000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        scheduler.advanceTimeTo(6999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(7000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        scheduler.advanceTimeTo(7999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(8000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void delayWithError() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler)
        .map(new Function<Long, Long>() {
            @Override
            public Long apply(Long value) {
                if (value == 1L) {
                    throw new RuntimeException("error!");
                }
                return value;
            }
        });
        Observable<Long> delayed = source.delay(1L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(1999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();

        scheduler.advanceTimeTo(5000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void delayWithMultipleSubscriptions() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);
        delayed.subscribe(observer2);

        InOrder inOrder = inOrder(observer);
        InOrder inOrder2 = inOrder(observer2);

        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder2.verify(observer2, times(1)).onNext(0L);

        scheduler.advanceTimeTo(2499L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder2.verify(observer2, times(1)).onNext(1L);

        verify(observer, never()).onComplete();
        verify(observer2, never()).onComplete();

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder2.verify(observer2, times(1)).onNext(2L);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());
        inOrder.verify(observer, times(1)).onComplete();
        inOrder2.verify(observer2, times(1)).onComplete();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void delaySubscription() {
        Observable<Integer> result = Observable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        inOrder.verify(o, never()).onNext(any());
        inOrder.verify(o, never()).onComplete();

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        inOrder.verify(o, times(1)).onNext(1);
        inOrder.verify(o, times(1)).onNext(2);
        inOrder.verify(o, times(1)).onNext(3);
        inOrder.verify(o, times(1)).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void delaySubscriptionDisposeBeforeTime() {
        Observable<Integer> result = Observable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();
        TestObserver<Object> to = new TestObserver<>(o);

        result.subscribe(to);
        to.dispose();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void delayWithObservableNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final List<PublishSubject<Integer>> delays = new ArrayList<>();
        final int n = 10;
        for (int i = 0; i < n; i++) {
            PublishSubject<Integer> delay = PublishSubject.create();
            delays.add(delay);
        }

        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return delays.get(t1);
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
            delays.get(i).onNext(i);
            inOrder.verify(o).onNext(i);
        }
        source.onComplete();

        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void delayWithObservableSingleSend1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();

        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return delay;
            }
        };
        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);
        delay.onNext(2);

        inOrder.verify(o).onNext(1);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void delayWithObservableSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();

        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return delay;
            }
        };
        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        source.onError(new TestException());
        delay.onNext(1);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void delayWithObservableDelayFunctionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();

        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };
        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void delayWithObservableDelayThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();

        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return delay;
            }
        };
        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        delay.onError(new TestException());

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void delayWithObservableSubscriptionNormal() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();
        Supplier<Observable<Integer>> subFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                return delay;
            }
        };
        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return delay;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(Observable.defer(subFunc), delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void delayWithObservableSubscriptionFunctionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();
        Supplier<Observable<Integer>> subFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                throw new TestException();
            }
        };
        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return delay;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(Observable.defer(subFunc), delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void delayWithObservableSubscriptionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();
        Supplier<Observable<Integer>> subFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                return delay;
            }
        };
        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return delay;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(Observable.defer(subFunc), delayFunc).subscribe(o);

        source.onNext(1);
        delay.onError(new TestException());

        source.onNext(2);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void delayWithObservableEmptyDelayer() {
        PublishSubject<Integer> source = PublishSubject.create();

        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return Observable.empty();
            }
        };
        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        source.onComplete();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void delayWithObservableSubscriptionRunCompletion() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> sdelay = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();
        Supplier<Observable<Integer>> subFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                return sdelay;
            }
        };
        Function<Integer, Observable<Integer>> delayFunc = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return delay;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.delay(Observable.defer(subFunc), delayFunc).subscribe(o);

        source.onNext(1);
        sdelay.onComplete();

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void delayWithObservableAsTimed() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);

        final Observable<Long> delayer = Observable.timer(500L, TimeUnit.MILLISECONDS, scheduler);

        Function<Long, Observable<Long>> delayFunc = new Function<Long, Observable<Long>>() {
            @Override
            public Observable<Long> apply(Long t1) {
                return delayer;
            }
        };

        Observable<Long> delayed = source.delay(delayFunc);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void delayWithObservableReorder() {
        int n = 3;

        PublishSubject<Integer> source = PublishSubject.create();
        final List<PublishSubject<Integer>> subjects = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            subjects.add(PublishSubject.<Integer> create());
        }

        Observable<Integer> result = source.delay(new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return subjects.get(t1);
            }
        });

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
        }
        source.onComplete();

        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o, never()).onComplete();

        for (int i = n - 1; i >= 0; i--) {
            subjects.get(i).onComplete();
            inOrder.verify(o).onNext(i);
        }

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void delayEmitsEverything() {
        Observable<Integer> source = Observable.range(1, 5);
        Observable<Integer> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed = delayed.doOnEach(new Consumer<Notification<Integer>>() {

            @Override
            public void accept(Notification<Integer> t1) {
                System.out.println(t1);
            }

        });
        TestObserver<Integer> observer = new TestObserver<>();
        delayed.subscribe(observer);
        // all will be delivered after 500ms since range does not delay between them
        scheduler.advanceTimeBy(500L, TimeUnit.MILLISECONDS);
        observer.assertValues(1, 2, 3, 4, 5);
    }

    @Test
    public void backpressureWithTimedDelay() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(1, Flowable.bufferSize() * 2)
                .delay(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, to.values().size());
    }

    @Test
    public void backpressureWithSubscriptionTimedDelay() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(1, Flowable.bufferSize() * 2)
                .delaySubscription(100, TimeUnit.MILLISECONDS)
                .delay(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, to.values().size());
    }

    @Test
    public void backpressureWithSelectorDelay() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(1, Flowable.bufferSize() * 2)
                .delay(new Function<Integer, Observable<Long>>() {

                    @Override
                    public Observable<Long> apply(Integer i) {
                        return Observable.timer(100, TimeUnit.MILLISECONDS);
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, to.values().size());
    }

    @Test
    public void backpressureWithSelectorDelayAndSubscriptionDelay() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(1, Flowable.bufferSize() * 2)
                .delay(Observable.timer(500, TimeUnit.MILLISECONDS)
                , new Function<Integer, Observable<Long>>() {

                    @Override
                    public Observable<Long> apply(Integer i) {
                        return Observable.timer(100, TimeUnit.MILLISECONDS);
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, to.values().size());
    }

    @Test
    public void errorRunsBeforeOnNext() {
        TestScheduler test = new TestScheduler();

        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<>();

        ps.delay(1, TimeUnit.SECONDS, test).subscribe(to);

        ps.onNext(1);

        test.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        ps.onError(new TestException());

        test.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertNoValues();
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void delaySupplierSimple() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        Observable<Integer> source = Observable.range(1, 5);

        TestObserver<Integer> to = new TestObserver<>();

        source.delaySubscription(ps).subscribe(to);

        to.assertNoValues();
        to.assertNoErrors();
        to.assertNotComplete();

        ps.onNext(1);

        to.assertValues(1, 2, 3, 4, 5);
        to.assertComplete();
        to.assertNoErrors();
    }

    @Test
    public void delaySupplierCompletes() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        Observable<Integer> source = Observable.range(1, 5);

        TestObserver<Integer> to = new TestObserver<>();

        source.delaySubscription(ps).subscribe(to);

        to.assertNoValues();
        to.assertNoErrors();
        to.assertNotComplete();

        // FIXME should this complete the source instead of consuming it?
        ps.onComplete();

        to.assertValues(1, 2, 3, 4, 5);
        to.assertComplete();
        to.assertNoErrors();
    }

    @Test
    public void delaySupplierErrors() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        Observable<Integer> source = Observable.range(1, 5);

        TestObserver<Integer> to = new TestObserver<>();

        source.delaySubscription(ps).subscribe(to);

        to.assertNoValues();
        to.assertNoErrors();
        to.assertNotComplete();

        ps.onError(new TestException());

        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(TestException.class);
    }

    @Test
    public void delayWithTimeDelayError() throws Exception {
        Observable.just(1).concatWith(Observable.<Integer>error(new TestException()))
        .delay(100, TimeUnit.MILLISECONDS, true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void onErrorCalledOnScheduler() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> thread = new AtomicReference<>();

        Observable.<String>error(new Exception())
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        thread.set(Thread.currentThread());
                        latch.countDown();
                    }
                })
                .onErrorResumeWith(Observable.<String>empty())
                .subscribe();

        latch.await();

        assertNotEquals(Thread.currentThread(), thread.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().delay(1, TimeUnit.SECONDS));

        TestHelper.checkDisposed(PublishSubject.create().delay(Functions.justFunction(Observable.never())));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.delay(1, TimeUnit.SECONDS);
            }
        });

        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.delay(Functions.justFunction(Observable.never()));
            }
        });
    }

    @Test
    public void onCompleteFinal() {
        TestScheduler scheduler = new TestScheduler();

        Observable.empty()
        .delay(1, TimeUnit.MILLISECONDS, scheduler)
        .subscribe(new DisposableObserver<Object>() {
            @Override
            public void onNext(Object value) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
                throw new TestException();
            }
        });

        try {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void onErrorFinal() {
        TestScheduler scheduler = new TestScheduler();

        Observable.error(new TestException())
        .delay(1, TimeUnit.MILLISECONDS, scheduler)
        .subscribe(new DisposableObserver<Object>() {
            @Override
            public void onNext(Object value) {
            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }

            @Override
            public void onComplete() {
            }
        });

        try {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void itemDelayReturnsNull() {
        Observable.just(1).delay(new Function<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Integer t) throws Exception {
                return null;
            }
        })
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The itemDelay returned a null ObservableSource");
    }
}
