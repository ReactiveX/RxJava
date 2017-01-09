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

package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class CompletableFromSingle<T> extends Completable {

    final SingleSource<T> single;

    public CompletableFromSingle(SingleSource<T> single) {
        this.single = single;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {
        single.subscribe(new CompletableFromSingleObserver<T>(s));
    }

    static final class CompletableFromSingleObserver<T> implements SingleObserver<T> {
        final CompletableObserver co;

        CompletableFromSingleObserver(CompletableObserver co) {
            this.co = co;
        }

        @Override
        public void onError(Throwable e) {
            co.onError(e);
        }

        @Override
        public void onSubscribe(Disposable d) {
            co.onSubscribe(d);
        }

        @Override
        public void onSuccess(T value) {
            co.onComplete();
        }
    }
}
