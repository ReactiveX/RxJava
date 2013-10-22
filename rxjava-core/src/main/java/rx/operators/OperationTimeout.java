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
package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

/**
 * Applies a timeout policy for each element in the observable sequence, using
 * the specified scheduler to run timeout timers. If the next element isn't
 * received within the specified timeout duration starting from its predecessor,
 * the other observable sequence is used to produce future messages from that
 * point on.
 */
public class OperationTimeout {

    public static <T> OnSubscribeFunc<T> timeout(
            Observable<? extends T> sequence, long timeout, TimeUnit unit) {
        return new TimeoutObservable<T>(sequence, timeout, unit, null,
                Schedulers.threadPoolForComputation());
    }

    public static <T> OnSubscribeFunc<T> timeout(
            Observable<? extends T> sequence, long timeout, TimeUnit unit,
            Observable<? extends T> other) {
        return new TimeoutObservable<T>(sequence, timeout, unit, other,
                Schedulers.threadPoolForComputation());
    }

    public static <T> OnSubscribeFunc<T> timeout(
            Observable<? extends T> sequence, long timeout, TimeUnit unit,
            Scheduler scheduler) {
        return new TimeoutObservable<T>(sequence, timeout, unit, null,
                scheduler);
    }

    public static <T> OnSubscribeFunc<T> timeout(
            Observable<? extends T> sequence, long timeout, TimeUnit unit,
            Observable<? extends T> other, Scheduler scheduler) {
        return new TimeoutObservable<T>(sequence, timeout, unit, other,
                scheduler);
    }

    private static class TimeoutObservable<T> implements OnSubscribeFunc<T> {

        private final Observable<? extends T> sequence;
        private final Observable<? extends T> other;
        private final long timeout;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        public TimeoutObservable(Observable<? extends T> sequence,
                long timeout, TimeUnit unit, Observable<? extends T> other,
                Scheduler scheduler) {
            this.sequence = sequence;
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
            this.other = other;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final CompositeSubscription parentSubscription = new CompositeSubscription();

            final SafeObservableSubscription subscription = new SafeObservableSubscription();
            subscription.wrap(sequence.subscribe(new TimeoutObserver(observer,
                    subscription, parentSubscription)));

            parentSubscription.add(subscription);
            return parentSubscription;
        }

        private class TimeoutObserver implements Observer<T> {

            private final AtomicLong lastActionId = new AtomicLong();
            private final AtomicReference<Subscription> lastScheduledNotification = new AtomicReference<Subscription>();

            private final Observer<? super T> observer;
            private final Observer<? super T> underlying;
            private final CompositeSubscription parentSubscription;

            private SafeObservableSubscription subscription;

            public TimeoutObserver(Observer<? super T> underlying,
                    SafeObservableSubscription subscription,
                    CompositeSubscription parentSubscription) {
                this.underlying = underlying;
                this.observer = new SynchronizedObserver<T>(underlying,
                        subscription);
                this.parentSubscription = parentSubscription;
                this.subscription = subscription;
                setupTimeout();
            }

            private void setupTimeout() {
                final long currentActionId = lastActionId.incrementAndGet();
                Subscription previousSubscription = lastScheduledNotification
                        .getAndSet(scheduler.schedule(new Action0() {
                            @Override
                            public void call() {
                                if (lastActionId.get() == currentActionId) {
                                    // No action happens, so it's a timeout.
                                    if (other == null) {
                                        observer.onError(new TimeoutException(
                                                "The operator has timed out"));
                                        subscription.unsubscribe();
                                    } else {
                                        subscription.unsubscribe();
                                        if (!parentSubscription
                                                .isUnsubscribed()) {
                                            parentSubscription
                                                    .add(new SafeObservableSubscription().wrap(other
                                                            .subscribe(underlying)));
                                        }
                                    }
                                }
                            }
                        }, timeout, unit));
                if (previousSubscription != null) {
                    previousSubscription.unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                lastActionId.incrementAndGet();
                observer.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                lastActionId.incrementAndGet();
                observer.onError(e);
            }

            @Override
            public void onNext(T args) {
                setupTimeout();
                observer.onNext(args);
            }
        }
    }

    public static class UnitTest {

        @Test
        public void testTimeout() {
            Observable<String> source = Observable.create(
                    new OnSubscribeFunc<String>() {
                        public Subscription onSubscribe(
                                Observer<? super String> t1) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            t1.onNext("a");
                            t1.onCompleted();
                            return Subscriptions.empty();
                        }
                    }).timeout(500, TimeUnit.MILLISECONDS);

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            source.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onError(
                    any(TimeoutException.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testTimeout2() {
            Observable<String> source = Observable.create(
                    new OnSubscribeFunc<String>() {
                        public Subscription onSubscribe(
                                Observer<? super String> t1) {
                            t1.onNext("a");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            t1.onNext("b");
                            t1.onCompleted();
                            return Subscriptions.empty();
                        }
                    }).timeout(500, TimeUnit.MILLISECONDS);

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            source.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext("a");
            inOrder.verify(observer, times(1)).onError(
                    any(TimeoutException.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testTimeoutWithOther() {
            Observable<String> other = Observable.from("x", "y", "z");

            Observable<String> source = Observable.create(
                    new OnSubscribeFunc<String>() {
                        public Subscription onSubscribe(
                                Observer<? super String> t1) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            t1.onNext("a");
                            t1.onCompleted();
                            return Subscriptions.empty();
                        }
                    }).timeout(500, TimeUnit.MILLISECONDS, other);

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            source.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext("x");
            inOrder.verify(observer, times(1)).onNext("y");
            inOrder.verify(observer, times(1)).onNext("z");
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testTimeoutWithOther2() {
            Observable<String> other = Observable.from("x", "y", "z");

            Observable<String> source = Observable.create(
                    new OnSubscribeFunc<String>() {
                        public Subscription onSubscribe(
                                Observer<? super String> t1) {
                            t1.onNext("a");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            t1.onNext("b");
                            t1.onCompleted();
                            return Subscriptions.empty();
                        }
                    }).timeout(500, TimeUnit.MILLISECONDS, other);

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            source.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext("a");
            inOrder.verify(observer, times(1)).onNext("x");
            inOrder.verify(observer, times(1)).onNext("y");
            inOrder.verify(observer, times(1)).onNext("z");
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }
    }
}
