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


import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;
import rx.internal.producers.SingleDelayedProducer;

/**
 * Returns an {@link Observable} that emits <code>true</code> if any element of
 * an observable sequence satisfies a condition, otherwise <code>false</code>.
 */
public final class OperatorAny<T> implements Operator<Boolean, T> {
    private final Func1<? super T, Boolean> predicate;
    private final boolean returnOnEmpty;

    public OperatorAny(Func1<? super T, Boolean> predicate, boolean returnOnEmpty) {
        this.predicate = predicate;
        this.returnOnEmpty = returnOnEmpty;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super Boolean> child) {
        final SingleDelayedProducer<Boolean> producer = new SingleDelayedProducer<Boolean>(child);
        Subscriber<T> s = new Subscriber<T>() {
            boolean hasElements;
            boolean done;

            @Override
            public void onNext(T t) {
                hasElements = true;
                boolean result;
                try {
                    result = predicate.call(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(OnErrorThrowable.addValueAsLastCause(e, t));
                    return;
                }
                if (result && !done) {
                    done = true;
                    producer.setValue(!returnOnEmpty);
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
                    if (hasElements) {
                        producer.setValue(false);
                    } else {
                        producer.setValue(returnOnEmpty);
                    }
                }
            }

        };
        child.add(s);
        child.setProducer(producer);
        return s;
    }
}
