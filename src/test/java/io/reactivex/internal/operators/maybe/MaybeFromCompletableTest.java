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


import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.fuseable.HasUpstreamCompletableSource;
import io.reactivex.processors.PublishProcessor;

import static org.junit.Assert.*;
import org.junit.Test;

public class MaybeFromCompletableTest {
    @Test(expected = NullPointerException.class)
    public void fromCompletableNull() {
        Maybe.fromCompletable(null);
    }

    @Test
    public void fromCompletable() {
        Maybe.fromCompletable(Completable.complete())
            .test()
            .assertResult();
    }

    @Test
    public void fromCompletableError() {
        Maybe.fromCompletable(Completable.error(new UnsupportedOperationException()))
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @Test
    public void source() {
        Completable c = Completable.complete();

        assertSame(c, ((HasUpstreamCompletableSource)Maybe.fromCompletable(c)).source());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.fromCompletable(PublishProcessor.create().ignoreElements()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletableToMaybe(new Function<Completable, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Completable v) throws Exception {
                return Maybe.fromCompletable(v);
            }
        });
    }
}
