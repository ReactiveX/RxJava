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

package io.reactivex.rxjava3.internal.jdk8;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.operators.QueueDisposable;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.subjects.MaybeSubject;
import io.reactivex.rxjava3.testsupport.*;

public class MaybeFlattenStreamAsObservableTest extends RxJavaTest {

    @Test
    public void successJust() {
        Maybe.just(1)
        .flattenStreamAsObservable(Stream::of)
        .test()
        .assertResult(1);
    }

    @Test
    public void successEmpty() {
        Maybe.just(1)
        .flattenStreamAsObservable(v -> Stream.of())
        .test()
        .assertResult();
    }

    @Test
    public void successMany() {
        Maybe.just(1)
        .flattenStreamAsObservable(v -> Stream.of(2, 3, 4, 5, 6))
        .test()
        .assertResult(2, 3, 4, 5, 6);
    }

    @Test
    public void successManyTake() {
        Maybe.just(1)
        .flattenStreamAsObservable(v -> Stream.of(2, 3, 4, 5, 6))
        .take(3)
        .test()
        .assertResult(2, 3, 4);
    }

    @Test
    public void empty() throws Throwable {
        @SuppressWarnings("unchecked")
        Function<? super Integer, Stream<? extends Integer>> f = mock(Function.class);

        Maybe.<Integer>empty()
        .flattenStreamAsObservable(f)
        .test()
        .assertResult();

        verify(f, never()).apply(any());
    }

    @Test
    public void error() throws Throwable {
        @SuppressWarnings("unchecked")
        Function<? super Integer, Stream<? extends Integer>> f = mock(Function.class);

        Maybe.<Integer>error(new TestException())
        .flattenStreamAsObservable(f)
        .test()
        .assertFailure(TestException.class);

        verify(f, never()).apply(any());
    }

