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

import java.util.Iterator;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

public final class PublisherIterableSource<T> extends AtomicBoolean implements Publisher<T> {
    /** */
    private static final long serialVersionUID = 9051303031779816842L;
    
    final Iterable<? extends T> source;
    public PublisherIterableSource(Iterable<? extends T> source) {
        this.source = source;
    }
    
    @Override
    public void subscribe(Subscriber<? super T> s) {
        Iterator<? extends T> it;
        try {
            it = source.iterator();
        } catch (Throwable e) {
            EmptySubscription.error(e, s);
            return;
        }
        boolean hasNext;
        try {
            hasNext = it.hasNext();
        } catch (Throwable e) {
            EmptySubscription.error(e, s);
            return;
        }
        if (!hasNext) {
            EmptySubscription.complete(s);
            return;
        }
        s.onSubscribe(new IteratorSourceSubscription<T>(it, s));
    }
    
    static final class IteratorSourceSubscription<T> extends AtomicLong implements Subscription {
        /** */
        private static final long serialVersionUID = 8931425802102883003L;
        final Iterator<? extends T> it;
        final Subscriber<? super T> subscriber;
        
        volatile boolean cancelled;
        
        public IteratorSourceSubscription(Iterator<? extends T> it, Subscriber<? super T> subscriber) {
            this.it = it;
            this.subscriber = subscriber;
        }
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            if (BackpressureHelper.add(this, n) != 0L) {
                return;
            }
            long r = n;
            long r0 = n;
            final Subscriber<? super T> subscriber = this.subscriber;
            final Iterator<? extends T> it = this.it;
            for (;;) {
                if (cancelled) {
                    return;
                }

                long e = 0L;
                while (r != 0L) {
                    T v;
                    try {
                        v = it.next();
                    } catch (Throwable ex) {
                        subscriber.onError(ex);
                        return;
                    }
                    
                    if (v == null) {
                        subscriber.onError(new NullPointerException("Iterator returned a null element"));
                        return;
                    }
                    
                    subscriber.onNext(v);
                    
                    if (cancelled) {
                        return;
                    }
                    
                    boolean hasNext;
                    try {
                        hasNext = it.hasNext();
                    } catch (Throwable ex) {
                        subscriber.onError(ex);
                        return;
                    }
                    if (!hasNext) {
                        subscriber.onComplete();
                        return;
                    }
                    
                    r--;
                    e--;
                }
                if (e != 0L && r0 != Long.MAX_VALUE) {
                    r = addAndGet(e);
                }
                if (r == 0L) {
                    break;
                }
            }
        }
        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
