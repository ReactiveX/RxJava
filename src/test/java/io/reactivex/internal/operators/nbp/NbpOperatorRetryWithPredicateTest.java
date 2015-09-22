/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorRetryWithPredicateTest {
    BiPredicate<Integer, Throwable> retryTwice = new BiPredicate<Integer, Throwable>() {
        @Override
        public boolean test(Integer t1, Throwable t2) {
            return t1 <= 2;
        }
    };
    BiPredicate<Integer, Throwable> retry5 = new BiPredicate<Integer, Throwable>() {
        @Override
        public boolean test(Integer t1, Throwable t2) {
            return t1 <= 5;
        }
    };
    BiPredicate<Integer, Throwable> retryOnTestException = new BiPredicate<Integer, Throwable>() {
        @Override
        public boolean test(Integer t1, Throwable t2) {
            return t2 instanceof IOException;
        }
    };
    @Test
    public void testWithNothingToRetry() {
        NbpObservable<Integer> source = NbpObservable.range(0, 3);
        
        NbpSubscriber<Integer> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);
        
        source.retry(retryTwice).subscribe(o);
        
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testRetryTwice() {
        NbpObservable<Integer> source = NbpObservable.create(new NbpOnSubscribe<Integer>() {
            int count;
            @Override
            public void accept(NbpSubscriber<? super Integer> t1) {
                t1.onSubscribe(EmptyDisposable.INSTANCE);
                count++;
                t1.onNext(0);
                t1.onNext(1);
                if (count == 1) {
                    t1.onError(new TestException());
                    return;
                }
                t1.onNext(2);
                t1.onNext(3);
                t1.onComplete();
            }
        });
        
        @SuppressWarnings("unchecked")
        NbpObserver<Integer> o = mock(NbpObserver.class);
        InOrder inOrder = inOrder(o);
        
        source.retry(retryTwice).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
        
    }
    @Test
    public void testRetryTwiceAndGiveUp() {
        NbpObservable<Integer> source = NbpObservable.create(new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(NbpSubscriber<? super Integer> t1) {
                t1.onSubscribe(EmptyDisposable.INSTANCE);
                t1.onNext(0);
                t1.onNext(1);
                t1.onError(new TestException());
            }
        });
        
        @SuppressWarnings("unchecked")
        NbpObserver<Integer> o = mock(NbpObserver.class);
        InOrder inOrder = inOrder(o);
        
        source.retry(retryTwice).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
        
    }
    @Test
    public void testRetryOnSpecificException() {
        NbpObservable<Integer> source = NbpObservable.create(new NbpOnSubscribe<Integer>() {
            int count;
            @Override
            public void accept(NbpSubscriber<? super Integer> t1) {
                t1.onSubscribe(EmptyDisposable.INSTANCE);
                count++;
                t1.onNext(0);
                t1.onNext(1);
                if (count == 1) {
                    t1.onError(new IOException());
                    return;
                }
                t1.onNext(2);
                t1.onNext(3);
                t1.onComplete();
            }
        });
        
        @SuppressWarnings("unchecked")
        NbpObserver<Integer> o = mock(NbpObserver.class);
        InOrder inOrder = inOrder(o);
        
        source.retry(retryOnTestException).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testRetryOnSpecificExceptionAndNotOther() {
        final IOException ioe = new IOException();
        final TestException te = new TestException();
        NbpObservable<Integer> source = NbpObservable.create(new NbpOnSubscribe<Integer>() {
            int count;
            @Override
            public void accept(NbpSubscriber<? super Integer> t1) {
                t1.onSubscribe(EmptyDisposable.INSTANCE);
                count++;
                t1.onNext(0);
                t1.onNext(1);
                if (count == 1) {
                    t1.onError(ioe);
                    return;
                }
                t1.onNext(2);
                t1.onNext(3);
                t1.onError(te);
            }
        });
        
        @SuppressWarnings("unchecked")
        NbpObserver<Integer> o = mock(NbpObserver.class);
        InOrder inOrder = inOrder(o);
        
        source.retry(retryOnTestException).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onError(te);
        verify(o, never()).onError(ioe);
        verify(o, never()).onComplete();
    }
    
    @Test
    public void testUnsubscribeFromRetry() {
        NbpPublishSubject<Integer> subject = NbpPublishSubject.create();
        final AtomicInteger count = new AtomicInteger(0);
        Disposable sub = subject.retry(retryTwice).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer n) {
                count.incrementAndGet();
            }
        });
        subject.onNext(1);
        sub.dispose();
        subject.onNext(2);
        assertEquals(1, count.get());
    }
    
    @Test(timeout = 10000)
    public void testUnsubscribeAfterError() {

        NbpSubscriber<Long> NbpObserver = TestHelper.mockNbpSubscriber();

        // NbpObservable that always fails after 100ms
        NbpOperatorRetryTest.SlowObservable so = new NbpOperatorRetryTest.SlowObservable(100, 0);
        NbpObservable<Long> o = NbpObservable
                .create(so)
                .retry(retry5);

        NbpOperatorRetryTest.AsyncObserver<Long> async = new NbpOperatorRetryTest.AsyncObserver<>(NbpObserver);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(NbpObserver);
        // Should fail once
        inOrder.verify(NbpObserver, times(1)).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
        assertEquals("Only 1 active subscription", 1, so.maxActive.get());
    }

    @Test(timeout = 10000)
    public void testTimeoutWithRetry() {

        NbpSubscriber<Long> NbpObserver = TestHelper.mockNbpSubscriber();

        // NbpObservable that sends every 100ms (timeout fails instead)
        NbpOperatorRetryTest.SlowObservable so = new NbpOperatorRetryTest.SlowObservable(100, 10);
        NbpObservable<Long> o = NbpObservable
                .create(so)
                .timeout(80, TimeUnit.MILLISECONDS)
                .retry(retry5);

        NbpOperatorRetryTest.AsyncObserver<Long> async = new NbpOperatorRetryTest.AsyncObserver<>(NbpObserver);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(NbpObserver);
        // Should fail once
        inOrder.verify(NbpObserver, times(1)).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, never()).onComplete();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
    }
    
    @Test
    public void testIssue2826() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        final RuntimeException e = new RuntimeException("You shall not pass");
        final AtomicInteger c = new AtomicInteger();
        NbpObservable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                c.incrementAndGet();
                throw e;
            }
        }).retry(retry5).subscribe(ts);

        ts.assertTerminated();
        assertEquals(6, c.get());
        assertEquals(Collections.singletonList(e), ts.errors());
    }
    @Test
    public void testJustAndRetry() throws Exception {
        final AtomicBoolean throwException = new AtomicBoolean(true);
        int value = NbpObservable.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                if (throwException.compareAndSet(true, false)) {
                    throw new TestException();
                }
                return t1;
            }
        }).retry(1).toBlocking().single();

        assertEquals(1, value);
    }
    
    @Test
    public void testIssue3008RetryWithPredicate() {
        final List<Long> list = new CopyOnWriteArrayList<>();
        final AtomicBoolean isFirst = new AtomicBoolean(true);
        NbpObservable.<Long> just(1L, 2L, 3L).map(new Function<Long, Long>(){
            @Override
            public Long apply(Long x) {
                System.out.println("map " + x);
                if (x == 2 && isFirst.getAndSet(false)) {
                    throw new RuntimeException("retryable error");
                }
                return x;
            }})
        .retry(new BiPredicate<Integer, Throwable>() {
            @Override
            public boolean test(Integer t1, Throwable t2) {
                return true;
            }})
        .forEach(new Consumer<Long>() {

            @Override
            public void accept(Long t) {
                System.out.println(t);
                list.add(t);
            }});
        assertEquals(Arrays.asList(1L,1L,2L,3L), list);
    }
    
    @Test
    public void testIssue3008RetryInfinite() {
        final List<Long> list = new CopyOnWriteArrayList<>();
        final AtomicBoolean isFirst = new AtomicBoolean(true);
        NbpObservable.<Long> just(1L, 2L, 3L).map(new Function<Long, Long>(){
            @Override
            public Long apply(Long x) {
                System.out.println("map " + x);
                if (x == 2 && isFirst.getAndSet(false)) {
                    throw new RuntimeException("retryable error");
                }
                return x;
            }})
        .retry()
        .forEach(new Consumer<Long>() {

            @Override
            public void accept(Long t) {
                System.out.println(t);
                list.add(t);
            }});
        assertEquals(Arrays.asList(1L,1L,2L,3L), list);
    }
}