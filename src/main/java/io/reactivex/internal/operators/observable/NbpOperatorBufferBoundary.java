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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;
import io.reactivex.Observable;
import io.reactivex.Observable.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.SetCompositeResource;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.observable.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.QueueDrainHelper;
import io.reactivex.observers.SerializedObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorBufferBoundary<T, U extends Collection<? super T>, Open, Close> implements NbpOperator<U, T> {
    final Supplier<U> bufferSupplier;
    final Observable<? extends Open> bufferOpen;
    final Function<? super Open, ? extends Observable<? extends Close>> bufferClose;

    public NbpOperatorBufferBoundary(Observable<? extends Open> bufferOpen,
            Function<? super Open, ? extends Observable<? extends Close>> bufferClose, Supplier<U> bufferSupplier) {
        this.bufferOpen = bufferOpen;
        this.bufferClose = bufferClose;
        this.bufferSupplier = bufferSupplier;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super U> t) {
        return new BufferBoundarySubscriber<T, U, Open, Close>(
                new SerializedObserver<U>(t),
                bufferOpen, bufferClose, bufferSupplier
                );
    }
    
    static final class BufferBoundarySubscriber<T, U extends Collection<? super T>, Open, Close>
    extends NbpQueueDrainSubscriber<T, U, U> implements Disposable {
        final Observable<? extends Open> bufferOpen;
        final Function<? super Open, ? extends Observable<? extends Close>> bufferClose;
        final Supplier<U> bufferSupplier;
        final SetCompositeResource<Disposable> resources;
        
        Disposable s;
        
        final List<U> buffers;
        
        final AtomicInteger windows = new AtomicInteger();

        public BufferBoundarySubscriber(Observer<? super U> actual, 
                Observable<? extends Open> bufferOpen,
                Function<? super Open, ? extends Observable<? extends Close>> bufferClose,
                Supplier<U> bufferSupplier) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferOpen = bufferOpen;
            this.bufferClose = bufferClose;
            this.bufferSupplier = bufferSupplier;
            this.buffers = new LinkedList<U>();
            this.resources = new SetCompositeResource<Disposable>(Disposables.consumeAndDispose());
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            BufferOpenSubscriber<T, U, Open, Close> bos = new BufferOpenSubscriber<T, U, Open, Close>(this);
            resources.add(bos);

            actual.onSubscribe(this);
            
            windows.lazySet(1);
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
            if (windows.decrementAndGet() == 0) {
                complete();
            }
        }
        
        void complete() {
            List<U> list;
            synchronized (this) {
                list = new ArrayList<U>(buffers);
                buffers.clear();
            }
            
            Queue<U> q = queue;
            for (U u : list) {
                q.offer(u);
            }
            done = true;
            if (enter()) {
                QueueDrainHelper.drainLoop(q, actual, false, this, this);
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
        public void accept(Observer<? super U> a, U v) {
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

            Observable<? extends Close> p;
            
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
            
            BufferCloseSubscriber<T, U, Open, Close> bcs = new BufferCloseSubscriber<T, U, Open, Close>(b, this);
            resources.add(bcs);
            
            windows.getAndIncrement();
            
            p.subscribe(bcs);
        }
        
        void openFinished(Disposable d) {
            if (resources.remove(d)) {
                if (windows.decrementAndGet() == 0) {
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
                if (windows.decrementAndGet() == 0) {
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
