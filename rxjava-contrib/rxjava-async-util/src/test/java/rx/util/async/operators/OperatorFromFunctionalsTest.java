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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.observers.TestObserver;
import rx.schedulers.TestScheduler;
import rx.util.async.Async;

public class OperatorFromFunctionalsTest {
    TestScheduler scheduler;
    @Before
    public void before() {
        scheduler = new TestScheduler();
    }
    private void testRunShouldThrow(Observable<Integer> source, Class<? extends Throwable> exception) {
        for (int i = 0; i < 3; i++) {
            
            @SuppressWarnings("unchecked")
            Observer<Object> observer = mock(Observer.class);
            source.subscribe(new TestObserver<Object>(observer));

            InOrder inOrder = inOrder(observer);

            inOrder.verify(observer, never()).onError(any(Throwable.class));

            scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

            inOrder.verify(observer, times(1)).onError(any(exception));
            verify(observer, never()).onNext(any());
            verify(observer, never()).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }
    }
    @Test
    public void testFromAction() {
        final AtomicInteger value = new AtomicInteger();
        
        Action0 action = new Action0() {
            @Override
            public void call() {
                value.set(2);
            }
        };
        
        Observable<Integer> source = Async.fromAction(action, 1, scheduler);
        
        for (int i = 0; i < 3; i++) {
            
            value.set(0);
            
            @SuppressWarnings("unchecked")
            Observer<Object> observer = mock(Observer.class);
            source.subscribe(new TestObserver<Object>(observer));

            InOrder inOrder = inOrder(observer);

            inOrder.verify(observer, never()).onNext(any());
            inOrder.verify(observer, never()).onCompleted();

            scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

            inOrder.verify(observer, times(1)).onNext(1);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer, never()).onError(any(Throwable.class));

            Assert.assertEquals(2, value.get());
        }
    }
    @Test
    public void testFromActionThrows() {
        Action0 action = new Action0() {
            @Override
            public void call() {
                throw new RuntimeException("Forced failure!");
            }
        };
        
        Observable<Integer> source = Async.fromAction(action, 1, scheduler);
        
        testRunShouldThrow(source, RuntimeException.class);
    }

    @Test
    public void testFromRunnable() {
        final AtomicInteger value = new AtomicInteger();
        
        Runnable action = new Runnable() {
            @Override
            public void run() {
                value.set(2);
            }
        };
        
        Observable<Integer> source = Async.fromRunnable(action, 1, scheduler);
        
        for (int i = 0; i < 3; i++) {
            
            value.set(0);
            
            @SuppressWarnings("unchecked")
            Observer<Object> observer = mock(Observer.class);
            source.subscribe(new TestObserver<Object>(observer));

            InOrder inOrder = inOrder(observer);

            inOrder.verify(observer, never()).onNext(any());
            inOrder.verify(observer, never()).onCompleted();

            scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

            inOrder.verify(observer, times(1)).onNext(1);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer, never()).onError(any(Throwable.class));

            Assert.assertEquals(2, value.get());
        }
    }
    @Test
    public void testFromRunnableThrows() {
        Runnable action = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("Forced failure!");
            }
        };
        
        Observable<Integer> source = Async.fromRunnable(action, 1, scheduler);
        
        testRunShouldThrow(source, RuntimeException.class);
    }
    @Test
    public void testFromCallable() {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 1;
            }
        };
        
        Observable<Integer> source = Async.fromCallable(callable, scheduler);
        
        for (int i = 0; i < 3; i++) {
            
            @SuppressWarnings("unchecked")
            Observer<Object> observer = mock(Observer.class);
            source.subscribe(new TestObserver<Object>(observer));

            InOrder inOrder = inOrder(observer);

            inOrder.verify(observer, never()).onNext(any());
            inOrder.verify(observer, never()).onCompleted();

            scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

            inOrder.verify(observer, times(1)).onNext(1);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer, never()).onError(any(Throwable.class));
        }
    }
    
    @Test
    public void testFromCallableThrows() {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new IOException("Forced failure!");
            }
        };
        
        Observable<Integer> source = Async.fromCallable(callable, scheduler);
        
        testRunShouldThrow(source, IOException.class);
    }
}
