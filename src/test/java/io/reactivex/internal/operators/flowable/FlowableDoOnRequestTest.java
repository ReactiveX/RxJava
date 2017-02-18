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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;

import io.reactivex.Flowable;
import io.reactivex.functions.*;
import io.reactivex.subscribers.DefaultSubscriber;

public class FlowableDoOnRequestTest {

    @Test
    public void testUnsubscribeHappensAgainstParent() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Flowable.just(1).concatWith(Flowable.<Integer>never())
        //
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        unsubscribed.set(true);
                    }
                })
                //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        // do nothing
                    }
                })
                //
                .subscribe().dispose();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void testDoRequest() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable.range(1, 5)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(t);
                    }
                });
        assertEquals(Arrays.asList(3L,1L,2L,3L,4L,5L), requests);
    }

    @Test
    @Ignore("This is a 1.x architecture-specific test")
    public void dontRequestIfDownstreamRequestsLate() {
//        final List<Long> requested = new ArrayList<Long>();
//
//        Action1<Long> empty = Actions.empty();
//
//        final AtomicReference<Producer> producer = new AtomicReference<Producer>();
//
//        Observable.create(new OnSubscribe<Integer>() {
//            @Override
//            public void call(Subscriber<? super Integer> t) {
//                t.setProducer(new Producer() {
//                    @Override
//                    public void request(long n) {
//                        requested.add(n);
//                    }
//                });
//            }
//        }).doOnRequest(empty).subscribe(new FlowableSubscriber<Object>() {
//            @Override
//            public void onNext(Object t) {
//
//            }
//
//            @Override
//            public void onError(Throwable e) {
//
//            }
//
//            @Override
//            public void onComplete() {
//
//            }
//
//            @Override
//            public void setProducer(Producer p) {
//                producer.set(p);
//            }
//        });
//
//        producer.get().request(1);
//
//        int s = requested.size();
//        if (s == 1) {
//            // this allows for an implementation that itself doesn't request
//            Assert.assertEquals(Arrays.asList(1L), requested);
//        } else {
//            Assert.assertEquals(Arrays.asList(0L, 1L), requested);
//        }
    }
}
