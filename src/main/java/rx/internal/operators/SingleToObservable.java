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

package rx.internal.operators;

import rx.*;
import rx.internal.operators.SingleLiftObservableOperator.WrapSubscriberIntoSingle;

/**
 * Expose a Single.OnSubscribe as an Observable.OnSubscribe.
 *
 * @param <T> the value type
 */
public final class SingleToObservable<T> implements Observable.OnSubscribe<T> {

    final Single.OnSubscribe<T> source;

    public SingleToObservable(Single.OnSubscribe<T> source) {
        this.source = source;
    }

    @Override
    public void call(Subscriber<? super T> t) {
        WrapSubscriberIntoSingle<T> parent = new WrapSubscriberIntoSingle<T>(t);
        t.add(parent);
        source.call(parent);
    }
}
