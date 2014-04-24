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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Timestamped;

/**
 * Skip delivering values in the time window before the values.
 */
public class OperatorSkipLastTimed<T> implements Operator<T, T> {

    private final long timeInMillis;
    private final Scheduler scheduler;

    public OperatorSkipLastTimed(long time, TimeUnit unit, Scheduler scheduler) {
        this.timeInMillis = unit.toMillis(time);
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            private Deque<Timestamped<T>> buffer = new ArrayDeque<Timestamped<T>>();

            private void emitItemsOutOfWindow(long now) {
                long limit = now - timeInMillis;
                while (!buffer.isEmpty()) {
                    Timestamped<T> v = buffer.getFirst();
                    if (v.getTimestampMillis() < limit) {
                        buffer.removeFirst();
                        subscriber.onNext(v.getValue());
                    } else {
                        break;
                    }
                }
            }

            @Override
            public void onNext(T value) {
                long now = scheduler.now();
                emitItemsOutOfWindow(now);
                buffer.offerLast(new Timestamped<T>(now, value));
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onCompleted() {
                emitItemsOutOfWindow(scheduler.now());
                subscriber.onCompleted();
            }

        };
    }
}
