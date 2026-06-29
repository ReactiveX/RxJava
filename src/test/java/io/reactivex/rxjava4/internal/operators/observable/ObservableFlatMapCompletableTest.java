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

package io.reactivex.rxjava4.internal.operators.observable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StandardConcurrentBufferedConfig;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.operators.*;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.*;

public class ObservableFlatMapCompletableTest extends RxJavaTest {

    @Test
    public void normalObservable() {
        Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete()).toObservable()
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrowsObservable() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
        .flatMapCompletable(_ -> {
            throw new TestException();
        }).<Integer>toObservable()
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void mapperReturnsNullObservable() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
        .flatMapCompletable(_ -> null).<Integer>toObservable()
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(NullPointerException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void normalDelayErrorObservable() {
        Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete(), new StandardConcurrentBufferedConfig(true)).toObservable()
        .test()
        .assertResult();
    }

    @Test
    public void normalAsyncObservable() {
        Observable.range(1, 1000)
        .flatMapCompletable(_ -> Observable.range(1, 100).subscribeOn(Schedulers.computation()).ignoreElements()).toObservable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalDelayErrorAllObservable() {
        TestObserverEx<Integer> to = Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
        .flatMapCompletable(_ -> Completable.error(new TestException()), new StandardConcurrentBufferedConfig(true))
        .<Integer>toObservable()
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().getFirst());

        for (int i = 0; i < 11; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalDelayInnerErrorAllObservable() {
        TestObserverEx<Integer> to = Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.error(new TestException()), new StandardConcurrentBufferedConfig(true))
        .<Integer>toObservable()
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().getFirst());

        for (int i = 0; i < 10; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalNonDelayErrorOuterObservable() {
        Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
        .flatMapCompletable(_ -> Completable.complete(), new StandardConcurrentBufferedConfig(false))
        .toObservable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedObservable() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete()).<Integer>toObservable()
        .subscribe(to);

        to
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void disposedObservable() {
        TestHelper.checkDisposed(Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete()).toObservable());
    }

    @Test
    public void normal() {
        Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete())
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrows() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Void> to = ps
        .flatMapCompletable(_ -> {
            throw new TestException();
        })
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void mapperReturnsNull() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Void> to = ps
        .flatMapCompletable(_ -> null)
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(NullPointerException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void normalDelayError() {
        Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete(), new StandardConcurrentBufferedConfig(true))
        .test()
        .assertResult();
    }

    @Test
    public void normalAsync() {
        Observable.range(1, 1000)
        .flatMapCompletable(_ -> Observable.range(1, 100).subscribeOn(Schedulers.computation()).ignoreElements())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalDelayErrorAll() {
        TestObserverEx<Void> to = Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
        .flatMapCompletable(_ -> Completable.error(new TestException()), new StandardConcurrentBufferedConfig(true))
        .to(TestHelper.<Void>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().getFirst());

        for (int i = 0; i < 11; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalDelayInnerErrorAll() {
        TestObserverEx<Void> to = Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.error(new TestException()), new StandardConcurrentBufferedConfig(true))
        .to(TestHelper.<Void>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().getFirst());

        for (int i = 0; i < 10; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalNonDelayErrorOuter() {
        Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
        .flatMapCompletable(_ -> Completable.complete(), new StandardConcurrentBufferedConfig(false))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fused() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete())
        .<Integer>toObservable()
        .subscribe(to);

        to
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete()));
    }

    @Test
    public void innerObserver() {
        Observable.range(1, 3)
        .flatMapCompletable(_ -> new Completable() /* NFI */ {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onSubscribe(Disposable.empty());

                assertFalse(((Disposable)observer).isDisposed());

                ((Disposable)observer).dispose();

                assertTrue(((Disposable)observer).isDisposed());
            }
        })
        .test();
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(o -> o.flatMapCompletable(_ -> Completable.complete()), false, 1, null);
    }

    @Test
    public void fusedInternalsObservable() {
        Observable.range(1, 10)
        .flatMapCompletable(_ -> Completable.complete())
        .toObservable()
        .subscribe(new Observer<>() /* NFI */ {
            @Override
            public void onSubscribe(Disposable d) {
                QueueDisposable<?> qd = (QueueDisposable<?>) d;
                try {
                    assertNull(qd.poll());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
                assertTrue(qd.isEmpty());
                qd.clear();
            }

            @Override
            public void onNext(Object t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void innerObserverObservable() {
        Observable.range(1, 3)
        .flatMapCompletable(_ -> new Completable() /* NFI */ {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onSubscribe(Disposable.empty());

                assertFalse(((Disposable)observer).isDisposed());

                ((Disposable)observer).dispose();

                assertTrue(((Disposable)observer).isDisposed());
            }
        })
        .toObservable()
        .test();
    }

    @Test
    public void badSourceObservable() {
        TestHelper.checkBadSourceObservable(o -> o.flatMapCompletable(_ -> Completable.complete()).toObservable(), false, 1, null);
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Completable>) upstream ->
            upstream.flatMapCompletable((Function<Integer, Completable>) _ -> Completable.complete().hide()));
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Completable>) upstream ->
            upstream.flatMapCompletable((Function<Integer, Completable>) _ -> Completable.complete().hide(), new StandardConcurrentBufferedConfig(true)));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(o -> o.flatMapCompletable(_ -> Completable.never()).toObservable());
    }

    @Test
    public void doubleOnSubscribeCompletable() {
        TestHelper.checkDoubleOnSubscribeObservableToCompletable(o -> o.flatMapCompletable(_ -> Completable.never()));
    }

    @Test
    public void cancelWhileMapping() throws Throwable {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishSubject<Integer> ps1 = PublishSubject.create();

            TestObserver<Object> to = new TestObserver<>();
            CountDownLatch cdl = new CountDownLatch(1);

            ps1.flatMapCompletable(_ -> {
                TestHelper.raceOther(to::dispose, cdl);
                return Completable.complete();
            })
            .toObservable()
            .subscribe(to);

            ps1.onNext(1);

            cdl.await();
        }
    }

    @Test
    public void cancelWhileMappingCompletable() throws Throwable {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishSubject<Integer> ps1 = PublishSubject.create();

            TestObserver<Void> to = new TestObserver<>();
            CountDownLatch cdl = new CountDownLatch(1);

            ps1.flatMapCompletable(_ -> {
                TestHelper.raceOther(to::dispose, cdl);
                return Completable.complete();
            })
            .subscribe(to);

            ps1.onNext(1);

            cdl.await();
        }
    }
}
