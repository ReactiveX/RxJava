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

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.InOrder;

import rx.*;
import rx.Observable;
import rx.Observer;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.schedulers.*;
import rx.subjects.PublishSubject;

public class OperatorSwitchTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private Observer<String> observer;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        observer = mock(Observer.class);
    }

    @Test
    public void testSwitchWhenOuterCompleteBeforeInner() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
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
        inOrder.verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSwitchWhenInnerCompleteBeforeOuter() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 10, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 0, "one");
                        publishNext(observer, 10, "two");
                        publishCompleted(observer, 20);
                    }
                }));

                publishNext(observer, 100, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
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
        inOrder.verify(observer, never()).onCompleted();
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        inOrder.verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSwitchWithComplete() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> observer) {
                        publishNext(observer, 60, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 200, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> observer) {
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
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("four");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithError() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 200, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
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
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testSwitchWithSubsequenceComplete() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishCompleted(observer, 0);
                    }
                }));

                publishNext(observer, 150, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
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
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithSubsequenceError() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishError(observer, 0, new TestException());
                    }
                }));

                publishNext(observer, 150, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
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
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext("three");
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    private <T> void publishCompleted(final Observer<T> observer, long delay) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Observer<T> observer, long delay, final Throwable error) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Observer<T> observer, long delay, final T value) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSwitchIssue737() {
        // https://github.com/ReactiveX/RxJava/issues/737
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 0, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 10, "1-one");
                        publishNext(observer, 20, "1-two");
                        // The following events will be ignored
                        publishNext(observer, 30, "1-three");
                        publishCompleted(observer, 40);
                    }
                }));
                publishNext(observer, 25, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
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
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testBackpressure() {
        final Observable<String> o1 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> observer) {
                observer.setProducer(new Producer() {

                    private int emitted = 0;

                    @Override
                    public void request(long n) {
                        for(int i = 0; i < n && emitted < 10 && !observer.isUnsubscribed(); i++) {
                            scheduler.advanceTimeBy(5, TimeUnit.MILLISECONDS);
                            emitted++;
                            observer.onNext("a" + emitted);
                        }
                        if(emitted == 10) {
                            observer.onCompleted();
                        }
                    }
                });
            }
        });
        final Observable<String> o2 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> observer) {
                observer.setProducer(new Producer() {

                    private int emitted = 0;

                    @Override
                    public void request(long n) {
                        for(int i = 0; i < n && emitted < 10 && !observer.isUnsubscribed(); i++) {
                            scheduler.advanceTimeBy(5, TimeUnit.MILLISECONDS);
                            emitted++;
                            observer.onNext("b" + emitted);
                        }
                        if(emitted == 10) {
                            observer.onCompleted();
                        }
                    }
                });
            }
        });
        final Observable<String> o3 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> observer) {
                observer.setProducer(new Producer() {

                    private int emitted = 0;

                    @Override
                    public void request(long n) {
                        for(int i = 0; i < n && emitted < 10 && !observer.isUnsubscribed(); i++) {
                            emitted++;
                            observer.onNext("c" + emitted);
                        }
                        if(emitted == 10) {
                            observer.onCompleted();
                        }
                    }
                });
            }
        });
        Observable<Observable<String>> o = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 10, o1);
                publishNext(observer, 20, o2);
                publishNext(observer, 30, o3);
                publishCompleted(observer, 30);
            }
        });
        final TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        Observable.switchOnNext(o).subscribe(new Subscriber<String>() {

            private int requested = 0;

            @Override
            public void onStart() {
                requested = 3;
                request(3);
            }

            @Override
            public void onCompleted() {
                testSubscriber.onCompleted();
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
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
        testSubscriber.assertReceivedOnNext(Arrays.asList("a1", "b1", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10"));
        testSubscriber.assertNoErrors();
        testSubscriber.assertTerminalEvent();
    }

    @Test
    public void testUnsubscribe() {
        final AtomicBoolean isUnsubscribed = new AtomicBoolean();
        Observable.switchOnNext(
                Observable.create(new Observable.OnSubscribe<Observable<Integer>>() {
                    @Override
                    public void call(final Subscriber<? super Observable<Integer>> subscriber) {
                        subscriber.onNext(Observable.just(1));
                        isUnsubscribed.set(subscriber.isUnsubscribed());
                    }
                })
        ).take(1).subscribe();
        assertTrue("Switch doesn't propagate 'unsubscribe'", isUnsubscribed.get());
    }
    /** The upstream producer hijacked the switch producer stopping the requests aimed at the inner observables. */
    @Test
    public void testIssue2654() {
        Observable<String> oneItem = Observable.just("Hello").mergeWith(Observable.<String>never());
        
        Observable<String> src = oneItem.switchMap(new Func1<String, Observable<String>>() {
            @Override
            public Observable<String> call(final String s) {
                return Observable.just(s)
                        .mergeWith(Observable.interval(10, TimeUnit.MILLISECONDS)
                        .map(new Func1<Long, String>() {
                            @Override
                            public String call(Long i) {
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
                if (getOnNextEvents().size() == 250) {
                    onCompleted();
                    unsubscribe();
                }
            }
        };
        src.subscribe(ts);
        
        ts.awaitTerminalEvent(10, TimeUnit.SECONDS);
        
        System.out.println("> testIssue2654: " + ts.getOnNextEvents().size());
        
        ts.assertTerminalEvent();
        ts.assertNoErrors();
        
        Assert.assertEquals(250, ts.getOnNextEvents().size());
    }
    
    @Test(timeout = 10000)
    public void testInitialRequestsAreAdditive() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(0);
        Observable.switchOnNext(
                Observable.interval(100, TimeUnit.MILLISECONDS)
                          .map(
                                new Func1<Long, Observable<Long>>() {
                                    @Override
                                    public Observable<Long> call(Long t) {
                                        return Observable.just(1L, 2L, 3L);
                                    }
                                }
                          ).take(3))
                          .subscribe(ts);
        ts.requestMore(Long.MAX_VALUE - 100);
        ts.requestMore(1);
        ts.awaitTerminalEvent();
    }
    
    @Test(timeout = 10000)
    public void testInitialRequestsDontOverflow() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(0);
        Observable.switchOnNext(
                Observable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Func1<Long, Observable<Long>>() {
                            @Override
                            public Observable<Long> call(Long t) {
                                return Observable.from(Arrays.asList(1L, 2L, 3L));
                            }
                        }).take(3)).subscribe(ts);
        ts.requestMore(Long.MAX_VALUE - 1);
        ts.requestMore(2);
        ts.awaitTerminalEvent();
        assertTrue(ts.getOnNextEvents().size() > 0);
    }
    
    
    @Test(timeout = 10000)
    public void testSecondaryRequestsDontOverflow() throws InterruptedException {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(0);
        Observable.switchOnNext(
                Observable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Func1<Long, Observable<Long>>() {
                            @Override
                            public Observable<Long> call(Long t) {
                                return Observable.from(Arrays.asList(1L, 2L, 3L));
                            }
                        }).take(3)).subscribe(ts);
        ts.requestMore(1);
        //we will miss two of the first observable
        Thread.sleep(250);
        ts.requestMore(Long.MAX_VALUE - 1);
        ts.requestMore(Long.MAX_VALUE - 1);
        ts.awaitTerminalEvent();
        ts.assertValueCount(7);
    }
    
    @Test(timeout = 10000)
    public void testSecondaryRequestsAdditivelyAreMoreThanLongMaxValueInducesMaxValueRequestFromUpstream()
            throws InterruptedException {
        final List<Long> requests = new CopyOnWriteArrayList<Long>();
        final Action1<Long> addRequest = new Action1<Long>() {

            @Override
            public void call(Long n) {
                requests.add(n);
            }
        };
        TestSubscriber<Long> ts = new TestSubscriber<Long>(1);
        Observable.switchOnNext(
                Observable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Func1<Long, Observable<Long>>() {
                            @Override
                            public Observable<Long> call(Long t) {
                                return Observable.from(Arrays.asList(1L, 2L, 3L)).doOnRequest(
                                        addRequest);
                            }
                        }).take(3)).subscribe(ts);
        // we will miss two of the first observables
        Thread.sleep(250);
        ts.requestMore(Long.MAX_VALUE - 1);
        ts.requestMore(Long.MAX_VALUE - 1);
        ts.awaitTerminalEvent();
        assertTrue(ts.getOnNextEvents().size() > 0);
        assertEquals(4, requests.size()); // depends on the request pattern
        assertEquals(Long.MAX_VALUE, (long) requests.get(requests.size()-1));
    }

    @Test
    public void mainError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        source.switchMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);
        
        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());
        
        ts.assertValues(1, 2, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(0, 3).switchMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return v == 1 ? Observable.<Integer>error(new TestException()) : Observable.range(v, 2);
            }
        }).subscribe(ts);
        
        ts.assertValues(0, 1, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerAllError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(0, 3).switchMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2).concatWith(Observable.<Integer>error(new TestException()));
            }
        }).subscribe(ts);
        
        ts.assertValues(0, 1, 1, 2, 2, 3);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();
        
        List<Throwable> exceptions = ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions();
        
        assertEquals(3, exceptions.size());
        
        for (Throwable ex : exceptions) {
            assertTrue(ex.toString(), ex instanceof TestException);
        }
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        Observable.range(0, 3).switchMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(2);
        
        ts.assertValues(2, 3);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void backpressureWithSwitch() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishSubject<Integer> source = PublishSubject.create();

        source.switchMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        source.onNext(0);
        
        ts.assertValues(0);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        source.onNext(1);

        ts.assertValues(0);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValues(0, 1);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        source.onNext(2);

        ts.requestMore(2);

        source.onCompleted();
        
        ts.assertValues(0, 1, 2, 3);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    Object ref;
    
    @Test
    public void producerIsNotRetained() throws Exception {
        ref = new Object();
        
        WeakReference<Object> wr = new WeakReference<Object>(ref);
        
        PublishSubject<Observable<Object>> ps = PublishSubject.create();
        
        Subscriber<Object> observer = new Subscriber<Object>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Object t) {
            }
        };
        
        Observable.switchOnNext(ps).subscribe(observer);
        
        ps.onNext(Observable.just(ref));
        
        ref = null;
        
        System.gc();
        
        Thread.sleep(500);
        
        Assert.assertNotNull(observer); // retain every other referenec in the pipeline
        Assert.assertNotNull(ps);
        Assert.assertNull("Object retained!", wr.get());
    }

    @Test
    public void switchAsyncHeavily() {
        for (int i = 1; i < 1024; i *= 2) {
            System.out.println("switchAsyncHeavily >> " + i);
            
            final Queue<Throwable> q = new ConcurrentLinkedQueue<Throwable>();
            
            final long[] lastSeen = { 0L };
            
            final int j = i;
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>(i) {
                int count;
                @Override
                public void onNext(Integer t) {
                    super.onNext(t);
                    lastSeen[0] = System.currentTimeMillis();
                    if (++count == j) {
                        count = 0;
                        requestMore(j);
                    }
                }
            };
            
            Observable.range(1, 25000)
            .observeOn(Schedulers.computation(), i)
            .switchMap(new Func1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Integer v) {
                    return Observable.range(1, 1000).observeOn(Schedulers.computation(), j)
                            .doOnError(new Action1<Throwable>() {
                                @Override
                                public void call(Throwable e) {
                                    q.add(e);
                                }
                            });
                }
            })
            .timeout(20, TimeUnit.SECONDS)
            .subscribe(ts);
            
            ts.awaitTerminalEvent(45, TimeUnit.SECONDS);
            if (!q.isEmpty()) {
                throw new AssertionError("Dropped exceptions", new CompositeException(q));
            }
            ts.assertNoErrors();
            if (ts.getOnCompletedEvents().size() == 0) {
                fail("switchAsyncHeavily timed out @ " + j + " (" + ts.getOnNextEvents().size() + " onNexts received, last was " + (System.currentTimeMillis() - lastSeen[0]) + " ms ago");
            }
        }
    }
    
    @Test
    public void asyncInner() throws Throwable {
        for (int i = 0; i < 100; i++) {
            
            final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
            
            Observable.just(Observable.range(1, 1000 * 1000).subscribeOn(Schedulers.computation()))
            .switchMap(UtilityFunctions.<Observable<Integer>>identity())
            .observeOn(Schedulers.computation())
            .ignoreElements()
            .timeout(5, TimeUnit.SECONDS)
            .toBlocking()
            .subscribe(Actions.empty(), new Action1<Throwable>() {
                @Override
                public void call(Throwable e) {
                    error.set(e);
                }
            });
            
            Throwable ex = error.get();
            if (ex != null) {
                throw ex;
            }
        }
    }
}
