/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.*;

public class FlowableSwitchTest {

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
        Flowable<Flowable<String>> source = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(new BooleanSubscription());
                publishNext(observer, 50, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 70, "one");
                        publishNext(observer, 100, "two");
                        publishCompleted(observer, 200);
                    }
                }));
                publishCompleted(observer, 60);
            }
        });

        Flowable<String> sampled = Flowable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(2)).onNext(anyString());
        inOrder.verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSwitchWhenInnerCompleteBeforeOuter() {
        Flowable<Flowable<String>> source = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(new BooleanSubscription());
                publishNext(observer, 10, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 0, "one");
                        publishNext(observer, 10, "two");
                        publishCompleted(observer, 20);
                    }
                }));

                publishNext(observer, 100, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 0, "three");
                        publishNext(observer, 10, "four");
                        publishCompleted(observer, 20);
                    }
                }));
                publishCompleted(observer, 200);
            }
        });

        Flowable<String> sampled = Flowable.switchOnNext(source);
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
        Flowable<Flowable<String>> source = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(new BooleanSubscription());
                publishNext(observer, 50, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(final Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 60, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 200, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(final Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 0, "three");
                        publishNext(observer, 100, "four");
                    }
                }));

                publishCompleted(observer, 250);
            }
        });

        Flowable<String> sampled = Flowable.switchOnNext(source);
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
        Flowable<Flowable<String>> source = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(new BooleanSubscription());
                publishNext(observer, 50, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(final Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 200, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 0, "three");
                        publishNext(observer, 100, "four");
                    }
                }));

                publishError(observer, 250, new TestException());
            }
        });

        Flowable<String> sampled = Flowable.switchOnNext(source);
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
        Flowable<Flowable<String>> source = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(new BooleanSubscription());
                publishNext(observer, 50, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishCompleted(observer, 0);
                    }
                }));

                publishNext(observer, 150, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 50, "three");
                    }
                }));
            }
        });

        Flowable<String> sampled = Flowable.switchOnNext(source);
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
        Flowable<Flowable<String>> source = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(new BooleanSubscription());
                publishNext(observer, 50, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishError(observer, 0, new TestException());
                    }
                }));

                publishNext(observer, 150, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 50, "three");
                    }
                }));

            }
        });

        Flowable<String> sampled = Flowable.switchOnNext(source);
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
        Flowable<Flowable<String>> source = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(new BooleanSubscription());
                publishNext(observer, 0, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 10, "1-one");
                        publishNext(observer, 20, "1-two");
                        // The following events will be ignored
                        publishNext(observer, 30, "1-three");
                        publishCompleted(observer, 40);
                    }
                }));
                publishNext(observer, 25, Flowable.unsafeCreate(new Publisher<String>() {
                    @Override
                    public void subscribe(Subscriber<? super String> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        publishNext(observer, 10, "2-one");
                        publishNext(observer, 20, "2-two");
                        publishNext(observer, 30, "2-three");
                        publishCompleted(observer, 40);
                    }
                }));
                publishCompleted(observer, 30);
            }
        });

        Flowable<String> sampled = Flowable.switchOnNext(source);
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

        PublishProcessor<String> o1 = PublishProcessor.create();
        PublishProcessor<String> o2 = PublishProcessor.create();
        PublishProcessor<String> o3 = PublishProcessor.create();

        PublishProcessor<PublishProcessor<String>> o = PublishProcessor.create();

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


        final TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        Flowable.switchOnNext(o).subscribe(new DefaultSubscriber<String>() {

            private int requested;

            @Override
            public void onStart() {
                requested = 3;
                request(3);
                testSubscriber.onSubscribe(new BooleanSubscription());
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
                if (requested == 0) {
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
        Flowable.switchOnNext(
                Flowable.unsafeCreate(new Publisher<Flowable<Integer>>() {
                    @Override
                    public void subscribe(final Subscriber<? super Flowable<Integer>> subscriber) {
                        BooleanSubscription bs = new BooleanSubscription();
                        subscriber.onSubscribe(bs);
                        subscriber.onNext(Flowable.just(1));
                        isUnsubscribed.set(bs.isCancelled());
                    }
                })
        ).take(1).subscribe();
        assertTrue("Switch doesn't propagate 'unsubscribe'", isUnsubscribed.get());
    }
    /** The upstream producer hijacked the switch producer stopping the requests aimed at the inner observables. */
    @Test
    public void testIssue2654() {
        Flowable<String> oneItem = Flowable.just("Hello").mergeWith(Flowable.<String>never());

        Flowable<String> src = oneItem.switchMap(new Function<String, Flowable<String>>() {
            @Override
            public Flowable<String> apply(final String s) {
                return Flowable.just(s)
                        .mergeWith(Flowable.interval(10, TimeUnit.MILLISECONDS)
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
        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        Flowable.switchOnNext(
                Flowable.interval(100, TimeUnit.MILLISECONDS)
                          .map(
                                new Function<Long, Flowable<Long>>() {
                                    @Override
                                    public Flowable<Long> apply(Long t) {
                                        return Flowable.just(1L, 2L, 3L);
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
        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        Flowable.switchOnNext(
                Flowable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, Flowable<Long>>() {
                            @Override
                            public Flowable<Long> apply(Long t) {
                                return Flowable.fromIterable(Arrays.asList(1L, 2L, 3L)).hide();
                            }
                        }).take(3)).subscribe(ts);
        ts.request(Long.MAX_VALUE - 1);
        ts.request(2);
        ts.awaitTerminalEvent();
        assertTrue(ts.valueCount() > 0);
    }


    @Test(timeout = 10000)
    public void testSecondaryRequestsDontOverflow() throws InterruptedException {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        Flowable.switchOnNext(
                Flowable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, Flowable<Long>>() {
                            @Override
                            public Flowable<Long> apply(Long t) {
                                return Flowable.fromIterable(Arrays.asList(1L, 2L, 3L)).hide();
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
        final List<Long> requests = new CopyOnWriteArrayList<Long>();

        TestSubscriber<Long> ts = new TestSubscriber<Long>(1L);
        Flowable.switchOnNext(
                Flowable.interval(100, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, Flowable<Long>>() {
                            @Override
                            public Flowable<Long> apply(Long t) {
                                return Flowable.fromIterable(Arrays.asList(1L, 2L, 3L))
                                        .doOnRequest(new LongConsumer() {
                                            @Override
                                            public void accept(long v) {
                                                requests.add(v);
                                            }
                                        });
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
        assertEquals(Long.MAX_VALUE, (long) requests.get(requests.size() - 1));
    }

    @Test
    public void delayErrors() {
        PublishProcessor<Publisher<Integer>> source = PublishProcessor.create();

        TestSubscriber<Integer> ts = source.switchMapDelayError(Functions.<Publisher<Integer>>identity())
        .test();

        ts.assertNoValues()
        .assertNoErrors()
        .assertNotComplete();

        source.onNext(Flowable.just(1));

        source.onNext(Flowable.<Integer>error(new TestException("Forced failure 1")));

        source.onNext(Flowable.just(2, 3, 4));

        source.onNext(Flowable.<Integer>error(new TestException("Forced failure 2")));

        source.onNext(Flowable.just(5));

        source.onError(new TestException("Forced failure 3"));

        ts.assertValues(1, 2, 3, 4, 5)
        .assertNotComplete()
        .assertError(CompositeException.class);

        List<Throwable> errors = ExceptionHelper.flatten(ts.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Forced failure 1");
        TestHelper.assertError(errors, 1, TestException.class, "Forced failure 2");
        TestHelper.assertError(errors, 2, TestException.class, "Forced failure 3");
    }

    @Test
    public void switchOnNextPrefetch() {
        final List<Integer> list = new ArrayList<Integer>();

        Flowable<Integer> source = Flowable.range(1, 10).hide().doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        });

        Flowable.switchOnNext(Flowable.just(source).hide(), 2)
        .test(1);

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void switchOnNextDelayError() {
        final List<Integer> list = new ArrayList<Integer>();

        Flowable<Integer> source = Flowable.range(1, 10).hide().doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        });

        Flowable.switchOnNextDelayError(Flowable.just(source).hide())
        .test(1);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), list);
    }

    @Test
    public void switchOnNextDelayErrorPrefetch() {
        final List<Integer> list = new ArrayList<Integer>();

        Flowable<Integer> source = Flowable.range(1, 10).hide().doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        });

        Flowable.switchOnNextDelayError(Flowable.just(source).hide(), 2)
        .test(1);

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void switchOnNextDelayErrorWithError() {
        PublishProcessor<Flowable<Integer>> ps = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.switchOnNextDelayError(ps).test();

        ps.onNext(Flowable.just(1));
        ps.onNext(Flowable.<Integer>error(new TestException()));
        ps.onNext(Flowable.range(2, 4));
        ps.onComplete();

        ts.assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void switchOnNextDelayErrorBufferSize() {
        PublishProcessor<Flowable<Integer>> ps = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.switchOnNextDelayError(ps, 2).test();

        ps.onNext(Flowable.just(1));
        ps.onNext(Flowable.range(2, 4));
        ps.onComplete();

        ts.assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void switchMapDelayErrorEmptySource() {
        assertSame(Flowable.empty(), Flowable.<Object>empty()
                .switchMapDelayError(new Function<Object, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Object v) throws Exception {
                        return Flowable.just(1);
                    }
                }, 16));
    }

    @Test
    public void switchMapDelayErrorJustSource() {
        Flowable.just(0)
        .switchMapDelayError(new Function<Object, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Object v) throws Exception {
                return Flowable.just(1);
            }
        }, 16)
        .test()
        .assertResult(1);

    }

    @Test
    public void switchMapErrorEmptySource() {
        assertSame(Flowable.empty(), Flowable.<Object>empty()
                .switchMap(new Function<Object, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Object v) throws Exception {
                        return Flowable.just(1);
                    }
                }, 16));
    }

    @Test
    public void switchMapJustSource() {
        Flowable.just(0)
        .switchMap(new Function<Object, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Object v) throws Exception {
                return Flowable.just(1);
            }
        }, 16)
        .test()
        .assertResult(1);

    }

    @Test
    public void switchMapInnerCancelled() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.just(1)
                .switchMap(Functions.justFunction(pp))
                .test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.switchOnNext(
                Flowable.just(Flowable.just(1)).hide()));
    }

    @Test
    public void nextSourceErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {

                final PublishProcessor<Integer> ps1 = PublishProcessor.create();
                final PublishProcessor<Integer> ps2 = PublishProcessor.create();

                ps1.switchMap(new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer v) throws Exception {
                        if (v == 1) {
                            return ps2;
                        }
                        return Flowable.never();
                    }
                })
                .test();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onNext(2);
                    }
                };

                final TestException ex = new TestException();

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex);
                    }
                };

                TestHelper.race(r1, r2);

                for (Throwable e : errors) {
                    assertTrue(e.toString(), e instanceof TestException);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void outerInnerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {

                final PublishProcessor<Integer> ps1 = PublishProcessor.create();
                final PublishProcessor<Integer> ps2 = PublishProcessor.create();

                ps1.switchMap(new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer v) throws Exception {
                        if (v == 1) {
                            return ps2;
                        }
                        return Flowable.never();
                    }
                })
                .test();

                final TestException ex1 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };

                final TestException ex2 = new TestException();

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2);

                for (Throwable e : errors) {
                    assertTrue(e.toString(), e instanceof TestException);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps1 = PublishProcessor.create();

            final TestSubscriber<Integer> to = ps1.switchMap(new Function<Integer, Flowable<Integer>>() {
                @Override
                public Flowable<Integer> apply(Integer v) throws Exception {
                    return Flowable.never();
                }
            })
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onNext(2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void mapperThrows() {
        Flowable.just(1).hide()
        .switchMap(new Function<Integer, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badMainSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onComplete();
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .switchMap(Functions.justFunction(Flowable.never()))
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void emptyInner() {
        Flowable.range(1, 5)
        .switchMap(Functions.justFunction(Flowable.empty()))
        .test()
        .assertResult();
    }

    @Test
    public void justInner() {
        Flowable.range(1, 5)
        .switchMap(Functions.justFunction(Flowable.just(1)))
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void badInnerSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.just(1).hide()
            .switchMap(Functions.justFunction(new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onError(new TestException());
                    observer.onComplete();
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }))
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void innerCompletesReentrant() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ps.onComplete();
            }
        };

        Flowable.just(1).hide()
        .switchMap(Functions.justFunction(ps))
        .subscribe(to);

        ps.onNext(1);

        to.assertResult(1);
    }

    @Test
    public void innerErrorsReentrant() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ps.onError(new TestException());
            }
        };

        Flowable.just(1).hide()
        .switchMap(Functions.justFunction(ps))
        .subscribe(to);

        ps.onNext(1);

        to.assertFailure(TestException.class, 1);
    }

    @Test
    public void scalarMap() {
        Flowable.switchOnNext(Flowable.just(Flowable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void scalarMapDelayError() {
        Flowable.switchOnNextDelayError(Flowable.just(Flowable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void scalarXMap() {
        Flowable.fromCallable(Functions.justCallable(1))
        .switchMap(Functions.justFunction(Flowable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.switchMap(Functions.justFunction(Flowable.just(1)));
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void innerOverflow() {
        Flowable.just(1).hide()
        .switchMap(Functions.justFunction(new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < 10; i++) {
                    s.onNext(i);
                }
            }
        }), 8)
        .test(1L)
        .assertFailure(MissingBackpressureException.class, 0);
    }

    @Test
    public void drainCancelRace() {
        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            Flowable.just(1).hide()
            .switchMap(Functions.justFunction(pp))
            .subscribe(ts);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void fusedInnerCrash() {
        Flowable.just(1).hide()
        .switchMap(Functions.justFunction(Flowable.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                throw new TestException();
            }
        })))
        .test()
        .assertFailure(TestException.class);
    }
}
