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

public final class SingleDoOnCancel<T> extends Single<T> {
    final SingleConsumable<T> source;

    final Runnable onCancel;

    public SingleDoOnCancel(SingleConsumable<T> source, Runnable onCancel) {
        this.source = source;
        this.onCancel = onCancel;
    }

    @Override
    protected void subscribeActual(final SingleSubscriber<? super T> s) {

        source.subscribe(new SingleSubscriber<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                CompositeDisposable set = new CompositeDisposable();
                set.add(Disposables.from(onCancel));
                set.add(d);
                s.onSubscribe(set);
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

}
