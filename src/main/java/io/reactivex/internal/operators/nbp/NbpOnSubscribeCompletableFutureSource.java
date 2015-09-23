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

import java.util.concurrent.CompletableFuture;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.BooleanDisposable;

/**
 *
 */
public final class NbpOnSubscribeCompletableFutureSource<T> implements NbpOnSubscribe<T> {
    final CompletableFuture<? extends T> future;
    public NbpOnSubscribeCompletableFutureSource(CompletableFuture<? extends T> future) {
        this.future = future;
    }
    @Override
    public void accept(NbpSubscriber<? super T> s) {
        BooleanDisposable bd = new BooleanDisposable();
        s.onSubscribe(bd);
        
        future.whenComplete((v, e) -> {
            if (!bd.isDisposed()) {
                if (e != null) {
                    s.onError(e);
                } else
                if (v == null) {
                    s.onError(new NullPointerException());
                } else {
                    s.onNext(v);
                    s.onComplete();
                }
            }
        });
    }
}