    @Test
    public void mapperCrash() {
        Maybe.just(1)
        .flattenStreamAsObservable(v -> { throw new TestException(); })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.never().flattenStreamAsObservable(Stream::of));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToObservable(m -> m.flattenStreamAsObservable(Stream::of));
    }

    @Test
    public void fusedEmpty() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.ANY);

        Maybe.just(1)
        .flattenStreamAsObservable(v -> Stream.<Integer>of())
        .subscribe(to);

        to.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void fusedJust() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.ANY);

        Maybe.just(1)
        .flattenStreamAsObservable(v -> Stream.<Integer>of(v))
        .subscribe(to);

        to.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1);
    }

    @Test
    public void fusedMany() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.ANY);

        Maybe.just(1)
        .flattenStreamAsObservable(v -> Stream.<Integer>of(v, v + 1, v + 2))
        .subscribe(to);

        to.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3);
    }

    @Test
    public void fusedManyRejected() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.SYNC);

        Maybe.just(1)
        .flattenStreamAsObservable(v -> Stream.<Integer>of(v, v + 1, v + 2))
        .subscribe(to);

        to.assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3);
    }

    @Test
    public void fusedStreamAvailableLater() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.ANY);

        MaybeSubject<Integer> ms = MaybeSubject.create();

        ms
        .flattenStreamAsObservable(v -> Stream.<Integer>of(v, v + 1, v + 2))
        .subscribe(to);

        to.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertEmpty();

        ms.onSuccess(1);

        to
        .assertResult(1, 2, 3);
    }

    @Test
    public void fused() throws Throwable {
        AtomicReference<QueueDisposable<Integer>> qdr = new AtomicReference<>();

        MaybeSubject<Integer> ms = MaybeSubject.create();

        ms
        .flattenStreamAsObservable(Stream::of)
        .subscribe(new Observer<Integer>() {

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            @SuppressWarnings("unchecked")
            public void onSubscribe(Disposable d) {
                qdr.set((QueueDisposable<Integer>)d);
            }
        });

        QueueDisposable<Integer> qd = qdr.get();

        assertEquals(QueueFuseable.ASYNC, qd.requestFusion(QueueFuseable.ASYNC));

        assertTrue(qd.isEmpty());
        assertNull(qd.poll());

        ms.onSuccess(1);

        assertFalse(qd.isEmpty());
        assertEquals(1, qd.poll().intValue());

        assertTrue(qd.isEmpty());
        assertNull(qd.poll());

        qd.dispose();

        assertTrue(qd.isEmpty());
        assertNull(qd.poll());
    }

    @Test
    public void fused2() throws Throwable {
        AtomicReference<QueueDisposable<Integer>> qdr = new AtomicReference<>();

        MaybeSubject<Integer> ms = MaybeSubject.create();

        ms
        .flattenStreamAsObservable(v -> Stream.of(v, v + 1))
        .subscribe(new Observer<Integer>() {

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            @SuppressWarnings("unchecked")
            public void onSubscribe(Disposable d) {
                qdr.set((QueueDisposable<Integer>)d);
            }
        });

        QueueDisposable<Integer> qd = qdr.get();

        assertEquals(QueueFuseable.ASYNC, qd.requestFusion(QueueFuseable.ASYNC));

        assertTrue(qd.isEmpty());
        assertNull(qd.poll());

        ms.onSuccess(1);

        assertFalse(qd.isEmpty());
        assertEquals(1, qd.poll().intValue());

        assertFalse(qd.isEmpty());
        assertEquals(2, qd.poll().intValue());

        assertTrue(qd.isEmpty());
        assertNull(qd.poll());

        qd.dispose();

        assertTrue(qd.isEmpty());
        assertNull(qd.poll());
    }

    @Test
    public void streamCloseCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Maybe.just(1)
            .flattenStreamAsObservable(v -> Stream.of(v).onClose(() -> { throw new TestException(); }))
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void hasNextThrowsInDrain() {
        @SuppressWarnings("unchecked")
        Stream<Integer> stream = mock(Stream.class);
        when(stream.iterator()).thenReturn(new Iterator<Integer>() {

            int count;

            @Override
            public boolean hasNext() {
                if (count++ > 0) {
                    throw new TestException();
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }
        });

        Maybe.just(1)
        .flattenStreamAsObservable(v -> stream)
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void nextThrowsInDrain() {
        @SuppressWarnings("unchecked")
        Stream<Integer> stream = mock(Stream.class);
        when(stream.iterator()).thenReturn(new Iterator<Integer>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                throw new TestException();
            }
        });

        Maybe.just(1)
        .flattenStreamAsObservable(v -> stream)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void cancelAfterHasNextInDrain() {
        @SuppressWarnings("unchecked")
        Stream<Integer> stream = mock(Stream.class);

        TestObserver<Integer> to = new TestObserver<>();

        when(stream.iterator()).thenReturn(new Iterator<Integer>() {

            int count;

            @Override
            public boolean hasNext() {
                if (count++ > 0) {
                    to.dispose();
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }
        });

        Maybe.just(1)
        .flattenStreamAsObservable(v -> stream)
        .subscribeWith(to)
        .assertValuesOnly(1);
    }

    @Test
    public void cancelAfterNextInDrain() {
        @SuppressWarnings("unchecked")
        Stream<Integer> stream = mock(Stream.class);

        TestObserver<Integer> to = new TestObserver<>();

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

        Maybe.just(1)
        .flattenStreamAsObservable(v -> stream)
        .subscribeWith(to)
        .assertEmpty();
    }

    @Test
    public void cancelSuccessRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            MaybeSubject<Integer> ms = MaybeSubject.create();

            TestObserver<Integer> to = new TestObserver<>();

            ms.flattenStreamAsObservable(Stream::of)
            .subscribe(to);

            Runnable r1 = () -> ms.onSuccess(1);
            Runnable r2 = () -> to.dispose();

            TestHelper.race(r1, r2);
        }
    }
}
