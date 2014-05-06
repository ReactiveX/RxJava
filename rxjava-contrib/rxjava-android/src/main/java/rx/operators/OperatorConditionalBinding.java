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
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Functions;

import android.util.Log;

/**
 * Ties a source sequence to the given target object using a predicate. If the predicate fails
 * to validate, the sequence unsubscribes itself and releases the bound reference.
 * <p/>
 * You can also pass in an optional predicate function, which whenever it evaluates to false
 * on the target object, will also result in the operator unsubscribing from the sequence.
 *
 * @param <T> the type of the objects emitted to a subscriber
 * @param <R> the type of the target object to bind to
 */
public final class OperatorConditionalBinding<T, R> implements Observable.Operator<T, T> {

    private static final String LOG_TAG = "ConditionalBinding";

    private R boundRef;
    private final Func1<? super R, Boolean> predicate;

    public OperatorConditionalBinding(R bound, Func1<? super R, Boolean> predicate) {
        boundRef = bound;
        this.predicate = predicate;
    }

    public OperatorConditionalBinding(R bound) {
        boundRef = bound;
        this.predicate = Functions.alwaysTrue();
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new  Subscriber<T>(child) {

            @Override
            public void onCompleted() {
                if (shouldForwardNotification()) {
                    child.onCompleted();
                } else {
                    handleLostBinding("onCompleted");
                }
            }

            @Override
            public void onError(Throwable e) {
                if (shouldForwardNotification()) {
                    child.onError(e);
                } else {
                    handleLostBinding("onError");
                }
            }

            @Override
            public void onNext(T t) {
                if (shouldForwardNotification()) {
                    child.onNext(t);
                } else {
                    handleLostBinding("onNext");
                }
            }

            private boolean shouldForwardNotification() {
                return boundRef != null && predicate.call(boundRef);
            }

            private void handleLostBinding(String context) {
                log("bound object has become invalid; skipping " + context);
                log("unsubscribing...");
                boundRef = null;
                unsubscribe();
            }

            private void log(String message) {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                    Log.d(LOG_TAG, message);
                }
            }
        };
    }

    /* Visible for testing */
    R getBoundRef() {
        return boundRef;
    }
}
