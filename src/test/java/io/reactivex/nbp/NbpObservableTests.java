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

package io.reactivex.nbp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Predicate;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observables.nbp.NbpConnectableObservable;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.nbp.*;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpObservableTests {

    NbpSubscriber<Number> w;

    private static final Predicate<Integer> IS_EVEN = v -> v % 2 == 0;

    @Before
    public void before() {
        w = TestHelper.mockNbpSubscriber();
    }

    @Test
    public void fromArray() {
        String[] items = new String[] { "one", "two", "three" };
        assertEquals((Long)3L, NbpObservable.fromArray(items).count().toBlocking().single());
        assertEquals("two", NbpObservable.fromArray(items).skip(1).take(1).toBlocking().single());
        assertEquals("three", NbpObservable.fromArray(items).takeLast(1).toBlocking().single());
    }

    @Test
    public void fromIterable() {
        ArrayList<String> items = new ArrayList<>();
        items.add("one");
        items.add("two");
        items.add("three");

        assertEquals((Long)3L, NbpObservable.fromIterable(items).count().toBlocking().single());
        assertEquals("two", NbpObservable.fromIterable(items).skip(1).take(1).toBlocking().single());
        assertEquals("three", NbpObservable.fromIterable(items).takeLast(1).toBlocking().single());
    }

    @Test
    public void fromArityArgs3() {
        NbpObservable<String> items = NbpObservable.just("one", "two", "three");

        assertEquals((Long)3L, items.count().toBlocking().single());
        assertEquals("two", items.skip(1).take(1).toBlocking().single());
        assertEquals("three", items.takeLast(1).toBlocking().single());
    }

    @Test
    public void fromArityArgs1() {
        NbpObservable<String> items = NbpObservable.just("one");

        assertEquals((Long)1L, items.count().toBlocking().single());
        assertEquals("one", items.takeLast(1).toBlocking().single());
    }

    @Test
    public void testCreate() {

        NbpObservable<String> o = NbpObservable.just("one", "two", "three");

        NbpSubscriber<String> observer = TestHelper.mockNbpSubscriber();
        
        o.subscribe(observer);
        
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testCountAFewItems() {
        NbpObservable<String> o = NbpObservable.just("a", "b", "c", "d");
        
        o.count().subscribe(w);
        
        // we should be called only once
        verify(w, times(1)).onNext(anyLong());
        verify(w).onNext(4L);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testCountZeroItems() {
        NbpObservable<String> o = NbpObservable.empty();
        o.count().subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyLong());
        verify(w).onNext(0L);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testCountError() {
        NbpObservable<String> o = NbpObservable.error(() -> new RuntimeException());
        
        o.count().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(any(RuntimeException.class));
    }

    public void testTakeFirstWithPredicateOfSome() {
        NbpObservable<Integer> o = NbpObservable.just(1, 3, 5, 4, 6, 3);
        o.takeFirst(IS_EVEN).subscribe(w);
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(4);
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstWithPredicateOfNoneMatchingThePredicate() {
        NbpObservable<Integer> o = NbpObservable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        o.takeFirst(IS_EVEN).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstOfSome() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3);
        o.take(1).subscribe(w);
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(1);
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstOfNone() {
        NbpObservable<Integer> o = NbpObservable.empty();
        o.take(1).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstOfNone() {
        NbpObservable<Integer> o = NbpObservable.empty();
        o.first().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(isA(NoSuchElementException.class));
    }

    @Test
    public void testFirstWithPredicateOfNoneMatchingThePredicate() {
        NbpObservable<Integer> o = NbpObservable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        o.filter(IS_EVEN).first().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(isA(NoSuchElementException.class));
    }

    @Test
    public void testReduce() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4);
        o.reduce((t1, t2) -> t1 + t2)
        .subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(10);
    }

    /**
     * A reduce should fail with an NoSuchElementException if done on an empty NbpObservable.
     */
    @Test(expected = NoSuchElementException.class)
    public void testReduceWithEmptyNbpObservable() {
        NbpObservable<Integer> o = NbpObservable.range(1, 0);
        o.reduce((t1, t2) -> t1 + t2)
        .toBlocking().forEach(t1 -> {
            // do nothing ... we expect an exception instead
        });

        fail("Expected an exception to be thrown");
    }

    /**
     * A reduce on an empty NbpObservable and a seed should just pass the seed through.
     * 
     * This is confirmed at https://github.com/ReactiveX/RxJava/issues/423#issuecomment-27642456
     */
    @Test
    public void testReduceWithEmptyNbpObservableAndSeed() {
        NbpObservable<Integer> o = NbpObservable.range(1, 0);
        int value = o.reduce(1, (t1, t2) -> t1 + t2)
                .toBlocking().last();

        assertEquals(1, value);
    }

    @Test
    public void testReduceWithInitialValue() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4);
        o.reduce(50, (t1, t2) -> t1 + t2)
        .subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(60);
    }

    @Ignore // FIXME throwing is not allowed from the create?!
    @Test
    public void testOnSubscribeFails() {
        NbpSubscriber<String> observer = TestHelper.mockNbpSubscriber();

        final RuntimeException re = new RuntimeException("bad impl");
        NbpObservable<String> o = NbpObservable.create(s -> { throw re; });
        
        o.subscribe(observer);
        verify(observer, times(0)).onNext(anyString());
        verify(observer, times(0)).onComplete();
        verify(observer, times(1)).onError(re);
    }

    @Test
    public void testMaterializeDematerializeChaining() {
        NbpObservable<Integer> obs = NbpObservable.just(1);
        NbpObservable<Integer> chained = obs.materialize().dematerialize();

        NbpSubscriber<Integer> observer = TestHelper.mockNbpSubscriber();

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
     */
    @Test
    public void testCustomNbpObservableWithErrorInObserverAsynchronous() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        
        // FIXME custom built???
        NbpObservable.just("1", "2", "three", "4")
        .subscribeOn(Schedulers.newThread())
        .safeSubscribe(new NbpObserver<String>() {
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
     * The error from the user provided Observer is handled by the subscribe try/catch because this is synchronous
     * 
     * Result: Passes
     */
    @Test
    public void testCustomNbpObservableWithErrorInObserverSynchronous() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        
        // FIXME custom built???
        NbpObservable.just("1", "2", "three", "4")
        .safeSubscribe(new NbpObserver<String>() {

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
     * The error from the user provided NbpObservable is handled by the subscribe try/catch because this is synchronous
     * 
     * 
     * Result: Passes
     */
    @Test
    public void testCustomNbpObservableWithErrorInNbpObservableSynchronous() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        // FIXME custom built???
        NbpObservable.just("1", "2").concatWith(NbpObservable.error(() -> new NumberFormatException()))
        .subscribe(new NbpObserver<String>() {

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
        NbpConnectableObservable<String> connectable = NbpObservable.<String>create(observer -> {
            observer.onSubscribe(EmptyDisposable.INSTANCE);
            count.incrementAndGet();
            new Thread(() -> {
                observer.onNext("first");
                observer.onNext("last");
                observer.onComplete();
            }).start();
        }).takeLast(1).publish();

        // subscribe once
        final CountDownLatch latch = new CountDownLatch(1);
        connectable.subscribe(value -> {
            assertEquals("last", value);
            latch.countDown();
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
        NbpConnectableObservable<String> o = NbpObservable.<String>create(observer -> {
                observer.onSubscribe(EmptyDisposable.INSTANCE);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
        }).replay();

        // we connect immediately and it will emit the value
        Disposable s = o.connect();
        try {

            // we then expect the following 2 subscriptions to get that same value
            final CountDownLatch latch = new CountDownLatch(2);

            // subscribe once
            o.subscribe(v -> {
                assertEquals("one", v);
                latch.countDown();
            });

            // subscribe again
            o.subscribe(v -> {
                assertEquals("one", v);
                latch.countDown();
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
        NbpObservable<String> o = NbpObservable.<String>create(observer -> {
                observer.onSubscribe(EmptyDisposable.INSTANCE);
                new Thread(() -> {
                    counter.incrementAndGet();
                    observer.onNext("one");
                    observer.onComplete();
                }).start();
        }).cache();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(v -> {
            assertEquals("one", v);
            latch.countDown();
        });

        // subscribe again
        o.subscribe(v -> {
            assertEquals("one", v);
            latch.countDown();
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void testCacheWithCapacity() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        NbpObservable<String> o = NbpObservable.<String>create(observer -> {
            observer.onSubscribe(EmptyDisposable.INSTANCE);
            new Thread(() -> {
                counter.incrementAndGet();
                observer.onNext("one");
                observer.onComplete();
            }).start();
        }).cache(1);

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(v -> {
            assertEquals("one", v);
            latch.countDown();
        });

        // subscribe again
        o.subscribe(v -> {
            assertEquals("one", v);
            latch.countDown();
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
     * to rethrow the exception on the thread that the message comes out from the NbpObservable.
     * The OnCompleted behavior in this case is to do nothing."
     */
    @Test
    @Ignore("Subscribers can't throw")
    public void testErrorThrownWithoutErrorHandlerSynchronous() {
        try {
            NbpObservable.error(new RuntimeException("failure"))
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
     * to rethrow the exception on the thread that the message comes out from the NbpObservable.
     * The OnCompleted behavior in this case is to do nothing."
     * 
     * @throws InterruptedException
     */
    @Test
    @Ignore("Subscribers can't throw")
    public void testErrorThrownWithoutErrorHandlerAsynchronous() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        NbpObservable.create(observer -> {
            new Thread(() -> {
                try {
                    observer.onError(new Error("failure"));
                } catch (Throwable e) {
                    // without an onError handler it has to just throw on whatever thread invokes it
                    exception.set(e);
                }
                latch.countDown();
            }).start();
        }).subscribe();
        // wait for exception
        latch.await(3000, TimeUnit.MILLISECONDS);
        assertNotNull(exception.get());
        assertEquals("failure", exception.get().getMessage());
    }

    @Test
    public void testTakeWithErrorInObserver() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        NbpObservable.just("1", "2", "three", "4").take(3)
        .safeSubscribe(new NbpObserver<String>() {

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
        NbpObservable<String> o = NbpObservable.just(1, "abc", false, 2L).ofType(String.class);

        NbpSubscriber<Object> observer = TestHelper.mockNbpSubscriber();
        
        o.subscribe(observer);
        
        verify(observer, never()).onNext(1);
        verify(observer, times(1)).onNext("abc");
        verify(observer, never()).onNext(false);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testOfTypeWithPolymorphism() {
        ArrayList<Integer> l1 = new ArrayList<>();
        l1.add(1);
        LinkedList<Integer> l2 = new LinkedList<>();
        l2.add(2);

        @SuppressWarnings("rawtypes")
        NbpObservable<List> o = NbpObservable.<Object> just(l1, l2, "123").ofType(List.class);

        NbpSubscriber<Object> observer = TestHelper.mockNbpSubscriber();
        
        o.subscribe(observer);
        
        verify(observer, times(1)).onNext(l1);
        verify(observer, times(1)).onNext(l2);
        verify(observer, never()).onNext("123");
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContains() {
        NbpObservable<Boolean> o = NbpObservable.just("a", "b", "c").contains("b"); // FIXME nulls not allowed, changed to "c"

        NbpSubscriber<Boolean> observer = TestHelper.mockNbpSubscriber();

        o.subscribe(observer);
        
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContainsWithInexistence() {
        NbpObservable<Boolean> o = NbpObservable.just("a", "b").contains("c"); // FIXME null values are not allowed, removed

        NbpSubscriber<Object> observer = TestHelper.mockNbpSubscriber();
        
        o.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    @Ignore("null values are not allowed")
    public void testContainsWithNull() {
        NbpObservable<Boolean> o = NbpObservable.just("a", "b", null).contains(null);

        NbpSubscriber<Object> observer = TestHelper.mockNbpSubscriber();

        o.subscribe(observer);
        
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContainsWithEmptyNbpObservable() {
        NbpObservable<Boolean> o = NbpObservable.<String> empty().contains("a");

        NbpSubscriber<Object> observer = TestHelper.mockNbpSubscriber();
        
        o.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIgnoreElements() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3).ignoreElements();

        NbpSubscriber<Object> observer = TestHelper.mockNbpSubscriber();

        o.subscribe(observer);
        
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testJustWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        NbpObservable<Integer> o = NbpObservable.fromArray(1, 2).subscribeOn(scheduler);

        NbpSubscriber<Integer> observer = TestHelper.mockNbpSubscriber();
        
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
        NbpObservable<Integer> o = NbpObservable.just(3, 4).startWith(Arrays.asList(1, 2)).subscribeOn(scheduler);

        NbpSubscriber<Integer> observer = TestHelper.mockNbpSubscriber();
        
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
        NbpObservable<Integer> o = NbpObservable.range(3, 4).subscribeOn(scheduler);

        NbpSubscriber<Integer> observer = TestHelper.mockNbpSubscriber();

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
    public void testCollectToList() {
        NbpObservable<List<Integer>> o = NbpObservable.just(1, 2, 3)
        .collect(ArrayList::new, (list, v) -> list.add(v));
        
        List<Integer> list =  o.toBlocking().last();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(2, list.get(1).intValue());
        assertEquals(3, list.get(2).intValue());
        
        // test multiple subscribe
        List<Integer> list2 =  o.toBlocking().last();

        assertEquals(3, list2.size());
        assertEquals(1, list2.get(0).intValue());
        assertEquals(2, list2.get(1).intValue());
        assertEquals(3, list2.get(2).intValue());
    }

    @Test
    public void testCollectToString() {
        String value = NbpObservable.just(1, 2, 3).collect(StringBuilder::new, 
            (sb, v) -> {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(v);
        }).toBlocking().last().toString();

        assertEquals("1-2-3", value);
    }
    
    @Test
    public void testMergeWith() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.just(1).mergeWith(NbpObservable.just(2)).subscribe(ts);
        ts.assertValues(1, 2);
    }
    
    @Test
    public void testConcatWith() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.just(1).concatWith(NbpObservable.just(2)).subscribe(ts);
        ts.assertValues(1, 2);
    }
    
    @Test
    public void testAmbWith() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.just(1).ambWith(NbpObservable.just(2)).subscribe(ts);
        ts.assertValue(1);
    }
// FIXME Subscribers can't throw
//    @Test(expected = OnErrorNotImplementedException.class)
//    public void testSubscribeWithoutOnError() {
//        NbpObservable<String> o = NbpObservable.just("a", "b").flatMap(new Func1<String, NbpObservable<String>>() {
//            @Override
//            public NbpObservable<String> call(String s) {
//                return NbpObservable.error(new Exception("test"));
//            }
//        });
//        o.subscribe();
//    }

    @Test
    public void testTakeWhileToList() {
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0;i < expectedCount; i++) {
            NbpObservable
                    .just(Boolean.TRUE, Boolean.FALSE)
                    .takeWhile(v -> v)
                    .toList()
                    .doOnNext(booleans -> count.incrementAndGet())
                    .subscribe();
        }
        assertEquals(expectedCount, count.get());
    }
    
    @Test
    public void testCompose() {
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        NbpObservable.just(1, 2, 3).compose(t1 ->t1.map(String::valueOf))
        .subscribe(ts);
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValues("1", "2", "3");
    }
    
    @Test
    public void testErrorThrownIssue1685() {
        NbpSubject<Object, Object> subject = NbpReplaySubject.create();

        NbpObservable.error(new RuntimeException("oops"))
            .materialize()
            .delay(1, TimeUnit.SECONDS)
            .dematerialize()
            .subscribe(subject);

        subject.subscribe();
        subject.materialize().toBlocking().first();

        System.out.println("Done");
    }

    @Test
    public void testEmptyIdentity() {
        assertEquals(NbpObservable.empty(), NbpObservable.empty());
    }
    
    @Test
    public void testEmptyIsEmpty() {
        NbpObservable.<Integer>empty().subscribe(w);
        
        verify(w).onComplete();
        verify(w, never()).onNext(any(Integer.class));
        verify(w, never()).onError(any(Throwable.class));
    }

// FIXME this test doesn't make sense 
//    @Test // cf. https://github.com/ReactiveX/RxJava/issues/2599
//    public void testSubscribingSubscriberAsObserverMaintainsSubscriptionChain() {
//        NbpTestSubscriber<Object> subscriber = new NbpTestSubscriber<>();
//        Subscription subscription = NbpObservable.just("event").subscribe((Observer<Object>) subscriber);
//        subscription.unsubscribe();
//
//        subscriber.assertUnsubscribed();
//    }

// FIXME subscribers can't throw
//    @Test(expected=OnErrorNotImplementedException.class)
//    public void testForEachWithError() {
//        NbpObservable.error(new Exception("boo"))
//        //
//        .forEach(new Action1<Object>() {
//            @Override
//            public void call(Object t) {
//                //do nothing
//            }});
//    }
    
    @Test(expected = NullPointerException.class)
    public void testForEachWithNull() {
        NbpObservable.error(new Exception("boo"))
        //
        .forEach(null);
    }
    
    @Test
    public void testExtend() {
        final NbpTestSubscriber<Object> subscriber = new NbpTestSubscriber<>();
        final Object value = new Object();
        NbpObservable.just(value).to(onSubscribe -> {
                onSubscribe.subscribe(subscriber);
                subscriber.assertNoErrors();
                subscriber.assertComplete();
                subscriber.assertValue(value);
                return subscriber.values().get(0);
            });
    }
}