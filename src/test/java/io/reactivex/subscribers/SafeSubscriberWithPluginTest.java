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

package io.reactivex.subscribers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.*;
import org.reactivestreams.Subscription;

import io.reactivex.exceptions.*;
import io.reactivex.plugins.RxJavaPlugins;

import static org.junit.Assert.*;

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
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
        try {
            safe.onComplete();
            Assert.fail();
        } catch (OnCompleteFailedException e) {
            // FIXME no longer assertable
            // assertTrue(safe.isUnsubscribed());
        }
    }
    
    @Test
    public void testOnCompletedThrows2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new OnErrorNotImplementedException(new TestException());
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
        
        try {
            safe.onComplete();
        } catch (OnErrorNotImplementedException ex) {
            // expected
        }
        
        // FIXME no longer assertable
        // assertTrue(safe.isUnsubscribed());
    }
    
    @Test(expected = OnCompleteFailedException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginException() {
        RxJavaPlugins.setErrorHandler(e -> {
            throw new RuntimeException();
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new TestException();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
        
        safe.onComplete();
    }
    
    @Test(expected = OnErrorFailedException.class)
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
        
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
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
                throw new OnErrorNotImplementedException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());
        
        safe.onError(new TestException());
    }
    
    @Test(expected = OnErrorFailedException.class)
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
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
        
        safe.onError(new TestException());
    }
    @Test(expected = OnErrorFailedException.class)
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
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());
        
        safe.onError(new TestException());
    }
    @Test(expected = OnErrorFailedException.class)
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
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
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
        
        final AtomicInteger errors = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                errors.incrementAndGet();
            }
        };
        final RuntimeException ex = new RuntimeException();
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
        safe.onSubscribe(new Subscription() {
            @Override
            public void cancel() {
                throw ex;
            }
            
            @Override
            public void request(long n) {
                
            }
        });
        
        try {
            safe.onComplete();
            Assert.fail();
        } catch(UnsubscribeFailedException e) {
            assertEquals(1, calls.get());
            assertEquals(0, errors.get());
        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void testPluginErrorHandlerReceivesExceptionFromFailingUnsubscribeAfterCompletionThrows() {
        final AtomicInteger calls = new AtomicInteger();
        RxJavaPlugins.setErrorHandler(e -> {
                calls.incrementAndGet();
        });
        
        final AtomicInteger errors = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            
            @Override 
            public void onComplete() {
                throw new RuntimeException();
            }
            
            @Override
            public void onError(Throwable e) {
                errors.incrementAndGet();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());
        
        try {
            safe.onComplete();
            Assert.fail();
        } catch(UnsubscribeFailedException e) {
            assertEquals(2, calls.get());
            assertEquals(0, errors.get());
        }
    }
}
