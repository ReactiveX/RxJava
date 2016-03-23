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

package io.reactivex.internal.operators.observable;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.disposables.EmptyDisposable;

public final class NbpOnSubscribeDefer<T> implements NbpOnSubscribe<T> {
    final Supplier<? extends Observable<? extends T>> supplier;
    public NbpOnSubscribeDefer(Supplier<? extends Observable<? extends T>> supplier) {
        this.supplier = supplier;
    }
    @Override
    public void accept(Observer<? super T> s) {
        Observable<? extends T> pub;
        try {
            pub = supplier.get();
        } catch (Throwable t) {
            EmptyDisposable.error(t, s);
            return;
        }
        
        if (pub == null) {
            EmptyDisposable.error(new NullPointerException("null publisher supplied"), s);
            return;
        }
        pub.subscribe(s);
    }
}
