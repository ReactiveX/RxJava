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

import java.util.concurrent.TimeUnit;

import rx.*;
import rx.Observable.Operator;

/**
 * Throttle by windowing a stream and returning the first value in each window.
 */
public final class OperatorThrottleFirst<T> implements Operator<T, T> {

    private final long windowDurationMs;
    private final Scheduler scheduler;
    
    private static long UNSET = Long.MIN_VALUE;

    public OperatorThrottleFirst(long windowDurationMs, TimeUnit unit, Scheduler scheduler) {
        this.windowDurationMs = unit.toMillis(windowDurationMs);
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            private long nextWindowStartTime = UNSET; 

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(T t) {
                long now = scheduler.now();
                if (nextWindowStartTime == UNSET) {
                    nextWindowStartTime = now + windowDurationMs;
                    subscriber.onNext(t);
                } else if (now >= nextWindowStartTime) {
                    // ensure that we advance the next window start time to just beyond now
                    long n = (now - nextWindowStartTime) / windowDurationMs + 1;
                    nextWindowStartTime += n * windowDurationMs;
                    subscriber.onNext(t);
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
