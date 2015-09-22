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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorObserveOnTest {

    /**
     * This is testing a no-op path since it uses Schedulers.immediate() which will not do scheduling.
     */
    @SuppressWarnings("deprecation")
    @Test
    @Ignore("immediate scheduler not supported")
    public void testObserveOn() {
        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.just(1, 2, 3).observeOn(Schedulers.immediate()).subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, times(1)).onNext(3);
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testOrdering() throws InterruptedException {
//        NbpObservable<String> obs = NbpObservable.just("one", null, "two", "three", "four");
        // FIXME null values not allowed
        NbpObservable<String> obs = NbpObservable.just("one", "null", "two", "three", "four");

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        InOrder inOrder = inOrder(NbpObserver);
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>(NbpObserver);

        obs.observeOn(Schedulers.computation()).subscribe(ts);

        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        if (ts.errors().size() > 0) {
            for (Throwable t : ts.errors()) {
                t.printStackTrace();
            }
            fail("failed with exception");
        }

        inOrder.verify(NbpObserver, times(1)).onNext("one");
        inOrder.verify(NbpObserver, times(1)).onNext("null");
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        inOrder.verify(NbpObserver, times(1)).onNext("four");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testThreadName() throws InterruptedException {
        System.out.println("Main Thread: " + Thread.currentThread().getName());
        // FIXME null values not allowed
//        NbpObservable<String> obs = NbpObservable.just("one", null, "two", "three", "four");
        NbpObservable<String> obs = NbpObservable.just("one", "null", "two", "three", "four");

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        final String parentThreadName = Thread.currentThread().getName();

        final CountDownLatch completedLatch = new CountDownLatch(1);

        // assert subscribe is on main thread
        obs = obs.doOnNext(new Consumer<String>() {

            @Override
            public void accept(String s) {
                String threadName = Thread.currentThread().getName();
                System.out.println("Source ThreadName: " + threadName + "  Expected => " + parentThreadName);
                assertEquals(parentThreadName, threadName);
            }

        });

        // assert observe is on new thread
        obs.observeOn(Schedulers.newThread()).doOnNext(new Consumer<String>() {

            @Override
            public void accept(String t1) {
                String threadName = Thread.currentThread().getName();
                boolean correctThreadName = threadName.startsWith("RxNewThreadScheduler");
                System.out.println("ObserveOn ThreadName: " + threadName + "  Correct => " + correctThreadName);
                assertTrue(correctThreadName);
            }

        }).finallyDo(new Runnable() {

            @Override
            public void run() {
                completedLatch.countDown();

            }
        }).subscribe(NbpObserver);

        if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting");
        }

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(5)).onNext(any(String.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @SuppressWarnings("deprecation")
    @Test
    @Ignore("immediate scheduler not supported")
    public void observeOnTheSameSchedulerTwice() {
        Scheduler scheduler = Schedulers.immediate();

        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3);
        NbpObservable<Integer> o2 = o.observeOn(scheduler);

        @SuppressWarnings("unchecked")
        NbpObserver<Object> observer1 = mock(NbpObserver.class);
        @SuppressWarnings("unchecked")
        NbpObserver<Object> observer2 = mock(NbpObserver.class);

        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);

        o2.subscribe(observer1);
        o2.subscribe(observer2);

        inOrder1.verify(observer1, times(1)).onNext(1);
        inOrder1.verify(observer1, times(1)).onNext(2);
        inOrder1.verify(observer1, times(1)).onNext(3);
        inOrder1.verify(observer1, times(1)).onComplete();
        verify(observer1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(observer2, times(1)).onNext(1);
        inOrder2.verify(observer2, times(1)).onNext(2);
        inOrder2.verify(observer2, times(1)).onNext(3);
        inOrder2.verify(observer2, times(1)).onComplete();
        verify(observer2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();
    }

    @Test
    public void observeSameOnMultipleSchedulers() {
        TestScheduler scheduler1 = new TestScheduler();
        TestScheduler scheduler2 = new TestScheduler();

        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3);
        NbpObservable<Integer> o1 = o.observeOn(scheduler1);
        NbpObservable<Integer> o2 = o.observeOn(scheduler2);

        NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
        NbpSubscriber<Object> observer2 = TestHelper.mockNbpSubscriber();

        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);

        o1.subscribe(observer1);
        o2.subscribe(observer2);

        scheduler1.advanceTimeBy(1, TimeUnit.SECONDS);
        scheduler2.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder1.verify(observer1, times(1)).onNext(1);
        inOrder1.verify(observer1, times(1)).onNext(2);
        inOrder1.verify(observer1, times(1)).onNext(3);
        inOrder1.verify(observer1, times(1)).onComplete();
        verify(observer1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(observer2, times(1)).onNext(1);
        inOrder2.verify(observer2, times(1)).onNext(2);
        inOrder2.verify(observer2, times(1)).onNext(3);
        inOrder2.verify(observer2, times(1)).onComplete();
        verify(observer2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();
    }

    /**
     * Confirm that running on a NewThreadScheduler uses the same thread for the entire stream
     */
    @Test
    public void testObserveOnWithNewThreadScheduler() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        NbpObservable.range(1, 100000).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.newThread())
        .toBlocking().forEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                // FIXME toBlocking methods run on the current thread
                String name = Thread.currentThread().getName();
                assertFalse("Wrong thread name: " + name, name.startsWith("Rx"));
            }

        });

    }

    /**
     * Confirm that running on a ThreadPoolScheduler allows multiple threads but is still ordered.
     */
    @Test
    public void testObserveOnWithThreadPoolScheduler() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        NbpObservable.range(1, 100000).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.computation())
        .toBlocking().forEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                // FIXME toBlocking methods run on the caller's thread
                String name = Thread.currentThread().getName();
                assertFalse("Wrong thread name: " + name, name.startsWith("Rx"));
            }

        });
    }

    /**
     * Attempts to confirm that when pauses exist between events, the ScheduledObserver
     * does not lose or reorder any events since the scheduler will not block, but will
     * be re-scheduled when it receives new events after each pause.
     * 
     * 
     * This is non-deterministic in proving success, but if it ever fails (non-deterministically)
     * it is a sign of potential issues as thread-races and scheduling should not affect output.
     */
    @Test
    public void testObserveOnOrderingConcurrency() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        NbpObservable.range(1, 10000).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                if (randomIntFrom0to100() > 98) {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.computation())
        .toBlocking().forEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
