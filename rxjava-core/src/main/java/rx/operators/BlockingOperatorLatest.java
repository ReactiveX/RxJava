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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;

/**
 * Wait for and iterate over the latest values of the source observable.
 * If the source works faster than the iterator, values may be skipped, but
 * not the onError or onCompleted events.
 */
public final class BlockingOperatorLatest {
    /** Utility class. */
    private BlockingOperatorLatest() {
        throw new IllegalStateException("No instances!");
    }

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
        final AtomicReference<Notification<? extends T>> reference = new AtomicReference<Notification<? extends T>>();

        @Override
        public void onNext(Notification<? extends T> args) {
            boolean wasntAvailable = reference.getAndSet(args) == null;
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

                    iNotif = reference.getAndSet(null);
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
