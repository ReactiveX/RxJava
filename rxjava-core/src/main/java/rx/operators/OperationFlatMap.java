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

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;

/**
 * Additional flatMap operators.
 */
public final class OperationFlatMap {
    /** Utility class. */
    private OperationFlatMap() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Observable that pairs up the source values and all the derived collection
     * values and projects them via the selector.
     */
    public static <T, U, R> OnSubscribeFunc<R> flatMap(Observable<? extends T> source,
            Func1<? super T, ? extends Observable<? extends U>> collectionSelector,
            Func2<? super T, ? super U, ? extends R> resultSelector
            ) {
        return new FlatMapPairSelector<T, U, R>(source, collectionSelector, resultSelector);
    }

    /**
     * Converts the result Iterable of a function into an Observable.
     */
    public static <T, U> Func1<T, Observable<U>> flatMapIterableFunc(
            Func1<? super T, ? extends Iterable<? extends U>> collectionSelector) {
        return new IterableToObservableFunc<T, U>(collectionSelector);
    }

    /**
     * Converts the result Iterable of a function into an Observable.
     * 
     * @param <T>
     *            the parameter type
     * @param <R>
     *            the result type
     */
    private static final class IterableToObservableFunc<T, R> implements Func1<T, Observable<R>> {
        final Func1<? super T, ? extends Iterable<? extends R>> func;

        public IterableToObservableFunc(Func1<? super T, ? extends Iterable<? extends R>> func) {
            this.func = func;
        }

        @Override
        public Observable<R> call(T t1) {
            return Observable.from(func.call(t1));
        }
    }

    /**
     * Pairs up the source value with each of the associated observable values
     * and uses a selector function to calculate the result sequence.
     * 
     * @param <T>
     *            the source value type
     * @param <U>
     *            the collection value type
     * @param <R>
     *            the result type
     */
    private static final class FlatMapPairSelector<T, U, R> implements OnSubscribeFunc<R> {
        final Observable<? extends T> source;
        final Func1<? super T, ? extends Observable<? extends U>> collectionSelector;
        final Func2<? super T, ? super U, ? extends R> resultSelector;

        public FlatMapPairSelector(Observable<? extends T> source, Func1<? super T, ? extends Observable<? extends U>> collectionSelector, Func2<? super T, ? super U, ? extends R> resultSelector) {
            this.source = source;
            this.collectionSelector = collectionSelector;
            this.resultSelector = resultSelector;
        }

        @Override
        public Subscription onSubscribe(Observer<? super R> t1) {
            CompositeSubscription csub = new CompositeSubscription();

            csub.add(source.subscribe(new SourceObserver<T, U, R>(t1, collectionSelector, resultSelector, csub)));

            return csub;
        }

        /** Observes the source, starts the collections and projects the result. */
        private static final class SourceObserver<T, U, R> implements Observer<T> {
            final Observer<? super R> observer;
            final Func1<? super T, ? extends Observable<? extends U>> collectionSelector;
            final Func2<? super T, ? super U, ? extends R> resultSelector;
            final CompositeSubscription csub;
            final AtomicInteger wip;
            /** Don't let various events run at the same time. */
            final Object guard;
            boolean done;

            public SourceObserver(Observer<? super R> observer, Func1<? super T, ? extends Observable<? extends U>> collectionSelector, Func2<? super T, ? super U, ? extends R> resultSelector, CompositeSubscription csub) {
                this.observer = observer;
                this.collectionSelector = collectionSelector;
                this.resultSelector = resultSelector;
                this.csub = csub;
                this.wip = new AtomicInteger(1);
                this.guard = new Object();
            }

            @Override
            public void onNext(T args) {
                Observable<? extends U> coll;
                try {
                    coll = collectionSelector.call(args);
                } catch (Throwable e) {
                    onError(e);
                    return;
                }

                SerialSubscription ssub = new SerialSubscription();
                csub.add(ssub);
                wip.incrementAndGet();

                ssub.set(coll.subscribe(new CollectionObserver<T, U, R>(this, args, ssub)));
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    if (done) {
                        return;
                    }
                    done = true;
                    observer.onError(e);
                }
                csub.unsubscribe();
            }

            @Override
            public void onCompleted() {
                if (wip.decrementAndGet() == 0) {
                    synchronized (guard) {
                        if (done) {
                            return;
                        }
                        done = true;
                        observer.onCompleted();
                    }
                    csub.unsubscribe();
                }
            }

            void complete(Subscription s) {
                csub.remove(s);
                onCompleted();
            }

            void emit(T t, U u) {
                R r;
                try {
                    r = resultSelector.call(t, u);
                } catch (Throwable e) {
                    onError(e);
                    return;
                }
                synchronized (guard) {
                    if (done) {
                        return;
                    }
                    observer.onNext(r);
                }
            }
        }

        /** Observe a collection and call emit with the pair of the key and the value. */
        private static final class CollectionObserver<T, U, R> implements Observer<U> {
            final SourceObserver<T, U, R> so;
            final Subscription cancel;
            final T value;

            public CollectionObserver(SourceObserver<T, U, R> so, T value, Subscription cancel) {
                this.so = so;
                this.value = value;
                this.cancel = cancel;
            }

