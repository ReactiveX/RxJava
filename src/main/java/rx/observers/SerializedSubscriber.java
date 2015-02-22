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
package rx.observers;

import rx.Observer;
import rx.Subscriber;

/**
 * Enforces single-threaded, serialized, ordered execution of {@link #onNext}, {@link #onCompleted}, and
 * {@link #onError}.
 * <p>
 * When multiple threads are emitting and/or notifying they will be serialized by:
 * </p><ul>
 * <li>Allowing only one thread at a time to emit</li>
 * <li>Adding notifications to a queue if another thread is already emitting</li>
 * <li>Not holding any locks or blocking any threads while emitting</li>
 * </ul>
 * 
 * @param <T>
 *          the type of items expected to be emitted to the {@code Subscriber}
 */
public class SerializedSubscriber<T> extends Subscriber<T> {

    private final Observer<T> s;

    public SerializedSubscriber(Subscriber<? super T> s) {
        this(s, true);
    }

    /**
     * Constructor for wrapping and serializing a subscriber optionally sharing the same underlying subscription
     * list.
     *
     * @param s
     *          the subscriber to wrap and serialize
     * @param shareSubscriptions
     *          if {@code true}, the same subscription list is shared between this subscriber and {@code s}.
     * @since 1.0.7
     */
    public SerializedSubscriber(Subscriber<? super T> s, boolean shareSubscriptions) {
        super(s, shareSubscriptions);
        this.s = new SerializedObserver<T>(s);
    }

    /**
     * Notifies the Subscriber that the {@code Observable} has finished sending push-based notifications.
     * <p>
     * The {@code Observable} will not call this method if it calls {@link #onError}.
     */
    @Override
    public void onCompleted() {
        s.onCompleted();
    }

    /**
     * Notifies the Subscriber that the {@code Observable} has experienced an error condition.
     * <p>
     * If the {@code Observable} calls this method, it will not thereafter call {@link #onNext} or
     * {@link #onCompleted}.
     * 
     * @param e
     *          the exception encountered by the Observable
     */
    @Override
    public void onError(Throwable e) {
        s.onError(e);
    }

    /**
     * Provides the Subscriber with a new item to observe.
     * <p>
     * The {@code Observable} may call this method 0 or more times.
     * <p>
     * The {@code Observable} will not call this method again after it calls either {@link #onCompleted} or
     * {@link #onError}.
     * 
     * @param t
     *          the item emitted by the Observable
     */
    @Override
    public void onNext(T t) {
        s.onNext(t);
    }
}
