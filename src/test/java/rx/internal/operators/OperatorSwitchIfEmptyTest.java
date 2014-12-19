/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import org.junit.Test;
import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OperatorSwitchIfEmptyTest {

    @Test
    public void testSwitchWhenNotEmpty() throws Exception {
        final AtomicBoolean subscribed = new AtomicBoolean(false);
        final Observable<Integer> observable = Observable.just(4).switchIfEmpty(Observable.just(2)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        subscribed.set(true);
                    }
                }));

        assertEquals(4, observable.toBlocking().single().intValue());
        assertFalse(subscribed.get());
    }

    @Test
    public void testSwitchWhenEmpty() throws Exception {
        final Observable<Integer> observable = Observable.<Integer>empty().switchIfEmpty(Observable.from(Arrays.asList(42)));

        assertEquals(42, observable.toBlocking().single().intValue());
    }

    @Test
    public void testSwitchWithProducer() throws Exception {
        Observable<Long> withProducer = Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(final Subscriber<? super Long> subscriber) {
                subscriber.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        if (n > 0) {
                            subscriber.onNext(42L);
                            subscriber.onCompleted();
                        }
                    }
                });
            }
        });

        final Observable<Long> observable = Observable.<Long>empty().switchIfEmpty(withProducer);
        assertEquals(42, observable.toBlocking().single().intValue());
    }

    @Test
    public void testSwitchTriggerUnsubscribe() throws Exception {
        final Subscription empty = Subscriptions.empty();

        Observable<Long> withProducer = Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(final Subscriber<? super Long> subscriber) {
                subscriber.add(empty);
                subscriber.onNext(42L);
            }
        });

        final Subscription sub = Observable.<Long>empty().switchIfEmpty(withProducer).lift(new Observable.Operator<Long, Long>() {
            @Override
            public Subscriber<? super Long> call(final Subscriber<? super Long> child) {
                return new Subscriber<Long>(child) {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        unsubscribe();
                    }
                };
            }
        }).subscribe();


        assertTrue(empty.isUnsubscribed());
        assertTrue(sub.isUnsubscribed());
    }
}