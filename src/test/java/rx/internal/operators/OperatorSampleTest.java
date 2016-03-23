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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.InOrder;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class OperatorSampleTest {
    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private Observer<Long> observer;
    private Observer<Object> observer2;

    @Before
    @SuppressWarnings("unchecked")
    // due to mocking
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        observer = mock(Observer.class);
        observer2 = mock(Observer.class);
    }

    @Test
    public void testSample() {
        Observable<Long> source = Observable.create(new OnSubscribe<Long>() {
            @Override
            public void call(final Subscriber<? super Long> observer1) {
                innerScheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        observer1.onNext(1L);
                    }
                }, 1, TimeUnit.SECONDS);
                innerScheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        observer1.onNext(2L);
                    }
                }, 2, TimeUnit.SECONDS);
                innerScheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        observer1.onCompleted();
                    }
                }, 3, TimeUnit.SECONDS);
            }
        });

        Observable<Long> sampled = source.sample(400L, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(800L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(any(Long.class));
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1200L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1600L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, never()).onNext(2L);
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithTimeEmitAndTerminate() {
        Observable<Long> source = Observable.create(new OnSubscribe<Long>() {
            @Override
            public void call(final Subscriber<? super Long> observer1) {
                innerScheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        observer1.onNext(1L);
                    }
                }, 1, TimeUnit.SECONDS);
                innerScheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        observer1.onNext(2L);
                        observer1.onCompleted();
                    }
                }, 2, TimeUnit.SECONDS);
            }
        });

        Observable<Long> sampled = source.sample(400L, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerNormal() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onNext(2);
        sampler.onNext(1);
        source.onNext(3);
        source.onNext(4);
        sampler.onNext(2);
        source.onCompleted();
        sampler.onNext(3);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, never()).onNext(1);
        inOrder.verify(observer2, times(1)).onNext(2);
        inOrder.verify(observer2, never()).onNext(3);
        inOrder.verify(observer2, times(1)).onNext(4);
        inOrder.verify(observer2, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerNoDuplicates() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onNext(2);
        sampler.onNext(1);
        sampler.onNext(1);

        source.onNext(3);
        source.onNext(4);
        sampler.onNext(2);
        sampler.onNext(2);

        source.onCompleted();
        sampler.onNext(3);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, never()).onNext(1);
        inOrder.verify(observer2, times(1)).onNext(2);
        inOrder.verify(observer2, never()).onNext(3);
        inOrder.verify(observer2, times(1)).onNext(4);
        inOrder.verify(observer2, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerTerminatingEarly() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onNext(2);
        sampler.onNext(1);
        sampler.onCompleted();

        source.onNext(3);
        source.onNext(4);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, never()).onNext(1);
        inOrder.verify(observer2, times(1)).onNext(2);
        inOrder.verify(observer2, times(1)).onCompleted();
        inOrder.verify(observer2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerEmitAndTerminate() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onNext(2);
        sampler.onNext(1);
        source.onNext(3);
        source.onCompleted();
        sampler.onNext(2);
        sampler.onCompleted();

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, never()).onNext(1);
        inOrder.verify(observer2, times(1)).onNext(2);
        inOrder.verify(observer2, times(1)).onNext(3);
        inOrder.verify(observer2, times(1)).onCompleted();
        inOrder.verify(observer2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerEmptySource() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onCompleted();
        sampler.onNext(1);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onCompleted();
        verify(observer2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onError(new RuntimeException("Forced failure!"));
        sampler.onNext(1);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onError(any(Throwable.class));
        verify(observer2, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }

    @Test
    public void sampleWithSamplerThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        sampler.onNext(1);
        sampler.onError(new RuntimeException("Forced failure!"));

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onNext(1);
        inOrder.verify(observer2, times(1)).onError(any(RuntimeException.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testSampleUnsubscribe() {
        final Subscription s = mock(Subscription.class);
        Observable<Integer> o = Observable.create(
                new OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        subscriber.add(s);
                    }
                }
        );
        o.throttleLast(1, TimeUnit.MILLISECONDS).subscribe().unsubscribe();
        verify(s).unsubscribe();
    }
    
    @Test
    public void testSampleOtherUnboundedIn() {
        
        final long[] requested = { -1 };
        
        PublishSubject.create()
        .doOnRequest(new Action1<Long>() {
            @Override
            public void call(Long t) {
                requested[0] = t;
            }
        })
        .sample(PublishSubject.create()).subscribe();
        
        Assert.assertEquals(Long.MAX_VALUE, requested[0]);
    }
    
    @Test
    public void testSampleTimedUnboundedIn() {
        
        final long[] requested = { -1 };
        
        PublishSubject.create()
        .doOnRequest(new Action1<Long>() {
            @Override
            public void call(Long t) {
                requested[0] = t;
            }
        })
        .sample(1, TimeUnit.SECONDS).subscribe().unsubscribe();
        
        Assert.assertEquals(Long.MAX_VALUE, requested[0]);
    }
    
    @Test
    public void dontUnsubscribeChild1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        PublishSubject<Integer> sampler = PublishSubject.create();
        
        source.sample(sampler).unsafeSubscribe(ts);
        
        source.onCompleted();
        
        Assert.assertFalse("Source has subscribers?", source.hasObservers());
        Assert.assertFalse("Sampler has subscribers?", sampler.hasObservers());
        
        Assert.assertFalse("TS unsubscribed?", ts.isUnsubscribed());
    }

    @Test
    public void dontUnsubscribeChild2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        PublishSubject<Integer> sampler = PublishSubject.create();
        
        source.sample(sampler).unsafeSubscribe(ts);
        
        sampler.onCompleted();
        
        Assert.assertFalse("Source has subscribers?", source.hasObservers());
        Assert.assertFalse("Sampler has subscribers?", sampler.hasObservers());
        
        Assert.assertFalse("TS unsubscribed?", ts.isUnsubscribed());
    }
    
    @Test
    public void neverSetProducer() {
        Observable<Integer> neverBackpressure = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t) {
                t.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        // irrelevant in this test
                    }
                });
            }
        });
        
        final AtomicInteger count = new AtomicInteger();
        
        neverBackpressure.sample(neverBackpressure).unsafeSubscribe(new Subscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                // irrelevant
            }
            
            @Override
            public void onError(Throwable e) {
                // irrelevant
            }
            
            @Override
            public void onCompleted() {
                // irrelevant
            }
            
            @Override
            public void setProducer(Producer p) {
                count.incrementAndGet();
            }
        });
        
        Assert.assertEquals(0, count.get());
    }
    
    @Test
    public void unsubscribeMainAfterCompleted() {
        final AtomicBoolean unsubscribed = new AtomicBoolean();
        
        Observable<Integer> source = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t) {
                t.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        unsubscribed.set(true);
                    }
                }));
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onCompleted() {
                if (unsubscribed.get()) {
                    onError(new IllegalStateException("Resource unsubscribed!"));
                } else {
                    super.onCompleted();
                }
            }
        };
        
        PublishSubject<Integer> sampler = PublishSubject.create();
        
        source.sample(sampler).unsafeSubscribe(ts);
        
        sampler.onCompleted();
        
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void unsubscribeSamplerAfterCompleted() {
        final AtomicBoolean unsubscribed = new AtomicBoolean();
        
        Observable<Integer> source = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t) {
                t.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        unsubscribed.set(true);
                    }
                }));
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onCompleted() {
                if (unsubscribed.get()) {
                    onError(new IllegalStateException("Resource unsubscribed!"));
                } else {
                    super.onCompleted();
                }
            }
        };
        
        PublishSubject<Integer> sampled = PublishSubject.create();
        
        sampled.sample(source).unsafeSubscribe(ts);
        
        sampled.onCompleted();
        
        ts.assertNoErrors();
        ts.assertCompleted();
    }
}
