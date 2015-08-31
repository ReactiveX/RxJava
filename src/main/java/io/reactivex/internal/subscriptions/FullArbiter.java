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

package io.reactivex.internal.subscriptions;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Performs full arbitration of Subscriber events with strict drain (i.e., old emissions of another
 * subscriber are dropped).
 *
 * @param <T> the value type
 */
public final class FullArbiter<T> extends FullArbiterPad2 implements Subscription {
    final Subscriber<? super T> actual;
    final SpscLinkedArrayQueue<Object> queue;

    long requested;
    Subscription s;
    
    Disposable resource;

    volatile boolean cancelled;

    static final Object REQUEST = new Object();

    public FullArbiter(Subscriber<? super T> actual, Disposable resource, int capacity) {
        this.actual = actual;
        this.resource = resource;
        this.queue = new SpscLinkedArrayQueue<>(capacity);
    }

    @Override
    public void request(long n) {
        if (SubscriptionHelper.validateRequest(n)) {
            return;
        }
        BackpressureHelper.add(MISSED_REQUESTED, this, n);
        queue.offer(REQUEST, REQUEST);
        drain();
    }

    @Override
    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            dispose();
        }
    }
    
    void dispose() {
        Disposable d = resource;
        resource = null;
        if (d != null) {
            d.dispose();
        }
    }

    public boolean setSubscription(Subscription s) {
        if (cancelled) {
            return false;
        }

        queue.offer(s, NotificationLite.subscription(s));
        drain();
        return true;
    }

    public boolean onNext(T value, Subscription s) {
        if (cancelled) {
            return false;
        }

        queue.offer(s, NotificationLite.next(value));
        drain();
        return true;
    }

    public void onError(Throwable value, Subscription s) {
        if (cancelled) {
            RxJavaPlugins.onError(value);
            return;
        }
        queue.offer(s, NotificationLite.error(value));
        drain();
    }

    public void onComplete(Subscription s) {
        queue.offer(s, NotificationLite.complete());
        drain();
    }

    void drain() {
        if (WIP.getAndIncrement(this) != 0) {
            return;
        }
        
        int missed = 1;
        
        final SpscLinkedArrayQueue<Object> q = queue;
        final Subscriber<? super T> a = actual;
        
        for (;;) {
            
            for (;;) {
                Object o = q.peek();
                
                if (o == null) {
                    break;
                }
                
                q.poll();
                Object v = q.poll();
                
                if (o == REQUEST) {
                    long mr = MISSED_REQUESTED.getAndSet(this, 0L);
                    if (mr != 0L) {
                        requested = BackpressureHelper.addCap(requested, mr);
                        if (s != null) {
                            s.request(mr);
                        }
                    }
                } else 
                if (o != s) {
                    continue;
                } else
                if (NotificationLite.isSubscription(v)) {
                    Subscription next = NotificationLite.getSubscription(v);
                    if (s != null) {
                        s.cancel();
                    }
                    s = next;
                    long r = requested;
                    if (r != 0L) {
                        next.request(r);
                    }
                } else 
                if (NotificationLite.isError(v)) {
                    q.clear();
                    dispose();
                    
                    Throwable ex = NotificationLite.getError(v);
                    if (!cancelled) {
                        cancelled = true;
                        a.onError(ex);
                    } else {
                        RxJavaPlugins.onError(ex);
                    }
                } else
                if (NotificationLite.isComplete(v)) {
                    q.clear();
                    dispose();

                    if (!cancelled) {
                        cancelled = true;
                        a.onComplete();
                    }
                } else {
                    long r = requested;
                    if (r != 0) {
                        a.onNext(NotificationLite.getValue(v));
                        requested = r - 1;
                    }
                }
            }
            
            missed = WIP.addAndGet(this, -missed);
            if (missed == 0) {
                break;
            }
        }
    }
}

/** Pads the object header away. */
class FullArbiterPad0 {
    volatile long p1a, p2a, p3a, p4a, p5a, p6a, p7a;
    volatile long p8a, p9a, p10a, p11a, p12a, p13a, p14a, p15a;
}

/** The work-in-progress counter. */
class FullArbiterWip extends FullArbiterPad0 {
    volatile int wip;
    static final AtomicIntegerFieldUpdater<FullArbiterWip> WIP =
    AtomicIntegerFieldUpdater.newUpdater(FullArbiterWip.class, "wip");
}

/** Pads the wip counter away. */
class FullArbiterPad1 extends FullArbiterWip {
    volatile long p1b, p2b, p3b, p4b, p5b, p6b, p7b;
    volatile long p8b, p9b, p10b, p11b, p12b, p13b, p14b, p15b;
}

/** The missed request counter. */
class FullArbiterMissed extends FullArbiterPad1 {
    volatile long missedRequested;
    static final AtomicLongFieldUpdater<FullArbiterMissed> MISSED_REQUESTED =
    AtomicLongFieldUpdater.newUpdater(FullArbiterMissed.class, "missedRequested");
}

/** Pads the missed request counter away. */
class FullArbiterPad2 extends FullArbiterMissed {
    volatile long p1c, p2c, p3c, p4c, p5c, p6c, p7c;
    volatile long p8c, p9c, p10c, p11c, p12c, p13c, p14c, p15c;
}