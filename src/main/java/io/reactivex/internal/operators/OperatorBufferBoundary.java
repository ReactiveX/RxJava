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

package io.reactivex.internal.operators;

import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SetCompositeResource;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorBufferBoundary<T, U extends Collection<? super T>, Open, Close> implements Operator<U, T> {
    final Supplier<U> bufferSupplier;
    final Publisher<? extends Open> bufferOpen;
    final Function<? super Open, ? extends Publisher<? extends Close>> bufferClose;

    public OperatorBufferBoundary(Publisher<? extends Open> bufferOpen,
            Function<? super Open, ? extends Publisher<? extends Close>> bufferClose, Supplier<U> bufferSupplier) {
        this.bufferOpen = bufferOpen;
        this.bufferClose = bufferClose;
        this.bufferSupplier = bufferSupplier;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super U> t) {
        return new BufferBoundarySubscriber<>(
                new SerializedSubscriber<>(t),
                bufferOpen, bufferClose, bufferSupplier
                );
    }
    
    static final class BufferBoundarySubscriber<T, U extends Collection<? super T>, Open, Close>
    extends QueueDrainSubscriber<T, U, U> implements Subscription, Disposable {
        final Publisher<? extends Open> bufferOpen;
        final Function<? super Open, ? extends Publisher<? extends Close>> bufferClose;
        final Supplier<U> bufferSupplier;
        final SetCompositeResource<Disposable> resources;
        
        Subscription s;
        
        final List<U> buffers;
        
        volatile int windows;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<BufferBoundarySubscriber> WINDOWS =
                AtomicIntegerFieldUpdater.newUpdater(BufferBoundarySubscriber.class, "windows");

        public BufferBoundarySubscriber(Subscriber<? super U> actual, 
                Publisher<? extends Open> bufferOpen,
                Function<? super Open, ? extends Publisher<? extends Close>> bufferClose,
                Supplier<U> bufferSupplier) {
            super(actual, new MpscLinkedQueue<>());
            this.bufferOpen = bufferOpen;
            this.bufferClose = bufferClose;
            this.bufferSupplier = bufferSupplier;
            this.buffers = new LinkedList<>();
            this.resources = new SetCompositeResource<>(Disposable::dispose);
        }
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            BufferOpenSubscriber<T, U, Open, Close> bos = new BufferOpenSubscriber<>(this);
            resources.add(bos);

            actual.onSubscribe(this);
            
            WINDOWS.lazySet(this, 1);
            bufferOpen.subscribe(bos);
            
            s.request(Long.MAX_VALUE);
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
            cancel();
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
                drainMaxLoop(q, actual, false, this);
            }
        }
        
        @Override
        public void request(long n) {
            requested(n);
        }
        
        @Override
        public void dispose() {
            resources.dispose();
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                dispose();
            }
        }
        
        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            a.onNext(v);
            return true;
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

            Publisher<? extends Close> p;
            
            try {
                p = bufferClose.apply(window);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            
            if (p == null) {
                onError(new NullPointerException("The buffer closing publisher is null"));
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
                fastpathOrderedEmitMax(b, false, this);
            }
            
            if (resources.remove(d)) {
                if (WINDOWS.decrementAndGet(this) == 0) {
                    complete();
                }
            }
        }
    }
    
    static final class BufferOpenSubscriber<T, U extends Collection<? super T>, Open, Close>
    extends DisposableSubscriber<Open> {
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
    extends DisposableSubscriber<Close> {
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
