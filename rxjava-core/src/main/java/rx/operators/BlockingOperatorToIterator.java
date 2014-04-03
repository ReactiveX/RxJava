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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;

/**
 * Returns an Iterator that iterates over all items emitted by a specified Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toIterator.png">
 * <p>
 * 
 * @see <a href="https://github.com/Netflix/RxJava/issues/50">Issue #50</a>
 */
public class BlockingOperatorToIterator {

    /**
     * Returns an iterator that iterates all values of the observable.
     * 
     * @param <T>
     *            the type of source.
     * @return the iterator that could be used to iterate over the elements of the observable.
     */
    public static <T> Iterator<T> toIterator(Observable<? extends T> source) {
        final BlockingQueue<Notification<? extends T>> notifications = new LinkedBlockingQueue<Notification<? extends T>>();

        // using subscribe instead of unsafeSubscribe since this is a BlockingObservable "final subscribe"
        source.materialize().subscribe(new Subscriber<Notification<? extends T>>() {
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
                notifications.offer(args);
            }
        });

        return new Iterator<T>() {
            private Notification<? extends T> buf;

            @Override
            public boolean hasNext() {
                if (buf == null) {
                    buf = take();
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
                    return notifications.take();
                } catch (InterruptedException e) {
                    throw Exceptions.propagate(e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Read-only iterator");
            }
        };
    }

}
