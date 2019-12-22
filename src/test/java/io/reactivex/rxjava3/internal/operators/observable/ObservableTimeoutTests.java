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

import static io.reactivex.rxjava3.internal.util.ExceptionHelper.timeoutMessage;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableTimeoutTests extends RxJavaTest {
    private PublishSubject<String> underlyingSubject;
    private TestScheduler testScheduler;
    private Observable<String> withTimeout;
    private static final long TIMEOUT = 3;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    @Before
    public void setUp() {

        underlyingSubject = PublishSubject.create();
        testScheduler = new TestScheduler();
        withTimeout = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler);
    }

    @Test
    public void shouldNotTimeoutIfOnNextWithinTimeout() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);

        withTimeout.subscribe(to);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        verify(observer).onNext("One");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer, never()).onError(any(Throwable.class));
        to.dispose();
    }

    @Test
    public void shouldNotTimeoutIfSecondOnNextWithinTimeout() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);

        withTimeout.subscribe(to);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");
        verify(observer).onNext("Two");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer, never()).onError(any(Throwable.class));
        to.dispose();
    }

    @Test
    public void shouldTimeoutIfOnNextNotWithinTimeout() {
        TestObserverEx<String> observer = new TestObserverEx<>();

        withTimeout.subscribe(observer);

        testScheduler.advanceTimeBy(TIMEOUT + 1, TimeUnit.SECONDS);
        observer.assertFailureAndMessage(TimeoutException.class, timeoutMessage(TIMEOUT, TIME_UNIT));
    }

    @Test
    public void shouldTimeoutIfSecondOnNextNotWithinTimeout() {
        TestObserverEx<String> observer = new TestObserverEx<>();
        withTimeout.subscribe(observer);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        observer.assertValue("One");
        testScheduler.advanceTimeBy(TIMEOUT + 1, TimeUnit.SECONDS);
        observer.assertFailureAndMessage(TimeoutException.class, timeoutMessage(TIMEOUT, TIME_UNIT), "One");
    }

    @Test
    public void shouldCompleteIfUnderlyingComletes() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);
        withTimeout.subscribe(observer);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onComplete();
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
        to.dispose();
    }

    @Test
    public void shouldErrorIfUnderlyingErrors() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);
        withTimeout.subscribe(observer);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onError(new UnsupportedOperationException());
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer).onError(any(UnsupportedOperationException.class));
        to.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnNextNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler, other);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);
        source.subscribe(to);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("One");
        inOrder.verify(observer, times(1)).onNext("a");
        inOrder.verify(observer, times(1)).onNext("b");
        inOrder.verify(observer, times(1)).onNext("c");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
        to.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnErrorNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler, other);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);
        source.subscribe(to);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onError(new UnsupportedOperationException());
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("One");
        inOrder.verify(observer, times(1)).onNext("a");
        inOrder.verify(observer, times(1)).onNext("b");
        inOrder.verify(observer, times(1)).onNext("c");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
        to.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnCompletedNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler, other);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);
        source.subscribe(to);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onComplete();
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("One");
        inOrder.verify(observer, times(1)).onNext("a");
        inOrder.verify(observer, times(1)).onNext("b");
        inOrder.verify(observer, times(1)).onNext("c");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
        to.dispose();
    }

    @Test
    public void shouldSwitchToOtherAndCanBeUnsubscribedIfOnNextNotWithinTimeout() {
        PublishSubject<String> other = PublishSubject.create();
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler, other);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);
        source.subscribe(to);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");

        other.onNext("a");
        other.onNext("b");
        to.dispose();

        // The following messages should not be delivered.
        other.onNext("c");
        other.onNext("d");
        other.onComplete();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("One");
        inOrder.verify(observer, times(1)).onNext("a");
        inOrder.verify(observer, times(1)).onNext("b");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldTimeoutIfSynchronizedObservableEmitFirstOnNextNotWithinTimeout()
            throws InterruptedException {
        final CountDownLatch exit = new CountDownLatch(1);
        final CountDownLatch timeoutSetuped = new CountDownLatch(1);

        final TestObserverEx<String> observer = new TestObserverEx<>();

        new Thread(new Runnable() {

            @Override
            public void run() {
                Observable.unsafeCreate(new ObservableSource<String>() {

                    @Override
                    public void subscribe(Observer<? super String> observer) {
                        observer.onSubscribe(Disposable.empty());
                        try {
                            timeoutSetuped.countDown();
                            exit.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        observer.onNext("a");
                        observer.onComplete();
                    }

                }).timeout(1, TimeUnit.SECONDS, testScheduler)
                        .subscribe(observer);
            }
        }).start();

        timeoutSetuped.await();
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        observer.assertFailureAndMessage(TimeoutException.class, timeoutMessage(1, TimeUnit.SECONDS));

        exit.countDown(); // exit the thread
    }

    @Test
    public void shouldUnsubscribeFromUnderlyingSubscriptionOnTimeout() throws InterruptedException {
        // From https://github.com/ReactiveX/RxJava/pull/951
        final Disposable upstream = mock(Disposable.class);

        Observable<String> never = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(upstream);
            }
        });

        TestScheduler testScheduler = new TestScheduler();
        Observable<String> observableWithTimeout = never.timeout(1000, TimeUnit.MILLISECONDS, testScheduler);

        TestObserverEx<String> observer = new TestObserverEx<>();
        observableWithTimeout.subscribe(observer);

        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);

        observer.assertFailureAndMessage(TimeoutException.class, timeoutMessage(1000, TimeUnit.MILLISECONDS));

        verify(upstream, times(1)).dispose();
    }

    @Test
    public void shouldUnsubscribeFromUnderlyingSubscriptionOnDispose() {
        final PublishSubject<String> subject = PublishSubject.create();
        final TestScheduler scheduler = new TestScheduler();

        final TestObserver<String> observer = subject
                .timeout(100, TimeUnit.MILLISECONDS, scheduler)
                .test();

        assertTrue(subject.hasObservers());

        observer.dispose();

        assertFalse(subject.hasObservers());
    }

    @Test
    public void timedAndOther() {
        Observable.never().timeout(100, TimeUnit.MILLISECONDS, Observable.just(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishSubject.create().timeout(1, TimeUnit.DAYS));

        TestHelper.checkDisposed(PublishSubject.create().timeout(1, TimeUnit.DAYS, Observable.just(1)));
    }

    @Test
    public void timedErrorOther() {
        Observable.error(new TestException())
        .timeout(1, TimeUnit.DAYS, Observable.just(1))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void timedError() {
        Observable.error(new TestException())
        .timeout(1, TimeUnit.DAYS)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void timedEmptyOther() {
        Observable.empty()
        .timeout(1, TimeUnit.DAYS, Observable.just(1))
        .test()
        .assertResult();
    }

    @Test
    public void timedEmpty() {
        Observable.empty()
        .timeout(1, TimeUnit.DAYS)
        .test()
        .assertResult();
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .timeout(1, TimeUnit.DAYS)
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceOther() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .timeout(1, TimeUnit.DAYS, Observable.just(3))
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void timedTake() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.timeout(1, TimeUnit.DAYS)
        .take(1)
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        to.assertResult(1);
    }

    @Test
    public void timedFallbackTake() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.timeout(1, TimeUnit.DAYS, Observable.just(2))
        .take(1)
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        to.assertResult(1);
    }

    @Test
    public void fallbackErrors() {
        Observable.never()
        .timeout(1, TimeUnit.MILLISECONDS, Observable.error(new TestException()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void onNextOnTimeoutRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestScheduler sch = new TestScheduler();

            final PublishSubject<Integer> ps = PublishSubject.create();

            TestObserverEx<Integer> to = ps.timeout(1, TimeUnit.SECONDS, sch).to(TestHelper.<Integer>testConsumer());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    sch.advanceTimeBy(1, TimeUnit.SECONDS);
                }
            };

            TestHelper.race(r1, r2);

            if (to.values().size() != 0) {
                if (to.errors().size() != 0) {
                    to.assertFailure(TimeoutException.class, 1);
                    to.assertErrorMessage(timeoutMessage(1, TimeUnit.SECONDS));
                } else {
                    to.assertValuesOnly(1);
                }
            } else {
                to.assertFailure(TimeoutException.class);
                to.assertErrorMessage(timeoutMessage(1, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    public void onNextOnTimeoutRaceFallback() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestScheduler sch = new TestScheduler();

            final PublishSubject<Integer> ps = PublishSubject.create();

            TestObserverEx<Integer> to = ps.timeout(1, TimeUnit.SECONDS, sch, Observable.just(2)).to(TestHelper.<Integer>testConsumer());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    sch.advanceTimeBy(1, TimeUnit.SECONDS);
                }
            };

            TestHelper.race(r1, r2);

            if (to.isTerminated()) {
                int c = to.values().size();
                if (c == 1) {
                    int v = to.values().get(0);
                    assertTrue("" + v, v == 1 || v == 2);
                } else {
                    to.assertResult(1, 2);
                }
            } else {
                to.assertValuesOnly(1);
            }
        }
    }
}
