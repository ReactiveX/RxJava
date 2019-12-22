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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableScanTest extends RxJavaTest {

    @Test
    public void scanIntegersWithInitialValue() {
        Observer<String> observer = TestHelper.mockObserver();

        Observable<Integer> o = Observable.just(1, 2, 3);

        Observable<String> m = o.scan("", new BiFunction<String, Integer, String>() {

            @Override
            public String apply(String s, Integer n) {
                return s + n.toString();
            }

        });
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext("");
        verify(observer, times(1)).onNext("1");
        verify(observer, times(1)).onNext("12");
        verify(observer, times(1)).onNext("123");
        verify(observer, times(4)).onNext(anyString());
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void scanIntegersWithoutInitialValue() {
        Observer<Integer> observer = TestHelper.mockObserver();

        Observable<Integer> o = Observable.just(1, 2, 3);

        Observable<Integer> m = o.scan(new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(0);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(6);
        verify(observer, times(3)).onNext(anyInt());
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void scanIntegersWithoutInitialValueAndOnlyOneValue() {
        Observer<Integer> observer = TestHelper.mockObserver();

        Observable<Integer> o = Observable.just(1);

        Observable<Integer> m = o.scan(new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(0);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(anyInt());
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void shouldNotEmitUntilAfterSubscription() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(1, 100).scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }).filter(new Predicate<Integer>() {

            @Override
            public boolean test(Integer t1) {
                // this will cause request(1) when 0 is emitted
                return t1 > 0;
            }

        }).subscribe(to);

        assertEquals(100, to.values().size());
    }

    @Test
    public void noBackpressureWithInitialValue() {
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, 100)
                .scan(0, new BiFunction<Integer, Integer, Integer>() {

                    @Override
                    public Integer apply(Integer t1, Integer t2) {
                        return t1 + t2;
                    }

                })
                .subscribe(new DefaultObserver<Integer>() {

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        fail(e.getMessage());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                    }

                });

        // we only expect to receive 101 as we'll receive all 100 + the initial value
        assertEquals(101, count.get());
    }

    /**
     * This uses the public API collect which uses scan under the covers.
     */
    @Test
    public void seedFactory() {
        Observable<List<Integer>> o = Observable.range(1, 10)
                .collect(new Supplier<List<Integer>>() {

                    @Override
                    public List<Integer> get() {
                        return new ArrayList<>();
                    }

                }, new BiConsumer<List<Integer>, Integer>() {

                    @Override
                    public void accept(List<Integer> list, Integer t2) {
                        list.add(t2);
                    }

                }).toObservable().takeLast(1);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.blockingSingle());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), o.blockingSingle());
    }

    @Test
    public void scanWithRequestOne() {
        Observable<Integer> o = Observable.just(1, 2).scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }).take(1);

        TestObserverEx<Integer> observer = new TestObserverEx<>();

        o.subscribe(observer);
        observer.assertValue(0);
        observer.assertTerminated();
        observer.assertNoErrors();
    }

    @Test
    public void initialValueEmittedNoProducer() {
        PublishSubject<Integer> source = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<>();

        source.scan(0, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        }).subscribe(to);

        to.assertNoErrors();
        to.assertNotComplete();
        to.assertValue(0);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().scan(new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return a;
            }
        }));

        TestHelper.checkDisposed(PublishSubject.<Integer>create().scan(0, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.scan(new BiFunction<Object, Object, Object>() {
                    @Override
                    public Object apply(Object a, Object b) throws Exception {
                        return a;
                    }
                });
            }
        });

        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.scan(0, new BiFunction<Object, Object, Object>() {
                    @Override
                    public Object apply(Object a, Object b) throws Exception {
                        return a;
                    }
                });
            }
        });
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .scan(new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return a;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Object>, Object>() {
            @Override
            public Object apply(Observable<Object> o) throws Exception {
                return o.scan(0, new BiFunction<Object, Object, Object>() {
                    @Override
                    public Object apply(Object a, Object b) throws Exception {
                        return a;
                    }
                });
            }
        }, false, 1, 1, 0, 0);
    }

    @Test
    public void scanFunctionThrowsAndUpstreamErrorsDoesNotResultInTwoTerminalEvents() {
        final RuntimeException err = new RuntimeException();
        final RuntimeException err2 = new RuntimeException();
        final List<Throwable> list = new CopyOnWriteArrayList<>();
        final Consumer<Throwable> errorConsumer = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) throws Exception {
                list.add(t);
            }};
        try {
            RxJavaPlugins.setErrorHandler(errorConsumer);
            Observable.unsafeCreate(new ObservableSource<Integer>() {
                @Override
                public void subscribe(Observer<? super Integer> o) {
                    Disposable d = Disposable.empty();
                    o.onSubscribe(d);
                    o.onNext(1);
                    o.onNext(2);
                    o.onError(err2);
                }})
            .scan(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer t1, Integer t2) throws Exception {
                    throw err;
                }})
            .test()
            .assertError(err)
            .assertValue(1);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void scanFunctionThrowsAndUpstreamCompletesDoesNotResultInTwoTerminalEvents() {
        final RuntimeException err = new RuntimeException();
        Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> o) {
                Disposable d = Disposable.empty();
                o.onSubscribe(d);
                o.onNext(1);
                o.onNext(2);
                o.onComplete();
            }})
        .scan(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) throws Exception {
                throw err;
            }})
        .test()
        .assertError(err)
        .assertValue(1);
    }

    @Test
    public void scanFunctionThrowsAndUpstreamEmitsOnNextResultsInScanFunctionBeingCalledOnlyOnce() {
        final RuntimeException err = new RuntimeException();
        final AtomicInteger count = new AtomicInteger();
        Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> o) {
                Disposable d = Disposable.empty();
                o.onSubscribe(d);
                o.onNext(1);
                o.onNext(2);
                o.onNext(3);
            }})
        .scan(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) throws Exception {
                count.incrementAndGet();
                throw err;
            }})
        .test()
        .assertError(err)
        .assertValue(1);
        assertEquals(1, count.get());
    }
}
