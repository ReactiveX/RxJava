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
package rx.observers;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.*;

import rx.exceptions.*;
import rx.functions.Action0;
import rx.plugins.*;
import rx.subscriptions.Subscriptions;

public class SafeSubscriberTest {
    
    @Before
    @After
    public void resetBefore() {
        RxJavaPlugins ps = RxJavaPlugins.getInstance();
        
        try {
            Method m = ps.getClass().getDeclaredMethod("reset");
            m.setAccessible(true);
            m.invoke(ps);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testOnCompletedThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onCompleted() {
                throw new TestException();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        
        safe.onCompleted();
        
        assertTrue(safe.isUnsubscribed());
    }
    
    @Test
    public void testOnCompletedThrows2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onCompleted() {
                throw new OnErrorNotImplementedException(new TestException());
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        
        try {
            safe.onCompleted();
        } catch (OnErrorNotImplementedException ex) {
            // expected
        }
        
        assertTrue(safe.isUnsubscribed());
    }
    
    @Test
    public void testPluginException() {
        RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
            @Override
            public void handleError(Throwable e) {
                throw new RuntimeException();
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onCompleted() {
                throw new TestException();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        
        safe.onCompleted();
    }
    
    @Test(expected = OnErrorFailedException.class)
    public void testPluginExceptionWhileOnErrorUnsubscribeThrows() {
        RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
            int calls;
            @Override
            public void handleError(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                throw new RuntimeException();
            }
        }));
        
        safe.onError(new TestException());
    }
    
    @Test(expected = RuntimeException.class)
    public void testPluginExceptionWhileOnErrorThrowsNotImplAndUnsubscribeThrows() {
        RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
            int calls;
            @Override
            public void handleError(Throwable e) {
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
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                throw new RuntimeException();
            }
        }));
        
        safe.onError(new TestException());
    }
    
    @Test(expected = OnErrorFailedException.class)
    public void testPluginExceptionWhileOnErrorThrows() {
        RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
            int calls;
            @Override
            public void handleError(Throwable e) {
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
    @Test(expected = OnErrorFailedException.class)
    public void testPluginExceptionWhileOnErrorThrowsAndUnsubscribeThrows() {
        RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
            int calls;
            @Override
            public void handleError(Throwable e) {
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
        safe.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                throw new RuntimeException();
            }
        }));
        
        safe.onError(new TestException());
    }
    @Test(expected = OnErrorFailedException.class)
    public void testPluginExceptionWhenUnsubscribing2() {
        RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
            int calls;
            @Override
            public void handleError(Throwable e) {
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
        safe.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                throw new RuntimeException();
            }
        }));
        
        safe.onError(new TestException());
    }
}
