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

package io.reactivex.rxjava3.internal.operators.mixed;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableSwitchMapSingleTest extends RxJavaTest {

    @Test
    public void simple() {
        Observable.range(1, 5)
        .switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .switchMapSingle(Functions.justFunction(Single.never()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Observable.just(1)
        .switchMapSingle(Functions.justFunction(Single.error(new TestException())))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> f)
                    throws Exception {
                return f
                        .switchMapSingle(Functions.justFunction(Single.never()));
            }
        }
        );
    }

    @Test
    public void take() {
        Observable.range(1, 5)
        .switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
            }
        })
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void switchOver() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final SingleSubject<Integer> ms1 = SingleSubject.create();
        final SingleSubject<Integer> ms2 = SingleSubject.create();

        TestObserver<Integer> to = ps.switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                        if (v == 1) {
                            return ms1;
                        }
                        return ms2;
                    }
        }).test();

        to.assertEmpty();

        ps.onNext(1);

        to.assertEmpty();

        assertTrue(ms1.hasObservers());

        ps.onNext(2);

        assertFalse(ms1.hasObservers());
        assertTrue(ms2.hasObservers());

        ms2.onError(new TestException());

        assertFalse(ps.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void switchOverDelayError() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final SingleSubject<Integer> ms1 = SingleSubject.create();
        final SingleSubject<Integer> ms2 = SingleSubject.create();

        TestObserver<Integer> to = ps.switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                        if (v == 1) {
                            return ms1;
                        }
                        return ms2;
                    }
        }).test();

        to.assertEmpty();

        ps.onNext(1);

        to.assertEmpty();

        assertTrue(ms1.hasObservers());

        ps.onNext(2);

        assertFalse(ms1.hasObservers());
        assertTrue(ms2.hasObservers());

        ms2.onError(new TestException());

        to.assertEmpty();

        assertTrue(ps.hasObservers());

        ps.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorInnerCompleteDelayError() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final SingleSubject<Integer> ms = SingleSubject.create();

        TestObserver<Integer> to = ps.switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                        return ms;
                    }
        }).test();

        to.assertEmpty();

        ps.onNext(1);

        to.assertEmpty();

        assertTrue(ms.hasObservers());

        ps.onError(new TestException());

        assertTrue(ms.hasObservers());

        to.assertEmpty();

        ms.onSuccess(1);

        to.assertFailure(TestException.class, 1);
    }

    @Test
    public void mainErrorInnerSuccessDelayError() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final SingleSubject<Integer> ms = SingleSubject.create();

        TestObserver<Integer> to = ps.switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                        return ms;
                    }
        }).test();

        to.assertEmpty();

        ps.onNext(1);

        to.assertEmpty();

        assertTrue(ms.hasObservers());

        ps.onError(new TestException());

        assertTrue(ms.hasObservers());

        to.assertEmpty();

        ms.onSuccess(1);

        to.assertFailure(TestException.class, 1);
    }

    @Test
    public void mapperCrash() {
        Observable.just(1).hide()
        .switchMapSingle(new Function<Integer, SingleSource<? extends Object>>() {
            @Override
            public SingleSource<? extends Object> apply(Integer v)
                    throws Exception {
                        throw new TestException();
                    }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void disposeBeforeSwitchInOnNext() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.just(1).hide()
        .switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                        to.dispose();
                        return Single.just(1);
                    }
        }).subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void disposeOnNextAfterFirst() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.just(1, 2)
        .switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                if (v == 2) {
                    to.dispose();
                }
                return Single.just(1);
            }
        }).subscribe(to);

        to.assertValue(1)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void cancel() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final SingleSubject<Integer> ms = SingleSubject.create();

        TestObserver<Integer> to = ps.switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                        return ms;
                    }
        }).test();

        to.assertEmpty();

        ps.onNext(1);

        to.assertEmpty();

        assertTrue(ps.hasObservers());
        assertTrue(ms.hasObservers());

        to.dispose();

        assertFalse(ps.hasObservers());
        assertFalse(ms.hasObservers());
    }

    @Test
    public void mainErrorAfterTermination() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onError(new TestException("outer"));
                }
            }
            .switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
                @Override
                public SingleSource<Integer> apply(Integer v)
                        throws Exception {
                    return Single.error(new TestException("inner"));
                }
            })
            .to(TestHelper.<Integer>testConsumer())
            .assertFailureAndMessage(TestException.class, "inner");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "outer");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void innerErrorAfterTermination() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final AtomicReference<SingleObserver<? super Integer>> moRef = new AtomicReference<>();

            TestObserverEx<Integer> to = new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onError(new TestException("outer"));
                }
            }
            .switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
                @Override
                public SingleSource<Integer> apply(Integer v)
                        throws Exception {
                    return new Single<Integer>() {
                        @Override
                        protected void subscribeActual(
                                SingleObserver<? super Integer> observer) {
                            observer.onSubscribe(Disposable.empty());
                            moRef.set(observer);
                        }
                    };
                }
            })
            .to(TestHelper.<Integer>testConsumer());

            to.assertFailureAndMessage(TestException.class, "outer");

            moRef.get().onError(new TestException("inner"));

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final SingleSubject<Integer> ms = SingleSubject.create();

            final TestObserver<Integer> to = ps.switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
                @Override
                public SingleSource<Integer> apply(Integer v)
                        throws Exception {
                            return ms;
                        }
            }).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            to.assertNoErrors()
            .assertNotComplete();
        }
    }

    @Test
    public void nextInnerErrorRace() {
        final TestException ex = new TestException();

        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps = PublishSubject.create();

                final SingleSubject<Integer> ms = SingleSubject.create();

                final TestObserverEx<Integer> to = ps.switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
                    @Override
                    public SingleSource<Integer> apply(Integer v)
                            throws Exception {
                        if (v == 1) {
                            return ms;
                        }
                        return Single.never();
                    }
                }).to(TestHelper.<Integer>testConsumer());

                ps.onNext(1);

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps.onNext(2);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ms.onError(ex);
                    }
                };

                TestHelper.race(r1, r2);

                if (to.errors().size() != 0) {
                    assertTrue(errors.isEmpty());
                    to.assertFailure(TestException.class);
                } else if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void mainErrorInnerErrorRace() {
        final TestException ex = new TestException();
        final TestException ex2 = new TestException();

        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps = PublishSubject.create();

                final SingleSubject<Integer> ms = SingleSubject.create();

                final TestObserver<Integer> to = ps.switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
                    @Override
                    public SingleSource<Integer> apply(Integer v)
                            throws Exception {
                        if (v == 1) {
                            return ms;
                        }
                        return Single.never();
                    }
                }).test();

                ps.onNext(1);

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ms.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2);

                to.assertError(new Predicate<Throwable>() {
                    @Override
                    public boolean test(Throwable e) throws Exception {
                        return e instanceof TestException || e instanceof CompositeException;
                    }
                });

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void nextInnerSuccessRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final SingleSubject<Integer> ms = SingleSubject.create();

            final TestObserver<Integer> to = ps.switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
                @Override
                public SingleSource<Integer> apply(Integer v)
                        throws Exception {
                    if (v == 1) {
                            return ms;
                    }
                    return Single.never();
                }
            }).test();

            ps.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ms.onSuccess(3);
                }
            };

            TestHelper.race(r1, r2);

            to.assertNoErrors()
            .assertNotComplete();
        }
    }

    @Test
    public void checkDisposed() {
        PublishSubject<Integer> ps = PublishSubject.create();
        SingleSubject<Integer> ms = SingleSubject.create();

        TestHelper.checkDisposed(ps.switchMapSingle(Functions.justFunction(ms)));
    }

    @Test
    public void drainReentrant() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(2);
                }
            }
        };

        ps.switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                        return Single.just(v);
                    }
        }).subscribe(to);

        ps.onNext(1);
        ps.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void scalarMapperCrash() {
        TestObserver<Integer> to = Observable.just(1)
        .switchMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                        throw new TestException();
                    }
        })
        .test();

        to.assertFailure(TestException.class);
    }

    @Test
    public void scalarEmptySource() {
        SingleSubject<Integer> ss = SingleSubject.create();

        Observable.empty()
        .switchMapSingle(Functions.justFunction(ss))
        .test()
        .assertResult();

        assertFalse(ss.hasObservers());
    }

    @Test
    public void scalarSource() {
        SingleSubject<Integer> ss = SingleSubject.create();

        TestObserver<Integer> to = Observable.just(1)
        .switchMapSingle(Functions.justFunction(ss))
        .test();

        assertTrue(ss.hasObservers());

        to.assertEmpty();

        ss.onSuccess(2);

        to.assertResult(2);
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.switchMapSingle(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer v) throws Throwable {
                        return Single.just(v).hide();
                    }
                });
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.switchMapSingleDelayError(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer v) throws Throwable {
                        return Single.just(v).hide();
                    }
                });
            }
        });
    }
}
