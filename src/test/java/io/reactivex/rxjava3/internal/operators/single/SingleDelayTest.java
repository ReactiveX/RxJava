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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.assertNotEquals;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleDelayTest extends RxJavaTest {
    @Test
    public void delayOnSuccess() {
        final TestScheduler scheduler = new TestScheduler();
        final TestObserver<Integer> observer = Single.just(1)
            .delay(5, TimeUnit.SECONDS, scheduler)
            .test();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        observer.assertNoValues();

        scheduler.advanceTimeTo(5, TimeUnit.SECONDS);
        observer.assertValue(1);
    }

    @Test
    public void delayOnError() {
        final TestScheduler scheduler = new TestScheduler();
        final TestObserver<?> observer = Single.error(new TestException())
            .delay(5, TimeUnit.SECONDS, scheduler)
            .test();

        scheduler.triggerActions();
        observer.assertError(TestException.class);
    }

    @Test
    public void delayedErrorOnSuccess() {
        final TestScheduler scheduler = new TestScheduler();
        final TestObserver<Integer> observer = Single.just(1)
            .delay(5, TimeUnit.SECONDS, scheduler, true)
            .test();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        observer.assertNoValues();

        scheduler.advanceTimeTo(5, TimeUnit.SECONDS);
        observer.assertValue(1);
    }

    @Test
    public void delayedErrorOnError() {
        final TestScheduler scheduler = new TestScheduler();
        final TestObserver<?> observer = Single.error(new TestException())
            .delay(5, TimeUnit.SECONDS, scheduler, true)
            .test();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        observer.assertNoErrors();

        scheduler.advanceTimeTo(5, TimeUnit.SECONDS);
        observer.assertError(TestException.class);
    }

    @Test
    public void delaySubscriptionCompletable() throws Exception {
        Single.just(1).delaySubscription(Completable.complete().delay(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionObservable() throws Exception {
        Single.just(1).delaySubscription(Observable.timer(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionFlowable() throws Exception {
        Single.just(1).delaySubscription(Flowable.timer(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionSingle() throws Exception {
        Single.just(1).delaySubscription(Single.timer(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionTime() throws Exception {
        Single.just(1).delaySubscription(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionTimeCustomScheduler() throws Exception {
        Single.just(1).delaySubscription(100, TimeUnit.MILLISECONDS, Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void onErrorCalledOnScheduler() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> thread = new AtomicReference<>();

        Single.<String>error(new Exception())
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        thread.set(Thread.currentThread());
                        latch.countDown();
                    }
                })
                .onErrorResumeWith(Single.just(""))
                .subscribe();

        latch.await();

        assertNotEquals(Thread.currentThread(), thread.get());
    }

    @Test
    public void withPublisherDispose() {
        TestHelper.checkDisposed(PublishSubject.create().singleOrError().delaySubscription(Flowable.just(1)));
    }

    @Test
    public void withPublisherError() {
        Single.just(1)
        .delaySubscription(Flowable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void withPublisherError2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Single.just(1)
            .delaySubscription(new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onError(new TestException());
                }
            })
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void withObservableDispose() {
        TestHelper.checkDisposed(PublishSubject.create().singleOrError().delaySubscription(Observable.just(1)));
    }

    @Test
    public void withObservableError() {
        Single.just(1)
        .delaySubscription(Observable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void withObservableError2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Single.just(1)
            .delaySubscription(new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onError(new TestException());
                }
            })
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void withSingleErrors() {
        Single.just(1)
        .delaySubscription(Single.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void withSingleDispose() {
        TestHelper.checkDisposed(Single.just(1).delaySubscription(Single.just(2)));
    }

    @Test
    public void withCompletableDispose() {
        TestHelper.checkDisposed(Completable.complete().andThen(Single.just(1)));
    }

    @Test
    public void withCompletableDoubleOnSubscribe() {

        TestHelper.checkDoubleOnSubscribeCompletableToSingle(new Function<Completable, Single<Object>>() {
            @Override
            public Single<Object> apply(Completable c) throws Exception {
                return c.andThen(Single.just((Object)1));
            }
        });

    }

    @Test
    public void withSingleDoubleOnSubscribe() {

        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Object>, Single<Object>>() {
            @Override
            public Single<Object> apply(Single<Object> s) throws Exception {
                return Single.just((Object)1).delaySubscription(s);
            }
        });

    }
}
