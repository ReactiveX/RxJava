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

package io.reactivex.internal.subscriptions;
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

import java.util.Queue;
import java.util.concurrent.atomic.*;

import org.reactivestreams.Subscription;

import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Arbitrates requests and cancellation between Subscriptions.
 */
public final class SubscriptionArbiter extends AtomicInteger implements Subscription {
    /** */
    private static final long serialVersionUID = -2189523197179400958L;
    
    final Queue<Subscription> missedSubscription = new MpscLinkedQueue<Subscription>();
    
    Subscription actual;
    long requested;
    
    volatile boolean cancelled;

    final AtomicLong missedRequested = new AtomicLong();
    
    final AtomicLong missedProduced = new AtomicLong();

    private long addRequested(long n) {
        long r = requested;
        long u = BackpressureHelper.addCap(r, n);
        requested = u;
        return r;
    }
    
    @Override
    public void request(final long n) {
        if (SubscriptionHelper.validateRequest(n)) {
            return;
        }
        if (cancelled) {
            return;
        }
        
        if (get() == 0 && compareAndSet(0, 1)) {
            addRequested(n);
            Subscription s = actual;
            if (s != null) {
                s.request(n);
            }
            if (decrementAndGet() == 0) {
                return;
            }
        } else {
            BackpressureHelper.add(missedRequested, n);
            if (getAndIncrement() != 0) {
                return;
            }
        }
        int missed = 1;
        for (;;) {
            SubscriptionArbiter.this.drain();
            
            missed = addAndGet(-missed);
            if (missed == 0) {
                return;
            }
        }
    }

    public void produced(final long n) {
        if (n <= 0) {
            RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
            return;
        }
        
        if (get() == 0 && compareAndSet(0, 1)) {
            long r = requested;
            if (r != Long.MAX_VALUE) {
                long u = r - n;
                if (u < 0L) {
                    RxJavaPlugins.onError(new IllegalArgumentException("More produced than requested: " + u));
                    u = 0;
                }
                requested = u;
            }
            if (decrementAndGet() == 0) {
                return;
            }
        } else {
            BackpressureHelper.add(missedProduced, n);
            if (getAndIncrement() != 0) {
                return;
            }
        }
        int missed = 1;
        for (;;) {
            SubscriptionArbiter.this.drain();
            
            missed = addAndGet(-missed);
            if (missed == 0) {
                return;
            }
        }
    }
    
    public void setSubscription(final Subscription s) {
        Objects.requireNonNull(s, "s is null");
        if (cancelled) {
            s.cancel();
            return;
        }
        
        if (get() == 0 && compareAndSet(0, 1)) {
            Subscription a = actual;
            if (a != null) {
                a.cancel();
            }
            actual = s;
            long r = requested;
            if (r != 0L) {
                s.request(r);
            }
            if (decrementAndGet() == 0) {
                return;
            }
        } else {
            missedSubscription.offer(s);
            if (getAndIncrement() != 0) {
                return;
            }
        }
        int missed = 1;
        for (;;) {
            SubscriptionArbiter.this.drain();
            
            missed = addAndGet(-missed);
            if (missed == 0) {
                return;
            }
        }
    }
    
    @Override
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        
        if (get() == 0 && compareAndSet(0, 1)) {
            Subscription a = actual;
            if (a != null) {
                actual = null;
                a.cancel();
            }
            if (decrementAndGet() == 0) {
                return;
            }
        } else {
            if (getAndIncrement() != 0) {
                return;
            }
        }
        int missed = 1;
        for (;;) {
            SubscriptionArbiter.this.drain();
            
            missed = addAndGet(-missed);
            if (missed == 0) {
                return;
            }
        }
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    void drain() {
        long mr = missedRequested.getAndSet(0L);
        long mp = missedProduced.getAndSet(0L);
        Subscription ms = missedSubscription.poll();
        boolean c = cancelled;
        
        long r = requested;
        if (r != Long.MAX_VALUE && !c) {
            long u = r + mr;
            if (u < 0L) {
                r = Long.MAX_VALUE;
                requested = Long.MAX_VALUE;
            } else {
                long v = u - mp;
                if (v < 0L) {
                    RxJavaPlugins.onError(new IllegalStateException("More produced than requested: " + v));
                    v = 0L;
                }
                r = v;
                requested = v;
            }
        }

        Subscription a = actual;
        if (c && a != null) {
            actual = null;
            a.cancel();
        }
        
        if (ms == null) {
            if (a != null && mr != 0L) {
                a.request(mr);
            }
        } else {
            if (c) {
                ms.cancel();
            } else {
                if (a != null) {
                    a.cancel();
                }
                actual = ms;
                if (r != 0L) {
                    ms.request(r);
                }
            }
        }
    }
}
