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

package io.reactivex.internal.operators.observable;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Allows lifting operators into a chain of Observables.
 *
 * <p>By having a concrete ObservableSource as lift, operator fusing can now identify
 * both the source and the operation inside it via casting, unlike the lambda version of this.
 *
 * @param <T> the upstream value type
 * @param <R> the downstream parameter type
 */
public final class ObservableLift<R, T> extends AbstractObservableWithUpstream<T, R> {
    /** The actual operator. */
    final ObservableOperator<? extends R, ? super T> operator;

    public ObservableLift(ObservableSource<T> source, ObservableOperator<? extends R, ? super T> operator) {
        super(source);
        this.operator = operator;
    }

    @Override
    public void subscribeActual(Observer<? super R> s) {
        Observer<? super T> observer;
        try {
            observer = ObjectHelper.requireNonNull(operator.apply(s), "Operator " + operator + " returned a null Observer");
        } catch (NullPointerException e) { // NOPMD
            throw e;
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            // can't call onError because no way to know if a Disposable has been set or not
            // can't call onSubscribe because the call might have set a Disposable already
            RxJavaPlugins.onError(e);

            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(e);
            throw npe;
        }

        source.subscribe(observer);
    }
}
