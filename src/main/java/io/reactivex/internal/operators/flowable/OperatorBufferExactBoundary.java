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

import java.util.Collection;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.flowable.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.QueueDrainHelper;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorBufferExactBoundary<T, U extends Collection<? super T>, B> implements Operator<U, T> {
    final Publisher<B> boundary;
    final Supplier<U> bufferSupplier;
    
    public OperatorBufferExactBoundary(Publisher<B> boundary, Supplier<U> bufferSupplier) {
        this.boundary = boundary;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super U> t) {
        return new BufferExactBondarySubscriber<T, U, B>(new SerializedSubscriber<U>(t), bufferSupplier, boundary);
    }
    
    static final class BufferExactBondarySubscriber<T, U extends Collection<? super T>, B>
    extends QueueDrainSubscriber<T, U, U> implements Subscriber<T>, Subscription, Disposable {
        /** */
        final Supplier<U> bufferSupplier;
        final Publisher<B> boundary;
        
        Subscription s;
        
        Disposable other;
        
        U buffer;
        
        public BufferExactBondarySubscriber(Subscriber<? super U> actual, Supplier<U> bufferSupplier,
                Publisher<B> boundary) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.boundary = boundary;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            U b;
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                cancelled = true;
                s.cancel();
                EmptySubscription.error(e, actual);
                return;
            }
            
            if (b == null) {
                cancelled = true;
                s.cancel();
                EmptySubscription.error(new NullPointerException("The buffer supplied is null"), actual);
                return;
            }
            buffer = b;
            
            BufferBoundarySubscriber<T, U, B> bs = new BufferBoundarySubscriber<T, U, B>(this);
            other = bs;
            
            actual.onSubscribe(this);
            
            if (!cancelled) {
                s.request(Long.MAX_VALUE);
                
                boundary.subscribe(bs);
            }
        }
        
        @Override
        public void onNext(T t) {
            synchronized (this) {
                U b = buffer;
                if (b == null) {
                    return;
                }
                b.add(t);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            cancel();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }
                buffer = null;
            }
            queue.offer(b);
            done = true;
            if (enter()) {
                QueueDrainHelper.drainMaxLoop(queue, actual, false, this, this);
            }
        }
        
        @Override
        public void request(long n) {
            requested(n);
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                other.dispose();
                s.cancel();
                
                if (enter()) {
                    queue.clear();
                }
            }
        }
        
        void next() {
            
            U next;
            
            try {
                next = bufferSupplier.get();
            } catch (Throwable e) {
                cancel();
                actual.onError(e);
                return;
            }
            
            if (next == null) {
                cancel();
                actual.onError(new NullPointerException("The buffer supplied is null"));
                return;
            }
            
            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }
                buffer = next;
            }
            
            fastpathEmitMax(b, false, this);
        }
        
        @Override
        public void dispose() {
            cancel();
        }
        
        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            actual.onNext(v);
            return true;
        }
        
    }
    
    static final class BufferBoundarySubscriber<T, U extends Collection<? super T>, B> extends DisposableSubscriber<B> {
        final BufferExactBondarySubscriber<T, U, B> parent;
        
        public BufferBoundarySubscriber(BufferExactBondarySubscriber<T, U, B> parent) {
            this.parent = parent;
        }

        @Override
        public void onNext(B t) {
            parent.next();
        }
        
        @Override
        public void onError(Throwable t) {
            parent.onError(t);
        }
        
        @Override
        public void onComplete() {
            parent.onComplete();
        }
    }
}
