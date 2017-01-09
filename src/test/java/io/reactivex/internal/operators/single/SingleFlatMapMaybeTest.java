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

package io.reactivex.internal.operators.single;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;

public class SingleFlatMapMaybeTest {
    @Test(expected = NullPointerException.class)
    public void flatMapMaybeNull() {
        Single.just(1)
            .flatMapMaybe(null);
    }

    @Test
    public void flatMapMaybeValue() {
        Single.just(1).flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override public MaybeSource<Integer> apply(final Integer integer) throws Exception {
                if (integer == 1) {
                    return Maybe.just(2);
                }

                return Maybe.just(1);
            }
        })
            .test()
            .assertResult(2);
    }

    @Test
    public void flatMapMaybeValueDifferentType() {
        Single.just(1).flatMapMaybe(new Function<Integer, MaybeSource<String>>() {
            @Override public MaybeSource<String> apply(final Integer integer) throws Exception {
                if (integer == 1) {
                    return Maybe.just("2");
                }

                return Maybe.just("1");
            }
        })
            .test()
            .assertResult("2");
    }

    @Test
    public void flatMapMaybeValueNull() {
        Single.just(1).flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override public MaybeSource<Integer> apply(final Integer integer) throws Exception {
                return null;
            }
        })
            .test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("The mapper returned a null MaybeSource");
    }

    @Test
    public void flatMapMaybeValueErrorThrown() {
        Single.just(1).flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override public MaybeSource<Integer> apply(final Integer integer) throws Exception {
                throw new RuntimeException("something went terribly wrong!");
            }
        })
            .test()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("something went terribly wrong!");
    }

    @Test
    public void flatMapMaybeError() {
        RuntimeException exception = new RuntimeException("test");

        Single.error(exception).flatMapMaybe(new Function<Object, MaybeSource<Object>>() {
            @Override public MaybeSource<Object> apply(final Object integer) throws Exception {
                return Maybe.just(new Object());
            }
        })
            .test()
            .assertError(exception);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(1);
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe(new Function<Single<Integer>, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Single<Integer> v) throws Exception {
                return v.flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> apply(Integer v) throws Exception {
                        return Maybe.just(1);
                    }
                });
            }
        });
    }

    @Test
    public void mapsToError() {
        Single.just(1).flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapsToEmpty() {
        Single.just(1).flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.empty();
            }
        })
        .test()
        .assertResult();
    }
}
