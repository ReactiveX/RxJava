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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.SingleAssignmentSubscription;
import rx.util.Exceptions;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Observable to iterable mapping by using custom collector, merger and
 * collector-replacer functions.
 */
public final class OperationCollect {
    /** Utility class. */
    private OperationCollect() { throw new IllegalStateException("No instances!"); }
    
    /**
     * Produces an Iterable sequence that returns elements 
     * collected/aggregated from the source sequence between consecutive next calls.
     * @param <T> the source value type
     * @param <U> the aggregation type
     * @param source the source Observable
     * @param initialCollector the factory to create the initial collector
     * @param merger the merger that combines the current collector with the observed value and returns a (new) collector
     * @param replaceCollector the function that replaces the current collector with a new collector.
     * @return the iterable sequence
     */
    public static <T, U> Iterable<U> collect(
            final Observable<? extends T> source,
            final Func0<? extends U> initialCollector,
            final Func2<? super U, ? super T, ? extends U> merger,
            final Func1<? super U, ? extends U> replaceCollector) {
        return new Iterable<U>() {
            @Override
            public Iterator<U> iterator() {
                SingleAssignmentSubscription sas = new SingleAssignmentSubscription();
                Collect<T, U> collect = new Collect<T, U>(initialCollector, merger, replaceCollector, sas);
                if (!sas.isUnsubscribed()) {
                    sas.set(source.subscribe(collect));
                }
                return collect;
            }
        };
    }
    
    /**
     * Produces an Iterable sequence of consecutive (possibly empty) chunks of the source sequence.
     * @param <T> the source value type
     * @param source the source Observable
     * @return an iterable sequence of the chunks
     */
    public static <T> Iterable<List<T>> chunkify(final Observable<? extends T> source) {
        ListManager<T> na = new ListManager<T>();
        return collect(source, na, na, na);
    }
    /** Creates a new ArrayList and manages its content. */
    private static final class ListManager<T> implements Func1<List<T>, List<T>>, Func0<List<T>>, Func2<List<T>, T, List<T>> {
        @Override
        public List<T> call() {
            return new ArrayList<T>();
        }

        @Override
        public List<T> call(List<T> t1) {
            return call();
        }
        @Override
        public List<T> call(List<T> t1, T t2) {
            t1.add(t2);
            return t1;
        }
    }
    
    /** The observer and iterator. */
    private static final class Collect<T, U> implements Observer<T>, Iterator<U> {
        final Func2<? super U, ? super T, ? extends U> merger;
        final Func1<? super U, ? extends U> replaceCollector;
        final Subscription cancel;
        final Lock lock = new ReentrantLock();
        U current;
        boolean hasDone;
        boolean hasError;
        Throwable error;
        /** Iterator's current collector. */
        U iCurrent;
        /** Iterator has unclaimed collector. */
        boolean iHasValue;
        /** Iterator completed. */
        boolean iDone;
        /** Iterator error. */
        Throwable iError;
        
        public Collect(final Func0<? extends U> initialCollector,
                final Func2<? super U, ? super T, ? extends U> merger,
                final Func1<? super U, ? extends U> replaceCollector,
                final Subscription cancel) {
            this.merger = merger;
            this.replaceCollector = replaceCollector;
            this.cancel = cancel;
            try {
                current = initialCollector.call();
            } catch (Throwable t) {
                hasError = true;
                error = t;
                cancel.unsubscribe();
            }
        }
        
        @Override
        public void onNext(T args) {
            boolean unsubscribe = false;
            lock.lock();
            try {
                if (hasDone || hasError) {
                    return;
                }
                try {
                    current = merger.call(current, args);
                } catch (Throwable t) {
                    error = t;
                    hasError = true;
                    unsubscribe = true;
                }
            } finally {
                lock.unlock();
            }
            if (unsubscribe) {
                cancel.unsubscribe();
            }
        }

        @Override
        public void onCompleted() {
            boolean unsubscribe = false;
            lock.lock();
            try {
                if (hasDone || hasError) {
                    return;
                }
                hasDone = true;
                unsubscribe = true;
            } finally {
                lock.unlock();
            }
            if (unsubscribe) {
                cancel.unsubscribe();
            }
        }

        @Override
        public void onError(Throwable e) {
            boolean unsubscribe = false;
            lock.lock();
            try {
                if (hasDone || hasError) {
                    return;
                }
                hasError = true;
                error = e;
                unsubscribe = true;
            } finally {
                lock.unlock();
            }
            if (unsubscribe) {
                cancel.unsubscribe();
            }
        }

        @Override
        public boolean hasNext() {
            if (iError != null) {
                throw Exceptions.propagate(iError);
            }
            if (!iHasValue) {
                if (!iDone) {
                    lock.lock();
                    try {
                        if (hasError) {
                            iError = error;
                            iDone = true;
                            current = null;
                            iCurrent = null;
                        } else {
                            iCurrent = current;
                            iHasValue = true;
                            if (hasDone) {
                                current = null;
                                iDone = true;
                            } else {
                                try {
                                    current = replaceCollector.call(iCurrent);
                                } catch (Throwable t) {
                                    iError = t;
                                    iDone = true;
                                }
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                    if (iDone && iError != null) {
                        cancel.unsubscribe();
                        throw Exceptions.propagate(iError);
                    }
                    return true;
                }
                return false;
            }
            return true;
        }

        @Override
        public U next() {
            if (hasNext()) {
                U value = iCurrent;
                iCurrent = null;
                iHasValue = false;
                return value;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read-only sequence");
        }
    }
}
