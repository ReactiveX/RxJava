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

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.testsupport.*;

public class SingleOnErrorXTest extends RxJavaTest {

    @Test
    public void returnSuccess() {
        Single.just(1)
        .onErrorReturnItem(2)
        .test()
        .assertResult(1);
    }

    @Test
    public void resumeThrows() {
        TestObserverEx<Integer> to = Single.<Integer>error(new TestException("Outer"))
        .onErrorReturn(new Function<Throwable, Integer>() {
            @Override
            public Integer apply(Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void resumeErrors() {
        Single.error(new TestException("Main"))
        .onErrorResumeWith(Single.error(new TestException("Resume")))
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "Resume");
    }

    @Test
    public void resumeDispose() {
        TestHelper.checkDisposed(Single.error(new TestException("Main"))
        .onErrorResumeWith(Single.just(1)));
    }

    @Test
    public void resumeDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Single<Object> s) throws Exception {
                return s.onErrorResumeWith(Single.just(1));
            }
        });
    }

    @Test
    public void resumeSuccess() {
        Single.just(1)
        .onErrorResumeWith(Single.just(2))
        .test()
        .assertResult(1);
    }
}
