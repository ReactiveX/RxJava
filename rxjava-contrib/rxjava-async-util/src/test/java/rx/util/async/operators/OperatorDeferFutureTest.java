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
package rx.util.async.operators;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

public class OperatorDeferFutureTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testSimple() throws InterruptedException {
        final ExecutorService exec = Executors.newCachedThreadPool();
        try {
            final CountDownLatch ready = new CountDownLatch(1);

            Func0<Future<Observable<Integer>>> func = new Func0<Future<Observable<Integer>>>() {
                @Override
                public Future<Observable<Integer>> call() {
                    return exec.submit(new Callable<Observable<Integer>>() {
                        @Override
                        public Observable<Integer> call() throws Exception {
                            if (!ready.await(1000, TimeUnit.MILLISECONDS)) {
                                throw new IllegalStateException("Not started in time");
                            }
                            return Observable.from(1);
                        }
                    });
                }
            };
            
            Observable<Integer> result = Async.deferFuture(func, Schedulers.computation());

            final Observer<Integer> observer = mock(Observer.class);

            final CountDownLatch done = new CountDownLatch(1);

            result.subscribe(new OperatorStartFutureTest.MockHelper<Integer>(observer, done));

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
        Func0<Future<Observable<Integer>>> func = new Func0<Future<Observable<Integer>>>() {

            @Override
            public Future<Observable<Integer>> call() {
                throw new TestException();
            }
        };
        
        Observable<Integer> result = Async.deferFuture(func);
        
        final Observer<Object> observer = mock(Observer.class);
        result.subscribe(observer);
        
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer).onError(any(TestException.class));
    }
}