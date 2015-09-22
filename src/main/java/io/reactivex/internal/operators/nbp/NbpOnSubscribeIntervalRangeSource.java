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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOnSubscribeIntervalRangeSource implements NbpOnSubscribe<Long> {
    final Scheduler scheduler;
    final long start;
    final long end;
    final long initialDelay;
    final long period;
    final TimeUnit unit;
    
    public NbpOnSubscribeIntervalRangeSource(long start, long end, long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
        this.start = start;
        this.end = end;
    }
    
    @Override
    public void accept(NbpSubscriber<? super Long> s) {
        IntervalRangeSubscriber is = new IntervalRangeSubscriber(s, start, end);
        s.onSubscribe(is);
        
        Disposable d = scheduler.schedulePeriodicallyDirect(is, initialDelay, period, unit);
        
        is.setResource(d);
    }
    
    static final class IntervalRangeSubscriber
    implements Disposable, Runnable {

        final NbpSubscriber<? super Long> actual;
        final long end;
        
        long count;
        
        volatile boolean cancelled;
        
        static final Disposable DISPOSED = () -> { };
        
        volatile Disposable resource;
        static final AtomicReferenceFieldUpdater<IntervalRangeSubscriber, Disposable> RESOURCE =
                AtomicReferenceFieldUpdater.newUpdater(IntervalRangeSubscriber.class, Disposable.class, "resource");
        
        public IntervalRangeSubscriber(NbpSubscriber<? super Long> actual, long start, long end) {
            this.actual = actual;
            this.count = start;
            this.end = end;
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                disposeResource();
            }
        }
        
        void disposeResource() {
            Disposable d = resource;
            if (d != DISPOSED) {
                d = RESOURCE.getAndSet(this, DISPOSED);
                if (d != DISPOSED && d != null) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public void run() {
            if (!cancelled) {
                long c = count;
                actual.onNext(c);
                
                if (c == end) {
                    cancelled = true;
                    try {
                        actual.onComplete();
                    } finally {
                        disposeResource();
                    }
                    return;
                }
                
                count = c + 1;
                
            }
        }
        
        public void setResource(Disposable d) {
            for (;;) {
                Disposable current = resource;
                if (current == DISPOSED) {
                    d.dispose();
                    return;
                }
                if (current != null) {
                    RxJavaPlugins.onError(new IllegalStateException("Resource already set!"));
                    return;
                }
                if (RESOURCE.compareAndSet(this, null, d)) {
                    return;
                }
            }
        }
    }
}
