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

package io.reactivex.subscribers.completable;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Completable.CompletableSubscriber;
import io.reactivex.disposables.Disposable;

public final class CompletableSerializedSubscriber implements CompletableSubscriber {

    final CompletableSubscriber actual;
    
    final AtomicBoolean once = new AtomicBoolean();
    
    public CompletableSerializedSubscriber(CompletableSubscriber actual) {
        this.actual = actual;
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        actual.onSubscribe(d);
    }

    @Override
    public void onError(Throwable e) {
        if (once.compareAndSet(false, true)) {
            actual.onError(e);
        }
    }

    @Override
    public void onComplete() {
        if (once.compareAndSet(false, true)) {
            actual.onComplete();
        }
    }

}