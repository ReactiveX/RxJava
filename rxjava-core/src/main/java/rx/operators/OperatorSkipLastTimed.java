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
import java.util.List;
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

            private List<Timestamped<T>> buffer = new ArrayList<Timestamped<T>>();

            @Override
            public void onNext(T value) {
                buffer.add(new Timestamped<T>(scheduler.now(), value));
            }

            @Override
            public void onError(Throwable e) {
                buffer = Collections.emptyList();
                subscriber.onError(e);
            }

            @Override
            public void onCompleted() {
                long limit = scheduler.now() - timeInMillis;
                try {
                    for (Timestamped<T> v : buffer) {
                        if (v.getTimestampMillis() < limit) {
                            try {
                                subscriber.onNext(v.getValue());
                            } catch (Throwable t) {
                                subscriber.onError(t);
                                return;
                            }
                        } else {
                            break;
                        }
                    }
                    subscriber.onCompleted();
                } finally {
                    buffer = Collections.emptyList();
                }
            }

        };
    }
}
