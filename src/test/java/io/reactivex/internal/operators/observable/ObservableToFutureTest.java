/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import static org.mockito.Mockito.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.*;

public class ObservableToFutureTest {

    @Test
    public void testSuccess() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        Object value = new Object();
        when(future.get()).thenReturn(value);

        Observer<Object> o = TestHelper.mockObserver();

        TestObserver<Object> ts = new TestObserver<Object>(o);

        Observable.fromFuture(future).subscribe(ts);

        ts.dispose();

        verify(o, times(1)).onNext(value);
        verify(o, times(1)).onComplete();
        verify(o, never()).onError(any(Throwable.class));
        verify(future, never()).cancel(true);
    }

    @Test
    public void testSuccessOperatesOnSuppliedScheduler() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        Object value = new Object();
        when(future.get()).thenReturn(value);

        Observer<Object> o = TestHelper.mockObserver();

        TestScheduler scheduler = new TestScheduler();
        TestObserver<Object> ts = new TestObserver<Object>(o);

        Observable.fromFuture(future, scheduler).subscribe(ts);

        verify(o, never()).onNext(value);

        scheduler.triggerActions();

        verify(o, times(1)).onNext(value);
    }

    @Test
    public void testFailure() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        RuntimeException e = new RuntimeException();
        when(future.get()).thenThrow(e);

        Observer<Object> o = TestHelper.mockObserver();

        TestObserver<Object> ts = new TestObserver<Object>(o);

        Observable.fromFuture(future).subscribe(ts);

        ts.dispose();

        verify(o, never()).onNext(null);
        verify(o, never()).onComplete();
        verify(o, times(1)).onError(e);
        verify(future, never()).cancel(true);
    }

    @Test
    public void testCancelledBeforeSubscribe() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        CancellationException e = new CancellationException("unit test synthetic cancellation");
        when(future.get()).thenThrow(e);

        Observer<Object> o = TestHelper.mockObserver();

        TestObserver<Object> ts = new TestObserver<Object>(o);
        ts.dispose();

        Observable.fromFuture(future).subscribe(ts);

        ts.assertNoErrors();
        ts.assertNotComplete();
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

        Observer<Object> o = TestHelper.mockObserver();

        TestObserver<Object> ts = new TestObserver<Object>(o);
        Observable<Object> futureObservable = Observable.fromFuture(future);

        futureObservable.subscribeOn(Schedulers.computation()).subscribe(ts);

        Thread.sleep(100);

        ts.dispose();

        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotComplete();
    }
}
