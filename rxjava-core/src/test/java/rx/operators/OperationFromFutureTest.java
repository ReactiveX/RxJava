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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;
import rx.Observable;
import rx.Observer;
import rx.operators.OperationStartFutureTest.CustomException;
import rx.schedulers.Schedulers;
import rx.util.functions.Func0;

public class OperationFromFutureTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testSimple() throws InterruptedException {
        final ExecutorService exec = Executors.newCachedThreadPool();
        
        try {
            final AtomicInteger runCount = new AtomicInteger(0);
            
            Func0<Future<Integer>> func = new Func0<Future<Integer>>() {
                @Override
                public Future<Integer> call() {
                    return exec.submit(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return runCount.incrementAndGet();
                        }
                    });
                }
            };
            
            Observable<Integer> result = Observable.fromFuture(func, Schedulers.threadPoolForComputation());
            
            for (int i = 1; i <= 10; i++) {
                Observer<Object> o = mock(Observer.class);
                InOrder inOrder = inOrder(o);
                
                final CountDownLatch done = new CountDownLatch(1);
                
                result.subscribe(new OperationStartFutureTest.MockHelper<Integer>(o, done));
                
                if (!done.await(1500, TimeUnit.MILLISECONDS)) {
                    fail("Not completed in time!");
                }
                
                inOrder.verify(o).onNext(i);
                inOrder.verify(o).onCompleted();
                verify(o, never()).onError(any(Throwable.class));
                
                assertEquals(i, runCount.get());
            }
            
        } finally {
            exec.shutdown();
        }
    }
    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleThrows() throws InterruptedException {
        final ExecutorService exec = Executors.newCachedThreadPool();
        
        try {
            Func0<Future<Integer>> func = new Func0<Future<Integer>>() {
                @Override
                public Future<Integer> call() {
                    return exec.submit(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            throw new CustomException();
                        }
                    });
                }
            };
            
            Observable<Integer> result = Observable.fromFuture(func, Schedulers.threadPoolForComputation());
            
            for (int i = 1; i <= 10; i++) {
                Observer<Object> o = mock(Observer.class);
                
                final CountDownLatch done = new CountDownLatch(1);
                
                result.subscribe(new OperationStartFutureTest.MockHelper<Integer>(o, done));
                
                if (!done.await(1500, TimeUnit.MILLISECONDS)) {
                    fail("Not completed in time!");
                }
                
                verify(o, times(1)).onError(any(CustomException.class));
                verify(o, never()).onNext(any());
                verify(o, never()).onCompleted();
            }
            
        } finally {
            exec.shutdown();
        }
    }
}
