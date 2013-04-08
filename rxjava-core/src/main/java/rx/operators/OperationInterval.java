/**
 * Copyright 2013 Netflix, Inc.
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.Tester.UnitTest.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Returns an observable sequence that produces a value after each period.
 * The value starts at 0 and counts up each period.
 */
public final class OperationInterval {

    /**
     * Creates an event each time interval.
     */
    public static Func1<Observer<Long>, Subscription> interval(long interval, TimeUnit unit) {
        return new Interval(interval, unit, Schedulers.currentThread());
    }

    /**
     * Creates an event each time interval.
     */
    public static Func1<Observer<Long>, Subscription> interval(long interval, TimeUnit unit, Scheduler scheduler) {
        return new Interval(interval, unit, scheduler);
    }

    private static class Interval implements Func1<Observer<Long>, Subscription> {
        private final long interval;
        private final TimeUnit unit;
        private final Scheduler scheduler;
        
        private long currentValue;

        private Interval(long interval, TimeUnit unit, Scheduler scheduler) {
            this.interval = interval;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription call(final Observer<Long> observer) {
            return scheduler.schedule(new Func0<Subscription>() {
                @Override
                public Subscription call() {
                    observer.onNext(currentValue);
                    currentValue++;
                    return Interval.this.call(observer);
                }
            }, interval, unit);
        }
    }
    
    public static class UnitTest {
        // TODO
    }
}
