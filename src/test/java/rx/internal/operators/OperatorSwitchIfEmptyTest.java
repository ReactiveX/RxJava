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

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

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
        final AtomicBoolean emitted = new AtomicBoolean(false);
        Observable<Long> withProducer = Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(final Subscriber<? super Long> subscriber) {
                subscriber.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        if (n > 0 && !emitted.get()) {
                            emitted.set(true);
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

    @Test
    public void testSwitchShouldTriggerUnsubscribe() {
        final Subscription s = Subscriptions.empty();

        Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(final Subscriber<? super Long> subscriber) {
                subscriber.add(s);
                subscriber.onCompleted();
            }
        }).switchIfEmpty(Observable.<Long>never()).subscribe();
        assertTrue(s.isUnsubscribed());
    }

    @Test
    public void testSwitchRequestAlternativeObservableWithBackpressure() {

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }
        };
        Observable.<Integer>empty().switchIfEmpty(Observable.just(1, 2, 3)).subscribe(ts);
        
        assertEquals(Arrays.asList(1), ts.getOnNextEvents());
        ts.assertNoErrors();
        ts.requestMore(1);
        ts.assertValueCount(2);
        ts.requestMore(1);
        ts.assertValueCount(3);
    }
    @Test
    public void testBackpressureNoRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(0);
            }
        };
        Observable.<Integer>empty().switchIfEmpty(Observable.just(1, 2, 3)).subscribe(ts);
        assertTrue(ts.getOnNextEvents().isEmpty());
        ts.assertNoErrors();
    }
    
    @Test
    public void testBackpressureOnFirstObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        Observable.just(1,2,3).switchIfEmpty(Observable.just(4, 5, 6)).subscribe(ts);
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();
    }
    
    @Test(timeout = 10000)
    public void testRequestsNotLost() throws InterruptedException {
        final TestSubscriber<Long> ts = new TestSubscriber<Long>(0);
        Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(final Subscriber<? super Long> subscriber) {
                subscriber.setProducer(new Producer() {
                    final AtomicBoolean completed = new AtomicBoolean(false);
                    @Override
                    public void request(long n) {
                        if (n > 0 && completed.compareAndSet(false, true)) {
                            Schedulers.io().createWorker().schedule(new Action0() {
                                @Override
                                public void call() {
                                    subscriber.onCompleted();
                                }}, 100, TimeUnit.MILLISECONDS);
                        }
                    }});
            }})
          .switchIfEmpty(Observable.from(Arrays.asList(1L, 2L, 3L)))
          .subscribeOn(Schedulers.computation())
          .subscribe(ts);
        ts.requestMore(0);
        Thread.sleep(50);
        //request while first observable is still finishing (as empty)
        ts.requestMore(1);
        ts.requestMore(1);
        Thread.sleep(500);
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertValueCount(2);
        ts.unsubscribe();
    }
}