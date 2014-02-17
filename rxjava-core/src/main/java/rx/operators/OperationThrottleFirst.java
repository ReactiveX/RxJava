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
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Throttle by windowing a stream and returning the first value in each window.
 */
public final class OperationThrottleFirst {

    /**
     * Throttles to first value in each window.
     * 
     * @param items
     *            The {@link Observable} which is publishing events.
     * @param windowDuration
     *            Duration of windows within with the first value will be chosen.
     * @param unit
     *            The unit of time for the specified timeout.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> throttleFirst(Observable<T> items, long windowDuration, TimeUnit unit) {
        return throttleFirst(items, windowDuration, unit, Schedulers.computation());
    }

    /**
     * Throttles to first value in each window.
     * 
     * @param items
     *            The {@link Observable} which is publishing events.
     * @param windowDuration
     *            Duration of windows within with the first value will be chosen.
     * @param unit
     *            The unit of time for the specified timeout.
     * @param scheduler
     *            The {@link Scheduler} to use internally to manage the timers which handle timeout for each event.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> throttleFirst(final Observable<T> items, final long windowDuration, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {

                final AtomicLong lastOnNext = new AtomicLong(0);
                final long timeInMilliseconds = unit.toMillis(windowDuration);

                return items.filter(new Func1<T, Boolean>() {

                    @Override
                    public Boolean call(T value) {
                        long now = scheduler.now();
                        if (lastOnNext.get() == 0 || now - lastOnNext.get() >= timeInMilliseconds) {
                            lastOnNext.set(now);
                            return Boolean.TRUE;
                        } else {
                            return Boolean.FALSE;
                        }
                    }

                }).subscribe(observer);
            }
        };
    }
}
