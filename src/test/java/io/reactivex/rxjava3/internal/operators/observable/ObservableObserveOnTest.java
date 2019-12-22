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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableObserveOnTest.DisposeTrackingScheduler;
import io.reactivex.rxjava3.internal.operators.observable.ObservableObserveOn.ObserveOnObserver;
import io.reactivex.rxjava3.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableObserveOnTest extends RxJavaTest {

    /**
     * This is testing a no-op path since it uses Schedulers.immediate() which will not do scheduling.
     */
    @Test
    public void observeOn() {
        Observer<Integer> observer = TestHelper.mockObserver();
        Observable.just(1, 2, 3).observeOn(ImmediateThinScheduler.INSTANCE).subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void ordering() throws InterruptedException {
//        Observable<String> obs = Observable.just("one", null, "two", "three", "four");
        // FIXME null values not allowed
        Observable<String> obs = Observable.just("one", "null", "two", "three", "four");

        Observer<String> observer = TestHelper.mockObserver();

        InOrder inOrder = inOrder(observer);
        TestObserverEx<String> to = new TestObserverEx<>(observer);

        obs.observeOn(Schedulers.computation()).subscribe(to);

        to.awaitDone(1000, TimeUnit.MILLISECONDS);
        if (to.errors().size() > 0) {
            for (Throwable t : to.errors()) {
                t.printStackTrace();
            }
            fail("failed with exception");
        }

        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("null");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void threadName() throws InterruptedException {
        System.out.println("Main Thread: " + Thread.currentThread().getName());
        // FIXME null values not allowed
//        Observable<String> obs = Observable.just("one", null, "two", "three", "four");
        Observable<String> obs = Observable.just("one", "null", "two", "three", "four");

        Observer<String> observer = TestHelper.mockObserver();
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

        }).doAfterTerminate(new Action() {

            @Override
            public void run() {
                completedLatch.countDown();

            }
        }).subscribe(observer);

        if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting");
        }

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(5)).onNext(any(String.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void observeOnTheSameSchedulerTwice() {
        Scheduler scheduler = ImmediateThinScheduler.INSTANCE;

        Observable<Integer> o = Observable.just(1, 2, 3);
        Observable<Integer> o2 = o.observeOn(scheduler);

        Observer<Object> observer1 = TestHelper.mockObserver();
        Observer<Object> observer2 = TestHelper.mockObserver();

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

        Observable<Integer> o = Observable.just(1, 2, 3);
        Observable<Integer> o1 = o.observeOn(scheduler1);
        Observable<Integer> o2 = o.observeOn(scheduler2);

        Observer<Object> observer1 = TestHelper.mockObserver();
        Observer<Object> observer2 = TestHelper.mockObserver();

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
     * Confirm that running on a NewThreadScheduler uses the same thread for the entire stream.
     */
    @Test
    public void observeOnWithNewThreadScheduler() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Observable.range(1, 100000).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.newThread())
        .blockingForEach(new Consumer<Integer>() {

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
    public void observeOnWithThreadPoolScheduler() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Observable.range(1, 100000).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.computation())
        .blockingForEach(new Consumer<Integer>() {

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
    public void observeOnOrderingConcurrency() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Observable.range(1, 10000).map(new Function<Integer, Integer>() {

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
        .blockingForEach(new Consumer<Integer>() {

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
    public void nonBlockingOuterWhileBlockingOnNext() throws InterruptedException {

        final CountDownLatch completedLatch = new CountDownLatch(1);
        final CountDownLatch nextLatch = new CountDownLatch(1);
        final AtomicLong completeTime = new AtomicLong();
        // use subscribeOn to make async, observeOn to move
        Observable.range(1, 2).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onComplete() {
                System.out.println("onComplete");
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
    public void delayedErrorDeliveryWhenSafeSubscriberUnsubscribes() {
        TestScheduler testScheduler = new TestScheduler();

        Observable<Integer> source = Observable.concat(Observable.<Integer> error(new TestException()), Observable.just(1));

        Observer<Integer> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.observeOn(testScheduler).subscribe(o);

        inOrder.verify(o, never()).onError(any(TestException.class));

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o, never()).onComplete();
    }

    @Test
    public void afterUnsubscribeCalledThenObserverOnNextNeverCalled() {
        final TestScheduler testScheduler = new TestScheduler();

        final Observer<Integer> observer = TestHelper.mockObserver();
        TestObserver<Integer> to = new TestObserver<>(observer);

        Observable.just(1, 2, 3)
                .observeOn(testScheduler)
                .subscribe(to);

        to.dispose();
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        final InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onNext(anyInt());
        inOrder.verify(observer, never()).onError(any(Exception.class));
        inOrder.verify(observer, never()).onComplete();
    }

    @Test
    public void backpressureWithTakeBefore() {
        final AtomicInteger generated = new AtomicInteger();
        Observable<Integer> o = Observable.fromIterable(new Iterable<Integer>() {
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

        TestObserver<Integer> to = new TestObserver<>();
        o
                .take(7)
                .observeOn(Schedulers.newThread())
                .subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertValues(0, 1, 2, 3, 4, 5, 6);
        assertEquals(7, generated.get());
    }

    @Test
    public void asyncChild() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(0, 100000).observeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(to);
        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
    }

    @Test
    public void delayError() {
        Observable.range(1, 5).concatWith(Observable.<Integer>error(new TestException()))
        .observeOn(Schedulers.computation(), true)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (v == 1) {
                    Thread.sleep(100);
                }
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void trampolineScheduler() {
        Observable.just(1)
        .observeOn(Schedulers.trampoline())
        .test()
        .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().observeOn(new TestScheduler()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.observeOn(new TestScheduler());
            }
        });
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestScheduler scheduler = new TestScheduler();
            TestObserver<Integer> to = new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onComplete();
                    observer.onNext(1);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .observeOn(scheduler)
            .test();

            scheduler.triggerActions();

            to.assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void inputSyncFused() {
        Observable.range(1, 5)
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void inputAsyncFused() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Integer> to = us.observeOn(Schedulers.single()).test();

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        to
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void inputAsyncFusedError() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Integer> to = us.observeOn(Schedulers.single()).test();

        us.onError(new TestException());

        to
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void inputAsyncFusedErrorDelayed() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Integer> to = us.observeOn(Schedulers.single(), true).test();

        us.onError(new TestException());

        to
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void outputFused() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        Observable.range(1, 5).hide()
        .observeOn(Schedulers.single())
        .subscribe(to);

        to.assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void outputFusedReject() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.SYNC);

        Observable.range(1, 5).hide()
        .observeOn(Schedulers.single())
        .subscribe(to);

        to.assertFusionMode(QueueFuseable.NONE)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void inputOutputAsyncFusedError() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        UnicastSubject<Integer> us = UnicastSubject.create();

        us.observeOn(Schedulers.single())
        .subscribe(to);

        us.onError(new TestException());

        to
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        to.assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void inputOutputAsyncFusedErrorDelayed() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        UnicastSubject<Integer> us = UnicastSubject.create();

        us.observeOn(Schedulers.single(), true)
        .subscribe(to);

        us.onError(new TestException());

        to
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        to.assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void outputFusedCancelReentrant() throws Exception {
        final UnicastSubject<Integer> us = UnicastSubject.create();

        final CountDownLatch cdl = new CountDownLatch(1);

        us.observeOn(Schedulers.single())
        .subscribe(new Observer<Integer>() {
            Disposable upstream;
            int count;
            @Override
            public void onSubscribe(Disposable d) {
                this.upstream = d;
                ((QueueDisposable<?>)d).requestFusion(QueueFuseable.ANY);
            }

            @Override
            public void onNext(Integer value) {
                if (++count == 1) {
                    us.onNext(2);
                    upstream.dispose();
                    cdl.countDown();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        us.onNext(1);

        cdl.await();
    }

    @Test
    public void nonFusedPollThrows() {
        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());

                @SuppressWarnings("unchecked")
                ObserveOnObserver<Integer> oo = (ObserveOnObserver<Integer>)observer;

                oo.queue = new SimpleQueue<Integer>() {

                    @Override
                    public boolean offer(Integer value) {
                        return false;
                    }

                    @Override
                    public boolean offer(Integer v1, Integer v2) {
                        return false;
                    }

                    @Nullable
                    @Override
                    public Integer poll() throws Exception {
                        throw new TestException();
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public void clear() {
                    }
                };

                oo.clear();

                oo.schedule();
            }
        }
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void outputFusedOneSignal() {
        final BehaviorSubject<Integer> bs = BehaviorSubject.createDefault(1);

        bs.observeOn(ImmediateThinScheduler.INSTANCE)
        .concatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                return Observable.just(v + 1);
            }
        })
        .subscribeWith(new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 2) {
                    bs.onNext(2);
                }
            }
        })
        .assertValuesOnly(2, 3);
    }

    @Test
    public void workerNotDisposedPrematurelyNormalInNormalOut() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        Observable.concat(
                Observable.just(1).hide().observeOn(s),
                Observable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void workerNotDisposedPrematurelySyncInNormalOut() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        Observable.concat(
                Observable.just(1).observeOn(s),
                Observable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void workerNotDisposedPrematurelyAsyncInNormalOut() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        UnicastSubject<Integer> up = UnicastSubject.create();
        up.onNext(1);
        up.onComplete();

        Observable.concat(
                up.observeOn(s),
                Observable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    static final class TestObserverFusedCanceling
            extends TestObserverEx<Integer> {

        TestObserverFusedCanceling() {
            super();
            initialFusionMode = QueueFuseable.ANY;
        }

        @Override
        public void onComplete() {
            dispose();
            super.onComplete();
        }
    }

    @Test
    public void workerNotDisposedPrematurelyNormalInAsyncOut() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        TestObserverEx<Integer> to = new TestObserverFusedCanceling();

        Observable.just(1).hide().observeOn(s).subscribe(to);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void fusedNoConcurrentCleanDueToCancel() {
        for (int j = 0; j < TestHelper.RACE_LONG_LOOPS; j++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final UnicastSubject<Integer> us = UnicastSubject.create();

                TestObserver<Integer> to = us.hide()
                .observeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .unsubscribeOn(Schedulers.computation())
                .firstOrError()
                .test();

                for (int i = 0; us.hasObservers() && i < 10000; i++) {
                    us.onNext(i);
                }

                to
                .awaitDone(5, TimeUnit.SECONDS)
                ;

                if (!errors.isEmpty()) {
                    throw new CompositeException(errors);
                }

                to.assertResult(0);
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }
}
