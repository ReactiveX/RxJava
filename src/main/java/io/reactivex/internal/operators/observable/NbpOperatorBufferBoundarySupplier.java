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

package io.reactivex.internal.operators.observable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.observable.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.QueueDrainHelper;
import io.reactivex.observers.SerializedObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorBufferBoundarySupplier<T, U extends Collection<? super T>, B> implements NbpOperator<U, T> {
    final Supplier<? extends Observable<B>> boundarySupplier;
    final Supplier<U> bufferSupplier;
    
    public NbpOperatorBufferBoundarySupplier(Supplier<? extends Observable<B>> boundarySupplier, Supplier<U> bufferSupplier) {
        this.boundarySupplier = boundarySupplier;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    public Observer<? super T> apply(Observer<? super U> t) {
        return new BufferBondarySupplierSubscriber<T, U, B>(new SerializedObserver<U>(t), bufferSupplier, boundarySupplier);
    }
    
    static final class BufferBondarySupplierSubscriber<T, U extends Collection<? super T>, B>
    extends NbpQueueDrainSubscriber<T, U, U> implements Observer<T>, Disposable {
        /** */
        final Supplier<U> bufferSupplier;
        final Supplier<? extends Observable<B>> boundarySupplier;
        
        Disposable s;
        
        final AtomicReference<Disposable> other = new AtomicReference<Disposable>();
        
        static final Disposable DISPOSED = new Disposable() {
            @Override
            public void dispose() { }
        };
        
        U buffer;
        
        public BufferBondarySupplierSubscriber(Observer<? super U> actual, Supplier<U> bufferSupplier,
                Supplier<? extends Observable<B>> boundarySupplier) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.boundarySupplier = boundarySupplier;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            Observer<? super U> actual = this.actual;
            
            U b;
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                cancelled = true;
                s.dispose();
                EmptyDisposable.error(e, actual);
                return;
            }
            
            if (b == null) {
                cancelled = true;
                s.dispose();
                EmptyDisposable.error(new NullPointerException("The buffer supplied is null"), actual);
                return;
            }
            buffer = b;
            
            Observable<B> boundary;
            
            try {
                boundary = boundarySupplier.get();
            } catch (Throwable ex) {
                cancelled = true;
                s.dispose();
                EmptyDisposable.error(ex, actual);
                return;
            }
            
            if (boundary == null) {
                cancelled = true;
                s.dispose();
                EmptyDisposable.error(new NullPointerException("The boundary publisher supplied is null"), actual);
                return;
            }
            
            BufferBoundarySubscriber<T, U, B> bs = new BufferBoundarySubscriber<T, U, B>(this);
            other.set(bs);
            
            actual.onSubscribe(this);
            
            if (!cancelled) {
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
            dispose();
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
                QueueDrainHelper.drainLoop(queue, actual, false, this, this);
            }
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                s.dispose();
                disposeOther();
                
                if (enter()) {
                    queue.clear();
                }
            }
        }
        
        void disposeOther() {
            Disposable d = other.get();
            if (d != DISPOSED) {
                d = other.getAndSet(DISPOSED);
                if (d != DISPOSED && d != null) {
                    d.dispose();
                }
            }
        }
        
        void next() {
            
            Disposable o = other.get();
            
            U next;
            
            try {
                next = bufferSupplier.get();
            } catch (Throwable e) {
                dispose();
                actual.onError(e);
                return;
            }
            
            if (next == null) {
                dispose();
                actual.onError(new NullPointerException("The buffer supplied is null"));
                return;
            }
            
            Observable<B> boundary;
            
            try {
                boundary = boundarySupplier.get();
            } catch (Throwable ex) {
                cancelled = true;
                s.dispose();
                actual.onError(ex);
                return;
            }
            
            if (boundary == null) {
                cancelled = true;
                s.dispose();
                actual.onError(new NullPointerException("The boundary publisher supplied is null"));
                return;
            }
            
            BufferBoundarySubscriber<T, U, B> bs = new BufferBoundarySubscriber<T, U, B>(this);
            
            if (!other.compareAndSet(o, bs)) {
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
            
            boundary.subscribe(bs);
            
            fastpathEmit(b, false, this);
        }
        
        @Override
        public void accept(Observer<? super U> a, U v) {
            actual.onNext(v);
        }
        
    }
    
    static final class BufferBoundarySubscriber<T, U extends Collection<? super T>, B> 
    extends NbpDisposableSubscriber<B> {
        final BufferBondarySupplierSubscriber<T, U, B> parent;
        
        boolean once;
        
        public BufferBoundarySubscriber(BufferBondarySupplierSubscriber<T, U, B> parent) {
            this.parent = parent;
        }

        @Override
        public void onNext(B t) {
            if (once) {
                return;
            }
            once = true;
            dispose();
            parent.next();
        }
        
        @Override
        public void onError(Throwable t) {
            if (once) {
                RxJavaPlugins.onError(t);
                return;
            }
            once = true;
            parent.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (once) {
                return;
            }
            once = true;
            parent.next();
        }
    }
}
