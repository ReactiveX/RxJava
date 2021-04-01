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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableMergeWithSingleTest extends RxJavaTest {

    @Test
    public void normal() {
        Observable.range(1, 5)
        .mergeWith(Single.just(100))
        .test()
        .assertResult(1, 2, 3, 4, 5, 100);
    }

    @Test
    public void normalLong() {
        Observable.range(1, 512)
        .mergeWith(Single.just(100))
        .test()
        .assertValueCount(513)
        .assertComplete();
    }

    @Test
    public void take() {
        Observable.range(1, 5)
        .mergeWith(Single.just(100))
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void cancel() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final SingleSubject<Integer> cs = SingleSubject.create();

        TestObserver<Integer> to = ps.mergeWith(cs).test();

        assertTrue(ps.hasObservers());
        assertTrue(cs.hasObservers());

        to.dispose();

        assertFalse(ps.hasObservers());
        assertFalse(cs.hasObservers());
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .mergeWith(Single.just(100))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        Observable.never()
        .mergeWith(Single.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void completeRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();
            final SingleSubject<Integer> cs = SingleSubject.create();

            TestObserver<Integer> to = ps.mergeWith(cs).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                    ps.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cs.onSuccess(1);
                }
            };

            TestHelper.race(r1, r2);

            to.assertResult(1, 1);
        }
    }

    @Test
    public void onNextSlowPath() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final SingleSubject<Integer> cs = SingleSubject.create();

        TestObserver<Integer> to = ps.mergeWith(cs).subscribeWith(new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(2);
                }
            }
        });

        ps.onNext(1);
        cs.onSuccess(3);

        ps.onNext(4);
        ps.onComplete();

        to.assertResult(1, 2, 3, 4);
    }

    @Test
    public void onSuccessSlowPath() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final SingleSubject<Integer> cs = SingleSubject.create();

        TestObserver<Integer> to = ps.mergeWith(cs).subscribeWith(new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cs.onSuccess(2);
                }
            }
        });

        ps.onNext(1);

        ps.onNext(3);
        ps.onComplete();

        to.assertResult(1, 2, 3);
    }

    @Test
    public void onErrorMainOverflow() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final AtomicReference<Observer<?>> observerRef = new AtomicReference<>();
            TestObserver<Integer> to = new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observerRef.set(observer);
                }
            }
            .mergeWith(Single.<Integer>error(new IOException()))
            .test();

            observerRef.get().onError(new TestException());

            to.assertFailure(IOException.class)
            ;

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorOtherOverflow() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.error(new IOException())
            .mergeWith(Single.error(new TestException()))
            .test()
            .assertFailure(IOException.class)
            ;

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doubleOnSubscribeMain() {
        TestHelper.checkDoubleOnSubscribeObservable(
                new Function<Observable<Object>, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Observable<Object> f)
                            throws Exception {
                        return f.mergeWith(Single.just(1));
                    }
                }
        );
    }

    @Test
    public void isDisposed() {
        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());

                assertFalse(((Disposable)observer).isDisposed());

                observer.onNext(1);

                assertTrue(((Disposable)observer).isDisposed());
            }
        }.mergeWith(Single.<Integer>just(1))
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void onNextSlowPathCreateQueue() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final SingleSubject<Integer> cs = SingleSubject.create();

        TestObserver<Integer> to = ps.mergeWith(cs).subscribeWith(new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(2);
                    ps.onNext(3);
                }
            }
        });

        cs.onSuccess(0);
        ps.onNext(1);

        ps.onNext(4);
        ps.onComplete();

        to.assertResult(0, 1, 2, 3, 4);
    }

    @Test
    public void cancelOtherOnMainError() {
        PublishSubject<Integer> ps = PublishSubject.create();
        SingleSubject<Integer> ss = SingleSubject.create();

        TestObserver<Integer> to = ps.mergeWith(ss).test();

        assertTrue(ps.hasObservers());
        assertTrue(ss.hasObservers());

        ps.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse("main has observers!", ps.hasObservers());
        assertFalse("other has observers", ss.hasObservers());
    }

    @Test
    public void cancelMainOnOtherError() {
        PublishSubject<Integer> ps = PublishSubject.create();
        SingleSubject<Integer> ss = SingleSubject.create();

        TestObserver<Integer> to = ps.mergeWith(ss).test();

        assertTrue(ps.hasObservers());
        assertTrue(ss.hasObservers());

        ss.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse("main has observers!", ps.hasObservers());
        assertFalse("other has observers", ss.hasObservers());
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.mergeWith(Single.just(1).hide());
            }
        });
    }
}
