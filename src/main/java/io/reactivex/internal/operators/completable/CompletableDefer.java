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

import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.internal.disposables.EmptyDisposable;

public final class CompletableDefer extends Completable {

    final Callable<? extends CompletableSource> completableSupplier;
    
    public CompletableDefer(Callable<? extends CompletableSource> completableSupplier) {
        this.completableSupplier = completableSupplier;
    }

    @Override
    protected void subscribeActual(CompletableObserver s) {
        CompletableSource c;
        
        try {
            c = completableSupplier.call();
        } catch (Throwable e) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onError(e);
            return;
        }
        
        if (c == null) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onError(new NullPointerException("The completable returned is null"));
            return;
        }
        
        c.subscribe(s);
    }

}
