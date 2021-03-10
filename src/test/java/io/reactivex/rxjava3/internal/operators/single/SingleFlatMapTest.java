/*
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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.testsupport.*;

public class SingleFlatMapTest extends RxJavaTest {

    @Test
    public void normal() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable((Function<Integer, Completable>) t -> Completable.complete().doOnComplete(() -> b[0] = true))
        .test()
        .assertResult();

        assertTrue(b[0]);
    }

    @Test
    public void error() {
        final boolean[] b = { false };

        Single.<Integer>error(new TestException())
        .flatMapCompletable((Function<Integer, Completable>) t -> Completable.complete().doOnComplete(() -> b[0] = true))
        .test()
        .assertFailure(TestException.class);

        assertFalse(b[0]);
    }

    @Test
    public void mapperThrows() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable((Function<Integer, Completable>) t -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(b[0]);
    }

    @Test
    public void mapperReturnsNull() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable((Function<Integer, Completable>) t -> null)
        .test()
        .assertFailure(NullPointerException.class);

        assertFalse(b[0]);
    }

    @Test
    public void flatMapObservable() {
        Single.just(1).flatMapObservable((Function<Integer, Observable<Integer>>) v -> Observable.range(v, 5))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void flatMapPublisher() {
        Single.just(1).flatMapPublisher((Function<Integer, Publisher<Integer>>) v -> Flowable.range(v, 5))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void flatMapPublisherMapperThrows() {
        final TestException ex = new TestException();
        Single.just(1)
        .flatMapPublisher((Function<Integer, Publisher<Integer>>) v -> {
            throw ex;
        })
        .test()
        .assertNoValues()
        .assertError(ex);
    }

    @Test
    public void flatMapPublisherSingleError() {
        final TestException ex = new TestException();
        Single.<Integer>error(ex)
        .flatMapPublisher((Function<Integer, Publisher<Integer>>) v -> Flowable.just(1))
        .test()
        .assertNoValues()
        .assertError(ex);
    }

    @Test
    public void flatMapPublisherCancelDuringSingle() {
        final AtomicBoolean disposed = new AtomicBoolean();
        TestSubscriberEx<Integer> ts = Single.<Integer>never()
        .doOnDispose(() -> disposed.set(true))
        .flatMapPublisher((Function<Integer, Publisher<Integer>>) v -> Flowable.range(v, 5))
        .to(TestHelper.testConsumer())
        .assertNoValues()
        .assertNotTerminated();
        assertFalse(disposed.get());
        ts.cancel();
        assertTrue(disposed.get());
        ts.assertNotTerminated();
    }

    @Test
    public void flatMapPublisherCancelDuringFlowable() {
        final AtomicBoolean disposed = new AtomicBoolean();
        TestSubscriberEx<Integer> ts =
        Single.just(1)
        .flatMapPublisher((Function<Integer, Publisher<Integer>>) v -> Flowable.<Integer>never()
                .doOnCancel(() -> disposed.set(true)))
        .to(TestHelper.testConsumer())
        .assertNoValues()
        .assertNotTerminated();
        assertFalse(disposed.get());
        ts.cancel();
        assertTrue(disposed.get());
        ts.assertNotTerminated();
    }

    @Test
    public void flatMapValue() {
        Single.just(1).flatMap((Function<Integer, SingleSource<Integer>>) integer -> {
            if (integer == 1) {
                return Single.just(2);
            }

            return Single.just(1);
        })
            .test()
            .assertResult(2);
    }

    @Test
    public void flatMapValueDifferentType() {
        Single.just(1).flatMap((Function<Integer, SingleSource<String>>) integer -> {
            if (integer == 1) {
                return Single.just("2");
            }

            return Single.just("1");
        })
            .test()
            .assertResult("2");
    }

    @Test
    public void flatMapValueNull() {
        Single.just(1).flatMap((Function<Integer, SingleSource<Integer>>) integer -> null)
        .to(TestHelper.testConsumer())
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("The single returned by the mapper is null");
    }

    @Test
    public void flatMapValueErrorThrown() {
        Single.just(1).flatMap((Function<Integer, SingleSource<Integer>>) integer -> {
            throw new RuntimeException("something went terribly wrong!");
        })
            .to(TestHelper.testConsumer())
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("something went terribly wrong!");
    }

    @Test
    public void flatMapError() {
        RuntimeException exception = new RuntimeException("test");

        Single.error(exception).flatMap((Function<Object, SingleSource<Object>>) integer -> Single.just(new Object()))
            .test()
            .assertError(exception);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).flatMap((Function<Integer, SingleSource<Integer>>) v -> Single.just(2)));
    }

    @Test
    public void mappedSingleOnError() {
        Single.just(1).flatMap((Function<Integer, SingleSource<Integer>>) v -> Single.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(s -> s.flatMap(Single::just));
    }
}
