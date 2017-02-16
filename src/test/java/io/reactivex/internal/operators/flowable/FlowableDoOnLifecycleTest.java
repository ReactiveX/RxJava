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

import java.util.List;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;

public class FlowableDoOnLifecycleTest {

    @Test
    public void onSubscribeCrashed() {
        Flowable.just(1)
        .doOnLifecycle(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                throw new TestException();
            }
        }, Functions.EMPTY_LONG_CONSUMER, Functions.EMPTY_ACTION)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        final int[] calls = { 0, 0 };

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> o) throws Exception {
                return o
                .doOnLifecycle(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription s) throws Exception {
                        calls[0]++;
                    }
                }, Functions.EMPTY_LONG_CONSUMER, new Action() {
                    @Override
                    public void run() throws Exception {
                        calls[1]++;
                    }
                });
            }
        });

        assertEquals(2, calls[0]);
        assertEquals(0, calls[1]);
    }

    @Test
    public void dispose() {
        final int[] calls = { 0, 0 };

        TestHelper.checkDisposed(Flowable.just(1)
                .doOnLifecycle(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription s) throws Exception {
                        calls[0]++;
                    }
                }, Functions.EMPTY_LONG_CONSUMER, new Action() {
                    @Override
                    public void run() throws Exception {
                        calls[1]++;
                    }
                })
            );

        assertEquals(1, calls[0]);
        assertEquals(2, calls[1]);
    }

    @Test
    public void requestCrashed() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .doOnLifecycle(Functions.emptyConsumer(),
                    new LongConsumer() {
                        @Override
                        public void accept(long v) throws Exception {
                            throw new TestException();
                        }
                    },
                    Functions.EMPTY_ACTION)
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelCrashed() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .doOnLifecycle(Functions.emptyConsumer(),
                    Functions.EMPTY_LONG_CONSUMER,
                    new Action() {
                        @Override
                        public void run() throws Exception {
                            throw new TestException();
                        }
                    })
            .take(1)
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BooleanSubscription bs = new BooleanSubscription();

            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(bs);
                    s.onError(new TestException("Second"));
                    s.onComplete();
                }
            }
            .doOnSubscribe(new Consumer<Subscription>() {
                @Override
                public void accept(Subscription s) throws Exception {
                    throw new TestException("First");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isCancelled());

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
