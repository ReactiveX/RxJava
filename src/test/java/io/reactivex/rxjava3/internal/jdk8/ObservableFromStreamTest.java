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
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.junit.Test;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.operators.QueueDisposable;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.operators.SimpleQueue;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableFromStreamTest extends RxJavaTest {

    @Test
    public void empty() {
        Observable.fromStream(Stream.<Integer>of())
        .test()
        .assertResult();
    }

    @Test
    public void just() {
        Observable.fromStream(Stream.<Integer>of(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void many() {
        Observable.fromStream(Stream.<Integer>of(1, 2, 3, 4, 5))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void noReuse() {
        Observable<Integer> source = Observable.fromStream(Stream.<Integer>of(1, 2, 3, 4, 5));

        source
        .test()
        .assertResult(1, 2, 3, 4, 5);

        source
        .test()
        .assertFailure(IllegalStateException.class);
    }

    @Test
    public void take() {
        Observable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void emptyConditional() {
        Observable.fromStream(Stream.<Integer>of())
        .filter(v -> true)
        .test()
        .assertResult();
    }

    @Test
    public void justConditional() {
        Observable.fromStream(Stream.<Integer>of(1))
        .filter(v -> true)
        .test()
        .assertResult(1);
    }

    @Test
    public void manyConditional() {
        Observable.fromStream(Stream.<Integer>of(1, 2, 3, 4, 5))
        .filter(v -> true)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void manyConditionalSkip() {
        Observable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .filter(v -> v % 2 == 0)
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void takeConditional() {
        Observable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .filter(v -> true)
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void noOfferNoCrashAfterClear() throws Throwable {
        AtomicReference<SimpleQueue<?>> queue = new AtomicReference<>();

        Observable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                queue.set((SimpleQueue<?>)d);
                ((QueueDisposable<?>)d).requestFusion(QueueFuseable.ANY);
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        SimpleQueue<?> q = queue.get();
        TestHelper.assertNoOffer(q);

        assertFalse(q.isEmpty());

        q.clear();

        assertNull(q.poll());

        assertTrue(q.isEmpty());

        q.clear();

        assertNull(q.poll());

        assertTrue(q.isEmpty());
    }

    @Test
    public void fusedPoll() throws Throwable {
        AtomicReference<SimpleQueue<?>> queue = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();

        Observable.fromStream(Stream.of(1).onClose(() -> calls.getAndIncrement()))
        .subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                queue.set((SimpleQueue<?>)d);
                ((QueueDisposable<?>)d).requestFusion(QueueFuseable.ANY);
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        SimpleQueue<?> q = queue.get();

        assertFalse(q.isEmpty());

        assertEquals(1, q.poll());

        assertTrue(q.isEmpty());

        assertEquals(1, calls.get());
    }

    @Test
    public void fusedPoll2() throws Throwable {
        AtomicReference<SimpleQueue<?>> queue = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();

        Observable.fromStream(Stream.of(1, 2).onClose(() -> calls.getAndIncrement()))
        .subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                queue.set((SimpleQueue<?>)d);
                ((QueueDisposable<?>)d).requestFusion(QueueFuseable.ANY);
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        SimpleQueue<?> q = queue.get();

        assertFalse(q.isEmpty());

        assertEquals(1, q.poll());

        assertFalse(q.isEmpty());

        assertEquals(2, q.poll());

        assertTrue(q.isEmpty());

        assertEquals(1, calls.get());
    }

    @Test
    public void streamOfNull() {
        Observable.fromStream(Stream.of((Integer)null))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void streamOfNullConditional() {
        Observable.fromStream(Stream.of((Integer)null))
        .filter(v -> true)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void syncFusionSupport() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.ANY);

        Observable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .subscribeWith(to)
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void asyncFusionNotSupported() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.ASYNC);

        Observable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .subscribeWith(to)
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void runToEndCloseCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).onClose(() -> { throw new TestException(); });

            Observable.fromStream(stream)
            .test()
            .assertResult(1, 2, 3, 4, 5);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void takeCloseCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).onClose(() -> { throw new TestException(); });

            Observable.fromStream(stream)
            .take(3)
            .test()
            .assertResult(1, 2, 3);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void hasNextCrash() {
        AtomicInteger v = new AtomicInteger();
        Observable.fromStream(Stream.<Integer>generate(() -> {
            int value = v.getAndIncrement();
            if (value == 1) {
                throw new TestException();
            }
            return value;
        }))
        .test()
        .assertFailure(TestException.class, 0);
    }

    @Test
    public void hasNextCrashConditional() {
        AtomicInteger counter = new AtomicInteger();
        Observable.fromStream(Stream.<Integer>generate(() -> {
            int value = counter.getAndIncrement();
            if (value == 1) {
                throw new TestException();
            }
            return value;
        }))
        .filter(v -> true)
        .test()
        .assertFailure(TestException.class, 0);
    }

    @Test
    public void closeCalledOnEmpty() {
        AtomicInteger calls = new AtomicInteger();

        Observable.fromStream(Stream.of().onClose(() -> calls.getAndIncrement()))
        .test()
        .assertResult();

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledAfterItems() {
        AtomicInteger calls = new AtomicInteger();

        Observable.fromStream(Stream.of(1, 2, 3, 4, 5).onClose(() -> calls.getAndIncrement()))
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledOnCancel() {
        AtomicInteger calls = new AtomicInteger();

        Observable.fromStream(Stream.of(1, 2, 3, 4, 5).onClose(() -> calls.getAndIncrement()))
        .take(3)
        .test()
        .assertResult(1, 2, 3);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledOnItemCrash() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        Observable.fromStream(Stream.<Integer>generate(() -> {
            int value = counter.getAndIncrement();
            if (value == 1) {
                throw new TestException();
            }
            return value;
        }).onClose(() -> calls.getAndIncrement()))
        .test()
        .assertFailure(TestException.class, 0);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledAfterItemsConditional() {
        AtomicInteger calls = new AtomicInteger();

        Observable.fromStream(Stream.of(1, 2, 3, 4, 5).onClose(() -> calls.getAndIncrement()))
        .filter(v -> true)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledOnCancelConditional() {
        AtomicInteger calls = new AtomicInteger();

        Observable.fromStream(Stream.of(1, 2, 3, 4, 5).onClose(() -> calls.getAndIncrement()))
        .filter(v -> true)
        .take(3)
        .test()
        .assertResult(1, 2, 3);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledOnItemCrashConditional() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        Observable.fromStream(Stream.<Integer>generate(() -> {
            int value = counter.getAndIncrement();
            if (value == 1) {
                throw new TestException();
            }
            return value;
        }).onClose(() -> calls.getAndIncrement()))
        .filter(v -> true)
        .test()
        .assertFailure(TestException.class, 0);

        assertEquals(1, calls.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.fromStream(Stream.of(1)));
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

        Observable.fromStream(stream)
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void cancelAfterIteratorHasNext() throws Exception {
        TestObserver<Integer> to = new TestObserver<>();

        @SuppressWarnings("unchecked")
        Stream<Integer> stream = mock(Stream.class);
        when(stream.iterator()).thenReturn(new Iterator<Integer>() {

            int calls;

            @Override
            public boolean hasNext() {
                if (++calls == 1) {
                    to.dispose();
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }
        });

        Observable.fromStream(stream)
        .subscribe(to);

        to.assertEmpty();
    }
}
