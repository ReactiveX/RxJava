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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ObservableDelaySubscriptionOtherTest {
    @Test
    public void testNoPrematureSubscription() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.just(1)
        .doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testNoMultipleSubscriptions() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.just(1)
        .doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);
        other.onNext(2);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testCompleteTriggersSubscription() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.just(1)
        .doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onComplete();

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testNoPrematureSubscriptionToError() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.<Integer>error(new TestException())
        .doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onComplete();

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void testNoSubscriptionIfOtherErrors() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.<Integer>error(new TestException())
        .doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onError(new TestException());

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void badSourceOther() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(Observable<Integer> o) throws Exception {
                return Observable.just(1).delaySubscription(o);
            }
        }, false, 1, 1, 1);
    }


    @Test
    public void afterDelayNoInterrupt() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            for (Scheduler s : new Scheduler[] { Schedulers.single(), Schedulers.computation(), Schedulers.newThread(), Schedulers.io(), Schedulers.from(exec) }) {
                final TestObserver<Boolean> observer = TestObserver.create();
                observer.withTag(s.getClass().getSimpleName());

                Observable.<Boolean>create(new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                      emitter.onNext(Thread.interrupted());
                      emitter.onComplete();
                    }
                })
                .delaySubscription(100, TimeUnit.MILLISECONDS, s)
                .subscribe(observer);

                observer.awaitTerminalEvent();
                observer.assertValue(false);
            }
        } finally {
            exec.shutdown();
        }
    }

}
