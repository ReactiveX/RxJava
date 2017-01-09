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

package io.reactivex.subscribers;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.Subscription;

import io.reactivex.exceptions.*;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

public class SafeSubscriberWithPluginTest {
    private final class SubscriptionCancelThrows implements Subscription {
        @Override
        public void cancel() {
            throw new RuntimeException();
        }

        @Override
        public void request(long n) {

        }
    }

    @Before
    @After
    public void resetBefore() {
        RxJavaPlugins.reset();
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void testOnCompletedThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new TestException();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        try {
            safe.onComplete();
            fail();
        } catch (RuntimeException e) {
            // FIXME no longer assertable
            // assertTrue(safe.isUnsubscribed());
        }
    }

    @Test
    public void testOnCompletedThrows2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new RuntimeException(new TestException());
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);

        try {
            safe.onComplete();
        } catch (RuntimeException ex) {
            // expected
        }

        // FIXME no longer assertable
        // assertTrue(safe.isUnsubscribed());
    }

    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginException() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                throw new RuntimeException();
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new TestException();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);

        safe.onComplete();
    }

    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhileOnErrorUnsubscribeThrows() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            int calls;
            @Override
            public void accept(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

        safe.onError(new TestException());
    }

    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhileOnErrorThrowsNotImplAndUnsubscribeThrows() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            int calls;
            @Override
            public void accept(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

        safe.onError(new TestException());
    }

    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhileOnErrorThrows() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            int calls;
            @Override
            public void accept(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);

        safe.onError(new TestException());
    }
    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhileOnErrorThrowsAndUnsubscribeThrows() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            int calls;
            @Override
            public void accept(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

        safe.onError(new TestException());
    }
    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhenUnsubscribing2() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            int calls;
            @Override
            public void accept(Throwable e) {
                if (++calls == 3) {
                    throw new RuntimeException();
                }
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

        safe.onError(new TestException());
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void testPluginErrorHandlerReceivesExceptionWhenUnsubscribeAfterCompletionThrows() {
        final AtomicInteger calls = new AtomicInteger();
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                calls.incrementAndGet();
            }
        });

        final AtomicInteger errorCount = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                errorCount.incrementAndGet();
            }
        };
        final RuntimeException ex = new RuntimeException();
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new Subscription() {
            @Override
            public void cancel() {
                throw ex;
            }

            @Override
            public void request(long n) {

            }
        });

//        try {
//            safe.onComplete();
//            Assert.fail();
//        } catch(UnsubscribeFailedException e) {
//            assertEquals(1, calls.get());
//            assertEquals(0, errors.get());
//        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void testPluginErrorHandlerReceivesExceptionFromFailingUnsubscribeAfterCompletionThrows() {
        final AtomicInteger calls = new AtomicInteger();
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                    calls.incrementAndGet();
            }
        });

        final AtomicInteger errorCount = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {

            @Override
            public void onComplete() {
                throw new RuntimeException();
            }

            @Override
            public void onError(Throwable e) {
                errorCount.incrementAndGet();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

//        try {
//            safe.onComplete();
//            Assert.fail();
//        } catch(UnsubscribeFailedException e) {
//            assertEquals(2, calls.get());
//            assertEquals(0, errors.get());
//        }
    }
}
