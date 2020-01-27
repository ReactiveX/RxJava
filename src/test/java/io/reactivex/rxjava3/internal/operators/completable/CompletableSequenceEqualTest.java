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

package io.reactivex.rxjava3.internal.operators.completable;

import org.junit.Test;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.exceptions.TestException;

public class CompletableSequenceEqualTest {

    @Test
    public void bothComplete() {
        Completable.sequenceEqual(Completable.complete(), Completable.complete())
        .test()
        .assertResult(true);
    }

    @Test
    public void firstFails() {
        Completable.sequenceEqual(Completable.error(new TestException()), Completable.complete())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void secondFails() {
        Completable.sequenceEqual(Completable.complete(), Completable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }
}
