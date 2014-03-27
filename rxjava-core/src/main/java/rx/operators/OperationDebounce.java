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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.SerializedObserver;
import rx.observers.SynchronizedObserver;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;

/**
 * This operation is used to filter out bursts of events. This is done by ignoring the events from an observable which are too
 * quickly followed up with other values. Values which are not followed up by other values within the specified timeout are published
 * as soon as the timeout expires.
 */
public final class OperationDebounce {

    /**
     * This operation filters out events which are published too quickly in succession. This is done by dropping events which are
     * followed up by other events before a specified timer has expired. If the timer expires and no follow up event was published (yet)
     * the last received event is published.
     * 
     * @param items
     *            The {@link Observable} which is publishing events.
     * @param timeout
     *            How long each event has to be the 'last event' before it gets published.
     * @param unit
     *            The unit of time for the specified timeout.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> debounce(Observable<T> items, long timeout, TimeUnit unit) {
        return debounce(items, timeout, unit, Schedulers.computation());
    }

    /**
     * This operation filters out events which are published too quickly in succession. This is done by dropping events which are
     * followed up by other events before a specified timer has expired. If the timer expires and no follow up event was published (yet)
     * the last received event is published.
     * 
     * @param items
     *            The {@link Observable} which is publishing events.
     * @param timeout
     *            How long each event has to be the 'last event' before it gets published.
     * @param unit
     *            The unit of time for the specified timeout.
     * @param scheduler
     *            The {@link Scheduler} to use internally to manage the timers which handle timeout for each event.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> debounce(final Observable<T> items, final long timeout, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new Debounce<T>(items, timeout, unit, scheduler).onSubscribe(observer);
            }
        };
    }

    private static class Debounce<T> implements OnSubscribeFunc<T> {

        private final Observable<T> items;
        private final long timeout;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        public Debounce(Observable<T> items, long timeout, TimeUnit unit, Scheduler scheduler) {
            this.items = items;
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            return items.subscribe(new DebounceObserver<T>(observer, timeout, unit, scheduler));
        }
    }

    private static class DebounceObserver<T> implements Observer<T> {

        private final Observer<? super T> observer;
        private final long timeout;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        private final AtomicReference<Subscription> lastScheduledNotification = new AtomicReference<Subscription>();

        public DebounceObserver(Observer<? super T> observer, long timeout, TimeUnit unit, Scheduler scheduler) {
            // we need to synchronize the observer since the on* events can be coming from different
            // threads and are thus non-deterministic and could be interleaved
            this.observer = new SerializedObserver<T>(observer);
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onCompleted() {
            /*
             * Cancel previous subscription if it has not already executed.
             * Expected that some race-condition will occur as this is crossing over thread boundaries
             * We are using SynchronizedObserver around 'observer' to handle interleaving and out-of-order calls.
             */
            lastScheduledNotification.get().unsubscribe();
            observer.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            /*
             * Cancel previous subscription if it has not already executed.
             * Expected that some race-condition will occur as this is crossing over thread boundaries
             * We are using SynchronizedObserver around 'observer' to handle interleaving and out-of-order calls.
             */
            lastScheduledNotification.get().unsubscribe();
            observer.onError(e);
        }

        @Override
        public void onNext(final T v) {
            Subscription previousSubscription = lastScheduledNotification.getAndSet(scheduler.schedule(new Action1<Inner>() {

                @Override
                public void call(Inner inner) {
                    observer.onNext(v);
                }

            }, timeout, unit));

            // cancel previous if not already executed
            if (previousSubscription != null) {
                previousSubscription.unsubscribe();
            }
        }
    }

    /**
     * Delay the emission via another observable if no new source appears in the meantime.
     */
    public static <T, U> OnSubscribeFunc<T> debounceSelector(
            Observable<? extends T> source,
            Func1<? super T, ? extends Observable<U>> debounceSelector) {
        return new DebounceSelector<T, U>(source, debounceSelector);
    }

    /**
     * Delay the emission via another observable if no new source appears in the meantime.
     */
    private static final class DebounceSelector<T, U> implements OnSubscribeFunc<T> {
        final Observable<? extends T> source;
        final Func1<? super T, ? extends Observable<U>> debounceSelector;

        public DebounceSelector(Observable<? extends T> source, Func1<? super T, ? extends Observable<U>> debounceSelector) {
            this.source = source;
            this.debounceSelector = debounceSelector;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            CompositeSubscription csub = new CompositeSubscription();

            csub.add(source.subscribe(new SourceObserver<T, U>(t1, debounceSelector, csub)));

            return csub;
        }

        /** Observe the source. */
        private static final class SourceObserver<T, U> implements Observer<T> {
            final Observer<? super T> observer;
            final Func1<? super T, ? extends Observable<U>> debounceSelector;
            final CompositeSubscription cancel;
            final SerialSubscription ssub = new SerialSubscription();
            long index;
            T value;
            boolean hasValue;
            final Object guard;

            public SourceObserver(
                    Observer<? super T> observer,
                    Func1<? super T, ? extends Observable<U>> debounceSelector,
                    CompositeSubscription cancel) {
                this.observer = observer;
                this.debounceSelector = debounceSelector;
                this.cancel = cancel;
                this.cancel.add(ssub);
                this.guard = new Object();
            }

            @Override
            public void onNext(T args) {
                Observable<U> o;
                try {
                    o = debounceSelector.call(args);
                } catch (Throwable t) {
                    synchronized (guard) {
                        observer.onError(t);
                    }
                    cancel.unsubscribe();
                    return;
                }
                long currentIndex;
                synchronized (guard) {
                    hasValue = true;
                    value = args;
                    currentIndex = ++index;
                }

                SerialSubscription osub = new SerialSubscription();
                ssub.set(osub);

                osub.set(o.subscribe(new DebounceObserver<T, U>(this, osub, args, currentIndex)));
            }

            @Override
            public void onError(Throwable e) {
                ssub.unsubscribe();
                try {
                    synchronized (guard) {
                        observer.onError(e);
                        hasValue = false;
                        value = null;
                        index++;
                    }
                } finally {
                    cancel.unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                ssub.unsubscribe();
                try {
                    synchronized (guard) {
                        if (hasValue) {
                            try {
                                observer.onNext(value);
                            } catch (Throwable t) {
                                observer.onError(t);
                                return;
                            }
                        }
                        observer.onCompleted();
                        hasValue = false;
                        value = null;
                        index++;
                    }
                } finally {
                    cancel.unsubscribe();
                }
            }
        }

        /**
         * The debounce observer.
         */
        private static final class DebounceObserver<T, U> implements Observer<U> {
            final SourceObserver<T, U> parent;
            final Subscription cancel;
            final T value;
            final long currentIndex;

            public DebounceObserver(SourceObserver<T, U> parent, Subscription cancel, T value, long currentIndex) {
                this.parent = parent;
                this.cancel = cancel;
                this.value = value;
                this.currentIndex = currentIndex;
            }

            @Override
            public void onNext(U args) {
                onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                synchronized (parent.guard) {
                    parent.observer.onError(e);
                }
                parent.cancel.unsubscribe();
            }

            @Override
            public void onCompleted() {
                synchronized (parent.guard) {
                    if (parent.hasValue && parent.index == currentIndex) {
                        parent.observer.onNext(value);
                    }
                    parent.hasValue = false;
                }
                cancel.unsubscribe();
            }

        }
    }
}
