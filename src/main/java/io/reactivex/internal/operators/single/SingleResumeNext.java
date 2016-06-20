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

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Function;

public final class SingleResumeNext<T> extends Single<T> {
    final SingleConsumable<? extends T> source;
    
    final Function<? super Throwable, ? extends SingleConsumable<? extends T>> nextFunction;
    
    public SingleResumeNext(SingleConsumable<? extends T> source,
            Function<? super Throwable, ? extends SingleConsumable<? extends T>> nextFunction) {
        this.source = source;
        this.nextFunction = nextFunction;
    }

    @Override
    protected void subscribeActual(final SingleSubscriber<? super T> s) {

        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        s.onSubscribe(mad);
        
        source.subscribe(new SingleSubscriber<T>() {

            @Override
            public void onSubscribe(Disposable d) {
                mad.set(d);
            }

            @Override
            public void onSuccess(T value) {
                s.onSuccess(value);
            }

            @Override
            public void onError(Throwable e) {
                SingleConsumable<? extends T> next;
                
                try {
                    next = nextFunction.apply(e);
                } catch (Throwable ex) {
                    s.onError(new CompositeException(ex, e));
                    return;
                }
                
                if (next == null) {
                    NullPointerException npe = new NullPointerException("The next Single supplied was null");
                    npe.initCause(e);
                    s.onError(npe);
                    return;
                }
                
                next.subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        mad.set(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        s.onSuccess(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }
                    
                });
            }
            
        });
    }

}
