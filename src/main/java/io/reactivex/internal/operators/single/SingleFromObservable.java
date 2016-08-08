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

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.NoSuchElementException;

public final class SingleFromObservable<T> extends Single<T> {
    private final Observable<T> upstream;

    public SingleFromObservable(Observable<T> upstream) {
        this.upstream = upstream;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {
        upstream.subscribe(new Observer<T>() {
            T last;
            @Override
            public void onSubscribe(Disposable d) {
                s.onSubscribe(d);
            }
            @Override
            public void onNext(T value) {
                last = value;
            }
            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }
            @Override
            public void onComplete() {
                T v = last;
                last = null;
                if (v != null) {
                    s.onSuccess(v);
                } else {
                    s.onError(new NoSuchElementException());
                }
            }
        });
    }
}
