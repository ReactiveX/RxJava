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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorSwitchTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private Subscriber<String> observer;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        observer = TestHelper.mockSubscriber();
    }

    @Test
    public void testSwitchWhenOuterCompleteBeforeInner() {
        Observable<Observable<String>> source = Observable.create(new Publisher<Observable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                publishNext(observer, 50, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 70, "one");
                        publishNext(observer, 100, "two");
                        publishCompleted(observer, 200);
                    }
                }));
                publishCompleted(observer, 60);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(2)).onNext(anyString());
        inOrder.verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSwitchWhenInnerCompleteBeforeOuter() {
        Observable<Observable<String>> source = Observable.create(new Publisher<Observable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                publishNext(observer, 10, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 0, "one");
                        publishNext(observer, 10, "two");
                        publishCompleted(observer, 20);
                    }
                }));

                publishNext(observer, 100, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 0, "three");
                        publishNext(observer, 10, "four");
                        publishCompleted(observer, 20);
                    }
                }));
                publishCompleted(observer, 200);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(150, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onComplete();
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        inOrder.verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSwitchWithComplete() {
        Observable<Observable<String>> source = Observable.create(new Publisher<Observable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                publishNext(observer, 50, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(final Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 60, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 200, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(final Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 0, "three");
                        publishNext(observer, 100, "four");
                    }
                }));

                publishCompleted(observer, 250);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("four");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithError() {
        Observable<Observable<String>> source = Observable.create(new Publisher<Observable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                publishNext(observer, 50, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(final Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 200, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 0, "three");
                        publishNext(observer, 100, "four");
                    }
                }));

                publishError(observer, 250, new TestException());
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testSwitchWithSubsequenceComplete() {
        Observable<Observable<String>> source = Observable.create(new Publisher<Observable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                publishNext(observer, 50, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishCompleted(observer, 0);
                    }
                }));

                publishNext(observer, 150, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 50, "three");
                    }
                }));
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithSubsequenceError() {
        Observable<Observable<String>> source = Observable.create(new Publisher<Observable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                publishNext(observer, 50, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishError(observer, 0, new TestException());
                    }
                }));

                publishNext(observer, 150, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 50, "three");
                    }
                }));

            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext("three");
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    private <T> void publishCompleted(final Subscriber<T> observer, long delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Subscriber<T> observer, long delay, final Throwable error) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Subscriber<T> observer, long delay, final T value) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSwitchIssue737() {
        // https://github.com/ReactiveX/RxJava/issues/737
        Observable<Observable<String>> source = Observable.create(new Publisher<Observable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                publishNext(observer, 0, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 10, "1-one");
                        publishNext(observer, 20, "1-two");
                        // The following events will be ignored
                        publishNext(observer, 30, "1-three");
                        publishCompleted(observer, 40);
                    }
                }));
                publishNext(observer, 25, Observable.create(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        publishNext(observer, 10, "2-one");
                        publishNext(observer, 20, "2-two");
                        publishNext(observer, 30, "2-three");
                        publishCompleted(observer, 40);
                    }
                }));
                publishCompleted(observer, 30);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("1-one");
        inOrder.verify(observer, times(1)).onNext("1-two");
        inOrder.verify(observer, times(1)).onNext("2-one");
        inOrder.verify(observer, times(1)).onNext("2-two");
        inOrder.verify(observer, times(1)).onNext("2-three");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testBackpressure() {

        PublishSubject<String> o1 = PublishSubject.create();
        PublishSubject<String> o2 = PublishSubject.create();
        PublishSubject<String> o3 = PublishSubject.create();
        
        PublishSubject<PublishSubject<String>> o = PublishSubject.create();

        publishNext(o, 0, o1);
        publishNext(o, 5, o2);
        publishNext(o, 10, o3);
        publishCompleted(o, 15);

        for (int i = 0; i < 10; i++) {
            publishNext(o1, i * 5, "a" + (i + 1));
            publishNext(o2, 5 + i * 5, "b" + (i + 1));
            publishNext(o3, 10 + i * 5, "c" + (i + 1));
        }

        publishCompleted(o1, 45);
        publishCompleted(o2, 50);
        publishCompleted(o3, 55);

        
        final TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        Observable.switchOnNext(o).subscribe(new Observer<String>() {

            private int requested = 0;

            @Override
            public void onStart() {
                requested = 3;
                request(3);
            }

            @Override
            public void onComplete() {
                testSubscriber.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                testSubscriber.onError(e);
            }

            @Override
            public void onNext(String s) {
                testSubscriber.onNext(s);
                requested--;
                if(requested == 0) {
                    requested = 3;
                    request(3);
                }
            }
        });
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        testSubscriber.assertValues("a1", "b1", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10");
        testSubscriber.assertNoErrors();
        testSubscriber.assertTerminated();
    }

    @Test
    public void testUnsubscribe() {
        final AtomicBoolean isUnsubscribed = new AtomicBoolean();
        Observable.switchOnNext(
                Observable.create(new Publisher<Observable<Integer>>() {
                    @Override
                    public void subscribe(final Subscriber<? super Observable<Integer>> subscriber) {
                        BooleanSubscription bs = new BooleanSubscription();
                        subscriber.onSubscribe(bs);
                        subscriber.onNext(Observable.just(1));
                        isUnsubscribed.set(bs.isCancelled());
                    }
                })
        ).take(1).subscribe();
        assertTrue("Switch doesn't propagate 'unsubscribe'", isUnsubscribed.get());
    }
    /** The upstream producer hijacked the switch producer stopping the requests aimed at the inner observables. */
    @Test
    public void testIssue2654() {
        Observable<String> oneItem = Observable.just("Hello").mergeWith(Observable.<String>never());
        
        Observable<String> src = oneItem.switchMap(new Function<String, Observable<String>>() {
            @Override
            public Observable<String> apply(final String s) {
                return Observable.just(s)
                        .mergeWith(Observable.interval(10, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, String>() {
                            @Override
                            public String apply(Long i) {
                                return s + " " + i;
                            }
                        })).take(250);
            }
        })
        .share()
        ;
        
        TestSubscriber<String> ts = new TestSubscriber<String>() {
            @Override
            public void onNext(String t) {
                super.onNext(t);
                if (valueCount() == 250) {
                    onComplete();
                    dispose();
                }
            }
        };
        src.subscribe(ts);
        
        ts.awaitTerminalEvent(10, TimeUnit.SECONDS);
        
        System.out.println("> testIssue2654: " + ts.valueCount());
        
        ts.assertTerminated();
        ts.assertNoErrors();
        
        Assert.assertEquals(250, ts.valueCount());
    }
    
    @Test(timeout = 10000)
    public void testInitialRequestsAreAdditive() {
        TestSubscriber<Long> ts = new TestSubscriber<>((Long)null);
        Observable.switchOnNext(
                Observable.interval(100, TimeUnit.MILLISECONDS)
                          .map(
                                new Function<Long, Observable<Long>>() {
                                    @Override
                                    public Observable<Long> apply(Long t) {
                                        return Observable.just(1L, 2L, 3L);
                                    }
                                }
                          ).take(3))
                          .subscribe(ts);
        ts.request(Long.MAX_VALUE - 100);
        ts.request(1);
        ts.awaitTerminalEvent();
    }
    
    @Test(timeout = 10000)
    public void testInitialRequestsDontOverflow() {
        TestSubscriber<Long> ts = new TestSubscriber<>((Long)null);
        Observable.switchOnNext(
                Observable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, Observable<Long>>() {
                            @Override
                            public Observable<Long> apply(Long t) {
                                return Observable.fromIterable(Arrays.asList(1L, 2L, 3L));
                            }
                        }).take(3)).subscribe(ts);
        ts.request(Long.MAX_VALUE - 1);
        ts.request(2);
        ts.awaitTerminalEvent();
        assertTrue(ts.valueCount() > 0);
    }
    
    
    @Test(timeout = 10000)
    public void testSecondaryRequestsDontOverflow() throws InterruptedException {
        TestSubscriber<Long> ts = new TestSubscriber<>((Long)null);
        Observable.switchOnNext(
                Observable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, Observable<Long>>() {
                            @Override
                            public Observable<Long> apply(Long t) {
                                return Observable.fromIterable(Arrays.asList(1L, 2L, 3L));
                            }
                        }).take(3)).subscribe(ts);
        ts.request(1);
        //we will miss two of the first observable
        Thread.sleep(250);
        ts.request(Long.MAX_VALUE - 1);
        ts.request(Long.MAX_VALUE - 1);
        ts.awaitTerminalEvent();
        ts.assertValueCount(7);
    }
    
    @Test(timeout = 10000)
    @Ignore("Request pattern changed and I can't decide if this is okay or not")
    public void testSecondaryRequestsAdditivelyAreMoreThanLongMaxValueInducesMaxValueRequestFromUpstream()
            throws InterruptedException {
        final List<Long> requests = new CopyOnWriteArrayList<>();

        TestSubscriber<Long> ts = new TestSubscriber<>(1L);
        Observable.switchOnNext(
                Observable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, Observable<Long>>() {
                            @Override
                            public Observable<Long> apply(Long t) {
                                return Observable.fromIterable(Arrays.asList(1L, 2L, 3L))
                                        .doOnRequest(v -> requests.add(v));
                            }
                        }).take(3)).subscribe(ts);
        // we will miss two of the first observables
        Thread.sleep(250);
        ts.request(Long.MAX_VALUE - 1);
        ts.request(Long.MAX_VALUE - 1);
        ts.awaitTerminalEvent();
        assertTrue(ts.valueCount() > 0);
        System.out.println(requests);
        assertEquals(5, requests.size());
        assertEquals(Long.MAX_VALUE, (long) requests.get(requests.size()-1));
    }

}