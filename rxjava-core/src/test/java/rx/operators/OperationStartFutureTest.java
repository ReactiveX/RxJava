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
package rx.operators;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.BooleanSubscription;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

public class OperationStartFutureTest {
    /** Custom exception to distinguish from any other RuntimeException. */
    static class CustomException extends RuntimeException {}
    /** 
     * Forwards the events to the underlying observer and counts down the latch
     * on terminal conditions.
     * @param <T> 
     */
    static class MockHelper<T> implements Observer<T> {
        final Observer<? super T> observer;
        final CountDownLatch latch;

        public MockHelper(Observer<? super T> observer, CountDownLatch latch) {
            this.observer = observer;
            this.latch = latch;
        }

        @Override
        public void onNext(T args) {
            try {
                observer.onNext(args);
            } catch (Throwable t) {
                onError(t);
            }
        }

        @Override
        public void onError(Throwable e) {
            try {
                observer.onError(e);
            } finally {
                latch.countDown();
            }
        }


        @Override
        public void onCompleted() {
            try {
                observer.onCompleted();
            } finally {
                latch.countDown();
            }
        }
        
    }
    @Test
    @SuppressWarnings("unchecked")
    public void testSimple() throws InterruptedException {
        final ExecutorService exec = Executors.newCachedThreadPool();
        try {
            final CountDownLatch ready = new CountDownLatch(1);

            Func0<Future<Integer>> func = new Func0<Future<Integer>>() {

                @Override
                public Future<Integer> call() {
                    return exec.submit(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            if (!ready.await(1000, TimeUnit.MILLISECONDS)) {
                                throw new IllegalStateException("Not started in time");
                            }
                            return 1;
                        }
                    });
                }
            };

            Observable<Integer> result = Observable.startFuture(func, Schedulers.threadPoolForComputation());

            final Observer<Integer> observer = mock(Observer.class);

            final CountDownLatch done = new CountDownLatch(1);

            result.subscribe(new MockHelper<Integer>(observer, done));

            ready.countDown();

            if (!done.await(1000, TimeUnit.MILLISECONDS)) {
                fail("Not completed in time!");
            }

            InOrder inOrder = inOrder(observer);

            inOrder.verify(observer).onNext(1);
            inOrder.verify(observer).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
        } finally {        
            exec.shutdown();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleFactoryThrows() {
        Func0<Future<Integer>> func = new Func0<Future<Integer>>() {

            @Override
            public Future<Integer> call() {
                throw new CustomException();
            }
        };
        
        Observable<Integer> result = Observable.startFuture(func);
        
        final Observer<Object> observer = mock(Observer.class);
        result.subscribe(observer);
        
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer).onError(any(CustomException.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleCancellableFactoryThrows() {
        Func1<BooleanSubscription, Future<Integer>> func = new Func1<BooleanSubscription, Future<Integer>>() {

            @Override
            public Future<Integer> call(BooleanSubscription token) {
                throw new CustomException();
            }
        };
        
        Observable<Integer> result = Observable.startCancellableFuture(func);
        
        final Observer<Object> observer = mock(Observer.class);
        result.subscribe(observer);
        
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer).onError(any(CustomException.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testStartCancellable() throws InterruptedException {
        final ExecutorService exec = Executors.newCachedThreadPool();
        try {
            final CountDownLatch ready = new CountDownLatch(1);

            Func1<BooleanSubscription, Future<Integer>> func = new Func1<BooleanSubscription, Future<Integer>>() {

                @Override
                public Future<Integer> call(BooleanSubscription token) {
                    return exec.submit(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            if (!ready.await(1000, TimeUnit.MILLISECONDS)) {
                                throw new IllegalStateException("Not started in time");
                            }
                            return 1;
                        }
                    });
                }
            };

            Observable<Integer> result = Observable.startCancellableFuture(func, Schedulers.threadPoolForComputation());

            final Observer<Integer> observer = mock(Observer.class);

            final CountDownLatch done = new CountDownLatch(1);

            result.subscribe(new MockHelper<Integer>(observer, done));

            ready.countDown();

            if (!done.await(1000, TimeUnit.MILLISECONDS)) {
                fail("Not completed in time!");
            }

            InOrder inOrder = inOrder(observer);

            inOrder.verify(observer).onNext(1);
            inOrder.verify(observer).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
        } finally {        
            exec.shutdown();
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testStartCancellableAndCancel() throws InterruptedException {
        final ExecutorService exec = Executors.newCachedThreadPool();
        try {
            final CountDownLatch ready = new CountDownLatch(1);
            final CountDownLatch ready2 = new CountDownLatch(1);

            Func1<BooleanSubscription, Future<Integer>> func = new Func1<BooleanSubscription, Future<Integer>>() {

                @Override
                public Future<Integer> call(final BooleanSubscription token) {
                    return exec.submit(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            ready.countDown();
                            
                            if (!ready2.await(1000, TimeUnit.MILLISECONDS)) {
                                throw new IllegalStateException("Not started in time");
                            }
                            if (token.isUnsubscribed()) {
                                throw new CancellationException();
                            }
                            
                            return 1;
                        }
                    });
                }
            };

            Observable<Integer> result = Observable.startCancellableFuture(func, Schedulers.threadPoolForComputation());

            final Observer<Object> observer = mock(Observer.class);

            final CountDownLatch done = new CountDownLatch(1);

            Subscription s = result.subscribe(new MockHelper<Object>(observer, done));

            if (!ready.await(1000, TimeUnit.MILLISECONDS)) {
                fail("Not entered the call() in time!");
            }
            
            s.unsubscribe();
            
            // resume call
            ready2.countDown();

            if (!done.await(1000, TimeUnit.MILLISECONDS)) {
                fail("Not completed in time!");
            }

            verify(observer, never()).onNext(any());
            verify(observer, never()).onCompleted();
            verify(observer, times(1)).onError(any(CancellationException.class));
            
        } finally {        
            exec.shutdown();
        }
    }
}
