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

package io.reactivex.rxjava3.internal.operators.maybe;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Executes an Action and signals its exception or completes normally.
 *
 * @param <T> the value type
 */
public final class MaybeFromAction<T> extends Maybe<T> implements Supplier<T> {

    final Action action;

    public MaybeFromAction(Action action) {
        this.action = action;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        Disposable d = Disposable.empty();
        observer.onSubscribe(d);

        if (!d.isDisposed()) {

            try {
                action.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                if (!d.isDisposed()) {
                    observer.onError(ex);
                } else {
                    RxJavaPlugins.onError(ex);
                }
                return;
            }

            if (!d.isDisposed()) {
                observer.onComplete();
            }
        }
    }

    @Override
    public T get() throws Throwable {
        action.run();
        return null; // considered as onComplete()
    }
}
