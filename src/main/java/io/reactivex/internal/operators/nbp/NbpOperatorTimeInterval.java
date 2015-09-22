/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.nbp;

import java.util.concurrent.TimeUnit;

import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Timed;

public final class NbpOperatorTimeInterval<T> implements NbpOperator<Timed<T>, T> {
    final Scheduler scheduler;
    final TimeUnit unit;
    
    public NbpOperatorTimeInterval(TimeUnit unit, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.unit = unit;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super Timed<T>> t) {
        return new TimeIntervalSubscriber<>(t, unit, scheduler);
    }
    
    static final class TimeIntervalSubscriber<T> implements NbpSubscriber<T> {
        final NbpSubscriber<? super Timed<T>> actual;
        final TimeUnit unit;
        final Scheduler scheduler;
        
        long lastTime;
        
        public TimeIntervalSubscriber(NbpSubscriber<? super Timed<T>> actual, TimeUnit unit, Scheduler scheduler) {
            this.actual = actual;
            this.scheduler = scheduler;
            this.unit = unit;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            lastTime = scheduler.now(unit);
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(T t) {
            long now = scheduler.now(unit);
            long last = lastTime;
            lastTime = now;
            long delta = now - last;
            actual.onNext(new Timed<>(t, delta, unit));
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }
}
