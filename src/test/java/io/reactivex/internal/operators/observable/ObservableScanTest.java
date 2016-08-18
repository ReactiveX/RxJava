/**
 * Copyright 2016 Netflix, Inc.
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.TestHelper;
import io.reactivex.functions.*;
import io.reactivex.observers.*;
import io.reactivex.subjects.PublishSubject;

public class ObservableScanTest {

    @Test
    public void testScanIntegersWithInitialValue() {
        Observer<String> NbpObserver = TestHelper.mockObserver();

        Observable<Integer> o = Observable.just(1, 2, 3);

        Observable<String> m = o.scan("", new BiFunction<String, Integer, String>() {

            @Override
            public String apply(String s, Integer n) {
                return s + n.toString();
            }

        });
        m.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onNext("");
        verify(NbpObserver, times(1)).onNext("1");
        verify(NbpObserver, times(1)).onNext("12");
        verify(NbpObserver, times(1)).onNext("123");
        verify(NbpObserver, times(4)).onNext(anyString());
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testScanIntegersWithoutInitialValue() {
        Observer<Integer> NbpObserver = TestHelper.mockObserver();

        Observable<Integer> o = Observable.just(1, 2, 3);

        Observable<Integer> m = o.scan(new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });
        m.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onNext(0);
        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(3);
        verify(NbpObserver, times(1)).onNext(6);
        verify(NbpObserver, times(3)).onNext(anyInt());
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testScanIntegersWithoutInitialValueAndOnlyOneValue() {
        Observer<Integer> NbpObserver = TestHelper.mockObserver();

        Observable<Integer> o = Observable.just(1);

        Observable<Integer> m = o.scan(new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });
        m.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onNext(0);
        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(anyInt());
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void shouldNotEmitUntilAfterSubscription() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(1, 100).scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }).filter(new Predicate<Integer>() {

            @Override
            public boolean test(Integer t1) {
                // this will cause request(1) when 0 is emitted
                return t1 > 0;
            }
            
        }).subscribe(ts);
        
        assertEquals(100, ts.values().size());
    }
    
    @Test
    public void testNoBackpressureWithInitialValue() {
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, 100)
                .scan(0, new BiFunction<Integer, Integer, Integer>() {

                    @Override
                    public Integer apply(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                })
                .subscribe(new DefaultObserver<Integer>() {

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        fail(e.getMessage());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                    }

                });

        // we only expect to receive 101 as we'll receive all 100 + the initial value
        assertEquals(101, count.get());
    }

    /**
     * This uses the public API collect which uses scan under the covers.
     */
    @Test
    public void testSeedFactory() {
        Observable<List<Integer>> o = Observable.range(1, 10)
                .collect(new Callable<List<Integer>>() {

                    @Override
                    public List<Integer> call() {
                        return new ArrayList<Integer>();
                    }
                    
                }, new BiConsumer<List<Integer>, Integer>() {

                    @Override
                    public void accept(List<Integer> list, Integer t2) {
                        list.add(t2);
                    }

                }).takeLast(1);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.blockingSingle());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.blockingSingle());
    }

    @Test
    public void testScanWithRequestOne() {
        Observable<Integer> o = Observable.just(1, 2).scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }).take(1);
        TestObserver<Integer> NbpSubscriber = new TestObserver<Integer>();
        o.subscribe(NbpSubscriber);
        NbpSubscriber.assertValue(0);
        NbpSubscriber.assertTerminated();
        NbpSubscriber.assertNoErrors();
    }

    @Test
    public void testInitialValueEmittedNoProducer() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        TestObserver<Integer> ts = new TestObserver<Integer>();
        
        source.scan(0, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        }).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValue(0);
    }
}