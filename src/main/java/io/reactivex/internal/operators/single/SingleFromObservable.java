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

import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public final class SingleFromObservable<T> extends Single<T> {
    private final Observable<T> upstream;
    private final T defaultValue;

    public SingleFromObservable(Observable<T> upstream, T defaultValue) {
        this.upstream = upstream;
        this.defaultValue = defaultValue;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {
        upstream.subscribe(new Observer<T>() {
            T last;
            boolean done;
            @Override
            public void onSubscribe(Disposable d) {
                s.onSubscribe(d);
            }

            @Override
            public void onNext(T value) {
                if (last == null) {
                    last = value;
                } else {
                    s.onError(new IndexOutOfBoundsException());
                    done = true;
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!done) {
                    s.onError(e);
                }
            }

            @Override
            public void onComplete() {
                T v = last;
                last = null;
                if (v != null) {
                    s.onSuccess(v);
                } else if (defaultValue != null) { 
                    s.onSuccess(defaultValue);
                } else {
                    s.onError(new NoSuchElementException());
                }
            }
        });
    }
}
