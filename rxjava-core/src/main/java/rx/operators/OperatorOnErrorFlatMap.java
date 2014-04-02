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
package rx.operators;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

/**
 * Allows inserting onNext events into a stream when onError events are received
 * and continuing the original sequence instead of terminating. Thus it allows a sequence
 * with multiple onError events.
 */
public final class OperatorOnErrorFlatMap<T> implements Operator<T, T> {

    private final Func1<OnErrorThrowable, ? extends Observable<? extends T>> resumeFunction;

    public OperatorOnErrorFlatMap(Func1<OnErrorThrowable, ? extends Observable<? extends T>> f) {
        this.resumeFunction = f;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                try {
                    Observable<? extends T> resume = resumeFunction.call(OnErrorThrowable.from(e));
                    resume.unsafeSubscribe(new Subscriber<T>() {

                        @Override
                        public void onCompleted() {
                            // ignore as we will continue the parent Observable
                        }

                        @Override
                        public void onError(Throwable e) {
                            // if the splice also fails we shut it all down
                            child.onError(e);
                        }

                        @Override
                        public void onNext(T t) {
                            child.onNext(t);
                        }

                    });
                } catch (Throwable e2) {
                    child.onError(e2);
                }
            }

            @Override
            public void onNext(T t) {
                child.onNext(t);
            }

        };
    }

}
