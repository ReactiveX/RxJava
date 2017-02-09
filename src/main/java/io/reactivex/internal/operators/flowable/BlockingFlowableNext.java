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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.DisposableSubscriber;

/**
 * Returns an Iterable that blocks until the Observable emits another item, then returns that item.
 * <p>
 * <img width="640" height="490" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.next.png" alt="">
 *
 * @param <T> the value type
 */
public final class BlockingFlowableNext<T> implements Iterable<T> {

    final Publisher<? extends T> source;

    public BlockingFlowableNext(Publisher<? extends T> source) {
        this.source = source;
    }

    @Override
    public Iterator<T> iterator() {
        NextSubscriber<T> nextSubscriber = new NextSubscriber<T>();
        return new NextIterator<T>(source, nextSubscriber);
    }

    // test needs to access the observer.waiting flag
    static final class NextIterator<T> implements Iterator<T> {

        private final NextSubscriber<T> observer;
        private final Publisher<? extends T> items;
        private T next;
        private boolean hasNext = true;
        private boolean isNextConsumed = true;
        private Throwable error;
        private boolean started;

        NextIterator(Publisher<? extends T> items, NextSubscriber<T> observer) {
            this.items = items;
            this.observer = observer;
        }

        @Override
        public boolean hasNext() {
            if (error != null) {
                // If any error has already been thrown, throw it again.
                throw ExceptionHelper.wrapOrThrow(error);
            }
            // Since an iterator should not be used in different thread,
            // so we do not need any synchronization.
            if (!hasNext) {
                // the iterator has reached the end.
                return false;
            }
            // next has not been used yet.
            return !isNextConsumed || moveToNext();
        }

        private boolean moveToNext() {
            try {
                if (!started) {
                    started = true;
                    // if not started, start now
                    observer.setWaiting();
                    Flowable.<T>fromPublisher(items)
                    .materialize().subscribe(observer);
                }

                Notification<T> nextNotification = observer.takeNext();
                if (nextNotification.isOnNext()) {
                    isNextConsumed = false;
                    next = nextNotification.getValue();
                    return true;
                }
                // If an observable is completed or fails,
                // hasNext() always return false.
                hasNext = false;
                if (nextNotification.isOnComplete()) {
                    return false;
                }
                if (nextNotification.isOnError()) {
                    error = nextNotification.getError();
                    throw ExceptionHelper.wrapOrThrow(error);
                }
                throw new IllegalStateException("Should not reach here");
            } catch (InterruptedException e) {
                observer.dispose();
                error = e;
                throw ExceptionHelper.wrapOrThrow(e);
            }
        }

        @Override
        public T next() {
            if (error != null) {
                // If any error has already been thrown, throw it again.
                throw ExceptionHelper.wrapOrThrow(error);
            }
            if (hasNext()) {
                isNextConsumed = true;
                return next;
            }
            else {
                throw new NoSuchElementException("No more elements");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }
    }

    static final class NextSubscriber<T> extends DisposableSubscriber<Notification<T>> {
        private final BlockingQueue<Notification<T>> buf = new ArrayBlockingQueue<Notification<T>>(1);
        final AtomicInteger waiting = new AtomicInteger();

        @Override
        public void onComplete() {
            // ignore
        }

        @Override
        public void onError(Throwable e) {
            RxJavaPlugins.onError(e);
        }

        @Override
        public void onNext(Notification<T> args) {

            if (waiting.getAndSet(0) == 1 || !args.isOnNext()) {
                Notification<T> toOffer = args;
                while (!buf.offer(toOffer)) {
                    Notification<T> concurrentItem = buf.poll();

                    // in case if we won race condition with onComplete/onError method
                    if (concurrentItem != null && !concurrentItem.isOnNext()) {
                        toOffer = concurrentItem;
                    }
                }
            }

        }

        public Notification<T> takeNext() throws InterruptedException {
            setWaiting();
            BlockingHelper.verifyNonBlocking();
            return buf.take();
        }
        void setWaiting() {
            waiting.set(1);
        }
    }
}
