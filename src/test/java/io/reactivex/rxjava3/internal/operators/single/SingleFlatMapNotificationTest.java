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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.testsupport.*;

public class SingleFlatMapNotificationTest extends RxJavaTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1)
                .flatMap(Functions.justFunction(Single.just(1)),
                        Functions.justFunction(Single.just(1))));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Integer>, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Single<Integer> m) throws Exception {
                return m
                        .flatMap(Functions.justFunction(Single.just(1)),
                                Functions.justFunction(Single.just(1)));
            }
        });
    }

    @Test
    public void onSuccessNull() {
        Single.just(1)
        .flatMap(Functions.justFunction((Single<Integer>)null),
                Functions.justFunction(Single.just(1)))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void onErrorNull() {
        TestObserverEx<Integer> to = Single.<Integer>error(new TestException())
        .flatMap(Functions.justFunction(Single.just(1)),
                Functions.justFunction((Single<Integer>)null))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> ce = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(ce, 0, TestException.class);
        TestHelper.assertError(ce, 1, NullPointerException.class);
    }

    @Test
    public void onSuccessError() {
        Single.just(1)
        .flatMap(Functions.justFunction(Single.<Integer>error(new TestException())),
                Functions.justFunction((Single<Integer>)null))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void onSucccessSuccess() {
        Single.just(1)
        .flatMap(v -> Single.just(2), e -> Single.just(3))
        .test()
        .assertResult(2);
    }

    @Test
    public void onErrorSuccess() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Single.error(new TestException())
            .flatMap(v -> Single.just(2), e -> Single.just(3))
            .test()
            .assertResult(3);

            assertTrue("" + errors, errors.isEmpty());
        });
    }

    @Test
    public void onErrorError() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Single.error(new TestException())
            .flatMap(v -> Single.just(2), e -> Single.<Integer>error(new IOException()))
            .test()
            .assertFailure(IOException.class);

            assertTrue("" + errors, errors.isEmpty());
        });
    }
}
