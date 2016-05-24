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

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;

/**
 * Returns an Iterable that always returns the item most recently emitted by an Observable, or a
 * seed value if no item has yet been emitted.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.mostRecent.png" alt="">
 */
public final class BlockingOperatorMostRecent {
    private BlockingOperatorMostRecent() {
        throw new IllegalStateException("No instances!");
    }
    /**
     * Returns an {@code Iterable} that always returns the item most recently emitted by the {@code Observable}.
     *
     * @param <T> the value type
     * @param source
     *            the source {@code Observable}
     * @param initialValue
     *            a default item to return from the {@code Iterable} if {@code source} has not yet emitted any
     *            items
     * @return an {@code Iterable} that always returns the item most recently emitted by {@code source}, or
     *         {@code initialValue} if {@code source} has not yet emitted any items
     */
    public static <T> Iterable<T> mostRecent(final Observable<? extends T> source, final T initialValue) {
        return new Iterable<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public Iterator<T> iterator() {
                MostRecentObserver<T> mostRecentObserver = new MostRecentObserver<T>(initialValue);

                /**
                 * Subscribe instead of unsafeSubscribe since this is the final subscribe in the chain
                 * since it is for BlockingObservable.
                 */
                ((Observable<T>)source).subscribe(mostRecentObserver);

                return mostRecentObserver.getIterable();
            }
        };
    }

    private static final class MostRecentObserver<T> extends Subscriber<T> {
        final NotificationLite<T> nl = NotificationLite.instance();
        volatile Object value;

        MostRecentObserver(T value) {
            this.value = nl.next(value);
        }

        @Override
        public void onCompleted() {
            value = nl.completed();
        }

        @Override
        public void onError(Throwable e) {
            value = nl.error(e);
        }

        @Override
        public void onNext(T args) {
            value = nl.next(args);
        }

        /**
         * The {@link Iterator} return is not thread safe. In other words don't call {@link Iterator#hasNext()} in one
         * thread expect {@link Iterator#next()} called from a different thread to work.
         * @return the Iterator instance
         */
        public Iterator<T> getIterable() {
            return new Iterator<T>() {
                /**
                 * buffer to make sure that the state of the iterator doesn't change between calling hasNext() and next().
                 */
                private Object buf = null;

                @Override
                public boolean hasNext() {
                    buf = value;
                    return !nl.isCompleted(buf);
                }

                @Override
                public T next() {
                    try {
                        // if hasNext wasn't called before calling next.
                        if (buf == null)
                            buf = value;
                        if (nl.isCompleted(buf))
                            throw new NoSuchElementException();
                        if (nl.isError(buf)) {
                            throw Exceptions.propagate(nl.getError(buf));
                        }
                        return nl.getValue(buf);
                    }
                    finally {
                        buf = null;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Read only iterator");
                }
            };
        }
    }
}
