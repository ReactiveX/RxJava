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

package io.reactivex.rxjava3.internal.operators.maybe;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;

public class MaybeFromPubisherTest extends RxJavaTest {

    @Test
    public void empty() {
        Maybe.fromPublisher(Flowable.empty().hide())
            .test()
            .assertResult();
    }

    @Test
    public void just() {
        Maybe.fromPublisher(Flowable.just(1).hide())
            .test()
            .assertResult(1);
    }

    @Test
    public void range() {
        Maybe.fromPublisher(Flowable.range(1, 5).hide())
            .test()
            .assertResult(1);
    }

    @Test
    public void error() {
        Maybe.fromPublisher(Flowable.error(new TestException()).hide())
            .test()
            .assertFailure(TestException.class);
    }
}
