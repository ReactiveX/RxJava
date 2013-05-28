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
import rx.util.functions.Func1;

/**
 * A Connectable Observable resembles an ordinary Observable, except that it does not begin
 * emitting a sequence of values when it is subscribed to, but only when its connect() method is
 * called. In this way you can wait for all intended observers to subscribe to the Observable
 * before the Observable begins emitting values.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/publishConnect.png">
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
 *
 * @param <T>
 */

public abstract class ConnectableObservable<T> extends Observable<T> {

    protected ConnectableObservable(Func1<Observer<T>, Subscription> onSubscribe) {
        super(onSubscribe);
    }

    /**
     * Call a Connectable Observable's connect() method to instruct it to begin emitting the
     * objects from its underlying Observable to its subscribing observers. 
     */
    public abstract Subscription connect();

}
