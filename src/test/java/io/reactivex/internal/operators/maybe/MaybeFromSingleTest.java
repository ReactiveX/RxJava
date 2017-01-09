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

import static org.junit.Assert.assertSame;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.fuseable.HasUpstreamSingleSource;
import io.reactivex.processors.PublishProcessor;

public class MaybeFromSingleTest {
    @Test(expected = NullPointerException.class)
    public void fromSingleNull() {
        Maybe.fromSingle(null);
    }

    @Test
    public void fromSingle() {
        Maybe.fromSingle(Single.just(1))
            .test()
            .assertResult(1);
    }

    @Test
    public void fromSingleThrows() {
        Maybe.fromSingle(Single.error(new UnsupportedOperationException()))
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @Test
    public void source() {
        Single<Integer> c = Single.never();

        assertSame(c, ((HasUpstreamSingleSource<?>)Maybe.fromSingle(c)).source());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.fromSingle(PublishProcessor.create().singleOrError()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe(new Function<Single<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Single<Object> v) throws Exception {
                return Maybe.fromSingle(v);
            }
        });
    }
}
