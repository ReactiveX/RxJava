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
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableAmbTest extends RxJavaTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void setUp() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    private Observable<String> createObservable(final String[] values,
            final long interval, final Throwable e) {
        return Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(final Observer<? super String> observer) {
                CompositeDisposable parentSubscription = new CompositeDisposable();

                observer.onSubscribe(parentSubscription);

                long delay = interval;
                for (final String value : values) {
                    parentSubscription.add(innerScheduler.schedule(new Runnable() {
                        @Override
                        public void run() {
                            observer.onNext(value);
                        }
                    }
                    , delay, TimeUnit.MILLISECONDS));
                    delay += interval;
                }
                parentSubscription.add(innerScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                            if (e == null) {
                                observer.onComplete();
                            } else {
                                observer.onError(e);
                            }
                    }
                }, delay, TimeUnit.MILLISECONDS));
            }
        });
    }

    @Test
    public void amb() {
        Observable<String> observable1 = createObservable(new String[] {
                "1", "11", "111", "1111" }, 2000, null);
        Observable<String> observable2 = createObservable(new String[] {
                "2", "22", "222", "2222" }, 1000, null);
        Observable<String> observable3 = createObservable(new String[] {
                "3", "33", "333", "3333" }, 3000, null);

        Observable<String> o = Observable.ambArray(observable1,
                observable2, observable3);

        Observer<String> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("2");
        inOrder.verify(observer, times(1)).onNext("22");
        inOrder.verify(observer, times(1)).onNext("222");
        inOrder.verify(observer, times(1)).onNext("2222");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void amb2() {
        IOException expectedException = new IOException(
                "fake exception");
        Observable<String> observable1 = createObservable(new String[] {},
                2000, new IOException("fake exception"));
        Observable<String> observable2 = createObservable(new String[] {
                "2", "22", "222", "2222" }, 1000, expectedException);
        Observable<String> observable3 = createObservable(new String[] {},
                3000, new IOException("fake exception"));

        Observable<String> o = Observable.ambArray(observable1,
                observable2, observable3);

        Observer<String> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("2");
        inOrder.verify(observer, times(1)).onNext("22");
        inOrder.verify(observer, times(1)).onNext("222");
        inOrder.verify(observer, times(1)).onNext("2222");
        inOrder.verify(observer, times(1)).onError(expectedException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void amb3() {
        Observable<String> observable1 = createObservable(new String[] {
                "1" }, 2000, null);
        Observable<String> observable2 = createObservable(new String[] {},
                1000, null);
        Observable<String> observable3 = createObservable(new String[] {
                "3" }, 3000, null);

        Observable<String> o = Observable.ambArray(observable1,
                observable2, observable3);

        Observer<String> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void subscriptionOnlyHappensOnce() throws InterruptedException {
        final AtomicLong count = new AtomicLong();
        Consumer<Disposable> incrementer = new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) {
                count.incrementAndGet();
            }
        };

        //this aync stream should emit first
        Observable<Integer> o1 = Observable.just(1).doOnSubscribe(incrementer)
                .delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        //this stream emits second
        Observable<Integer> o2 = Observable.just(1).doOnSubscribe(incrementer)
                .delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        TestObserver<Integer> to = new TestObserver<>();
        Observable.ambArray(o1, o2).subscribe(to);
        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        assertEquals(2, count.get());
    }

    @Test
    public void synchronousSources() {
        // under async subscription the second Observable would complete before
        // the first but because this is a synchronous subscription to sources
        // then second Observable does not get subscribed to before first
        // subscription completes hence first Observable emits result through
        // amb
        int result = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        //
                    }
            }
        }).ambWith(Observable.just(2)).blockingSingle();
        assertEquals(1, result);
    }

    @Test
    public void ambCancelsOthers() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();
        PublishSubject<Integer> source3 = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<>();

        Observable.ambArray(source1, source2, source3).subscribe(to);

        assertTrue("Source 1 doesn't have subscribers!", source1.hasObservers());
        assertTrue("Source 2 doesn't have subscribers!", source2.hasObservers());
        assertTrue("Source 3 doesn't have subscribers!", source3.hasObservers());

        source1.onNext(1);

        assertTrue("Source 1 doesn't have subscribers!", source1.hasObservers());
        assertFalse("Source 2 still has subscribers!", source2.hasObservers());
        assertFalse("Source 2 still has subscribers!", source3.hasObservers());

    }

    @Test
    public void ambArrayEmpty() {
        assertSame(Observable.empty(), Observable.ambArray());
    }

    @Test
    public void ambArraySingleElement() {
        assertSame(Observable.never(), Observable.ambArray(Observable.never()));
    }

    @Test
    public void manySources() {
        Observable<?>[] a = new Observable[32];
        Arrays.fill(a, Observable.never());
        a[31] = Observable.just(1);

        Observable.amb(Arrays.asList(a))
        .test()
        .assertResult(1);
    }

    @Test
    public void emptyIterable() {
        Observable.amb(Collections.<Observable<Integer>>emptyList())
        .test()
        .assertResult();
    }

    @Test
    public void singleIterable() {
        Observable.amb(Collections.singletonList(Observable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.ambArray(Observable.never(), Observable.never()));
    }

    @Test
    public void onNextRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();
            final PublishSubject<Integer> ps2 = PublishSubject.create();

            TestObserverEx<Integer> to = Observable.ambArray(ps1, ps2).to(TestHelper.<Integer>testConsumer());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onNext(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps2.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            to.assertSubscribed()
            .assertNoErrors()
            .assertNotComplete()
            .assertValueCount(1);
        }
    }

    @Test
    public void onCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();
            final PublishSubject<Integer> ps2 = PublishSubject.create();

            TestObserver<Integer> to = Observable.ambArray(ps1, ps2).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps2.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            to.assertResult();
        }
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();
            final PublishSubject<Integer> ps2 = PublishSubject.create();

            TestObserver<Integer> to = Observable.ambArray(ps1, ps2).test();

            final Throwable ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onError(ex);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps2.onError(ex);
                }
            };

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestHelper.race(r1, r2);
            } finally {
                RxJavaPlugins.reset();
            }

            to.assertFailure(TestException.class);
            if (!errors.isEmpty()) {
                TestHelper.assertUndeliverable(errors, 0, TestException.class);
            }
        }
    }

    @Test
    public void ambWithOrder() {
        Observable<Integer> error = Observable.error(new RuntimeException());
        Observable.just(1).ambWith(error).test().assertValue(1).assertComplete();
    }

    @Test
    public void ambIterableOrder() {
        Observable<Integer> error = Observable.error(new RuntimeException());
        Observable.amb(Arrays.asList(Observable.just(1), error)).test().assertValue(1).assertComplete();
    }

    @Test
    public void ambArrayOrder() {
        Observable<Integer> error = Observable.error(new RuntimeException());
        Observable.ambArray(Observable.just(1), error).test().assertValue(1).assertComplete();
    }

    @Test
    public void noWinnerSuccessDispose() throws Exception {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Observable.ambArray(
                Observable.just(1)
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Observable.never()
            )
            .subscribe(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void noWinnerErrorDispose() throws Exception {
        final TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Observable.ambArray(
                Observable.error(ex)
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Observable.never()
            )
            .subscribe(Functions.emptyConsumer(), new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void noWinnerCompleteDispose() throws Exception {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Observable.ambArray(
                Observable.empty()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Observable.never()
            )
            .subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), new Action() {
                @Override
                public void run() throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void observableSourcesInIterable() {
        ObservableSource<Integer> source = new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> observer) {
                Observable.just(1).subscribe(observer);
            }
        };

        Observable.amb(Arrays.asList(source, source))
        .test()
        .assertResult(1);
    }
}
