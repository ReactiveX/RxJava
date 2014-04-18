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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observable.Operator;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Timestamped;

/**
 * Bypasses a specified number of elements at the end of an observable sequence.
 */
public class OperationSkipLast<T> implements Operator<T, T> {

    private final int count;

    public OperationSkipLast(int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("count could not be negative");
        }
        this.count = count;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {
            /**
             * Store the last count elements until now.
             */
            private final Deque<T> deque = new LinkedList<T>();

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(T value) {
                if (count == 0) {
                    // If count == 0, we do not need to put value into deque
                    // and remove it at once. We can emit the value
                    // directly.
                    subscriber.onNext(value);
                    return;
                }
                deque.offerLast(value);
                if (deque.size() > count) {
                    // Now deque has count + 1 elements, so the first
                    // element in the deque definitely does not belong
                    // to the last count elements of the source
                    // sequence. We can emit it now.
                    subscriber.onNext(deque.removeFirst());
                }
            }

        };
    }

    /**
     * Skip delivering values in the time window before the values.
     *
     * @param <T>
     *            the result value type
     */
    public static final class SkipLastTimed<T> implements OnSubscribeFunc<T> {
        final Observable<? extends T> source;
        final long timeInMillis;
        final Scheduler scheduler;

        public SkipLastTimed(Observable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
            this.source = source;
            this.timeInMillis = unit.toMillis(time);
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            return source.unsafeSubscribe(new SourceObserver<T>(t1, timeInMillis, scheduler));
        }

        /** Observes the source. */
        private static final class SourceObserver<T> extends Subscriber<T> {
            final Observer<? super T> observer;
            final long timeInMillis;
            final Scheduler scheduler;
            List<Timestamped<T>> buffer = new ArrayList<Timestamped<T>>();

            public SourceObserver(Observer<? super T> observer,
                    long timeInMillis, Scheduler scheduler) {
                this.observer = observer;
                this.timeInMillis = timeInMillis;
                this.scheduler = scheduler;
            }

            @Override
            public void onNext(T args) {
                buffer.add(new Timestamped<T>(scheduler.now(), args));
            }

            @Override
            public void onError(Throwable e) {
                buffer = Collections.emptyList();
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                long limit = scheduler.now() - timeInMillis;
                try {
                    for (Timestamped<T> v : buffer) {
                        if (v.getTimestampMillis() < limit) {
                            try {
                                observer.onNext(v.getValue());
                            } catch (Throwable t) {
                                observer.onError(t);
                                return;
                            }
                        } else {
                            observer.onCompleted();
                            break;
                        }
                    }
                } finally {
                    buffer = Collections.emptyList();
                }
            }

        }
    }
}
