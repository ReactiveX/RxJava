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

package io.reactivex.rxjava3.internal.operators.mixed;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.NonNull;
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

public class ObservableSwitchMapMaybeTest extends RxJavaTest {

    @Test
    public void simple() {
        Observable.range(1, 5)
        .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) Maybe::just)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void simpleEmpty() {
        Observable.range(1, 5)
        .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) v -> Maybe.empty())
        .test()
        .assertResult();
    }

    @Test
    public void simpleMixed() {
        Observable.range(1, 10)
        .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) v -> {
            if (v % 2 == 0) {
                return Maybe.just(v);
            }
            return Maybe.empty();
        })
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .switchMapMaybe(Functions.justFunction(Maybe.never()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Observable.just(1)
        .switchMapMaybe(Functions.justFunction(Maybe.error(new TestException())))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable((Function<Observable<Object>, Observable<Object>>) f -> f
                .switchMapMaybe(Functions.justFunction(Maybe.never()))
        );
    }

    @Test
    public void take() {
        Observable.range(1, 5)
        .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) Maybe::just)
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void switchOver() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final MaybeSubject<Integer> ms1 = MaybeSubject.create();
        final MaybeSubject<Integer> ms2 = MaybeSubject.create();

        TestObserver<Integer> to = ps.switchMapMaybe((Function<Integer, MaybeSource<Integer>>) v -> {
                    if (v == 1) {
                        return ms1;
                    }
                    return ms2;
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

        final MaybeSubject<Integer> ms1 = MaybeSubject.create();
        final MaybeSubject<Integer> ms2 = MaybeSubject.create();

        TestObserver<Integer> to = ps.switchMapMaybeDelayError((Function<Integer, MaybeSource<Integer>>) v -> {
                    if (v == 1) {
                        return ms1;
                    }
                    return ms2;
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

        final MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ps.switchMapMaybeDelayError((Function<Integer, MaybeSource<Integer>>) v -> ms).test();

        to.assertEmpty();

        ps.onNext(1);

        to.assertEmpty();

        assertTrue(ms.hasObservers());

        ps.onError(new TestException());

        assertTrue(ms.hasObservers());

        to.assertEmpty();

        ms.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorInnerSuccessDelayError() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ps.switchMapMaybeDelayError((Function<Integer, MaybeSource<Integer>>) v -> ms).test();

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
        .switchMapMaybe(v -> {
                    throw new TestException();
                })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void disposeBeforeSwitchInOnNext() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.just(1)
        .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) v -> {
                    to.dispose();
                    return Maybe.just(1);
                }).subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void disposeOnNextAfterFirst() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.just(1, 2)
        .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) v -> {
            if (v == 2) {
                to.dispose();
            }
            return Maybe.just(1);
        }).subscribe(to);

        to.assertValue(1)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void cancel() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ps.switchMapMaybeDelayError((Function<Integer, MaybeSource<Integer>>) v -> ms).test();

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
                protected void subscribeActual(@NonNull Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onError(new TestException("outer"));
                }
            }
            .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) v -> Maybe.error(new TestException("inner")))
            .to(TestHelper.testConsumer())
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
            final AtomicReference<MaybeObserver<? super Integer>> moRef = new AtomicReference<>();

            TestObserverEx<Integer> to = new Observable<Integer>() {
                @Override
                protected void subscribeActual(@NonNull Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onError(new TestException("outer"));
                }
            }
            .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) v -> new Maybe<Integer>() {
                @Override
                protected void subscribeActual(
                        @NonNull MaybeObserver<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    moRef.set(observer);
                }
            })
            .to(TestHelper.testConsumer());

            to.assertFailureAndMessage(TestException.class, "outer");

            moRef.get().onError(new TestException("inner"));
            moRef.get().onComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final MaybeSubject<Integer> ms = MaybeSubject.create();

            final TestObserver<Integer> to = ps.switchMapMaybeDelayError((Function<Integer, MaybeSource<Integer>>) v -> ms).test();

            Runnable r1 = () -> ps.onNext(1);

            Runnable r2 = to::dispose;

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

                final MaybeSubject<Integer> ms = MaybeSubject.create();

                final TestObserverEx<Integer> to = ps.switchMapMaybeDelayError((Function<Integer, MaybeSource<Integer>>) v -> {
                    if (v == 1) {
                        return ms;
                    }
                    return Maybe.never();
                }).to(TestHelper.testConsumer());

                ps.onNext(1);

                Runnable r1 = () -> ps.onNext(2);

                Runnable r2 = () -> ms.onError(ex);

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

                final MaybeSubject<Integer> ms = MaybeSubject.create();

                final TestObserver<Integer> to = ps.switchMapMaybeDelayError((Function<Integer, MaybeSource<Integer>>) v -> {
                    if (v == 1) {
                        return ms;
                    }
                    return Maybe.never();
                }).test();

                ps.onNext(1);

                Runnable r1 = () -> ps.onError(ex);

                Runnable r2 = () -> ms.onError(ex2);

                TestHelper.race(r1, r2);

                to.assertError(e -> e instanceof TestException || e instanceof CompositeException);

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

            final MaybeSubject<Integer> ms = MaybeSubject.create();

            final TestObserver<Integer> to = ps.switchMapMaybeDelayError((Function<Integer, MaybeSource<Integer>>) v -> {
                if (v == 1) {
                        return ms;
                }
                return Maybe.empty();
            }).test();

            ps.onNext(1);

            Runnable r1 = () -> ps.onNext(2);

            Runnable r2 = () -> ms.onSuccess(3);

            TestHelper.race(r1, r2);

            to.assertNoErrors()
            .assertNotComplete();
        }
    }

    @Test
    public void checkDisposed() {
        PublishSubject<Integer> ps = PublishSubject.create();
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestHelper.checkDisposed(ps.switchMapMaybe(Functions.justFunction(ms)));
    }

    @Test
    public void drainReentrant() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(@NonNull Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(2);
                }
            }
        };

        ps.switchMapMaybe((Function<Integer, MaybeSource<Integer>>) Maybe::just).subscribe(to);

        ps.onNext(1);
        ps.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void scalarMapperCrash() {
        TestObserver<Integer> to = Observable.just(1)
        .switchMapMaybe((Function<Integer, MaybeSource<Integer>>) v -> {
                    throw new TestException();
                })
        .test();

        to.assertFailure(TestException.class);
    }

    @Test
    public void scalarEmptySource() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        Observable.empty()
        .switchMapMaybe(Functions.justFunction(ms))
        .test()
        .assertResult();

        assertFalse(ms.hasObservers());
    }

    @Test
    public void scalarSource() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = Observable.just(1)
        .switchMapMaybe(Functions.justFunction(ms))
        .test();

        assertTrue(ms.hasObservers());

        to.assertEmpty();

        ms.onSuccess(2);

        to.assertResult(2);
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream -> upstream.switchMapMaybe((Function<Integer, Maybe<Integer>>) v -> Maybe.just(v).hide()));
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream -> upstream.switchMapMaybeDelayError((Function<Integer, Maybe<Integer>>) v -> Maybe.just(v).hide()));
    }
}
