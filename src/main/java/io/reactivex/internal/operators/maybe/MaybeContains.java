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

package io.reactivex.internal.operators.maybe;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiPredicate;

public final class MaybeContains<T> extends Maybe<Boolean> {

    final MaybeSource<T> source;
    
    final Object value;
    
    final BiPredicate<Object, Object> comparer;
    
    public MaybeContains(MaybeSource<T> source, Object value, BiPredicate<Object, Object> comparer) {
        this.source = source;
        this.value = value;
        this.comparer = comparer;
    }

    @Override
    protected void subscribeActual(final MaybeObserver<? super Boolean> s) {

        source.subscribe(new MaybeObserver<T>() {

            @Override
            public void onSubscribe(Disposable d) {
                s.onSubscribe(d);
            }

            @Override
            public void onSuccess(T v) {
                boolean b;
                
                try {
                    b = comparer.test(v, value);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    s.onError(ex);
                    return;
                }
                s.onSuccess(b);
            }

            @Override
            public void onComplete() {
                s.onSuccess(false);
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }
            
        });
    }

}
