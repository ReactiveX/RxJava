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
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableStrictTest {

    @Test
    public void empty() {
        Flowable.empty()
        .strict()
        .test()
        .assertResult();
    }

    @Test
    public void just() {
        Flowable.just(1)
        .strict()
        .test()
        .assertResult(1);
    }

    @Test
    public void range() {
        Flowable.range(1, 5)
        .strict()
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .take(2)
        .strict()
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void backpressure() {
        Flowable.range(1, 5)
        .strict()
        .test(0)
        .assertEmpty()
        .requestMore(1)
        .assertValue(1)
        .requestMore(2)
        .assertValues(1, 2, 3)
        .requestMore(2)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .strict()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void observeOn() {
        Flowable.range(1, 5)
        .hide()
        .observeOn(Schedulers.single())
        .strict()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void invalidRequest() {
        for (int i = 0; i > -100; i--) {
            final int j = i;
            final List<Object> items = new ArrayList<Object>();

            Flowable.range(1, 2)
            .strict()
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(j);
                }

                @Override
                public void onNext(Integer t) {
                    items.add(t);
                }

                @Override
                public void onError(Throwable t) {
                    items.add(t);
                }

                @Override
                public void onComplete() {
                    items.add("Done");
                }
            });

            assertTrue(items.toString(), items.size() == 1);
            assertTrue(items.toString(), items.get(0) instanceof IllegalArgumentException);
            assertTrue(items.toString(), items.get(0).toString().contains("§3.9"));
        }
    }

    @Test
    public void doubleOnSubscribe() {
        final BooleanSubscription bs1 = new BooleanSubscription();
        final BooleanSubscription bs2 = new BooleanSubscription();

        TestSubscriber<Object> ts = Flowable.fromPublisher(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> p) {
                p.onSubscribe(bs1);
                p.onSubscribe(bs2);
            }
        })
        .strict()
        .test()
        .assertFailure(IllegalStateException.class);

        assertTrue(bs1.isCancelled());
        assertTrue(bs2.isCancelled());

        String es = ts.errors().get(0).toString();
        assertTrue(es, es.contains("§2.12"));
    }

    @Test
    public void noCancelOnComplete() {
        final BooleanSubscription bs = new BooleanSubscription();

        Flowable.fromPublisher(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> p) {
                p.onSubscribe(bs);
                p.onComplete();
            }
        })
        .strict()
        .subscribe(new Subscriber<Object>() {

            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
            }

            @Override
            public void onNext(Object t) {
                // not called
            }

            @Override
            public void onError(Throwable t) {
                // not called
            }

            @Override
            public void onComplete() {
                s.cancel();
            }
        });

        assertFalse(bs.isCancelled());
    }

    @Test
    public void noCancelOnError() {
        final BooleanSubscription bs = new BooleanSubscription();

        Flowable.fromPublisher(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> p) {
                p.onSubscribe(bs);
                p.onError(new TestException());
            }
        })
        .strict()
        .subscribe(new Subscriber<Object>() {

            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
            }

            @Override
            public void onNext(Object t) {
                // not called
            }

            @Override
            public void onError(Throwable t) {
                s.cancel();
            }

            @Override
            public void onComplete() {
                // not called
            }
        });

        assertFalse(bs.isCancelled());
    }
}
