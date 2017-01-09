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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ObservableWindowWithSizeTest {

    private static <T> List<List<T>> toLists(Observable<Observable<T>> observables) {

        final List<List<T>> lists = new ArrayList<List<T>>();
        Observable.concat(observables.map(new Function<Observable<T>, Observable<List<T>>>() {
            @Override
            public Observable<List<T>> apply(Observable<T> xs) {
                return xs.toList().toObservable();
            }
        }))
                .blockingForEach(new Consumer<List<T>>() {
                    @Override
                    public void accept(List<T> xs) {
                        lists.add(xs);
                    }
                });
        return lists;
    }

    @Test
    public void testNonOverlappingWindows() {
        Observable<String> subject = Observable.just("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testSkipAndCountGaplessWindows() {
        Observable<String> subject = Observable.just("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(3, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testOverlappingWindows() {
        Observable<String> subject = Observable.fromArray(new String[] { "zero", "one", "two", "three", "four", "five" });
        Observable<Observable<String>> windowed = subject.window(3, 1);

        List<List<String>> windows = toLists(windowed);

        assertEquals(6, windows.size());
        assertEquals(list("zero", "one", "two"), windows.get(0));
        assertEquals(list("one", "two", "three"), windows.get(1));
        assertEquals(list("two", "three", "four"), windows.get(2));
        assertEquals(list("three", "four", "five"), windows.get(3));
        assertEquals(list("four", "five"), windows.get(4));
        assertEquals(list("five"), windows.get(5));
    }

    @Test
    public void testSkipAndCountWindowsWithGaps() {
        Observable<String> subject = Observable.just("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(2, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testWindowUnsubscribeNonOverlapping() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 10000).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                count.incrementAndGet();
            }

        }).window(5).take(2)).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        //        System.out.println(ts.getOnNextEvents());
        assertEquals(10, count.get());
    }

    @Test
    public void testWindowUnsubscribeNonOverlappingAsyncSource() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 100000)
                .doOnNext(new Consumer<Integer>() {

                    @Override
                    public void accept(Integer t1) {
                        if (count.incrementAndGet() == 500000) {
                            // give it a small break halfway through
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ex) {
                                // ignored
                            }
                        }
                    }

                })
                .observeOn(Schedulers.computation())
                .window(5)
                .take(2))
                .subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // make sure we don't emit all values ... the unsubscribe should propagate
        assertTrue(count.get() < 100000);
    }

    @Test
    public void testWindowUnsubscribeOverlapping() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 10000).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                count.incrementAndGet();
            }

        }).window(5, 4).take(2)).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        //        System.out.println(ts.getOnNextEvents());
        ts.assertValues(1, 2, 3, 4, 5, 5, 6, 7, 8, 9);
        assertEquals(9, count.get());
    }

    @Test
    public void testWindowUnsubscribeOverlappingAsyncSource() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 100000)
                .doOnNext(new Consumer<Integer>() {

                    @Override
                    public void accept(Integer t1) {
                        count.incrementAndGet();
                    }

                })
                .observeOn(Schedulers.computation())
                .window(5, 4)
                .take(2), 128)
                .subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 5, 6, 7, 8, 9);
        // make sure we don't emit all values ... the unsubscribe should propagate
        // assertTrue(count.get() < 100000); // disabled: a small hiccup in the consumption may allow the source to run to completion
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<String>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }


    public static Observable<Integer> hotStream() {
        return Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> s) {
                Disposable d = Disposables.empty();
                s.onSubscribe(d);
                while (!d.isDisposed()) {
                    // burst some number of items
                    for (int i = 0; i < Math.random() * 20; i++) {
                        s.onNext(i);
                    }
                    try {
                        // sleep for a random amount of time
                        // NOTE: Only using Thread.sleep here as an artificial demo.
                        Thread.sleep((long) (Math.random() * 200));
                    } catch (Exception e) {
                        // do nothing
                    }
                }
                System.out.println("Hot done.");
            }
        }).subscribeOn(Schedulers.newThread()); // use newThread since we are using sleep to block
    }

    @Test
    public void testTakeFlatMapCompletes() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        final int indicator = 999999999;

        hotStream()
        .window(10)
        .take(2)
        .flatMap(new Function<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> w) {
                return w.startWith(indicator);
            }
        }).subscribe(ts);

        ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
        ts.assertComplete();
        ts.assertValueCount(22);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().window(1));

        TestHelper.checkDisposed(PublishSubject.create().window(2, 1));

        TestHelper.checkDisposed(PublishSubject.create().window(1, 2));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Observable<Object>>>() {
            @Override
            public ObservableSource<Observable<Object>> apply(Observable<Object> o) throws Exception {
                return o.window(1);
            }
        });

        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Observable<Object>>>() {
            @Override
            public ObservableSource<Observable<Object>> apply(Observable<Object> o) throws Exception {
                return o.window(2, 1);
            }
        });

        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Observable<Object>>>() {
            @Override
            public ObservableSource<Observable<Object>> apply(Observable<Object> o) throws Exception {
                return o.window(1, 2);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorExact() {
        Observable.error(new TestException())
        .window(1)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorSkip() {
        Observable.error(new TestException())
        .window(1, 2)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorOverlap() {
        Observable.error(new TestException())
        .window(2, 1)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorExactInner() {
        @SuppressWarnings("rawtypes")
        final TestObserver[] to = { null };
        Observable.just(1).concatWith(Observable.<Integer>error(new TestException()))
        .window(2)
        .doOnNext(new Consumer<Observable<Integer>>() {
            @Override
            public void accept(Observable<Integer> w) throws Exception {
                to[0] = w.test();
            }
        })
        .test()
        .assertError(TestException.class);

        to[0].assertFailure(TestException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorSkipInner() {
        @SuppressWarnings("rawtypes")
        final TestObserver[] to = { null };
        Observable.just(1).concatWith(Observable.<Integer>error(new TestException()))
        .window(2, 3)
        .doOnNext(new Consumer<Observable<Integer>>() {
            @Override
            public void accept(Observable<Integer> w) throws Exception {
                to[0] = w.test();
            }
        })
        .test()
        .assertError(TestException.class);

        to[0].assertFailure(TestException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorOverlapInner() {
        @SuppressWarnings("rawtypes")
        final TestObserver[] to = { null };
        Observable.just(1).concatWith(Observable.<Integer>error(new TestException()))
        .window(3, 2)
        .doOnNext(new Consumer<Observable<Integer>>() {
            @Override
            public void accept(Observable<Integer> w) throws Exception {
                to[0] = w.test();
            }
        })
        .test()
        .assertError(TestException.class);

        to[0].assertFailure(TestException.class, 1);
    }
}
