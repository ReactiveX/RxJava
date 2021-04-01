/*
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

package io.reactivex.rxjava3.internal.operators.completable;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableAndThenTest extends RxJavaTest {
    @Test
    public void andThenMaybeCompleteValue() {
        Completable.complete()
            .andThen(Maybe.just(1))
            .test()
            .assertResult(1);
    }

    @Test
    public void andThenMaybeCompleteError() {
        Completable.complete()
            .andThen(Maybe.error(new RuntimeException("test")))
            .to(TestHelper.testConsumer())
            .assertNotComplete()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("test");
    }

    @Test
    public void andThenMaybeCompleteEmpty() {
        Completable.complete()
            .andThen(Maybe.empty())
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete();
    }

    @Test
    public void andThenMaybeError() {
        Completable.error(new RuntimeException("bla"))
            .andThen(Maybe.empty())
            .to(TestHelper.testConsumer())
            .assertNotComplete()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("bla");
    }
}
