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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SetCompositeResource;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.nbp.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorBufferBoundary<T, U extends Collection<? super T>, Open, Close> implements NbpOperator<U, T> {
    final Supplier<U> bufferSupplier;
    final NbpObservable<? extends Open> bufferOpen;
    final Function<? super Open, ? extends NbpObservable<? extends Close>> bufferClose;

    public NbpOperatorBufferBoundary(NbpObservable<? extends Open> bufferOpen,
            Function<? super Open, ? extends NbpObservable<? extends Close>> bufferClose, Supplier<U> bufferSupplier) {
        this.bufferOpen = bufferOpen;
        this.bufferClose = bufferClose;
        this.bufferSupplier = bufferSupplier;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super U> t) {
        return new BufferBoundarySubscriber<>(
                new NbpSerializedSubscriber<>(t),
                bufferOpen, bufferClose, bufferSupplier
                );
    }
    
    static final class BufferBoundarySubscriber<T, U extends Collection<? super T>, Open, Close>
    extends NbpQueueDrainSubscriber<T, U, U> implements Disposable {
        final NbpObservable<? extends Open> bufferOpen;
        final Function<? super Open, ? extends NbpObservable<? extends Close>> bufferClose;
        final Supplier<U> bufferSupplier;
        final SetCompositeResource<Disposable> resources;
        
        Disposable s;
        
        final List<U> buffers;
        
        volatile int windows;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<BufferBoundarySubscriber> WINDOWS =
                AtomicIntegerFieldUpdater.newUpdater(BufferBoundarySubscriber.class, "windows");

        public BufferBoundarySubscriber(NbpSubscriber<? super U> actual, 
                NbpObservable<? extends Open> bufferOpen,
                Function<? super Open, ? extends NbpObservable<? extends Close>> bufferClose,
                Supplier<U> bufferSupplier) {
            super(actual, new MpscLinkedQueue<>());
            this.bufferOpen = bufferOpen;
            this.bufferClose = bufferClose;
            this.bufferSupplier = bufferSupplier;
            this.buffers = new LinkedList<>();
            this.resources = new SetCompositeResource<>(Disposable::dispose);
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            BufferOpenSubscriber<T, U, Open, Close> bos = new BufferOpenSubscriber<>(this);
            resources.add(bos);

            actual.onSubscribe(this);
            
            WINDOWS.lazySet(this, 1);
            bufferOpen.subscribe(bos);
        }
        
        @Override
        public void onNext(T t) {
            synchronized (t) {
                for (U b : buffers) {
                    b.add(t);
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            dispose();
            cancelled = true;
            synchronized (this) {
                buffers.clear();
            }
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (WINDOWS.decrementAndGet(this) == 0) {
                complete();
            }
        }
        
        void complete() {
            List<U> list;
            synchronized (this) {
                list = new ArrayList<>(buffers);
                buffers.clear();
            }
            
            Queue<U> q = queue;
            for (U u : list) {
                q.offer(u);
            }
            done = true;
            if (enter()) {
                drainLoop(q, actual, false, this);
            }
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                resources.dispose();
            }
        }
        
        @Override
        public void accept(NbpSubscriber<? super U> a, U v) {
            a.onNext(v);
        }
        
        void open(Open window) {
            if (cancelled) {
                return;
            }
            
            U b;
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                onError(e);
                return;
            }
            
            if (b == null) {
                onError(new NullPointerException("The buffer supplied is null"));
                return;
            }

            NbpObservable<? extends Close> p;
            
            try {
                p = bufferClose.apply(window);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            
            if (p == null) {
                onError(new NullPointerException("The buffer closing Observable is null"));
                return;
            }
            
            if (cancelled) {
                return;
            }

            synchronized (this) {
                if (cancelled) {
                    return;
                }
                buffers.add(b);
            }
            
            BufferCloseSubscriber<T, U, Open, Close> bcs = new BufferCloseSubscriber<>(b, this);
            resources.add(bcs);
            
            WINDOWS.getAndIncrement(this);
            
            p.subscribe(bcs);
        }
        
        void openFinished(Disposable d) {
            if (resources.remove(d)) {
                if (WINDOWS.decrementAndGet(this) == 0) {
                    complete();
                }
            }
        }
        
        void close(U b, Disposable d) {
            
            boolean e;
            synchronized (this) {
                e = buffers.remove(b);
            }
            
            if (e) {
                fastpathOrderedEmit(b, false, this);
            }
            
            if (resources.remove(d)) {
                if (WINDOWS.decrementAndGet(this) == 0) {
                    complete();
                }
            }
        }
    }
    
    static final class BufferOpenSubscriber<T, U extends Collection<? super T>, Open, Close>
    extends NbpDisposableSubscriber<Open> {
        final BufferBoundarySubscriber<T, U, Open, Close> parent;
        
        boolean done;
        
        public BufferOpenSubscriber(BufferBoundarySubscriber<T, U, Open, Close> parent) {
            this.parent = parent;
        }
        @Override
        public void onNext(Open t) {
            if (done) {
                return;
            }
            parent.open(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            parent.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.openFinished(this);
        }
    }
    
    static final class BufferCloseSubscriber<T, U extends Collection<? super T>, Open, Close>
    extends NbpDisposableSubscriber<Close> {
        final BufferBoundarySubscriber<T, U, Open, Close> parent;
        final U value;
        boolean done;
        public BufferCloseSubscriber(U value, BufferBoundarySubscriber<T, U, Open, Close> parent) {
            this.parent = parent;
            this.value = value;
        }
        
        @Override
        public void onNext(Close t) {
            onComplete();
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            parent.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.close(value, this);
        }
    }
}
