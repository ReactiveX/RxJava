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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.Callable;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.TestHelper;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.observers.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.UnicastSubject;

public class ObservableDistinctTest {

    Observer<String> w;

    // nulls lead to exceptions
    final Function<String, String> TO_UPPER_WITH_EXCEPTION = new Function<String, String>() {
        @Override
        public String apply(String s) {
            if (s.equals("x")) {
                return "XX";
            }
            return s.toUpperCase();
        }
    };

    @Before
    public void before() {
        w = TestHelper.mockObserver();
    }

    @Test
    public void testDistinctOfNone() {
        Observable<String> src = Observable.empty();
        src.distinct().subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testDistinctOfNoneWithKeySelector() {
        Observable<String> src = Observable.empty();
        src.distinct(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testDistinctOfNormalSource() {
        Observable<String> src = Observable.just("a", "b", "c", "c", "c", "b", "b", "a", "e");
        src.distinct().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctOfNormalSourceWithKeySelector() {
        Observable<String> src = Observable.just("a", "B", "c", "C", "c", "B", "b", "a", "E");
        src.distinct(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("B");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("E");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testDistinctOfSourceWithNulls() {
        Observable<String> src = Observable.just(null, "a", "a", null, null, "b", null);
        src.distinct().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testDistinctOfSourceWithExceptionsFromKeySelector() {
        Observable<String> src = Observable.just("a", "b", null, "c");
        src.distinct(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onError(any(NullPointerException.class));
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, never()).onComplete();
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .distinct()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedSync() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        Observable.just(1, 1, 2, 1, 3, 2, 4, 5, 4)
        .distinct()
        .subscribe(to);

        ObserverFusion.assertFusion(to, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedAsync() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        UnicastSubject<Integer> us = UnicastSubject.create();

        us
        .distinct()
        .subscribe(to);

        TestHelper.emit(us, 1, 1, 2, 1, 3, 2, 4, 5, 4);

        ObserverFusion.assertFusion(to, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedClear() {
        Observable.just(1, 1, 2, 1, 3, 2, 4, 5, 4)
        .distinct()
        .subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                QueueDisposable<?> qd = (QueueDisposable<?>)d;

                assertFalse(qd.isEmpty());

                qd.clear();

                assertTrue(qd.isEmpty());
            }

            @Override
            public void onNext(Integer value) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void collectionSupplierThrows() {
        Observable.just(1)
        .distinct(Functions.identity(), new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectionSupplierIsNull() {
        Observable.just(1)
        .distinct(Functions.identity(), new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class)
        .assertErrorMessage("The collectionSupplier returned a null collection. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .distinct()
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
