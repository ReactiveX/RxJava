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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.util.ExceptionHelper;

public enum SingleAwait {
    ;
    
    public static <T> T get(SingleSource<T> source) {
        final AtomicReference<T> valueRef = new AtomicReference<T>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);
        
        source.subscribe(new SingleObserver<T>() {
            @Override
            public void onError(Throwable e) {
                errorRef.lazySet(e);
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
            }
            @Override
            public void onSuccess(T value) {
                valueRef.lazySet(value);
                cdl.countDown();
            }
        });
        
        if (cdl.getCount() != 0L) {
            try {
                cdl.await();
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }
        Throwable e = errorRef.get();
        if (e != null) {
            throw ExceptionHelper.wrapOrThrow(e);
        }
        return valueRef.get();
    }
}
