/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.single;

import java.util.*;
import java.util.concurrent.Callable;

import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * Helper utility class to support Single with inner classes.
 */
public final class SingleInternalHelper {

    /** Utility class. */
    private SingleInternalHelper() {
        throw new IllegalStateException("No instances!");
    }

    enum NoSuchElementCallable implements Callable<NoSuchElementException> {
        INSTANCE;

        @Override
        public NoSuchElementException call() throws Exception {
            return new NoSuchElementException();
        }
    }

    public static <T> Callable<NoSuchElementException> emptyThrower() {
        return NoSuchElementCallable.INSTANCE;
    }

    @SuppressWarnings("rawtypes")
    enum ToFlowable implements Function<SingleSource, Publisher> {
        INSTANCE;
        @SuppressWarnings("unchecked")
        @Override
        public Publisher apply(SingleSource v) {
            return new SingleToFlowable(v);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Function<SingleSource<? extends T>, Publisher<? extends T>> toFlowable() {
        return (Function)ToFlowable.INSTANCE;
    }

    static final class ToFlowableIterator<T> implements Iterator<Flowable<T>> {
        private final Iterator<? extends SingleSource<? extends T>> sit;

        ToFlowableIterator(Iterator<? extends SingleSource<? extends T>> sit) {
            this.sit = sit;
        }

        @Override
        public boolean hasNext() {
            return sit.hasNext();
        }

        @Override
        public Flowable<T> next() {
            return new SingleToFlowable<T>(sit.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    static final class ToFlowableIterable<T> implements Iterable<Flowable<T>> {

        private final Iterable<? extends SingleSource<? extends T>> sources;

        ToFlowableIterable(Iterable<? extends SingleSource<? extends T>> sources) {
            this.sources = sources;
        }

        @Override
        public Iterator<Flowable<T>> iterator() {
            return new ToFlowableIterator<T>(sources.iterator());
        }
    }

    public static <T> Iterable<? extends Flowable<T>> iterableToFlowable(final Iterable<? extends SingleSource<? extends T>> sources) {
        return new ToFlowableIterable<T>(sources);
    }

    @SuppressWarnings("rawtypes")
    enum ToObservable implements Function<SingleSource, Observable> {
        INSTANCE;
        @SuppressWarnings("unchecked")
        @Override
        public Observable apply(SingleSource v) {
            return new SingleToObservable(v);
        }
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Function<SingleSource<? extends T>, Observable<? extends T>> toObservable() {
        return (Function)ToObservable.INSTANCE;
    }
}
