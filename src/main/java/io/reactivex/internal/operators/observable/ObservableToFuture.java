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

import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.SequentialDisposable;

/**
 * Utility method to turn a Publisher into a Future.
 */
public enum ObservableToFuture {
    ;
    
    public static <T> Future<T> toFuture(ObservableSource<? extends T> o) {
        final CountDownLatch cdl = new CountDownLatch(1);
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final SequentialDisposable sd = new SequentialDisposable();
        
        o.subscribe(new Observer<T>() {

            @Override
            public void onSubscribe(Disposable d) {
                sd.replace(d);
            }

            @Override
            public void onNext(T v) {
                if (value.get() != null) {
                    sd.dispose();
                    onError(new IndexOutOfBoundsException("More than one element received"));
                    return;
                }
                value.lazySet(v);
            }

            @Override
            public void onError(Throwable e) {
                error.lazySet(e);
                cdl.countDown();
            }

            @Override
            public void onComplete() {
                if (value.get() == null) {
                    onError(new NoSuchElementException("The source is empty"));
                    return;
                }
                cdl.countDown();
            }
            
        });
        
        return new Future<T>() {
            
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (cdl.getCount() != 0) {
                    sd.dispose();
                    error.set(new CancellationException());
                    cdl.countDown();
                    return true;
                }
                return false;
            }

            @Override
            public boolean isCancelled() {
                return sd.isDisposed();
            }

            @Override
            public boolean isDone() {
                return cdl.getCount() == 0 && !sd.isDisposed();
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                if (cdl.getCount() != 0) {
                    cdl.await();
                }
                Throwable e = error.get();
                if (e != null) {
                    if (e instanceof CancellationException) {
                        throw (CancellationException)e;
                    }
                    throw new ExecutionException(e);
                }
                return value.get();
            }

            @Override
            public T get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                if (cdl.getCount() != 0) {
                    if (!cdl.await(timeout, unit)) {
                        throw new TimeoutException();
                    }
                }
                Throwable e = error.get();
                if (e != null) {
                    if (e instanceof CancellationException) {
                        throw (CancellationException)e;
                    }
                    throw new ExecutionException(e);
                }
                return value.get();
            }
            
        };
    }
}
