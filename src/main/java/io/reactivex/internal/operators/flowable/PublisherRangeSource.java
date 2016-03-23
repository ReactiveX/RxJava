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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * 
 */
public final class PublisherRangeSource implements Publisher<Integer> {
    final int start;
    final long end;
    public PublisherRangeSource(int start, int count) {
        this.start = start;
        this.end = (long)start + (count - 1);
    }
    @Override
    public void subscribe(Subscriber<? super Integer> s) {
        s.onSubscribe(new RangeSubscription(s, start, end));
    }
    
    static final class RangeSubscription extends AtomicLong implements Subscription {
        /** */
        private static final long serialVersionUID = 7600071995978874818L;
        final long end;
        final Subscriber<? super Integer> actual;

        long index;
        volatile boolean cancelled;
        
        public RangeSubscription(Subscriber<? super Integer> actual, int start, long end) {
            this.actual = actual;
            this.index = start;
            this.end = end;
        }
        @Override
        public void request(long n) {
            if (n == Long.MAX_VALUE && compareAndSet(0L, Long.MAX_VALUE)) {
                fastpath();
            } else
            if (n > 0) {
                if (BackpressureHelper.add(this, n) == 0L) {
                    slowpath(n);
                }
            } else {
                RxJavaPlugins.onError(new IllegalArgumentException("request > 0 required but it was " + n));
            }
        }
        
        void fastpath() {
            final long e = end + 1L;
            final Subscriber<? super Integer> actual = this.actual;
            for (long i = index; i != e; i++) {
                if (cancelled) {
                    return;
                }
                actual.onNext((int)i);
            }
            if (!cancelled) {
                actual.onComplete();
            }
        }
        
        void slowpath(long r) {
            long idx = index;
            
            long e = 0L;
            
            for (;;) {
                long fs = end - idx + 1;
                final boolean complete = fs <= r;

                fs = Math.min(fs, r) + idx;
                final Subscriber<? super Integer> o = this.actual;
                
                for (long i = idx; i != fs; i++) {
                    if (cancelled) {
                        return;
                    }
                    o.onNext((int) i);
                }
                
                if (complete) {
                    if (!cancelled) {
                        o.onComplete();
                    }
                    return;
                }
                
                e -= fs - idx;

                idx = fs;

                r = get() + e;
                
                if (r == 0L) {
                    index = fs;
                    r = addAndGet(e);
                    if (r == 0L) {
                        return;
                    }
                    e = 0L;
                }
            }
        }
        
        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
