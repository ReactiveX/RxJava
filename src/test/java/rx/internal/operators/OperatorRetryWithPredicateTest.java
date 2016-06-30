/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.TestException;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorRetryWithPredicateTest {
    Func2<Integer, Throwable, Boolean> retryTwice = new Func2<Integer, Throwable, Boolean>() {
        @Override
        public Boolean call(Integer t1, Throwable t2) {
            return t1 <= 2;
        }
    };
    Func2<Integer, Throwable, Boolean> retry5 = new Func2<Integer, Throwable, Boolean>() {
        @Override
        public Boolean call(Integer t1, Throwable t2) {
            return t1 <= 5;
        }
    };
    Func2<Integer, Throwable, Boolean> retryOnTestException = new Func2<Integer, Throwable, Boolean>() {
        @Override
        public Boolean call(Integer t1, Throwable t2) {
            return t2 instanceof IOException;
        }
    };
    @Test
    public void testWithNothingToRetry() {
        Observable<Integer> source = Observable.range(0, 3);
        
        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.retry(retryTwice).subscribe(o);
        
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testRetryTwice() {
        Observable<Integer> source = Observable.create(new OnSubscribe<Integer>() {
            int count;
            @Override
            public void call(Subscriber<? super Integer> t1) {
                count++;
                t1.onNext(0);
                t1.onNext(1);
                if (count == 1) {
                    t1.onError(new TestException());
                    return;
                }
                t1.onNext(2);
                t1.onNext(3);
                t1.onCompleted();
            }
        });
        
        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.retry(retryTwice).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
        
    }
    @Test
    public void testRetryTwiceAndGiveUp() {
        Observable<Integer> source = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t1) {
                t1.onNext(0);
                t1.onNext(1);
                t1.onError(new TestException());
            }
        });
        
        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.retry(retryTwice).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onCompleted();
        
    }
    @Test
    public void testRetryOnSpecificException() {
        Observable<Integer> source = Observable.create(new OnSubscribe<Integer>() {
            int count;
            @Override
            public void call(Subscriber<? super Integer> t1) {
                count++;
                t1.onNext(0);
                t1.onNext(1);
                if (count == 1) {
                    t1.onError(new IOException());
                    return;
                }
                t1.onNext(2);
                t1.onNext(3);
                t1.onCompleted();
            }
        });
        
        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.retry(retryOnTestException).subscribe(o);

        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(0);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testRetryOnSpecificExceptionAndNotOther() {
        final IOException ioe = new IOException();
        final TestException te = new TestException();
        Observable<Integer> source = Observable.create(new OnSubscribe<Integer>() {
            int count;
            @Override
            public void call(Subscriber<? super Integer> t1) {
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
        Observer<Integer> o = mock(Observer.class);
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
        verify(o, never()).onCompleted();
    }
    
    @Test
    public void testUnsubscribeFromRetry() {
        PublishSubject<Integer> subject = PublishSubject.create();
        final AtomicInteger count = new AtomicInteger(0);
        Subscription sub = subject.retry(retryTwice).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer n) {
                count.incrementAndGet();
            }
        });
        subject.onNext(1);
        sub.unsubscribe();
        subject.onNext(2);
        assertEquals(1, count.get());
    }
    
    @Test(timeout = 10000)
    public void testUnsubscribeAfterError() {

        @SuppressWarnings("unchecked")
        Observer<Long> observer = mock(Observer.class);

        // Observable that always fails after 100ms
        OperatorRetryTest.SlowObservable so = new OperatorRetryTest.SlowObservable(100, 0);
        Observable<Long> o = Observable
                .create(so)
                .retry(retry5);

        OperatorRetryTest.AsyncObserver<Long> async = new OperatorRetryTest.AsyncObserver<Long>(observer);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(observer);
        // Should fail once
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
        assertEquals("Only 1 active subscription", 1, so.maxActive.get());
    }

    @Test(timeout = 10000)
    public void testTimeoutWithRetry() {

        @SuppressWarnings("unchecked")
        Observer<Long> observer = mock(Observer.class);

        // Observable that sends every 100ms (timeout fails instead)
        OperatorRetryTest.SlowObservable so = new OperatorRetryTest.SlowObservable(100, 10);
        Observable<Long> o = Observable
                .create(so)
                .timeout(80, TimeUnit.MILLISECONDS)
                .retry(retry5);

        OperatorRetryTest.AsyncObserver<Long> async = new OperatorRetryTest.AsyncObserver<Long>(observer);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(observer);
        // Should fail once
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
    }
    
    @Test
    public void testIssue2826() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final RuntimeException e = new RuntimeException("You shall not pass");
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1).map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t1) {
                c.incrementAndGet();
                throw e;
            }
        }).retry(retry5).subscribe(ts);

        ts.assertTerminalEvent();
        assertEquals(6, c.get());
        assertEquals(Collections.singletonList(e), ts.getOnErrorEvents());
    }
    @Test
    public void testJustAndRetry() throws Exception {
        final AtomicBoolean throwException = new AtomicBoolean(true);
        int value = Observable.just(1).map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t1) {
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
        final List<Long> list = new CopyOnWriteArrayList<Long>();
        final AtomicBoolean isFirst = new AtomicBoolean(true);
        Observable.<Long> just(1L, 2L, 3L).map(new Func1<Long, Long>(){
            @Override
            public Long call(Long x) {
                System.out.println("map " + x);
                if (x == 2 && isFirst.getAndSet(false)) {
                    throw new RuntimeException("retryable error");
                }
                return x;
            }})
        .retry(new Func2<Integer, Throwable, Boolean>() {
            @Override
            public Boolean call(Integer t1, Throwable t2) {
                return true;
            }})
        .forEach(new Action1<Long>() {

            @Override
            public void call(Long t) {
                System.out.println(t);
                list.add(t);
            }});
        assertEquals(Arrays.asList(1L,1L,2L,3L), list);
    }
    
    @Test
    public void testIssue3008RetryInfinite() {
        final List<Long> list = new CopyOnWriteArrayList<Long>();
        final AtomicBoolean isFirst = new AtomicBoolean(true);
        Observable.<Long> just(1L, 2L, 3L).map(new Func1<Long, Long>(){
            @Override
            public Long call(Long x) {
                System.out.println("map " + x);
                if (x == 2 && isFirst.getAndSet(false)) {
                    throw new RuntimeException("retryable error");
                }
                return x;
            }})
        .retry()
        .forEach(new Action1<Long>() {

            @Override
            public void call(Long t) {
                System.out.println(t);
                list.add(t);
            }});
        assertEquals(Arrays.asList(1L,1L,2L,3L), list);
    }
    @Test
    public void testBackpressure() {
        final List<Long> requests = new ArrayList<Long>();
        
        Observable<Integer> source = Observable
                .just(1)
                .concatWith(Observable.<Integer>error(new TestException()))
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        requests.add(t);
                    }
                });
        
        TestSubscriber<Integer> ts = TestSubscriber.create(3);
        source
        .retry(new Func2<Integer, Throwable, Boolean>() {
            @Override
            public Boolean call(Integer t1, Throwable t2) {
                return t1 < 3;
            }
        }).subscribe(ts);
        
        assertEquals(Arrays.asList(3L, 2L, 1L), requests);
        ts.assertValues(1, 1, 1);
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
}
