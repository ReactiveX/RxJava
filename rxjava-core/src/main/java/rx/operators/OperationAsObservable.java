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
package rx.operators;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * Hides the identity of another observable.
 * 
 * @param <T>
 *            the return value type of the wrapped observable.
 */
public final class OperationAsObservable<T> implements OnSubscribeFunc<T> {
    private final Observable<? extends T> source;

    public OperationAsObservable(Observable<? extends T> source) {
        this.source = source;
    }

    @Override
    public Subscription onSubscribe(Observer<? super T> t1) {
        return source.subscribe(t1);
    }
}
