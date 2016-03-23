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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class PublisherIntervalRangeSource implements Publisher<Long> {
    final Scheduler scheduler;
    final long start;
    final long end;
    final long initialDelay;
    final long period;
    final TimeUnit unit;
    
    public PublisherIntervalRangeSource(long start, long end, long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
        this.start = start;
        this.end = end;
    }
    
    @Override
    public void subscribe(Subscriber<? super Long> s) {
        IntervalRangeSubscriber is = new IntervalRangeSubscriber(s, start, end);
        s.onSubscribe(is);
        
        Disposable d = scheduler.schedulePeriodicallyDirect(is, initialDelay, period, unit);
        
        is.setResource(d);
    }
    
    static final class IntervalRangeSubscriber extends AtomicLong 
    implements Subscription, Runnable {
        /** */
        private static final long serialVersionUID = -2809475196591179431L;

        final Subscriber<? super Long> actual;
        final long end;
        
        long count;
        
        volatile boolean cancelled;
        
        static final Disposable DISPOSED = new Disposable() {
            @Override
            public void dispose() { }
        };
        
        final AtomicReference<Disposable> resource = new AtomicReference<Disposable>();
        
        public IntervalRangeSubscriber(Subscriber<? super Long> actual, long start, long end) {
            this.actual = actual;
            this.count = start;
            this.end = end;
        }
        
        @Override
        public void request(long n) {
            if (n <= 0) {
                RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
                return;
            }
            
            BackpressureHelper.add(this, n);
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                disposeResource();
            }
        }
        
        void disposeResource() {
            Disposable d = resource.get();
            if (d != DISPOSED) {
                d = resource.getAndSet(DISPOSED);
                if (d != DISPOSED && d != null) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public void run() {
            if (!cancelled) {
                long r = get();
                
                if (r != 0L) {
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
                    
                    if (r != Long.MAX_VALUE) {
                        decrementAndGet();
                    }
                } else {
                    cancelled = true;
                    try {
                        actual.onError(new IllegalStateException("Can't deliver value " + count + " due to lack of requests"));
                    } finally {
                        disposeResource();
                    }
                }
            }
        }
        
        public void setResource(Disposable d) {
            for (;;) {
                Disposable current = resource.get();
                if (current == DISPOSED) {
                    d.dispose();
                    return;
                }
                if (current != null) {
                    RxJavaPlugins.onError(new IllegalStateException("Resource already set!"));
                    return;
                }
                if (resource.compareAndSet(null, d)) {
                    return;
                }
            }
        }
    }
}
