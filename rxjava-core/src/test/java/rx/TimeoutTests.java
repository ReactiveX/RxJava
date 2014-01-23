/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockitoAnnotations;

import rx.observers.TestObserver;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class TimeoutTests {
    private PublishSubject<String> underlyingSubject;
    private TestScheduler testScheduler;
    private Observable<String> withTimeout;
    private static final long TIMEOUT = 3;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underlyingSubject = PublishSubject.create();
        testScheduler = new TestScheduler();
        withTimeout = underlyingSubject.toObservable().timeout(TIMEOUT, TIME_UNIT, testScheduler);
    }

    @Test
    public void shouldNotTimeoutIfOnNextWithinTimeout() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = withTimeout.subscribe(new TestObserver<String>(observer));
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        verify(observer).onNext("One");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer, never()).onError(any(Throwable.class));
        subscription.unsubscribe();
    }

    @Test
    public void shouldNotTimeoutIfSecondOnNextWithinTimeout() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = withTimeout.subscribe(new TestObserver<String>(observer));
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");
        verify(observer).onNext("Two");
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer, never()).onError(any(Throwable.class));
        subscription.unsubscribe();
    }

    @Test
    public void shouldTimeoutIfOnNextNotWithinTimeout() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = withTimeout.subscribe(new TestObserver<String>(observer));
        testScheduler.advanceTimeBy(TIMEOUT + 1, TimeUnit.SECONDS);
        verify(observer).onError(any(TimeoutException.class));
        subscription.unsubscribe();
    }

    @Test
    public void shouldTimeoutIfSecondOnNextNotWithinTimeout() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = withTimeout.subscribe(new TestObserver<String>(observer));
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        verify(observer).onNext("One");
        testScheduler.advanceTimeBy(TIMEOUT + 1, TimeUnit.SECONDS);
        verify(observer).onError(any(TimeoutException.class));
        subscription.unsubscribe();
    }

    @Test
    public void shouldCompleteIfUnderlyingComletes() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = withTimeout.subscribe(new TestObserver<String>(observer));
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onCompleted();
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        subscription.unsubscribe();
    }

    @Test
    public void shouldErrorIfUnderlyingErrors() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = withTimeout.subscribe(new TestObserver<String>(observer));
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onError(new UnsupportedOperationException());
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        verify(observer).onError(any(UnsupportedOperationException.class));
        subscription.unsubscribe();
    }

    @Test
    public void shouldSwitchToOtherIfOnNextNotWithinTimeout() {
        Observable<String> other = Observable.from("a", "b", "c");
        Observable<String> source = underlyingSubject.toObservable().timeout(TIMEOUT, TIME_UNIT, other, testScheduler);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = source.subscribe(new TestObserver<String>(observer));

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("One");
        inOrder.verify(observer, times(1)).onNext("a");
        inOrder.verify(observer, times(1)).onNext("b");
        inOrder.verify(observer, times(1)).onNext("c");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
        subscription.unsubscribe();
    }

    @Test
    public void shouldSwitchToOtherIfOnErrorNotWithinTimeout() {
        Observable<String> other = Observable.from("a", "b", "c");
        Observable<String> source = underlyingSubject.toObservable().timeout(TIMEOUT, TIME_UNIT, other, testScheduler);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = source.subscribe(new TestObserver<String>(observer));

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onError(new UnsupportedOperationException());
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("One");
        inOrder.verify(observer, times(1)).onNext("a");
        inOrder.verify(observer, times(1)).onNext("b");
        inOrder.verify(observer, times(1)).onNext("c");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
        subscription.unsubscribe();
    }

    @Test
    public void shouldSwitchToOtherIfOnCompletedNotWithinTimeout() {
        Observable<String> other = Observable.from("a", "b", "c");
        Observable<String> source = underlyingSubject.toObservable().timeout(TIMEOUT, TIME_UNIT, other, testScheduler);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = source.subscribe(new TestObserver<String>(observer));

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onCompleted();
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("One");
        inOrder.verify(observer, times(1)).onNext("a");
        inOrder.verify(observer, times(1)).onNext("b");
        inOrder.verify(observer, times(1)).onNext("c");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
        subscription.unsubscribe();
    }

    @Test
    public void shouldSwitchToOtherAndCanBeUnsubscribedIfOnNextNotWithinTimeout() {
        PublishSubject<String> other = PublishSubject.create();
        Observable<String> source = underlyingSubject.toObservable().timeout(TIMEOUT, TIME_UNIT, other.toObservable(), testScheduler);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = source.subscribe(new TestObserver<String>(observer));

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        underlyingSubject.onNext("One");
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS);
        underlyingSubject.onNext("Two");

        other.onNext("a");
        other.onNext("b");
        subscription.unsubscribe();

        // The following messages should not be delivered.
        other.onNext("c");
        other.onNext("d");
        other.onCompleted();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("One");
        inOrder.verify(observer, times(1)).onNext("a");
        inOrder.verify(observer, times(1)).onNext("b");
        inOrder.verifyNoMoreInteractions();
    }
}
