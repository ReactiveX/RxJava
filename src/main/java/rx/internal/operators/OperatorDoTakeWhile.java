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
import rx.annotations.Experimental;
import rx.functions.Func1;

/**
 * Returns an Observable that emits items emitted by the source Observable until
 * the provided predicate returns false
 * <p>
 */
@Experimental
public final class OperatorDoTakeWhile<T> implements Operator<T, T> {

    private final Func1<? super T, Boolean> predicate;

    public OperatorDoTakeWhile(final Func1<? super T, Boolean> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        Subscriber<T> parent = new Subscriber<T>() {

            private boolean done = false;

            @Override
            public void onNext(T args) {
                child.onNext(args);
                
                boolean doContinue = false;
                try {
                    doContinue = predicate.call(args);
                } catch (Throwable e) {
                    done = true;
                    child.onError(e);
                    unsubscribe();
                    return;
                }
                if (!doContinue) {
                    done = true;
                    child.onCompleted();
                    unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                if (!done) {
                    child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!done) {
                    child.onError(e);
                }
            }

        };
        child.add(parent); // don't unsubscribe downstream
        
        return parent;
    }

}
