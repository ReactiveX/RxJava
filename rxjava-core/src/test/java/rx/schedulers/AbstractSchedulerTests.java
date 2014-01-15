/**
 * Copyright 2013 Netflix, Inc.
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
package rx.schedulers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.IObservable;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Base tests for all schedulers including Immediate/Current.
 */
public abstract class AbstractSchedulerTests {

    /**
     * The scheduler to test
     */
    protected abstract Scheduler getScheduler();

    @Test
    public final void unsubscribeWithFastProducerWithSlowConsumerCausingQueuing() throws InterruptedException {
        final AtomicInteger countEmitted = new AtomicInteger();
        final AtomicInteger countTaken = new AtomicInteger();
        int value = Observable.from(new IObservable<Integer>() {

            @Override
            public Subscription subscribe(final Observer<? super Integer> o) {
                final BooleanSubscription s = BooleanSubscription.create();
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        int i = 1;
                        while (!s.isUnsubscribed() && i <= 100) {
                            System.out.println("onNext from fast producer: " + i);
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
                System.out.println("-------- Done Emitting from Source ---------");
            }
        }).observeOn(getScheduler()).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer i) {
                System.out.println(">> onNext to slowConsumer pre-take: " + i);
                //force it to be slower than the producer
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countTaken.incrementAndGet();
            }
        }).take(10).toBlockingObservable().last();

        if (getScheduler() instanceof CurrentThreadScheduler || getScheduler() instanceof ImmediateScheduler) {
            // since there is no concurrency it will block and only emit as many as it can process
            assertEquals(10, countEmitted.get());
        } else {
            // they will all emit because the consumer is running slow
            assertEquals(100, countEmitted.get());
        }
        // number received after take (but take will filter any extra)
        assertEquals(10, value);
        // so we also want to check the doOnNext after observeOn to see if it got unsubscribed
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        // we expect only 10 to make it through the observeOn side
        assertEquals(10, countTaken.get());
    }

    @Test
    public void testNestedActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final CountDownLatch latch = new CountDownLatch(1);

        final Action0 firstStepStart = mock(Action0.class);
        final Action0 firstStepEnd = mock(Action0.class);

        final Action0 secondStepStart = mock(Action0.class);
        final Action0 secondStepEnd = mock(Action0.class);

        final Action0 thirdStepStart = mock(Action0.class);
        final Action0 thirdStepEnd = mock(Action0.class);

        final Action0 firstAction = new Action0() {
            @Override
            public void call() {
                firstStepStart.call();
                firstStepEnd.call();
                latch.countDown();
            }
        };
        final Action0 secondAction = new Action0() {
            @Override
            public void call() {
                secondStepStart.call();
                scheduler.schedule(firstAction);
                secondStepEnd.call();

            }
        };
        final Action0 thirdAction = new Action0() {
            @Override
            public void call() {
                thirdStepStart.call();
                scheduler.schedule(secondAction);
                thirdStepEnd.call();
            }
        };

        InOrder inOrder = inOrder(firstStepStart, firstStepEnd, secondStepStart, secondStepEnd, thirdStepStart, thirdStepEnd);

        scheduler.schedule(thirdAction);

        latch.await();

        inOrder.verify(thirdStepStart, times(1)).call();
        inOrder.verify(thirdStepEnd, times(1)).call();
        inOrder.verify(secondStepStart, times(1)).call();
        inOrder.verify(secondStepEnd, times(1)).call();
        inOrder.verify(firstStepStart, times(1)).call();
        inOrder.verify(firstStepEnd, times(1)).call();
    }

    @Test
    public final void testNestedScheduling() {

        Observable<Integer> ids = Observable.from(Arrays.asList(1, 2), getScheduler());

        Observable<String> m = ids.flatMap(new Func1<Integer, Observable<String>>() {

            @Override
            public Observable<String> call(Integer id) {
                return Observable.from(Arrays.asList("a-" + id, "b-" + id), getScheduler())
                        .map(new Func1<String, String>() {

                            @Override
                            public String call(String s) {
                                return "names=>" + s;
                            }
                        });
            }

        });

        List<String> strings = m.toList().toBlockingObservable().last();

        assertEquals(4, strings.size());
        // because flatMap does a merge there is no guarantee of order
        assertTrue(strings.contains("names=>a-1"));
        assertTrue(strings.contains("names=>a-2"));
        assertTrue(strings.contains("names=>b-1"));
        assertTrue(strings.contains("names=>b-2"));
    }

    /**
     * The order of execution is nondeterministic.
     * @throws InterruptedException
     */
    @SuppressWarnings("rawtypes")
    @Test
    public final void testSequenceOfActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(2);
        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);

        // make it wait until both the first and second are called
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                try {
                    return invocation.getMock();
                } finally {
                    latch.countDown();
                }
            }
        }).when(first).call();
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                try {
                    return invocation.getMock();
                } finally {
                    latch.countDown();
                }
            }
        }).when(second).call();

        scheduler.schedule(first);
        scheduler.schedule(second);

        latch.await();

        verify(first, times(1)).call();
        verify(second, times(1)).call();

    }

    @Test
    public void testSequenceOfDelayedActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(1);
        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);

        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                scheduler.schedule(first, 30, TimeUnit.MILLISECONDS);
                scheduler.schedule(second, 10, TimeUnit.MILLISECONDS);
                scheduler.schedule(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }
                }, 40, TimeUnit.MILLISECONDS);
            }
        });

        latch.await();
        InOrder inOrder = inOrder(first, second);

        inOrder.verify(second, times(1)).call();
        inOrder.verify(first, times(1)).call();

    }

    @Test
    public void testMixOfDelayedAndNonDelayedActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(1);
        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);
        final Action0 third = mock(Action0.class);
        final Action0 fourth = mock(Action0.class);

        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                scheduler.schedule(first);
                scheduler.schedule(second, 300, TimeUnit.MILLISECONDS);
                scheduler.schedule(third, 100, TimeUnit.MILLISECONDS);
                scheduler.schedule(fourth);
                scheduler.schedule(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }
                }, 400, TimeUnit.MILLISECONDS);
            }
        });

        latch.await();
        InOrder inOrder = inOrder(first, second, third, fourth);

        inOrder.verify(first, times(1)).call();
        inOrder.verify(fourth, times(1)).call();
        inOrder.verify(third, times(1)).call();
        inOrder.verify(second, times(1)).call();
    }

    @Test
    public final void testRecursiveExecutionWithAction0() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final AtomicInteger i = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        scheduler.schedule(new Action1<Action0>() {

            @Override
            public void call(Action0 self) {
                if (i.incrementAndGet() < 100) {
                    self.call();
                } else {
                    latch.countDown();
                }
            }
        });

        latch.await();
        assertEquals(100, i.get());
    }

    @Test
    public final void testRecursiveExecutionWithFunc2() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final AtomicInteger i = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(0, new Func2<Scheduler, Integer, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Integer state) {
                i.set(state);
                if (state < 100) {
                    return innerScheduler.schedule(state + 1, this);
                } else {
                    latch.countDown();
                    return Subscriptions.empty();
                }
            }

        });

        latch.await();
        assertEquals(100, i.get());
    }

    @Test
    public final void testRecursiveExecutionWithFunc2AndDelayTime() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final AtomicInteger i = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(0, new Func2<Scheduler, Integer, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Integer state) {
                i.set(state);
                if (state < 100) {
                    return innerScheduler.schedule(state + 1, this, 5, TimeUnit.MILLISECONDS);
                } else {
                    latch.countDown();
                    return Subscriptions.empty();
                }
            }

        }, 50, TimeUnit.MILLISECONDS);

        latch.await();
        assertEquals(100, i.get());
    }

    @Test
    public final void testRecursiveSchedulerSimple() {
        final Scheduler scheduler = getScheduler();

        Observable<Integer> obs = Observable.from(new IObservable<Integer>() {
            @Override
            public Subscription subscribe(final Observer<? super Integer> observer) {
                return scheduler.schedule(0, new Func2<Scheduler, Integer, Subscription>() {
                    @Override
                    public Subscription call(Scheduler scheduler, Integer i) {
                        if (i > 42) {
                            observer.onCompleted();
                            return Subscriptions.empty();
                        }

                        observer.onNext(i);

                        return scheduler.schedule(i + 1, this);
                    }
                });
            }
        });

        final AtomicInteger lastValue = new AtomicInteger();
        obs.toBlockingObservable().forEach(new Action1<Integer>() {

            @Override
            public void call(Integer v) {
                System.out.println("Value: " + v);
                lastValue.set(v);
            }
        });

        assertEquals(42, lastValue.get());
    }

    @Test
    public final void testSchedulingWithDueTime() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(5);
        final AtomicInteger counter = new AtomicInteger();

        long start = System.currentTimeMillis();

        scheduler.schedule(null, new Func2<Scheduler, String, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, String state) {
                System.out.println("doing work");
                counter.incrementAndGet();
                latch.countDown();
                if (latch.getCount() == 0) {
                    return Subscriptions.empty();
                } else {
                    return scheduler.schedule(state, this, new Date(scheduler.now() + 50));
                }
            }
        }, new Date(scheduler.now() + 100));

        if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
            fail("didn't execute ... timed out");
        }

        long end = System.currentTimeMillis();

        assertEquals(5, counter.get());
        System.out.println("Time taken: " + (end - start));
        if ((end - start) < 250) {
            fail("it should have taken over 250ms since each step was scheduled 50ms in the future");
        }
    }

    @Test
    public final void testConcurrentOnNextFailsValidation() throws InterruptedException {
        final int count = 10;
        final CountDownLatch latch = new CountDownLatch(count);
        IObservable<String> o = new IObservable<String>() {

            @Override
            public Subscription subscribe(final Observer<? super String> observer) {
                for (int i = 0; i < count; i++) {
                    final int v = i;
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            observer.onNext("v: " + v);

                            latch.countDown();
                        }
                    }).start();
                }
                return Subscriptions.empty();
            }
        };

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<String>();
        // this should call onNext concurrently
        o.subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() == null) {
            fail("We expected error messages due to concurrency");
        }
    }

    @Test
    public final void testObserveOn() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        Observable<String> o = Observable.from("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<String>();

        o.observeOn(scheduler).subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() != null) {
            observer.error.get().printStackTrace();
            fail("Error: " + observer.error.get().getMessage());
        }
    }

    @Test
    public final void testSubscribeOnNestedConcurrency() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        Observable<String> o = Observable.from("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
                .mergeMap(new Func1<String, Observable<String>>() {

                    @Override
                    public Observable<String> call(final String v) {
                        return Observable.from(new IObservable<String>() {

                            @Override
                            public Subscription subscribe(final Observer<? super String> observer) {
                                observer.onNext("value_after_map-" + v);
                                observer.onCompleted();
                                return Subscriptions.empty();
                            }
                        }).subscribeOn(scheduler);
                    }
                });

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<String>();

        o.subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() != null) {
            observer.error.get().printStackTrace();
            fail("Error: " + observer.error.get().getMessage());
        }
    }

    /**
     * Used to determine if onNext is being invoked concurrently.
     * 
     * @param <T>
     */
    private static class ConcurrentObserverValidator<T> implements Observer<T> {

        final AtomicInteger concurrentCounter = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final CountDownLatch completed = new CountDownLatch(1);

        @Override
        public void onCompleted() {
            completed.countDown();
        }

        @Override
        public void onError(Throwable e) {
            completed.countDown();
            error.set(e);
        }

        @Override
        public void onNext(T args) {
            int count = concurrentCounter.incrementAndGet();
            System.out.println("ConcurrentObserverValidator.onNext: " + args);
            if (count > 1) {
                onError(new RuntimeException("we should not have concurrent execution of onNext"));
            }
            try {
                try {
                    // take some time so other onNext calls could pile up (I haven't yet thought of a way to do this without sleeping)
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // ignore
                }
            } finally {
                concurrentCounter.decrementAndGet();
            }
        }

    }

}
