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
import io.reactivex.disposables.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableMerge extends Completable {
    final Publisher<? extends CompletableSource> source;
    final int maxConcurrency;
    final boolean delayErrors;
    
    public CompletableMerge(Publisher<? extends CompletableSource> source, int maxConcurrency, boolean delayErrors) {
        this.source = source;
        this.maxConcurrency = maxConcurrency;
        this.delayErrors = delayErrors;
    }
    
    @Override
    public void subscribeActual(CompletableObserver s) {
        CompletableMergeSubscriber parent = new CompletableMergeSubscriber(s, maxConcurrency, delayErrors);
        source.subscribe(parent);
    }
    
    static final class CompletableMergeSubscriber
    extends AtomicInteger
    implements Subscriber<CompletableSource>, Disposable {
        /** */
        private static final long serialVersionUID = -2108443387387077490L;
        
        final CompletableObserver actual;
        final int maxConcurrency;
        final boolean delayErrors;

        final AtomicThrowable error;
        
        final AtomicBoolean once;

        final CompositeDisposable set;

        Subscription s;
        
        volatile boolean done;

        
        public CompletableMergeSubscriber(CompletableObserver actual, int maxConcurrency, boolean delayErrors) {
            this.actual = actual;
            this.maxConcurrency = maxConcurrency;
            this.delayErrors = delayErrors;
            this.set = new CompositeDisposable();
            this.error = new AtomicThrowable();
            this.once = new AtomicBoolean();
            lazySet(1);
        }
        
        @Override
        public void dispose() {
            s.cancel();
            set.dispose();
        }

        @Override
        public boolean isDisposed() {
            return set.isDisposed();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                if (maxConcurrency == Integer.MAX_VALUE) {
                    s.request(Long.MAX_VALUE);
                } else {
                    s.request(maxConcurrency);
                }
            }
        }
        
        @Override
        public void onNext(CompletableSource t) {
            if (done) {
                return;
            }

            getAndIncrement();
            
            t.subscribe(new InnerObserver());
        }

        @Override
        public void onError(Throwable t) {
            if (done || !error.addThrowable(t)) {
                RxJavaPlugins.onError(t);
                return;
            }
            
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
                Throwable ex = error.terminate();
                if (ex == null && ex != ExceptionHelper.TERMINATED) {
                    actual.onComplete();
                } else {
                    actual.onError(ex);
                }
            }
            else if (!delayErrors) {
                Throwable ex = error.get();
                if (ex != null && ex != ExceptionHelper.TERMINATED) {
                    s.cancel();
                    set.dispose();
                    if (once.compareAndSet(false, true)) {
                        actual.onError(error.terminate());
                    } else {
                        RxJavaPlugins.onError(ex);
                    }
                }
            }
        }
        
        final class InnerObserver implements CompletableObserver {
            Disposable d;
            boolean innerDone;

            @Override
            public void onSubscribe(Disposable d) {
                this.d = d;
                set.add(d);
            }

            @Override
            public void onError(Throwable e) {
                if (innerDone || !error.addThrowable(e)) {
                    RxJavaPlugins.onError(e);
                    return;
                }
                
                set.delete(d);
                innerDone = true;
                
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
                
                set.delete(d);
                innerDone = true;
                
                terminate();
                
                if (!done) {
                    s.request(1);
                }
            }
        }
    }
}
