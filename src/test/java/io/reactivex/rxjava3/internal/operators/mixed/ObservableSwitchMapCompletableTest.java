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

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableSwitchMapCompletableTest extends RxJavaTest {

    @Test
    public void normal() {
        Observable.range(1, 10)
        .switchMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void mainError() {
        Observable.<Integer>error(new TestException())
        .switchMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        PublishSubject<Integer> ps = PublishSubject.create();
        CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = ps.switchMapCompletable(Functions.justFunction(cs))
        .test();

        assertTrue(ps.hasObservers());
        assertFalse(cs.hasObservers());

        ps.onNext(1);

        assertTrue(cs.hasObservers());

        to.assertEmpty();

        cs.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse(ps.hasObservers());
        assertFalse(cs.hasObservers());
    }

    @Test
    public void switchOver() {
        final CompletableSubject[] css = {
                CompletableSubject.create(),
                CompletableSubject.create()
        };

        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Void> to = ps.switchMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return css[v];
            }
        })
        .test();

        to.assertEmpty();

        ps.onNext(0);

        assertTrue(css[0].hasObservers());

        ps.onNext(1);

        assertFalse(css[0].hasObservers());
        assertTrue(css[1].hasObservers());

        ps.onComplete();

        to.assertEmpty();

        assertTrue(css[1].hasObservers());

        css[1].onComplete();

        to.assertResult();
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> ps = PublishSubject.create();
        CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = ps.switchMapCompletable(Functions.justFunction(cs))
        .test();

        ps.onNext(1);

        assertTrue(ps.hasObservers());
        assertTrue(cs.hasObservers());

        to.dispose();

        assertFalse(ps.hasObservers());
        assertFalse(cs.hasObservers());
    }

    @Test
    public void checkDisposed() {
        PublishSubject<Integer> ps = PublishSubject.create();
        CompletableSubject cs = CompletableSubject.create();

        TestHelper.checkDisposed(ps.switchMapCompletable(Functions.justFunction(cs)));
    }

    @Test
    public void checkBadSource() {
        TestHelper.checkDoubleOnSubscribeObservableToCompletable(new Function<Observable<Object>, Completable>() {
            @Override
            public Completable apply(Observable<Object> f) throws Exception {
                return f.switchMapCompletable(Functions.justFunction(Completable.never()));
            }
        });
    }

    @Test
    public void mapperCrash() {
        Observable.range(1, 5).switchMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer f) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperCancels() {
        final TestObserver<Void> to = new TestObserver<>();

        Observable.range(1, 5).switchMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer f) throws Exception {
                to.dispose();
                return Completable.complete();
            }
        })
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void onNextInnerCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();
            final CompletableSubject cs = CompletableSubject.create();

            TestObserver<Void> to = ps.switchMapCompletable(Functions.justFunction(cs)).test();

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
                    cs.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }

    @Test
    public void onNextInnerErrorRace() {
        final TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps = PublishSubject.create();
                final CompletableSubject cs = CompletableSubject.create();

                TestObserver<Void> to = ps.switchMapCompletable(Functions.justFunction(cs)).test();

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
                        cs.onError(ex);
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
    public void onErrorInnerErrorRace() {
        final TestException ex0 = new TestException();
        final TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps = PublishSubject.create();
                final CompletableSubject cs = CompletableSubject.create();

                TestObserver<Void> to = ps.switchMapCompletable(Functions.justFunction(cs)).test();

                ps.onNext(1);

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps.onError(ex0);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        cs.onError(ex);
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
    public void innerErrorThenMainError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onError(new TestException("main"));
                }
            }
            .switchMapCompletable(Functions.justFunction(Completable.error(new TestException("inner"))))
            .to(TestHelper.testConsumer())
            .assertFailureAndMessage(TestException.class, "inner");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "main");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void innerErrorDelayed() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = ps.switchMapCompletableDelayError(Functions.justFunction(cs)).test();

        ps.onNext(1);

        cs.onError(new TestException());

        to.assertEmpty();

        assertTrue(ps.hasObservers());

        ps.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainCompletesinnerErrorDelayed() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = ps.switchMapCompletableDelayError(Functions.justFunction(cs)).test();

        ps.onNext(1);
        ps.onComplete();

        to.assertEmpty();

        cs.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorDelayed() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = ps.switchMapCompletableDelayError(Functions.justFunction(cs)).test();

        ps.onNext(1);

        ps.onError(new TestException());

        to.assertEmpty();

        assertTrue(cs.hasObservers());

        cs.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void scalarMapperCrash() {
        TestObserver<Void> to = Observable.just(1)
        .switchMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v)
                    throws Exception {
                        throw new TestException();
                    }
        })
        .test();

        to.assertFailure(TestException.class);
    }

    @Test
    public void scalarEmptySource() {
        CompletableSubject cs = CompletableSubject.create();

        Observable.empty()
        .switchMapCompletable(Functions.justFunction(cs))
        .test()
        .assertResult();

        assertFalse(cs.hasObservers());
    }

    @Test
    public void scalarSource() {
        CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = Observable.just(1)
        .switchMapCompletable(Functions.justFunction(cs))
        .test();

        assertTrue(cs.hasObservers());

        to.assertEmpty();

        cs.onComplete();

        to.assertResult();
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Completable>() {
            @Override
            public Completable apply(Observable<Integer> upstream) {
                return upstream.switchMapCompletable(new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer v) throws Throwable {
                        return Completable.complete().hide();
                    }
                });
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Completable>() {
            @Override
            public Completable apply(Observable<Integer> upstream) {
                return upstream.switchMapCompletableDelayError(new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer v) throws Throwable {
                        return Completable.complete().hide();
                    }
                });
            }
        });
    }
}
