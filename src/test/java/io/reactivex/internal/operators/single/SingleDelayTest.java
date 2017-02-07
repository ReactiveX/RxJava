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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class SingleDelayTest {
    @Test
    public void delay() throws Exception {
        final AtomicInteger value = new AtomicInteger();

        Single.just(1).delay(200, TimeUnit.MILLISECONDS)
        .subscribe(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                value.set(v);
            }
        });

        Thread.sleep(100);

        assertEquals(0, value.get());

        Thread.sleep(200);

        assertEquals(1, value.get());
    }

    @Test
    public void delayError() {
        Single.error(new TestException()).delay(5, TimeUnit.SECONDS)
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
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
    public void testOnErrorCalledOnScheduler() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> thread = new AtomicReference<Thread>();

        Single.<String>error(new Exception())
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        thread.set(Thread.currentThread());
                        latch.countDown();
                    }
                })
                .onErrorResumeNext(Single.just(""))
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
                protected void subscribeActual(Observer<? super Integer> s) {
                    s.onSubscribe(Disposables.empty());
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
}
