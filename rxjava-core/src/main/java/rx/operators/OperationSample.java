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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Samples the observable sequence at each interval.
 */
public final class OperationSample {

    /**
     * Samples the observable sequence at each interval.
     */
    public static <T> Func1<Observer<T>, Subscription> sample(final Observable<T> source, long interval, TimeUnit unit) {
        return new Sample<T>(source, interval, unit);
    }

    private static class Sample<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<T> source;
        private final long interval;
        private final TimeUnit unit;
        
        private final AtomicBoolean hasValue = new AtomicBoolean();
        private final AtomicReference<T> latestValue = new AtomicReference<T>();
        private final AtomicBoolean sourceCompleted = new AtomicBoolean();
        
        private Sample(Observable<T> source, long interval, TimeUnit unit) {
            this.source = source;
            this.interval = interval;
            this.unit = unit;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            Clock clock = new Clock(Schedulers.currentThread(), interval, unit);
            final Subscription clockSubscription = Observable.create(clock).subscribe(new Observer<Long>() {
                @Override
                public void onCompleted() { /* the clock never completes */ }
                
                @Override
                public void onError(Exception e) { /* the clock has no errors */ }
                
                @Override
                public void onNext(Long totalTimePassed) {
                    if (hasValue.get()) {
                        observer.onNext(latestValue.get());
                    }
                }
            });
      
            Subscription sourceSubscription = source.subscribe(new Observer<T>() {
                @Override
                public void onCompleted() {
                    clockSubscription.unsubscribe();
                    sourceCompleted.set(true);
                    observer.onCompleted();
                }
        
                @Override
                public void onError(Exception e) {
                    clockSubscription.unsubscribe();
                    sourceCompleted.set(true);
                    observer.onError(e);
                }
        
                @Override
                public void onNext(T value) {
                    latestValue.set(value);
                    hasValue.set(true);
                }
            });
            
            return clockSubscription;
        }

        private class Clock implements Func1<Observer<Long>, Subscription> {
            private final Scheduler scheduler;
            private final long interval;
            private final TimeUnit unit;
            
            private long timePassed;
            
            private Clock(Scheduler scheduler, long interval, TimeUnit unit) {
              this.scheduler = scheduler;
              this.interval = interval;
              this.unit = unit;
            }
            
            @Override
            public Subscription call(final Observer<Long> observer) {
                return scheduler.schedule(new Func0<Subscription>() {
                    @Override
                    public Subscription call() {
                        if (! sourceCompleted.get()) {
                            timePassed += interval;
                            observer.onNext(timePassed);
                            return Clock.this.call(observer);
                        }
                        return Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                              // TODO Auto-generated method stub
                            }
                        });
                    }
                }, interval, unit);
            }
        }
    }
    
    public static class UnitTest {
        // TODO
    }
}
