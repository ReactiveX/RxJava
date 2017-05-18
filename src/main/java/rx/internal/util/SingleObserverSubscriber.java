/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.util;

import rx.Observer;
import rx.SingleSubscriber;

/**
 * Wraps an Observer and forwards the onXXX method calls to it.
 * @param <T> the value type
 */
public final class SingleObserverSubscriber<T> extends SingleSubscriber<T> {
    final Observer<? super T> observer;

    public SingleObserverSubscriber(Observer<? super T> observer) {
        this.observer = observer;
    }

    @Override
    public void onSuccess(T t) {
        observer.onNext(t);
        observer.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        observer.onError(e);
    }
}
