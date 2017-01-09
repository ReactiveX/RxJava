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

public class MaybeFlatMapSingleElementTest {
    @Test(expected = NullPointerException.class)
    public void flatMapSingleElementNull() {
        Maybe.just(1)
            .flatMapSingleElement(null);
    }

    @Test
    public void flatMapSingleElementValue() {
        Maybe.just(1).flatMapSingleElement(new Function<Integer, SingleSource<Integer>>() {
            @Override public SingleSource<Integer> apply(final Integer integer) throws Exception {
                if (integer == 1) {
                    return Single.just(2);
                }

                return Single.just(1);
            }
        })
            .test()
            .assertResult(2);
    }

    @Test
    public void flatMapSingleElementValueDifferentType() {
        Maybe.just(1).flatMapSingleElement(new Function<Integer, SingleSource<String>>() {
            @Override public SingleSource<String> apply(final Integer integer) throws Exception {
                if (integer == 1) {
                    return Single.just("2");
                }

                return Single.just("1");
            }
        })
            .test()
            .assertResult("2");
    }

    @Test
    public void flatMapSingleElementValueNull() {
        Maybe.just(1).flatMapSingleElement(new Function<Integer, SingleSource<Integer>>() {
            @Override public SingleSource<Integer> apply(final Integer integer) throws Exception {
                return null;
            }
        })
            .test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("The mapper returned a null SingleSource");
    }

    @Test
    public void flatMapSingleElementValueErrorThrown() {
        Maybe.just(1).flatMapSingleElement(new Function<Integer, SingleSource<Integer>>() {
            @Override public SingleSource<Integer> apply(final Integer integer) throws Exception {
                throw new RuntimeException("something went terribly wrong!");
            }
        })
            .test()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("something went terribly wrong!");
    }

    @Test
    public void flatMapSingleElementError() {
        RuntimeException exception = new RuntimeException("test");

        Maybe.error(exception).flatMapSingleElement(new Function<Object, SingleSource<Object>>() {
            @Override public SingleSource<Object> apply(final Object integer) throws Exception {
                return Single.just(new Object());
            }
        })
            .test()
            .assertError(exception);
    }

    @Test
    public void flatMapSingleElementEmpty() {
        Maybe.<Integer>empty().flatMapSingleElement(new Function<Integer, SingleSource<Integer>>() {
            @Override public SingleSource<Integer> apply(final Integer integer) throws Exception {
                return Single.just(2);
            }
        })
            .test()
            .assertNoValues()
            .assertResult();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.just(1).flatMapSingleElement(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(final Integer integer) throws Exception {
                return Single.just(2);
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Integer>, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Maybe<Integer> m) throws Exception {
                return m.flatMapSingleElement(new Function<Integer, SingleSource<Integer>>() {
                    @Override
                    public SingleSource<Integer> apply(final Integer integer) throws Exception {
                        return Single.just(2);
                    }
                });
            }
        });
    }

    @Test
    public void singleErrors() {
        Maybe.just(1)
        .flatMapSingleElement(new Function<Integer, SingleSource<Integer>>() {
                    @Override
                    public SingleSource<Integer> apply(final Integer integer) throws Exception {
                        return Single.error(new TestException());
                    }
                })
        .test()
        .assertFailure(TestException.class);
    }
}
