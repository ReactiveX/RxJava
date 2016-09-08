/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import rx.Subscriber;
import rx.plugins.RxJavaHooks;

/**
 * Supplements {@code DeferredScalarSubscriber} with defensive behaviour that ensures no emissions
 * occur after a terminal event. If {@code onError} is called more than once then errors after the first
 * are reported to {@code RxJavaHooks.onError}.
 *
 * @param <T> source value type
 * @param <R> result value type
 */
public abstract class DeferredScalarSubscriberSafe<T, R> extends DeferredScalarSubscriber<T,R> {

    protected boolean done;

    public DeferredScalarSubscriberSafe(Subscriber<? super R> actual) {
        super(actual);
    }

    @Override
    public void onError(Throwable ex) {
        if (!done) {
            done = true;
            super.onError(ex);
        } else {
            RxJavaHooks.onError(ex);
        }
    }

    @Override
    public void onCompleted() {
        if (done) {
            return;
        }
        done = true;
        super.onCompleted();
    }

}
