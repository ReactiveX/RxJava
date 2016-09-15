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

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import rx.*;
import rx.Observable;
import rx.exceptions.Exceptions;

/**
 * Wait for and iterate over the latest values of the source observable. If the source works faster than the
 * iterator, values may be skipped, but not the {@code onError} or {@code onCompleted} events.
 */
public final class BlockingOperatorLatest {
    /** Utility class. */
    private BlockingOperatorLatest() {
        throw new IllegalStateException("No instances!");
    }

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
            @SuppressWarnings("unchecked")
            @Override
            public Iterator<T> iterator() {
                LatestObserverIterator<T> lio = new LatestObserverIterator<T>();
                ((Observable<T>)source).materialize().subscribe(lio);
                return lio;
            }
        };
    }

    /** Observer of source, iterator for output. */
    static final class LatestObserverIterator<T> extends Subscriber<Notification<? extends T>> implements Iterator<T> {
        final Semaphore notify = new Semaphore(0);
        // observer's notification
        final AtomicReference<Notification<? extends T>> value = new AtomicReference<Notification<? extends T>>();
        // iterator's notification
        Notification<? extends T> iteratorNotification;

        @Override
        public void onNext(Notification<? extends T> args) {
            boolean wasNotAvailable = value.getAndSet(args) == null;
            if (wasNotAvailable) {
                notify.release();
            }
        }

        @Override
        public void onError(Throwable e) {
            // not expected
        }

        @Override
        public void onCompleted() {
            // not expected
        }

        @Override
        public boolean hasNext() {
            if (iteratorNotification != null && iteratorNotification.isOnError()) {
                throw Exceptions.propagate(iteratorNotification.getThrowable());
            }
            if (iteratorNotification == null || !iteratorNotification.isOnCompleted()) {
                if (iteratorNotification == null) {
                    try {
                        notify.acquire();
                    } catch (InterruptedException ex) {
                        unsubscribe();
                        Thread.currentThread().interrupt();
                        iteratorNotification = Notification.createOnError(ex);
                        throw Exceptions.propagate(ex);
                    }

                    Notification<? extends T> n = value.getAndSet(null);
                    iteratorNotification = n;
                    if (iteratorNotification.isOnError()) {
                        throw Exceptions.propagate(iteratorNotification.getThrowable());
                    }
                }
            }
            return !iteratorNotification.isOnCompleted();
        }

        @Override
        public T next() {
            if (hasNext()) {
                if (iteratorNotification.isOnNext()) {
                    T v = iteratorNotification.getValue();
                    iteratorNotification = null;
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
