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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;
import rx.util.functions.Func0;

public class AsyncSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testNeverCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(aObserver, Mockito.never()).onNext(anyString());
        verify(aObserver, Mockito.never()).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testError() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);
        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onCompleted();

        verify(aObserver, Mockito.never()).onNext(anyString());
        verify(aObserver, times(1)).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testUnsubscribeBeforeCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        Subscription subscription = subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");

        subscription.unsubscribe();

        verify(aObserver, Mockito.never()).onNext(anyString());
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, Mockito.never()).onCompleted();

        subject.onNext("three");
        subject.onCompleted();

        verify(aObserver, Mockito.never()).onNext(anyString());
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testUnsubscribe() {
        UnsubscribeTester.test(
                new Func0<AsyncSubject<Object>>() {
                    @Override
                    public AsyncSubject<Object> call() {
                        return AsyncSubject.create();
                    }
                }, new Action1<AsyncSubject<Object>>() {
                    @Override
                    public void call(AsyncSubject<Object> DefaultSubject) {
                        DefaultSubject.onCompleted();
                    }
                }, new Action1<AsyncSubject<Object>>() {
                    @Override
                    public void call(AsyncSubject<Object> DefaultSubject) {
                        DefaultSubject.onError(new Throwable());
                    }
                },
                null
                );
    }

    @Test
    public void testEmptySubjectCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onCompleted();

        InOrder inOrder = inOrder(aObserver);
        inOrder.verify(aObserver, never()).onNext(null);
        inOrder.verify(aObserver, never()).onNext(any(String.class));
        inOrder.verify(aObserver, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
}
