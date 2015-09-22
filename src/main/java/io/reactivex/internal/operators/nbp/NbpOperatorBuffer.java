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

package io.reactivex.internal.operators.nbp;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.subscribers.nbp.NbpEmptySubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorBuffer<T, U extends Collection<? super T>> implements NbpOperator<U, T> {
    final int count;
    final int skip;
    final Supplier<U> bufferSupplier;
    
    public NbpOperatorBuffer(int count, int skip, Supplier<U> bufferSupplier) {
        this.count = count;
        this.skip = skip;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super U> t) {
        if (skip == count) {
            BufferExactSubscriber<T, U> bes = new BufferExactSubscriber<>(t, count, bufferSupplier);
            if (bes.createBuffer()) {
                return bes;
            }
            return NbpEmptySubscriber.INSTANCE;
        }
        return new BufferSkipSubscriber<>(t, count, skip, bufferSupplier);
    }
    
    static final class BufferExactSubscriber<T, U extends Collection<? super T>> implements NbpSubscriber<T> {
        final NbpSubscriber<? super U> actual;
        final int count;
        final Supplier<U> bufferSupplier;
        U buffer;
        
        int size;
        
        Disposable s;

        public BufferExactSubscriber(NbpSubscriber<? super U> actual, int count, Supplier<U> bufferSupplier) {
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
                    EmptyDisposable.error(t, actual);
                } else {
                    s.dispose();
                    actual.onError(t);
                }
                return false;
            }
            
            buffer = b;
            if (b == null) {
                Throwable t = new NullPointerException("Empty buffer supplied");
                if (s == null) {
                    EmptyDisposable.error(t, actual);
                } else {
                    s.dispose();
                    actual.onError(t);
                }
                return false;
            }
            
            return true;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(s);
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
    }
    
    static final class BufferSkipSubscriber<T, U extends Collection<? super T>> 
    extends AtomicBoolean implements NbpSubscriber<T> {
        /** */
        private static final long serialVersionUID = -8223395059921494546L;
        final NbpSubscriber<? super U> actual;
        final int count;
        final int skip;
        final Supplier<U> bufferSupplier;

        Disposable s;
        
        final ArrayDeque<U> buffers;
        
        long index;

        public BufferSkipSubscriber(NbpSubscriber<? super U> actual, int count, int skip, Supplier<U> bufferSupplier) {
            this.actual = actual;
            this.count = count;
            this.skip = skip;
            this.bufferSupplier = bufferSupplier;
            this.buffers = new ArrayDeque<>();
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            if (index++ % skip == 0) {
                U b;
                
                try {
                    b = bufferSupplier.get();
                } catch (Throwable e) {
                    buffers.clear();
                    s.dispose();
                    actual.onError(e);
                    return;
                }
                
                if (b == null) {
                    buffers.clear();
                    s.dispose();
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
    }
}
