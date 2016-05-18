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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.internal.util.RxRingBuffer;

/**
 * Returns an Iterator that iterates over all items emitted by a specified Observable.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.toIterator.png" alt="">
 * <p>
 * 
 * @see <a href="https://github.com/ReactiveX/RxJava/issues/50">Issue #50</a>
 */
public final class BlockingOperatorToIterator {
    private BlockingOperatorToIterator() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Returns an iterator that iterates all values of the observable.
     * 
     * @param <T>
     *            the type of source.
     * @param source the source Observable
     * @return the iterator that could be used to iterate over the elements of the observable.
     */
    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> toIterator(Observable<? extends T> source) {
        SubscriberIterator<T> subscriber = new SubscriberIterator<T>();

        // using subscribe instead of unsafeSubscribe since this is a BlockingObservable "final subscribe"
        ((Observable<T>)source).materialize().subscribe(subscriber);
        return subscriber;
    }

    public static final class SubscriberIterator<T>
        extends Subscriber<Notification<? extends T>> implements Iterator<T> {

        static final int LIMIT = 3 * RxRingBuffer.SIZE / 4;

        private final BlockingQueue<Notification<? extends T>> notifications;
        private Notification<? extends T> buf;
        private int received;

        public SubscriberIterator() {
            this.notifications = new LinkedBlockingQueue<Notification<? extends T>>();
        }

        @Override
        public void onStart() {
            request(RxRingBuffer.SIZE);
        }

        @Override
        public void onCompleted() {
            // ignore
        }

        @Override
        public void onError(Throwable e) {
            notifications.offer(Notification.<T>createOnError(e));
        }

        @Override
        public void onNext(Notification<? extends T> args) {
            notifications.offer(args);
        }

        @Override
        public boolean hasNext() {
            if (buf == null) {
                buf = take();
                received++;
                if (received >= LIMIT) {
                    request(received);
                    received = 0;
                }
            }
            if (buf.isOnError()) {
                throw Exceptions.propagate(buf.getThrowable());
            }
            return !buf.isOnCompleted();
        }

        @Override
        public T next() {
            if (hasNext()) {
                T result = buf.getValue();
                buf = null;
                return result;
            }
            throw new NoSuchElementException();
        }

        private Notification<? extends T> take() {
            try {
                Notification<? extends T> poll = notifications.poll();
                if (poll != null) {
                    return poll;
                }
                return notifications.take();
            } catch (InterruptedException e) {
                unsubscribe();
                throw Exceptions.propagate(e);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read-only iterator");
        }
    }
}
