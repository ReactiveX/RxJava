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
package rx.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.MockitoAnnotations;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorScanTest {

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testScanIntegersWithInitialValue() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.just(1, 2, 3);

        Observable<String> m = observable.scan("", new Func2<String, Integer, String>() {

            @Override
            public String call(String s, Integer n) {
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
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testScanIntegersWithoutInitialValue() {
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.just(1, 2, 3);

        Observable<Integer> m = observable.scan(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
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
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testScanIntegersWithoutInitialValueAndOnlyOneValue() {
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.just(1);

        Observable<Integer> m = observable.scan(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(0);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(anyInt());
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void shouldNotEmitUntilAfterSubscription() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.range(1, 100).scan(0, new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }).filter(new Func1<Integer, Boolean>() {

            @Override
            public Boolean call(Integer t1) {
                // this will cause request(1) when 0 is emitted
                return t1 > 0;
            }
            
        }).subscribe(ts);
        
        assertEquals(100, ts.getOnNextEvents().size());
    }
    
    @Test
    public void testBackpressureWithInitialValue() {
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, 100)
                .scan(0, new Func2<Integer, Integer, Integer>() {

                    @Override
                    public Integer call(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                })
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(10);
                    }

                    @Override
                    public void onCompleted() {

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
                .scan(new Func2<Integer, Integer, Integer>() {

                    @Override
                    public Integer call(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                })
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(10);
                    }

                    @Override
                    public void onCompleted() {

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
                .scan(0, new Func2<Integer, Integer, Integer>() {

                    @Override
                    public Integer call(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                })
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onCompleted() {

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
                .collect(new Func0<List<Integer>>() {

                    @Override
                    public List<Integer> call() {
                        return new ArrayList<Integer>();
                    }
                    
                }, new Action2<List<Integer>, Integer>() {

                    @Override
                    public void call(List<Integer> list, Integer t2) {
                        list.add(t2);
                    }

                }).takeLast(1);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.toBlocking().single());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.toBlocking().single());
    }

    @Test
    public void testScanWithRequestOne() {
        Observable<Integer> o = Observable.just(1, 2).scan(0, new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }).take(1);
        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        o.subscribe(subscriber);
        subscriber.assertReceivedOnNext(Arrays.asList(0));
        subscriber.assertTerminalEvent();
        subscriber.assertNoErrors();
    }

    @Test
    public void testScanShouldNotRequestZero() {
        final AtomicReference<Producer> producer = new AtomicReference<Producer>();
        Observable<Integer> o = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(final Subscriber<? super Integer> subscriber) {
                Producer p = spy(new Producer() {

                    private AtomicBoolean requested = new AtomicBoolean(false);

                    @Override
                    public void request(long n) {
                        if (requested.compareAndSet(false, true)) {
                            subscriber.onNext(1);
                        } else {
                            subscriber.onCompleted();
                        }
                    }
                });
                producer.set(p);
                subscriber.setProducer(p);
            }
        }).scan(100, new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });

        o.subscribe(new TestSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });

        verify(producer.get(), never()).request(0);
        verify(producer.get(), times(2)).request(1);
    }
    
    @Test
    public void testInitialValueEmittedNoProducer() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        source.scan(0, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }
        }).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertValue(0);
    }
    
    @Test
    public void testInitialValueEmittedWithProducer() {
        Observable<Integer> source = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t) {
                t.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        // deliberately no op
                    }
                });
            }
        });
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        source.scan(0, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }
        }).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertValue(0);
    }
    
    @Test
    public void testInitialValueNull() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(1, 10).scan(null, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                if (t1 == null) {
                    return t2;
                }
                return t1 + t2;
            }
        }).subscribe(ts);
        
        ts.assertValues(null, 1, 3, 6, 10, 15, 21, 28, 36, 45, 55);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void testEverythingIsNull() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(1, 6).scan(null, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return null;
            }
        }).subscribe(ts);
        
        ts.assertValues(null, null, null, null, null, null, null);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test(timeout = 1000)
    public void testUnboundedSource() {
        Observable.range(0, Integer.MAX_VALUE)
        .scan(0, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer a, Integer b) {
                return 0;
            }
        })
        .subscribe(new TestSubscriber<Integer>() {
            int count;
            @Override
            public void onNext(Integer t) {
                if (++count == 2) {
                    unsubscribe();
                }
            }
        });
    }
    
    @Test
    public void scanShouldPassUpstreamARequestForMaxValue() {
        final List<Long> requests = new ArrayList<Long>();
        Observable.just(1,2,3).doOnRequest(new Action1<Long>() {
            @Override
            public void call(Long n) {
                requests.add(n);
            }
        })
        .scan(new Func2<Integer,Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return 0;
            }}).count().subscribe();
        
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }
}
