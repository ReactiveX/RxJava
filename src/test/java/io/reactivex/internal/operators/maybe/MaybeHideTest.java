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

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.fuseable.ScalarCallable;
import io.reactivex.processors.PublishProcessor;

public class MaybeHideTest {

    @Test
    public void normal() {
        Maybe.just(1)
        .hide()
        .test()
        .assertResult(1);
    }

    @Test
    public void empty() {
        Maybe.empty()
        .hide()
        .test()
        .assertResult();
    }

    @Test
    public void error() {
        Maybe.error(new TestException())
        .hide()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void hidden() {
        assertTrue(Maybe.just(1) instanceof ScalarCallable);

        assertFalse(Maybe.just(1).hide() instanceof ScalarCallable);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposedMaybe(new Function<Maybe<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Maybe<Object> m) throws Exception {
                return m.hide();
            }
        });
    }

    @Test
    public void isDisposed() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.singleElement().hide());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Object>, Maybe<Object>>() {
            @Override
            public Maybe<Object> apply(Maybe<Object> f) throws Exception {
                return f.hide();
            }
        });
    }
}
