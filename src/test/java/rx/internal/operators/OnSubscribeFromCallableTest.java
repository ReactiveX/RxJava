/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;
import static rx.schedulers.Schedulers.computation;

public class OnSubscribeFromCallableTest {

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotInvokeFuncUntilSubscription() throws Exception {
        Callable<Object> func = mock(Callable.class);

        when(func.call()).thenReturn(new Object());

        Observable<Object> fromCallableObservable = Observable.fromCallable(func);

        verifyZeroInteractions(func);

        fromCallableObservable.subscribe();

        verify(func).call();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCallOnNextAndOnCompleted() throws Exception {
        Callable<String> func = mock(Callable.class);

        when(func.call()).thenReturn("test_value");

        Observable<String> fromCallableObservable = Observable.fromCallable(func);

        Observer<String> observer = mock(Observer.class);

        fromCallableObservable.subscribe(observer);

        verify(observer).onNext("test_value");
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCallOnError() throws Exception {
        Callable<Object> func = mock(Callable.class);

        Throwable throwable = new IllegalStateException("Test exception");
        when(func.call()).thenThrow(throwable);

        Observable<Object> fromCallableObservable = Observable.fromCallable(func);

        Observer<Object> observer = mock(Observer.class);

        fromCallableObservable.subscribe(observer);

        verify(observer, never()).onNext(anyObject());
        verify(observer, never()).onCompleted();
        verify(observer).onError(throwable);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotDeliverResultIfSubscriberUnsubscribedBeforeEmission() throws Exception {
        Callable<String> func = mock(Callable.class);

        final CountDownLatch funcLatch = new CountDownLatch(1);
        final CountDownLatch observerLatch = new CountDownLatch(1);

        when(func.call()).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                observerLatch.countDown();

                try {
                    funcLatch.await();
                } catch (InterruptedException e) {
                    // It's okay, unsubscription causes Thread interruption

                    // Restoring interruption status of the Thread
                    Thread.currentThread().interrupt();
                }

                return "should_not_be_delivered";
            }
        });

        Observable<String> fromCallableObservable = Observable.fromCallable(func);

        Observer<String> observer = mock(Observer.class);

        Subscription subscription = fromCallableObservable
                .subscribeOn(computation())
                .subscribe(observer);

        // Wait until func will be invoked
        observerLatch.await();

        // Unsubscribing before emission
        subscription.unsubscribe();

        // Emitting result
        funcLatch.countDown();

        // func must be invoked
        verify(func).call();

        // Observer must not be notified at all
        verifyZeroInteractions(observer);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAllowToThrowCheckedException() {
        final Exception checkedException = new Exception("test exception");

        Observable<Object> fromCallableObservable = Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw checkedException;
            }
        });

        Observer<Object> observer = mock(Observer.class);

        fromCallableObservable.subscribe(observer);

        verify(observer).onError(checkedException);
        verifyNoMoreInteractions(observer);
    }
}