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

package io.reactivex.internal.operators.maybe;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.PublishProcessor;

public class MaybeOfTypeTest {

    @Test
    public void normal() {
        Maybe.just(1).ofType(Integer.class)
        .test()
        .assertResult(1);
    }

    @Test
    public void normalDowncast() {
        TestObserver<Number> ts = Maybe.just(1)
        .ofType(Number.class)
        .test();
        // don't make this fluent, target type required!
        ts.assertResult((Number)1);
    }

    @Test
    public void notInstance() {
        TestObserver<String> ts = Maybe.just(1)
        .ofType(String.class)
        .test();
        // don't make this fluent, target type required!
        ts.assertResult();
    }

    @Test
    public void error() {
        TestObserver<Number> ts = Maybe.<Integer>error(new TestException())
        .ofType(Number.class)
        .test();
        // don't make this fluent, target type required!
        ts.assertFailure(TestException.class);
    }

    @Test
    public void errorNotInstance() {
        TestObserver<String> ts = Maybe.<Integer>error(new TestException())
        .ofType(String.class)
        .test();
        // don't make this fluent, target type required!
        ts.assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposedMaybe(new Function<Maybe<Object>, Maybe<Object>>() {
            @Override
            public Maybe<Object> apply(Maybe<Object> m) throws Exception {
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
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Object>, Maybe<Object>>() {
            @Override
            public Maybe<Object> apply(Maybe<Object> f) throws Exception {
                return f.ofType(Object.class);
            }
        });
    }
}
