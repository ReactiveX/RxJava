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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOnSubscribeTimerOnceSource implements NbpOnSubscribe<Long> {
    final Scheduler scheduler;
    final long delay;
    final TimeUnit unit;
    public NbpOnSubscribeTimerOnceSource(long delay, TimeUnit unit, Scheduler scheduler) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    @Override
    public void accept(NbpSubscriber<? super Long> s) {
        IntervalOnceSubscriber ios = new IntervalOnceSubscriber(s);
        s.onSubscribe(ios);
        
        Disposable d = scheduler.scheduleDirect(ios, delay, unit);
        
        ios.setResource(d);
    }
    
    static final class IntervalOnceSubscriber extends AtomicReference<Disposable> 
    implements Disposable, Runnable {
        /** */
        private static final long serialVersionUID = -2809475196591179431L;

        final NbpSubscriber<? super Long> actual;
        
        static final Disposable DISPOSED = () -> { };

        /** This state tells the setResource not to call dispose since the run is finishing anyway. */
        static final Disposable DONE = () -> { };
        
        volatile boolean cancelled;
        
        public IntervalOnceSubscriber(NbpSubscriber<? super Long> actual) {
            this.actual = actual;
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                
                Disposable d = get();
                if (d != DISPOSED && d != DONE) {
                    d = getAndSet(DISPOSED);
                    if (d != DISPOSED && d != null) {
                        d.dispose();
                    }
                }
            }
        }
        
        @Override
        public void run() {
            if (!cancelled) {
                actual.onNext(0L);
                actual.onComplete();
            }
            lazySet(DONE);
        }
        
        public void setResource(Disposable d) {
            for (;;) {
                Disposable current = get();
                if (current == DISPOSED) {
                    d.dispose();
                    return;
                }
                if (current == DONE) {
                    return;
                }
                if (current != null) {
                    RxJavaPlugins.onError(new IllegalStateException("Resource already set!"));
                    return;
                }
                if (compareAndSet(null, d)) {
                    return;
                }
            }
        }
    }
}
