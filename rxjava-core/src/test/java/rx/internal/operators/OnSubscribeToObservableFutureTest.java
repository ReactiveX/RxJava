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

import org.junit.Test;
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OnSubscribeToObservableFutureTest {

    @Test
    public void testSuccess() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        Object value = new Object();
        when(future.get()).thenReturn(value);
        when(future.isDone()).thenReturn(true);
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Subscription sub = Observable.from(future).subscribe(new TestObserver<Object>(o));
        sub.unsubscribe();

        verify(o, times(1)).onNext(value);
        verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFailure() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        RuntimeException e = new RuntimeException();
        when(future.isDone()).thenReturn(true);
        when(future.get()).thenThrow(e);
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Subscription sub = Observable.from(future).subscribe(new TestObserver<Object>(o));
        sub.unsubscribe();

        verify(o, never()).onNext(null);
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(e);
    }

    @Test
    public void testCancelledBeforeSubscribe() throws Exception {
        Future<Object> future = mock(Future.class);

        CancellationException e = new CancellationException("unit test synthetic cancellation");
        when(future.isCancelled()).thenReturn(true);
        when(future.isDone()).thenReturn(true);
        when(future.get()).thenThrow(e);
        Observer<Object> o = mock(Observer.class);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>(o);
        testSubscriber.unsubscribe();
        Observable.from(future).subscribe(testSubscriber);

        verify(o, never()).onNext(null);
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(e);
    }

    @Test
    public void testCancellationDuringFutureGet() throws Exception {
        TestableFuture future = new TestableFuture();

        Observer<Object> o = mock(Observer.class);
        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>(o);

        Observable<Object> futureObservable = Observable.from(future);
        // Must subscribe on the current thread, otherwise we will never get subscription executed.
        Subscription sub = futureObservable.subscribe(testSubscriber);
        sub.unsubscribe();

        assertTrue(future.awaitIsDone(200)); // Give future polling task time to execute

        assertTrue(future.isCancelled());
        assertEquals(0, testSubscriber.getOnErrorEvents().size());
        assertEquals(0, testSubscriber.getOnCompletedEvents().size());
        assertEquals(0, testSubscriber.getOnNextEvents().size());
    }

    @Test
    public void testProlongedFutureComputation() throws Exception {
        TestableFuture future = new TestableFuture();

        Observer<Object> o = mock(Observer.class);
        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>(o);

        Observable<Object> futureObservable = Observable.from(future);
        futureObservable.subscribeOn(Schedulers.computation()).subscribe(testSubscriber);

        Thread.sleep(10); // Give future polling task enough time to reschedule itself.
        future.complete();

        testSubscriber.awaitTerminalEvent();

        List<Notification<Object>> events = testSubscriber.getOnCompletedEvents();
        assertTrue(testSubscriber.getOnCompletedEvents().size() == 1);
    }

    @Test
    public void testFutureTimeout() throws Exception {
        TestableFuture future = new TestableFuture();

        Observable<Object> futureObservable = Observable.from(future, 50, TimeUnit.MILLISECONDS);

        Notification<Object> result = Observable.amb(futureObservable, Observable.timer(100, TimeUnit.MILLISECONDS)).materialize().toBlocking().first();

        assertTrue("Observable error expected", result.isOnError());
        assertTrue("Expected TimeoutException", result.getThrowable() instanceof TimeoutException);
    }

    private static class TestableFuture implements Future {
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);
        private final AtomicBoolean isDone = new AtomicBoolean(false);
        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            isCancelled.compareAndSet(false, true);
            latch.countDown();
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
            if (isCancelled()) {
                throw new RuntimeException("future already canceled");
            }
            if (!isDone()) {
                throw new RuntimeException("future.get called too soon");
            }
            return "foo";
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }

        public boolean complete() {
            latch.countDown();
            return isDone.compareAndSet(false, true);
        }

        public boolean awaitIsDone(int timeout) throws InterruptedException {
            return latch.await(timeout, TimeUnit.MILLISECONDS);
        }
    }
}
