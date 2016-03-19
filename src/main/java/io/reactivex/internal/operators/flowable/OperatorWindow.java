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

import java.util.ArrayDeque;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.Flowable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.processors.UnicastProcessor;

public final class OperatorWindow<T> implements Operator<Flowable<T>, T> {
    final long count;
    final long skip;
    final int capacityHint;
    
    public OperatorWindow(long count, long skip, int capacityHint) {
        this.count = count;
        this.skip = skip;
        this.capacityHint = capacityHint;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super Flowable<T>> t) {
        if (count == skip) {
            return new WindowExactSubscriber<T>(t, count, capacityHint);
        }
        return new WindowSkipSubscriber<T>(t, count, skip, capacityHint);
    }
    
    static final class WindowExactSubscriber<T>
    extends AtomicInteger
    implements Subscriber<T>, Subscription, Runnable {
        /** */
        private static final long serialVersionUID = -7481782523886138128L;
        final Subscriber<? super Flowable<T>> actual;
        final long count;
        final int capacityHint;
        
        long size;
        
        Subscription s;
        
        UnicastProcessor<T> window;
        
        volatile boolean cancelled;
        
        public WindowExactSubscriber(Subscriber<? super Flowable<T>> actual, long count, int capacityHint) {
            this.actual = actual;
            this.count = count;
            this.capacityHint = capacityHint;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            
            this.s = s;
            
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            UnicastProcessor<T> w = window;
            if (w == null && !cancelled) {
                w = UnicastProcessor.create(capacityHint, this);
                window = w;
                actual.onNext(w);
            }

            w.onNext(t);
            if (++size >= count) {
                size = 0;
                window = null;
                w.onComplete();
                if (cancelled) {
                    s.cancel();
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            UnicastProcessor<T> w = window;
            if (w != null) {
                window = null;
                w.onError(t);
            }
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            UnicastProcessor<T> w = window;
            if (w != null) {
                window = null;
                w.onComplete();
            }
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            
            long m = BackpressureHelper.multiplyCap(n, count);
            s.request(m);
        }
        
        @Override
        public void cancel() {
            cancelled = true;
        }
        
        @Override
        public void run() {
            if (cancelled) {
                s.cancel();
            }
        }
    }
    
    static final class WindowSkipSubscriber<T> extends AtomicBoolean 
    implements Subscriber<T>, Subscription, Runnable {
        /** */
        private static final long serialVersionUID = 3366976432059579510L;
        final Subscriber<? super Flowable<T>> actual;
        final long count;
        final long skip;
        final int capacityHint;
        final ArrayDeque<UnicastProcessor<T>> windows;
        
        long index;
        
        volatile boolean cancelled;
        
        /** Counts how many elements were emitted to the very first window in windows. */
        long firstEmission;
        
        Subscription s;
        
        final AtomicInteger wip = new AtomicInteger();
        
        public WindowSkipSubscriber(Subscriber<? super Flowable<T>> actual, long count, long skip, int capacityHint) {
            this.actual = actual;
            this.count = count;
            this.skip = skip;
            this.capacityHint = capacityHint;
            this.windows = new ArrayDeque<UnicastProcessor<T>>();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            
            this.s = s;
            
            actual.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            final ArrayDeque<UnicastProcessor<T>> ws = windows;
            
            long i = index;
            
            long s = skip;
            
            if (i % s == 0 && !cancelled) {
                wip.getAndIncrement();
                UnicastProcessor<T> w = UnicastProcessor.create(capacityHint, this);
                ws.offer(w);
                actual.onNext(w);
            }

            long c = firstEmission + 1;
            
            for (UnicastProcessor<T> w : ws) {
                w.onNext(t);
            }
            
            if (c >= count) {
                ws.poll().onComplete();
                if (ws.isEmpty() && cancelled) {
                    this.s.cancel();
                    return;
                }
                firstEmission = c - s;
            } else {
                firstEmission = c;
            }
            
            index = i + 1;
        }
        
        @Override
        public void onError(Throwable t) {
            final ArrayDeque<UnicastProcessor<T>> ws = windows;
            while (!ws.isEmpty()) {
                ws.poll().onError(t);
            }
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            final ArrayDeque<UnicastProcessor<T>> ws = windows;
            while (!ws.isEmpty()) {
                ws.poll().onComplete();
            }
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
         // requesting the first set of buffers must happen only once
            if (!get() && compareAndSet(false, true)) {
                
                if (count < skip) {
                    // don't request the first gap after n buffers
                    long m = BackpressureHelper.multiplyCap(n, count);
                    s.request(m);
                } else {
                    // request 1 full and n - 1 skip gaps
                    long m = BackpressureHelper.multiplyCap(n - 1, skip);
                    long k = BackpressureHelper.addCap(count, m);
                    s.request(k);
                }
                
            } else {
                
                if (count < skip) {
                    // since this isn't the first, request n buffers and n gaps
                    long m = BackpressureHelper.multiplyCap(n, count + skip);
                    s.request(m);
                } else {
                    // request the remaining n * skip
                    long m = BackpressureHelper.multiplyCap(n, skip);
                    s.request(m);
                }
            }
        }
        
        @Override
        public void cancel() {
            cancelled = true;
        }
        
        @Override
        public void run() {
            if (wip.decrementAndGet() == 0) {
                if (cancelled) {
                    s.cancel();
                }
            }
        }
    }
}
