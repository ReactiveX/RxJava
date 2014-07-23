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
package rx.internal.operators;

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

import static rx.Observable.create;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

public final class OnSubscribeRedo<T> implements OnSubscribe<T> {

    static final Func1<Observable<? extends Notification<?>>, Observable<?>> REDO_INIFINITE = new Func1<Observable<? extends Notification<?>>, Observable<?>>() {
        @Override
        public Observable<?> call(Observable<? extends Notification<?>> ts) {
            return ts.map(new Func1<Notification<?>, Notification<?>>() {
                @Override
                public Notification<?> call(Notification<?> terminal) {
                    return Notification.createOnNext(null);
                }
            });
        }
    };

    public static final class RedoFinite implements Func1<Observable<? extends Notification<?>>, Observable<?>> {
        private final long count;

        public RedoFinite(long count) {
            this.count = count;
        }

        @Override
        public Observable<?> call(Observable<? extends Notification<?>> ts) {
            final Notification<Long> first = count < 0 ? Notification.<Long> createOnCompleted() : Notification.createOnNext(0l);

            return ts.scan(first, new Func2<Notification<Long>, Notification<?>, Notification<Long>>() {
                @SuppressWarnings("unchecked")
                @Override
                public Notification<Long> call(Notification<Long> n, Notification<?> term) {
                    final long value = n.getValue();
                    if (value < count) return Notification.createOnNext(value + 1);
                    else return (Notification<Long>) term;
                }
            }).dematerialize();
        }
    }

    public static final class RetryWithPredicate implements Func1<Observable<? extends Notification<?>>, Observable<? extends Notification<?>>> {
        private Func2<Integer, Throwable, Boolean> predicate;

        public RetryWithPredicate(Func2<Integer, Throwable, Boolean> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Observable<? extends Notification<?>> call(Observable<? extends Notification<?>> ts) {
            return ts.scan(Notification.createOnNext(0), new Func2<Notification<Integer>, Notification<?>, Notification<Integer>>() {
                @SuppressWarnings("unchecked")
                @Override
                public Notification<Integer> call(Notification<Integer> n, Notification<?> term) {
                    final int value = n.getValue();
                    if (predicate.call(value, term.getThrowable()).booleanValue()) return Notification.createOnNext(value + 1);
                    else return (Notification<Integer>) term;
                }
            });
        }
    }

    public static <T> Observable<T> retry(Observable<T> source) {
        return retry(source, REDO_INIFINITE);
    }

    public static <T> Observable<T> retry(Observable<T> source, final long count) {
        if (count < 0) throw new IllegalArgumentException("count >= 0 expected");
        if (count == 0) return source;
        return retry(source, new RedoFinite(count));
    }

    public static <T> Observable<T> retry(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, true, false, Schedulers.trampoline()));
    }

    public static <T> Observable<T> retry(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, true, false, scheduler));
    }

    public static <T> Observable<T> repeat(Observable<T> source) {
        return repeat(source, Schedulers.trampoline());
    }

    public static <T> Observable<T> repeat(Observable<T> source, Scheduler scheduler) {
        return repeat(source, REDO_INIFINITE, scheduler);
    }

    public static <T> Observable<T> repeat(Observable<T> source, final long count) {
        return repeat(source, count, Schedulers.trampoline());
    }

    public static <T> Observable<T> repeat(Observable<T> source, final long count, Scheduler scheduler) {
        if (count < 0) throw new IllegalArgumentException("count >= 0 expected");
        return repeat(source, new RedoFinite(count - 1), scheduler);
    }

    public static <T> Observable<T> repeat(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, true, Schedulers.trampoline()));
    }

    public static <T> Observable<T> repeat(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, true, scheduler));
    }

    public static <T> Observable<T> redo(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, false, scheduler));
    }

    private Observable<T> source;
    private final Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> f;
    private boolean stopOnComplete;
    private boolean stopOnError;
    private final Scheduler scheduler;
    private final AtomicBoolean isLocked = new AtomicBoolean(true);

    private OnSubscribeRedo(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> f, boolean stopOnComplete, boolean stopOnError,
            Scheduler scheduler) {
        this.source = source;
        this.f = f;
        this.stopOnComplete = stopOnComplete;
        this.stopOnError = stopOnError;
        this.scheduler = scheduler;
    }

    @Override
    public void call(final Subscriber<? super T> child) {
        final Scheduler.Worker inner = scheduler.createWorker();
        child.add(inner);

        final CompositeSubscription sourceSubscriptions = new CompositeSubscription();
        child.add(sourceSubscriptions);

        final PublishSubject<Notification<?>> terminals = PublishSubject.create();


        final Action0 subscribeToSource = new Action0() {
            @Override
            public void call() {
                Subscriber<T> terminalDelegatingSubscriber = new Subscriber<T>() {
                    @Override
                    public void onCompleted() {
                        unsubscribe();
                        terminals.onNext(Notification.createOnCompleted());
                    }

                    @Override
                    public void onError(Throwable e) {
                        unsubscribe();
                        terminals.onNext(Notification.createOnError(e));
                    }

                    @Override
                    public void onNext(T v) {
                        child.onNext(v);
                    }
                };
                // new subscription each time so if it unsubscribes itself it does not prevent retries
                // by unsubscribing the child subscription
                sourceSubscriptions.add(terminalDelegatingSubscriber);
                source.unsafeSubscribe(terminalDelegatingSubscriber);
            }
        };

        final Observable<?> restarts = f.call(
        // lifting in a custom operator to kind of do a merge/map/filter thing.
                terminals.lift(new Operator<Notification<?>, Notification<?>>() {
                    @Override
                    public Subscriber<? super Notification<?>> call(final Subscriber<? super Notification<?>> filteredTerminals) {
                        return new Subscriber<Notification<?>>(filteredTerminals) {
                            @Override
                            public void onCompleted() {
                                filteredTerminals.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                filteredTerminals.onError(e);
                            }

                            @Override
                            public void onNext(Notification<?> t) {
                                if (t.isOnCompleted() && stopOnComplete) child.onCompleted();
                                else if (t.isOnError() && stopOnError) child.onError(t.getThrowable());
                                else {
                                    isLocked.set(false);
                                    filteredTerminals.onNext(t);
                                }
                            }
                        };
                    }
                }));

        // subscribe to the restarts observable to know when to schedule the next redo.
        child.add(inner.schedule(new Action0() {
            @Override
            public void call() {
                restarts.unsafeSubscribe(new Subscriber<Object>(child) {
                    @Override
                    public void onCompleted() {
                        child.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Object t) {
                        if (!isLocked.get() && !child.isUnsubscribed()) {
                            // if (!child.isUnsubscribed()) {
                            child.add(inner.schedule(subscribeToSource));
                        }
                    }
                });
            }
        }));
        if (!child.isUnsubscribed()) {
            child.add(inner.schedule(subscribeToSource));
        }
    }
}
