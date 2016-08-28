/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;

public final class ObservableIntervalRange extends Observable<Long> {
    final Scheduler scheduler;
    final long start;
    final long end;
    final long initialDelay;
    final long period;
    final TimeUnit unit;
    
    public ObservableIntervalRange(long start, long end, long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
        this.start = start;
        this.end = end;
    }
    
    @Override
    public void subscribeActual(Observer<? super Long> s) {
        IntervalRangeSubscriber is = new IntervalRangeSubscriber(s, start, end);
        s.onSubscribe(is);
        
        Disposable d = scheduler.schedulePeriodicallyDirect(is, initialDelay, period, unit);
        
        is.setResource(d);
    }
    
    static final class IntervalRangeSubscriber
    extends AtomicReference<Disposable>
    implements Disposable, Runnable {

        /** */
        private static final long serialVersionUID = 1891866368734007884L;
        
        final Observer<? super Long> actual;
        final long end;
        
        long count;
        
        public IntervalRangeSubscriber(Observer<? super Long> actual, long start, long end) {
            this.actual = actual;
            this.count = start;
            this.end = end;
        }
        
        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }

        @Override
        public void run() {
            if (!isDisposed()) {
                long c = count;
                actual.onNext(c);
                
                if (c == end) {
                    DisposableHelper.dispose(this);
                    actual.onComplete();
                    return;
                }
                
                count = c + 1;
                
            }
        }
        
        public void setResource(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }
    }
}
