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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import static org.mockito.Mockito.*;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.Subscribers;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

public class OperatorSubscribeOnTest {

    private class ThreadSubscription implements Subscription {
        private volatile Thread thread;

        private final CountDownLatch latch = new CountDownLatch(1);

        private final Subscription s = Subscriptions.create(new Action0() {

            @Override
            public void call() {
                thread = Thread.currentThread();
                latch.countDown();
            }

        });

        @Override
        public void unsubscribe() {
            s.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return s.isUnsubscribed();
        }

        public Thread getThread() throws InterruptedException {
            latch.await();
            return thread;
        }
    }

    @Test
    public void testSubscribeOnAndVerifySubscribeAndUnsubscribeThreads()
            throws InterruptedException {
        final ThreadSubscription subscription = new ThreadSubscription();
        final AtomicReference<Thread> subscribeThread = new AtomicReference<Thread>();
        Observable<Integer> w = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> t1) {
                subscribeThread.set(Thread.currentThread());
                t1.add(subscription);
                t1.onNext(1);
                t1.onNext(2);
                t1.onCompleted();
            }
        });

        TestObserver<Integer> observer = new TestObserver<Integer>();
        w.subscribeOn(Schedulers.newThread()).subscribe(observer);

        Thread unsubscribeThread = subscription.getThread();

        assertNotNull(unsubscribeThread);
        assertNotSame(Thread.currentThread(), unsubscribeThread);

        assertNotNull(subscribeThread.get());
        assertNotSame(Thread.currentThread(), subscribeThread.get());
        // True for Schedulers.newThread()
        assertTrue(unsubscribeThread == subscribeThread.get());

        observer.assertReceivedOnNext(Arrays.asList(1, 2));
        observer.assertTerminalEvent();
    }

    @Test(timeout = 1000)
    public void testIssue813() throws InterruptedException {
        // https://github.com/Netflix/RxJava/issues/813
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);

        TestObserver<Integer> observer = new TestObserver<Integer>();
        final ThreadSubscription s = new ThreadSubscription();

        final Subscription subscription = Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(
                            final Subscriber<? super Integer> subscriber) {
                        subscriber.add(s);
                        try {
                            latch.await();
                            // Already called "unsubscribe", "isUnsubscribed"
                            // shouble be true
                            if (!subscriber.isUnsubscribed()) {
                                throw new IllegalStateException(
                                        "subscriber.isUnsubscribed should be true");
                            }
                            subscriber.onCompleted();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Throwable e) {
                            subscriber.onError(e);
                        } finally {
                            doneLatch.countDown();
                        }
                    }
                }).subscribeOn(Schedulers.computation()).subscribe(observer);

        subscription.unsubscribe();
        // As unsubscribe is called in other thread, we need to wait for it.
        s.getThread();
        latch.countDown();
        doneLatch.await();
        assertEquals(0, observer.getOnErrorEvents().size());
        assertEquals(1, observer.getOnCompletedEvents().size());
    }

    @Test
    public void testSynchronousSubscribeOnThread() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.range(1, 10).subscribeOn(Schedulers.newThread()).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testInfiniteStream() {
        final AtomicInteger counter = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                for (int i = 1; !s.isUnsubscribed(); i++) {
                    s.onNext(i);
                    counter.incrementAndGet();
                }
            }

        }).subscribeOn(Schedulers.newThread()).take(10).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(10, counter.get());
        System.out.println("Executed on thread: " + ts.getLastSeenThread());
        assertTrue(ts.getLastSeenThread().getName().startsWith("RxNewThreadScheduler"));
    }

    /**
     * This is used to demonstrate the "time gap" issue: https://github.com/Netflix/RxJava/pull/848
     */
    @Test
    public void testSubscribeOnPublishSubjectWithSlowScheduler() {
        PublishSubject<Integer> ps = PublishSubject.create();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ps.subscribeOn(new SlowScheduler()).subscribe(ts);
        ps.onNext(1);
        ps.onNext(2);
        ps.onCompleted();

        ts.awaitTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2));
    }

    private class SlowScheduler extends Scheduler {

        @Override
        public Subscription schedule(Action1<Inner> action) {
            InnerSlowScheduler inner = new InnerSlowScheduler();
            inner.schedule(action);
            return inner;
        }

        @Override
        public Subscription schedule(Action1<Inner> action, long delayTime, TimeUnit unit) {
            InnerSlowScheduler inner = new InnerSlowScheduler();
            inner.schedule(action, delayTime, unit);
            return inner;
        }

        private class InnerSlowScheduler extends Inner {

            volatile Inner threadInner;

            public InnerSlowScheduler() {
                final CountDownLatch latch = new CountDownLatch(1);
                Schedulers.newThread().schedule(new Action1<Inner>() {

                    @Override
                    public void call(Inner inner) {
                        threadInner = inner;
                        latch.countDown();
                    }

                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            private final BooleanSubscription s = new BooleanSubscription();

            @Override
            public void unsubscribe() {
                s.unsubscribe();
            }

            @Override
            public boolean isUnsubscribed() {
                return s.isUnsubscribed();
            }

            @Override
            public void schedule(final Action1<Inner> action, long delayTime, TimeUnit unit) {
                threadInner.schedule(new Action1<Inner>() {

                    @Override
                    public void call(Inner inner) {
                        // inject delay to simulate slow scheduling
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        action.call(inner);
                    }

                }, delayTime, unit);
            }

            @Override
            public void schedule(final Action1<Inner> action) {
                threadInner.schedule(new Action1<Inner>() {

                    @Override
                    public void call(Inner inner) {
                        // inject delay to simulate slow scheduling
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        action.call(inner);
                    }

                });
            }

        }
    }
    @Test(timeout = 1000)
    public void testCancelBeforeRun() throws Exception {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        Subscriber<Object> s = Subscribers.from(o);
        s.unsubscribe();
        
        Observable.from(1).subscribeOn(Schedulers.newThread()).subscribe(s);
        Thread.sleep(100);
        
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onCompleted();
    }
    
    static class DelayingScheduler extends Scheduler {
        final Scheduler actual;
        final long delay;
        final TimeUnit unit;

        public DelayingScheduler(Scheduler actual, long delay, TimeUnit unit) {
            this.actual = actual;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        public Subscription schedule(final Action1<Scheduler.Inner> action) {
            final CompositeSubscription cs = new CompositeSubscription();
            final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
            cs.add(mas);
            mas.set(actual.schedule(new Action1<Inner>() {

                @Override
                public void call(Inner t1) {
                    cs.delete(mas);
                    cs.add(actual.schedule(action, delay, unit));
                }
                
            }));
            return cs;
        }

        @Override
        public Subscription schedule(final Action1<Scheduler.Inner> action, final long delayTime, final TimeUnit delayUnit) {
            final CompositeSubscription cs = new CompositeSubscription();
            final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
            cs.add(mas);
            mas.set(actual.schedule(new Action1<Inner>() {
                @Override
                public void call(Inner t1) {
                    cs.delete(mas);
                    long nanos = unit.toNanos(delay) + delayUnit.toNanos(delayTime);
                    cs.add(actual.schedule(action, nanos, TimeUnit.NANOSECONDS));
                }
            }));
            return cs;
        }
    }
    @Test(timeout = 1000)
    public void testCancelBeforeDelayedSubscribe() throws Exception {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        Subscriber<Object> s = Subscribers.from(o);
        s.unsubscribe();

        Observable.from(1)
                .subscribeOn(new DelayingScheduler(Schedulers.computation(), 2, TimeUnit.SECONDS))
                .subscribe(s);
        
        Thread.sleep(100);
    }
}
