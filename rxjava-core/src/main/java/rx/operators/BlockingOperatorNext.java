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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;

/**
 * Returns an Iterable that blocks until the Observable emits another item, then returns that item.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.next.png">
 */
public final class BlockingOperatorNext {

    public static <T> Iterable<T> next(final Observable<? extends T> items) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                NextObserver<T> nextObserver = new NextObserver<T>();
                final NextIterator<T> nextIterator = new NextIterator<T>(nextObserver);

                items.materialize().subscribe(nextObserver);

                return nextIterator;
            }
        };

    }

    // test needs to access the observer.waiting flag non-blockingly.
    /* private */static final class NextIterator<T> implements Iterator<T> {

        private final NextObserver<? extends T> observer;
        private T next;
        private boolean hasNext = true;
        private boolean isNextConsumed = true;
        private Throwable error = null;

        private NextIterator(NextObserver<? extends T> observer) {
            this.observer = observer;
        }

        // in tests, set the waiting flag without blocking for the next value to 
        // allow lockstepping instead of multi-threading
        void setWaiting(boolean value) {
            observer.waiting.set(value);
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
                Notification<? extends T> nextNotification = observer.takeNext();
                if (nextNotification.isOnNext()) {
                    isNextConsumed = false;
                    next = nextNotification.getValue();
                    return true;
                }
                // If an observable is completed or fails,
                // hasNext() always return false.
                hasNext = false;
                if (nextNotification.isOnCompleted()) {
                    return false;
                }
                if (nextNotification.isOnError()) {
                    error = nextNotification.getThrowable();
                    throw Exceptions.propagate(error);
                }
                throw new IllegalStateException("Should not reach here");
            } catch (InterruptedException e) {
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

    private static class NextObserver<T> extends Subscriber<Notification<? extends T>> {
        private final BlockingQueue<Notification<? extends T>> buf = new ArrayBlockingQueue<Notification<? extends T>>(1);
        private final AtomicBoolean waiting = new AtomicBoolean(false);

        @Override
        public void onCompleted() {
            // ignore
        }

        @Override
        public void onError(Throwable e) {
            // ignore
        }

        @Override
        public void onNext(Notification<? extends T> args) {

            if (waiting.getAndSet(false) || !args.isOnNext()) {
                Notification<? extends T> toOffer = args;
                while (!buf.offer(toOffer)) {
                    Notification<? extends T> concurrentItem = buf.poll();

                    // in case if we won race condition with onComplete/onError method
                    if (concurrentItem != null && !concurrentItem.isOnNext()) {
                        toOffer = concurrentItem;
                    }
                }
            }

        }

        public Notification<? extends T> takeNext() throws InterruptedException {
            waiting.set(true);
            return buf.take();
        }

    }
}
