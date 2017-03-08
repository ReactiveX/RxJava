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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.PublishSubject;

public class ObservableTimeoutTests {
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
        TestObserver<String> ts = new TestObserver<String>(observer);

        withTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        verify(observer).onNext("One");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer, never()).onError(any(Throwable.class));
        ts.dispose();
    }

    @Test
    public void shouldNotTimeoutIfSecondOnNextWithinTimeout() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);

        withTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");
        verify(observer).onNext("Two");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer, never()).onError(any(Throwable.class));
        ts.dispose();
    }

    @Test
    public void shouldTimeoutIfOnNextNotWithinTimeout() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);

        withTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(TIMEOUT + 1, TimeUnit.SECONDS);
        verify(observer).onError(any(TimeoutException.class));
        ts.dispose();
    }

    @Test
    public void shouldTimeoutIfSecondOnNextNotWithinTimeout() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        withTimeout.subscribe(observer);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        verify(observer).onNext("One");
        testScheduler.advanceTimeBy(TIMEOUT + 1, TimeUnit.SECONDS);
        verify(observer).onError(any(TimeoutException.class));
        ts.dispose();
    }

    @Test
    public void shouldCompleteIfUnderlyingComletes() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        withTimeout.subscribe(observer);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onComplete();
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
        ts.dispose();
    }

    @Test
    public void shouldErrorIfUnderlyingErrors() {
        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        withTimeout.subscribe(observer);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onError(new UnsupportedOperationException());
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer).onError(any(UnsupportedOperationException.class));
        ts.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnNextNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler, other);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        source.subscribe(ts);

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
        ts.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnErrorNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler, other);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        source.subscribe(ts);

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
        ts.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnCompletedNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler, other);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        source.subscribe(ts);

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
        ts.dispose();
    }

    @Test
    public void shouldSwitchToOtherAndCanBeUnsubscribedIfOnNextNotWithinTimeout() {
        PublishSubject<String> other = PublishSubject.create();
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, testScheduler, other);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        source.subscribe(ts);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");

        other.onNext("a");
        other.onNext("b");
        ts.dispose();

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

        final Observer<String> observer = TestHelper.mockObserver();
        final TestObserver<String> ts = new TestObserver<String>(observer);

        new Thread(new Runnable() {

            @Override
            public void run() {
                Observable.unsafeCreate(new ObservableSource<String>() {

                    @Override
                    public void subscribe(Observer<? super String> observer) {
                        observer.onSubscribe(Disposables.empty());
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
                        .subscribe(ts);
            }
        }).start();

        timeoutSetuped.await();
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();

        exit.countDown(); // exit the thread
    }

    @Test
    public void shouldUnsubscribeFromUnderlyingSubscriptionOnTimeout() throws InterruptedException {
        // From https://github.com/ReactiveX/RxJava/pull/951
        final Disposable s = mock(Disposable.class);

        Observable<String> never = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(s);
            }
        });

        TestScheduler testScheduler = new TestScheduler();
        Observable<String> observableWithTimeout = never.timeout(1000, TimeUnit.MILLISECONDS, testScheduler);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        observableWithTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();

        verify(s, times(1)).dispose();
    }

    @Test
    @Ignore("s should be considered cancelled upon executing onComplete and not expect downstream to call cancel")
    public void shouldUnsubscribeFromUnderlyingSubscriptionOnImmediatelyComplete() {
        // From https://github.com/ReactiveX/RxJava/pull/951
        final Disposable s = mock(Disposable.class);

        Observable<String> immediatelyComplete = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(s);
                observer.onComplete();
            }
        });

        TestScheduler testScheduler = new TestScheduler();
        Observable<String> observableWithTimeout = immediatelyComplete.timeout(1000, TimeUnit.MILLISECONDS,
                testScheduler);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        observableWithTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(s, times(1)).dispose();
    }

    @Test
    @Ignore("s should be considered cancelled upon executing onError and not expect downstream to call cancel")
    public void shouldUnsubscribeFromUnderlyingSubscriptionOnImmediatelyErrored() throws InterruptedException {
        // From https://github.com/ReactiveX/RxJava/pull/951
        final Disposable s = mock(Disposable.class);

        Observable<String> immediatelyError = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(s);
                observer.onError(new IOException("Error"));
            }
        });

        TestScheduler testScheduler = new TestScheduler();
        Observable<String> observableWithTimeout = immediatelyError.timeout(1000, TimeUnit.MILLISECONDS,
                testScheduler);

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        observableWithTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onError(isA(IOException.class));
        inOrder.verifyNoMoreInteractions();

        verify(s, times(1)).dispose();
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
    public void newTimer() {
        ObservableTimeoutTimed.NEW_TIMER.dispose();
        assertTrue(ObservableTimeoutTimed.NEW_TIMER.isDisposed());
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

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
                    observer.onSubscribe(Disposables.empty());

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
}
