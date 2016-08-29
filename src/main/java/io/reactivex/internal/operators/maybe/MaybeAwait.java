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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.util.NotificationLite;

public enum MaybeAwait {
    ;
    
    public static <T> T get(MaybeSource<T> source, T defaultIfComplete) {
        final AtomicReference<Object> valueRef = new AtomicReference<Object>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);
        
        source.subscribe(new MaybeObserver<T>() {
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
                valueRef.lazySet(NotificationLite.next(value));
                cdl.countDown();
            }

            @Override
            public void onComplete() {
                valueRef.lazySet(NotificationLite.complete());
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
            throw Exceptions.propagate(e);
        }

        Object value = valueRef.get();
        if (NotificationLite.isComplete(value))
            return defaultIfComplete;
        else
            return NotificationLite.getValue(value);
    }
}
