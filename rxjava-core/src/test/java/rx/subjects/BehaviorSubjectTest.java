/**
 * Copyright 2013 Netflix, Inc.
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

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testThatObserverDoesNotReceiveDefaultValueIfSomethingWasPublished() {
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
    public void testCompleted() {
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
    public void testCompletedAfterError() {
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
    }

    @Test
    public void testUnsubscribe() {
        UnsubscribeTester.test(
                new Func0<BehaviorSubject<String>>() {
                    @Override
                    public BehaviorSubject<String> call() {
                        return BehaviorSubject.create("default");
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
    @Test
    public void testFirstErrorOnly() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create(0);
        Observer<Object> observer = mock(Observer.class);
        
        subject.subscribe(observer);
        
        RuntimeException ex = new RuntimeException("Forced failure");
        
        subject.onError(ex);

        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(ex);
        verify(observer, never()).onCompleted();
        
        RuntimeException ex2 = new RuntimeException("Forced failure 2");
        
        subject.onError(ex2);

        Observer<Object> observer2 = mock(Observer.class);
        
        subject.subscribe(observer2);
        verify(observer2, never()).onNext(any());
        verify(observer2, times(1)).onError(ex);
        verify(observer2, never()).onError(ex2);
        verify(observer2, never()).onCompleted();
    }
}
