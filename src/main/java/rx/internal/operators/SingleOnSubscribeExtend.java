/*
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
package rx.internal.operators;

import rx.*;

/**
 * Transforms a Single.OnSubscribe.call() into an Single.subscribe() call.
 * @param <T> the value type
 */
public final class SingleOnSubscribeExtend<T> implements Single.OnSubscribe<T> {
    final Single<T> parent;

    public SingleOnSubscribeExtend(Single<T> parent) {
        this.parent = parent;
    }

    @Override
    public void call(SingleSubscriber<? super T> subscriber) {
        subscriber.add(parent.subscribe(subscriber));
    }
}
