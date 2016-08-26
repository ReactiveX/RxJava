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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observers.DisposableObserver;

/**
 * Returns an Iterable that blocks until the Observable emits another item, then returns that item.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.next.png" alt="">
 */
public enum BlockingObservableNext {
    ;
    /**
     * Returns an {@code Iterable} that blocks until the {@code Observable} emits another item, then returns
     * that item.
     *
     * @param <T> the value type
     * @param items
     *            the {@code Observable} to observe
     * @return an {@code Iterable} that behaves like a blocking version of {@code items}
     */
    public static <T> Iterable<T> next(final Observable<? extends T> items) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                NextObserver<T> nextObserver = new NextObserver<T>();
                return new NextIterator<T>(items, nextObserver);
            }
        };

    }

    // test needs to access the observer.waiting flag
    static final class NextIterator<T> implements Iterator<T> {

        private final NextObserver<T> observer;
        private final Observable<? extends T> items;
        private T next;
        private boolean hasNext = true;
        private boolean isNextConsumed = true;
        private Throwable error;
        private boolean started;

        NextIterator(Observable<? extends T> items, NextObserver<T> observer) {
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
                    @SuppressWarnings("unchecked")
                    Observable<T> nbpObservable = (Observable<T>)items;
                    nbpObservable.materialize().subscribe(observer);
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
                Thread.currentThread().interrupt();
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

    static final class NextObserver<T> extends DisposableObserver<Notification<T>> {
        private final BlockingQueue<Notification<T>> buf = new ArrayBlockingQueue<Notification<T>>(1);
        final AtomicInteger waiting = new AtomicInteger();

        @Override
        public void onComplete() {
            // ignore
        }

        @Override
        public void onError(Throwable e) {
            // ignore
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
            return buf.take();
        }
        void setWaiting() {
            waiting.set(1);
        }
    }
}