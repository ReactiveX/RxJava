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

import java.io.IOException;
import java.util.concurrent.Callable;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.TestObserver;

public class MaybeFlatMapEventSingleTest {

    @Test
    public void mapSuccess() {
        Maybe.just(1)
        .flatMapSingle(Functions.justFunction(Single.just(2)),
                Functions.justFunction(Single.just(3)),
                Functions.justCallable(Single.just(4))
        )
        .test()
        .assertResult(2);
    }

    @Test
    public void mapSuccessCrash() {
        Maybe.just(1)
        .flatMapSingle(
                new Function<Object, SingleSource<? extends Integer>>() {
                    @Override
                    public SingleSource<? extends Integer> apply(Object v)
                            throws Exception { throw new TestException(); }
                },
                Functions.justFunction(Single.just(3)),
                Functions.justCallable(Single.just(4))
        )
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapError() {
        Maybe.error(new TestException())
        .flatMapSingle(Functions.justFunction(Single.just(2)),
                Functions.justFunction(Single.just(3)),
                Functions.justCallable(Single.just(4))
        )
        .test()
        .assertResult(3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mapErrorCrash() {
        TestObserver<Integer> to = Maybe.error(new IOException())
        .flatMapSingle(
                Functions.justFunction(Single.just(2)),
                new Function<Object, SingleSource<? extends Integer>>() {
                    @Override
                    public SingleSource<? extends Integer> apply(Object v)
                            throws Exception { throw new TestException(); }
                },
                Functions.justCallable(Single.just(4))
        )
        .test()
        .assertFailure(CompositeException.class)
        ;

        TestHelper.assertCompositeExceptions(to, IOException.class, TestException.class);
    }

    @Test
    public void mapComplete() {
        Maybe.empty()
        .flatMapSingle(Functions.justFunction(Single.just(2)),
                Functions.justFunction(Single.just(3)),
                Functions.justCallable(Single.just(4))
        )
        .test()
        .assertResult(4);
    }

    @Test
    public void mapCompleteCrash() {
        Maybe.empty()
        .flatMapSingle(
                Functions.justFunction(Single.just(2)),
                Functions.justFunction(Single.just(3)),
                new Callable<SingleSource<? extends Integer>>() {
                    @Override
                    public SingleSource<? extends Integer> call()
                            throws Exception { throw new TestException(); }
                }
        )
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapToError() {
        Maybe.just(1)
        .flatMapSingle(Functions.justFunction(Single.error(new TestException())),
                Functions.justFunction(Single.just(3)),
                Functions.justCallable(Single.just(4))
        )
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void disposeMain() {
        TestHelper.checkDisposedMaybeToSingle(new Function<Maybe<Object>, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Maybe<Object> m)
                    throws Exception {
                return m.flatMapSingle(Functions.justFunction(Single.just(2)),
                        Functions.justFunction(Single.just(3)),
                        Functions.justCallable(Single.just(4))
                );
            }
        });
    }

    @Test
    public void disposeMain2() {
        TestHelper.checkDisposed(
            Maybe.never().flatMapSingle(
                Functions.justFunction(Single.just(2)),
                Functions.justFunction(Single.just(3)),
                Functions.justCallable(Single.just(4))
            )
        );
    }

    @Test
    public void disposeInnerSuccess() {
        TestHelper.checkDisposedSingle(new Function<Single<Integer>, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Single<Integer> m)
                    throws Exception {
                return Maybe.just(1).flatMapSingle(Functions.justFunction(m),
                        Functions.justFunction(Single.just(3)),
                        Functions.justCallable(Single.just(4))
                );
            }
        });
    }

    @Test
    public void disposeInnerError() {
        TestHelper.checkDisposedSingle(new Function<Single<Integer>, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Single<Integer> m)
                    throws Exception {
                return Maybe.error(new TestException()).flatMapSingle(Functions.justFunction(Single.just(2)),
                        Functions.justFunction(m),
                        Functions.justCallable(Single.just(4))
                );
            }
        });
    }

    @Test
    public void disposeInnerComplete() {
        TestHelper.checkDisposedSingle(new Function<Single<Integer>, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Single<Integer> m)
                    throws Exception {
                return Maybe.empty().flatMapSingle(Functions.justFunction(Single.just(2)),
                        Functions.justFunction(Single.just(3)),
                        Functions.justCallable(m)
                );
            }
        });
    }
}
