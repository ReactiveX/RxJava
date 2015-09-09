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

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.Observer;
import io.reactivex.exceptions.*;
import io.reactivex.internal.subscriptions.EmptySubscription;

public class SafeObserverTest {

    @Test
    public void onNextFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<>();
        try {
            OBSERVER_ONNEXT_FAIL(onError).onNext("one");
            fail("expects exception to be thrown");
        } catch (Exception e) {
            assertNull(onError.get());
            assertTrue(e instanceof SafeObserverTestException);
            assertEquals("onNextFail", e.getMessage());
        }
    }

    @Test
    public void onNextFailureSafe() {
        AtomicReference<Throwable> onError = new AtomicReference<>();
        try {
            SafeSubscriber<String> safeSubscriber = new SafeSubscriber<>(OBSERVER_ONNEXT_FAIL(onError));
            safeSubscriber.onSubscribe(EmptySubscription.INSTANCE);
            safeSubscriber.onNext("one");
            assertNotNull(onError.get());
            assertTrue(onError.get() instanceof SafeObserverTestException);
            assertEquals("onNextFail", onError.get().getMessage());
        } catch (Exception e) {
            fail("expects exception to be passed to onError");
        }
    }

    @Test
    public void onCompletedFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<>();
        try {
            OBSERVER_ONCOMPLETED_FAIL(onError).onComplete();
            fail("expects exception to be thrown");
        } catch (Exception e) {
            assertNull(onError.get());
            assertTrue(e instanceof SafeObserverTestException);
            assertEquals("onCompletedFail", e.getMessage());
        }
    }

    @Test
    public void onErrorFailure() {
        try {
            OBSERVER_ONERROR_FAIL().onError(new SafeObserverTestException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            assertTrue(e instanceof SafeObserverTestException);
            assertEquals("onErrorFail", e.getMessage());
        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void onErrorFailureSafe() {
        try {
            new SafeSubscriber<>(OBSERVER_ONERROR_FAIL()).onError(new SafeObserverTestException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e instanceof RuntimeException);
            assertEquals("Error occurred when trying to propagate error to Observer.onError", e.getMessage());

            Throwable e2 = e.getCause();
            assertTrue(e2 instanceof CompositeException);
            List<Throwable> innerExceptions = ((CompositeException) e2).getExceptions();
            assertEquals(2, innerExceptions.size());

            Throwable e3 = innerExceptions.get(0);
            assertTrue(e3 instanceof SafeObserverTestException);
            assertEquals("error!", e3.getMessage());

            Throwable e4 = innerExceptions.get(1);
            assertTrue(e4 instanceof SafeObserverTestException);
            assertEquals("onErrorFail", e4.getMessage());
        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void onErrorNotImplementedFailureSafe() {
        try {
            new SafeSubscriber<>(OBSERVER_ONERROR_NOTIMPLEMENTED()).onError(new SafeObserverTestException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            assertTrue(e instanceof OnErrorNotImplementedException);
            assertTrue(e.getCause() instanceof SafeObserverTestException);
            assertEquals("error!", e.getCause().getMessage());
        }
    }

    @Test
    public void onNextOnErrorFailure() {
        try {
            OBSERVER_ONNEXT_ONERROR_FAIL().onNext("one");
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e instanceof SafeObserverTestException);
            assertEquals("onNextFail", e.getMessage());
        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void onNextOnErrorFailureSafe() {
        try {
            new SafeSubscriber<>(OBSERVER_ONNEXT_ONERROR_FAIL()).onNext("one");
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e instanceof RuntimeException);
            assertEquals("Error occurred when trying to propagate error to Observer.onError", e.getMessage());

            Throwable e2 = e.getCause();
            assertTrue(e2 instanceof CompositeException);
            List<Throwable> innerExceptions = ((CompositeException) e2).getExceptions();
            assertEquals(2, innerExceptions.size());

            Throwable e3 = innerExceptions.get(0);
            assertTrue(e3 instanceof SafeObserverTestException);
            assertEquals("onNextFail", e3.getMessage());

            Throwable e4 = innerExceptions.get(1);
            assertTrue(e4 instanceof SafeObserverTestException);
            assertEquals("onErrorFail", e4.getMessage());
        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void onCompleteSuccessWithUnsubscribeFailure() {
        Subscriber<String> o = OBSERVER_SUCCESS();
        try {
            o.onSubscribe(new Subscription() {

                @Override
                public void cancel() {
                    // break contract by throwing exception
                    throw new SafeObserverTestException("failure from unsubscribe");
                }
                
                @Override
                public void request(long n) {
                    
                }
            });
            new SafeSubscriber<>(o).onComplete();
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            // FIXME no longer assertable
//            assertTrue(o.isUnsubscribed());
            assertTrue(e instanceof UnsubscribeFailedException);
            assertTrue(e.getCause() instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e.getMessage());
            // expected since onError fails so SafeObserver can't help
        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void onErrorSuccessWithUnsubscribeFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<>();
        Subscriber<String> o = OBSERVER_SUCCESS(onError);
        try {
            o.onSubscribe(new Subscription() {

                @Override
                public void cancel() {
                    // break contract by throwing exception
                    throw new SafeObserverTestException("failure from unsubscribe");
                }
                
                @Override
                public void request(long n) {
                    
                }
            });
            new SafeSubscriber<>(o).onError(new SafeObserverTestException("failed"));
            fail("we expect the unsubscribe failure to cause an exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            // FIXME no longer assertable
//            assertTrue(o.isUnsubscribed());

            // we still expect onError to have received something before unsubscribe blew up
            assertNotNull(onError.get());
            assertTrue(onError.get() instanceof SafeObserverTestException);
            assertEquals("failed", onError.get().getMessage());

            // now assert the exception that was thrown
            OnErrorFailedException onErrorFailedException = (OnErrorFailedException) e;
            assertTrue(onErrorFailedException.getCause() instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e.getMessage());
        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void onErrorFailureWithUnsubscribeFailure() {
        Subscriber<String> o = OBSERVER_ONERROR_FAIL();
        try {
            o.onSubscribe(new Subscription() {

                @Override
                public void cancel() {
                    // break contract by throwing exception
                    throw new SafeObserverTestException("failure from unsubscribe");
                }
                
                @Override
                public void request(long n) {
                    
                }
            });
            new SafeSubscriber<>(o).onError(new SafeObserverTestException("onError failure"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            // FIXME no longer assertable
//            assertTrue(o.isUnsubscribed());

            // assertions for what is expected for the actual failure propagated to onError which then fails
            assertTrue(e instanceof RuntimeException);
            assertEquals("Error occurred when trying to propagate error to Observer.onError and during unsubscription.", e.getMessage());

            Throwable e2 = e.getCause();
            assertTrue(e2 instanceof CompositeException);
            List<Throwable> innerExceptions = ((CompositeException) e2).getExceptions();
            assertEquals(3, innerExceptions.size());

            Throwable e3 = innerExceptions.get(0);
            assertTrue(e3 instanceof SafeObserverTestException);
            assertEquals("onError failure", e3.getMessage());

            Throwable e4 = innerExceptions.get(1);
            assertTrue(e4 instanceof SafeObserverTestException);
            assertEquals("onErrorFail", e4.getMessage());

            Throwable e5 = innerExceptions.get(2);
            assertTrue(e5 instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e5.getMessage());
        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void onErrorNotImplementedFailureWithUnsubscribeFailure() {
        Subscriber<String> o = OBSERVER_ONERROR_NOTIMPLEMENTED();
        try {
            o.onSubscribe(new Subscription() {

                @Override
                public void cancel() {
                    // break contract by throwing exception
                    throw new SafeObserverTestException("failure from unsubscribe");
                }
                
                @Override
                public void request(long n) {
                    
                }
            });
            new SafeSubscriber<>(o).onError(new SafeObserverTestException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            // FIXME no longer assertable
//            assertTrue(o.isUnsubscribed());

            // assertions for what is expected for the actual failure propagated to onError which then fails
            assertTrue(e instanceof RuntimeException);
            assertEquals("Observer.onError not implemented and error while unsubscribing.", e.getMessage());

            Throwable e2 = e.getCause();
            assertTrue(e2 instanceof CompositeException);
            List<Throwable> innerExceptions = ((CompositeException) e2).getExceptions();
            assertEquals(2, innerExceptions.size());

            Throwable e3 = innerExceptions.get(0);
            assertTrue(e3 instanceof SafeObserverTestException);
            assertEquals("error!", e3.getMessage());

            Throwable e4 = innerExceptions.get(1);
            assertTrue(e4 instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e4.getMessage());
        }
    }

    private static Subscriber<String> OBSERVER_SUCCESS() {
        return new Observer<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String args) {

            }
        };

    }

    private static Subscriber<String> OBSERVER_SUCCESS(final AtomicReference<Throwable> onError) {
        return new Observer<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                onError.set(e);
            }

            @Override
            public void onNext(String args) {

            }
        };

    }

    private static Subscriber<String> OBSERVER_ONNEXT_FAIL(final AtomicReference<Throwable> onError) {
        return new Observer<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                onError.set(e);
            }

            @Override
            public void onNext(String args) {
                throw new SafeObserverTestException("onNextFail");
            }
        };

    }

    private static Subscriber<String> OBSERVER_ONNEXT_ONERROR_FAIL() {
        return new Observer<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                throw new SafeObserverTestException("onErrorFail");
            }

            @Override
            public void onNext(String args) {
                throw new SafeObserverTestException("onNextFail");
            }

        };
    }

    private static Subscriber<String> OBSERVER_ONERROR_FAIL() {
        return new Observer<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                throw new SafeObserverTestException("onErrorFail");
            }

            @Override
            public void onNext(String args) {

            }

        };
    }

    private static Subscriber<String> OBSERVER_ONERROR_NOTIMPLEMENTED() {
        return new Observer<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public void onNext(String args) {

            }

        };
    }

    private static Subscriber<String> OBSERVER_ONCOMPLETED_FAIL(final AtomicReference<Throwable> onError) {
        return new Observer<String>() {

            @Override
            public void onComplete() {
                throw new SafeObserverTestException("onCompletedFail");
            }

            @Override
            public void onError(Throwable e) {
                onError.set(e);
            }

            @Override
            public void onNext(String args) {

            }

        };
    }

    @SuppressWarnings("serial")
    private static class SafeObserverTestException extends RuntimeException {
        public SafeObserverTestException(String message) {
            super(message);
        }
    }
    
    @Test
    @Ignore("Subscribers can't throw")
    public void testOnCompletedThrows() {
        final AtomicReference<Throwable> error = new AtomicReference<>();
        SafeSubscriber<Integer> s = new SafeSubscriber<>(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
                
            }
            @Override
            public void onError(Throwable e) {
                error.set(e);
            }
            @Override
            public void onComplete() {
                throw new TestException();
            }
        });
        
        try {
            s.onComplete();
            Assert.fail();
        } catch (OnCompleteFailedException e) {
           assertNull(error.get());
        }
    }
    
    @Test
    public void testActual() {
        Subscriber<Integer> actual = new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
            }
            @Override
            public void onError(Throwable e) {
            }
            @Override
            public void onComplete() {
            }
        };
        SafeSubscriber<Integer> s = new SafeSubscriber<>(actual);
        
        assertSame(actual, s.actual());
    }
}