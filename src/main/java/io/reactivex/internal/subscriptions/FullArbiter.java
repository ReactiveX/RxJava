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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.ObjectHelper;
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

    volatile Subscription s;
    static final Subscription INITIAL = new InitialSubscription();


    Disposable resource;

    volatile boolean cancelled;

    static final Object REQUEST = new Object();

    public FullArbiter(Subscriber<? super T> actual, Disposable resource, int capacity) {
        this.actual = actual;
        this.resource = resource;
        this.queue = new SpscLinkedArrayQueue<Object>(capacity);
        this.s = INITIAL;
    }

    @Override
    public void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            BackpressureHelper.add(missedRequested, n);
            queue.offer(REQUEST, REQUEST);
            drain();
        }
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
            if (s != null) {
                s.cancel();
            }
            return false;
        }

        ObjectHelper.requireNonNull(s, "s is null");
        queue.offer(this.s, NotificationLite.subscription(s));
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
        if (wip.getAndIncrement() != 0) {
            return;
        }

        int missed = 1;

        final SpscLinkedArrayQueue<Object> q = queue;
        final Subscriber<? super T> a = actual;

        for (;;) {

            for (;;) {

                Object o = q.poll();
                if (o == null) {
                    break;
                }
                Object v = q.poll();

                if (o == REQUEST) {
                    long mr = missedRequested.getAndSet(0L);
                    if (mr != 0L) {
                        requested = BackpressureHelper.addCap(requested, mr);
                        s.request(mr);
                    }
                } else
                if (o == s) {
                    if (NotificationLite.isSubscription(v)) {
                        Subscription next = NotificationLite.getSubscription(v);
                        if (!cancelled) {
                            s = next;
                            long r = requested;
                            if (r != 0L) {
                                next.request(r);
                            }
                        } else {
                            next.cancel();
                        }
                    } else if (NotificationLite.isError(v)) {
                        q.clear();
                        dispose();

                        Throwable ex = NotificationLite.getError(v);
                        if (!cancelled) {
                            cancelled = true;
                            a.onError(ex);
                        } else {
                            RxJavaPlugins.onError(ex);
                        }
                    } else if (NotificationLite.isComplete(v)) {
                        q.clear();
                        dispose();

                        if (!cancelled) {
                            cancelled = true;
                            a.onComplete();
                        }
                    } else {
                        long r = requested;
                        if (r != 0) {
                            a.onNext(NotificationLite.<T>getValue(v));
                            requested = r - 1;
                        }
                    }
                }
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    static final class InitialSubscription implements Subscription {
        @Override
        public void request(long n) {
            // deliberately no op
        }

        @Override
        public void cancel() {
            // deliberately no op
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
    final AtomicInteger wip = new AtomicInteger();
}

/** Pads the wip counter away. */
class FullArbiterPad1 extends FullArbiterWip {
    volatile long p1b, p2b, p3b, p4b, p5b, p6b, p7b;
    volatile long p8b, p9b, p10b, p11b, p12b, p13b, p14b, p15b;
}

/** The missed request counter. */
class FullArbiterMissed extends FullArbiterPad1 {
    final AtomicLong missedRequested = new AtomicLong();
}

/** Pads the missed request counter away. */
class FullArbiterPad2 extends FullArbiterMissed {
    volatile long p1c, p2c, p3c, p4c, p5c, p6c, p7c;
    volatile long p8c, p9c, p10c, p11c, p12c, p13c, p14c, p15c;
}
