/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.parallel;

import org.reactivestreams.Subscriber;

import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * Signals two onErrors to each subscriber for testing purposes.
 */
public final class ParallelInvalid extends ParallelFlowable<Object> {

    @Override
    public void subscribe(Subscriber<? super Object>[] subscribers) {
        TestException ex = new TestException();
        for (Subscriber<? super Object> s : subscribers) {
            EmptySubscription.error(ex, s);
            s.onError(ex);
            s.onNext(0);
            s.onComplete();
            s.onComplete();
        }
    }

    @Override
    public int parallelism() {
        return 4;
    }

}
