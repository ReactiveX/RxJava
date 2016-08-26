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

package io.reactivex.internal.operators.completable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableConcat extends Completable {
    final Publisher<? extends CompletableSource> sources;
    final int prefetch;
    
    public CompletableConcat(Publisher<? extends CompletableSource> sources, int prefetch) {
        this.sources = sources;
        this.prefetch = prefetch;
    }
    
    @Override
    public void subscribeActual(CompletableObserver s) {
        CompletableConcatSubscriber parent = new CompletableConcatSubscriber(s, prefetch);
        sources.subscribe(parent);
    }
    
    static final class CompletableConcatSubscriber
    extends AtomicInteger
    implements Subscriber<CompletableSource>, Disposable {
        /** */
        private static final long serialVersionUID = 7412667182931235013L;
        final CompletableObserver actual;
        final int prefetch;
        final SequentialDisposable sd;
        
        final SpscArrayQueue<CompletableSource> queue;
        
        Subscription s;
        
        volatile boolean done;

        final AtomicBoolean once = new AtomicBoolean();
        
        final ConcatInnerObserver inner;
        
        public CompletableConcatSubscriber(CompletableObserver actual, int prefetch) {
            this.actual = actual;
            this.prefetch = prefetch;
            this.queue = new SpscArrayQueue<CompletableSource>(prefetch);
            this.sd = new SequentialDisposable();
            this.inner = new ConcatInnerObserver();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(prefetch);
            }
        }
        
        @Override
        public void onNext(CompletableSource t) {
            if (!queue.offer(t)) {
                onError(new MissingBackpressureException());
                return;
            }
            if (getAndIncrement() == 0) {
                next();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                actual.onError(t);
                return;
            }
            done = true;
            RxJavaPlugins.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            if (getAndIncrement() == 0) {
                next();
            }
        }
        
        void innerError(Throwable e) {
            s.cancel();
            onError(e);
        }
        
        void innerComplete() {
            if (decrementAndGet() != 0) {
                next();
            }
            if (!done) {
                s.request(1);
            }
        }
        
        @Override
        public void dispose() {
            s.cancel();
            sd.dispose();
        }

        @Override
        public boolean isDisposed() {
            return sd.isDisposed();
        }

        void next() {
            boolean d = done;
            CompletableSource c = queue.poll();
            if (c == null) {
                if (d) {
                    if (once.compareAndSet(false, true)) {
                        actual.onComplete();
                    }
                    return;
                }
                RxJavaPlugins.onError(new IllegalStateException("Queue is empty?!"));
                return;
            }
            
            c.subscribe(inner);
        }
        
        final class ConcatInnerObserver implements CompletableObserver {
            @Override
            public void onSubscribe(Disposable d) {
                sd.update(d);
            }
            
            @Override
            public void onError(Throwable e) {
                innerError(e);
            }
            
            @Override
            public void onComplete() {
                innerComplete();
            }
        }
    }
}
