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

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Returns an Observable that emits a Boolean that indicates whether all items emitted by an
 * Observable satisfy a condition.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
 */
public final class OperatorAll<T> implements Operator<Boolean, T> {
    private final Func1<? super T, Boolean> predicate;

    public OperatorAll(Func1<? super T, Boolean> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super Boolean> child) {
        return new Subscriber<T>(child) {
            boolean done;

            @Override
            public void onNext(T t) {
                boolean result = predicate.call(t);
                if (!result && !done) {
                    done = true;
                    child.onNext(false);
                    child.onCompleted();
                    unsubscribe();
                }
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                if (!done) {
                    done = true;
                    child.onNext(true);
                    child.onCompleted();
                }
            }
        };
    }
}
