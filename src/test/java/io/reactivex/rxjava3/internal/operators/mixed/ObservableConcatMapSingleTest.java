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
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.mixed.ObservableConcatMapSingle.ConcatMapSingleMainObserver;
import io.reactivex.rxjava3.internal.util.ErrorMode;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableConcatMapSingleTest extends RxJavaTest {

    @Test
    public void simple() {
        Observable.range(1, 5)
        .concatMapSingle(new Function<Integer, SingleSource<Integer>>() {
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
    public void simpleLong() {
        Observable.range(1, 1024)
        .concatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
            }
        }, 32)
        .test()
        .assertValueCount(1024)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .concatMapSingle(Functions.justFunction(Single.just(1)))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Observable.just(1)
        .concatMapSingle(Functions.justFunction(Single.error(new TestException())))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainBoundaryErrorInnerSuccess() {
        PublishSubject<Integer> ps = PublishSubject.create();
        SingleSubject<Integer> ms = SingleSubject.create();

        TestObserver<Integer> to = ps.concatMapSingleDelayError(Functions.justFunction(ms), false).test();

        to.assertEmpty();

        ps.onNext(1);

        assertTrue(ms.hasObservers());

        ps.onError(new TestException());

        assertTrue(ms.hasObservers());

        to.assertEmpty();

        ms.onSuccess(1);

        to.assertFailure(TestException.class, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(
                new Function<Observable<Object>, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Observable<Object> f)
                            throws Exception {
                        return f.concatMapSingleDelayError(
                                Functions.justFunction(Single.never()));
                    }
                }
        );
    }

    @Test
    public void take() {
        Observable.range(1, 5)
        .concatMapSingle(new Function<Integer, SingleSource<Integer>>() {
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
    public void cancel() {
        Observable.range(1, 5).concatWith(Observable.<Integer>never())
        .concatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
            }
        })
        .test()
        .assertValues(1, 2, 3, 4, 5)
        .assertNoErrors()
        .assertNotComplete()
        .dispose();
    }

    @Test
    public void mainErrorAfterInnerError() {
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
            .concatMapSingle(
                    Functions.justFunction(Single.error(new TestException("inner"))), 1
            )
            .to(TestHelper.<Object>testConsumer())
            .assertFailureAndMessage(TestException.class, "inner");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "outer");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void innerErrorAfterMainError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final AtomicReference<SingleObserver<? super Integer>> obs = new AtomicReference<>();

            TestObserverEx<Integer> to = ps.concatMapSingle(
                    new Function<Integer, SingleSource<Integer>>() {
                        @Override
                        public SingleSource<Integer> apply(Integer v)
                                throws Exception {
                            return new Single<Integer>() {
                                    @Override
                                    protected void subscribeActual(
                                            SingleObserver<? super Integer> observer) {
                                        observer.onSubscribe(Disposable.empty());
                                        obs.set(observer);
                                    }
                            };
                        }
                    }
            ).to(TestHelper.<Integer>testConsumer());

            ps.onNext(1);

            ps.onError(new TestException("outer"));
            obs.get().onError(new TestException("inner"));

            to.assertFailureAndMessage(TestException.class, "outer");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void delayAllErrors() {
        TestObserverEx<Object> to = Observable.range(1, 5)
        .concatMapSingleDelayError(new Function<Integer, SingleSource<? extends Object>>() {
            @Override
            public SingleSource<? extends Object> apply(Integer v)
                    throws Exception {
                return Single.error(new TestException());
            }
        })
        .to(TestHelper.<Object>testConsumer())
        .assertFailure(CompositeException.class)
        ;

        CompositeException ce = (CompositeException)to.errors().get(0);
        assertEquals(5, ce.getExceptions().size());
    }

    @Test
    public void mapperCrash() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Object> to = ps
        .concatMapSingle(new Function<Integer, SingleSource<? extends Object>>() {
            @Override
            public SingleSource<? extends Object> apply(Integer v)
                    throws Exception {
                        throw new TestException();
                    }
        })
        .test();

        to.assertEmpty();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void mapperCrashScalar() {
        TestObserver<Object> to = Observable.just(1)
        .concatMapSingle(new Function<Integer, SingleSource<? extends Object>>() {
            @Override
            public SingleSource<? extends Object> apply(Integer v)
                    throws Exception {
                        throw new TestException();
                    }
        })
        .test();

        to.assertFailure(TestException.class);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.just(1).hide()
                .concatMapSingle(Functions.justFunction(Single.never()))
        );
    }

    @Test
    public void mainCompletesWhileInnerActive() {
        PublishSubject<Integer> ps = PublishSubject.create();
        SingleSubject<Integer> ms = SingleSubject.create();

        TestObserver<Integer> to = ps.concatMapSingleDelayError(Functions.justFunction(ms), false).test();

        to.assertEmpty();

        ps.onNext(1);
        ps.onNext(2);
        ps.onComplete();

        assertTrue(ms.hasObservers());

        to.assertEmpty();

        ms.onSuccess(1);

        to.assertResult(1, 1);
    }

    @Test
    public void scalarEmptySource() {
        SingleSubject<Integer> ss = SingleSubject.create();

        Observable.empty()
        .concatMapSingle(Functions.justFunction(ss))
        .test()
        .assertResult();

        assertFalse(ss.hasObservers());
    }

    @Test
    public void cancelNoConcurrentClean() {
        TestObserver<Integer> to = new TestObserver<>();
        ConcatMapSingleMainObserver<Integer, Integer> operator =
                new ConcatMapSingleMainObserver<>(
                        to, Functions.justFunction(Single.<Integer>never()), 16, ErrorMode.IMMEDIATE);

        operator.onSubscribe(Disposable.empty());

        operator.queue.offer(1);

        operator.getAndIncrement();

        to.dispose();

        assertFalse(operator.queue.isEmpty());

        operator.addAndGet(-2);

        operator.dispose();

        assertTrue(operator.queue.isEmpty());
    }

    @Test
    public void checkUnboundedInnerQueue() {
        SingleSubject<Integer> ss = SingleSubject.create();

        TestObserver<Integer> to = Observable
                .fromArray(ss, Single.just(2), Single.just(3), Single.just(4))
                .concatMapSingle(Functions.<Single<Integer>>identity(), 2)
                .test();

        to.assertEmpty();

        ss.onSuccess(1);

        to.assertResult(1, 2, 3, 4);
    }

    @Test
    public void innerSuccessDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final SingleSubject<Integer> ss = SingleSubject.create();

            final TestObserver<Integer> to = Observable.just(1)
                    .hide()
                    .concatMapSingle(Functions.justFunction(ss))
                    .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ss.onSuccess(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            to.assertNoErrors();
        }
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.concatMapSingle(new Function<Integer, Single<Integer>>() {
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
                return upstream.concatMapSingleDelayError(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer v) throws Throwable {
                        return Single.just(v).hide();
                    }
                }, false, 2);
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayErrorTillEnd() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.concatMapSingleDelayError(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer v) throws Throwable {
                        return Single.just(v).hide();
                    }
                }, true, 2);
            }
        });
    }
}
