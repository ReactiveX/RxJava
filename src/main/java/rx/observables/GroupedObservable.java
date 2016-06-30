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
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * An {@link Observable} that has been grouped by key, the value of which can be obtained with {@link #getKey()}.
 * <p>
 * <em>Note:</em> A {@link GroupedObservable} will cache the items it is to emit until such time as it
 * is subscribed to. For this reason, in order to avoid memory leaks, you should not simply ignore those
 * {@code GroupedObservable}s that do not concern you. Instead, you can signal to them that they
 * may discard their buffers by applying an operator like {@link Observable#take take}{@code (0)} to them.
 * 
 * @param <K>
 *            the type of the key
 * @param <T>
 *            the type of the items emitted by the {@code GroupedObservable}
 * @see Observable#groupBy(Func1)
 * @see <a href="http://reactivex.io/documentation/operators/groupby.html">ReactiveX documentation: GroupBy</a>
 */
public class GroupedObservable<K, T> extends Observable<T> {
    private final K key;

    /**
     * Converts an {@link Observable} into a {@code GroupedObservable} with a particular key.
     * 
     * @param <K> the key type
     * @param <T> the value type
     * @param key
     *            the key to identify the group of items emitted by this {@code GroupedObservable}
     * @param o
     *            the {@link Observable} to convert
     * @return a {@code GroupedObservable} representation of {@code o}, with key {@code key}
     */
    public static <K, T> GroupedObservable<K, T> from(K key, final Observable<T> o) {
        return new GroupedObservable<K, T>(key, new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> s) {
                o.unsafeSubscribe(s);
            }
        });
    }

    /**
     * Returns an Observable that will execute the specified function when a {@link Subscriber} subscribes to
     * it.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/create.png" alt="">
     * <p>
     * Write the function you pass to {@code create} so that it behaves as an Observable: It should invoke the
     * Subscriber's {@link Subscriber#onNext onNext}, {@link Subscriber#onError onError}, and {@link Subscriber#onCompleted onCompleted} methods appropriately.
     * <p>
     * A well-formed Observable must invoke either the Subscriber's {@code onCompleted} method exactly once or
     * its {@code onError} method exactly once.
     * <p>
     * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a> for detailed
     * information.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <K>
     *            the type of the key
     * @param <T>
     *            the type of the items that this Observable emits
     * @param key the key value
     * @param f
     *            a function that accepts an {@code Subscriber<T>}, and invokes its {@code onNext}, {@code onError}, and {@code onCompleted} methods as appropriate
     * @return a GroupedObservable that, when a {@link Subscriber} subscribes to it, will execute the specified
     *         function
     */
    public static <K, T> GroupedObservable<K, T> create(K key, OnSubscribe<T> f) {
        return new GroupedObservable<K, T>(key, f);
    }

    protected GroupedObservable(K key, OnSubscribe<T> onSubscribe) {
        super(onSubscribe);
        this.key = key;
    }

    /**
     * Returns the key that identifies the group of items emitted by this {@code GroupedObservable}
     * 
     * @return the key that the items emitted by this {@code GroupedObservable} were grouped by
     */
    public K getKey() {
        return key;
    }
}
