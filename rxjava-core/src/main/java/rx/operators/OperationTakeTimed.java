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
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Returns an Observable that emits the first <code>num</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/take.png">
 * <p>
 * You can choose to pay attention only to the first <code>num</code> items emitted by an
 * Observable by using the take operation. This operation returns an Observable that will invoke a
 * subscribing Observer's <code>onNext</code> function a maximum of <code>num</code> times before
 * invoking <code>onCompleted</code>.
 */
public final class OperationTakeTimed {

    //TODO this has not been migrated to use bind yet

    /**
     * Returns a specified number of contiguous values from the start of an observable sequence.
     * 
     * @param items
     * @param num
     * @return the specified number of contiguous values from the start of the given observable sequence
     */
    public static <T> OnSubscribeFunc<T> take(final Observable<? extends T> items, final int num) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new Take<T>(items, num).onSubscribe(observer);
            }

        };
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * <p>
     * This should all be fine as long as it's kept as a private class and a new instance created from static factory method above.
     * <p>
     * Note how the take() factory method above protects us from a single instance being exposed with the Observable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static class Take<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> items;
        private final int num;
        private final SafeObservableSubscription subscription = new SafeObservableSubscription();

        private Take(Observable<? extends T> items, int num) {
            this.items = items;
            this.num = num;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            if (num < 1) {
                items.subscribe(new Observer<T>()
                {
                    @Override
                    public void onCompleted()
                    {
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                    }

                    @Override
                    public void onNext(T args)
                    {
                    }
                }).unsubscribe();
                observer.onCompleted();
                return Subscriptions.empty();
            }

            return subscription.wrap(items.subscribe(new ItemObserver(observer)));
        }

        private class ItemObserver implements Observer<T> {
            private final Observer<? super T> observer;

            private final AtomicInteger counter = new AtomicInteger();
            private volatile boolean hasEmitedError = false;

            public ItemObserver(Observer<? super T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                if (hasEmitedError) {
                    return;
                }
                if (counter.getAndSet(num) < num) {
                    observer.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (hasEmitedError) {
                    return;
                }
                if (counter.getAndSet(num) < num) {
                    observer.onError(e);
                }
            }

            @Override
            public void onNext(T args) {
                if (hasEmitedError) {
                    return;
                }
                final int count = counter.incrementAndGet();
                if (count <= num) {
                    try {
                        observer.onNext(args);
                    } catch (Throwable ex) {
                        hasEmitedError = true;
                        observer.onError(ex);
                        subscription.unsubscribe();
                        return;
                    }
                    if (count == num) {
                        observer.onCompleted();
                    }
                }
                if (count >= num) {
                    // this will work if the sequence is asynchronous, it will have no effect on a synchronous observable
                    subscription.unsubscribe();
                }
            }

        }

    }

    /**
     * Takes values from the source until a timer fires.
     * 
     * @param <T>
     *            the result value type
     */
    public static final class TakeTimed<T> implements OnSubscribeFunc<T> {
        final Observable<? extends T> source;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;

        public TakeTimed(Observable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
            this.source = source;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {

            SafeObservableSubscription timer = new SafeObservableSubscription();
            SafeObservableSubscription data = new SafeObservableSubscription();

            CompositeSubscription csub = new CompositeSubscription(timer, data);

            final SourceObserver<T> so = new SourceObserver<T>(t1, csub);
            data.wrap(source.subscribe(so));
            if (!data.isUnsubscribed()) {
                timer.wrap(scheduler.schedule(so, time, unit));
            }

            return csub;
        }

        /**
         * Observes the source and relays its values until gate turns into false.
         * 
         * @param <T>
         *            the observed value type
         */
        private static final class SourceObserver<T> implements Observer<T>, Action1<Inner> {
            final Observer<? super T> observer;
            final Subscription cancel;
            final AtomicInteger state = new AtomicInteger();
            static final int ACTIVE = 0;
            static final int NEXT = 1;
            static final int DONE = 2;

            public SourceObserver(Observer<? super T> observer,
                    Subscription cancel) {
                this.observer = observer;
                this.cancel = cancel;
            }

            @Override
            public void onNext(T args) {
                do {
                    int s = state.get();
                    if (s == DONE) {
                        return;
                    }
                    if (state.compareAndSet(s, NEXT)) {
                        try {
                            observer.onNext(args);
                        } finally {
                            state.set(ACTIVE);
                            return;
                        }
                    }
                } while (true);
            }

            @Override
            public void onError(Throwable e) {
                do {
                    int s = state.get();
                    if (s == DONE) {
                        return;
                    } else if (s == NEXT) {
                        continue;
                    } else if (state.compareAndSet(s, DONE)) {
                        try {
                            observer.onError(e);
                        } finally {
                            cancel.unsubscribe();
                        }
                        return;
                    }
                } while (true);
            }

            @Override
            public void onCompleted() {
                do {
                    int s = state.get();
                    if (s == DONE) {
                        return;
                    } else if (s == NEXT) {
                        continue;
                    } else if (state.compareAndSet(s, DONE)) {
                        try {
                            observer.onCompleted();
                        } finally {
                            cancel.unsubscribe();
                        }
                        return;
                    }
                } while (true);
            }

            @Override
            public void call(Inner inner) {
                onCompleted();
            }

        }
    }
}
