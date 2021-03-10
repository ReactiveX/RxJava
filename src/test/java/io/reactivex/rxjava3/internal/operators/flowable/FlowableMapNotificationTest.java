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

package io.reactivex.rxjava3.internal.operators.flowable;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableMapNotification.MapNotificationSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableMapNotificationTest extends RxJavaTest {
    @Test
    public void just() {
        TestSubscriber<Object> ts = new TestSubscriber<>();
        Flowable.just(1)
        .flatMap(
                (Function<Integer, Flowable<Object>>) item -> Flowable.just((Object)(item + 1)),
                (Function<Throwable, Flowable<Object>>) Flowable::error,
                (Supplier<Flowable<Object>>) Flowable::never
        ).subscribe(ts);

        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValue(2);
    }

    @Test
    public void backpressure() {
        TestSubscriber<Object> ts = TestSubscriber.create(0L);

        new FlowableMapNotification<>(Flowable.range(1, 3),
                item -> item + 1,
                e -> 0,
                () -> 5
        ).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(3);

        ts.assertValues(2, 3, 4);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1);

        ts.assertValues(2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void noBackpressure() {
        TestSubscriber<Object> ts = TestSubscriber.create(0L);

        PublishProcessor<Integer> pp = PublishProcessor.create();

        new FlowableMapNotification<>(pp,
                item -> item + 1,
                e -> 0,
                () -> 5
        ).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        pp.onNext(1);
        pp.onNext(2);
        pp.onNext(3);
        pp.onComplete();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1);

        ts.assertValue(0);
        ts.assertNoErrors();
        ts.assertComplete();

    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(new Flowable<Integer>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected void subscribeActual(@NonNull Subscriber<? super Integer> subscriber) {
                MapNotificationSubscriber mn = new MapNotificationSubscriber(
                        subscriber,
                        Functions.justFunction(Flowable.just(1)),
                        Functions.justFunction(Flowable.just(2)),
                        Functions.justSupplier(Flowable.just(3))
                );
                mn.onSubscribe(new BooleanSubscription());
            }
        });
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable((Function<Flowable<Object>, Flowable<Integer>>) f -> f.flatMap(
                Functions.justFunction(Flowable.just(1)),
                Functions.justFunction(Flowable.just(2)),
                Functions.justSupplier(Flowable.just(3))
        ));
    }

    @Test
    public void onErrorCrash() {
        TestSubscriberEx<Integer> ts = Flowable.<Integer>error(new TestException("Outer"))
        .flatMap(Functions.justFunction(Flowable.just(1)),
                (Function<Throwable, Publisher<Integer>>) t -> {
                    throw new TestException("Inner");
                },
                Functions.justSupplier(Flowable.just(3)))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class, "Outer");
        TestHelper.assertError(ts, 1, TestException.class, "Inner");
    }
}
