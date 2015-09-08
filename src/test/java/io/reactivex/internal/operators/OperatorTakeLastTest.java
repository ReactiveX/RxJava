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

package io.reactivex.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.TestHelper;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorTakeLastTest {

    @Test
    public void testTakeLastEmpty() {
        Observable<String> w = Observable.empty();
        Observable<String> take = w.takeLast(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeLast1() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> take = w.takeLast(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(observer);
        take.subscribe(observer);
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeLast2() {
        Observable<String> w = Observable.just("one");
        Observable<String> take = w.takeLast(10);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeLastWithZeroCount() {
        Observable<String> w = Observable.just("one");
        Observable<String> take = w.takeLast(0);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testTakeLastWithNull() {
        Observable<String> w = Observable.just("one", null, "three");
        Observable<String> take = w.takeLast(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTakeLastWithNegativeCount() {
        Observable.just("one").takeLast(-1);
    }

    @Test
    public void testBackpressure1() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.range(1, 100000).takeLast(1)
        .observeOn(Schedulers.newThread())
        .map(newSlowProcessor()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertValue(100000);
    }

    @Test
    public void testBackpressure2() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.range(1, 100000).takeLast(Observable.bufferSize() * 4)
        .observeOn(Schedulers.newThread()).map(newSlowProcessor()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 4, ts.valueCount());
    }

    private Function<Integer, Integer> newSlowProcessor() {
        return new Function<Integer, Integer>() {
            int c = 0;

            @Override
            public Integer apply(Integer i) {
                if (c++ < 100) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
                return i;
            }

        };
    }

    @Test
    public void testIssue1522() {
        // https://github.com/ReactiveX/RxJava/issues/1522
        assertEquals(0, Observable
                .empty()
                .count()
                .filter(v -> false)
                .toList()
                .toBlocking().single().size());
    }

    @Test
    public void testIgnoreRequest1() {
        // If `takeLast` does not ignore `request` properly, StackOverflowError will be thrown.
        Observable.range(0, 100000).takeLast(100000).subscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                request(Long.MAX_VALUE);
            }
        });
    }

    @Test
    public void testIgnoreRequest2() {
        // If `takeLast` does not ignore `request` properly, StackOverflowError will be thrown.
        Observable.range(0, 100000).takeLast(100000).subscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });
    }

    @Test(timeout = 30000)
    public void testIgnoreRequest3() {
        // If `takeLast` does not ignore `request` properly, it will enter an infinite loop.
        Observable.range(0, 100000).takeLast(100000).subscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                request(Long.MAX_VALUE);
            }
        });
    }


    @Test
    public void testIgnoreRequest4() {
        // If `takeLast` does not ignore `request` properly, StackOverflowError will be thrown.
        Observable.range(0, 100000).takeLast(100000).subscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });
    }
    
    @Test
    public void testUnsubscribeTakesEffectEarlyOnFastPath() {
        final AtomicInteger count = new AtomicInteger();
        Observable.range(0, 100000).takeLast(100000).subscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                count.incrementAndGet();
                cancel();
            }
        });
        assertEquals(1,count.get());
    }
    
    @Test(timeout=10000)
    public void testRequestOverflow() {
        final List<Integer> list = new ArrayList<>();
        Observable.range(1, 100).takeLast(50).subscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                request(2);
            }
            
            @Override
            public void onComplete() {
                
            }

            @Override
            public void onError(Throwable e) {
                
            }

            @Override
            public void onNext(Integer t) {
                list.add(t);
                request(Long.MAX_VALUE-1);
            }});
        assertEquals(50, list.size());
    }
}