            @Override
            public void onNext(U args) {
                so.emit(value, args);
            }

            @Override
            public void onError(Throwable e) {
                so.onError(e);
            }

            @Override
            public void onCompleted() {
                so.complete(cancel);
            }
        };
    }

    /**
     * Projects the notification of an observable sequence to an observable
     * sequence and merges the results into one.
     */
    public static <T, R> OnSubscribeFunc<R> flatMap(Observable<? extends T> source,
            Func1<? super T, ? extends Observable<? extends R>> onNext,
            Func1<? super Throwable, ? extends Observable<? extends R>> onError,
            Func0<? extends Observable<? extends R>> onCompleted) {
        return new FlatMapTransform<T, R>(source, onNext, onError, onCompleted);
    }

    /**
     * Projects the notification of an observable sequence to an observable
     * sequence and merges the results into one.
     * 
     * @param <T>
     *            the source value type
     * @param <R>
     *            the result value type
     */
    private static final class FlatMapTransform<T, R> implements OnSubscribeFunc<R> {
        final Observable<? extends T> source;
        final Func1<? super T, ? extends Observable<? extends R>> onNext;
        final Func1<? super Throwable, ? extends Observable<? extends R>> onError;
        final Func0<? extends Observable<? extends R>> onCompleted;

        public FlatMapTransform(Observable<? extends T> source, Func1<? super T, ? extends Observable<? extends R>> onNext, Func1<? super Throwable, ? extends Observable<? extends R>> onError, Func0<? extends Observable<? extends R>> onCompleted) {
            this.source = source;
            this.onNext = onNext;
            this.onError = onError;
            this.onCompleted = onCompleted;
        }

        @Override
        public Subscription onSubscribe(Observer<? super R> t1) {
            CompositeSubscription csub = new CompositeSubscription();

            csub.add(source.subscribe(new SourceObserver<T, R>(t1, onNext, onError, onCompleted, csub)));

            return csub;
        }

        /**
         * Observe the source and merge the values.
         * 
         * @param <T>
         *            the source value type
         * @param <R>
         *            the result value type
         */
        private static final class SourceObserver<T, R> implements Observer<T> {
            final Observer<? super R> observer;
            final Func1<? super T, ? extends Observable<? extends R>> onNext;
            final Func1<? super Throwable, ? extends Observable<? extends R>> onError;
            final Func0<? extends Observable<? extends R>> onCompleted;
            final CompositeSubscription csub;
            final AtomicInteger wip;
            volatile boolean done;
            final Object guard;

            public SourceObserver(Observer<? super R> observer, Func1<? super T, ? extends Observable<? extends R>> onNext, Func1<? super Throwable, ? extends Observable<? extends R>> onError, Func0<? extends Observable<? extends R>> onCompleted, CompositeSubscription csub) {
                this.observer = observer;
                this.onNext = onNext;
                this.onError = onError;
                this.onCompleted = onCompleted;
                this.csub = csub;
                this.guard = new Object();
                this.wip = new AtomicInteger(1);
            }

            @Override
            public void onNext(T args) {
                Observable<? extends R> o;
                try {
                    o = onNext.call(args);
                } catch (Throwable t) {
                    synchronized (guard) {
                        observer.onError(t);
                    }
                    csub.unsubscribe();
                    return;
                }
                subscribeInner(o);
            }

            @Override
            public void onError(Throwable e) {
                Observable<? extends R> o;
                try {
                    o = onError.call(e);
                } catch (Throwable t) {
                    synchronized (guard) {
                        observer.onError(t);
                    }
                    csub.unsubscribe();
                    return;
                }
                subscribeInner(o);
                done = true;
                finish();
            }

            @Override
            public void onCompleted() {
                Observable<? extends R> o;
                try {
                    o = onCompleted.call();
                } catch (Throwable t) {
                    synchronized (guard) {
                        observer.onError(t);
                    }
                    csub.unsubscribe();
                    return;
                }
                subscribeInner(o);
                done = true;
                finish();
            }

            void subscribeInner(Observable<? extends R> o) {
                SerialSubscription ssub = new SerialSubscription();
                wip.incrementAndGet();
                csub.add(ssub);

                ssub.set(o.subscribe(new CollectionObserver<T, R>(this, ssub)));
            }

            void finish() {
                if (wip.decrementAndGet() == 0) {
                    synchronized (guard) {
                        observer.onCompleted();
                    }
                    csub.unsubscribe();
                }
            }
        }

        /** Observes the collections. */
        private static final class CollectionObserver<T, R> implements Observer<R> {
            final SourceObserver<T, R> parent;
            final Subscription cancel;

            public CollectionObserver(SourceObserver<T, R> parent, Subscription cancel) {
                this.parent = parent;
                this.cancel = cancel;
            }

            @Override
            public void onNext(R args) {
                synchronized (parent.guard) {
                    parent.observer.onNext(args);
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (parent.guard) {
                    parent.observer.onError(e);
                }
                parent.csub.unsubscribe();
            }

            @Override
            public void onCompleted() {
                parent.csub.remove(cancel);
                parent.finish();
            }
        }
    }
}
