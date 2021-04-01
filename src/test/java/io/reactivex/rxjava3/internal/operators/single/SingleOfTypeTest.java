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
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleOfTypeTest extends RxJavaTest {

    @Test
    public void normal() {
        Single.just(1).ofType(Integer.class)
        .test()
        .assertResult(1);
    }

    @Test
    public void normalDowncast() {
        TestObserver<Number> to = Single.just(1)
        .ofType(Number.class)
        .test();
        // don't make this fluent, target type required!
        to.assertResult((Number)1);
    }

    @Test
    public void notInstance() {
        TestObserver<String> to = Single.just(1)
        .ofType(String.class)
        .test();
        // don't make this fluent, target type required!
        to.assertResult();
    }

    @Test
    public void error() {
        TestObserver<Number> to = Single.<Integer>error(new TestException())
        .ofType(Number.class)
        .test();
        // don't make this fluent, target type required!
        to.assertFailure(TestException.class);
    }

    @Test
    public void errorNotInstance() {
        TestObserver<String> to = Single.<Integer>error(new TestException())
        .ofType(String.class)
        .test();
        // don't make this fluent, target type required!
        to.assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposedSingleToMaybe(new Function<Single<Object>, Maybe<Object>>() {
            @Override
            public Maybe<Object> apply(Single<Object> m) throws Exception {
                return m.ofType(Object.class);
            }
        });
    }

    @Test
    public void isDisposed() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.singleElement().ofType(Object.class));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe(new Function<Single<Object>, Maybe<Object>>() {
            @Override
            public Maybe<Object> apply(Single<Object> f) throws Exception {
                return f.ofType(Object.class);
            }
        });
    }
}
