/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.*;


import io.reactivex.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;


public class OperatorSwitchIfEmptyTest {

    @Test
    public void testSwitchWhenNotEmpty() throws Exception {
        final AtomicBoolean subscribed = new AtomicBoolean(false);
        final Observable<Integer> observable = Observable.just(4)
                .switchIfEmpty(Observable.just(2)
                .doOnSubscribe(s -> subscribed.set(true)));

        assertEquals(4, observable.toBlocking().single().intValue());
        assertFalse(subscribed.get());
    }

    @Test
    public void testSwitchWhenEmpty() throws Exception {
        final Observable<Integer> observable = Observable.<Integer>empty()
                .switchIfEmpty(Observable.fromIterable(Arrays.asList(42)));

        assertEquals(42, observable.toBlocking().single().intValue());
    }

    @Test
    public void testSwitchWithProducer() throws Exception {
        final AtomicBoolean emitted = new AtomicBoolean(false);
        Observable<Long> withProducer = Observable.create(new Publisher<Long>() {
            @Override
            public void subscribe(final Subscriber<? super Long> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        if (n > 0 && emitted.compareAndSet(false, true)) {
                            emitted.set(true);
                            subscriber.onNext(42L);
                            subscriber.onComplete();
                        }
                    }
                    
                    @Override
                    public void cancel() {
                        
                    }
                });
            }
        });

        final Observable<Long> observable = Observable.<Long>empty().switchIfEmpty(withProducer);
        assertEquals(42, observable.toBlocking().single().intValue());
    }

    @Test
    public void testSwitchTriggerUnsubscribe() throws Exception {

        BooleanSubscription bs = new BooleanSubscription();
        
        Observable<Long> withProducer = Observable.create(new Publisher<Long>() {
            @Override
            public void subscribe(final Subscriber<? super Long> subscriber) {
                subscriber.onSubscribe(bs);
                subscriber.onNext(42L);
            }
        });

        Observable.<Long>empty()
                .switchIfEmpty(withProducer)
                .lift(new Observable.Operator<Long, Long>() {
            @Override
            public Subscriber<? super Long> apply(final Subscriber<? super Long> child) {
                return new Observer<Long>() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        cancel();
                    }
                    
                };
            }
        }).subscribe();


        assertTrue(bs.isCancelled());
        // FIXME no longer assertable
//        assertTrue(sub.isUnsubscribed());
    }

    @Test
    public void testSwitchShouldTriggerUnsubscribe() {
        BooleanSubscription bs = new BooleanSubscription();
        
        Observable.create(new Publisher<Long>() {
            @Override
            public void subscribe(final Subscriber<? super Long> subscriber) {
                subscriber.onSubscribe(bs);
                subscriber.onComplete();
            }
        }).switchIfEmpty(Observable.<Long>never()).subscribe();
        assertTrue(bs.isCancelled());
    }

    @Test
    public void testSwitchRequestAlternativeObservableWithBackpressure() {

        TestSubscriber<Integer> ts = new TestSubscriber<>(1L);
        
        Observable.<Integer>empty().switchIfEmpty(Observable.just(1, 2, 3)).subscribe(ts);
        
        assertEquals(Arrays.asList(1), ts.values());
        ts.assertNoErrors();
        ts.request(1);
        ts.assertValueCount(2);
        ts.request(1);
        ts.assertValueCount(3);
    }
    @Test
    public void testBackpressureNoRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>((Long)null);
        Observable.<Integer>empty().switchIfEmpty(Observable.just(1, 2, 3)).subscribe(ts);
        ts.assertNoValues();
        ts.assertNoErrors();
    }
    
    @Test
    public void testBackpressureOnFirstObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<>((Long)null);
        Observable.just(1,2,3).switchIfEmpty(Observable.just(4, 5, 6)).subscribe(ts);
        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();
    }
    
    @Test(timeout = 10000)
    public void testRequestsNotLost() throws InterruptedException {
        final TestSubscriber<Long> ts = new TestSubscriber<>((Long)null);
        Observable.create(new Publisher<Long>() {

            @Override
            public void subscribe(final Subscriber<? super Long> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    final AtomicBoolean completed = new AtomicBoolean(false);
                    @Override
                    public void request(long n) {
                        if (n > 0 && completed.compareAndSet(false, true)) {
                            Schedulers.io().createWorker().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    subscriber.onComplete();
                                }}, 100, TimeUnit.MILLISECONDS);
                        }
                    }
                    
                    @Override
                    public void cancel() {
                        
                    }
                });
            }})
          .switchIfEmpty(Observable.fromIterable(Arrays.asList(1L, 2L, 3L)))
          .subscribeOn(Schedulers.computation())
          .subscribe(ts);

        Thread.sleep(50);
        //request while first observable is still finishing (as empty)
        ts.request(1);
        ts.request(1);
        Thread.sleep(500);
        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertValueCount(2);
        ts.dispose();
    }
}