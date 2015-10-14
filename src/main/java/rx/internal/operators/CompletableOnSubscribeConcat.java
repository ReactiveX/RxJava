/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.operators;

import java.util.concurrent.atomic.*;

import rx.*;
import rx.Completable.*;
import rx.exceptions.MissingBackpressureException;
import rx.internal.util.unsafe.SpscArrayQueue;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.SerialSubscription;

public final class CompletableOnSubscribeConcat implements CompletableOnSubscribe {
    final Observable<? extends Completable> sources;
    final int prefetch;
    
    public CompletableOnSubscribeConcat(Observable<? extends Completable> sources, int prefetch) {
        this.sources = sources;
        this.prefetch = prefetch;
    }
    
    @Override
    public void call(CompletableSubscriber s) {
        CompletableConcatSubscriber parent = new CompletableConcatSubscriber(s, prefetch);
        s.onSubscribe(parent);
        sources.subscribe(parent);
    }
    
    static final class CompletableConcatSubscriber
    extends Subscriber<Completable> {
        final CompletableSubscriber actual;
        final int prefetch;
        final SerialSubscription sr;
        
        final SpscArrayQueue<Completable> queue;
        
        volatile boolean done;

        volatile int once;
        static final AtomicIntegerFieldUpdater<CompletableConcatSubscriber> ONCE =
                AtomicIntegerFieldUpdater.newUpdater(CompletableConcatSubscriber.class, "once");
        
        final ConcatInnerSubscriber inner;
        
        final AtomicInteger wip;
        
        public CompletableConcatSubscriber(CompletableSubscriber actual, int prefetch) {
            this.actual = actual;
            this.prefetch = prefetch;
            this.queue = new SpscArrayQueue<Completable>(prefetch);
            this.sr = new SerialSubscription();
            this.inner = new ConcatInnerSubscriber();
            this.wip = new AtomicInteger();
            add(sr);
            request(prefetch);
        }
        
        @Override
        public void onNext(Completable t) {
            if (!queue.offer(t)) {
                onError(new MissingBackpressureException());
                return;
            }
            if (wip.getAndIncrement() == 0) {
                next();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (ONCE.compareAndSet(this, 0, 1)) {
                actual.onError(t);
                return;
            }
            RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
        }
        
        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            done = true;
            if (wip.getAndIncrement() == 0) {
                next();
            }
        }
        
        void innerError(Throwable e) {
            unsubscribe();
            onError(e);
        }
        
        void innerComplete() {
            if (wip.decrementAndGet() != 0) {
                next();
            }
            if (!done) {
                request(1);
            }
        }
        
        void next() {
            boolean d = done;
            Completable c = queue.poll();
            if (c == null) {
                if (d) {
                    if (ONCE.compareAndSet(this, 0, 1)) {
                        actual.onCompleted();
                    }
                    return;
                }
                RxJavaPlugins.getInstance().getErrorHandler().handleError(new IllegalStateException("Queue is empty?!"));
                return;
            }
            
            c.subscribe(inner);
        }
        
        final class ConcatInnerSubscriber implements CompletableSubscriber {
            @Override
            public void onSubscribe(Subscription d) {
                sr.set(d);
            }
            
            @Override
            public void onError(Throwable e) {
                innerError(e);
            }
            
            @Override
            public void onCompleted() {
                innerComplete();
            }
        }
    }
}