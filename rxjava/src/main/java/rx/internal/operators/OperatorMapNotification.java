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
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Applies a function of your choosing to every item emitted by an {@code Observable}, and emits the results of
 * this transformation as a new {@code Observable}.
 * <p>
 * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/map.png" alt="">
 */
public final class OperatorMapNotification<T, R> implements Operator<R, T> {

    private final Func1<? super T, ? extends R> onNext;
    private final Func1<? super Throwable, ? extends R> onError;
    private final Func0<? extends R> onCompleted;

    public OperatorMapNotification(Func1<? super T, ? extends R> onNext, Func1<? super Throwable, ? extends R> onError, Func0<? extends R> onCompleted) {
        this.onNext = onNext;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> o) {
        return new Subscriber<T>(o) {

            @Override
            public void onCompleted() {
                try {
                    o.onNext(onCompleted.call());
                    o.onCompleted();
                } catch (Throwable e) {
                    o.onError(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                try {
                    o.onNext(onError.call(e));
                    o.onCompleted();
                } catch (Throwable e2) {
                    o.onError(e);
                }
            }

            @Override
            public void onNext(T t) {
                try {
                    o.onNext(onNext.call(t));
                } catch (Throwable e) {
                    o.onError(OnErrorThrowable.addValueAsLastCause(e, t));
                }
            }

        };
    }

}
