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
import rx.functions.Func1;

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
        return new Subscriber<T>(child) {
            boolean hasElements;
            boolean done;
            @Override
            public void onNext(T t) {
                hasElements = true;
                boolean result = predicate.call(t);
                if (result && !done) {
                    done = true;
                    child.onNext(!returnOnEmpty);
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
                    if (hasElements) {
                        child.onNext(false);
                    } else {
                        child.onNext(returnOnEmpty);
                    }
                    child.onCompleted();
                }
            }
            
        };
    }
}
