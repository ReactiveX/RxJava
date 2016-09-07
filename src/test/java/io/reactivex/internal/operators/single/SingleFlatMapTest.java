/**
 * Copyright 2016 Netflix, Inc.
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

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;

public class SingleFlatMapTest {

    @Test
    public void normal() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return Completable.complete().doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        b[0] = true;
                    }
                });
            }
        })
        .test()
        .assertResult();

        assertTrue(b[0]);
    }

    @Test
    public void error() {
        final boolean[] b = { false };

        Single.<Integer>error(new TestException())
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return Completable.complete().doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        b[0] = true;
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(b[0]);
    }

    @Test
    public void mapperThrows() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(b[0]);
    }

    @Test
    public void mapperReturnsNull() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);

        assertFalse(b[0]);
    }


    @Test
    public void flatMapObservable() {
        Single.just(1).flatMapObservable(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 5);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void flatMapPublisher() {
        Single.just(1).flatMapPublisher(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(v, 5);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }
}
