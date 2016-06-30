/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import org.junit.Assert;
import org.junit.Test;
import rx.Single;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleOnSubscribeDelaySubscriptionOtherTest {
    @Test
    public void noPrematureSubscription() {
        PublishSubject<Object> other = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger subscribed = new AtomicInteger();

        Single.just(1)
            .doOnSubscribe(new Action0() {
                @Override
                public void call() {
                    subscribed.getAndIncrement();
                }
            })
            .delaySubscription(other)
            .subscribe(ts);

        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void noPrematureSubscriptionToError() {
        PublishSubject<Object> other = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger subscribed = new AtomicInteger();

        Single.<Integer>error(new TestException())
            .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        subscribed.getAndIncrement();
                    }
                })
            .delaySubscription(other)
            .subscribe(ts);

        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }

    @Test
    public void noSubscriptionIfOtherErrors() {
        PublishSubject<Object> other = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger subscribed = new AtomicInteger();

        Single.<Integer>error(new TestException())
            .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        subscribed.getAndIncrement();
                    }
                })
            .delaySubscription(other)
            .subscribe(ts);

        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onError(new TestException());

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
}
