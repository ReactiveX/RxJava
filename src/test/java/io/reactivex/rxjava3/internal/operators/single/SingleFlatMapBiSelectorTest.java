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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleFlatMapBiSelectorTest extends RxJavaTest {

    BiFunction<Integer, Integer, String> stringCombine() {
        return (a, b) -> a + ":" + b;
    }

    @Test
    public void normal() {
        Single.just(1)
        .flatMap((Function<Integer, SingleSource<Integer>>) v -> Single.just(2), stringCombine())
        .test()
        .assertResult("1:2");
    }

    @Test
    public void errorWithJust() {
        final int[] call = { 0 };

        Single.<Integer>error(new TestException())
        .flatMap((Function<Integer, SingleSource<Integer>>) v -> {
            call[0]++;
            return Single.just(1);
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void justWithError() {
        final int[] call = { 0 };

        Single.just(1)
        .flatMap((Function<Integer, SingleSource<Integer>>) v -> {
            call[0]++;
            return Single.<Integer>error(new TestException());
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, call[0]);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(SingleSubject.create()
                .flatMap((Function<Object, SingleSource<Integer>>) v -> Single.just(1), (BiFunction<Object, Integer, Object>) (a, b) -> b));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle((Function<Single<Object>, SingleSource<Object>>) v -> v.flatMap((Function<Object, SingleSource<Integer>>) v1 -> Single.just(1), (a, b) -> b));
    }

    @Test
    public void mapperThrows() {
        Single.just(1)
        .flatMap((Function<Integer, SingleSource<Integer>>) v -> {
            throw new TestException();
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperReturnsNull() {
        Single.just(1)
        .flatMap((Function<Integer, SingleSource<Integer>>) v -> null, stringCombine())
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void resultSelectorThrows() {
        Single.just(1)
        .flatMap((Function<Integer, SingleSource<Integer>>) v -> Single.just(2), (a, b) -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void resultSelectorReturnsNull() {
        Single.just(1)
        .flatMap((Function<Integer, SingleSource<Integer>>) v -> Single.just(2), (a, b) -> null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void mapperCancels() {
        final TestObserver<Integer> to = new TestObserver<>();

        Single.just(1)
        .flatMap((Function<Integer, SingleSource<Integer>>) v -> {
            to.dispose();
            return Single.just(2);
        }, (BiFunction<Integer, Integer, Integer>) (a, b) -> {
            throw new IllegalStateException();
        })
        .subscribeWith(to)
        .assertEmpty();
    }
}
