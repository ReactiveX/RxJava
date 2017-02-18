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

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableBlockingTest {

    @Test
    public void blockingFirst() {
        assertEquals(1, Flowable.range(1, 10)
                .subscribeOn(Schedulers.computation()).blockingFirst().intValue());
    }

    @Test
    public void blockingFirstDefault() {
        assertEquals(1, Flowable.<Integer>empty()
                .subscribeOn(Schedulers.computation()).blockingFirst(1).intValue());
    }

    @Test
    public void blockingSubscribeConsumer() {
        final List<Integer> list = new ArrayList<Integer>();

        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumer() {
        final List<Object> list = new ArrayList<Object>();

        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, Functions.emptyConsumer());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerError() {
        final List<Object> list = new ArrayList<Object>();

        TestException ex = new TestException();

        Consumer<Object> cons = new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                list.add(v);
            }
        };

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerAction() {
        final List<Object> list = new ArrayList<Object>();

        Consumer<Object> cons = new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                list.add(v);
            }
        };

        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons, new Action() {
            @Override
            public void run() throws Exception {
                list.add(100);
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserver() {
        final List<Object> list = new ArrayList<Object>();

        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new FlowableSubscriber<Object>() {

            @Override
            public void onSubscribe(Subscription d) {
                d.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }

        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserverError() {
        final List<Object> list = new ArrayList<Object>();

        final TestException ex = new TestException();

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new FlowableSubscriber<Object>() {

            @Override
            public void onSubscribe(Subscription d) {
                d.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }

        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test(expected = TestException.class)
    public void blockingForEachThrows() {
        Flowable.just(1)
        .blockingForEach(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                throw new TestException();
            }
        });
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingFirstEmpty() {
        Flowable.empty().blockingFirst();
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingLastEmpty() {
        Flowable.empty().blockingLast();
    }

    @Test
    public void blockingFirstNormal() {
        assertEquals(1, Flowable.just(1, 2).blockingFirst(3).intValue());
    }

    @Test
    public void blockingLastNormal() {
        assertEquals(2, Flowable.just(1, 2).blockingLast(3).intValue());
    }

    @Test
    public void firstFgnoredCancelAndOnNext() {
        Flowable<Integer> source = Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
            }
        });

        assertEquals(1, source.blockingFirst().intValue());
    }

    @Test
    public void firstIgnoredCancelAndOnError() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        try {
            Flowable<Integer> source = Flowable.fromPublisher(new Publisher<Integer>() {
                @Override
                public void subscribe(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onError(new TestException());
                }
            });

            assertEquals(1, source.blockingFirst().intValue());

            TestHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test(expected = TestException.class)
    public void firstOnError() {
        Flowable<Integer> source = Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onError(new TestException());
            }
        });

        source.blockingFirst();
    }

    @Test
    public void interrupt() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        Thread.currentThread().interrupt();

        try {
            Flowable.just(1)
            .blockingSubscribe(ts);

            ts.assertFailure(InterruptedException.class);
        } finally {
            Thread.interrupted(); // clear interrupted status just in case
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingSingleEmpty() {
        Flowable.empty().blockingSingle();
    }

    @Test
    public void onCompleteDelayed() {
        TestSubscriber<Object> to = new TestSubscriber<Object>();

        Flowable.empty().delay(100, TimeUnit.MILLISECONDS)
        .blockingSubscribe(to);

        to.assertResult();
    }

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(FlowableBlockingSubscribe.class);
    }

    @Test
    public void disposeUpFront() {
        TestSubscriber<Object> to = new TestSubscriber<Object>();
        to.dispose();
        Flowable.just(1).blockingSubscribe(to);

        to.assertEmpty();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void delayed() throws Exception {
        final TestSubscriber<Object> to = new TestSubscriber<Object>();
        final Subscriber[] s = { null };

        Schedulers.single().scheduleDirect(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                to.dispose();
                s[0].onNext(1);
            }
        }, 200, TimeUnit.MILLISECONDS);

        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> observer) {
                observer.onSubscribe(new BooleanSubscription());
                s[0] = observer;
            }
        }.blockingSubscribe(to);

        while (!to.isDisposed()) {
            Thread.sleep(100);
        }

        to.assertEmpty();
    }

    @Test
    public void blockinsSubscribeCancelAsync() {
        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            final Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            final AtomicInteger c = new AtomicInteger(2);

            Schedulers.computation().scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    c.decrementAndGet();
                    while (c.get() != 0 && !pp.hasSubscribers()) { }

                    TestHelper.race(r1, r2);
                }
            });

            c.decrementAndGet();
            while (c.get() != 0) { }

            pp
            .blockingSubscribe(ts);
        }
    }
}
