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
package rx.schedulers;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * Base tests for all schedulers including Immediate/Current.
 */
public abstract class AbstractSchedulerTests {

    /**
     * The scheduler to test
     */
    protected abstract Scheduler getScheduler();

    @Test
    public void testNestedActions() throws InterruptedException {
        Scheduler scheduler = getScheduler();
        final CountDownLatch latch = new CountDownLatch(1);

        final Action0 firstStepStart = mock(Action0.class);
        final Action0 firstStepEnd = mock(Action0.class);

        final Action0 secondStepStart = mock(Action0.class);
        final Action0 secondStepEnd = mock(Action0.class);

        final Action0 thirdStepStart = mock(Action0.class);
        final Action0 thirdStepEnd = mock(Action0.class);

        final Action1<Inner> firstAction = new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                firstStepStart.call();
                firstStepEnd.call();
                latch.countDown();
            }
        };
        final Action1<Inner> secondAction = new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                secondStepStart.call();
                inner.schedule(firstAction);
                secondStepEnd.call();

            }
        };
        final Action1<Inner> thirdAction = new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                thirdStepStart.call();
                inner.schedule(secondAction);
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
     * 
     * @throws InterruptedException
     */
    @SuppressWarnings("rawtypes")
    @Test
    public final void testSequenceOfActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(2);
        final Action1<Inner> first = mock(Action1.class);
        final Action1<Inner> second = mock(Action1.class);

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
        }).when(first).call(any(Inner.class));
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                try {
                    return invocation.getMock();
                } finally {
                    latch.countDown();
                }
            }
        }).when(second).call(any(Inner.class));

        scheduler.schedule(first);
        scheduler.schedule(second);

        latch.await();

        verify(first, times(1)).call(any(Inner.class));
        verify(second, times(1)).call(any(Inner.class));

    }

    @Test
    public void testSequenceOfDelayedActions() throws InterruptedException {
        Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(1);
        final Action1<Inner> first = mock(Action1.class);
        final Action1<Inner> second = mock(Action1.class);

        scheduler.schedule(new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                inner.schedule(first, 30, TimeUnit.MILLISECONDS);
                inner.schedule(second, 10, TimeUnit.MILLISECONDS);
                inner.schedule(new Action1<Inner>() {

                    @Override
                    public void call(Inner inner) {
                        latch.countDown();
                    }
                }, 40, TimeUnit.MILLISECONDS);
            }
        });

        latch.await();
        InOrder inOrder = inOrder(first, second);

        inOrder.verify(second, times(1)).call(any(Inner.class));
        inOrder.verify(first, times(1)).call(any(Inner.class));

    }

    @Test
    public void testMixOfDelayedAndNonDelayedActions() throws InterruptedException {
        Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(1);
        final Action1<Inner> first = mock(Action1.class);
        final Action1<Inner> second = mock(Action1.class);
        final Action1<Inner> third = mock(Action1.class);
        final Action1<Inner> fourth = mock(Action1.class);

        scheduler.schedule(new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                inner.schedule(first);
                inner.schedule(second, 300, TimeUnit.MILLISECONDS);
                inner.schedule(third, 100, TimeUnit.MILLISECONDS);
                inner.schedule(fourth);
                inner.schedule(new Action1<Inner>() {

                    @Override
                    public void call(Inner inner) {
                        latch.countDown();
                    }
                }, 400, TimeUnit.MILLISECONDS);
            }
        });

        latch.await();
        InOrder inOrder = inOrder(first, second, third, fourth);

        inOrder.verify(first, times(1)).call(any(Inner.class));
        inOrder.verify(fourth, times(1)).call(any(Inner.class));
        inOrder.verify(third, times(1)).call(any(Inner.class));
        inOrder.verify(second, times(1)).call(any(Inner.class));
    }

    @Test
    public final void testRecursiveExecution() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final AtomicInteger i = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        scheduler.schedule(new Action1<Inner>() {

            @Override
            public void call(Inner inner) {
                if (i.incrementAndGet() < 100) {
                    inner.schedule(this);
                } else {
                    latch.countDown();
                }
            }
        });

        latch.await();
        assertEquals(100, i.get());
    }

    @Test
    public final void testRecursiveExecutionWithDelayTime() throws InterruptedException {
        Scheduler scheduler = getScheduler();
        final AtomicInteger i = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(new Action1<Inner>() {

            int state = 0;

            @Override
            public void call(Inner inner) {
                i.set(state);
                if (state++ < 100) {
                    inner.schedule(this, 1, TimeUnit.MILLISECONDS);
                } else {
                    latch.countDown();
                }
            }

        });

        latch.await();
        assertEquals(100, i.get());
    }

    @Test
    public final void testRecursiveSchedulerInObservable() {
        Observable<Integer> obs = Observable.create(new OnSubscribeFunc<Integer>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Integer> observer) {
                return getScheduler().schedule(new Action1<Inner>() {
                    int i = 0;

                    @Override
                    public void call(Inner inner) {
                        if (i > 42) {
                            observer.onCompleted();
                            return;
                        }

                        observer.onNext(i++);

                        inner.schedule(this);
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
    public final void testConcurrentOnNextFailsValidation() throws InterruptedException {
        final int count = 10;
        final CountDownLatch latch = new CountDownLatch(count);
        Observable<String> o = Observable.create(new OnSubscribeFunc<String>() {

            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
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
        });

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
                        return Observable.create(new OnSubscribeFunc<String>() {

                            @Override
                            public Subscription onSubscribe(final Observer<? super String> observer) {
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
    private static class ConcurrentObserverValidator<T> extends Subscriber<T> {

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
