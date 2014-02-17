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
package rx.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.ImmediateScheduler;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.schedulers.TrampolineScheduler;
import rx.subscriptions.BooleanSubscription;

public class OperatorObserveOnBoundedTest {

    /**
     * This is testing a no-op path since it uses Schedulers.immediate() which will not do scheduling.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testObserveOn() {
        Observer<Integer> observer = mock(Observer.class);
        Observable.from(1, 2, 3).lift(new OperatorObserveOnBounded<Integer>(Schedulers.immediate(), 1)).subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOrdering() throws InterruptedException {
        Observable<String> obs = Observable.from("one", null, "two", "three", "four");

        Observer<String> observer = mock(Observer.class);

        InOrder inOrder = inOrder(observer);

        final CountDownLatch completedLatch = new CountDownLatch(1);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                completedLatch.countDown();
                return null;
            }
        }).when(observer).onCompleted();

        obs.lift(new OperatorObserveOnBounded<String>(Schedulers.computation(), 1)).subscribe(observer);

        if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting");
        }

        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext(null);
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThreadName() throws InterruptedException {
        System.out.println("Main Thread: " + Thread.currentThread().getName());
        Observable<String> obs = Observable.from("one", null, "two", "three", "four");

        Observer<String> observer = mock(Observer.class);
        final String parentThreadName = Thread.currentThread().getName();

        final CountDownLatch completedLatch = new CountDownLatch(1);

        // assert subscribe is on main thread
        obs = obs.doOnNext(new Action1<String>() {

            @Override
            public void call(String s) {
                String threadName = Thread.currentThread().getName();
                System.out.println("Source ThreadName: " + threadName + "  Expected => " + parentThreadName);
                assertEquals(parentThreadName, threadName);
            }

        });

        // assert observe is on new thread
        obs.lift(new OperatorObserveOnBounded<String>(Schedulers.newThread(), 1)).doOnNext(new Action1<String>() {

            @Override
            public void call(String t1) {
                String threadName = Thread.currentThread().getName();
                boolean correctThreadName = threadName.startsWith("RxNewThreadScheduler");
                System.out.println("ObserveOn ThreadName: " + threadName + "  Correct => " + correctThreadName);
                assertTrue(correctThreadName);
            }

        }).finallyDo(new Action0() {

            @Override
            public void call() {
                completedLatch.countDown();

            }
        }).subscribe(observer);

        if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting");
        }

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(5)).onNext(any(String.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void observeOnTheSameSchedulerTwice() {
        Scheduler scheduler = Schedulers.immediate();

        Observable<Integer> o = Observable.from(1, 2, 3);
        Observable<Integer> o2 = o.lift(new OperatorObserveOnBounded<Integer>(scheduler, 1));

        @SuppressWarnings("unchecked")
        Observer<Object> observer1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observer2 = mock(Observer.class);

        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);

        o2.subscribe(observer1);
        o2.subscribe(observer2);

        inOrder1.verify(observer1, times(1)).onNext(1);
        inOrder1.verify(observer1, times(1)).onNext(2);
        inOrder1.verify(observer1, times(1)).onNext(3);
        inOrder1.verify(observer1, times(1)).onCompleted();
        verify(observer1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(observer2, times(1)).onNext(1);
        inOrder2.verify(observer2, times(1)).onNext(2);
        inOrder2.verify(observer2, times(1)).onNext(3);
        inOrder2.verify(observer2, times(1)).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();
    }

    @Test
    public void observeSameOnMultipleSchedulers() {
        TestScheduler scheduler1 = new TestScheduler();
        TestScheduler scheduler2 = new TestScheduler();

        Observable<Integer> o = Observable.from(1, 2, 3);
        Observable<Integer> o1 = o.lift(new OperatorObserveOnBounded<Integer>(scheduler1, 1));
        Observable<Integer> o2 = o.lift(new OperatorObserveOnBounded<Integer>(scheduler2, 1));

        @SuppressWarnings("unchecked")
        Observer<Object> observer1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observer2 = mock(Observer.class);

        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);

        o1.subscribe(observer1);
        o2.subscribe(observer2);

        scheduler1.advanceTimeBy(1, TimeUnit.SECONDS);
        scheduler2.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder1.verify(observer1, times(1)).onNext(1);
        inOrder1.verify(observer1, times(1)).onNext(2);
        inOrder1.verify(observer1, times(1)).onNext(3);
        inOrder1.verify(observer1, times(1)).onCompleted();
        verify(observer1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(observer2, times(1)).onNext(1);
        inOrder2.verify(observer2, times(1)).onNext(2);
        inOrder2.verify(observer2, times(1)).onNext(3);
        inOrder2.verify(observer2, times(1)).onCompleted();
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

        Observable.range(1, 100000).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * _multiple;
            }

        }).lift(new OperatorObserveOnBounded<Integer>(Schedulers.newThread(), 1))
                .toBlockingObservable().forEach(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                        assertTrue(Thread.currentThread().getName().startsWith("RxNewThreadScheduler"));
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

        Observable.range(1, 100000).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * _multiple;
            }

        }).lift(new OperatorObserveOnBounded<Integer>(Schedulers.computation(), 1))
                .toBlockingObservable().forEach(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                        assertTrue(Thread.currentThread().getName().startsWith("RxComputationThreadPool"));
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

        Observable.range(1, 10000).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                if (randomIntFrom0to100() > 98) {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return t1 * _multiple;
            }

        }).lift(new OperatorObserveOnBounded<Integer>(Schedulers.computation(), 1))
                .toBlockingObservable().forEach(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                        assertTrue(Thread.currentThread().getName().startsWith("RxComputationThreadPool"));
                    }

                });
    }

    @Test
    public void testNonBlockingOuterWhileBlockingOnNext() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicLong completeTime = new AtomicLong();
        // use subscribeOn to make async, observeOn to move
        Observable.range(1, 1000).subscribeOn(Schedulers.newThread()).lift(new OperatorObserveOnBounded<Integer>(Schedulers.newThread(), 1)).subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
                completeTime.set(System.nanoTime());
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {

            }

        });

        long afterSubscribeTime = System.nanoTime();
        System.out.println("After subscribe: " + latch.getCount());
        assertEquals(1, latch.getCount());
        latch.await();
        assertTrue(completeTime.get() > afterSubscribeTime);
        System.out.println("onComplete nanos after subscribe: " + (completeTime.get() - afterSubscribeTime));
    }

    @Test
    public final void testBackpressureOnFastProducerSlowConsumerWithUnsubscribeNewThread() throws InterruptedException {
        testBackpressureOnFastProducerSlowConsumerWithUnsubscribe(Schedulers.newThread(), 1);
    }

    @Test
    public final void testBackpressureOnFastProducerSlowConsumerWithUnsubscribeNewThreadAndBuffer8() throws InterruptedException {
        testBackpressureOnFastProducerSlowConsumerWithUnsubscribe(Schedulers.newThread(), 8);
    }

    @Test
    public final void testBackpressureOnFastProducerSlowConsumerWithUnsubscribeIO() throws InterruptedException {
        testBackpressureOnFastProducerSlowConsumerWithUnsubscribe(Schedulers.io(), 1);
    }

    @Test
    public final void testBackpressureOnFastProducerSlowConsumerWithUnsubscribeTrampoline() throws InterruptedException {
        testBackpressureOnFastProducerSlowConsumerWithUnsubscribe(Schedulers.trampoline(), 1);
    }

    @Test
    public final void testBackpressureOnFastProducerSlowConsumerWithUnsubscribeTestScheduler() throws InterruptedException {
        testBackpressureOnFastProducerSlowConsumerWithUnsubscribe(Schedulers.test(), 1);
    }

    @Test
    public final void testBackpressureOnFastProducerSlowConsumerWithUnsubscribeComputation() throws InterruptedException {
        testBackpressureOnFastProducerSlowConsumerWithUnsubscribe(Schedulers.computation(), 1);
    }

    private final void testBackpressureOnFastProducerSlowConsumerWithUnsubscribe(Scheduler scheduler, int bufferSize) throws InterruptedException {
        final AtomicInteger countEmitted = new AtomicInteger();
        final AtomicInteger countTaken = new AtomicInteger();
        int value = Observable.create(new OnSubscribeFunc<Integer>() {

            @Override
            public Subscription onSubscribe(final Observer<? super Integer> o) {
                final BooleanSubscription s = BooleanSubscription.create();
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        int i = 1;
                        while (!s.isUnsubscribed() && i <= 100) {
                            //                            System.out.println("onNext from fast producer [" + Thread.currentThread() + "]: " + i);
                            o.onNext(i++);
                        }
                        o.onCompleted();
                    }
                });
                t.setDaemon(true);
                t.start();
                return s;
            }
        }).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer i) {
                countEmitted.incrementAndGet();
            }
        }).doOnCompleted(new Action0() {

            @Override
            public void call() {
                //                System.out.println("-------- Done Emitting from Source ---------");
            }
        }).lift(new OperatorObserveOnBounded<Integer>(scheduler, bufferSize)).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer i) {
                //                System.out.println(">> onNext to slowConsumer  [" + Thread.currentThread() + "] pre-take: " + i);
                //force it to be slower than the producer
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countTaken.incrementAndGet();
            }
        }).take(10).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer t) {
                System.out.println("*********** value: " + t);
            }

        }).toBlockingObservable().last();

        if (scheduler instanceof TrampolineScheduler || scheduler instanceof ImmediateScheduler || scheduler instanceof TestScheduler) {
            // since there is no concurrency it will block and only emit as many as it can process
            assertEquals(10, countEmitted.get());
        } else {
            // the others with concurrency should not emit all 100 ... but 10 + 2 in the pipeline
            // NOTE: The +2 could change if the implementation of the queue logic changes. See Javadoc at top of class.
            assertEquals(11, countEmitted.get(), bufferSize); // can be up to 11 + bufferSize
        }
        // number received after take (but take will filter any extra)
        assertEquals(10, value);
        // so we also want to check the doOnNext after observeOn to see if it got unsubscribed
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        // we expect only 10 to make it through the observeOn side
        assertEquals(10, countTaken.get());
    }

    private static int randomIntFrom0to100() {
        // XORShift instead of Math.random http://javamex.com/tutorials/random_numbers/xorshift.shtml
        long x = System.nanoTime();
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return Math.abs((int) x % 100);
    }
}
