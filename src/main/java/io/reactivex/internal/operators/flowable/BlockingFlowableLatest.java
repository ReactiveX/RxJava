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

package io.reactivex.internal.operators.flowable;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.DisposableSubscriber;

/**
 * Wait for and iterate over the latest values of the source observable. If the source works faster than the
 * iterator, values may be skipped, but not the {@code onError} or {@code onComplete} events.
 * @param <T> the value type emitted
 */
public final class BlockingFlowableLatest<T> implements Iterable<T> {

    final Publisher<? extends T> source;

    public BlockingFlowableLatest(Publisher<? extends T> source) {
        this.source = source;
    }

    @Override
    public Iterator<T> iterator() {
        LatestSubscriberIterator<T> lio = new LatestSubscriberIterator<T>();
        Flowable.<T>fromPublisher(source).materialize().subscribe(lio);
        return lio;
    }

    /** Subscriber of source, iterator for output. */
    static final class LatestSubscriberIterator<T> extends DisposableSubscriber<Notification<T>> implements Iterator<T> {
        final Semaphore notify = new Semaphore(0);
        // observer's notification
        final AtomicReference<Notification<T>> value = new AtomicReference<Notification<T>>();

        // iterator's notification
        Notification<T> iteratorNotification;

        @Override
        public void onNext(Notification<T> args) {
            boolean wasNotAvailable = value.getAndSet(args) == null;
            if (wasNotAvailable) {
                notify.release();
            }
        }

        @Override
        public void onError(Throwable e) {
            RxJavaPlugins.onError(e);
        }

        @Override
        public void onComplete() {
            // not expected
        }

        @Override
        public boolean hasNext() {
            if (iteratorNotification != null && iteratorNotification.isOnError()) {
                throw ExceptionHelper.wrapOrThrow(iteratorNotification.getError());
            }
            if (iteratorNotification == null || iteratorNotification.isOnNext()) {
                if (iteratorNotification == null) {
                    try {
                        BlockingHelper.verifyNonBlocking();
                        notify.acquire();
                    } catch (InterruptedException ex) {
                        dispose();
                        iteratorNotification = Notification.createOnError(ex);
                        throw ExceptionHelper.wrapOrThrow(ex);
                    }

                    Notification<T> n = value.getAndSet(null);
                    iteratorNotification = n;
                    if (n.isOnError()) {
                        throw ExceptionHelper.wrapOrThrow(n.getError());
                    }
                }
            }
            return iteratorNotification.isOnNext();
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
