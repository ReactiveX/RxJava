/**
 * Copyright 2013 Netflix, Inc.
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
package rx.observables;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.operators.OperationRefCount;
import rx.util.functions.Func1;

/**
 * A ConnectableObservable resembles an ordinary {@link Observable}, except that it does not begin
 * emitting items when it is subscribed to, but only when its {@link #connect} method is called. In
 * this way you can wait for all intended {@link Observer}s to {@link Observable#subscribe} to the
 * Observable before the Observable begins emitting items.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/publishConnect.png">
 * <p>
 * For more information see
 * <a href="https://github.com/Netflix/RxJava/wiki/Connectable-Observable-Operators">Connectable
 * Observable Operators</a> at the RxJava Wiki
 *
 * @param <T>
 */

public abstract class ConnectableObservable<T> extends Observable<T> {

    protected ConnectableObservable(Func1<Observer<T>, Subscription> onSubscribe) {
        super(onSubscribe);
    }

    /**
     * Call a ConnectableObservable's connect() method to instruct it to begin emitting the
     * items from its underlying {@link Observable} to its {@link Observer}s. 
     */
    public abstract Subscription connect();

    /**
     * Returns an observable sequence that stays connected to the source as long
     * as there is at least one subscription to the observable sequence.
     * @return a {@link Observable}
     */
    public Observable<T> refCount() {
        return refCount(this);
    }

    /**
     * Returns an observable sequence that stays connected to the source as long
     * as there is at least one subscription to the observable sequence.
     * @param that
     *              a {@link ConnectableObservable}
     * @return a {@link Observable}
     */
    public static <T> Observable<T> refCount(ConnectableObservable<T> that) {
        return Observable.create(OperationRefCount.refCount(that));
    }
}
