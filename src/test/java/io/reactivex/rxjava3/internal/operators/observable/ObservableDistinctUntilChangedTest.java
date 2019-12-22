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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableDistinctUntilChangedTest extends RxJavaTest {

    Observer<String> w;
    Observer<String> w2;

    // nulls lead to exceptions
    final Function<String, String> TO_UPPER_WITH_EXCEPTION = new Function<String, String>() {
        @Override
        public String apply(String s) {
            if (s.equals("x")) {
                return "xx";
            }
            return s.toUpperCase();
        }
    };

    @Before
    public void before() {
        w = TestHelper.mockObserver();
        w2 = TestHelper.mockObserver();
    }

    @Test
    public void distinctUntilChangedOfNone() {
        Observable<String> src = Observable.empty();
        src.distinctUntilChanged().subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void distinctUntilChangedOfNoneWithKeySelector() {
        Observable<String> src = Observable.empty();
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void distinctUntilChangedOfNormalSource() {
        Observable<String> src = Observable.just("a", "b", "c", "c", "c", "b", "b", "a", "e");
        src.distinctUntilChanged().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void distinctUntilChangedOfNormalSourceWithKeySelector() {
        Observable<String> src = Observable.just("a", "b", "c", "C", "c", "B", "b", "a", "e");
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("B");
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void customComparator() {
        Observable<String> source = Observable.just("a", "b", "B", "A", "a", "C");

        TestObserver<String> to = TestObserver.create();

        source.distinctUntilChanged(new BiPredicate<String, String>() {
            @Override
            public boolean test(String a, String b) {
                return a.compareToIgnoreCase(b) == 0;
            }
        })
        .subscribe(to);

        to.assertValues("a", "b", "A", "C");
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void customComparatorThrows() {
        Observable<String> source = Observable.just("a", "b", "B", "A", "a", "C");

        TestObserver<String> to = TestObserver.create();

        source.distinctUntilChanged(new BiPredicate<String, String>() {
            @Override
            public boolean test(String a, String b) {
                throw new TestException();
            }
        })
        .subscribe(to);

        to.assertValue("a");
        to.assertNotComplete();
        to.assertError(TestException.class);
    }

    @Test
    public void fused() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        Observable.just(1, 2, 2, 3, 3, 4, 5)
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) throws Exception {
                return a.equals(b);
            }
        })
        .subscribe(to);

        to.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void fusedAsync() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        UnicastSubject<Integer> up = UnicastSubject.create();

        up
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) throws Exception {
                return a.equals(b);
            }
        })
        .subscribe(to);

        TestHelper.emit(up, 1, 2, 2, 3, 3, 4, 5);

        to.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void ignoreCancel() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Observable.wrap(new ObservableSource<Integer>() {
                @Override
                public void subscribe(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onNext(3);
                    observer.onError(new IOException());
                    observer.onComplete();
                }
            })
            .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
                @Override
                public boolean test(Integer a, Integer b) throws Exception {
                    throw new TestException();
                }
            })
            .test()
            .assertFailure(TestException.class, 1);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
   }

    class Mutable {
        int value;
    }

    @Test
    public void mutableWithSelector() {
        Mutable m = new Mutable();

        PublishSubject<Mutable> ps = PublishSubject.create();

        TestObserver<Mutable> to = ps.distinctUntilChanged(new Function<Mutable, Object>() {
            @Override
            public Object apply(Mutable m) throws Exception {
                return m.value;
            }
        })
        .test();

        ps.onNext(m);
        m.value = 1;
        ps.onNext(m);
        ps.onComplete();

        to.assertResult(m, m);
    }
}
