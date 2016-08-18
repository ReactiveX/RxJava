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

import java.util.Collection;
import java.util.concurrent.Callable;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.ArrayListSupplier;

public final class FlowableToList<T, U extends Collection<? super T>> extends AbstractFlowableWithUpstream<T, U> {
    final Callable<U> collectionSupplier;

    @SuppressWarnings("unchecked")
    public FlowableToList(Publisher<T> source) {
        this(source, (Callable<U>)ArrayListSupplier.asCallable());
    }

    public FlowableToList(Publisher<T> source, Callable<U> collectionSupplier) {
        super(source);
        this.collectionSupplier = collectionSupplier;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        U coll;
        try {
            coll = collectionSupplier.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }
        source.subscribe(new ToListSubscriber<T, U>(s, coll));
    }
    
    
    static final class ToListSubscriber<T, U extends Collection<? super T>>
    extends DeferredScalarSubscription<U>
    implements Subscriber<T>, Subscription {
        
        /** */
        private static final long serialVersionUID = -8134157938864266736L;
        Subscription s;
        
        public ToListSubscriber(Subscriber<? super U> actual, U collection) {
            super(actual);
            this.value = collection;
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
            value.add(t);
        }
        
        @Override
        public void onError(Throwable t) {
            value = null;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            complete(value);
        }
        
        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
        }
    }
}
