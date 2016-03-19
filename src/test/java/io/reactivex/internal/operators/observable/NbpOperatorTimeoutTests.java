/**
 * Copyright 2016 Netflix, Inc.
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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.PublishSubject;

public class NbpOperatorTimeoutTests {
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
        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        
        withTimeout.subscribe(ts);
        
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        verify(NbpObserver).onNext("One");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(NbpObserver, never()).onError(any(Throwable.class));
        ts.dispose();
    }

    @Test
    public void shouldNotTimeoutIfSecondOnNextWithinTimeout() {
        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        
        withTimeout.subscribe(ts);
        
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");
        verify(NbpObserver).onNext("Two");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(NbpObserver, never()).onError(any(Throwable.class));
        ts.dispose();
    }

    @Test
    public void shouldTimeoutIfOnNextNotWithinTimeout() {
        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        
        withTimeout.subscribe(ts);
        
        testScheduler.advanceTimeBy(TIMEOUT + 1, TimeUnit.SECONDS);
        verify(NbpObserver).onError(any(TimeoutException.class));
        ts.dispose();
    }

    @Test
    public void shouldTimeoutIfSecondOnNextNotWithinTimeout() {
        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        withTimeout.subscribe(NbpObserver);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        verify(NbpObserver).onNext("One");
        testScheduler.advanceTimeBy(TIMEOUT + 1, TimeUnit.SECONDS);
        verify(NbpObserver).onError(any(TimeoutException.class));
        ts.dispose();
    }

    @Test
    public void shouldCompleteIfUnderlyingComletes() {
        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        withTimeout.subscribe(NbpObserver);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onComplete();
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(NbpObserver).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
        ts.dispose();
    }

    @Test
    public void shouldErrorIfUnderlyingErrors() {
        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        withTimeout.subscribe(NbpObserver);
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onError(new UnsupportedOperationException());
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(NbpObserver).onError(any(UnsupportedOperationException.class));
        ts.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnNextNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, other, testScheduler);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        source.subscribe(ts);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");
        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("One");
        inOrder.verify(NbpObserver, times(1)).onNext("a");
        inOrder.verify(NbpObserver, times(1)).onNext("b");
        inOrder.verify(NbpObserver, times(1)).onNext("c");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
        ts.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnErrorNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, other, testScheduler);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        source.subscribe(ts);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onError(new UnsupportedOperationException());
        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("One");
        inOrder.verify(NbpObserver, times(1)).onNext("a");
        inOrder.verify(NbpObserver, times(1)).onNext("b");
        inOrder.verify(NbpObserver, times(1)).onNext("c");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
        ts.dispose();
    }

    @Test
    public void shouldSwitchToOtherIfOnCompletedNotWithinTimeout() {
        Observable<String> other = Observable.just("a", "b", "c");
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, other, testScheduler);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        source.subscribe(ts);

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onComplete();
        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("One");
        inOrder.verify(NbpObserver, times(1)).onNext("a");
        inOrder.verify(NbpObserver, times(1)).onNext("b");
        inOrder.verify(NbpObserver, times(1)).onNext("c");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
        ts.dispose();
    }

    @Test
    public void shouldSwitchToOtherAndCanBeUnsubscribedIfOnNextNotWithinTimeout() {
        PublishSubject<String> other = PublishSubject.create();
        Observable<String> source = underlyingSubject.timeout(TIMEOUT, TIME_UNIT, other, testScheduler);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
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

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("One");
        inOrder.verify(NbpObserver, times(1)).onNext("a");
        inOrder.verify(NbpObserver, times(1)).onNext("b");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldTimeoutIfSynchronizedObservableEmitFirstOnNextNotWithinTimeout()
            throws InterruptedException {
        final CountDownLatch exit = new CountDownLatch(1);
        final CountDownLatch timeoutSetuped = new CountDownLatch(1);

        final Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        final TestObserver<String> ts = new TestObserver<String>(NbpObserver);

        new Thread(new Runnable() {

            @Override
            public void run() {
                Observable.create(new NbpOnSubscribe<String>() {

                    @Override
                    public void accept(Observer<? super String> NbpSubscriber) {
                        NbpSubscriber.onSubscribe(EmptyDisposable.INSTANCE);
                        try {
                            timeoutSetuped.countDown();
                            exit.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        NbpSubscriber.onNext("a");
                        NbpSubscriber.onComplete();
                    }

                }).timeout(1, TimeUnit.SECONDS, testScheduler)
                        .subscribe(ts);
            }
        }).start();

        timeoutSetuped.await();
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();

        exit.countDown(); // exit the thread
    }

    @Test
    public void shouldUnsubscribeFromUnderlyingSubscriptionOnTimeout() throws InterruptedException {
        // From https://github.com/ReactiveX/RxJava/pull/951
        final Disposable s = mock(Disposable.class);

        Observable<String> never = Observable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(Observer<? super String> NbpSubscriber) {
                NbpSubscriber.onSubscribe(s);
            }
        });

        TestScheduler testScheduler = new TestScheduler();
        Observable<String> observableWithTimeout = never.timeout(1000, TimeUnit.MILLISECONDS, testScheduler);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        observableWithTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();

        verify(s, times(1)).dispose();
    }

    @Test
    @Ignore("s should be considered cancelled upon executing onComplete and not expect downstream to call cancel")
    public void shouldUnsubscribeFromUnderlyingSubscriptionOnImmediatelyComplete() {
        // From https://github.com/ReactiveX/RxJava/pull/951
        final Disposable s = mock(Disposable.class);

        Observable<String> immediatelyComplete = Observable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(Observer<? super String> NbpSubscriber) {
                NbpSubscriber.onSubscribe(s);
                NbpSubscriber.onComplete();
            }
        });

        TestScheduler testScheduler = new TestScheduler();
        Observable<String> observableWithTimeout = immediatelyComplete.timeout(1000, TimeUnit.MILLISECONDS,
                testScheduler);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        observableWithTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(s, times(1)).dispose();
    }

    @Test
    @Ignore("s should be considered cancelled upon executing onError and not expect downstream to call cancel")
    public void shouldUnsubscribeFromUnderlyingSubscriptionOnImmediatelyErrored() throws InterruptedException {
        // From https://github.com/ReactiveX/RxJava/pull/951
        final Disposable s = mock(Disposable.class);

        Observable<String> immediatelyError = Observable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(Observer<? super String> NbpSubscriber) {
                NbpSubscriber.onSubscribe(s);
                NbpSubscriber.onError(new IOException("Error"));
            }
        });

        TestScheduler testScheduler = new TestScheduler();
        Observable<String> observableWithTimeout = immediatelyError.timeout(1000, TimeUnit.MILLISECONDS,
                testScheduler);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        observableWithTimeout.subscribe(ts);

        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver).onError(isA(IOException.class));
        inOrder.verifyNoMoreInteractions();

        verify(s, times(1)).dispose();
    }
}