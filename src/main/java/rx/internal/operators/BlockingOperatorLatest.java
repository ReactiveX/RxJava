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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
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
     * @param source
     *            the source {@code Observable}
     * @return an {@code Iterable} that blocks until or unless the {@code Observable} emits an item that has not
     *         been returned by the {@code Iterable}, then returns that item
     */
    public static <T> Iterable<T> latest(final Observable<? extends T> source) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                LatestObserverIterator<T> lio = new LatestObserverIterator<T>();
                source.materialize().subscribe(lio);
                return lio;
            }
        };
    }

    /** Observer of source, iterator for output. */
    static final class LatestObserverIterator<T> extends Subscriber<Notification<? extends T>> implements Iterator<T> {
        final Semaphore notify = new Semaphore(0);
        // observer's notification
        volatile Notification<? extends T> value;
        /** Updater for the value field. */
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<LatestObserverIterator, Notification> REFERENCE_UPDATER
                = AtomicReferenceFieldUpdater.newUpdater(LatestObserverIterator.class, Notification.class, "value");

        @Override
        public void onNext(Notification<? extends T> args) {
            boolean wasntAvailable = REFERENCE_UPDATER.getAndSet(this, args) == null;
            if (wasntAvailable) {
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

        // iterator's notification
        Notification<? extends T> iNotif;

        @Override
        public boolean hasNext() {
            if (iNotif != null && iNotif.isOnError()) {
                throw Exceptions.propagate(iNotif.getThrowable());
            }
            if (iNotif == null || !iNotif.isOnCompleted()) {
                if (iNotif == null) {
                    try {
                        notify.acquire();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        iNotif = Notification.createOnError(ex);
                        throw Exceptions.propagate(ex);
                    }

                    @SuppressWarnings("unchecked")
                    Notification<? extends T> n = REFERENCE_UPDATER.getAndSet(this, null);
                    iNotif = n;
                    if (iNotif.isOnError()) {
                        throw Exceptions.propagate(iNotif.getThrowable());
                    }
                }
            }
            return !iNotif.isOnCompleted();
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
