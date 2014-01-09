/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observer;
import rx.subscriptions.Subscriptions;
import rx.util.CompositeException;
import rx.util.OnErrorNotImplementedException;
import rx.util.functions.Action0;

public class SafeObserverTest {

    @Test
    public void onNextFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
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
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        try {
            new SafeObserver<String>(OBSERVER_ONNEXT_FAIL(onError)).onNext("one");
            assertNotNull(onError.get());
            assertTrue(onError.get() instanceof SafeObserverTestException);
            assertEquals("onNextFail", onError.get().getMessage());
        } catch (Exception e) {
            fail("expects exception to be passed to onError");
        }
    }

    @Test
    public void onCompletedFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        try {
            OBSERVER_ONCOMPLETED_FAIL(onError).onCompleted();
            fail("expects exception to be thrown");
        } catch (Exception e) {
            assertNull(onError.get());
            assertTrue(e instanceof SafeObserverTestException);
            assertEquals("onCompletedFail", e.getMessage());
        }
    }

    @Test
    public void onCompletedFailureSafe() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        try {
            new SafeObserver<String>(OBSERVER_ONCOMPLETED_FAIL(onError)).onCompleted();
            assertNotNull(onError.get());
            assertTrue(onError.get() instanceof SafeObserverTestException);
            assertEquals("onCompletedFail", onError.get().getMessage());
        } catch (Exception e) {
            fail("expects exception to be passed to onError");
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
    public void onErrorFailureSafe() {
        try {
            new SafeObserver<String>(OBSERVER_ONERROR_FAIL()).onError(new SafeObserverTestException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e instanceof RuntimeException);
            assertEquals("Error occurred when trying to propagate error to Observer.onError", e.getMessage());

            Throwable e2 = e.getCause();
            assertTrue(e2 instanceof CompositeException);
            assertEquals("Chain of Causes for CompositeException In Order Received =>", e2.getCause().getMessage());

            Throwable e3 = e2.getCause();
            assertTrue(e3.getCause() instanceof SafeObserverTestException);
            assertEquals("error!", e3.getCause().getMessage());

            Throwable e4 = e3.getCause();
            assertTrue(e4.getCause() instanceof SafeObserverTestException);
            assertEquals("onErrorFail", e4.getCause().getMessage());
        }
    }

    @Test
    public void onErrorNotImplementedFailureSafe() {
        try {
            new SafeObserver<String>(OBSERVER_ONERROR_NOTIMPLEMENTED()).onError(new SafeObserverTestException("error!"));
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
    public void onNextOnErrorFailureSafe() {
        try {
            new SafeObserver<String>(OBSERVER_ONNEXT_ONERROR_FAIL()).onNext("one");
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e instanceof RuntimeException);
            assertEquals("Error occurred when trying to propagate error to Observer.onError", e.getMessage());

            Throwable e2 = e.getCause();
            assertTrue(e2 instanceof CompositeException);
            assertEquals("Chain of Causes for CompositeException In Order Received =>", e2.getCause().getMessage());

            Throwable e3 = e2.getCause();
            assertTrue(e3.getCause() instanceof SafeObserverTestException);
            assertEquals("onNextFail", e3.getCause().getMessage());

            Throwable e4 = e3.getCause();
            assertTrue(e4.getCause() instanceof SafeObserverTestException);
            assertEquals("onErrorFail", e4.getCause().getMessage());
        }
    }

    @Test
    public void onCompleteSuccessWithUnsubscribeFailure() {
        SafeObservableSubscription s = null;
        try {
            s = new SafeObservableSubscription(Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    // break contract by throwing exception
                    throw new SafeObserverTestException("failure from unsubscribe");
                }
            }));
            new SafeObserver<String>(s, OBSERVER_SUCCESS()).onCompleted();
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            assertTrue(s.isUnsubscribed());

            assertTrue(e instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e.getMessage());
            // expected since onError fails so SafeObserver can't help
        }
    }

    @Test
    public void onErrorSuccessWithUnsubscribeFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        SafeObservableSubscription s = null;
        try {
            s = new SafeObservableSubscription(Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    // break contract by throwing exception
                    throw new SafeObserverTestException("failure from unsubscribe");
                }
            }));
            new SafeObserver<String>(s, OBSERVER_SUCCESS(onError)).onError(new SafeObserverTestException("failed"));
            fail("we expect the unsubscribe failure to cause an exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            assertTrue(s.isUnsubscribed());

            // we still expect onError to have received something before unsubscribe blew up
            assertNotNull(onError.get());
            assertTrue(onError.get() instanceof SafeObserverTestException);
            assertEquals("failed", onError.get().getMessage());

            // now assert the exception that was thrown
            assertTrue(e instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e.getMessage());
        }
    }

    @Test
    public void onErrorFailureWithUnsubscribeFailure() {
        SafeObservableSubscription s = null;
        try {
            s = new SafeObservableSubscription(Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    // break contract by throwing exception
                    throw new SafeObserverTestException("failure from unsubscribe");
                }
            }));
            new SafeObserver<String>(s, OBSERVER_ONERROR_FAIL()).onError(new SafeObserverTestException("onError failure"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            assertTrue(s.isUnsubscribed());

            // assertions for what is expected for the actual failure propagated to onError which then fails
            assertTrue(e instanceof RuntimeException);
            assertEquals("Error occurred when trying to propagate error to Observer.onError and during unsubscription.", e.getMessage());

            Throwable e2 = e.getCause();
            assertTrue(e2 instanceof CompositeException);
            assertEquals("Chain of Causes for CompositeException In Order Received =>", e2.getCause().getMessage());

            Throwable e3 = e2.getCause();
            assertTrue(e3.getCause() instanceof SafeObserverTestException);
            assertEquals("onError failure", e3.getCause().getMessage());

            Throwable e4 = e3.getCause();
            assertTrue(e4.getCause() instanceof SafeObserverTestException);
            assertEquals("onErrorFail", e4.getCause().getMessage());

            Throwable e5 = e4.getCause();
            assertTrue(e5.getCause() instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e5.getCause().getMessage());
        }
    }

    @Test
    public void onErrorNotImplementedFailureWithUnsubscribeFailure() {
        SafeObservableSubscription s = null;
        try {
            s = new SafeObservableSubscription(Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    // break contract by throwing exception
                    throw new SafeObserverTestException("failure from unsubscribe");
                }
            }));
            new SafeObserver<String>(s, OBSERVER_ONERROR_NOTIMPLEMENTED()).onError(new SafeObserverTestException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            assertTrue(s.isUnsubscribed());

            // assertions for what is expected for the actual failure propagated to onError which then fails
            assertTrue(e instanceof RuntimeException);
            assertEquals("Observer.onError not implemented and error while unsubscribing.", e.getMessage());

            Throwable e2 = e.getCause();
            assertTrue(e2 instanceof CompositeException);
            assertEquals("Chain of Causes for CompositeException In Order Received =>", e2.getCause().getMessage());

            Throwable e3 = e2.getCause();
            assertTrue(e3.getCause() instanceof SafeObserverTestException);
            assertEquals("error!", e3.getCause().getMessage());

            Throwable e4 = e3.getCause();
            assertTrue(e4.getCause() instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e4.getCause().getMessage());
        }
    }

    private static Observer<String> OBSERVER_SUCCESS() {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String args) {

            }
        };

    }

    private static Observer<String> OBSERVER_SUCCESS(final AtomicReference<Throwable> onError) {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

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

    private static Observer<String> OBSERVER_ONNEXT_FAIL(final AtomicReference<Throwable> onError) {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

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

    private static Observer<String> OBSERVER_ONNEXT_ONERROR_FAIL() {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

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

    private static Observer<String> OBSERVER_ONERROR_FAIL() {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

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

    private static Observer<String> OBSERVER_ONERROR_NOTIMPLEMENTED() {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

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

    private static Observer<String> OBSERVER_ONCOMPLETED_FAIL(final AtomicReference<Throwable> onError) {
        return new Observer<String>() {

            @Override
            public void onCompleted() {
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
}
