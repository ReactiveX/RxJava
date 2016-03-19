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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.subscribers.flowable.EmptySubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

public final class OperatorBuffer<T, U extends Collection<? super T>> implements Operator<U, T> {
    final int count;
    final int skip;
    final Supplier<U> bufferSupplier;
    
    public OperatorBuffer(int count, int skip, Supplier<U> bufferSupplier) {
        this.count = count;
        this.skip = skip;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super U> t) {
        if (skip == count) {
            BufferExactSubscriber<T, U> bes = new BufferExactSubscriber<T, U>(t, count, bufferSupplier);
            if (bes.createBuffer()) {
                return bes;
            }
            return EmptySubscriber.INSTANCE;
        }
        return new BufferSkipSubscriber<T, U>(t, count, skip, bufferSupplier);
    }
    
    static final class BufferExactSubscriber<T, U extends Collection<? super T>> implements Subscriber<T>, Subscription {
        final Subscriber<? super U> actual;
        final int count;
        final Supplier<U> bufferSupplier;
        U buffer;
        
        int size;
        
        Subscription s;

        public BufferExactSubscriber(Subscriber<? super U> actual, int count, Supplier<U> bufferSupplier) {
            this.actual = actual;
            this.count = count;
            this.bufferSupplier = bufferSupplier;
        }
        
        boolean createBuffer() {
            U b;
            try {
                b = bufferSupplier.get();
            } catch (Throwable t) {
                buffer = null;
                if (s == null) {
                    EmptySubscription.error(t, actual);
                } else {
                    s.cancel();
                    actual.onError(t);
                }
                return false;
            }
            
            buffer = b;
            if (b == null) {
                Throwable t = new NullPointerException("Empty buffer supplied");
                if (s == null) {
                    EmptySubscription.error(t, actual);
                } else {
                    s.cancel();
                    actual.onError(t);
                }
                return false;
            }
            
            return true;
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
            U b = buffer;
            if (b == null) {
                return;
            }
            
            b.add(t);
            
            if (++size >= count) {
                actual.onNext(b);
                
                size = 0;
                createBuffer();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            buffer = null;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            U b = buffer;
            buffer = null;
            if (b != null && !b.isEmpty()) {
                actual.onNext(b);
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
            s.cancel();
        }
    }
    
    static final class BufferSkipSubscriber<T, U extends Collection<? super T>> extends AtomicBoolean implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -8223395059921494546L;
        final Subscriber<? super U> actual;
        final int count;
        final int skip;
        final Supplier<U> bufferSupplier;

        Subscription s;
        
        final ArrayDeque<U> buffers;
        
        long index;

        public BufferSkipSubscriber(Subscriber<? super U> actual, int count, int skip, Supplier<U> bufferSupplier) {
            this.actual = actual;
            this.count = count;
            this.skip = skip;
            this.bufferSupplier = bufferSupplier;
            this.buffers = new ArrayDeque<U>();
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
            if (index++ % skip == 0) {
                U b;
                
                try {
                    b = bufferSupplier.get();
                } catch (Throwable e) {
                    buffers.clear();
                    s.cancel();
                    actual.onError(e);
                    return;
                }
                
                if (b == null) {
                    buffers.clear();
                    s.cancel();
                    actual.onError(new NullPointerException());
                    return;
                }
                
                buffers.offer(b);
            }
            
            Iterator<U> it = buffers.iterator();
            while (it.hasNext()) {
                U b = it.next();
                b.add(t);
                if (count <= b.size()) {
                    it.remove();
                    
                    actual.onNext(b);
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            buffers.clear();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            while (!buffers.isEmpty()) {
                actual.onNext(buffers.poll());
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
            s.cancel();
        }
    }
}
