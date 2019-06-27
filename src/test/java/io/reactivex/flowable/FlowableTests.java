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

package io.reactivex.flowable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.*;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.*;

public class FlowableTests {

    Subscriber<Number> w;

    SingleObserver<Number> wo;

    MaybeObserver<Number> wm;

    private static final Predicate<Integer> IS_EVEN = new Predicate<Integer>() {
        @Override
        public boolean test(Integer v) {
            return v % 2 == 0;
        }
    };

    @Before
    public void before() {
        w = TestHelper.mockSubscriber();
        wo = TestHelper.mockSingleObserver();
        wm = TestHelper.mockMaybeObserver();
    }

    @Test
    public void fromArray() {
        String[] items = new String[] { "one", "two", "three" };
        assertEquals((Long)3L, Flowable.fromArray(items).count().blockingGet());
        assertEquals("two", Flowable.fromArray(items).skip(1).take(1).blockingSingle());
        assertEquals("three", Flowable.fromArray(items).takeLast(1).blockingSingle());
    }

    @Test
    public void fromIterable() {
        ArrayList<String> items = new ArrayList<String>();
        items.add("one");
        items.add("two");
        items.add("three");

        assertEquals((Long)3L, Flowable.fromIterable(items).count().blockingGet());
        assertEquals("two", Flowable.fromIterable(items).skip(1).take(1).blockingSingle());
        assertEquals("three", Flowable.fromIterable(items).takeLast(1).blockingSingle());
    }

    @Test
    public void fromArityArgs3() {
        Flowable<String> items = Flowable.just("one", "two", "three");

        assertEquals((Long)3L, items.count().blockingGet());
        assertEquals("two", items.skip(1).take(1).blockingSingle());
        assertEquals("three", items.takeLast(1).blockingSingle());
    }

    @Test
    public void fromArityArgs1() {
        Flowable<String> items = Flowable.just("one");

        assertEquals((Long)1L, items.count().blockingGet());
        assertEquals("one", items.takeLast(1).blockingSingle());
    }

    @Test
    public void testCreate() {

        Flowable<String> flowable = Flowable.just("one", "two", "three");

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testCountAFewItemsFlowable() {
        Flowable<String> flowable = Flowable.just("a", "b", "c", "d");

        flowable.count().toFlowable().subscribe(w);

        // we should be called only once
        verify(w).onNext(4L);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testCountZeroItemsFlowable() {
        Flowable<String> flowable = Flowable.empty();
        flowable.count().toFlowable().subscribe(w);
        // we should be called only once
        verify(w).onNext(0L);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testCountErrorFlowable() {
        Flowable<String> f = Flowable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new RuntimeException();
            }
        });

        f.count().toFlowable().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(any(RuntimeException.class));
    }

    @Test
    public void testCountAFewItems() {
        Flowable<String> flowable = Flowable.just("a", "b", "c", "d");

        flowable.count().subscribe(wo);

        // we should be called only once
        verify(wo).onSuccess(4L);
        verify(wo, never()).onError(any(Throwable.class));
    }

    @Test
    public void testCountZeroItems() {
        Flowable<String> flowable = Flowable.empty();
        flowable.count().subscribe(wo);
        // we should be called only once
        verify(wo).onSuccess(0L);
        verify(wo, never()).onError(any(Throwable.class));
    }

