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
package rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OnSubscribeToObservableFutureTest {

    @Test
    public void testSuccess() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        Object value = new Object();
        when(future.get()).thenReturn(value);
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Subscription sub = Observable.from(future).subscribe(new TestObserver<Object>(o));
        sub.unsubscribe();

        verify(o, times(1)).onNext(value);
        verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
        verify(future, times(1)).cancel(true);
    }

    @Test
    public void testFailure() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        RuntimeException e = new RuntimeException();
        when(future.get()).thenThrow(e);
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Subscription sub = Observable.from(future).subscribe(new TestObserver<Object>(o));
        sub.unsubscribe();

        verify(o, never()).onNext(null);
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(e);
        verify(future, times(1)).cancel(true);
    }

    @Test
    public void testCancelledBeforeSubscribe() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        CancellationException e = new CancellationException("unit test synthetic cancellation");
        when(future.get()).thenThrow(e);
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>(o);
        testSubscriber.unsubscribe();
        Subscription sub = Observable.from(future).subscribe(testSubscriber);
        assertEquals(0, testSubscriber.getOnErrorEvents().size());
        assertEquals(0, testSubscriber.getOnCompletedEvents().size());
    }

    @Test
    public void testCancellationDuringFutureGet() throws Exception {
        Future<Object> future = new Future<Object>() {
            private AtomicBoolean isCancelled = new AtomicBoolean(false);
            private AtomicBoolean isDone = new AtomicBoolean(false);

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                isCancelled.compareAndSet(false, true);
                return true;
            }

            @Override
            public boolean isCancelled() {
                return isCancelled.get();
            }

            @Override
            public boolean isDone() {
                return isCancelled() || isDone.get();
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                Thread.sleep(500);
                isDone.compareAndSet(false, true);
                return "foo";
            }

            @Override
            public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return get();
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>(o);
        Observable<Object> futureObservable = Observable.from(future);
        Subscription sub = futureObservable.subscribeOn(Schedulers.computation()).subscribe(testSubscriber);
        sub.unsubscribe();
        assertEquals(0, testSubscriber.getOnErrorEvents().size());
        assertEquals(0, testSubscriber.getOnCompletedEvents().size());
        assertEquals(0, testSubscriber.getOnNextEvents().size());
    }
}
