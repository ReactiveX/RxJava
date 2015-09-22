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

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorConcatMap<T, U> implements NbpOperator<U, T> {
    final Function<? super T, ? extends NbpObservable<? extends U>> mapper;
    final int bufferSize;
    public NbpOperatorConcatMap(Function<? super T, ? extends NbpObservable<? extends U>> mapper, int bufferSize) {
        this.mapper = mapper;
        this.bufferSize = Math.max(8, bufferSize);
    }
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super U> s) {
        NbpSerializedSubscriber<U> ssub = new NbpSerializedSubscriber<>(s);
        SerialDisposable sa = new SerialDisposable();
        ssub.onSubscribe(sa);
        return new SourceSubscriber<>(ssub, sa, mapper, bufferSize);
    }
    
    static final class SourceSubscriber<T, U> extends AtomicInteger implements NbpSubscriber<T> {
        /** */
        private static final long serialVersionUID = 8828587559905699186L;
        final NbpSubscriber<? super U> actual;
        final SerialDisposable sa;
        final Function<? super T, ? extends NbpObservable<? extends U>> mapper;
        final NbpSubscriber<U> inner;
        final Queue<T> queue;
        final int bufferSize;
        
        Disposable s;
        
        volatile boolean done;
        
        volatile long index;
        
        public SourceSubscriber(NbpSubscriber<? super U> actual, SerialDisposable sa,
                Function<? super T, ? extends NbpObservable<? extends U>> mapper, int bufferSize) {
            this.actual = actual;
            this.sa = sa;
            this.mapper = mapper;
            this.bufferSize = bufferSize;
            this.inner = new InnerSubscriber<>(actual, sa, this);
            this.queue = new SpscLinkedArrayQueue<>(bufferSize);
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
        }
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (!queue.offer(t)) {
                cancel();
                actual.onError(new IllegalStateException("More values received than requested!"));
                return;
            }
            if (getAndIncrement() == 0) {
                drain();
            }
        }
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            cancel();
            actual.onError(t);
        }
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            if (getAndIncrement() == 0) {
                drain();
            }
        }
        
        void innerComplete() {
            if (decrementAndGet() != 0) {
                drain();
            }
        }
        
        void cancel() {
            sa.dispose();
            s.dispose();
        }
        
        void drain() {
            boolean d = done;
            T o = queue.poll();
            
            if (o == null) {
                if (d) {
                    actual.onComplete();
                    return;
                }
                RxJavaPlugins.onError(new IllegalStateException("Queue is empty?!"));
                return;
            }
            NbpObservable<? extends U> p;
            try {
                p = mapper.apply(o);
            } catch (Throwable e) {
                cancel();
                actual.onError(e);
                return;
            }
            index++;
            // this is not RS but since our Subscriber doesn't hold state by itself,
            // subscribing it to each source is safe and saves allocation
            p.subscribe(inner);
        }
    }
    
    static final class InnerSubscriber<U> implements NbpSubscriber<U> {
        final NbpSubscriber<? super U> actual;
        final SerialDisposable sa;
        final SourceSubscriber<?, ?> parent;

        /*
         * FIXME this is a workaround for now, but doesn't work 
         * for async non-conforming sources.
         * Such sources require individual instances of InnerSubscriber and a
         * done field.
         */
         
        long index;
        
        public InnerSubscriber(NbpSubscriber<? super U> actual, 
                SerialDisposable sa, SourceSubscriber<?, ?> parent) {
            this.actual = actual;
            this.sa = sa;
            this.parent = parent;
            this.index = 1;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (index == parent.index) {
                sa.set(s);
            }
        }
        
        @Override
        public void onNext(U t) {
            if (index == parent.index) {
                actual.onNext(t);
            }
        }
        @Override
        public void onError(Throwable t) {
            if (index == parent.index) {
                index++;
                parent.cancel();
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }
        @Override
        public void onComplete() {
            if (index == parent.index) {
                index++;
                parent.innerComplete();
            }
        }
    }
}