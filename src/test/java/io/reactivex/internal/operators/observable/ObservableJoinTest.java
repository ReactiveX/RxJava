/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.internal.operators.observable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.*;
import org.mockito.MockitoAnnotations;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;

public class ObservableJoinTest {
    Observer<Object> observer = TestHelper.mockObserver();

    BiFunction<Integer, Integer, Integer> add = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    <T> Function<Integer, Observable<T>> just(final Observable<T> observable) {
        return new Function<Integer, Observable<T>>() {
            @Override
            public Observable<T> apply(Integer t1) {
                return observable;
            }
        };
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void normal1() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> m = source1.join(source2,
                just(Observable.never()),
                just(Observable.never()), add);

        m.subscribe(observer);

        source1.onNext(1);
        source1.onNext(2);
        source1.onNext(4);

        source2.onNext(16);
        source2.onNext(32);
        source2.onNext(64);

        source1.onComplete();
        source2.onComplete();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(20);
        verify(observer, times(1)).onNext(33);
        verify(observer, times(1)).onNext(34);
        verify(observer, times(1)).onNext(36);
        verify(observer, times(1)).onNext(65);
        verify(observer, times(1)).onNext(66);
        verify(observer, times(1)).onNext(68);

        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void normal1WithDuration() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        PublishSubject<Integer> duration1 = PublishSubject.create();

        Observable<Integer> m = source1.join(source2,
                just(duration1),
                just(Observable.never()), add);
        m.subscribe(observer);

        source1.onNext(1);
        source1.onNext(2);
        source2.onNext(16);

        duration1.onNext(1);

        source1.onNext(4);
        source1.onNext(8);

        source1.onComplete();
        source2.onComplete();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(20);
        verify(observer, times(1)).onNext(24);

        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

    }

    @Test
    public void normal2() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> m = source1.join(source2,
                just(Observable.never()),
                just(Observable.never()), add);

        m.subscribe(observer);

        source1.onNext(1);
        source1.onNext(2);
        source1.onComplete();

        source2.onNext(16);
        source2.onNext(32);
        source2.onNext(64);

        source2.onComplete();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(33);
        verify(observer, times(1)).onNext(34);
        verify(observer, times(1)).onNext(65);
        verify(observer, times(1)).onNext(66);

        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void leftThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> m = source1.join(source2,
                just(Observable.never()),
                just(Observable.never()), add);

        m.subscribe(observer);

        source2.onNext(1);
        source1.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> m = source1.join(source2,
                just(Observable.never()),
                just(Observable.never()), add);

        m.subscribe(observer);

        source1.onNext(1);
        source2.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void leftDurationThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> duration1 = Observable.<Integer> error(new RuntimeException("Forced failure"));

        Observable<Integer> m = source1.join(source2,
                just(duration1),
                just(Observable.never()), add);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> duration1 = Observable.<Integer> error(new RuntimeException("Forced failure"));

        Observable<Integer> m = source1.join(source2,
                just(Observable.never()),
                just(duration1), add);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void leftDurationSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Function<Integer, Observable<Integer>> fail = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Integer> m = source1.join(source2,
                fail,
                just(Observable.never()), add);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Function<Integer, Observable<Integer>> fail = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Integer> m = source1.join(source2,
                just(Observable.never()),
                fail, add);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void resultSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        BiFunction<Integer, Integer, Integer> fail = new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Integer> m = source1.join(source2,
                just(Observable.never()),
                just(Observable.never()), fail);
        m.subscribe(observer);

        source1.onNext(1);
        source2.onNext(2);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.<Integer>create().join(Observable.just(1),
                Functions.justFunction(Observable.never()),
                Functions.justFunction(Observable.never()), new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer a, Integer b) throws Exception {
                        return a + b;
                    }
                }));
    }

    @Test
    public void take() {
        Observable.just(1).join(
                Observable.just(2),
                Functions.justFunction(Observable.never()),
                Functions.justFunction(Observable.never()),
                new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer a, Integer b) throws Exception {
                        return a + b;
                    }
                })
        .take(1)
        .test()
        .assertResult(3);
    }

    @Test
    public void rightClose() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.join(Observable.just(2),
                Functions.justFunction(Observable.never()),
                Functions.justFunction(Observable.empty()),
                new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer a, Integer b) throws Exception {
                        return a + b;
                    }
            })
        .test()
        .assertEmpty();

        ps.onNext(1);

        to.assertEmpty();
    }

    @Test
    public void resultSelectorThrows2() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.join(
                Observable.just(2),
                Functions.justFunction(Observable.never()),
                Functions.justFunction(Observable.never()),
                new BiFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer a, Integer b) throws Exception {
                        throw new TestException();
                    }
                })
        .test();

        ps.onNext(1);
        ps.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void badOuterSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new TestException("First"));
                    observer.onError(new TestException("Second"));
                }
            }
            .join(Observable.just(2),
                    Functions.justFunction(Observable.never()),
                    Functions.justFunction(Observable.never()),
                    new BiFunction<Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b) throws Exception {
                            return a + b;
                        }
                })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badEndSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            @SuppressWarnings("rawtypes")
            final Observer[] o = { null };

            TestObserver<Integer> to = Observable.just(1)
            .join(Observable.just(2),
                    Functions.justFunction(Observable.never()),
                    Functions.justFunction(new Observable<Integer>() {
                        @Override
                        protected void subscribeActual(Observer<? super Integer> observer) {
                            o[0] = observer;
                            observer.onSubscribe(Disposables.empty());
                            observer.onError(new TestException("First"));
                        }
                    }),
                    new BiFunction<Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b) throws Exception {
                            return a + b;
                        }
                })
            .test();

            o[0].onError(new TestException("Second"));

            to
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
