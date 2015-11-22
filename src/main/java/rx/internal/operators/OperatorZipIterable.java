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
package rx.internal.operators;

import java.util.Iterator;

import rx.Observable.Operator;
import rx.exceptions.Exceptions;
import rx.Subscriber;
import rx.functions.Func2;
import rx.observers.Subscribers;

public final class OperatorZipIterable<T1, T2, R> implements Operator<R, T1> {

    final Iterable<? extends T2> iterable;
    final Func2<? super T1, ? super T2, ? extends R> zipFunction;

    public OperatorZipIterable(Iterable<? extends T2> iterable, Func2<? super T1, ? super T2, ? extends R> zipFunction) {
        this.iterable = iterable;
        this.zipFunction = zipFunction;
    }

    @Override
    public Subscriber<? super T1> call(final Subscriber<? super R> subscriber) {
        final Iterator<? extends T2> iterator = iterable.iterator();
        try {
            if (!iterator.hasNext()) {
                subscriber.onCompleted();
                return Subscribers.empty();
            }
        } catch (Throwable e) {
            Exceptions.throwOrReport(e, subscriber);
            return Subscribers.empty();
        }
        return new Subscriber<T1>(subscriber) {
            boolean done;
            @Override
            public void onCompleted() {
                if (done) {
                    return;
                }
                done = true;
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                if (done) {
                    Exceptions.throwIfFatal(e);
                    return;
                }
                done = true;
                subscriber.onError(e);
            }

            @Override
            public void onNext(T1 t) {
                if (done) {
                    return;
                }
                try {
                    subscriber.onNext(zipFunction.call(t, iterator.next()));
                    if (!iterator.hasNext()) {
                        onCompleted();
                    }
                } catch (Throwable e) {
                    Exceptions.throwOrReport(e, this);
                }
            }

        };
    }

}
