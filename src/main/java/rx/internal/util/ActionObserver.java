/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.util;

import rx.Observer;
import rx.functions.*;

/**
 * An Observer that forwards the onXXX method calls to callbacks.
 * @param <T> the value type
 */
public final class ActionObserver<T> implements Observer<T> {

    final Action1<? super T> onNext;
    final Action1<? super Throwable> onError;
    final Action0 onCompleted;

    public ActionObserver(Action1<? super T> onNext, Action1<? super Throwable> onError, Action0 onCompleted) {
        this.onNext = onNext;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public void onNext(T t) {
        onNext.call(t);
    }

    @Override
    public void onError(Throwable e) {
        onError.call(e);
    }

    @Override
    public void onCompleted() {
        onCompleted.call();
    }
}