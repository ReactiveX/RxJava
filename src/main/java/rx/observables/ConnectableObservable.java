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

import rx.*;
import rx.annotations.Beta;
import rx.functions.*;
import rx.internal.operators.*;

/**
 * A {@code ConnectableObservable} resembles an ordinary {@link Observable}, except that it does not begin
 * emitting items when it is subscribed to, but only when its {@link #connect} method is called. In this way you
 * can wait for all intended {@link Subscriber}s to {@link Observable#subscribe} to the {@code Observable}
 * before the {@code Observable} begins emitting items.
 * <p>
 * <img width="640" height="510" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/publishConnect.png" alt="">
 * 
 * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators">RxJava Wiki:
 *      Connectable Observable Operators</a>
 * @param <T>
 *          the type of items emitted by the {@code ConnectableObservable}
 */
public abstract class ConnectableObservable<T> extends Observable<T> {

    protected ConnectableObservable(OnSubscribe<T> onSubscribe) {
        super(onSubscribe);
    }

    /**
     * Instructs the {@code ConnectableObservable} to begin emitting the items from its underlying
     * {@link Observable} to its {@link Subscriber}s.
     * <p>
     * To disconnect from a synchronous source, use the {@link #connect(rx.functions.Action1)} method.
     *
     * @return the subscription representing the connection
     * @see <a href="http://reactivex.io/documentation/operators/connect.html">ReactiveX documentation: Connect</a>
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
     * Instructs the {@code ConnectableObservable} to begin emitting the items from its underlying
     * {@link Observable} to its {@link Subscriber}s.
     *
     * @param connection
     *          the action that receives the connection subscription before the subscription to source happens
     *          allowing the caller to synchronously disconnect a synchronous source
     * @see <a href="http://reactivex.io/documentation/operators/connect.html">ReactiveX documentation: Connect</a>
     */
    public abstract void connect(Action1<? super Subscription> connection);

    /**
     * Returns an {@code Observable} that stays connected to this {@code ConnectableObservable} as long as there
     * is at least one subscription to this {@code ConnectableObservable}.
     * 
     * @return a {@link Observable}
     * @see <a href="http://reactivex.io/documentation/operators/refcount.html">ReactiveX documentation: RefCount</a>
     */
    public Observable<T> refCount() {
        return create(new OnSubscribeRefCount<T>(this));
    }
    
    /**
     * Returns an Observable that automatically connects to this ConnectableObservable
     * when the first Subscriber subscribes.
     * 
     * @return an Observable that automatically connects to this ConnectableObservable
     *         when the first Subscriber subscribes
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
     */
    @Beta
    public Observable<T> autoConnect() {
        return autoConnect(1);
    }
    /**
     * Returns an Observable that automatically connects to this ConnectableObservable
     * when the specified number of Subscribers subscribe to it.
     * 
     * @param numberOfSubscribers the number of subscribers to await before calling connect
     *                            on the ConnectableObservable. A non-positive value indicates
     *                            an immediate connection.
     * @return an Observable that automatically connects to this ConnectableObservable
     *         when the specified number of Subscribers subscribe to it
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
     */
    @Beta
    public Observable<T> autoConnect(int numberOfSubscribers) {
        return autoConnect(numberOfSubscribers, Actions.empty());
    }
    
    /**
     * Returns an Observable that automatically connects to this ConnectableObservable
     * when the specified number of Subscribers subscribe to it and calls the 
     * specified callback with the Subscription associated with the established connection.
     * 
     * @param numberOfSubscribers the number of subscribers to await before calling connect
     *                            on the ConnectableObservable. A non-positive value indicates
     *                            an immediate connection.
     * @param connection the callback Action1 that will receive the Subscription representing the
     *                   established connection
     * @return an Observable that automatically connects to this ConnectableObservable
     *         when the specified number of Subscribers subscribe to it and calls the 
     *         specified callback with the Subscription associated with the established connection
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
     */
    @Beta
    public Observable<T> autoConnect(int numberOfSubscribers, Action1<? super Subscription> connection) {
        if (numberOfSubscribers <= 0) {
            this.connect(connection);
            return this;
        }
        return create(new OnSubscribeAutoConnect<T>(this, numberOfSubscribers, connection));
    }
}
