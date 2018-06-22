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

package io.reactivex.internal.operators.mixed;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.operators.mixed.ObservableConcatMapMaybe.ConcatMapMaybeMainObserver;
import io.reactivex.internal.util.ErrorMode;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.*;

public class ObservableConcatMapMaybeTest {

    @Test
    public void simple() {
        Observable.range(1, 5)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void simpleLong() {
        Observable.range(1, 1024)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        }, 32)
        .test()
        .assertValueCount(1024)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void empty() {
        Observable.range(1, 10)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.empty();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void mixed() {
        Observable.range(1, 10)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                if (v % 2 == 0) {
                    return Maybe.just(v);
                }
                return Maybe.empty();
            }
        })
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void mixedLong() {
        Observable.range(1, 1024)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                if (v % 2 == 0) {
                    return Maybe.just(v).subscribeOn(Schedulers.computation());
                }
                return Maybe.<Integer>empty().subscribeOn(Schedulers.computation());
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(512)
        .assertNoErrors()
        .assertComplete()
        .assertOf(new Consumer<TestObserver<Integer>>() {
            @Override
            public void accept(TestObserver<Integer> to) throws Exception {
                for (int i = 0; i < 512; i ++) {
                    to.assertValueAt(i, (i + 1) * 2);
                }
            }
        });
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .concatMapMaybe(Functions.justFunction(Maybe.just(1)))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Observable.just(1)
        .concatMapMaybe(Functions.justFunction(Maybe.error(new TestException())))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainBoundaryErrorInnerSuccess() {
        PublishSubject<Integer> ps = PublishSubject.create();
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ps.concatMapMaybeDelayError(Functions.justFunction(ms), false).test();

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
    public void mainBoundaryErrorInnerEmpty() {
        PublishSubject<Integer> ps = PublishSubject.create();
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ps.concatMapMaybeDelayError(Functions.justFunction(ms), false).test();

        to.assertEmpty();

        ps.onNext(1);

        assertTrue(ms.hasObservers());

        ps.onError(new TestException());

        assertTrue(ms.hasObservers());

        to.assertEmpty();

        ms.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(
                new Function<Observable<Object>, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Observable<Object> f)
                            throws Exception {
                        return f.concatMapMaybeDelayError(
                                Functions.justFunction(Maybe.empty()));
                    }
                }
        );
    }

    @Test
    public void take() {
        Observable.range(1, 5)
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        })
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void cancel() {
        Observable.range(1, 5).concatWith(Observable.<Integer>never())
        .concatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v)
                    throws Exception {
                return Maybe.just(v);
            }
        })
        .test()
        .assertValues(1, 2, 3, 4, 5)
        .assertNoErrors()
        .assertNotComplete()
        .cancel();
    }

    @Test
    public void mainErrorAfterInnerError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> s) {
                    s.onSubscribe(Disposables.empty());
                    s.onNext(1);
                    s.onError(new TestException("outer"));
                }
            }
            .concatMapMaybe(
                    Functions.justFunction(Maybe.error(new TestException("inner"))), 1
            )
            .test()
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

            final AtomicReference<MaybeObserver<? super Integer>> obs = new AtomicReference<MaybeObserver<? super Integer>>();

            TestObserver<Integer> to = ps.concatMapMaybe(
                    new Function<Integer, MaybeSource<Integer>>() {
                        @Override
                        public MaybeSource<Integer> apply(Integer v)
                                throws Exception {
                            return new Maybe<Integer>() {
                                    @Override
                                    protected void subscribeActual(
                                            MaybeObserver<? super Integer> observer) {
                                        observer.onSubscribe(Disposables.empty());
                                        obs.set(observer);
                                    }
                            };
                        }
                    }
            ).test();

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
        Observable.range(1, 5)
        .concatMapMaybeDelayError(new Function<Integer, MaybeSource<? extends Object>>() {
            @Override
            public MaybeSource<? extends Object> apply(Integer v)
                    throws Exception {
                return Maybe.error(new TestException());
            }
        })
        .test()
        .assertFailure(CompositeException.class)
        .assertOf(new Consumer<TestObserver<Object>>() {
            @Override
            public void accept(TestObserver<Object> to) throws Exception {
                CompositeException ce = (CompositeException)to.errors().get(0);
                assertEquals(5, ce.getExceptions().size());
            }
        });
    }

    @Test
    public void mapperCrash() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Object> to = ps
        .concatMapMaybe(new Function<Integer, MaybeSource<? extends Object>>() {
            @Override
            public MaybeSource<? extends Object> apply(Integer v)
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
    public void scalarMapperCrash() {
        TestObserver<Object> to = Observable.just(1)
        .concatMapMaybe(new Function<Integer, MaybeSource<? extends Object>>() {
            @Override
            public MaybeSource<? extends Object> apply(Integer v)
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
                .concatMapMaybe(Functions.justFunction(Maybe.never()))
        );
    }

    @Test
    public void scalarEmptySource() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        Observable.empty()
        .concatMapMaybe(Functions.justFunction(ms))
        .test()
        .assertResult();

        assertFalse(ms.hasObservers());
    }

    @Test(timeout = 10000)
    public void cancelNoConcurrentClean() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        ConcatMapMaybeMainObserver<Integer, Integer> operator =
                new ConcatMapMaybeMainObserver<Integer, Integer>(
                        to, Functions.justFunction(Maybe.<Integer>never()), 16, ErrorMode.IMMEDIATE);

        operator.onSubscribe(Disposables.empty());

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
        MaybeSubject<Integer> ms = MaybeSubject.create();

        @SuppressWarnings("unchecked")
        TestObserver<Integer> to = Observable
                .fromArray(ms, Maybe.just(2), Maybe.just(3), Maybe.just(4))
                .concatMapMaybe(Functions.<Maybe<Integer>>identity(), 2)
                .test();

        to.assertEmpty();

        ms.onSuccess(1);

        to.assertResult(1, 2, 3, 4);
    }

    @Test
    public void innerSuccessDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final MaybeSubject<Integer> ms = MaybeSubject.create();

            final TestObserver<Integer> to = Observable.just(1)
                    .hide()
                    .concatMapMaybe(Functions.justFunction(ms))
                    .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ms.onSuccess(1);
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
}
