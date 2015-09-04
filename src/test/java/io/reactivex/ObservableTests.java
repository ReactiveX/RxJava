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

package io.reactivex;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Predicate;

import org.junit.*;
import org.mockito.*;
import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.*;
import io.reactivex.subscribers.TestSubscriber;

public class ObservableTests {

    Subscriber<Number> w;

    private static final Predicate<Integer> IS_EVEN = v -> v % 2 == 0;

    @Before
    public void before() {
        w = TestHelper.mockSubscriber();
    }

    @Test
    public void fromArray() {
        String[] items = new String[] { "one", "two", "three" };
        assertEquals((Long)3L, Observable.fromArray(items).count().toBlocking().single());
        assertEquals("two", Observable.fromArray(items).skip(1).take(1).toBlocking().single());
        assertEquals("three", Observable.fromArray(items).takeLast(1).toBlocking().single());
    }

    @Test
    public void fromIterable() {
        ArrayList<String> items = new ArrayList<>();
        items.add("one");
        items.add("two");
        items.add("three");

        assertEquals((Long)3L, Observable.fromIterable(items).count().toBlocking().single());
        assertEquals("two", Observable.fromIterable(items).skip(1).take(1).toBlocking().single());
        assertEquals("three", Observable.fromIterable(items).takeLast(1).toBlocking().single());
    }

    @Test
    public void fromArityArgs3() {
        Observable<String> items = Observable.just("one", "two", "three");

        assertEquals((Long)3L, items.count().toBlocking().single());
        assertEquals("two", items.skip(1).take(1).toBlocking().single());
        assertEquals("three", items.takeLast(1).toBlocking().single());
    }

    @Test
    public void fromArityArgs1() {
        Observable<String> items = Observable.just("one");

        assertEquals((Long)1L, items.count().toBlocking().single());
        assertEquals("one", items.takeLast(1).toBlocking().single());
    }

