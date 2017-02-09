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

package io.reactivex.internal.operators.observable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.internal.util.*;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Returns an Iterable that blocks until the Observable emits another item, then returns that item.
 * <p>
 * <img width="640" height="490" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.next.png" alt="">
 * 
 * @param <T> the value type
 */
public final class BlockingObservableNext<T> implements Iterable<T> {

    final ObservableSource<T> source;

    public BlockingObservableNext(ObservableSource<T> source) {
        this.source = source;
    }

    @Override
    public Iterator<T> iterator() {
        NextObserver<T> nextObserver = new NextObserver<T>();
        return new NextIterator<T>(source, nextObserver);
    }

    // test needs to access the observer.waiting flag
    static final class NextIterator<T> implements Iterator<T> {

        private final NextObserver<T> observer;
        private final ObservableSource<T> items;
        private T next;
        private boolean hasNext = true;
        private boolean isNextConsumed = true;
        private Throwable error;
        private boolean started;

        NextIterator(ObservableSource<T> items, NextObserver<T> observer) {
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
            if (!started) {
                started = true;
                // if not started, start now
                observer.setWaiting();
                new ObservableMaterialize<T>(items).subscribe(observer);
            }

            Notification<T> nextNotification;

            try {
                nextNotification = observer.takeNext();
            } catch (InterruptedException e) {
                observer.dispose();
                error = e;
                throw ExceptionHelper.wrapOrThrow(e);
            }

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
            error = nextNotification.getError();
            throw ExceptionHelper.wrapOrThrow(error);
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

    static final class NextObserver<T> extends DisposableObserver<Notification<T>> {
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
