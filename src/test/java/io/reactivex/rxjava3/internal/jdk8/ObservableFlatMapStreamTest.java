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

package io.reactivex.rxjava3.internal.jdk8;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

import org.junit.Test;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableFlatMapStreamTest extends RxJavaTest {

    @Test
    public void empty() {
        Observable.empty()
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertResult();
    }

    @Test
    public void emptyHidden() {
        Observable.empty()
        .hide()
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertResult();
    }

    @Test
    public void just() {
        Observable.just(1)
        .flatMapStream(v -> Stream.of(v + 1, v + 2, v + 3, v + 4, v + 5))
        .test()
        .assertResult(2, 3, 4, 5, 6);
    }

    @Test
    public void justHidden() {
        Observable.just(1).hide()
        .flatMapStream(v -> Stream.of(v + 1, v + 2, v + 3, v + 4, v + 5))
        .test()
        .assertResult(2, 3, 4, 5, 6);
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void supplierFusedError() {
        Observable.fromCallable(() -> { throw new TestException(); })
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorHidden() {
        Observable.error(new TestException())
        .hide()
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void range() {
        Observable.range(1, 5)
        .flatMapStream(v -> IntStream.range(v * 10, v * 10 + 5).boxed())
        .test()
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31, 32, 33, 34,
                40, 41, 42, 43, 44,
                50, 51, 52, 53, 54
        );
    }

    @Test
    public void rangeHidden() {
        Observable.range(1, 5)
        .hide()
        .flatMapStream(v -> IntStream.range(v * 10, v * 10 + 5).boxed())
        .test()
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31, 32, 33, 34,
                40, 41, 42, 43, 44,
                50, 51, 52, 53, 54
        );
    }

    @Test
    public void rangeToEmpty() {
        Observable.range(1, 5)
        .flatMapStream(v -> Stream.of())
        .test()
        .assertResult();
    }

    @Test
    public void rangeTake() {
        Observable.range(1, 5)
        .flatMapStream(v -> IntStream.range(v * 10, v * 10 + 5).boxed())
        .take(12)
        .test()
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31
        );
    }

    @Test
    public void rangeTakeHidden() {
        Observable.range(1, 5)
        .hide()
        .flatMapStream(v -> IntStream.range(v * 10, v * 10 + 5).boxed())
        .take(12)
        .test()
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31
        );
    }

    @Test
    public void upstreamCancelled() {
        PublishSubject<Integer> ps = PublishSubject.create();

        AtomicInteger calls = new AtomicInteger();

        TestObserver<Integer> to = ps
                .flatMapStream(v -> Stream.of(v + 1, v + 2).onClose(() -> calls.getAndIncrement()))
                .take(1)
                .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertResult(2);

        assertFalse(ps.hasObservers());

        assertEquals(1, calls.get());
    }

    @Test
    public void upstreamCancelledCloseCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            PublishSubject<Integer> ps = PublishSubject.create();

            TestObserver<Integer> to = ps
                    .flatMapStream(v -> Stream.of(v + 1, v + 2).onClose(() -> { throw new TestException(); }))
                    .take(1)
                    .test();

            assertTrue(ps.hasObservers());

            ps.onNext(1);

            to.assertResult(2);

            assertFalse(ps.hasObservers());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void crossMap() {
        Observable.range(1, 1000)
        .flatMapStream(v -> IntStream.range(v * 1000, v * 1000 + 1000).boxed())
        .test()
        .assertValueCount(1_000_000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void crossMapHidden() {
        Observable.range(1, 1000)
        .hide()
        .flatMapStream(v -> IntStream.range(v * 1000, v * 1000 + 1000).boxed())
        .test()
        .assertValueCount(1_000_000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void onSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(f -> f.flatMapStream(v -> Stream.of(1, 2)));
    }

    @Test
    public void mapperThrows() {
        Observable.just(1).hide()
        .concatMapStream(v -> { throw new TestException(); })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperNull() {
        Observable.just(1).hide()
        .concatMapStream(v -> null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void streamNull() {
        Observable.just(1).hide()
        .concatMapStream(v -> Stream.of(1, null))
        .test()
        .assertFailure(NullPointerException.class, 1);
    }

    @Test
    public void hasNextThrows() {
        Observable.just(1).hide()
        .concatMapStream(v -> Stream.generate(() -> { throw new TestException(); }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void hasNextThrowsLater() {
        AtomicInteger counter = new AtomicInteger();
        Observable.just(1).hide()
        .concatMapStream(v -> Stream.generate(() -> {
            if (counter.getAndIncrement() == 0) {
                return 1;
            }
            throw new TestException();
        }))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void mapperThrowsWhenUpstreamErrors() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            PublishSubject<Integer> ps = PublishSubject.create();

            AtomicInteger counter = new AtomicInteger();

            TestObserver<Integer> to = ps.hide()
            .concatMapStream(v -> {
                if (counter.getAndIncrement() == 0) {
                    return Stream.of(1, 2);
                }
                ps.onError(new IOException());
                throw new TestException();
            })
            .test();

            ps.onNext(1);
            ps.onNext(2);

            to
            .assertFailure(IOException.class, 1, 2);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void cancelAfterIteratorNext() throws Exception {
        TestObserver<Integer> to = new TestObserver<>();

        @SuppressWarnings("unchecked")
        Stream<Integer> stream = mock(Stream.class);
        when(stream.iterator()).thenReturn(new Iterator<Integer>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                to.dispose();
                return 1;
            }
        });

        Observable.just(1)
        .hide()
        .concatMapStream(v -> stream)
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void cancelAfterIteratorHasNext() throws Exception {
        TestObserver<Integer> to = new TestObserver<>();

        @SuppressWarnings("unchecked")
        Stream<Integer> stream = mock(Stream.class);
        when(stream.iterator()).thenReturn(new Iterator<Integer>() {

            @Override
            public boolean hasNext() {
                to.dispose();
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }
        });

        Observable.just(1)
        .hide()
        .concatMapStream(v -> stream)
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void asyncUpstreamFused() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Integer> to = us.flatMapStream(v -> Stream.of(1, 2))
        .test();

        assertTrue(us.hasObservers());

        us.onNext(1);

        to.assertValuesOnly(1, 2);

        us.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void asyncUpstreamFusionBoundary() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Integer> to = us
                .map(v -> v + 1)
                .flatMapStream(v -> Stream.of(1, 2))
        .test();

        assertTrue(us.hasObservers());

        us.onNext(1);

        to.assertValuesOnly(1, 2);

        us.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void fusedPollCrash() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Integer> to = us
                .map(v -> { throw new TestException(); })
                .compose(TestHelper.observableStripBoundary())
                .flatMapStream(v -> Stream.of(1, 2))
        .test();

        assertTrue(us.hasObservers());

        us.onNext(1);

        assertFalse(us.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().flatMapStream(v -> Stream.of(1)));
    }

    @Test
    public void eventsIgnoredAfterCrash() {
        AtomicInteger calls = new AtomicInteger();

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(@NonNull Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onNext(1);
                observer.onNext(2);
                observer.onComplete();
            }
        }
        .flatMapStream(v -> {
            calls.getAndIncrement();
            throw new TestException();
        })
        .take(1)
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls.get());
    }

    @Test
    public void eventsIgnoredAfterDispose() {
        AtomicInteger calls = new AtomicInteger();

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(@NonNull Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onNext(1);
                observer.onNext(2);
                observer.onComplete();
            }
        }
        .flatMapStream(v -> {
            calls.getAndIncrement();
            return Stream.of(1);
        })
        .take(1)
        .test()
        .assertResult(1);

        assertEquals(1, calls.get());
    }
}
