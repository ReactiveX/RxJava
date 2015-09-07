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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.TestHelper;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorScanTest {

    @Test
    public void testScanIntegersWithInitialValue() {
        Subscriber<String> observer = TestHelper.mockSubscriber();

        Observable<Integer> observable = Observable.just(1, 2, 3);

        Observable<String> m = observable.scan("", new BiFunction<String, Integer, String>() {

            @Override
            public String apply(String s, Integer n) {
                return s + n.toString();
            }

        });
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext("");
        verify(observer, times(1)).onNext("1");
        verify(observer, times(1)).onNext("12");
        verify(observer, times(1)).onNext("123");
        verify(observer, times(4)).onNext(anyString());
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testScanIntegersWithoutInitialValue() {
        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        Observable<Integer> observable = Observable.just(1, 2, 3);

        Observable<Integer> m = observable.scan(new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(0);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(6);
        verify(observer, times(3)).onNext(anyInt());
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testScanIntegersWithoutInitialValueAndOnlyOneValue() {
        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        Observable<Integer> observable = Observable.just(1);

        Observable<Integer> m = observable.scan(new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(0);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(anyInt());
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void shouldNotEmitUntilAfterSubscription() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
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
    public void testBackpressureWithInitialValue() {
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, 100)
                .scan(0, new BiFunction<Integer, Integer, Integer>() {

                    @Override
                    public Integer apply(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                })
                .subscribe(new Observer<Integer>() {

                    @Override
                    public void onStart() {
                        request(10);
                    }

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

        // we only expect to receive 10 since we request(10)
        assertEquals(10, count.get());
    }
    
    @Test
    public void testBackpressureWithoutInitialValue() {
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, 100)
                .scan(new BiFunction<Integer, Integer, Integer>() {

                    @Override
                    public Integer apply(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                })
                .subscribe(new Observer<Integer>() {

                    @Override
                    public void onStart() {
                        request(10);
                    }

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

        // we only expect to receive 10 since we request(10)
        assertEquals(10, count.get());
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
                .subscribe(new Observer<Integer>() {

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
                .collect(new Supplier<List<Integer>>() {

                    @Override
                    public List<Integer> get() {
                        return new ArrayList<>();
                    }
                    
                }, new BiConsumer<List<Integer>, Integer>() {

                    @Override
                    public void accept(List<Integer> list, Integer t2) {
                        list.add(t2);
                    }

                }).takeLast(1);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.toBlocking().single());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.toBlocking().single());
    }

    @Test
    public void testScanWithRequestOne() {
        Observable<Integer> o = Observable.just(1, 2).scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }).take(1);
        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        o.subscribe(subscriber);
        subscriber.assertValue(0);
        subscriber.assertTerminated();
        subscriber.assertNoErrors();
    }

    @Test
    public void testScanShouldNotRequestZero() {
        final AtomicReference<Subscription> producer = new AtomicReference<>();
        Observable<Integer> o = Observable.create(new Publisher<Integer>() {
            @Override
            public void subscribe(final Subscriber<? super Integer> subscriber) {
                Subscription p = spy(new Subscription() {

                    private AtomicBoolean requested = new AtomicBoolean(false);

                    @Override
                    public void request(long n) {
                        if (requested.compareAndSet(false, true)) {
                            subscriber.onNext(1);
                            subscriber.onComplete();
                        }
                    }
                    
                    @Override
                    public void cancel() {
                        
                    }
                });
                producer.set(p);
                subscriber.onSubscribe(p);
            }
        }).scan(100, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });

        o.subscribe(new TestSubscriber<Integer>(1L) {

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });

        verify(producer.get(), never()).request(0);
        verify(producer.get(), times(3)).request(1); // FIXME this was 2 in 1.x
    }
    
    @Test
    public void testInitialValueEmittedNoProducer() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        
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
    
    @Test
    public void testInitialValueEmittedWithProducer() {
        Observable<Integer> source = Observable.never();
        
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        
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