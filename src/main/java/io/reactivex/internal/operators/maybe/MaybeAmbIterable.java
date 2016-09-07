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

import io.reactivex.*;
import io.reactivex.internal.operators.maybe.MaybeAmbArray.AmbMaybeObserver;

/**
 * Signals the event of the first MaybeSource that signals.
 *
 * @param <T> the value type emitted
 */
public final class MaybeAmbIterable<T> extends Maybe<T> {

    final Iterable<? extends MaybeSource<? extends T>> sources;

    public MaybeAmbIterable(Iterable<? extends MaybeSource<? extends T>> sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        AmbMaybeObserver<T> parent = new AmbMaybeObserver<T>(observer);
        observer.onSubscribe(parent);

        int i = 0;
        for (MaybeSource<? extends T> s : sources) {
            if (parent.isDisposed()) {
                return;
            }

            if (s == null) {
                parent.onError(new NullPointerException("One of the MaybeSources is null"));
                return;
            }

            s.subscribe(parent);
            i++;
        }

        if (i == 0) {
            observer.onComplete();
        }
    }
}
