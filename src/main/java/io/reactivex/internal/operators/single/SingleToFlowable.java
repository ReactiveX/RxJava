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
package io.reactivex.internal.operators.single;

import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.*;

/**
 * Wraps a Single and exposes it as a Flowable.
 *
 * @param <T> the value type
 */
public final class SingleToFlowable<T> extends Flowable<T> {
    
    final SingleConsumable<? extends T> source;
    
    public SingleToFlowable(SingleConsumable<? extends T> source) {
        this.source = source;
    }
    
    @Override
    public void subscribeActual(final Subscriber<? super T> s) {
        final ScalarAsyncSubscription<T> sas = new ScalarAsyncSubscription<T>(s);
        final AsyncSubscription as = new AsyncSubscription();
        as.setSubscription(sas);
        s.onSubscribe(as);
        
        source.subscribe(new SingleSubscriber<T>() {
            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }

            @Override
            public void onSubscribe(Disposable d) {
                as.setResource(d);
            }

            @Override
            public void onSuccess(T value) {
                sas.setValue(value);
            }
            
        });
    }
}
