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

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.TimeInterval;

/**
 * Records the time interval between consecutive elements in an observable sequence.
 */
public final class OperatorTimeInterval<T> implements Operator<TimeInterval<T>, T> {

    private final Scheduler scheduler;

    public OperatorTimeInterval(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super TimeInterval<T>> subscriber) {
        return new Subscriber<T>(subscriber) {

            // The beginning time is the time when the observer subscribes.
            private long lastTimestamp = scheduler.now();


            @Override
            public void onNext(T args) {
                long nowTimestamp = scheduler.now();
                subscriber.onNext(new TimeInterval<T>(nowTimestamp - lastTimestamp, args));
                lastTimestamp = nowTimestamp;
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
