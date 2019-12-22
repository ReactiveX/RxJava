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

package io.reactivex.rxjava3.internal.subscribers;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class BoundedSubscriberTest extends RxJavaTest {

    @Test
    public void onSubscribeThrows() {
        final List<Object> received = new ArrayList<>();

        BoundedSubscriber<Object> subscriber = new BoundedSubscriber<>(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                received.add(o);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                received.add(throwable);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                received.add(1);
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription subscription) throws Exception {
                throw new TestException();
            }
        }, 128);

        assertFalse(subscriber.isDisposed());

        Flowable.just(1).subscribe(subscriber);

        assertTrue(received.toString(), received.get(0) instanceof TestException);
        assertEquals(received.toString(), 1, received.size());

        assertTrue(subscriber.isDisposed());
    }

    @Test
    public void onNextThrows() {
        final List<Object> received = new ArrayList<>();

        BoundedSubscriber<Object> subscriber = new BoundedSubscriber<>(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                throw new TestException();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                received.add(throwable);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                received.add(1);
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription subscription) throws Exception {
                subscription.request(128);
            }
        }, 128);

        assertFalse(subscriber.isDisposed());

        Flowable.just(1).subscribe(subscriber);

        assertTrue(received.toString(), received.get(0) instanceof TestException);
        assertEquals(received.toString(), 1, received.size());

        assertTrue(subscriber.isDisposed());
    }

    @Test
    public void onErrorThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<>();

            BoundedSubscriber<Object> subscriber = new BoundedSubscriber<>(new Consumer<Object>() {
                @Override
                public void accept(Object o) throws Exception {
                    received.add(o);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    throw new TestException("Inner");
                }
            }, new Action() {
                @Override
                public void run() throws Exception {
                    received.add(1);
                }
            }, new Consumer<Subscription>() {
                @Override
                public void accept(Subscription subscription) throws Exception {
                    subscription.request(128);
                }
            }, 128);

            assertFalse(subscriber.isDisposed());

            Flowable.<Integer>error(new TestException("Outer")).subscribe(subscriber);

            assertTrue(received.toString(), received.isEmpty());

            assertTrue(subscriber.isDisposed());

            TestHelper.assertError(errors, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(errors.get(0));
            TestHelper.assertError(ce, 0, TestException.class, "Outer");
            TestHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<>();

            BoundedSubscriber<Object> subscriber = new BoundedSubscriber<>(new Consumer<Object>() {
                @Override
                public void accept(Object o) throws Exception {
                    received.add(o);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    received.add(throwable);
                }
            }, new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            }, new Consumer<Subscription>() {
                @Override
                public void accept(Subscription subscription) throws Exception {
                    subscription.request(128);
                }
            }, 128);

            assertFalse(subscriber.isDisposed());

            Flowable.<Integer>empty().subscribe(subscriber);

            assertTrue(received.toString(), received.isEmpty());

            assertTrue(subscriber.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextThrowsCancelsUpstream() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final List<Throwable> errors = new ArrayList<>();

        BoundedSubscriber<Integer> s = new BoundedSubscriber<>(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                errors.add(e);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {

            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription subscription) throws Exception {
                subscription.request(128);
            }
        }, 128);

        pp.subscribe(s);

        assertTrue("No observers?!", pp.hasSubscribers());
        assertTrue("Has errors already?!", errors.isEmpty());

        pp.onNext(1);

        assertFalse("Has observers?!", pp.hasSubscribers());
        assertFalse("No errors?!", errors.isEmpty());

        assertTrue(errors.toString(), errors.get(0) instanceof TestException);
    }

    @Test
    public void onSubscribeThrowsCancelsUpstream() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final List<Throwable> errors = new ArrayList<>();

        BoundedSubscriber<Integer> s = new BoundedSubscriber<>(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                errors.add(e);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                throw new TestException();
            }
        }, 128);

        pp.subscribe(s);

        assertFalse("Has observers?!", pp.hasSubscribers());
        assertFalse("No errors?!", errors.isEmpty());

        assertTrue(errors.toString(), errors.get(0) instanceof TestException);
    }

    @Test
    public void badSourceOnSubscribe() {
        Flowable<Integer> source = Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                BooleanSubscription s1 = new BooleanSubscription();
                s.onSubscribe(s1);
                BooleanSubscription s2 = new BooleanSubscription();
                s.onSubscribe(s2);

                assertFalse(s1.isCancelled());
                assertTrue(s2.isCancelled());

                s.onNext(1);
                s.onComplete();
            }
        });

        final List<Object> received = new ArrayList<>();

        BoundedSubscriber<Object> subscriber = new BoundedSubscriber<>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                received.add(100);
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                s.request(128);
            }
        }, 128);

        source.subscribe(subscriber);

        assertEquals(Arrays.asList(1, 100), received);
    }

    @Test
    public void badSourceEmitAfterDone() {
        Flowable<Integer> source = Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                BooleanSubscription s1 = new BooleanSubscription();
                s.onSubscribe(s1);

                s.onNext(1);
                s.onComplete();
                s.onNext(2);
                s.onError(new TestException());
                s.onComplete();
            }
        });

        final List<Object> received = new ArrayList<>();

        BoundedSubscriber<Object> subscriber = new BoundedSubscriber<>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                received.add(100);
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                s.request(128);
            }
        }, 128);

        source.subscribe(subscriber);

        assertEquals(Arrays.asList(1, 100), received);
    }

    @Test
    public void onErrorMissingShouldReportNoCustomOnError() {
        BoundedSubscriber<Integer> subscriber = new BoundedSubscriber<>(Functions.<Integer>emptyConsumer(),
                Functions.ON_ERROR_MISSING,
                Functions.EMPTY_ACTION,
                Functions.<Subscription>boundedConsumer(128), 128);

        assertFalse(subscriber.hasCustomOnError());
    }

    @Test
    public void customOnErrorShouldReportCustomOnError() {
        BoundedSubscriber<Integer> subscriber = new BoundedSubscriber<>(Functions.<Integer>emptyConsumer(),
                Functions.<Throwable>emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.<Subscription>boundedConsumer(128), 128);

        assertTrue(subscriber.hasCustomOnError());
    }
}
