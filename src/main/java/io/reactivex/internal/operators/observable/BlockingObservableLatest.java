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
import io.reactivex.exceptions.Exceptions;
import io.reactivex.observers.DisposableObserver;

/**
 * Wait for and iterate over the latest values of the source observable. If the source works faster than the
 * iterator, values may be skipped, but not the {@code onError} or {@code onCompleted} events.
 */
public enum BlockingObservableLatest {
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
    public static <T> Iterable<T> latest(final ObservableSource<? extends T> source) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                LatestObserverIterator<T> lio = new LatestObserverIterator<T>();
                
                @SuppressWarnings("unchecked")
                Observable<Notification<T>> materialized = Observable.wrap((ObservableSource<T>)source).materialize();
                
                materialized.subscribe(lio);
                return lio;
            }
        };
    }

    /** Observer of source, iterator for output. */
    static final class LatestObserverIterator<T> extends DisposableObserver<Notification<T>> implements Iterator<T> {
        // iterator's notification
        Notification<T> iNotif;

        final Semaphore notify = new Semaphore(0);
        // observer's notification
        final AtomicReference<Notification<T>> value = new AtomicReference<Notification<T>>();

        @Override
        public void onNext(Notification<T> args) {
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

        @Override
        public boolean hasNext() {
            if (iNotif != null && iNotif.isOnError()) {
                throw Exceptions.propagate(iNotif.getError());
            }
            if (iNotif == null || iNotif.isOnNext()) {
                if (iNotif == null) {
                    try {
                        notify.acquire();
                    } catch (InterruptedException ex) {
                        dispose();
                        Thread.currentThread().interrupt();
                        iNotif = Notification.createOnError(ex);
                        throw Exceptions.propagate(ex);
                    }

                    Notification<T> n = value.getAndSet(null);
                    iNotif = n;
                    if (iNotif.isOnError()) {
                        throw Exceptions.propagate(iNotif.getError());
                    }
                }
            }
            return iNotif.isOnNext();
        }

        @Override
        public T next() {
            if (hasNext()) {
                if (iNotif.isOnNext()) {
                    T v = iNotif.getValue();
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
