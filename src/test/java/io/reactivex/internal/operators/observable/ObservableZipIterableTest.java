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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.util.CrashingIterable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;

public class ObservableZipIterableTest {
    BiFunction<String, String, String> concat2Strings;
    PublishSubject<String> s1;
    PublishSubject<String> s2;
    Observable<String> zipped;

    Observer<String> observer;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = new BiFunction<String, String, String>() {
            @Override
            public String apply(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = PublishSubject.create();
        s2 = PublishSubject.create();
        zipped = Observable.zip(s1, s2, concat2Strings);

        observer = TestHelper.mockObserver();
        inOrder = inOrder(observer);

        zipped.subscribe(observer);
    }

    BiFunction<Object, Object, String> zipr2 = new BiFunction<Object, Object, String>() {

        @Override
        public String apply(Object t1, Object t2) {
            return "" + t1 + t2;
        }

    };
    Function3<Object, Object, Object, String> zipr3 = new Function3<Object, Object, Object, String>() {

        @Override
        public String apply(Object t1, Object t2, Object t3) {
            return "" + t1 + t2 + t3;
        }

    };

    @Test
    public void testZipIterableSameSize() {
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
    public void testZipIterableEmptyFirstSize() {
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
    public void testZipIterableEmptySecond() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList();

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
    public void testZipIterableFirstShorter() {
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
    public void testZipIterableSecondShorter() {
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
    public void testZipIterableFirstThrows() {
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
    public void testZipIterableIteratorThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                throw new TestException();
            }
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
    public void testZipIterableHasNextThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
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
    public void testZipIterableNextThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define an Observer to receive aggregated events */
        Observer<String> o = TestHelper.mockObserver();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
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
            }

        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onError(new TestException());

        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onComplete();

    }

    Consumer<String> printer = new Consumer<String>() {
        @Override
        public void accept(String pv) {
            System.out.println(pv);
        }
    };

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
    public void testTake2() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4, 5);
        Iterable<String> it = Arrays.asList("a", "b", "c", "d", "e");

        SquareStr squareStr = new SquareStr();

        o.map(squareStr).zipWith(it, concat2Strings).take(2).subscribe(printer);

        assertEquals(2, squareStr.counter.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).zipWith(Arrays.asList(1), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Integer>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Integer> o) throws Exception {
                return o.zipWith(Arrays.asList(1), new BiFunction<Integer, Integer, Object>() {
                    @Override
                    public Object apply(Integer a, Integer b) throws Exception {
                        return a + b;
                    }
                });
            }
        });
    }

    @Test
    public void iteratorThrows() {
        Observable.just(1).zipWith(new CrashingIterable(100, 1, 100), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");
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
            .zipWith(Arrays.asList(1), new BiFunction<Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b) throws Exception {
                    return a + b;
                }
            })
            .test()
            .assertResult(2);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
