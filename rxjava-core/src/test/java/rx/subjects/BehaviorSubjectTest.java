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
package rx.subjects;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observer;
import rx.Subscription;

public class BehaviorSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testThatObserverReceivesDefaultValueAndSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testThatObserverReceivesLatestAndThenSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        subject.onNext("one");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("two");
        subject.onNext("three");

        verify(aObserver, Mockito.never()).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testSubscribeThenOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onCompleted();

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeToCompletedOnlyEmitsOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");
        subject.onNext("one");
        subject.onCompleted();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        verify(aObserver, never()).onNext("default");
        verify(aObserver, never()).onNext("one");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeToErrorOnlyEmitsOnError() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");
        subject.onNext("one");
        RuntimeException re = new RuntimeException("test error");
        subject.onError(re);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        verify(aObserver, never()).onNext("default");
        verify(aObserver, never()).onNext("one");
        verify(aObserver, times(1)).onError(re);
        verify(aObserver, never()).onCompleted();
    }

    @Test
    public void testCompletedStopsEmittingData() {
        BehaviorSubject<Integer> channel = BehaviorSubject.create(2013);
        @SuppressWarnings("unchecked")
        Observer<Object> observerA = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerB = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerC = mock(Observer.class);

        Subscription a = channel.subscribe(observerA);
        Subscription b = channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);

        inOrderA.verify(observerA).onNext(2013);
        inOrderB.verify(observerB).onNext(2013);

        channel.onNext(42);

        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        a.unsubscribe();
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        inOrderB.verify(observerB).onNext(4711);

        channel.onCompleted();

        inOrderB.verify(observerB).onCompleted();

        Subscription c = channel.subscribe(observerC);

        inOrderC.verify(observerC).onCompleted();

        channel.onNext(13);

        inOrderB.verifyNoMoreInteractions();
        inOrderC.verifyNoMoreInteractions();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onError(testException);
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onCompleted();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent2() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onError(testException);
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onCompleted();

        Observer<Object> o2 = mock(Observer.class);
        subject.subscribe(o2);
        verify(o2, times(1)).onError(testException);
        verify(o2, never()).onNext(any());
        verify(o2, never()).onCompleted();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent3() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onCompleted();
        subject.onNext("two");
        subject.onCompleted();

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onCompleted();
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, never()).onNext("two");

        Observer<Object> o2 = mock(Observer.class);
        subject.subscribe(o2);
        verify(o2, times(1)).onCompleted();
        verify(o2, never()).onNext(any());
        verify(aObserver, never()).onError(any(Throwable.class));
    }
}
