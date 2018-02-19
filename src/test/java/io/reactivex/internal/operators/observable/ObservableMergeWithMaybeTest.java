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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.*;

public class ObservableMergeWithMaybeTest {

    @Test
    public void normal() {
        Observable.range(1, 5)
        .mergeWith(Maybe.just(100))
        .test()
        .assertResult(1, 2, 3, 4, 5, 100);
    }

    @Test
    public void emptyOther() {
        Observable.range(1, 5)
        .mergeWith(Maybe.<Integer>empty())
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalLong() {
        Observable.range(1, 512)
        .mergeWith(Maybe.just(100))
        .test()
        .assertValueCount(513)
        .assertComplete();
    }

    @Test
    public void take() {
        Observable.range(1, 5)
        .mergeWith(Maybe.just(100))
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void cancel() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final MaybeSubject<Integer> cs = MaybeSubject.create();

        TestObserver<Integer> to = ps.mergeWith(cs).test();

        assertTrue(ps.hasObservers());
        assertTrue(cs.hasObservers());

        to.cancel();

        assertFalse(ps.hasObservers());
        assertFalse(cs.hasObservers());
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .mergeWith(Maybe.just(100))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        Observable.never()
        .mergeWith(Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void completeRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();
            final MaybeSubject<Integer> cs = MaybeSubject.create();

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
        final MaybeSubject<Integer> cs = MaybeSubject.create();

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
        final MaybeSubject<Integer> cs = MaybeSubject.create();

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
            final AtomicReference<Observer<?>> subscriber = new AtomicReference<Observer<?>>();
            TestObserver<Integer> to = new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> s) {
                    s.onSubscribe(Disposables.empty());
                    subscriber.set(s);
                }
            }
            .mergeWith(Maybe.<Integer>error(new IOException()))
            .test();

            subscriber.get().onError(new TestException());

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
            .mergeWith(Maybe.error(new TestException()))
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
                        return f.mergeWith(Maybe.just(1));
                    }
                }
        );
    }

    @Test
    public void isDisposed() {
        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());

                assertFalse(((Disposable)observer).isDisposed());

                observer.onNext(1);

                assertTrue(((Disposable)observer).isDisposed());
            }
        }.mergeWith(Maybe.<Integer>empty())
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void onNextSlowPathCreateQueue() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final MaybeSubject<Integer> cs = MaybeSubject.create();

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

}
