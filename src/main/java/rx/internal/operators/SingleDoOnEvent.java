/**
 * Copyright 2014 Netflix, Inc.
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

package rx.internal.operators;

import rx.Single;
import rx.SingleSubscriber;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.functions.Action1;

public final class SingleDoOnEvent<T> implements Single.OnSubscribe<T> {
    final Single<T> source;
    final Action1<? super T> onSuccess;
    final Action1<Throwable> onError;

    public SingleDoOnEvent(Single<T> source, Action1<? super T> onSuccess, Action1<Throwable> onError) {
        this.source = source;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    @Override
    public void call(SingleSubscriber<? super T> actual) {
        SingleDoOnEventSubscriber<T> parent = new SingleDoOnEventSubscriber<T>(actual, onSuccess, onError);
        actual.add(parent);
        source.subscribe(parent);
    }

    static final class SingleDoOnEventSubscriber<T> extends SingleSubscriber<T> {
        final SingleSubscriber<? super T> actual;
        final Action1<? super T> onSuccess;
        final Action1<Throwable> onError;

        SingleDoOnEventSubscriber(SingleSubscriber<? super T> actual, Action1<? super T> onSuccess, Action1<Throwable> onError) {
            this.actual = actual;
            this.onSuccess = onSuccess;
            this.onError = onError;
        }

        @Override
        public void onSuccess(T value) {
            try {
                onSuccess.call(value);
            } catch (Throwable e) {
                Exceptions.throwOrReport(e, this, value);
                return;
            }

            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable error) {
            try {
                onError.call(error);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(error, e));
                return;
            }

            actual.onError(error);
        }
    }
}
