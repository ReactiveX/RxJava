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

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.internal.producers.SingleDelayedProducer;

/**
 * Returns an Observable that emits a Boolean that indicates whether all items emitted by an
 * Observable satisfy a condition.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/all.png" alt="">
 * @param <T> the value type
 */
public final class OperatorAll<T> implements Operator<Boolean, T> {
    final Func1<? super T, Boolean> predicate;

    public OperatorAll(Func1<? super T, Boolean> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super Boolean> child) {
        final SingleDelayedProducer<Boolean> producer = new SingleDelayedProducer<Boolean>(child);
        Subscriber<T> s = new Subscriber<T>() {
            boolean done;

            @Override
            public void onNext(T t) {
                Boolean result;
                try {
                    result = predicate.call(t);
                } catch (Throwable e) {
                    Exceptions.throwOrReport(e, this, t);
                    return;
                }
                if (!result && !done) {
                    done = true;
                    producer.setValue(false);
                    unsubscribe();
                } 
                // note that don't need to request more of upstream because this subscriber 
                // defaults to requesting Long.MAX_VALUE
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                if (!done) {
                    done = true;
                    producer.setValue(true);
                }
            }
        };
        child.add(s);
        child.setProducer(producer);
        return s;
    }
}