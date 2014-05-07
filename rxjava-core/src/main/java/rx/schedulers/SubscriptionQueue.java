 /**
  * Copyright 2014 Netflix, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
package rx.schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import rx.Subscription;
import rx.exceptions.CompositeException;

/**
 * Array-based queue that grows as neccessary, allows identity-based
 * dequeueing and the contents can be unsubscribed at once.
 * <p>Enqueued and dequeued subscriptions must not be {@code null}.
 * <p>Dequeueing is done based on object identity {@code ==} c
 */
public final class SubscriptionQueue implements Subscription {
    static final int INITIAL_CAPACITY = 8;
    Subscription[] array = new Subscription[INITIAL_CAPACITY];
    volatile boolean isUnsubscribed;
    int head;
    int tail;
    int size;
    /**
     * Enqueue a subscription.
     * @param s the subscription to enqueue, mustn't be {@code null}
     */
    public void add(Subscription s) {
        synchronized (this) {
            if (!isUnsubscribed) {
                Subscription[] a = array;
                int n = a.length;
                int p = head;
                int t = tail;
                int r = n - p;
                a[t] = s;
                tail = (t + 1) & (n - 1);
                size++;
                if (head == tail) {
                    Subscription[] a2 = new Subscription[n << 1];
                    System.arraycopy(a, p, a2, 0, r);
                    System.arraycopy(a, 0, a2, r, p);
                    array = a2;
                    head = 0;
                    tail = n;
                }
                return;
            }
        }
        s.unsubscribe();
    }
    /**
     * Dequeue a subscription. If the subscription is
     * not in this queue, nothing happens.
     * <p>Does not unsubscribe the subscription.
     * @param s the subscription, mustn't be null
     */
    public void remove(Subscription s) {
        synchronized (this) {
            if (!isUnsubscribed) {
                Subscription[] a = array;
                int h = head;
                int k = a.length - 1;
                if (a[h] == s) {
                    a[h] = null;
                    size--;
                    head = (h + 1) & k;
                } else {
                    int i = h;
                    int t = tail;
                    while (i != t && a[i] == null) {
                        i = (i + 1) & k;
                        h = i;
                    }
                    while (i != t) {
                        if (a[i] == s) {
                            a[i] = null;
                            size--;
                            if (i == h) {
                                h = (h + 1) & k;
                            }
                            break;
                        }
                        i = (i + 1) & k;
                    }
                    head = h;
                }
            }
        }
    }
    @Override
    public boolean isUnsubscribed() {
        return isUnsubscribed;
    }
    @Override
    public void unsubscribe() {
        Subscription[] a;
        synchronized (this) {
            if (isUnsubscribed) {
                return;
            }
            isUnsubscribed = true;
            a = array;
            array = null;
            size = 0;
            head = 0;
            tail = 0;
        }
        unsubscribeFromAll(a);
    }
    /**
     * Creates a subscription which dequeues the given
     * subscription from this queue when the unsubscribe is called on it.
     * @param s the subscription to queue, mustn't be null
     * @return the subscription to perform the dequeueing
     */
    public Subscription createDequeuer(Subscription s) {
        return new Dequeuer(s, this);
    }
    /**
     * Dequeues a subscription from a queue when the unsubscribe is called.
     */
    private static final class Dequeuer implements Subscription {
        final Subscription s;
        final SubscriptionQueue sq;
//        final AtomicBoolean once;
        
        public Dequeuer(Subscription s, SubscriptionQueue sq) {
            this.s = s;
            this.sq = sq;
//            this.once = new AtomicBoolean();
        }
        
        @Override
        public boolean isUnsubscribed() {
//            return once.get();
            return s.isUnsubscribed();
        }
        
        @Override
        public void unsubscribe() {
//            if (once.compareAndSet(false, true)) {
//                sq.remove(s);
//            }
            sq.remove(s);
        }
    }
    
    private static void unsubscribeFromAll(Subscription[] subscriptions) {
        final List<Throwable> es = new ArrayList<Throwable>();
        for (Subscription s : subscriptions) {
            if (s != null) {
                try {
                    s.unsubscribe();
                } catch (Throwable e) {
                    es.add(e);
                }
            }
        }
        if (!es.isEmpty()) {
            if (es.size() == 1) {
                Throwable t = es.get(0);
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new CompositeException(
                            "Failed to unsubscribe to 1 or more subscriptions.", es);
                }
            } else {
                throw new CompositeException(
                        "Failed to unsubscribe to 2 or more subscriptions.", es);
            }
        }
    }
}
