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

package io.reactivex.processors;

import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public abstract class DelayedFlowableProcessorTest<T> extends FlowableProcessorTest<T> {

    @Test
    public void onNextNullDelayed() {
        final FlowableProcessor<T> p = create();

        TestSubscriber<T> ts = p.test();

        p.onNext(null);



        ts
                .assertNoValues()
                .assertError(NullPointerException.class)
                .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNullDelayed() {
        final FlowableProcessor<T> p = create();

        TestSubscriber<T> ts = p.test();

        p.onError(null);
        assertFalse(p.hasSubscribers());

        ts
                .assertNoValues()
                .assertError(NullPointerException.class)
                .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }
}