    @Test
    public void testCreate() {

        Observable<String> observable = Observable.just("one", "two", "three");

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testCountAFewItems() {
        Observable<String> observable = Observable.just("a", "b", "c", "d");
        
        observable.count().subscribe(w);
        
        // we should be called only once
        verify(w, times(1)).onNext(anyLong());
        verify(w).onNext(4L);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testCountZeroItems() {
        Observable<String> observable = Observable.empty();
        observable.count().subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyLong());
        verify(w).onNext(0L);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testCountError() {
        Observable<String> o = Observable.error(() -> new RuntimeException());
        
        o.count().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(any(RuntimeException.class));
    }

    public void testTakeFirstWithPredicateOfSome() {
        Observable<Integer> observable = Observable.just(1, 3, 5, 4, 6, 3);
        observable.takeFirst(IS_EVEN).subscribe(w);
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(4);
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstWithPredicateOfNoneMatchingThePredicate() {
        Observable<Integer> observable = Observable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        observable.takeFirst(IS_EVEN).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstOfSome() {
        Observable<Integer> observable = Observable.just(1, 2, 3);
        observable.take(1).subscribe(w);
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(1);
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTakeFirstOfNone() {
        Observable<Integer> observable = Observable.empty();
        observable.take(1).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onComplete();
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstOfNone() {
        Observable<Integer> observable = Observable.empty();
        observable.first().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(isA(NoSuchElementException.class));
    }

    @Test
    public void testFirstWithPredicateOfNoneMatchingThePredicate() {
        Observable<Integer> observable = Observable.just(1, 3, 5, 7, 9, 7, 5, 3, 1);
        observable.filter(IS_EVEN).first().subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onComplete();
        verify(w, times(1)).onError(isA(NoSuchElementException.class));
    }

    @Test
    public void testReduce() {
        Observable<Integer> observable = Observable.just(1, 2, 3, 4);
        observable.reduce((t1, t2) -> t1 + t2)
        .subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(10);
    }

    /**
     * A reduce should fail with an NoSuchElementException if done on an empty Observable.
     */
    @Test(expected = NoSuchElementException.class)
    public void testReduceWithEmptyObservable() {
        Observable<Integer> observable = Observable.range(1, 0);
        observable.reduce((t1, t2) -> t1 + t2)
        .toBlocking().forEach(t1 -> {
            // do nothing ... we expect an exception instead
        });

        fail("Expected an exception to be thrown");
    }

    /**
     * A reduce on an empty Observable and a seed should just pass the seed through.
     * 
     * This is confirmed at https://github.com/ReactiveX/RxJava/issues/423#issuecomment-27642456
     */
    @Test
    public void testReduceWithEmptyObservableAndSeed() {
        Observable<Integer> observable = Observable.range(1, 0);
        int value = observable.reduce(1, (t1, t2) -> t1 + t2)
                .toBlocking().last();

        assertEquals(1, value);
    }

    @Test
    public void testReduceWithInitialValue() {
        Observable<Integer> observable = Observable.just(1, 2, 3, 4);
        observable.reduce(50, (t1, t2) -> t1 + t2)
        .subscribe(w);
        // we should be called only once
        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(60);
    }

    @Ignore // FIXME throwing is not allowed from the create?!
    @Test
    public void testOnSubscribeFails() {
        Subscriber<String> observer = TestHelper.mockSubscriber();

        final RuntimeException re = new RuntimeException("bad impl");
        Observable<String> o = Observable.create(s -> { throw re; });
        
        o.subscribe(observer);
        verify(observer, times(0)).onNext(anyString());
        verify(observer, times(0)).onComplete();
        verify(observer, times(1)).onError(re);
    }

    @Test
    public void testMaterializeDematerializeChaining() {
        Observable<Integer> obs = Observable.just(1);
        Observable<Integer> chained = obs.materialize().dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

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
    public void testCustomObservableWithErrorInObserverAsynchronous() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        
        // FIXME custom built???
        Observable.just("1", "2", "three", "4")
        .subscribeOn(Schedulers.newThread())
        .safeSubscribe(new Observer<String>() {
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
    public void testCustomObservableWithErrorInObserverSynchronous() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        
        // FIXME custom built???
        Observable.just("1", "2", "three", "4")
        .safeSubscribe(new Observer<String>() {

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
     * The error from the user provided Observable is handled by the subscribe try/catch because this is synchronous
     * 
     * 
     * Result: Passes
     */
    @Test
    public void testCustomObservableWithErrorInObservableSynchronous() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        // FIXME custom built???
        Observable.just("1", "2").concatWith(Observable.error(() -> new NumberFormatException()))
        .subscribe(new Observer<String>() {

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
        ConnectableObservable<String> connectable = Observable.<String>create(observer -> {
            observer.onSubscribe(EmptySubscription.INSTANCE);
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
        ConnectableObservable<String> o = Observable.<String>create(observer -> {
                observer.onSubscribe(EmptySubscription.INSTANCE);
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
        Observable<String> o = Observable.<String>create(observer -> {
                observer.onSubscribe(EmptySubscription.INSTANCE);
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
        Observable<String> o = Observable.<String>create(observer -> {
            observer.onSubscribe(EmptySubscription.INSTANCE);
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
     * @throws InterruptedException
     */
    @Test
    @Ignore("Subscribers can't throw")
    public void testErrorThrownWithoutErrorHandlerAsynchronous() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        Observable.create(observer -> {
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
        Observable.just("1", "2", "three", "4").take(3)
        .safeSubscribe(new Observer<String>() {

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
        Observable<String> observable = Observable.just(1, "abc", false, 2L).ofType(String.class);

        Subscriber<Object> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);
        
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
        Observable<List> observable = Observable.<Object> just(l1, l2, "123").ofType(List.class);

        Subscriber<Object> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(l1);
        verify(observer, times(1)).onNext(l2);
        verify(observer, never()).onNext("123");
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContains() {
        Observable<Boolean> observable = Observable.just("a", "b", "c").contains("b"); // FIXME nulls not allowed, changed to "c"

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContainsWithInexistence() {
        Observable<Boolean> observable = Observable.just("a", "b").contains("c"); // FIXME null values are not allowed, removed

        Subscriber<Object> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    @Ignore("null values are not allowed")
    public void testContainsWithNull() {
        Observable<Boolean> observable = Observable.just("a", "b", null).contains(null);

        Subscriber<Object> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testContainsWithEmptyObservable() {
        Observable<Boolean> observable = Observable.<String> empty().contains("a");

        Subscriber<Object> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIgnoreElements() {
        Observable<Integer> observable = Observable.just(1, 2, 3).ignoreElements();

        Subscriber<Object> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testJustWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        Observable<Integer> observable = Observable.fromArray(1, 2).subscribeOn(scheduler);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);

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
        Observable<Integer> observable = Observable.just(3, 4).startWith(Arrays.asList(1, 2)).subscribeOn(scheduler);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);

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
        Observable<Integer> observable = Observable.range(3, 4).subscribeOn(scheduler);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

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
        Observable<List<Integer>> o = Observable.just(1, 2, 3)
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
        String value = Observable.just(1, 2, 3).collect(StringBuilder::new, 
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
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.just(1).mergeWith(Observable.just(2)).subscribe(ts);
        ts.assertValues(1, 2);
    }
    
    @Test
    public void testConcatWith() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.just(1).concatWith(Observable.just(2)).subscribe(ts);
        ts.assertValues(1, 2);
    }
    
    @Test
    public void testAmbWith() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.just(1).ambWith(Observable.just(2)).subscribe(ts);
        ts.assertValue(1);
    }
// FIXME Subscribers can't throw
//    @Test(expected = OnErrorNotImplementedException.class)
//    public void testSubscribeWithoutOnError() {
//        Observable<String> o = Observable.just("a", "b").flatMap(new Func1<String, Observable<String>>() {
//            @Override
//            public Observable<String> call(String s) {
//                return Observable.error(new Exception("test"));
//            }
//        });
//        o.subscribe();
//    }

    @Test
    public void testTakeWhileToList() {
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0;i < expectedCount; i++) {
            Observable
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
        TestSubscriber<String> ts = new TestSubscriber<>();
        Observable.just(1, 2, 3).compose(t1 ->t1.map(String::valueOf))
        .subscribe(ts);
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValues("1", "2", "3");
    }
    
    @Test
    public void testErrorThrownIssue1685() {
        Subject<Object, Object> subject = ReplaySubject.create();

        Observable.error(new RuntimeException("oops"))
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
//        TestSubscriber<Object> subscriber = new TestSubscriber<>();
//        Subscription subscription = Observable.just("event").subscribe((Observer<Object>) subscriber);
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
        final TestSubscriber<Object> subscriber = new TestSubscriber<>();
        final Object value = new Object();
        Observable.just(value).to(onSubscribe -> {
                onSubscribe.subscribe(subscriber);
                subscriber.assertNoErrors();
                subscriber.assertComplete();
                subscriber.assertValue(value);
                return subscriber.values().get(0);
            });
    }
}