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

package io.reactivex.rxjava4.internal.operators.maybe;

import org.junit.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class MaybeFlattenTest extends RxJavaTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.just(1).flatMap(_ -> Maybe.just(2)));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe((Function<Maybe<Integer>, MaybeSource<Integer>>) v ->
                v.flatMap(_ -> Maybe.just(2)));
    }

    @Test
    public void mainError() {
        Maybe.<Integer>error(new TestException())
        .flatMap(_ -> Maybe.just(2))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainEmpty() {
        Maybe.<Integer>empty()
        .flatMap(_ -> Maybe.just(2))
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrows() {
        Maybe.just(1)
        .flatMap(_ -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperReturnsNull() {
        Maybe.just(1)
        .flatMap(_ -> null)
        .test()
        .assertFailure(NullPointerException.class);
    }
}
