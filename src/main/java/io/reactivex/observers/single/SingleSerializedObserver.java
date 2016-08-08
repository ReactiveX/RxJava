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

package io.reactivex.observers.single;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SingleSerializedObserver<T> implements SingleObserver<T> {

    final SingleObserver<? super T> actual;

    final AtomicBoolean once = new AtomicBoolean();

    public SingleSerializedObserver(SingleObserver<? super T> actual) {
        this.actual = actual;
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        actual.onSubscribe(d);
    }

    @Override
    public void onSuccess(T value) {
        if (once.compareAndSet(false, true)) {
            actual.onSuccess(value);
        }
    }

    @Override
    public void onError(Throwable e) {
        if (once.compareAndSet(false, true)) {
            actual.onError(e);
        }
    }

}