    @Test
    public void testCountError() {
        Flowable<String> f = Flowable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new RuntimeException();
            }
        });

        f.count().subscribe(wo);
        verify(wo, never()).onSuccess(anyInt());
        verify(wo, times(1)).onError(any(RuntimeException.class));
    }

    @Test
    public void testTakeFirstWithPredicateOfSome() {
        Flowable<Integer> flowable = Flowable.just(1, 3, 5, 4, 6, 3);
        flowable.filter(IS_EVEN).take(1).subscribe(w);
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(4);
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstWithPredicateOfNoneMatchingThePredicate() {
        Flowable<Integer> flowable = Flowable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        flowable.filter(IS_EVEN).take(1).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstOfSome() {
        Flowable<Integer> flowable = Flowable.just(1, 2, 3);
        flowable.take(1).subscribe(w);
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(1);
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstOfNone() {
        Flowable<Integer> flowable = Flowable.empty();
        flowable.take(1).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstOfNoneFlowable() {
        Flowable<Integer> flowable = Flowable.empty();
        flowable.firstElement().toFlowable().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstWithPredicateOfNoneMatchingThePredicateFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        flowable.filter(IS_EVEN).firstElement().toFlowable().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstOfNone() {
        Flowable<Integer> flowable = Flowable.empty();
        flowable.firstElement().subscribe(wm);
        verify(wm, never()).onSuccess(anyInt());
        verify(wm).onComplete();
        verify(wm, never()).onError(isA(NoSuchElementException.class));
    }

    @Test
    public void testFirstWithPredicateOfNoneMatchingThePredicate() {
        Flowable<Integer> flowable = Flowable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        flowable.filter(IS_EVEN).firstElement().subscribe(wm);
        verify(wm, never()).onSuccess(anyInt());
        verify(wm, times(1)).onComplete();
        verify(wm, never()).onError(isA(NoSuchElementException.class));
    }

    @Test
    public void testReduce() {
        Flowable<Integer> flowable = Flowable.just(1, 2, 3, 4);
        flowable.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .toFlowable()
        .subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(10);
    }

    @Test
    public void testReduceWithEmptyObservable() {
        Flowable<Integer> flowable = Flowable.range(1, 0);
        flowable.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .toFlowable()
        .test()
        .assertResult();
    }

    /**
     * A reduce on an empty Observable and a seed should just pass the seed through.
     *
     * This is confirmed at https://github.com/ReactiveX/RxJava/issues/423#issuecomment-27642456
     */
    @Test
    public void testReduceWithEmptyObservableAndSeed() {
        Flowable<Integer> flowable = Flowable.range(1, 0);
        int value = flowable.reduce(1, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .blockingGet();

        assertEquals(1, value);
    }

    @Test
    public void testReduceWithInitialValue() {
        Flowable<Integer> flowable = Flowable.just(1, 2, 3, 4);
        flowable.reduce(50, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .subscribe(wo);
        // we should be called only once
        verify(wo, times(1)).onSuccess(anyInt());
        verify(wo).onSuccess(60);
    }

    @Ignore("Throwing is not allowed from the unsafeCreate?!")
    @Test // FIXME throwing is not allowed from the create?!
    public void testOnSubscribeFails() {
        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        final RuntimeException re = new RuntimeException("bad impl");
        Flowable<String> f = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> s) { throw re; }
        });

        f.subscribe(subscriber);

        verify(subscriber, times(0)).onNext(anyString());
        verify(subscriber, times(0)).onComplete();
        verify(subscriber, times(1)).onError(re);
    }

    @Test
    public void testMaterializeDematerializeChaining() {
        Flowable<Integer> obs = Flowable.just(1);
        Flowable<Integer> chained = obs.materialize()
                .dematerialize(Functions.<Notification<Integer>>identity());

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        chained.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(1);
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, times(0)).onError(any(Throwable.class));
    }

    /**
     * The error from the user provided Observer is not handled by the subscribe method try/catch.
     *
     * It is handled by the AtomicObserver that wraps the provided Observer.
     *
     * Result: Passes (if AtomicObserver functionality exists)
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testCustomObservableWithErrorInObserverAsynchronous() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        // FIXME custom built???
        Flowable.just("1", "2", "three", "4")
        .subscribeOn(Schedulers.newThread())
        .safeSubscribe(new DefaultSubscriber<String>() {
            @Override
            public void onComplete() {
                System.out.println("completed");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                error.set(e);
                System.out.println("error");
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onNext(String v) {
                int num = Integer.parseInt(v);
                System.out.println(num);
                // doSomething(num);
                count.incrementAndGet();
            }

        });

        // wait for async sequence to complete
        latch.await();

        assertEquals(2, count.get());
        assertNotNull(error.get());
        if (!(error.get() instanceof NumberFormatException)) {
            fail("It should be a NumberFormatException");
        }
    }

    /**
     * The error from the user provided Observer is handled by the subscribe try/catch because this is synchronous.
     *
     * Result: Passes
     */
    @Test
    public void testCustomObservableWithErrorInObserverSynchronous() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        // FIXME custom built???
        Flowable.just("1", "2", "three", "4")
        .safeSubscribe(new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {
                System.out.println("completed");
            }

            @Override
            public void onError(Throwable e) {
                error.set(e);
                System.out.println("error");
                e.printStackTrace();
            }

            @Override
            public void onNext(String v) {
                int num = Integer.parseInt(v);
                System.out.println(num);
                // doSomething(num);
                count.incrementAndGet();
            }

        });
        assertEquals(2, count.get());
        assertNotNull(error.get());
        if (!(error.get() instanceof NumberFormatException)) {
            fail("It should be a NumberFormatException");
        }
    }

    /**
     * The error from the user provided Observable is handled by the subscribe try/catch because this is synchronous.
     *
     * Result: Passes
     */
    @Test
    public void testCustomObservableWithErrorInObservableSynchronous() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        // FIXME custom built???
        Flowable.just("1", "2").concatWith(Flowable.<String>error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new NumberFormatException();
            }
        }))
        .subscribe(new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {
                System.out.println("completed");
            }

            @Override
            public void onError(Throwable e) {
                error.set(e);
                System.out.println("error");
                e.printStackTrace();
            }

            @Override
            public void onNext(String v) {
                System.out.println(v);
                count.incrementAndGet();
            }

        });
        assertEquals(2, count.get());
        assertNotNull(error.get());
        if (!(error.get() instanceof NumberFormatException)) {
            fail("It should be a NumberFormatException");
        }
    }

    @Test
    public void testPublishLast() throws InterruptedException {
        final AtomicInteger count = new AtomicInteger();
        ConnectableFlowable<String> connectable = Flowable.<String>unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                count.incrementAndGet();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        subscriber.onNext("first");
                        subscriber.onNext("last");
                        subscriber.onComplete();
                    }
                }).start();
            }
        }).takeLast(1).publish();

        // subscribe once
        final CountDownLatch latch = new CountDownLatch(1);
        connectable.subscribe(new Consumer<String>() {
            @Override
            public void accept(String value) {
                assertEquals("last", value);
                latch.countDown();
            }
        });

        // subscribe twice
        connectable.subscribe();

        Disposable subscription = connectable.connect();
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(1, count.get());
        subscription.dispose();
    }

    @Test
    public void testReplay() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableFlowable<String> f = Flowable.<String>unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            counter.incrementAndGet();
                            subscriber.onNext("one");
                            subscriber.onComplete();
                        }
                    }).start();
            }
        }).replay();

        // we connect immediately and it will emit the value
        Disposable connection = f.connect();
        try {

            // we then expect the following 2 subscriptions to get that same value
            final CountDownLatch latch = new CountDownLatch(2);

            // subscribe once
            f.subscribe(new Consumer<String>() {
                @Override
                public void accept(String v) {
                    assertEquals("one", v);
                    latch.countDown();
                }
            });

            // subscribe again
            f.subscribe(new Consumer<String>() {
                @Override
                public void accept(String v) {
                    assertEquals("one", v);
                    latch.countDown();
                }
            });

            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }
            assertEquals(1, counter.get());
        } finally {
            connection.dispose();
        }
    }

    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Flowable<String> f = Flowable.<String>unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            counter.incrementAndGet();
                            subscriber.onNext("one");
                            subscriber.onComplete();
                        }
                    }).start();
            }
        }).cache();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        f.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        // subscribe again
        f.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void testCacheWithCapacity() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Flowable<String> f = Flowable.<String>unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        subscriber.onNext("one");
                        subscriber.onComplete();
                    }
                }).start();
            }
        }).cacheWithInitialCapacity(1);

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        f.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        // subscribe again
        f.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    /**
     * https://github.com/ReactiveX/RxJava/issues/198
     *
     * Rx Design Guidelines 5.2
     *
     * "when calling the Subscribe method that only has an onNext argument, the OnError behavior will be
     * to rethrow the exception on the thread that the message comes out from the Observable.
     * The OnCompleted behavior in this case is to do nothing."
     */
    @Test
    @Ignore("Subscribers can't throw")
    public void testErrorThrownWithoutErrorHandlerSynchronous() {
        try {
            Flowable.error(new RuntimeException("failure"))
            .subscribe();
            fail("expected exception");
        } catch (Throwable e) {
            assertEquals("failure", e.getMessage());
        }
    }

    /**
     * https://github.com/ReactiveX/RxJava/issues/198
     *
     * Rx Design Guidelines 5.2
     *
     * "when calling the Subscribe method that only has an onNext argument, the OnError behavior will be
     * to rethrow the exception on the thread that the message comes out from the Observable.
     * The OnCompleted behavior in this case is to do nothing."
     *
     * @throws InterruptedException if the await is interrupted
     */
    @Test
    @Ignore("Subscribers can't throw")
    public void testErrorThrownWithoutErrorHandlerAsynchronous() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        Flowable.unsafeCreate(new Publisher<Object>() {
            @Override
            public void subscribe(final Subscriber<? super Object> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            subscriber.onError(new Error("failure"));
                        } catch (Throwable e) {
                            // without an onError handler it has to just throw on whatever thread invokes it
                            exception.set(e);
                        }
                        latch.countDown();
                    }
                }).start();
            }
        }).subscribe();
        // wait for exception
        latch.await(3000, TimeUnit.MILLISECONDS);
        assertNotNull(exception.get());
        assertEquals("failure", exception.get().getMessage());
    }

    @Test
    public void testTakeWithErrorInObserver() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Flowable.just("1", "2", "three", "4").take(3)
        .safeSubscribe(new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {
                System.out.println("completed");
            }

            @Override
            public void onError(Throwable e) {
                error.set(e);
                System.out.println("error");
                e.printStackTrace();
            }

            @Override
            public void onNext(String v) {
                int num = Integer.parseInt(v);
                System.out.println(num);
                // doSomething(num);
                count.incrementAndGet();
            }

        });
        assertEquals(2, count.get());
        assertNotNull(error.get());
        if (!(error.get() instanceof NumberFormatException)) {
            fail("It should be a NumberFormatException");
        }
    }

    @Test
    public void testOfType() {
        Flowable<String> flowable = Flowable.just(1, "abc", false, 2L).ofType(String.class);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, never()).onNext(1);
        verify(subscriber, times(1)).onNext("abc");
        verify(subscriber, never()).onNext(false);
        verify(subscriber, never()).onNext(2L);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testOfTypeWithPolymorphism() {
        ArrayList<Integer> l1 = new ArrayList<Integer>();
        l1.add(1);
        LinkedList<Integer> l2 = new LinkedList<Integer>();
        l2.add(2);

        @SuppressWarnings("rawtypes")
        Flowable<List> flowable = Flowable.<Object> just(l1, l2, "123").ofType(List.class);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(l1);
        verify(subscriber, times(1)).onNext(l2);
        verify(subscriber, never()).onNext("123");
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testContainsFlowable() {
        Flowable<Boolean> flowable = Flowable.just("a", "b", "c").contains("b").toFlowable();

        FlowableSubscriber<Boolean> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(true);
        verify(subscriber, never()).onNext(false);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testContainsWithInexistenceFlowable() {
        Flowable<Boolean> flowable = Flowable.just("a", "b").contains("c").toFlowable();

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(false);
        verify(subscriber, never()).onNext(true);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    @Ignore("null values are not allowed")
    public void testContainsWithNullFlowable() {
        Flowable<Boolean> flowable = Flowable.just("a", "b", null).contains(null).toFlowable();

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(true);
        verify(subscriber, never()).onNext(false);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testContainsWithEmptyObservableFlowable() {
        Flowable<Boolean> flowable = Flowable.<String> empty().contains("a").toFlowable();

        FlowableSubscriber<Object> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(false);
        verify(subscriber, never()).onNext(true);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testContains() {
        Single<Boolean> single = Flowable.just("a", "b", "c").contains("b"); // FIXME nulls not allowed, changed to "c"

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onSuccess(false);
        verify(observer, never()).onError(
                any(Throwable.class));
    }

    @Test
    public void testContainsWithInexistence() {
        Single<Boolean> single = Flowable.just("a", "b").contains("c"); // FIXME null values are not allowed, removed

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(
                any(Throwable.class));
    }

    @Test
    @Ignore("null values are not allowed")
    public void testContainsWithNull() {
        Single<Boolean> single = Flowable.just("a", "b", null).contains(null);

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onSuccess(false);
        verify(observer, never()).onError(
                any(Throwable.class));
    }

    @Test
    public void testContainsWithEmptyObservable() {
        Single<Boolean> single = Flowable.<String> empty().contains("a");

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(
                any(Throwable.class));
    }

    @Test
    public void testIgnoreElementsFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 2, 3).ignoreElements().toFlowable();

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, never()).onNext(any(Integer.class));
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testIgnoreElements() {
        Completable completable = Flowable.just(1, 2, 3).ignoreElements();

        CompletableObserver observer = TestHelper.mockCompletableObserver();

        completable.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testJustWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        Flowable<Integer> flowable = Flowable.fromArray(1, 2).subscribeOn(scheduler);

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(1);
        inOrder.verify(subscriber, times(1)).onNext(2);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStartWithWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        Flowable<Integer> flowable = Flowable.just(3, 4).startWith(Arrays.asList(1, 2)).subscribeOn(scheduler);

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(1);
        inOrder.verify(subscriber, times(1)).onNext(2);
        inOrder.verify(subscriber, times(1)).onNext(3);
        inOrder.verify(subscriber, times(1)).onNext(4);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRangeWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        Flowable<Integer> flowable = Flowable.range(3, 4).subscribeOn(scheduler);

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(3);
        inOrder.verify(subscriber, times(1)).onNext(4);
        inOrder.verify(subscriber, times(1)).onNext(5);
        inOrder.verify(subscriber, times(1)).onNext(6);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMergeWith() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.just(1).mergeWith(Flowable.just(2)).subscribe(ts);
        ts.assertValues(1, 2);
    }

    @Test
    public void testConcatWith() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.just(1).concatWith(Flowable.just(2)).subscribe(ts);
        ts.assertValues(1, 2);
    }

    @Test
    public void testAmbWith() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.just(1).ambWith(Flowable.just(2)).subscribe(ts);
        ts.assertValue(1);
    }

    @Test
    public void testTakeWhileToList() {
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < expectedCount; i++) {
            Flowable
                    .just(Boolean.TRUE, Boolean.FALSE)
                    .takeWhile(new Predicate<Boolean>() {
                        @Override
                        public boolean test(Boolean v) {
                            return v;
                        }
                    })
                    .toList()
                    .doOnSuccess(new Consumer<List<Boolean>>() {
                        @Override
                        public void accept(List<Boolean> booleans) {
                            count.incrementAndGet();
                        }
                    })
                    .subscribe();
        }
        assertEquals(expectedCount, count.get());
    }

    @Test
    public void testCompose() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.just(1, 2, 3).compose(new FlowableTransformer<Integer, String>() {
            @Override
            public Publisher<String> apply(Flowable<Integer> t1) {
                return t1.map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer v) {
                        return String.valueOf(v);
                    }
                });
            }
        })
        .subscribe(ts);
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValues("1", "2", "3");
    }

    @Test
    public void testErrorThrownIssue1685() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            FlowableProcessor<Object> processor = ReplayProcessor.create();

            Flowable.error(new RuntimeException("oops"))
                .materialize()
                .delay(1, TimeUnit.SECONDS)
                .dematerialize(Functions.<Notification<Object>>identity())
                .subscribe(processor);

            processor.subscribe();
            processor.materialize().blockingFirst();

            System.out.println("Done");

            TestHelper.assertError(errors, 0, OnErrorNotImplementedException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void testEmptyIdentity() {
        assertEquals(Flowable.empty(), Flowable.empty());
    }

    @Test
    public void testEmptyIsEmpty() {
        Flowable.<Integer>empty().subscribe(w);

        verify(w).onComplete();
        verify(w, never()).onNext(any(Integer.class));
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test(expected = NullPointerException.class)
    public void testForEachWithNull() {
        Flowable.error(new Exception("boo"))
        //
        .forEach(null);
    }

    @Test
    public void testExtend() {
        final TestSubscriber<Object> subscriber = new TestSubscriber<Object>();
        final Object value = new Object();
        Object returned = Flowable.just(value).to(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> onSubscribe) {
                    onSubscribe.subscribe(subscriber);
                    subscriber.assertNoErrors();
                    subscriber.assertComplete();
                    subscriber.assertValue(value);
                    return subscriber.values().get(0);
                }
        });
        assertSame(returned, value);
    }

    @Test
    public void testAsExtend() {
        final TestSubscriber<Object> subscriber = new TestSubscriber<Object>();
        final Object value = new Object();
        Object returned = Flowable.just(value).as(new FlowableConverter<Object, Object>() {
            @Override
            public Object apply(Flowable<Object> onSubscribe) {
                    onSubscribe.subscribe(subscriber);
                    subscriber.assertNoErrors();
                    subscriber.assertComplete();
                    subscriber.assertValue(value);
                    return subscriber.values().get(0);
                }
        });
        assertSame(returned, value);
    }

    @Test
    public void as() {
        Flowable.just(1).as(new FlowableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Flowable<Integer> v) {
                return v.toObservable();
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void toObservableEmpty() {
        Flowable.empty().toObservable().test().assertResult();
    }

    @Test
    public void toObservableJust() {
        Flowable.just(1).toObservable().test().assertResult(1);
    }

    @Test
    public void toObservableRange() {
        Flowable.range(1, 5).toObservable().test().assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void toObservableError() {
        Flowable.error(new TestException()).toObservable().test().assertFailure(TestException.class);
    }

    @Test
    public void zipIterableObject() {
        @SuppressWarnings("unchecked")
        final List<Flowable<Integer>> flowables = Arrays.asList(Flowable.just(1, 2, 3), Flowable.just(1, 2, 3));
        Flowable.zip(flowables, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] o) throws Exception {
                int sum = 0;
                for (Object i : o) {
                    sum += (Integer) i;
                }
                return sum;
            }
        }).test().assertResult(2, 4, 6);
    }

    @Test
    public void combineLatestObject() {
        @SuppressWarnings("unchecked")
        final List<Flowable<Integer>> flowables = Arrays.asList(Flowable.just(1, 2, 3), Flowable.just(1, 2, 3));
        Flowable.combineLatest(flowables, new Function<Object[], Object>() {
            @Override
            public Object apply(final Object[] o) throws Exception {
                int sum = 1;
                for (Object i : o) {
                    sum *= (Integer) i;
                }
                return sum;
            }
        }).test().assertResult(3, 6, 9);
    }
}
