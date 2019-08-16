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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamMaybeSource;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeCountTest extends RxJavaTest {

    @Test
    public void one() {
        Maybe.just(1).count().test().assertResult(1L);
    }

    @Test
    public void empty() {
        Maybe.empty().count().test().assertResult(0L);
    }

    @Test
    public void error() {
        Maybe.error(new TestException()).count().test().assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Long> to = pp.singleElement().count().test();

        assertTrue(pp.hasSubscribers());

        to.dispose();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void isDisposed() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.singleElement().count());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToSingle(new Function<Maybe<Object>, SingleSource<Long>>() {
            @Override
            public SingleSource<Long> apply(Maybe<Object> f) throws Exception {
                return f.count();
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void hasSource() {
        assertSame(Maybe.empty(), ((HasUpstreamMaybeSource<Object>)(Maybe.empty().count())).source());
    }
}
