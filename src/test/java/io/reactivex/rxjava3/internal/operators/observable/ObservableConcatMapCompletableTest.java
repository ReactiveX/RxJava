/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableConcatMapCompletableTest extends RxJavaTest {

    @Test
    public void asyncFused() throws Exception {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Void> to = us.concatMapCompletable(completableComplete(), 2).test();

        us.onNext(1);
        us.onComplete();

        to.assertComplete();
        to.assertValueCount(0);
    }

    @Test
    public void notFused() throws Exception {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestObserver<Void> to = us.hide().concatMapCompletable(completableComplete(), 2).test();

        us.onNext(1);
        us.onNext(2);
        us.onComplete();

        to.assertComplete();
        to.assertValueCount(0);
        to.assertNoErrors();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.<Integer>just(1).hide()
        .concatMapCompletable(completableError()));
    }

    @Test
    public void mainError() {
        Observable.<Integer>error(new TestException())
        .concatMapCompletable(completableComplete())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Observable.<Integer>just(1).hide()
        .concatMapCompletable(completableError())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .concatMapCompletable(completableComplete())
            .test()
            .assertComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestObserver<Void> to = ps1.concatMapCompletable(new Function<Integer, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Integer v) throws Exception {
                        return Completable.fromObservable(ps2);
                    }
                }).test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2);

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestHelper.assertError(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void mapperThrows() {
        Observable.just(1).hide()
        .concatMapCompletable(completableThrows())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedPollThrows() {
        Observable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .concatMapCompletable(completableComplete())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void concatReportsDisposedOnComplete() {
        final Disposable[] disposable = { null };

        Observable.just(1)
        .hide()
        .concatMapCompletable(completableComplete())
        .subscribe(new CompletableObserver() {

            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertTrue(disposable[0].isDisposed());
    }

    @Test
    public void concatReportsDisposedOnError() {
        final Disposable[] disposable = { null };

        Observable.just(1)
        .hide()
        .concatMapCompletable(completableError())
        .subscribe(new CompletableObserver() {

            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertTrue(disposable[0].isDisposed());
    }

    private Function<Integer, CompletableSource> completableComplete() {
        return new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        };
    }

    private Function<Integer, CompletableSource> completableError() {
        return new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        };
    }

    private Function<Integer, CompletableSource> completableThrows() {
        return new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                throw new TestException();
            }
        };
    }
}
