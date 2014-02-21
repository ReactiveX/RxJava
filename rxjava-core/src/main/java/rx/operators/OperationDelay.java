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

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;

public final class OperationDelay {

    public static <T> Observable<T> delay(Observable<T> observable, final long delay, final TimeUnit unit, final Scheduler scheduler) {
        // observable.map(x => Observable.timer(t).map(_ => x).startItAlreadyNow()).concat()
        Observable<Observable<T>> seqs = observable.map(new Func1<T, Observable<T>>() {
            public Observable<T> call(final T x) {
                ConnectableObservable<T> co = Observable.timer(delay, unit, scheduler).map(new Func1<Long, T>() {
                    @Override
                    public T call(Long ignored) {
                        return x;
                    }
                }).replay();
                co.connect();
                return co;
            }
        });
        return Observable.concat(seqs);
    }

    /**
     * Delays the subscription to the source by the given amount, running on the given scheduler.
     */
    public static <T> OnSubscribeFunc<T> delaySubscription(Observable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
        return new DelaySubscribeFunc<T>(source, time, unit, scheduler);
    }

    /** Subscribe function which schedules the actual subscription to source on a scheduler at a later time. */
    private static final class DelaySubscribeFunc<T> implements OnSubscribeFunc<T> {
        final Observable<? extends T> source;
        final Scheduler scheduler;
        final long time;
        final TimeUnit unit;

        public DelaySubscribeFunc(Observable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
            this.time = time;
            this.unit = unit;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> t1) {
            final SerialSubscription ssub = new SerialSubscription();

            ssub.set(scheduler.schedule(new Action1<Inner>() {
                @Override
                public void call(Inner inner) {
                    if (!ssub.isUnsubscribed()) {
                        ssub.set(source.subscribe(t1));
                    }
                }
            }, time, unit));

            return ssub;
        }
    }

    /**
     * Delay the emission of the source items by a per-item observable that fires its first element.
     */
    public static <T, U> OnSubscribeFunc<T> delay(Observable<? extends T> source,
            Func1<? super T, ? extends Observable<U>> itemDelay) {
        return new DelayViaObservable<T, Object, U>(source, null, itemDelay);
    }

    /**
     * Delay the subscription and emission of the source items by a per-item observable that fires its first element.
     */
    public static <T, U, V> OnSubscribeFunc<T> delay(Observable<? extends T> source,
            Func0<? extends Observable<U>> subscriptionDelay,
            Func1<? super T, ? extends Observable<V>> itemDelay) {
        return new DelayViaObservable<T, U, V>(source, subscriptionDelay, itemDelay);
    }

    /**
     * Delay the emission of the source items by a per-item observable that fires its first element.
     */
    private static final class DelayViaObservable<T, U, V> implements OnSubscribeFunc<T> {
        final Observable<? extends T> source;
        final Func0<? extends Observable<U>> subscriptionDelay;
        final Func1<? super T, ? extends Observable<V>> itemDelay;

        public DelayViaObservable(Observable<? extends T> source,
                Func0<? extends Observable<U>> subscriptionDelay,
                Func1<? super T, ? extends Observable<V>> itemDelay) {
            this.source = source;
            this.subscriptionDelay = subscriptionDelay;
            this.itemDelay = itemDelay;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            CompositeSubscription csub = new CompositeSubscription();

            SerialSubscription sosub = new SerialSubscription();
            csub.add(sosub);
            SourceObserver<T, V> so = new SourceObserver<T, V>(t1, itemDelay, csub, sosub);
            if (subscriptionDelay == null) {
                sosub.set(source.subscribe(so));
            } else {
                Observable<U> subscriptionSource;
                try {
                    subscriptionSource = subscriptionDelay.call();
                } catch (Throwable t) {
                    t1.onError(t);
                    return Subscriptions.empty();
                }
                SerialSubscription ssub = new SerialSubscription();
                csub.add(ssub);
                ssub.set(subscriptionSource.subscribe(new SubscribeDelay<T, U, V>(source, so, csub, ssub)));
            }

            return csub;
        }

        /** Subscribe delay observer. */
        private static final class SubscribeDelay<T, U, V> implements Observer<U> {
            final Observable<? extends T> source;
            final SourceObserver<T, V> so;
            final CompositeSubscription csub;
            final Subscription self;
            /** Prevent any onError once the first item was delivered. */
            boolean subscribed;

            public SubscribeDelay(
                    Observable<? extends T> source,
                    SourceObserver<T, V> so,
                    CompositeSubscription csub, Subscription self) {
                this.source = source;
                this.so = so;
                this.csub = csub;
                this.self = self;
            }

            @Override
            public void onNext(U args) {
                onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                if (!subscribed) {
                    so.observer.onError(e);
                    csub.unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                subscribed = true;
                csub.remove(self);
                so.self.set(source.subscribe(so));
            }
        }

        /** The source observer. */
        private static final class SourceObserver<T, U> implements Observer<T> {
            final Observer<? super T> observer;
            final Func1<? super T, ? extends Observable<U>> itemDelay;
            final CompositeSubscription csub;
            final SerialSubscription self;
            /** Guard to avoid overlapping events from the various sources. */
            final Object guard;
            boolean done;
            int wip;

            public SourceObserver(Observer<? super T> observer,
                    Func1<? super T, ? extends Observable<U>> itemDelay,
                    CompositeSubscription csub,
                    SerialSubscription self) {
                this.observer = observer;
                this.itemDelay = itemDelay;
                this.csub = csub;
                this.guard = new Object();
                this.self = self;
            }

            @Override
            public void onNext(T args) {
                Observable<U> delayer;
                try {
                    delayer = itemDelay.call(args);
                } catch (Throwable t) {
                    onError(t);
                    return;
                }

                synchronized (guard) {
                    wip++;
                }

                SerialSubscription ssub = new SerialSubscription();
                csub.add(ssub);
                ssub.set(delayer.subscribe(new DelayObserver<T, U>(args, this, ssub)));
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    observer.onError(e);
                }
                csub.unsubscribe();
            }

            @Override
            public void onCompleted() {
                boolean b;
                synchronized (guard) {
                    done = true;
                    b = checkDone();
                }
                if (b) {
                    csub.unsubscribe();
                } else {
                    self.unsubscribe();
                }
            }

            void emit(T value, Subscription token) {
                boolean b;
                synchronized (guard) {
                    observer.onNext(value);
                    wip--;
                    b = checkDone();
                }
                if (b) {
                    csub.unsubscribe();
                } else {
                    csub.remove(token);
                }
            }

            boolean checkDone() {
                if (done && wip == 0) {
                    observer.onCompleted();
                    return true;
                }
                return false;
            }
        }

        /**
         * Delay observer.
         */
        private static final class DelayObserver<T, U> implements Observer<U> {
            final T value;
            final SourceObserver<T, U> parent;
            final Subscription token;

            public DelayObserver(T value, SourceObserver<T, U> parent, Subscription token) {
                this.value = value;
                this.parent = parent;
                this.token = token;
            }

            @Override
            public void onNext(U args) {
                parent.emit(value, token);
            }

            @Override
            public void onError(Throwable e) {
                parent.onError(e);
            }

            @Override
            public void onCompleted() {
                parent.emit(value, token);
            }

        }
    }
}
