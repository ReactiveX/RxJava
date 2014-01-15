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
package rx;

import rx.util.functions.Function;


/**
 * The Observable interface that implements the Reactive Pattern.
 * <p>
 * The documentation for this interface and its implementations makes use of
 * marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/legend.png">
 * <p>
 * For more information see the
 * <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
 *
 * @param <T> the type of the item emitted by the Observable.
 */
public interface IObservable<T> extends Function {

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in
     * order to receive items and notifications from the Observable.
     * <p>
     * A typical implementation of {@code subscribe} does the following:
     * <ol>
     * <li>It stores a reference to the Observer in a collection object, such as
     *     a {@code List<T>} object.</li>
     * <li>It returns a reference to the {@link Subscription} interface. This
     *     enables Observers to unsubscribe, that is, to stop receiving items
     *     and notifications before the Observable stops sending them, which
     *     also invokes the Observer's {@link Observer#onCompleted onCompleted}
     *     method.</li>
     * </ol><p>
     * An <code>IObservable&lt;T&gt;</code> instance is responsible for accepting
     * all subscriptions and notifying all Observers. Unless the documentation
     * for a particular <code>IObservable&lt;T&gt;</code> implementation
     * indicates otherwise, Observers should make no assumptions about the order
     * in which multiple Observers will receive their notifications.
     * <p>
     * For more information see the
     * <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
     *
     * @param observer the Observer
     * @return a {@link Subscription} reference with which the {@link Observer}
     *         can stop receiving items before the Observable has finished
     *         sending them
     * @throws IllegalArgumentException if the {@link Observer} provided as the
     *                                  argument to {@code subscribe()} is
     *                                  {@code null}.
     */
    public abstract Subscription subscribe(Observer<? super T> observer);

}
