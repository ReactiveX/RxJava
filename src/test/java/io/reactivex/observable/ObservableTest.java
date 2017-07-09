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

package io.reactivex.observable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.*;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.*;

public class ObservableTest {

    Observer<Number> w;
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
        w = TestHelper.mockObserver();
        wo = TestHelper.mockSingleObserver();
        wm = TestHelper.mockMaybeObserver();
    }

    @Test
    public void fromArray() {
        String[] items = new String[] { "one", "two", "three" };
        assertEquals((Long)3L, Observable.fromArray(items).count().blockingGet());
        assertEquals("two", Observable.fromArray(items).skip(1).take(1).blockingSingle());
        assertEquals("three", Observable.fromArray(items).takeLast(1).blockingSingle());
    }

    @Test
    public void fromIterable() {
        ArrayList<String> items = new ArrayList<String>();
        items.add("one");
        items.add("two");
        items.add("three");

        assertEquals((Long)3L, Observable.fromIterable(items).count().blockingGet());
        assertEquals("two", Observable.fromIterable(items).skip(1).take(1).blockingSingle());
        assertEquals("three", Observable.fromIterable(items).takeLast(1).blockingSingle());
    }

    @Test
    public void fromArityArgs3() {
        Observable<String> items = Observable.just("one", "two", "three");

        assertEquals((Long)3L, items.count().blockingGet());
        assertEquals("two", items.skip(1).take(1).blockingSingle());
        assertEquals("three", items.takeLast(1).blockingSingle());
    }

    @Test
    public void fromArityArgs1() {
        Observable<String> items = Observable.just("one");

        assertEquals((Long)1L, items.count().blockingGet());
        assertEquals("one", items.takeLast(1).blockingSingle());
    }

    @Test
    public void testCreate() {

        Observable<String> o = Observable.just("one", "two", "three");

        Observer<String> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testCountAFewItemsObservable() {
        Observable<String> o = Observable.just("a", "b", "c", "d");

        o.count().toObservable().subscribe(w);

        // we should be called only once
        verify(w, times(1)).onNext(anyLong());
        verify(w).onNext(4L);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testCountZeroItemsObservable() {
        Observable<String> o = Observable.empty();
        o.count().toObservable().subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyLong());
        verify(w).onNext(0L);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testCountErrorObservable() {
        Observable<String> o = Observable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new RuntimeException();
            }
        });

        o.count().toObservable().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(any(RuntimeException.class));
    }


    @Test
    public void testCountAFewItems() {
        Observable<String> o = Observable.just("a", "b", "c", "d");

        o.count().subscribe(wo);

        // we should be called only once
        verify(wo, times(1)).onSuccess(anyLong());
        verify(wo).onSuccess(4L);
        verify(wo, never()).onError(any(Throwable.class));
    }

    @Test
    public void testCountZeroItems() {
        Observable<String> o = Observable.empty();
        o.count().subscribe(wo);
        // we should be called only once
        verify(wo, times(1)).onSuccess(anyLong());
        verify(wo).onSuccess(0L);
        verify(wo, never()).onError(any(Throwable.class));
    }

    @Test
    public void testCountError() {
        Observable<String> o = Observable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new RuntimeException();
            }
        });

        o.count().subscribe(wo);
        verify(wo, never()).onSuccess(anyInt());
        verify(wo, times(1)).onError(any(RuntimeException.class));
    }

    @Test
    public void testTakeFirstWithPredicateOfSome() {
        Observable<Integer> o = Observable.just(1, 3, 5, 4, 6, 3);
        o.filter(IS_EVEN).take(1).subscribe(w);
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(4);
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstWithPredicateOfNoneMatchingThePredicate() {
        Observable<Integer> o = Observable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        o.filter(IS_EVEN).take(1).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstOfSome() {
        Observable<Integer> o = Observable.just(1, 2, 3);
        o.take(1).subscribe(w);
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(1);
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstOfNone() {
        Observable<Integer> o = Observable.empty();
        o.take(1).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstOfNone() {
        Observable<Integer> o = Observable.empty();
        o.firstElement().subscribe(wm);
        verify(wm, never()).onSuccess(anyInt());
        verify(wm).onComplete();
        verify(wm, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstWithPredicateOfNoneMatchingThePredicate() {
        Observable<Integer> o = Observable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        o.filter(IS_EVEN).firstElement().subscribe(wm);
        verify(wm, never()).onSuccess(anyInt());
        verify(wm).onComplete();
        verify(wm, never()).onError(any(Throwable.class));
    }

    @Test
    public void testReduce() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4);
        o.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .subscribe(wm);
        // we should be called only once
        verify(wm, times(1)).onSuccess(anyInt());
        verify(wm).onSuccess(10);
        verify(wm, never()).onError(any(Throwable.class));
        verify(wm, never()).onComplete();
    }

    @Test
    public void testReduceObservable() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4);
        o.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .toObservable()
        .subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(10);
        verify(w, never()).onError(any(Throwable.class));
        verify(w).onComplete();
    }

    @Test
    public void testReduceWithEmptyObservable() {
        Observable<Integer> o = Observable.range(1, 0);
        o.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .toObservable()
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
        Observable<Integer> o = Observable.range(1, 0);
        int value = o.reduce(1, new BiFunction<Integer, Integer, Integer>() {
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
        Observable<Integer> o = Observable.just(1, 2, 3, 4);
        o.reduce(50, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .subscribe(wo);
        // we should be called only once
        verify(wo, times(1)).onSuccess(anyInt());
        verify(wo).onSuccess(60);
        verify(wo, never()).onError(any(Throwable.class));
    }

    @Test
    public void testReduceWithInitialValueObservable() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4);
        o.reduce(50, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .toObservable()
        .subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(60);
    }

    @Ignore("Throwing is not allowed from the unsafeCreate?!")
    @Test // FIXME throwing is not allowed from the create?!
    public void testOnSubscribeFails() {
        Observer<String> observer = TestHelper.mockObserver();

        final RuntimeException re = new RuntimeException("bad impl");
        Observable<String> o = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> s) { throw re; }
        });

        o.subscribe(observer);
        verify(observer, times(0)).onNext(anyString());
        verify(observer, times(0)).onComplete();
        verify(observer, times(1)).onError(re);
    }

    @Test
    public void testMaterializeDematerializeChaining() {
        Observable<Integer> obs = Observable.just(1);
        Observable<Integer> chained = obs.materialize().dematerialize();

        Observer<Integer> observer = TestHelper.mockObserver();

        chained.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onComplete();
        verify(observer, times(0)).onError(any(Throwable.class));
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
        Observable.just("1", "2", "three", "4")
        .subscribeOn(Schedulers.newThread())
        .safeSubscribe(new DefaultObserver<String>() {
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
        Observable.just("1", "2", "three", "4")
        .safeSubscribe(new DefaultObserver<String>() {

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
     *
     * Result: Passes
     */
    @Test
    public void testCustomObservableWithErrorInObservableSynchronous() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        // FIXME custom built???
        Observable.just("1", "2").concatWith(Observable.<String>error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new NumberFormatException();
            }
        }))
        .subscribe(new DefaultObserver<String>() {

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
        ConnectableObservable<String> connectable = Observable.<String>unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(final Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                count.incrementAndGet();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        observer.onNext("first");
                        observer.onNext("last");
                        observer.onComplete();
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
        ConnectableObservable<String> o = Observable.<String>unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(final Observer<? super String> observer) {
                    observer.onSubscribe(Disposables.empty());
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            counter.incrementAndGet();
                            observer.onNext("one");
                            observer.onComplete();
                        }
                    }).start();
            }
        }).replay();

        // we connect immediately and it will emit the value
        Disposable s = o.connect();
        try {

            // we then expect the following 2 subscriptions to get that same value
            final CountDownLatch latch = new CountDownLatch(2);

            // subscribe once
            o.subscribe(new Consumer<String>() {
                @Override
                public void accept(String v) {
                    assertEquals("one", v);
                    latch.countDown();
                }
            });

            // subscribe again
            o.subscribe(new Consumer<String>() {
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
            s.dispose();
        }
    }

    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Observable<String> o = Observable.<String>unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(final Observer<? super String> observer) {
                    observer.onSubscribe(Disposables.empty());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            counter.incrementAndGet();
                            observer.onNext("one");
                            observer.onComplete();
                        }
                    }).start();
            }
        }).cache();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Consumer<String>() {
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
        Observable<String> o = Observable.<String>unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(final Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
            }
        }).cacheWithInitialCapacity(1);

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Consumer<String>() {
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
            Observable.error(new RuntimeException("failure"))
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
        Observable.unsafeCreate(new ObservableSource<Object>() {
            @Override
            public void subscribe(final Observer<? super Object> observer) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            observer.onError(new Error("failure"));
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
        Observable.just("1", "2", "three", "4").take(3)
        .safeSubscribe(new DefaultObserver<String>() {

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
        Observable<String> o = Observable.just(1, "abc", false, 2L).ofType(String.class);

        Observer<Object> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, never()).onNext(1);
        verify(observer, times(1)).onNext("abc");
        verify(observer, never()).onNext(false);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onError(
                any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testOfTypeWithPolymorphism() {
        ArrayList<Integer> l1 = new ArrayList<Integer>();
        l1.add(1);
        LinkedList<Integer> l2 = new LinkedList<Integer>();
        l2.add(2);

        @SuppressWarnings("rawtypes")
        Observable<List> o = Observable.<Object> just(l1, l2, "123").ofType(List.class);

        Observer<Object> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext(l1);
        verify(observer, times(1)).onNext(l2);
        verify(observer, never()).onNext("123");
        verify(observer, never()).onError(
                any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContainsObservable() {
        Observable<Boolean> o = Observable.just("a", "b", "c").contains("b").toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(
                any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContainsWithInexistenceObservable() {
        Observable<Boolean> o = Observable.just("a", "b").contains("c").toObservable();

        Observer<Object> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(
                any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    @Ignore("null values are not allowed")
    public void testContainsWithNullObservable() {
        Observable<Boolean> o = Observable.just("a", "b", null).contains(null).toObservable();

        Observer<Object> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(
                any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContainsWithEmptyObservableObservable() {
        Observable<Boolean> o = Observable.<String> empty().contains("a").toObservable();

        Observer<Object> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(
                any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContains() {
        Single<Boolean> o = Observable.just("a", "b", "c").contains("b"); // FIXME nulls not allowed, changed to "c"

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onSuccess(false);
        verify(observer, never()).onError(
                any(Throwable.class));
    }

    @Test
    public void testContainsWithInexistence() {
        Single<Boolean> o = Observable.just("a", "b").contains("c"); // FIXME null values are not allowed, removed

        SingleObserver<Object> observer = TestHelper.mockSingleObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(
                any(Throwable.class));
    }

    @Test
    @Ignore("null values are not allowed")
    public void testContainsWithNull() {
        Single<Boolean> o = Observable.just("a", "b", null).contains(null);

        SingleObserver<Object> observer = TestHelper.mockSingleObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onSuccess(false);
        verify(observer, never()).onError(
                any(Throwable.class));
    }

    @Test
    public void testContainsWithEmptyObservable() {
        Single<Boolean> o = Observable.<String> empty().contains("a");

        SingleObserver<Object> observer = TestHelper.mockSingleObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(
                any(Throwable.class));
    }

    @Test
    public void testIgnoreElements() {
        Completable o = Observable.just(1, 2, 3).ignoreElements();

        CompletableObserver observer = TestHelper.mockCompletableObserver();

        o.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIgnoreElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2, 3).ignoreElements().toObservable();

        Observer<Object> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testJustWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        Observable<Integer> o = Observable.fromArray(1, 2).subscribeOn(scheduler);

        Observer<Integer> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStartWithWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        Observable<Integer> o = Observable.just(3, 4).startWith(Arrays.asList(1, 2)).subscribeOn(scheduler);

        Observer<Integer> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onNext(4);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRangeWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        Observable<Integer> o = Observable.range(3, 4).subscribeOn(scheduler);

        Observer<Integer> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onNext(4);
        inOrder.verify(observer, times(1)).onNext(5);
        inOrder.verify(observer, times(1)).onNext(6);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMergeWith() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.just(1).mergeWith(Observable.just(2)).subscribe(ts);
        ts.assertValues(1, 2);
    }

    @Test
    public void testConcatWith() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.just(1).concatWith(Observable.just(2)).subscribe(ts);
        ts.assertValues(1, 2);
    }

    @Test
    public void testAmbWith() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.just(1).ambWith(Observable.just(2)).subscribe(ts);
        ts.assertValue(1);
    }

    @Test
    public void testTakeWhileToList() {
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0;i < expectedCount; i++) {
            Observable
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
        TestObserver<String> ts = new TestObserver<String>();
        Observable.just(1, 2, 3).compose(new ObservableTransformer<Integer, String>() {
            @Override
            public Observable<String> apply(Observable<Integer> t1) {
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
        Subject<Object> subject = ReplaySubject.create();

        Observable.error(new RuntimeException("oops"))
            .materialize()
            .delay(1, TimeUnit.SECONDS)
            .dematerialize()
            .subscribe(subject);

        subject.subscribe();
        subject.materialize().blockingFirst();

        System.out.println("Done");
    }

    @Test
    public void testEmptyIdentity() {
        assertEquals(Observable.empty(), Observable.empty());
    }

    @Test
    public void testEmptyIsEmpty() {
        Observable.<Integer>empty().subscribe(w);

        verify(w).onComplete();
        verify(w, never()).onNext(any(Integer.class));
        verify(w, never()).onError(any(Throwable.class));
    }

// FIXME this test doesn't make sense
//    @Test // cf. https://github.com/ReactiveX/RxJava/issues/2599
//    public void testSubscribingSubscriberAsObserverMaintainsSubscriptionChain() {
//        TestObserver<Object> observer = new TestObserver<T>();
//        Subscription subscription = Observable.just("event").subscribe((Observer<Object>) observer);
//        subscription.unsubscribe();
//
//        subscriber.assertUnsubscribed();
//    }

// FIXME subscribers can't throw
//    @Test(expected=OnErrorNotImplementedException.class)
//    public void testForEachWithError() {
//        Observable.error(new Exception("boo"))
//        //
//        .forEach(new Action1<Object>() {
//            @Override
//            public void call(Object t) {
//                //do nothing
//            }});
//    }

    @Test(expected = NullPointerException.class)
    public void testForEachWithNull() {
        Observable.error(new Exception("boo"))
        //
        .forEach(null);
    }

    @Test
    public void testExtend() {
        final TestObserver<Object> subscriber = new TestObserver<Object>();
        final Object value = new Object();
        Observable.just(value).to(new Function<Observable<Object>, Object>() {
            @Override
            public Object apply(Observable<Object> onSubscribe) {
                    onSubscribe.subscribe(subscriber);
                    subscriber.assertNoErrors();
                    subscriber.assertComplete();
                    subscriber.assertValue(value);
                    return subscriber.values().get(0);
                }
        });
    }

    @Test
    public void testFlatMap() {
        List<Integer> list = Observable.range(1, 5).flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return Observable.range(v, 2);
            }
        }).toList().blockingGet();

        Assert.assertEquals(Arrays.asList(1, 2, 2, 3, 3, 4, 4, 5, 5, 6), list);
    }

    @Test
    public void singleDefault() {
        Observable.just(1).single(100).test().assertResult(1);

        Observable.empty().single(100).test().assertResult(100);
    }

    @Test
    public void singleDefaultObservable() {
        Observable.just(1).single(100).toObservable().test().assertResult(1);

        Observable.empty().single(100).toObservable().test().assertResult(100);
    }

    @Test
    public void zipIterableObject() {
        @SuppressWarnings("unchecked")
        final List<Observable<Integer>> observables = Arrays.asList(Observable.just(1, 2, 3), Observable.just(1, 2, 3));
        Observable.zip(observables, new Function<Object[], Object>() {
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
        final List<Observable<Integer>> observables = Arrays.asList(Observable.just(1, 2, 3), Observable.just(1, 2, 3));
        Observable.combineLatest(observables, new Function<Object[], Object>() {
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
