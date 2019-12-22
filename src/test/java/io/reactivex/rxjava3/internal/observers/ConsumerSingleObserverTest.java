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

package io.reactivex.rxjava3.internal.observers;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.internal.functions.Functions;

public final class ConsumerSingleObserverTest extends RxJavaTest {

    @Test
    public void onErrorMissingShouldReportNoCustomOnError() {
        ConsumerSingleObserver<Integer> o = new ConsumerSingleObserver<>(Functions.<Integer>emptyConsumer(),
                Functions.ON_ERROR_MISSING);

        assertFalse(o.hasCustomOnError());
    }

    @Test
    public void customOnErrorShouldReportCustomOnError() {
        ConsumerSingleObserver<Integer> o = new ConsumerSingleObserver<>(Functions.<Integer>emptyConsumer(),
                Functions.<Throwable>emptyConsumer());

        assertTrue(o.hasCustomOnError());
    }

}
