/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Optional;
import io.reactivex.internal.subscribers.observable.NbpDisposableSubscriber;
import io.reactivex.internal.util.Exceptions;

/**
 * Wait for and iterate over the latest values of the source observable. If the source works faster than the
 * iterator, values may be skipped, but not the {@code onError} or {@code onCompleted} events.
 */
public enum NbpBlockingOperatorLatest {
    ;

    /**
     * Returns an {@code Iterable} that blocks until or unless the {@code Observable} emits an item that has not
     * been returned by the {@code Iterable}, then returns that item
     *
     * @param <T> the value type
     * @param source
     *            the source {@code Observable}
     * @return an {@code Iterable} that blocks until or unless the {@code Observable} emits an item that has not
     *         been returned by the {@code Iterable}, then returns that item
     */
    public static <T> Iterable<T> latest(final Observable<? extends T> source) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                NbpLatestObserverIterator<T> lio = new NbpLatestObserverIterator<T>();
                
                @SuppressWarnings("unchecked")
                Observable<Try<Optional<T>>> materialized = ((Observable<T>)source).materialize();
                
                materialized.subscribe(lio);
                return lio;
            }
        };
    }

    /** Observer of source, iterator for output. */
    static final class NbpLatestObserverIterator<T> extends NbpDisposableSubscriber<Try<Optional<T>>> implements Iterator<T> {
        final Semaphore notify = new Semaphore(0);
        // observer's notification
        final AtomicReference<Try<Optional<T>>> value = new AtomicReference<Try<Optional<T>>>();

        @Override
        public void onNext(Try<Optional<T>> args) {
            boolean wasntAvailable = value.getAndSet(args) == null;
            if (wasntAvailable) {
                notify.release();
            }
        }

        @Override
        public void onError(Throwable e) {
            // not expected
        }

        @Override
        public void onComplete() {
            // not expected
        }

        // iterator's notification
        Try<Optional<T>> iNotif;

        @Override
        public boolean hasNext() {
            if (iNotif != null && iNotif.hasError()) {
                throw Exceptions.propagate(iNotif.error());
            }
            if (iNotif == null || iNotif.value().isPresent()) {
                if (iNotif == null) {
                    try {
                        notify.acquire();
                    } catch (InterruptedException ex) {
                        dispose();
                        Thread.currentThread().interrupt();
                        iNotif = Notification.error(ex);
                        throw Exceptions.propagate(ex);
                    }

                    Try<Optional<T>> n = value.getAndSet(null);
                    iNotif = n;
                    if (iNotif.hasError()) {
                        throw Exceptions.propagate(iNotif.error());
                    }
                }
            }
            return iNotif.value().isPresent();
        }

        @Override
        public T next() {
            if (hasNext()) {
                if (iNotif.value().isPresent()) {
                    T v = iNotif.value().get();
                    iNotif = null;
                    return v;
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read-only iterator.");
        }

    }
}