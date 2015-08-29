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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.reactivestreams.Publisher;

import io.reactivex.Observable;
import io.reactivex.Try;
import io.reactivex.internal.subscribers.DisposableSubscriber;
import io.reactivex.internal.util.Exceptions;

/**
 * Returns an Iterable that blocks until the Observable emits another item, then returns that item.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.next.png" alt="">
 */
public enum BlockingOperatorNext {
    ;
    /**
     * Returns an {@code Iterable} that blocks until the {@code Observable} emits another item, then returns
     * that item.
     *
     * @param items
     *            the {@code Observable} to observe
     * @return an {@code Iterable} that behaves like a blocking version of {@code items}
     */
    public static <T> Iterable<T> next(final Publisher<? extends T> items) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                NextObserver<T> nextObserver = new NextObserver<>();
                return new NextIterator<>(items, nextObserver);
            }
        };

    }

    // test needs to access the observer.waiting flag non-blockingly.
    /* private */static final class NextIterator<T> implements Iterator<T> {

        private final NextObserver<T> observer;
        private final Publisher<? extends T> items;
        private T next;
        private boolean hasNext = true;
        private boolean isNextConsumed = true;
        private Throwable error = null;
        private boolean started = false;

        private NextIterator(Publisher<? extends T> items, NextObserver<T> observer) {
            this.items = items;
            this.observer = observer;
        }

        @Override
        public boolean hasNext() {
            if (error != null) {
                // If any error has already been thrown, throw it again.
                throw Exceptions.propagate(error);
            }
            // Since an iterator should not be used in different thread,
            // so we do not need any synchronization.
            if (hasNext == false) {
                // the iterator has reached the end.
                return false;
            }
            if (isNextConsumed == false) {
                // next has not been used yet.
                return true;
            }
            return moveToNext();
        }

        private boolean moveToNext() {
            try {
                if (!started) {
                    started = true;
                    // if not started, start now
                    observer.setWaiting(1);
                    Observable.<T>fromPublisher(items)
                    .materialize().subscribe(observer);
                }
                
                Try<Optional<T>> nextNotification = observer.takeNext();
                if (isOnNext(nextNotification)) {
                    isNextConsumed = false;
                    next = nextNotification.value().get();
                    return true;
                }
                // If an observable is completed or fails,
                // hasNext() always return false.
                hasNext = false;
                if (isOnComplete(nextNotification)) {
                    return false;
                }
                if (nextNotification.hasError()) {
                    error = nextNotification.error();
                    throw Exceptions.propagate(error);
                }
                throw new IllegalStateException("Should not reach here");
            } catch (InterruptedException e) {
                observer.dispose();
                Thread.currentThread().interrupt();
                error = e;
                throw Exceptions.propagate(error);
            }
        }

        @Override
        public T next() {
            if (error != null) {
                // If any error has already been thrown, throw it again.
                throw Exceptions.propagate(error);
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

    private static class NextObserver<T> extends DisposableSubscriber<Try<Optional<T>>> {
        private final BlockingQueue<Try<Optional<T>>> buf = new ArrayBlockingQueue<>(1);
        @SuppressWarnings("unused")
        volatile int waiting;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<NextObserver> WAITING_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(NextObserver.class, "waiting");

        @Override
        public void onComplete() {
            // ignore
        }

        @Override
        public void onError(Throwable e) {
            // ignore
        }

        @Override
        public void onNext(Try<Optional<T>> args) {

            if (WAITING_UPDATER.getAndSet(this, 0) == 1 || !isOnNext(args)) {
                Try<Optional<T>> toOffer = args;
                while (!buf.offer(toOffer)) {
                    Try<Optional<T>> concurrentItem = buf.poll();

                    // in case if we won race condition with onComplete/onError method
                    if (concurrentItem != null && !isOnNext(concurrentItem)) {
                        toOffer = concurrentItem;
                    }
                }
            }

        }
        
        public Try<Optional<T>> takeNext() throws InterruptedException {
            setWaiting(1);
            return buf.take();
        }
        void setWaiting(int value) {
            waiting = value;
        }
    }
    
    static <T> boolean isOnNext(Try<Optional<T>> notification) {
        return notification.hasValue() && notification.value().isPresent();
    }
    
    static <T> boolean isOnComplete(Try<Optional<T>> notification) {
        return notification.hasValue() && !notification.value().isPresent();
    }

}