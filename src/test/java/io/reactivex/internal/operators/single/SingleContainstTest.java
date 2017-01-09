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

package io.reactivex.internal.operators.single;

import org.junit.Test;

import io.reactivex.Single;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.BiPredicate;

public class SingleContainstTest {

    @Test
    public void comparerThrows() {
        Single.just(1)
        .contains(2, new BiPredicate<Object, Object>() {
            @Override
            public boolean test(Object a, Object b) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void error() {
        Single.error(new TestException())
        .contains(2)
        .test()
        .assertFailure(TestException.class);
    }
}
