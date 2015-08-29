/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.internal.subscribers.DisposableSubscriber;
import io.reactivex.internal.util.Exceptions;

/**
 * Wait for and iterate over the latest values of the source observable. If the source works faster than the
 * iterator, values may be skipped, but not the {@code onError} or {@code onCompleted} events.
 */
public enum BlockingOperatorLatest {
    ;

    /**
     * Returns an {@code Iterable} that blocks until or unless the {@code Observable} emits an item that has not
     * been returned by the {@code Iterable}, then returns that item
     *
     * @param source
     *            the source {@code Observable}
     * @return an {@code Iterable} that blocks until or unless the {@code Observable} emits an item that has not
     *         been returned by the {@code Iterable}, then returns that item
     */
    public static <T> Iterable<T> latest(final Publisher<? extends T> source) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                LatestObserverIterator<T> lio = new LatestObserverIterator<>();
                Observable.<T>fromPublisher(source).materialize().subscribe(lio);
                return lio;
            }
        };
    }

    /** Observer of source, iterator for output. */
    static final class LatestObserverIterator<T> extends DisposableSubscriber<Try<Optional<T>>> implements Iterator<T> {
        final Semaphore notify = new Semaphore(0);
        // observer's notification
        volatile Try<Optional<T>> value;
        /** Updater for the value field. */
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<LatestObserverIterator, Try> REFERENCE_UPDATER
                = AtomicReferenceFieldUpdater.newUpdater(LatestObserverIterator.class, Try.class, "value");

        @Override
        public void onNext(Try<Optional<T>> args) {
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

                    @SuppressWarnings("unchecked")
                    Try<Optional<T>> n = REFERENCE_UPDATER.getAndSet(this, null);
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