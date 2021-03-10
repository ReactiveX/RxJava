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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.util.CrashingIterable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableZipIterableTest extends RxJavaTest {
    BiFunction<String, String, String> concat2Strings;
    PublishSubject<String> s1;
    PublishSubject<String> s2;
    Observable<String> zipped;

    Observer<String> observer;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = (t1, t2) -> t1 + "-" + t2;

        s1 = PublishSubject.create();
        s2 = PublishSubject.create();
        zipped = Observable.zip(s1, s2, concat2Strings);

        observer = TestHelper.mockObserver();
        inOrder = inOrder(observer);

        zipped.subscribe(observer);
    }

    BiFunction<Object, Object, String> zipr2 = (t1, t2) -> "" + t1 + t2;
    Function3<Object, Object, Object, String> zipr3 = (t1, t2, t3) -> "" + t1 + t2 + t3;

    @Test
    public void zipIterableSameSize() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onNext("three-3");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void zipIterableEmptyFirstSize() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onComplete();

        io.verify(o).onComplete();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void zipIterableEmptySecond() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Collections.emptyList();

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onComplete();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void zipIterableFirstShorter() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void zipIterableSecondShorter() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void zipIterableFirstThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();

    }

    @Test
    public void zipIterableIteratorThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = () -> {
            throw new TestException();
        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any(String.class));

    }

    @Test
    public void zipIterableHasNextThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = () -> new Iterator<String>() {
            int count;

            @Override
            public boolean hasNext() {
                if (count == 0) {
                    return true;
                }
                throw new TestException();
            }

            @Override
            public String next() {
                count++;
                return "1";
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onError(new TestException());

        io.verify(o).onNext("one-1");
        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();

    }

    @Test
    public void zipIterableNextThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = () -> new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                throw new TestException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onError(new TestException());

        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onComplete();

    }

    Consumer<String> printer = System.out::println;

    static final class SquareStr implements Function<Integer, String> {
        final AtomicInteger counter = new AtomicInteger();
        @Override
        public String apply(Integer t1) {
            counter.incrementAndGet();
            System.out.println("Omg I'm calculating so hard: " + t1 + "*" + t1 + "=" + (t1 * t1));
            return " " + (t1 * t1);
        }
    }

    @Test
    public void take2() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4, 5);
        Iterable<String> it = Arrays.asList("a", "b", "c", "d", "e");

        SquareStr squareStr = new SquareStr();

        o.map(squareStr).zipWith(it, concat2Strings).take(2).subscribe(printer);

        assertEquals(2, squareStr.counter.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).zipWith(Collections.singletonList(1), (BiFunction<Integer, Integer, Object>) Integer::sum));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable((Function<Observable<Integer>, ObservableSource<Object>>) o -> o.zipWith(Collections.singletonList(1), Integer::sum));
    }

    @Test
    public void iteratorThrows() {
        Observable.just(1).zipWith(new CrashingIterable(100, 1, 100), (BiFunction<Integer, Integer, Object>) Integer::sum)
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(@NonNull Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .zipWith(Collections.singletonList(1), (BiFunction<Integer, Integer, Object>) Integer::sum)
            .test()
            .assertResult(2);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
