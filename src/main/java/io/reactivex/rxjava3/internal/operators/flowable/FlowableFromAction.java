/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.operators.flowable;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.CancellableQueueFuseable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Executes an {@link Action} and signals its exception or completes normally.
 *
 * @param <T> the value type
 * @since 3.0.0
 */
public final class FlowableFromAction<T> extends Flowable<T> implements Supplier<T> {

    final Action action;

    public FlowableFromAction(Action action) {
        this.action = action;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> subscriber) {
        CancellableQueueFuseable<T> qs = new CancellableQueueFuseable<>();
        subscriber.onSubscribe(qs);

        if (!qs.isDisposed()) {

            try {
                action.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                if (!qs.isDisposed()) {
                    subscriber.onError(ex);
                } else {
                    RxJavaPlugins.onError(ex);
                }
                return;
            }

            if (!qs.isDisposed()) {
                subscriber.onComplete();
            }
        }
    }

    @Override
    public T get() throws Throwable {
        action.run();
        return null; // considered as onComplete()
    }
}
