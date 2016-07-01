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

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableDoOnEach<T> extends Flowable<T> {
    final Publisher<T> source;
    final Consumer<? super T> onNext;
    final Consumer<? super Throwable> onError;
    final Runnable onComplete;
    final Runnable onAfterTerminate;
    
    public FlowableDoOnEach(Publisher<T> source, Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, 
            Runnable onComplete,
            Runnable onAfterTerminate) {
        this.source = source;
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onAfterTerminate = onAfterTerminate;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DoOnEachSubscriber<T>(s, onNext, onError, onComplete, onAfterTerminate));
    }
    
    static final class DoOnEachSubscriber<T> implements Subscriber<T>, Subscription {
        final Subscriber<? super T> actual;
        final Consumer<? super T> onNext;
        final Consumer<? super Throwable> onError;
        final Runnable onComplete;
        final Runnable onAfterTerminate;
        
        Subscription s;
        
        boolean done;
        
        public DoOnEachSubscriber(
                Subscriber<? super T> actual,
                Consumer<? super T> onNext, 
                Consumer<? super Throwable> onError, 
                Runnable onComplete,
                Runnable onAfterTerminate) {
            this.actual = actual;
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onAfterTerminate = onAfterTerminate;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            try {
                onNext.accept(t);
            } catch (Throwable e) {
                s.cancel();
                onError(e);
                return;
            }
            
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            boolean relay = true;
            try {
                onError.accept(t);
            } catch (Throwable e) {
                actual.onError(new CompositeException(e, t));
                relay = false;
            }
            if (relay) {
                actual.onError(t);
            }
            
            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                RxJavaPlugins.onError(e);
            }
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            try {
                onComplete.run();
            } catch (Throwable e) {
                onError(e);
                return;
            }
            
            actual.onComplete();
            
            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                RxJavaPlugins.onError(e);
            }
        }
        
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }

    }
}
