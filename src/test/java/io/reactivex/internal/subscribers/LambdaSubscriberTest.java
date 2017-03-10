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

package io.reactivex.internal.subscribers;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;

public class LambdaSubscriberTest {

    @Test
    public void onSubscribeThrows() {
        final List<Object> received = new ArrayList<Object>();

        LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
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
                throw new TestException();
            }
        });

        assertFalse(o.isDisposed());

        Flowable.just(1).subscribe(o);

        assertTrue(received.toString(), received.get(0) instanceof TestException);
        assertEquals(received.toString(), 1, received.size());

        assertTrue(o.isDisposed());
    }

    @Test
    public void onNextThrows() {
        final List<Object> received = new ArrayList<Object>();

        LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                throw new TestException();
            }
        },
        new Consumer<Throwable>() {
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
                s.request(Long.MAX_VALUE);
            }
        });

        assertFalse(o.isDisposed());

        Flowable.just(1).subscribe(o);

        assertTrue(received.toString(), received.get(0) instanceof TestException);
        assertEquals(received.toString(), 1, received.size());

        assertTrue(o.isDisposed());
    }

    @Test
    public void onErrorThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<Object>();

            LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    received.add(v);
                }
            },
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    throw new TestException("Inner");
                }
            }, new Action() {
                @Override
                public void run() throws Exception {
                    received.add(100);
                }
            }, new Consumer<Subscription>() {
                @Override
                public void accept(Subscription s) throws Exception {
                    s.request(Long.MAX_VALUE);
                }
            });

            assertFalse(o.isDisposed());

            Flowable.<Integer>error(new TestException("Outer")).subscribe(o);

            assertTrue(received.toString(), received.isEmpty());

            assertTrue(o.isDisposed());

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
            final List<Object> received = new ArrayList<Object>();

            LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    received.add(v);
                }
            },
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    received.add(e);
                }
            }, new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            }, new Consumer<Subscription>() {
                @Override
                public void accept(Subscription s) throws Exception {
                    s.request(Long.MAX_VALUE);
                }
            });

            assertFalse(o.isDisposed());

            Flowable.<Integer>empty().subscribe(o);

            assertTrue(received.toString(), received.isEmpty());

            assertTrue(o.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
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

        final List<Object> received = new ArrayList<Object>();

        LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
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
                s.request(Long.MAX_VALUE);
            }
        });

        source.subscribe(o);

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

        final List<Object> received = new ArrayList<Object>();

        LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
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
                s.request(Long.MAX_VALUE);
            }
        });

        source.subscribe(o);

        assertEquals(Arrays.asList(1, 100), received);
    }

    @Test
    public void onNextThrowsCancelsUpstream() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        final List<Throwable> errors = new ArrayList<Throwable>();

        ps.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                errors.add(e);
            }
        });

        assertTrue("No observers?!", ps.hasSubscribers());
        assertTrue("Has errors already?!", errors.isEmpty());

        ps.onNext(1);

        assertFalse("Has observers?!", ps.hasSubscribers());
        assertFalse("No errors?!", errors.isEmpty());

        assertTrue(errors.toString(), errors.get(0) instanceof TestException);
    }

    @Test
    public void onSubscribeThrowsCancelsUpstream() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        final List<Throwable> errors = new ArrayList<Throwable>();

        ps.subscribe(new Consumer<Integer>() {
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
        });

        assertFalse("Has observers?!", ps.hasSubscribers());
        assertFalse("No errors?!", errors.isEmpty());

        assertTrue(errors.toString(), errors.get(0) instanceof TestException);
    }
}
