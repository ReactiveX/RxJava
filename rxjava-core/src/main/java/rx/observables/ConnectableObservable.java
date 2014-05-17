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
package rx.observables;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.operators.OperatorRefCount;

/**
 * A ConnectableObservable resembles an ordinary {@link Observable}, except that it does not begin
 * emitting items when it is subscribed to, but only when its {@link #connect} method is called. In
 * this way you can wait for all intended {@link Subscriber}s to {@link Observable#subscribe} to the
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

    protected ConnectableObservable(OnSubscribe<T> onSubscribe) {
        super(onSubscribe);
    }

    /**
     * Call a ConnectableObservable's connect() method to instruct it to begin emitting the
     * items from its underlying {@link Observable} to its {@link Subscriber}s.
     * <p>To disconnect from a synchronous source, use the {@link #connect(rx.functions.Action1)}
     * method.
     * @return the subscription representing the connection
     */
    public final Subscription connect() {
        final Subscription[] out = new Subscription[1];
        connect(new Action1<Subscription>() {
            @Override
            public void call(Subscription t1) {
                out[0] = t1;
            }
        });
        return out[0];
    }
    /**
     * Call a ConnectableObservable's connect() method to instruct it to begin emitting the
     * items from its underlying {@link Observable} to its {@link Subscriber}s.
     * @param connection the action that receives the connection subscription
     * before the subscription to source happens allowing the caller
     * to synchronously disconnect a synchronous source.
     */
    public abstract void connect(Action1<? super Subscription> connection);
    /**
     * Returns an observable sequence that stays connected to the source as long
     * as there is at least one subscription to the observable sequence.
     * 
     * @return a {@link Observable}
     */
    public Observable<T> refCount() {
        return create(new OperatorRefCount<T>(this));
    }
}
