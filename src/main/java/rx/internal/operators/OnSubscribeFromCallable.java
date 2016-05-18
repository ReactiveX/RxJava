/**
 * Copyright 2016 Netflix, Inc.
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
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.internal.producers.SingleDelayedProducer;

import java.util.concurrent.Callable;

/**
 * Do not invoke the function until an Observer subscribes; Invokes function on each
 * subscription.
 * <p>
 * Pass {@code fromCallable} a function, and {@code fromCallable} will call this function to emit result of invocation
 * afresh each time a new Observer subscribes.
 * @param <T> the value type emitted
 */
public final class OnSubscribeFromCallable<T> implements Observable.OnSubscribe<T> {

    private final Callable<? extends T> resultFactory;

    public OnSubscribeFromCallable(Callable<? extends T> resultFactory) {
        this.resultFactory = resultFactory;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        final SingleDelayedProducer<T> singleDelayedProducer = new SingleDelayedProducer<T>(subscriber);

        subscriber.setProducer(singleDelayedProducer);

        try {
            singleDelayedProducer.setValue(resultFactory.call());
        } catch (Throwable t) {
            Exceptions.throwOrReport(t, subscriber);
        }
    }
}
