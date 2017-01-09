/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
 * Copyright (c) 2016-present, RxJava Contributors.
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

import java.util.concurrent.atomic.*;

import org.reactivestreams.Subscription;

import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Arbitrates requests and cancellation between Subscriptions.
 */
public class SubscriptionArbiter extends AtomicInteger implements Subscription {

    private static final long serialVersionUID = -2189523197179400958L;

    /**
     * The current subscription which may null if no Subscriptions have been set.
     */
    Subscription actual;

    /**
     * The current outstanding request amount.
     */
    long requested;

    final AtomicReference<Subscription> missedSubscription;

    final AtomicLong missedRequested;

    final AtomicLong missedProduced;

    volatile boolean cancelled;

    protected boolean unbounded;

    public SubscriptionArbiter() {
        missedSubscription = new AtomicReference<Subscription>();
        missedRequested = new AtomicLong();
        missedProduced = new AtomicLong();
    }

    /**
     * Atomically sets a new subscription.
     * @param s the subscription to set, not null (verified)
     */
    public final void setSubscription(Subscription s) {
        if (cancelled) {
            s.cancel();
            return;
        }

        ObjectHelper.requireNonNull(s, "s is null");

        if (get() == 0 && compareAndSet(0, 1)) {
            Subscription a = actual;

            if (a != null) {
                a.cancel();
            }

            actual = s;

            long r = requested;

            if (decrementAndGet() != 0) {
                drainLoop();
            }

            if (r != 0L) {
                s.request(r);
            }

            return;
        }

        Subscription a = missedSubscription.getAndSet(s);
        if (a != null) {
            a.cancel();
        }
        drain();
    }

    @Override
    public final void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            if (unbounded) {
                return;
            }
            if (get() == 0 && compareAndSet(0, 1)) {
                long r = requested;

                if (r != Long.MAX_VALUE) {
                    r = BackpressureHelper.addCap(r, n);
                    requested = r;
                    if (r == Long.MAX_VALUE) {
                        unbounded = true;
                    }
                }
                Subscription a = actual;

                if (decrementAndGet() != 0) {
                    drainLoop();
                }

                if (a != null) {
                    a.request(n);
                }

                return;
            }

            BackpressureHelper.add(missedRequested, n);

            drain();
        }
    }

    public final void produced(long n) {
        if (unbounded) {
            return;
        }
        if (get() == 0 && compareAndSet(0, 1)) {
            long r = requested;

            if (r != Long.MAX_VALUE) {
                long u = r - n;
                if (u < 0L) {
                    SubscriptionHelper.reportMoreProduced(u);
                    u = 0;
                }
                requested = u;
            }

            if (decrementAndGet() == 0) {
                return;
            }

            drainLoop();

            return;
        }

        BackpressureHelper.add(missedProduced, n);

        drain();
    }

    @Override
    public void cancel() {
        if (!cancelled) {
            cancelled = true;

            drain();
        }
    }

    final void drain() {
        if (getAndIncrement() != 0) {
            return;
        }
        drainLoop();
    }

    final void drainLoop() {
        int missed = 1;

        long requestAmount = 0L;
        Subscription requestTarget = null;

        for (; ; ) {

            Subscription ms = missedSubscription.get();

            if (ms != null) {
                ms = missedSubscription.getAndSet(null);
            }

            long mr = missedRequested.get();
            if (mr != 0L) {
                mr = missedRequested.getAndSet(0L);
            }

            long mp = missedProduced.get();
            if (mp != 0L) {
                mp = missedProduced.getAndSet(0L);
            }

            Subscription a = actual;

            if (cancelled) {
                if (a != null) {
                    a.cancel();
                    actual = null;
                }
                if (ms != null) {
                    ms.cancel();
                }
            } else {
                long r = requested;
                if (r != Long.MAX_VALUE) {
                    long u = BackpressureHelper.addCap(r, mr);

                    if (u != Long.MAX_VALUE) {
                        long v = u - mp;
                        if (v < 0L) {
                            SubscriptionHelper.reportMoreProduced(v);
                            v = 0;
                        }
                        r = v;
                    } else {
                        r = u;
                    }
                    requested = r;
                }

                if (ms != null) {
                    if (a != null) {
                        a.cancel();
                    }
                    actual = ms;
                    if (r != 0L) {
                        requestAmount = BackpressureHelper.addCap(requestAmount, r);
                        requestTarget = ms;
                    }
                } else if (a != null && mr != 0L) {
                    requestAmount = BackpressureHelper.addCap(requestAmount, mr);
                    requestTarget = a;
                }
            }

            missed = addAndGet(-missed);
            if (missed == 0) {
                if (requestAmount != 0L) {
                    requestTarget.request(requestAmount);
                }
                return;
            }
        }
    }

    /**
     * Returns true if the arbiter runs in unbounded mode.
     * @return true if the arbiter runs in unbounded mode
     */
    public final boolean isUnbounded() {
        return unbounded;
    }

    /**
     * Returns true if the arbiter has been cancelled.
     * @return true if the arbiter has been cancelled
     */
    public final boolean isCancelled() {
        return cancelled;
    }
}
