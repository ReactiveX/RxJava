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
import io.reactivex.processors.PublishProcessor;

public class MaybeIsEmptyTest {

    @Test
    public void normal() {
        Maybe.just(1)
        .isEmpty()
        .test()
        .assertResult(false);
    }

    @Test
    public void empty() {
        Maybe.empty()
        .isEmpty()
        .test()
        .assertResult(true);
    }

    @Test
    public void error() {
        Maybe.error(new TestException())
        .isEmpty()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedBackToMaybe() {
        assertTrue(Maybe.just(1)
        .isEmpty()
        .toMaybe() instanceof MaybeIsEmpty);
    }


    @Test
    public void normalToMaybe() {
        Maybe.just(1)
        .isEmpty()
        .toMaybe()
        .test()
        .assertResult(false);
    }

    @Test
    public void emptyToMaybe() {
        Maybe.empty()
        .isEmpty()
        .toMaybe()
        .test()
        .assertResult(true);
    }

    @Test
    public void errorToMaybe() {
        Maybe.error(new TestException())
        .isEmpty()
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposedMaybeToSingle(new Function<Maybe<Object>, SingleSource<Boolean>>() {
            @Override
            public SingleSource<Boolean> apply(Maybe<Object> m) throws Exception {
                return m.isEmpty();
            }
        });
    }

    @Test
    public void isDisposed() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.singleElement().isEmpty());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToSingle(new Function<Maybe<Object>, Single<Boolean>>() {
            @Override
            public Single<Boolean> apply(Maybe<Object> f) throws Exception {
                return f.isEmpty();
            }
        });
    }

    @Test
    public void disposeToMaybe() {
        TestHelper.checkDisposedMaybe(new Function<Maybe<Object>, Maybe<Boolean>>() {
            @Override
            public Maybe<Boolean> apply(Maybe<Object> m) throws Exception {
                return m.isEmpty().toMaybe();
            }
        });
    }

    @Test
    public void isDisposedToMaybe() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.singleElement().isEmpty().toMaybe());
    }

    @Test
    public void doubleOnSubscribeToMaybe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Object>, Maybe<Boolean>>() {
            @Override
            public Maybe<Boolean> apply(Maybe<Object> f) throws Exception {
                return f.isEmpty().toMaybe();
            }
        });
    }
}
