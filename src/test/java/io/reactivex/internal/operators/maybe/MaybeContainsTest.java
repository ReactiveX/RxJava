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
import io.reactivex.internal.fuseable.HasUpstreamMaybeSource;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.PublishProcessor;

public class MaybeContainsTest {

    @Test
    public void doesContain() {
        Maybe.just(1).contains(1).test().assertResult(true);
    }

    @Test
    public void doesntContain() {
        Maybe.just(1).contains(2).test().assertResult(false);
    }

    @Test
    public void empty() {
        Maybe.empty().contains(2).test().assertResult(false);
    }

    @Test
    public void error() {
        Maybe.error(new TestException()).contains(2).test().assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Boolean> ts = pp.singleElement().contains(1).test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }


    @Test
    public void isDisposed() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.singleElement().contains(1));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToSingle(new Function<Maybe<Object>, SingleSource<Boolean>>() {
            @Override
            public SingleSource<Boolean> apply(Maybe<Object> f) throws Exception {
                return f.contains(1);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void hasSource() {
        assertSame(Maybe.empty(), ((HasUpstreamMaybeSource<Object>)(Maybe.empty().contains(0))).source());
    }
}
