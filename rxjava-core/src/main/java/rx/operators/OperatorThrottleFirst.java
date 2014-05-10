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

import java.util.concurrent.TimeUnit;

import rx.*;
import rx.Observable.Operator;

/**
 * Throttle by windowing a stream and returning the first value in each window.
 */
public final class OperatorThrottleFirst<T> implements Operator<T, T> {

    private final long timeInMilliseconds;
    private final Scheduler scheduler;

    public OperatorThrottleFirst(long windowDuration, TimeUnit unit, Scheduler scheduler) {
        this.timeInMilliseconds = unit.toMillis(windowDuration);
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            private long lastOnNext = 0;

            @Override
            public void onNext(T v) {
                long now = scheduler.now();
                if (lastOnNext == 0 || now - lastOnNext >= timeInMilliseconds) {
                    lastOnNext = now;
                    subscriber.onNext(v);
                }
            }

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

        };
    }
}
