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

package io.reactivex.rxjava4.internal.operators.single;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class SingleFlatMapMaybeTest extends RxJavaTest {
    @Test
    public void flatMapMaybeValue() {
        Single.just(1).flatMapMaybe((Function<Integer, MaybeSource<Integer>>) integer -> {
            if (integer == 1) {
                return Maybe.just(2);
            }

            return Maybe.just(1);
        })
            .test()
            .assertResult(2);
    }

    @Test
    public void flatMapMaybeValueDifferentType() {
        Single.just(1).flatMapMaybe((Function<Integer, MaybeSource<String>>) integer -> {
            if (integer == 1) {
                return Maybe.just("2");
            }

            return Maybe.just("1");
        })
            .test()
            .assertResult("2");
    }

    @Test
    public void flatMapMaybeValueNull() {
        Single.just(1).flatMapMaybe((Function<Integer, MaybeSource<Integer>>) _ -> null)
            .to(TestHelper.<Integer>testConsumer())
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("The mapper returned a null MaybeSource");
    }

    @Test
    public void flatMapMaybeValueErrorThrown() {
        Single.just(1).flatMapMaybe((Function<Integer, MaybeSource<Integer>>) _ -> {
            throw new RuntimeException("something went terribly wrong!");
        })
            .to(TestHelper.<Integer>testConsumer())
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("something went terribly wrong!");
    }

    @Test
    public void flatMapMaybeError() {
        RuntimeException exception = new RuntimeException("test");

        Single.error(exception).flatMapMaybe((Function<Object, MaybeSource<Object>>) _ -> Maybe.just(new Object()))
            .test()
            .assertError(exception);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).flatMapMaybe((Function<Integer, MaybeSource<Integer>>) _ -> Maybe.just(1)));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe((Function<Single<Integer>, MaybeSource<Integer>>) v ->
            v.flatMapMaybe((Function<Integer, MaybeSource<Integer>>) _ -> Maybe.just(1)));
    }

    @Test
    public void mapsToError() {
        Single.just(1).flatMapMaybe((Function<Integer, MaybeSource<Integer>>) _ -> Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapsToEmpty() {
        Single.just(1).flatMapMaybe((Function<Integer, MaybeSource<Integer>>) _ -> Maybe.empty())
        .test()
        .assertResult();
    }
}
