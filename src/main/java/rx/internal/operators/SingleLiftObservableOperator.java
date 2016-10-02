/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.operators;

import rx.*;
import rx.Observable.Operator;
import rx.Single.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.internal.operators.SingleFromObservable.WrapSingleIntoSubscriber;
import rx.internal.producers.SingleProducer;
import rx.plugins.RxJavaHooks;

/**
 * Lift an Observable.Operator into the Single sequence.
 * @param <T> the input value type
 * @param <R> the output value type
 */
public final class SingleLiftObservableOperator<T, R> implements Single.OnSubscribe<R> {

    final Single.OnSubscribe<T> source;

    final Operator<? extends R, ? super T> lift;

    public SingleLiftObservableOperator(OnSubscribe<T> source, Operator<? extends R, ? super T> lift) {
        this.source = source;
        this.lift = lift;
    }

    @Override
    public void call(SingleSubscriber<? super R> t) {
        Subscriber<R> outputAsSubscriber = new WrapSingleIntoSubscriber<R>(t);
        t.add(outputAsSubscriber);

        try {
            Subscriber<? super T> inputAsSubscriber = RxJavaHooks.onSingleLift(lift).call(outputAsSubscriber);

            SingleSubscriber<? super T> input = wrap(inputAsSubscriber);

            inputAsSubscriber.onStart();

            source.call(input);
        } catch (Throwable ex) {
            Exceptions.throwOrReport(ex, t);
        }
    }

    public static <T> SingleSubscriber<T> wrap(Subscriber<T> subscriber) {
        WrapSubscriberIntoSingle<T> parent = new WrapSubscriberIntoSingle<T>(subscriber);
        subscriber.add(parent);
        return parent;
    }

    static final class WrapSubscriberIntoSingle<T> extends SingleSubscriber<T> {
        final Subscriber<? super T> actual;

        WrapSubscriberIntoSingle(Subscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onSuccess(T value) {
            actual.setProducer(new SingleProducer<T>(actual, value));
        }

        @Override
        public void onError(Throwable error) {
            actual.onError(error);
        }
    }
}
