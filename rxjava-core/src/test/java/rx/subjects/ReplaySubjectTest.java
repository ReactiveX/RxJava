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

public class ReplaySubjectTest {

    private final Throwable testException = new Throwable();

    @SuppressWarnings("unchecked")
    @Test
    public void testCompleted() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> o1 = mock(Observer.class);
        subject.subscribe(o1);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        subject.onNext("four");
        subject.onCompleted();
        subject.onError(new Throwable());

        assertCompletedObserver(o1);

        // assert that subscribing a 2nd time gets the same data
        Observer<String> o2 = mock(Observer.class);
        subject.subscribe(o2);
        assertCompletedObserver(o2);
    }

    private void assertCompletedObserver(Observer<String> aObserver) {
        InOrder inOrder = inOrder(aObserver);

        inOrder.verify(aObserver, times(1)).onNext("one");
        inOrder.verify(aObserver, times(1)).onNext("two");
        inOrder.verify(aObserver, times(1)).onNext("three");
        inOrder.verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        inOrder.verify(aObserver, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testError() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);

        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onCompleted();

        assertErrorObserver(aObserver);

        aObserver = mock(Observer.class);
        subject.subscribe(aObserver);
        assertErrorObserver(aObserver);
    }

    private void assertErrorObserver(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, times(1)).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSubscribeMidSequence() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");

        assertObservedUntilTwo(aObserver);

        Observer<String> anotherObserver = mock(Observer.class);
        subject.subscribe(anotherObserver);
        assertObservedUntilTwo(anotherObserver);

        subject.onNext("three");
        subject.onCompleted();

        assertCompletedObserver(aObserver);
        assertCompletedObserver(anotherObserver);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnsubscribeFirstObserver() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> aObserver = mock(Observer.class);
        Subscription subscription = subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");

        subscription.unsubscribe();
        assertObservedUntilTwo(aObserver);

        Observer<String> anotherObserver = mock(Observer.class);
        subject.subscribe(anotherObserver);
        assertObservedUntilTwo(anotherObserver);

        subject.onNext("three");
        subject.onCompleted();

        assertObservedUntilTwo(aObserver);
        assertCompletedObserver(anotherObserver);
    }

    private void assertObservedUntilTwo(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testUnsubscribe() {
        UnsubscribeTester.test(
                new Func0<ReplaySubject<Object>>() {
                    @Override
                    public ReplaySubject<Object> call() {
                        return ReplaySubject.create();
                    }
                }, new Action1<ReplaySubject<Object>>() {
                    @Override
                    public void call(ReplaySubject<Object> repeatSubject) {
                        repeatSubject.onCompleted();
                    }
                }, new Action1<ReplaySubject<Object>>() {
                    @Override
                    public void call(ReplaySubject<Object> repeatSubject) {
                        repeatSubject.onError(new Throwable());
                    }
                }, new Action1<ReplaySubject<Object>>() {
                    @Override
                    public void call(ReplaySubject<Object> repeatSubject) {
                        repeatSubject.onNext("one");
                    }
                }
                );
    }
}
