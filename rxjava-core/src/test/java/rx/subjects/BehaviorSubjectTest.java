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

import rx.Subscriber;
import rx.Subscription;
import rx.observers.TestObserver;

public class BehaviorSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testThatObserverReceivesDefaultValueAndSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Subscriber<String> observer = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<String>(observer));

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(testException);
        verify(observer, Mockito.never()).onCompleted();
    }

    @Test
    public void testThatObserverReceivesLatestAndThenSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        subject.onNext("one");

        @SuppressWarnings("unchecked")
        Subscriber<String> observer = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<String>(observer));

        subject.onNext("two");
        subject.onNext("three");

        verify(observer, Mockito.never()).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(testException);
        verify(observer, Mockito.never()).onCompleted();
    }

    @Test
    public void testSubscribeThenOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Subscriber<String> observer = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<String>(observer));

        subject.onNext("one");
        subject.onCompleted();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeToCompletedOnlyEmitsOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");
        subject.onNext("one");
        subject.onCompleted();

        @SuppressWarnings("unchecked")
        Subscriber<String> observer = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<String>(observer));

        verify(observer, never()).onNext("default");
        verify(observer, never()).onNext("one");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeToErrorOnlyEmitsOnError() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");
        subject.onNext("one");
        RuntimeException re = new RuntimeException("test error");
        subject.onError(re);

        @SuppressWarnings("unchecked")
        Subscriber<String> observer = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<String>(observer));

        verify(observer, never()).onNext("default");
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onError(re);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testCompletedStopsEmittingData() {
        BehaviorSubject<Integer> channel = BehaviorSubject.create(2013);
        @SuppressWarnings("unchecked")
        Subscriber<Object> observerA = mock(Subscriber.class);
        @SuppressWarnings("unchecked")
        Subscriber<Object> observerB = mock(Subscriber.class);
        @SuppressWarnings("unchecked")
        Subscriber<Object> observerC = mock(Subscriber.class);

        Subscription a = channel.toObservable().subscribe(new TestObserver<Object>(observerA));
        Subscription b = channel.toObservable().subscribe(new TestObserver<Object>(observerB));

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

        Subscription c = channel.toObservable().subscribe(new TestObserver<Object>(observerC));

        inOrderC.verify(observerC).onCompleted();

        channel.onNext(13);

        inOrderB.verifyNoMoreInteractions();
        inOrderC.verifyNoMoreInteractions();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Subscriber<String> observer = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<String>(observer));

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verify(observer, never()).onNext("two");
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent2() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Subscriber<String> observer = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<String>(observer));

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verify(observer, never()).onNext("two");
        verify(observer, never()).onCompleted();

        Subscriber<Object> o2 = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<Object>(o2));
        verify(o2, times(1)).onError(testException);
        verify(o2, never()).onNext(any());
        verify(o2, never()).onCompleted();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent3() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Subscriber<String> observer = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<String>(observer));

        subject.onNext("one");
        subject.onCompleted();
        subject.onNext("two");
        subject.onCompleted();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext("two");

        Subscriber<Object> o2 = mock(Subscriber.class);
        subject.toObservable().subscribe(new TestObserver<Object>(o2));
        verify(o2, times(1)).onCompleted();
        verify(o2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }
}
