/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;
import io.reactivex.internal.observers.DisposableLambdaObserver;

public final class ObservableDoOnLifecycle<T> extends AbstractObservableWithUpstream<T, T> {
    private final Consumer<? super Disposable> onSubscribe;
    private final Action onDispose;

    public ObservableDoOnLifecycle(Observable<T> upstream, Consumer<? super Disposable> onSubscribe,
            Action onDispose) {
        super(upstream);
        this.onSubscribe = onSubscribe;
        this.onDispose = onDispose;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new DisposableLambdaObserver<T>(observer, onSubscribe, onDispose));
    }
}
