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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Completable.*;
import rx.exceptions.CompositeException;
import rx.Observable;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

public final class CompletableOnSubscribeMerge implements CompletableOnSubscribe {
    final Observable<Completable> source;
    final int maxConcurrency;
    final boolean delayErrors;
    
    @SuppressWarnings("unchecked")
    public CompletableOnSubscribeMerge(Observable<? extends Completable> source, int maxConcurrency, boolean delayErrors) {
        this.source = (Observable<Completable>)source;
        this.maxConcurrency = maxConcurrency;
        this.delayErrors = delayErrors;
    }
    
    @Override
    public void call(CompletableSubscriber s) {
        CompletableMergeSubscriber parent = new CompletableMergeSubscriber(s, maxConcurrency, delayErrors);
        s.onSubscribe(parent);
        source.subscribe(parent);
    }
    
    static final class CompletableMergeSubscriber
    extends Subscriber<Completable> {
        final CompletableSubscriber actual;
        final CompositeSubscription set;
        final int maxConcurrency;
        final boolean delayErrors;
        
        volatile boolean done;
        
        final AtomicReference<Queue<Throwable>> errors;
        
        final AtomicBoolean once;
        
        final AtomicInteger wip;
        
        public CompletableMergeSubscriber(CompletableSubscriber actual, int maxConcurrency, boolean delayErrors) {
            this.actual = actual;
            this.maxConcurrency = maxConcurrency;
            this.delayErrors = delayErrors;
            this.set = new CompositeSubscription();
            this.wip = new AtomicInteger(1);
            this.once = new AtomicBoolean();
            this.errors = new AtomicReference<Queue<Throwable>>();
            if (maxConcurrency == Integer.MAX_VALUE) {
                request(Long.MAX_VALUE);
            } else {
                request(maxConcurrency);
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

            wip.getAndIncrement();
            
            t.unsafeSubscribe(new CompletableSubscriber() {
                Subscription d;
                boolean innerDone;
                @Override
                public void onSubscribe(Subscription d) {
                    this.d = d;
                    set.add(d);
                }
                
                @Override
                public void onError(Throwable e) {
                    if (innerDone) {
                        RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                        return;
                    }
                    innerDone = true;
                    set.remove(d);
                    
                    getOrCreateErrors().offer(e);
                    
                    terminate();
                    
                    if (delayErrors && !done) {
                        request(1);
                    }
                }
                
                @Override
                public void onCompleted() {
                    if (innerDone) {
                        return;
                    }
                    innerDone = true;
                    set.remove(d);
                    
                    terminate();
                    
                    if (!done) {
                        request(1);
                    }
                }
            });
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
                return;
            }
            getOrCreateErrors().offer(t);
            done = true;
            terminate();
        }

        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            done = true;
            terminate();
        }

        void terminate() {
            if (wip.decrementAndGet() == 0) {
                Queue<Throwable> q = errors.get();
                if (q == null || q.isEmpty()) {
                    actual.onCompleted();
                } else {
                    Throwable e = collectErrors(q);
                    if (once.compareAndSet(false, true)) {
                        actual.onError(e);
                    } else {
                        RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
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
                        RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
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
        List<Throwable> list = new ArrayList<Throwable>();
        
        Throwable t;
        while ((t = q.poll()) != null) {
            list.add(t);
        }
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return new CompositeException(list);
    }
}