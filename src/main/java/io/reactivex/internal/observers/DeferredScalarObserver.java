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

package io.reactivex.internal.observers;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * A fuseable Observer that can generate 0 or 1 resulting value.
 * @param <T> the input value type
 * @param <R> the output value type
 */
public abstract class DeferredScalarObserver<T, R>
extends DeferredScalarDisposable<R>
implements Observer<T> {

    private static final long serialVersionUID = -266195175408988651L;

    /** The upstream disposable. */
    protected Disposable s;

    /**
     * Creates a DeferredScalarObserver instance and wraps a downstream Observer.
     * @param actual the downstream subscriber, not null (not verified)
     */
    public DeferredScalarObserver(Observer<? super R> actual) {
        super(actual);
    }

    @Override
    public void onSubscribe(Disposable s) {
        if (DisposableHelper.validate(this.s, s)) {
            this.s = s;

            actual.onSubscribe(this);
        }
    }

    @Override
    public void onError(Throwable t) {
        value = null;
        error(t);
    }

    @Override
    public void onComplete() {
        R v = value;
        if (v != null) {
            value = null;
            complete(v);
        } else {
            complete();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        s.dispose();
    }
}
