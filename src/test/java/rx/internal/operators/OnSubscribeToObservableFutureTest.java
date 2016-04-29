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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.*;
import rx.observers.*;
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
        Observable.from(future).subscribe(testSubscriber);
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
    
    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        
        FutureTask<Integer> f = new FutureTask<Integer>(new Runnable() {
            @Override
            public void run() {
                
            }
        }, 1);
        
        f.run();
        
        Observable.from(f).subscribe(ts);
        
        ts.assertNoValues();
        
        ts.requestMore(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
}
