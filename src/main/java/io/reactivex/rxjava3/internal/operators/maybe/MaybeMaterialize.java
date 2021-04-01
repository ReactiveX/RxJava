/*
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
import io.reactivex.rxjava3.internal.operators.mixed.MaterializeSingleObserver;

/**
 * Turn the signal types of a Maybe source into a single Notification of
 * equal kind.
 * <p>History: 2.2.4 - experimental
 *
 * @param <T> the element type of the source
 * @since 3.0.0
 */
public final class MaybeMaterialize<T> extends Single<Notification<T>> {

    final Maybe<T> source;

    public MaybeMaterialize(Maybe<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super Notification<T>> observer) {
        source.subscribe(new MaterializeSingleObserver<>(observer));
    }
}
