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

package io.reactivex.rxjava3.internal.operators.single;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleMapTest extends RxJavaTest {

    @Test
    public void mapValue() {
        Single.just(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(final Integer integer) throws Exception {
                if (integer == 1) {
                    return 2;
                }

                return 1;
            }
        })
        .test()
        .assertResult(2);
    }

    @Test
    public void mapValueNull() {
        Single.just(1).map(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(final Integer integer) throws Exception {
                return null;
            }
        })
        .to(TestHelper.<SingleSource<Integer>>testConsumer())
        .assertNoValues()
        .assertError(NullPointerException.class)
        .assertErrorMessage("The mapper function returned a null value.");
    }

    @Test
    public void mapValueErrorThrown() {
        Single.just(1).map(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(final Integer integer) throws Exception {
                throw new RuntimeException("something went terribly wrong!");
            }
        })
        .to(TestHelper.<SingleSource<Integer>>testConsumer())
        .assertNoValues()
        .assertError(RuntimeException.class)
        .assertErrorMessage("something went terribly wrong!");
    }

    @Test
    public void mapError() {
        RuntimeException exception = new RuntimeException("test");

        Single.error(exception).map(new Function<Object, Object>() {
            @Override
            public Object apply(final Object integer) throws Exception {
                return new Object();
            }
        })
        .test()
        .assertError(exception);
    }
}
