/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import rx.util.functions.Action1;
import rx.util.functions.Func0;

public class BehaviorSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testThatObserverReceivesDefaultValueIfNothingWasPublished() {
        BehaviorSubject<String> subject = BehaviorSubject.createWithDefaultValue("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        assertReceivedAllEvents(aObserver);
    }

    private void assertReceivedAllEvents(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testThatObserverDoesNotReceiveDefaultValueIfSomethingWasPublished() {
        BehaviorSubject<String> subject = BehaviorSubject.createWithDefaultValue("default");

        subject.onNext("one");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("two");
        subject.onNext("three");

        assertDidNotReceiveTheDefaultValue(aObserver);
    }

    private void assertDidNotReceiveTheDefaultValue(Observer<String> aObserver) {
        verify(aObserver, Mockito.never()).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testCompleted() {
        BehaviorSubject<String> subject = BehaviorSubject.createWithDefaultValue("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onCompleted();

        assertCompletedObserver(aObserver);
    }
    
    @Test
    public void testCompletedStopsEmittingData() {
        BehaviorSubject<Integer> channel = BehaviorSubject.createWithDefaultValue(2013);
        @SuppressWarnings("unchecked")
        Observer<Object> observerA = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerB = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerC = mock(Observer.class);

        InOrder inOrder = inOrder(observerA, observerB, observerC);

        Subscription a = channel.subscribe(observerA);
        Subscription b = channel.subscribe(observerB);

        inOrder.verify(observerA).onNext(2013);
        inOrder.verify(observerB).onNext(2013);
        
        channel.onNext(42);

        inOrder.verify(observerA).onNext(42);
        inOrder.verify(observerB).onNext(42);

        a.unsubscribe();

        channel.onNext(4711);

        inOrder.verify(observerA, never()).onNext(any());
        inOrder.verify(observerB).onNext(4711);

        channel.onCompleted();

        inOrder.verify(observerA, never()).onCompleted();
        inOrder.verify(observerB).onCompleted();

        Subscription c = channel.subscribe(observerC);

        inOrder.verify(observerC).onCompleted();

        channel.onNext(13);

        inOrder.verifyNoMoreInteractions();
    }

    private void assertCompletedObserver(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testCompletedAfterError() {
        BehaviorSubject<String> subject = BehaviorSubject.createWithDefaultValue("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();

        assertErrorObserver(aObserver);
    }

    private void assertErrorObserver(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onError(testException);
    }

    @Test
    public void testUnsubscribe() {
        UnsubscribeTester.test(
                new Func0<BehaviorSubject<String>>() {
                    @Override
                    public BehaviorSubject<String> call() {
                        return BehaviorSubject.createWithDefaultValue("default");
                    }
                }, new Action1<BehaviorSubject<String>>() {
                    @Override
                    public void call(BehaviorSubject<String> DefaultSubject) {
                        DefaultSubject.onCompleted();
                    }
                }, new Action1<BehaviorSubject<String>>() {
                    @Override
                    public void call(BehaviorSubject<String> DefaultSubject) {
                        DefaultSubject.onError(new Throwable());
                    }
                }, new Action1<BehaviorSubject<String>>() {
                    @Override
                    public void call(BehaviorSubject<String> DefaultSubject) {
                        DefaultSubject.onNext("one");
                    }
                }
                );
    }
}
