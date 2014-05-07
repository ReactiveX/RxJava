/**
 * Copyright 2014 Netflix, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

public class OperatorForEachFutureTest {
    @Test
    public void testSimple() {
        final ExecutorService exec = Executors.newCachedThreadPool();
        
        try {
            Observable<Integer> source = Observable.from(1, 2, 3)
                    .subscribeOn(Schedulers.computation());
            
            final AtomicInteger sum = new AtomicInteger();
            Action1<Integer> add = new Action1<Integer>() {
                @Override
                public void call(Integer t1) {
                    sum.addAndGet(t1);
                }
            };
            
            FutureTask<Void> task = Async.forEachFuture(source, add);
            
            exec.execute(task);
            
            try {
                Void value = task.get(1000, TimeUnit.MILLISECONDS);
                
                assertEquals(null, value);
                
                assertEquals(6, sum.get());
            } catch (TimeoutException ex) {
                fail("Timed out: " + ex);
            } catch (ExecutionException ex) {
                fail("Exception: " + ex);
            } catch (InterruptedException ex) {
                fail("Exception: " + ex);
            }
        } finally {
            exec.shutdown();
        }
    }
    @Test
    public void testSimpleThrowing() {
        
        final ExecutorService exec = Executors.newCachedThreadPool();
        
        try {
            Observable<Integer> source = Observable.<Integer>error(new TestException())
                    .subscribeOn(Schedulers.computation());
            
            final AtomicInteger sum = new AtomicInteger();
            Action1<Integer> add = new Action1<Integer>() {
                @Override
                public void call(Integer t1) {
                    sum.addAndGet(t1);
                }
            };
            
            FutureTask<Void> task = Async.forEachFuture(source, add);
            
            exec.execute(task);
            
            try {
                task.get(1000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ex) {
                fail("Timed out: " + ex);
            } catch (ExecutionException ex) {
                if (!(ex.getCause() instanceof TestException)) {
                    fail("Got different exception: " + ex.getCause());
                }
            } catch (InterruptedException ex) {
                fail("Exception: " + ex);
            }
            
            assertEquals(0, sum.get());
        } finally {
            exec.shutdown();
        }
    }
    
    @Test
    public void testSimpleScheduled() {
        Observable<Integer> source = Observable.from(1, 2, 3)
                .subscribeOn(Schedulers.computation());
        
        final AtomicInteger sum = new AtomicInteger();
        Action1<Integer> add = new Action1<Integer>() {
            @Override
            public void call(Integer t1) {
                sum.addAndGet(t1);
            }
        };
        
        FutureTask<Void> task = Async.forEachFuture(source, add, Schedulers.newThread());
        
        try {
            Void value = task.get(1000, TimeUnit.MILLISECONDS);
            
            assertEquals(null, value);
            
            assertEquals(6, sum.get());
        } catch (TimeoutException ex) {
            fail("Timed out: " + ex);
        } catch (ExecutionException ex) {
            fail("Exception: " + ex);
        } catch (InterruptedException ex) {
            fail("Exception: " + ex);
        }
    }
    @Test
    public void testSimpleScheduledThrowing() {
        
        Observable<Integer> source = Observable.<Integer>error(new TestException())
                .subscribeOn(Schedulers.computation());
        
        final AtomicInteger sum = new AtomicInteger();
        Action1<Integer> add = new Action1<Integer>() {
            @Override
            public void call(Integer t1) {
                sum.addAndGet(t1);
            }
        };
        
        FutureTask<Void> task = Async.forEachFuture(source, add, Schedulers.newThread());
        
        try {
            task.get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            fail("Timed out: " + ex);
        } catch (ExecutionException ex) {
            if (!(ex.getCause() instanceof TestException)) {
                fail("Got different exception: " + ex.getCause());
            }
        } catch (InterruptedException ex) {
            fail("Exception: " + ex);
        }
        
        assertEquals(0, sum.get());
    }
}
