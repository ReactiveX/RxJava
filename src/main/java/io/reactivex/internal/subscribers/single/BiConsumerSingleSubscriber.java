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

package io.reactivex.internal.subscribers.single;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.SingleSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.internal.disposables.DisposableHelper;

public final class BiConsumerSingleSubscriber<T> 
extends AtomicReference<Disposable>
implements SingleSubscriber<T>, Disposable {

    /** */
    private static final long serialVersionUID = 4943102778943297569L;
    final BiConsumer<? super T, ? super Throwable> onCallback;

    public BiConsumerSingleSubscriber(BiConsumer<? super T, ? super Throwable> onCallback) {
        this.onCallback = onCallback;
    }
    
    @Override
    public void onError(Throwable e) {
        onCallback.accept(null, e);
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
    }
    
    @Override
    public void onSuccess(T value) {
        onCallback.accept(value, null);
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }
}
