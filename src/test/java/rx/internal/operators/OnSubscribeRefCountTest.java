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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.*;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.functions.*;
import rx.observers.*;
import rx.schedulers.*;
import rx.subjects.ReplaySubject;
import rx.subscriptions.Subscriptions;

public class OnSubscribeRefCountTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRefCountAsync() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Long> r = Observable.interval(0, 5, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        subscribeCount.incrementAndGet();
                    }

                })
                .doOnNext(new Action1<Long>() {

                    @Override
                    public void call(Long l) {
                        nextCount.incrementAndGet();
                    }

                })
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        Subscription s1 = r.subscribe(new Action1<Long>() {

            @Override
            public void call(Long l) {
                receivedCount.incrementAndGet();
            }

        });
        Subscription s2 = r.subscribe();

        // give time to emit
        try {
            Thread.sleep(52);
        } catch (InterruptedException e) {
        }

        // now unsubscribe
        s2.unsubscribe(); // unsubscribe s2 first as we're counting in 1 and there can be a race between unsubscribe and one subscriber getting a value but not the other
        s1.unsubscribe();

        System.out.println("onNext: " + nextCount.get());

        // should emit once for both subscribers
        assertEquals(nextCount.get(), receivedCount.get());
        // only 1 subscribe
        assertEquals(1, subscribeCount.get());
    }

    @Test
    public void testRefCountSynchronous() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Integer> r = Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        subscribeCount.incrementAndGet();
                    }

                })
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer l) {
                        nextCount.incrementAndGet();
                    }

                })
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        Subscription s1 = r.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer l) {
                receivedCount.incrementAndGet();
            }

        });
        Subscription s2 = r.subscribe();

        // give time to emit
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        // now unsubscribe
        s2.unsubscribe(); // unsubscribe s2 first as we're counting in 1 and there can be a race between unsubscribe and one subscriber getting a value but not the other
        s1.unsubscribe();

        System.out.println("onNext Count: " + nextCount.get());

        // it will emit twice because it is synchronous
        assertEquals(nextCount.get(), receivedCount.get() * 2);
        // it will subscribe twice because it is synchronous
        assertEquals(2, subscribeCount.get());
    }

    @Test
    public void testRefCountSynchronousTake() {
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Integer> r = Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer l) {
                        System.out.println("onNext --------> " + l);
                        nextCount.incrementAndGet();
                    }

                })
                .take(4)
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        r.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer l) {
                receivedCount.incrementAndGet();
            }

        });

        System.out.println("onNext: " + nextCount.get());

        assertEquals(4, receivedCount.get());
        assertEquals(4, receivedCount.get());
    }

    @Test
    public void testRepeat() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger unsubscribeCount = new AtomicInteger();
        Observable<Long> r = Observable.interval(0, 1, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Subscribe received");
                        // when we are subscribed
                        subscribeCount.incrementAndGet();
                    }

                })
                .doOnUnsubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Unsubscribe received");
                        // when we are unsubscribed
                        unsubscribeCount.incrementAndGet();
                    }

                })
                .publish().refCount();

        for (int i = 0; i < 10; i++) {
            TestSubscriber<Long> ts1 = new TestSubscriber<Long>();
            TestSubscriber<Long> ts2 = new TestSubscriber<Long>();
            r.subscribe(ts1);
            r.subscribe(ts2);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            ts1.unsubscribe();
            ts2.unsubscribe();
            ts1.assertNoErrors();
            ts2.assertNoErrors();
            assertTrue(ts1.getOnNextEvents().size() > 0);
            assertTrue(ts2.getOnNextEvents().size() > 0);
        }

        assertEquals(10, subscribeCount.get());
        assertEquals(10, unsubscribeCount.get());
    }

    @Test
    public void testConnectUnsubscribe() throws InterruptedException {
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        Observable<Long> o = synchronousInterval()
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Subscribe received");
                        // when we are subscribed
                        subscribeLatch.countDown();
                    }

                })
                .doOnUnsubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Unsubscribe received");
                        // when we are unsubscribed
                        unsubscribeLatch.countDown();
                    }

                });
        TestSubscriber<Long> s = new TestSubscriber<Long>();
        o.publish().refCount().subscribeOn(Schedulers.newThread()).subscribe(s);
        System.out.println("send unsubscribe");
        // wait until connected
        subscribeLatch.await();
        // now unsubscribe
        s.unsubscribe();
        System.out.println("DONE sending unsubscribe ... now waiting");
        if (!unsubscribeLatch.await(3000, TimeUnit.MILLISECONDS)) {
            System.out.println("Errors: " + s.getOnErrorEvents());
            if (s.getOnErrorEvents().size() > 0) {
                s.getOnErrorEvents().get(0).printStackTrace();
            }
            fail("timed out waiting for unsubscribe");
        }
        s.assertNoErrors();
    }

    @Test
    public void testConnectUnsubscribeRaceConditionLoop() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            testConnectUnsubscribeRaceCondition();
        }
    }
    
    @Test
    public void testConnectUnsubscribeRaceCondition() throws InterruptedException {
        final AtomicInteger subUnsubCount = new AtomicInteger();
        Observable<Long> o = synchronousInterval()
                .doOnUnsubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Unsubscribe received");
                        // when we are unsubscribed
                        subUnsubCount.decrementAndGet();
                    }

                })
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* SUBSCRIBE received");
                        subUnsubCount.incrementAndGet();
                    }

                });

        TestSubscriber<Long> s = new TestSubscriber<Long>();
        
        o.publish().refCount().subscribeOn(Schedulers.computation()).subscribe(s);
        System.out.println("send unsubscribe");
        // now immediately unsubscribe while subscribeOn is racing to subscribe
        s.unsubscribe();
        // this generally will mean it won't even subscribe as it is already unsubscribed by the time connect() gets scheduled
        // give time to the counter to update
        Thread.sleep(1);
        // either we subscribed and then unsubscribed, or we didn't ever even subscribe
        assertEquals(0, subUnsubCount.get());

        System.out.println("DONE sending unsubscribe ... now waiting");
        System.out.println("Errors: " + s.getOnErrorEvents());
        if (s.getOnErrorEvents().size() > 0) {
            s.getOnErrorEvents().get(0).printStackTrace();
        }
        s.assertNoErrors();
    }

    private Observable<Long> synchronousInterval() {
        return Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(Subscriber<? super Long> subscriber) {
                while (!subscriber.isUnsubscribed()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    subscriber.onNext(1L);
                }
            }
        });
    }

    @Test
    public void onlyFirstShouldSubscribeAndLastUnsubscribe() {
        final AtomicInteger subscriptionCount = new AtomicInteger();
        final AtomicInteger unsubscriptionCount = new AtomicInteger();
        Observable<Integer> observable = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> observer) {
                subscriptionCount.incrementAndGet();
                observer.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        unsubscriptionCount.incrementAndGet();
                    }
                }));
            }
        });
        Observable<Integer> refCounted = observable.publish().refCount();
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        Subscription first = refCounted.subscribe(observer);
        assertEquals(1, subscriptionCount.get());
        Subscription second = refCounted.subscribe(observer);
        assertEquals(1, subscriptionCount.get());
        first.unsubscribe();
        assertEquals(0, unsubscriptionCount.get());
        second.unsubscribe();
        assertEquals(1, unsubscriptionCount.get());
    }

    @Test
    public void testRefCount() {
        TestScheduler s = new TestScheduler();
        Observable<Long> interval = Observable.interval(100, TimeUnit.MILLISECONDS, s).publish().refCount();

        // subscribe list1
        final List<Long> list1 = new ArrayList<Long>();
        Subscription s1 = interval.subscribe(new Action1<Long>() {

            @Override
            public void call(Long t1) {
                list1.add(t1);
            }

        });
        s.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(2, list1.size());
        assertEquals(0L, list1.get(0).longValue());
        assertEquals(1L, list1.get(1).longValue());

        // subscribe list2
        final List<Long> list2 = new ArrayList<Long>();
        Subscription s2 = interval.subscribe(new Action1<Long>() {

            @Override
            public void call(Long t1) {
                list2.add(t1);
            }

        });
        s.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        // list 1 should have 5 items
        assertEquals(5, list1.size());
        assertEquals(2L, list1.get(2).longValue());
        assertEquals(3L, list1.get(3).longValue());
        assertEquals(4L, list1.get(4).longValue());

        // list 2 should only have 3 items
        assertEquals(3, list2.size());
        assertEquals(2L, list2.get(0).longValue());
        assertEquals(3L, list2.get(1).longValue());
        assertEquals(4L, list2.get(2).longValue());

        // unsubscribe list1
        s1.unsubscribe();

        // advance further
        s.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        // list 1 should still have 5 items
        assertEquals(5, list1.size());

        // list 2 should have 6 items
        assertEquals(6, list2.size());
        assertEquals(5L, list2.get(3).longValue());
        assertEquals(6L, list2.get(4).longValue());
        assertEquals(7L, list2.get(5).longValue());

        // unsubscribe list2
        s2.unsubscribe();

        // advance further
        s.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        // subscribing a new one should start over because the source should have been unsubscribed
        // subscribe list3
        final List<Long> list3 = new ArrayList<Long>();
        interval.subscribe(new Action1<Long>() {

            @Override
            public void call(Long t1) {
                list3.add(t1);
            }

        });
        s.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(2, list3.size());
        assertEquals(0L, list3.get(0).longValue());
        assertEquals(1L, list3.get(1).longValue());

    }

    @Test
    public void testAlreadyUnsubscribedClient() {
        Subscriber<Integer> done = Subscribers.empty();
        done.unsubscribe();

        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);

        Observable<Integer> result = Observable.just(1).publish().refCount();

        result.subscribe(done);

        result.subscribe(o);

        verify(o).onNext(1);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAlreadyUnsubscribedInterleavesWithClient() {
        ReplaySubject<Integer> source = ReplaySubject.create();

        Subscriber<Integer> done = Subscribers.empty();
        done.unsubscribe();

        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        Observable<Integer> result = source.publish().refCount();

        result.subscribe(o);

        source.onNext(1);

        result.subscribe(done);

        source.onNext(2);
        source.onCompleted();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testConnectDisconnectConnectAndSubjectState() {
        Observable<Integer> o1 = Observable.just(10);
        Observable<Integer> o2 = Observable.just(20);
        Observable<Integer> combined = Observable.combineLatest(o1, o2, new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }).publish().refCount();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();

        combined.subscribe(ts1);
        combined.subscribe(ts2);

        ts1.assertTerminalEvent();
        ts1.assertNoErrors();
        ts1.assertReceivedOnNext(Arrays.asList(30));

        ts2.assertTerminalEvent();
        ts2.assertNoErrors();
        ts2.assertReceivedOnNext(Arrays.asList(30));
    }

    @Test(timeout = 10000)
    public void testUpstreamErrorAllowsRetry() throws InterruptedException {
        
        final AtomicReference<Throwable> err1 = new AtomicReference<Throwable>();
        final AtomicReference<Throwable> err2 = new AtomicReference<Throwable>();
        
        final AtomicInteger intervalSubscribed = new AtomicInteger();
        Observable<String> interval =
                Observable.interval(200,TimeUnit.MILLISECONDS)
                        .doOnSubscribe(
                                new Action0() {
                                    @Override
                                    public void call() {
                                        System.out.println("Subscribing to interval " + intervalSubscribed.incrementAndGet());
                                    }
                                }
                         )
                        .flatMap(new Func1<Long, Observable<String>>() {
                            @Override
                            public Observable<String> call(Long t1) {
                                return Observable.defer(new Func0<Observable<String>>() {
                                    @Override
                                    public Observable<String> call() {
                                        return Observable.<String>error(new Exception("Some exception"));
                                    }
                                });
                            }
                        })
                        .onErrorResumeNext(new Func1<Throwable, Observable<String>>() {
                            @Override
                            public Observable<String> call(Throwable t1) {
                                return Observable.error(t1);
                            }
                        })
                        .publish()
                        .refCount();

        interval
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable t1) {
                        System.out.println("Subscriber 1 onError: " + t1);
                    }
                })
                .retry(5)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String t1) {
                        System.out.println("Subscriber 1: " + t1);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable t) {
                        err1.set(t);
                    }
                });
        Thread.sleep(100);
        interval
        .doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable t1) {
                System.out.println("Subscriber 2 onError: " + t1);
            }
        })
        .retry(5)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String t1) {
                        System.out.println("Subscriber 2: " + t1);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable t) {
                        err2.set(t);
                    }
                });
        
        Thread.sleep(1300);
        
        System.out.println(intervalSubscribed.get());
        assertEquals(6, intervalSubscribed.get());
        
        assertNotNull("First subscriber didn't get the error", err1);
        assertNotNull("Second subscriber didn't get the error", err2);
    }
}
