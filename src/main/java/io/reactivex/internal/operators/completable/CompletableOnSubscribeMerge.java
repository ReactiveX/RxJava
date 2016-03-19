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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Completable.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.internal.disposables.SetCompositeResource;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableOnSubscribeMerge implements CompletableOnSubscribe {
    final Flowable<? extends Completable> source;
    final int maxConcurrency;
    final boolean delayErrors;
    
    public CompletableOnSubscribeMerge(Flowable<? extends Completable> source, int maxConcurrency, boolean delayErrors) {
        this.source = source;
        this.maxConcurrency = maxConcurrency;
        this.delayErrors = delayErrors;
    }
    
    @Override
    public void accept(CompletableSubscriber s) {
        CompletableMergeSubscriber parent = new CompletableMergeSubscriber(s, maxConcurrency, delayErrors);
        source.subscribe(parent);
    }
    
    static final class CompletableMergeSubscriber
    extends AtomicInteger
    implements Subscriber<Completable>, Disposable {
        /** */
        private static final long serialVersionUID = -2108443387387077490L;
        
        final CompletableSubscriber actual;
        final SetCompositeResource<Disposable> set;
        final int maxConcurrency;
        final boolean delayErrors;
        
        Subscription s;
        
        volatile boolean done;
        
        final AtomicReference<Queue<Throwable>> errors = new AtomicReference<Queue<Throwable>>();
        
        final AtomicBoolean once = new AtomicBoolean();
        
        public CompletableMergeSubscriber(CompletableSubscriber actual, int maxConcurrency, boolean delayErrors) {
            this.actual = actual;
            this.maxConcurrency = maxConcurrency;
            this.delayErrors = delayErrors;
            this.set = new SetCompositeResource<Disposable>(Disposables.consumeAndDispose());
            lazySet(1);
        }
        
        @Override
        public void dispose() {
            s.cancel();
            set.dispose();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            set.add(Disposables.from(s));
            actual.onSubscribe(this);
            if (maxConcurrency == Integer.MAX_VALUE) {
                s.request(Long.MAX_VALUE);
            } else {
                s.request(maxConcurrency);
            }
        }
        
        Queue<Throwable> getOrCreateErrors() {
            Queue<Throwable> q = errors.get();
            
            if (q != null) {
                return q;
            }
            
            q = new ConcurrentLinkedQueue<Throwable>();
            if (errors.compareAndSet(null, q)) {
                return q;
            }
            return errors.get();
        }

        @Override
        public void onNext(Completable t) {
            if (done) {
                return;
            }

            getAndIncrement();
            
            t.subscribe(new CompletableSubscriber() {
                Disposable d;
                boolean innerDone;
                @Override
                public void onSubscribe(Disposable d) {
                    this.d = d;
                    set.add(d);
                }
                
                @Override
                public void onError(Throwable e) {
                    if (innerDone) {
                        RxJavaPlugins.onError(e);
                        return;
                    }
                    innerDone = true;
                    set.remove(d);
                    
                    getOrCreateErrors().offer(e);
                    
                    terminate();
                    
                    if (delayErrors && !done) {
                        s.request(1);
                    }
                }
                
                @Override
                public void onComplete() {
                    if (innerDone) {
                        return;
                    }
                    innerDone = true;
                    set.remove(d);
                    
                    terminate();
                    
                    if (!done) {
                        s.request(1);
                    }
                }
            });
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            getOrCreateErrors().offer(t);
            done = true;
            terminate();
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            terminate();
        }

        void terminate() {
            if (decrementAndGet() == 0) {
                Queue<Throwable> q = errors.get();
                if (q == null || q.isEmpty()) {
                    actual.onComplete();
                } else {
                    Throwable e = collectErrors(q);
                    if (once.compareAndSet(false, true)) {
                        actual.onError(e);
                    } else {
                        RxJavaPlugins.onError(e);
                    }
                }
            } else
            if (!delayErrors) {
                Queue<Throwable> q = errors.get();
                if (q != null && !q.isEmpty()) {
                    Throwable e = collectErrors(q);
                    if (once.compareAndSet(false, true)) {
                        actual.onError(e);
                    } else {
                        RxJavaPlugins.onError(e);
                    }
                }
            }
        }
    }
    
    /**
     * Collects the Throwables from the queue, adding subsequent Throwables as suppressed to
     * the first Throwable and returns it.
     * @param q the queue to drain
     * @return the Throwable containing all other Throwables as suppressed
     */
    public static Throwable collectErrors(Queue<Throwable> q) {
        CompositeException composite = null;
        Throwable first = null;
        
        Throwable t;
        int count = 0;
        while ((t = q.poll()) != null) {
            if (count == 0) {
                first = t;
            } else {
                if (composite == null) {
                    composite = new CompositeException(first);
                }
                composite.suppress(t);
            }
            
            count++;
        }
        if (composite != null) {
            return composite;
        }
        return first;
    }
}