//                assertTrue(name.startsWith("RxComputationThreadPool"));
                // FIXME toBlocking now runs its methods on the caller thread
                String name = Thread.currentThread().getName();
                assertFalse("Wrong thread name: " + name, name.startsWith("Rx"));
            }

        });
    }

    @Test
    public void testNonBlockingOuterWhileBlockingOnNext() throws InterruptedException {

        final CountDownLatch completedLatch = new CountDownLatch(1);
        final CountDownLatch nextLatch = new CountDownLatch(1);
        final AtomicLong completeTime = new AtomicLong();
        // use subscribeOn to make async, observeOn to move
        NbpObservable.range(1, 2).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(new NbpObserver<Integer>() {

            @Override
            public void onComplete() {
                System.out.println("onCompleted");
                completeTime.set(System.nanoTime());
                completedLatch.countDown();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                // don't let this thing finish yet
                try {
                    if (!nextLatch.await(1000, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("it shouldn't have timed out");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("it shouldn't have failed");
                }
            }

        });

        long afterSubscribeTime = System.nanoTime();
        System.out.println("After subscribe: " + completedLatch.getCount());
        assertEquals(1, completedLatch.getCount());
        nextLatch.countDown();
        completedLatch.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(completeTime.get() > afterSubscribeTime);
        System.out.println("onComplete nanos after subscribe: " + (completeTime.get() - afterSubscribeTime));
    }

    private static int randomIntFrom0to100() {
        // XORShift instead of Math.random http://javamex.com/tutorials/random_numbers/xorshift.shtml
        long x = System.nanoTime();
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return Math.abs((int) x % 100);
    }

    @Test
    public void testDelayedErrorDeliveryWhenSafeSubscriberUnsubscribes() {
        TestScheduler testScheduler = new TestScheduler();

        NbpObservable<Integer> source = NbpObservable.concat(NbpObservable.<Integer> error(new TestException()), NbpObservable.just(1));

        @SuppressWarnings("unchecked")
        NbpObserver<Integer> o = mock(NbpObserver.class);
        InOrder inOrder = inOrder(o);

        source.observeOn(testScheduler).subscribe(o);

        inOrder.verify(o, never()).onError(any(TestException.class));

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o, never()).onComplete();
    }

    @Test
    public void testAfterUnsubscribeCalledThenObserverOnNextNeverCalled() {
        final TestScheduler testScheduler = new TestScheduler();

        final NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>(NbpObserver);
        
        NbpObservable.just(1, 2, 3)
                .observeOn(testScheduler)
                .subscribe(ts);
        
        ts.dispose();
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        final InOrder inOrder = inOrder(NbpObserver);

        inOrder.verify(NbpObserver, never()).onNext(anyInt());
        inOrder.verify(NbpObserver, never()).onError(any(Exception.class));
        inOrder.verify(NbpObserver, never()).onComplete();
    }

    @Test
    public void testBackpressureWithTakeBefore() {
        final AtomicInteger generated = new AtomicInteger();
        NbpObservable<Integer> o = NbpObservable.fromIterable(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                    }

                    @Override
                    public Integer next() {
                        return generated.getAndIncrement();
                    }

                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                };
            }
        });

        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        o
                .take(7)
                .observeOn(Schedulers.newThread())
                .subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertValues(0, 1, 2, 3, 4, 5, 6);
        assertEquals(7, generated.get());
    }

    @Test
    public void testAsyncChild() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(0, 100000).observeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }
}