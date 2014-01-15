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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.Func5;
import rx.util.functions.Func6;
import rx.util.functions.Func7;
import rx.util.functions.Func8;
import rx.util.functions.Func9;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

/**
 * Returns an Observable that emits the results of a function applied to sets of items emitted, in
 * sequence, by two or more other Observables.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/zip.png">
 * <p>
 * The zip operation applies this function in strict sequence, so the first item emitted by the new
 * Observable will be the result of the function applied to the first item emitted by each zipped
 * Observable; the second item emitted by the new Observable will be the result of the function
 * applied to the second item emitted by each zipped Observable; and so forth.
 * <p>
 * The resulting Observable returned from zip will invoke <code>onNext</code> as many times as the
 * number of <code>onNext</code> invocations of the source Observable that emits the fewest items.
 */
public final class OperationZip {

    @SuppressWarnings("unchecked")
    public static <T1, T2, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, final Func2<? super T1, ? super T2, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2), Functions.fromFunc(zipFunction));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, final Func3<? super T1, ? super T2, ? super T3, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3), Functions.fromFunc(zipFunction));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, final Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4), Functions.fromFunc(zipFunction));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, final Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5), Functions.fromFunc(zipFunction));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6,
            final Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5, o6), Functions.fromFunc(zipFunction));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7,
            final Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5, o6, o7), Functions.fromFunc(zipFunction));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7, Observable<? extends T8> o8,
            final Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5, o6, o7, o8), Functions.fromFunc(zipFunction));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7, Observable<? extends T8> o8,
            Observable<? extends T9> o9, final Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5, o6, o7, o8, o9), Functions.fromFunc(zipFunction));
    }

    public static <R> OnSubscribeFunc<R> zip(Iterable<? extends Observable<?>> ws, final FuncN<? extends R> zipFunction) {
        ManyObservables<?, R> a = new ManyObservables<Object, R>(ws, zipFunction);
        return a;
    }

    /**
     * Merges the values across multiple sources and applies the selector
     * function.
     * <p>The resulting sequence terminates if no more pairs can be
     * established, i.e., streams of length 1 and 2 zipped will produce
     * only 1 item.</p>
     * <p>Exception semantics: errors from the source observable are
     * propagated as-is.</p>
     * 
     * @param <T>
     *            the common element type
     * @param <U>
     *            the result element type
     */
    private static final class ManyObservables<T, U> implements OnSubscribeFunc<U> {
        /** */
        protected final Iterable<? extends Observable<? extends T>> sources;
        /** */
        protected final FuncN<? extends U> selector;

        /**
         * Constructor.
         * 
         * @param sources
         *            the sources
         * @param selector
         *            the result selector
         */
        public ManyObservables(
                Iterable<? extends Observable<? extends T>> sources,
                FuncN<? extends U> selector) {
            this.sources = sources;
            this.selector = selector;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super U> observer) {

            final CompositeSubscription composite = new CompositeSubscription();

            final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);

            final List<ItemObserver<T>> all = new ArrayList<ItemObserver<T>>();

            Observer<List<T>> o2 = new Observer<List<T>>() {
                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable t) {
                    observer.onError(t);
                }

                @Override
                public void onNext(List<T> value) {
                    observer.onNext(selector.call(value.toArray(new Object[value.size()])));
                }
            };

            for (Observable<? extends T> o : sources) {

                ItemObserver<T> io = new ItemObserver<T>(
                        rwLock, all, o, o2, composite);
                composite.add(io);
                all.add(io);
            }

            for (ItemObserver<T> io : all) {
                io.connect();
            }

            return composite;
        }

        /**
         * The individual line's observer.
         * 
         * @author akarnokd, 2013.01.14.
         * @param <T>
         *            the element type
         */
        private static final class ItemObserver<T> implements Observer<T>, Subscription {
            /** Reader-writer lock. */
            protected final ReadWriteLock rwLock;
            /** The queue. */
            public final Queue<Object> queue = new LinkedList<Object>();
            /** The list of the other observers. */
            public final List<ItemObserver<T>> all;
            /** The null sentinel value. */
            protected static final Object NULL_SENTINEL = new Object();
            /** The global cancel. */
            protected final Subscription cancel;
            /** The subscription to the source. */
            protected final SerialSubscription toSource = new SerialSubscription();
            /** Indicate completion of this stream. */
            protected boolean done;
            /** The source. */
            protected final Observable<? extends T> source;
            /** The observer. */
            protected final Observer<? super List<T>> observer;

            /**
             * Constructor.
             * 
             * @param rwLock
             *            the reader-writer lock to use
             * @param all
             *            all observers
             * @param source
             *            the source sequence
             * @param observer
             *            the output observer
             * @param cancel
             *            the cancellation handler
             */
            public ItemObserver(
                    ReadWriteLock rwLock,
                    List<ItemObserver<T>> all,
                    Observable<? extends T> source,
                    Observer<? super List<T>> observer,
                    Subscription cancel) {
                this.rwLock = rwLock;
                this.all = all;
                this.source = source;
                this.observer = observer;
                this.cancel = cancel;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onNext(T value) {
                rwLock.readLock().lock();
                try {
                    if (done) {
                        return;
                    }
                    queue.add(value != null ? value : NULL_SENTINEL);
                } finally {
                    rwLock.readLock().unlock();
                }
                // run collector
                if (rwLock.writeLock().tryLock()) {
                    try {
                        while (true) {
                            List<T> values = new ArrayList<T>(all.size());
                            for (ItemObserver<T> io : all) {
                                if (io.queue.isEmpty()) {
                                    if (io.done) {
                                        observer.onCompleted();
                                        cancel.unsubscribe();
                                        return;
                                    }
                                    continue;
                                }
                                Object v = io.queue.peek();
                                if (v == NULL_SENTINEL) {
                                    v = null;
                                }
                                values.add((T) v);
                            }
                            if (values.size() == all.size()) {
                                for (ItemObserver<T> io : all) {
                                    io.queue.poll();
                                }
                                observer.onNext(values);
                            } else {
                                break;
                            }
                        }
                    } finally {
                        rwLock.writeLock().unlock();
                    }
                }
            }

            @Override
            public void onError(Throwable ex) {
                boolean c = false;
                rwLock.writeLock().lock();
                try {
                    if (done) {
                        return;
                    }
                    done = true;
                    c = true;
                    observer.onError(ex);
                    cancel.unsubscribe();
                } finally {
                    rwLock.writeLock().unlock();
                }
                if (c) {
                    unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                boolean c = false;
                rwLock.readLock().lock();
                try {
                    done = true;
                    c = true;
                } finally {
                    rwLock.readLock().unlock();
                }
                if (rwLock.writeLock().tryLock()) {
                    try {
                        for (ItemObserver<T> io : all) {
                            if (io.queue.isEmpty() && io.done) {
                                observer.onCompleted();
                                cancel.unsubscribe();
                                return;
                            }
                        }
                    } finally {
                        rwLock.writeLock().unlock();
                    }
                }
                if (c) {
                    unsubscribe();
                }
            }

            /** Connect to the source observable. */
            public void connect() {
                toSource.setSubscription(source.subscribe(this));
            }

            @Override
            public void unsubscribe() {
                toSource.unsubscribe();
            }

        }
    }

    /**
     * Zips an Observable and an iterable sequence and applies
     * a function to the pair of values.
     */
    public static <T, U, R> OnSubscribeFunc<R> zipIterable(Observable<? extends T> source, Iterable<? extends U> other, Func2<? super T, ? super U, ? extends R> zipFunction) {
        return new ZipIterable<T, U, R>(source, other, zipFunction);
    }

    /**
     * Zips an Observable and an iterable sequence and applies
     * a function to the pair of values.
     */
    private static final class ZipIterable<T, U, R> implements OnSubscribeFunc<R> {
        final Observable<? extends T> source;
        final Iterable<? extends U> other;
        final Func2<? super T, ? super U, ? extends R> zipFunction;

        public ZipIterable(Observable<? extends T> source, Iterable<? extends U> other, Func2<? super T, ? super U, ? extends R> zipFunction) {
            this.source = source;
            this.other = other;
            this.zipFunction = zipFunction;
        }

        @Override
        public Subscription onSubscribe(Observer<? super R> t1) {

            Iterator<? extends U> it;
            boolean first;
            try {
                it = other.iterator();
                first = it.hasNext();
            } catch (Throwable t) {
                t1.onError(t);
                return Subscriptions.empty();
            }

            if (!first) {
                t1.onCompleted();
                return Subscriptions.empty();
            }

            SerialSubscription ssub = new SerialSubscription();

            ssub.set(source.subscribe(new SourceObserver<T, U, R>(t1, it, zipFunction, ssub)));

            return ssub;
        }

        /** Observe the source. */
        private static final class SourceObserver<T, U, R> implements Observer<T> {
            final Observer<? super R> observer;
            final Iterator<? extends U> other;
            final Func2<? super T, ? super U, ? extends R> zipFunction;
            final Subscription cancel;

            public SourceObserver(Observer<? super R> observer, Iterator<? extends U> other,
                    Func2<? super T, ? super U, ? extends R> zipFunction, Subscription cancel) {
                this.observer = observer;
                this.other = other;
                this.zipFunction = zipFunction;
                this.cancel = cancel;
            }

            @Override
            public void onNext(T args) {
                U u = other.next();

                R r;
                try {
                    r = zipFunction.call(args, u);
                } catch (Throwable t) {
                    onError(t);
                    return;
                }

                observer.onNext(r);

                boolean has;
                try {
                    has = other.hasNext();
                } catch (Throwable t) {
                    onError(t);
                    return;
                }

                if (!has) {
                    onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
                cancel.unsubscribe();
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
                cancel.unsubscribe();
            }

        }
    }
}
