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

import java.util.concurrent.Callable;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiConsumer;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableCollect<T, U> extends AbstractFlowableWithUpstream<T, U> {
    
    final Callable<? extends U> initialSupplier;
    final BiConsumer<? super U, ? super T> collector;
    
    public FlowableCollect(Publisher<T> source, Callable<? extends U> initialSupplier, BiConsumer<? super U, ? super T> collector) {
        super(source);
        this.initialSupplier = initialSupplier;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        U u;
        try {
            u = initialSupplier.call();
        } catch (Throwable e) {
            EmptySubscription.error(e, s);
            return;
        }
        if (u == null) {
            EmptySubscription.error(new NullPointerException("The initial value supplied is null"), s);
            return;
        }
        
        source.subscribe(new CollectSubscriber<T, U>(s, u, collector));
    }
    
    static final class CollectSubscriber<T, U> extends DeferredScalarSubscription<U> implements Subscriber<T> {
        /** */
        private static final long serialVersionUID = -3589550218733891694L;
        
        final BiConsumer<? super U, ? super T> collector;
        
        final U u;
        
        Subscription s;
        
        boolean done;
        
        public CollectSubscriber(Subscriber<? super U> actual, U u, BiConsumer<? super U, ? super T> collector) {
            super(actual);
            this.collector = collector;
            this.u = u;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            try {
                collector.accept(u, t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.cancel();
                onError(e);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            } 
            done = true;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            complete(u);
        }
        
        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
        }
    }
}