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

import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Operation Timer with several overloads.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.timer.aspx'>MSDN Observable.Timer</a>
 */
public final class OperationTimer {
    private OperationTimer() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Emit a single 0L after the specified time elapses.
     */
    public static class TimerOnce implements OnSubscribeFunc<Long> {
        private final Scheduler scheduler;
        private final long dueTime;
        private final TimeUnit dueUnit;

        public TimerOnce(long dueTime, TimeUnit unit, Scheduler scheduler) {
            this.scheduler = scheduler;
            this.dueTime = dueTime;
            this.dueUnit = unit;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Long> t1) {
            return scheduler.schedule(new Action1<Inner>() {
                @Override
                public void call(Inner inner) {
                    t1.onNext(0L);
                    t1.onCompleted();
                }

            }, dueTime, dueUnit);
        }
    }

    /**
     * Emit 0L after the initial period and ever increasing number after each period.
     */
    public static class TimerPeriodically implements OnSubscribeFunc<Long> {
        private final Scheduler scheduler;
        private final long initialDelay;
        private final long period;
        private final TimeUnit unit;

        public TimerPeriodically(long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {
            this.scheduler = scheduler;
            this.initialDelay = initialDelay;
            this.period = period;
            this.unit = unit;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Long> t1) {
            return scheduler.schedulePeriodically(new Action1<Inner>() {
                long count;

                @Override
                public void call(Inner inner) {
                    t1.onNext(count++);
                }
            }, initialDelay, period, unit);
        }
    }
}
