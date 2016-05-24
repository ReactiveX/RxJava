/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.SerialSubscription;

/**
 * Delays the subscription to the Single until the Observable
 * fires an event or completes.
 *
 * @param <T> the Single value type
 */
public final class SingleOnSubscribeDelaySubscriptionOther<T> implements Single.OnSubscribe<T> {
    final Single<? extends T> main;
    final Observable<?> other;

    public SingleOnSubscribeDelaySubscriptionOther(Single<? extends T> main, Observable<?> other) {
        this.main = main;
        this.other = other;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void call(final SingleSubscriber<? super T> subscriber) {
        final SingleSubscriber<T> child = new SingleSubscriber<T>() {
            @Override
            public void onSuccess(T value) {
                subscriber.onSuccess(value);
            }

            @Override
            public void onError(Throwable error) {
                subscriber.onError(error);
            }
        };

        final SerialSubscription serial = new SerialSubscription();
        subscriber.add(serial);

        Subscriber<Object> otherSubscriber = new Subscriber<Object>() {
            boolean done;
            @Override
            public void onNext(Object t) {
                onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                if (done) {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                    return;
                }
                done = true;
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                if (done) {
                    return;
                }
                done = true;
                serial.set(child);

                main.subscribe(child);
            }
        };

        serial.set(otherSubscriber);

        ((Observable<Object>)other).subscribe(otherSubscriber);
    }
}
