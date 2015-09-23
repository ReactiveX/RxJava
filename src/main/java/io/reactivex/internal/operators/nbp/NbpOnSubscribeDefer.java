/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import java.util.function.Supplier;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.internal.disposables.EmptyDisposable;

/**
 *
 */
public final class NbpOnSubscribeDefer<T> implements NbpOnSubscribe<T> {
    final Supplier<? extends NbpObservable<? extends T>> supplier;
    public NbpOnSubscribeDefer(Supplier<? extends NbpObservable<? extends T>> supplier) {
        this.supplier = supplier;
    }
    @Override
    public void accept(NbpSubscriber<? super T> s) {
        NbpObservable<? extends T> pub;
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
