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

import org.junit.Test;

public abstract class FlowableProcessorTest<T> {

    protected abstract FlowableProcessor<T> create();

    @Test
    public void onNextNull() {
        final FlowableProcessor<T> p = create();

        p.onNext(null);

        p.test()
                .assertNoValues()
                .assertError(NullPointerException.class)
                .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNull() {
        final FlowableProcessor<T> p = create();

        p.onError(null);

        p.test()
                .assertNoValues()
                .assertError(NullPointerException.class)
                .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }
}
