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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class MaybeFlatMapBiSelectorTest extends RxJavaTest {

    BiFunction<Integer, Integer, String> stringCombine() {
        return (a, b) -> a + ":" + b;
    }

    @Test
    public void normal() {
        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> Maybe.just(2), stringCombine())
        .test()
        .assertResult("1:2");
    }

    @Test
    public void normalWithEmpty() {
        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> Maybe.empty(), stringCombine())
        .test()
        .assertResult();
    }

    @Test
    public void emptyWithJust() {
        final int[] call = { 0 };

        Maybe.<Integer>empty()
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> {
            call[0]++;
            return Maybe.just(1);
        }, stringCombine())
        .test()
        .assertResult();

        assertEquals(0, call[0]);
    }

    @Test
    public void errorWithJust() {
        final int[] call = { 0 };

        Maybe.<Integer>error(new TestException())
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> {
            call[0]++;
            return Maybe.just(1);
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void justWithError() {
        final int[] call = { 0 };

        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> {
            call[0]++;
            return Maybe.<Integer>error(new TestException());
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, call[0]);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().singleElement()
                .flatMap((Function<Object, MaybeSource<Integer>>) _ -> Maybe.just(1),
                        (BiFunction<Object, Integer, Object>) (_, b) -> b));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(v ->
                v.flatMap((Function<Object, MaybeSource<Integer>>) _ ->
                        Maybe.just(1), (BiFunction<Object, Integer, Object>) (_, b) -> b));
    }

    @Test
    public void mapperThrows() {
        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> {
            throw new TestException();
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperReturnsNull() {
        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> null, stringCombine())
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void resultSelectorThrows() {
        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> Maybe.just(2), (_, _) -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void resultSelectorReturnsNull() {
        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> Maybe.just(2), (_, _) -> null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void mapperCancels() {
        final TestObserver<Integer> to = new TestObserver<>();

        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) _ -> {
            to.dispose();
            return Maybe.just(2);
        }, (BiFunction<Integer, Integer, Integer>) (_, _) -> {
            throw new IllegalStateException();
        })
        .subscribeWith(to)
        .assertEmpty();
    }
}
