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

package io.reactivex.observers;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.plugins.RxJavaPlugins;

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
            SafeObserver<String> safeObserver = new SafeObserver<String>(OBSERVER_ONNEXT_FAIL(onError));
            safeObserver.onSubscribe(Disposables.empty());
            safeObserver.onNext("one");
            assertNotNull(onError.get());
            assertTrue(onError.get() instanceof SafeObserverTestException);
            assertEquals("onNextFail", onError.get().getMessage());
        } catch (Exception e) {
            fail("expects exception to be passed to onError");
        }
    }

    @Test
    public void onCompleteFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        try {
            OBSERVER_ONCOMPLETED_FAIL(onError).onComplete();
            fail("expects exception to be thrown");
        } catch (Exception e) {
            assertNull(onError.get());
            assertTrue(e instanceof SafeObserverTestException);
            assertEquals("onCompleteFail", e.getMessage());
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
    @Ignore("Observers can't throw")
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
    @Ignore("Observers can't throw")
    public void onErrorNotImplementedFailureSafe() {
        try {
            new SafeObserver<String>(OBSERVER_ONERROR_NOTIMPLEMENTED()).onError(new SafeObserverTestException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
//            assertTrue(e instanceof OnErrorNotImplementedException);
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
    @Ignore("Observers can't throw")
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

    static final Disposable THROWING_DISPOSABLE = new Disposable() {

        @Override
        public boolean isDisposed() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void dispose() {
            // break contract by throwing exception
            throw new SafeObserverTestException("failure from unsubscribe");
        }
    };

    @Test
    @Ignore("Observers can't throw")
    public void onCompleteSuccessWithUnsubscribeFailure() {
        Observer<String> o = OBSERVER_SUCCESS();
        try {
            o.onSubscribe(THROWING_DISPOSABLE);
            new SafeObserver<String>(o).onComplete();
            fail("expects exception to be thrown");
        } catch (Exception e) {
            e.printStackTrace();

            // FIXME no longer assertable
//            assertTrue(o.isUnsubscribed());
//            assertTrue(e instanceof UnsubscribeFailedException);
            assertTrue(e.getCause() instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e.getMessage());
            // expected since onError fails so SafeObserver can't help
        }
    }

    @Test
    @Ignore("Observers can't throw")
    public void onErrorSuccessWithUnsubscribeFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        Observer<String> o = OBSERVER_SUCCESS(onError);
        try {
            o.onSubscribe(THROWING_DISPOSABLE);
            new SafeObserver<String>(o).onError(new SafeObserverTestException("failed"));
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
            RuntimeException onErrorFailedException = (RuntimeException) e;
            assertTrue(onErrorFailedException.getCause() instanceof SafeObserverTestException);
            assertEquals("failure from unsubscribe", e.getMessage());
        }
    }

    @Test
    @Ignore("Observers can't throw")
    public void onErrorFailureWithUnsubscribeFailure() {
        Observer<String> o = OBSERVER_ONERROR_FAIL();
        try {
            o.onSubscribe(THROWING_DISPOSABLE);
            new SafeObserver<String>(o).onError(new SafeObserverTestException("onError failure"));
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
    @Ignore("Observers can't throw")
    public void onErrorNotImplementedFailureWithUnsubscribeFailure() {
        Observer<String> o = OBSERVER_ONERROR_NOTIMPLEMENTED();
        try {
            o.onSubscribe(THROWING_DISPOSABLE);
            new SafeObserver<String>(o).onError(new SafeObserverTestException("error!"));
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

    private static Observer<String> OBSERVER_SUCCESS() {
        return new DefaultObserver<String>() {

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

    private static Observer<String> OBSERVER_SUCCESS(final AtomicReference<Throwable> onError) {
        return new DefaultObserver<String>() {

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

    private static Observer<String> OBSERVER_ONNEXT_FAIL(final AtomicReference<Throwable> onError) {
        return new DefaultObserver<String>() {

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

    private static Observer<String> OBSERVER_ONNEXT_ONERROR_FAIL() {
        return new DefaultObserver<String>() {

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

    private static Observer<String> OBSERVER_ONERROR_FAIL() {
        return new DefaultObserver<String>() {

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

    private static Observer<String> OBSERVER_ONERROR_NOTIMPLEMENTED() {
        return new DefaultObserver<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
//                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public void onNext(String args) {

            }

        };
    }

    private static Observer<String> OBSERVER_ONCOMPLETED_FAIL(final AtomicReference<Throwable> onError) {
        return new DefaultObserver<String>() {

            @Override
            public void onComplete() {
                throw new SafeObserverTestException("onCompleteFail");
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
    static class SafeObserverTestException extends RuntimeException {
        SafeObserverTestException(String message) {
            super(message);
        }
    }

    @Test
    @Ignore("Observers can't throw")
    public void testOnCompletedThrows() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        SafeObserver<Integer> s = new SafeObserver<Integer>(new DefaultObserver<Integer>() {
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
        } catch (RuntimeException e) {
           assertNull(error.get());
        }
    }

    @Test
    public void testActual() {
        Observer<Integer> actual = new DefaultObserver<Integer>() {
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
        SafeObserver<Integer> s = new SafeObserver<Integer>(actual);

        assertSame(actual, s.actual);
    }

    @Test
    public void dispose() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        SafeObserver<Integer> so = new SafeObserver<Integer>(ts);

        Disposable d = Disposables.empty();

        so.onSubscribe(d);

        ts.dispose();

        assertTrue(d.isDisposed());

        assertTrue(so.isDisposed());
    }

    @Test
    public void onNextAfterComplete() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        SafeObserver<Integer> so = new SafeObserver<Integer>(ts);

        Disposable d = Disposables.empty();

        so.onSubscribe(d);

        so.onComplete();

        so.onNext(1);

        so.onError(new TestException());

        so.onComplete();

        ts.assertResult();
    }

    @Test
    public void onNextNull() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        SafeObserver<Integer> so = new SafeObserver<Integer>(ts);

        Disposable d = Disposables.empty();

        so.onSubscribe(d);

        so.onNext(null);

        ts.assertFailure(NullPointerException.class);
    }

    @Test
    public void onNextWithoutOnSubscribe() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        SafeObserver<Integer> so = new SafeObserver<Integer>(ts);

        so.onNext(1);

        ts.assertFailureAndMessage(NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onErrorWithoutOnSubscribe() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        SafeObserver<Integer> so = new SafeObserver<Integer>(ts);

        so.onError(new TestException());

        ts.assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class);
        TestHelper.assertError(ts, 1, NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onCompleteWithoutOnSubscribe() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        SafeObserver<Integer> so = new SafeObserver<Integer>(ts);

        so.onComplete();

        ts.assertFailureAndMessage(NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onNextNormal() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        SafeObserver<Integer> so = new SafeObserver<Integer>(ts);

        Disposable d = Disposables.empty();

        so.onSubscribe(d);

        so.onNext(1);
        so.onComplete();

        ts.assertResult(1);
    }

    static final class CrashDummy implements Observer<Object>, Disposable {
        boolean crashOnSubscribe;
        int crashOnNext;
        boolean crashOnError;
        boolean crashOnComplete;

        boolean crashDispose;

        Throwable error;

        CrashDummy(boolean crashOnSubscribe, int crashOnNext,
                boolean crashOnError, boolean crashOnComplete, boolean crashDispose) {
            this.crashOnSubscribe = crashOnSubscribe;
            this.crashOnNext = crashOnNext;
            this.crashOnError = crashOnError;
            this.crashOnComplete = crashOnComplete;
            this.crashDispose = crashDispose;
        }

        @Override
        public void dispose() {
            if (crashDispose) {
                throw new TestException("dispose()");
            }
        }

        @Override
        public boolean isDisposed() {
            return false;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (crashOnSubscribe) {
                throw new TestException("onSubscribe()");
            }
        }

        @Override
        public void onNext(Object value) {
            if (--crashOnNext == 0) {
                throw new TestException("onNext(" + value + ")");
            }
        }

        @Override
        public void onError(Throwable e) {
            if (crashOnError) {
                throw new TestException("onError(" + e + ")");
            }
            error = e;
        }

        @Override
        public void onComplete() {
            if (crashOnComplete) {
                throw new TestException("onComplete()");
            }
        }

        public SafeObserver<Object> toSafe() {
            return new SafeObserver<Object>(this);
        }

        public CrashDummy assertError(Class<? extends Throwable> clazz) {
            if (!clazz.isInstance(error)) {
                throw new AssertionError("Different error: " + error);
            }
            return this;
        }

        public CrashDummy assertInnerError(int index, Class<? extends Throwable> clazz) {
            List<Throwable> cel = TestHelper.compositeList(error);
            TestHelper.assertError(cel, index, clazz);
            return this;
        }

        public CrashDummy assertInnerError(int index, Class<? extends Throwable> clazz, String message) {
            List<Throwable> cel = TestHelper.compositeList(error);
            TestHelper.assertError(cel, index, clazz, message);
            return this;
        }

    }

    @Test
    public void onNextOnErrorCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, true, false, false);
            SafeObserver<Object> so = cd.toSafe();
            so.onSubscribe(cd);

            so.onNext(1);

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, TestException.class, "onNext(1)");
            TestHelper.assertError(ce, 1, TestException.class, "onError(io.reactivex.exceptions.TestException: onNext(1))");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextDisposeCrash() {
        CrashDummy cd = new CrashDummy(false, 1, false, false, true);
        SafeObserver<Object> so = cd.toSafe();
        so.onSubscribe(cd);

        so.onNext(1);

        cd.assertError(CompositeException.class);
        cd.assertInnerError(0, TestException.class, "onNext(1)");
        cd.assertInnerError(1, TestException.class, "dispose()");
    }

    @Test
    public void onSubscribeTwice() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, false, false, false);
            SafeObserver<Object> so = cd.toSafe();
            so.onSubscribe(cd);
            so.onSubscribe(cd);

            TestHelper.assertError(list, 0, IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onSubscribeCrashes() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(true, 1, false, false, false);
            SafeObserver<Object> so = cd.toSafe();
            so.onSubscribe(cd);

            TestHelper.assertUndeliverable(list, 0, TestException.class, "onSubscribe()");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onSubscribeAndDisposeCrashes() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(true, 1, false, false, true);
            SafeObserver<Object> so = cd.toSafe();
            so.onSubscribe(cd);

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, TestException.class, "onSubscribe()");
            TestHelper.assertError(ce, 1, TestException.class, "dispose()");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextOnSubscribeCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(true, 1, false, false, false);
            SafeObserver<Object> so = cd.toSafe();

            so.onNext(1);

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, NullPointerException.class, "Subscription not set!");
            TestHelper.assertError(ce, 1, TestException.class, "onSubscribe()");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextNullDisposeCrashes() {
        CrashDummy cd = new CrashDummy(false, 1, false, false, true);
        SafeObserver<Object> so = cd.toSafe();
        so.onSubscribe(cd);

        so.onNext(null);

        cd.assertInnerError(0, NullPointerException.class);
        cd.assertInnerError(1, TestException.class, "dispose()");
    }

    @Test
    public void noSubscribeOnErrorCrashes() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, true, false, false);
            SafeObserver<Object> so = cd.toSafe();

            so.onNext(1);

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, NullPointerException.class, "Subscription not set!");
            TestHelper.assertError(ce, 1, TestException.class, "onError(java.lang.NullPointerException: Subscription not set!)");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorNull() {
        CrashDummy cd = new CrashDummy(false, 1, false, false, false);
        SafeObserver<Object> so = cd.toSafe();
        so.onSubscribe(cd);

        so.onError(null);

        cd.assertError(NullPointerException.class);
    }

    @Test
    public void onErrorNoSubscribeCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(true, 1, false, false, false);
            SafeObserver<Object> so = cd.toSafe();

            so.onError(new TestException());

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, TestException.class);
            TestHelper.assertError(ce, 1, NullPointerException.class, "Subscription not set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorNoSubscribeOnErrorCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, true, false, false);
            SafeObserver<Object> so = cd.toSafe();

            so.onError(new TestException());

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, TestException.class);
            TestHelper.assertError(ce, 1, NullPointerException.class, "Subscription not set!");
            TestHelper.assertError(ce, 2, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteteCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, false, true, false);
            SafeObserver<Object> so = cd.toSafe();

            so.onSubscribe(cd);

            so.onComplete();

            TestHelper.assertUndeliverable(list, 0, TestException.class, "onComplete()");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteteNoSubscribeCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(true, 1, false, true, false);
            SafeObserver<Object> so = cd.toSafe();

            so.onComplete();

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, NullPointerException.class, "Subscription not set!");
            TestHelper.assertError(ce, 1, TestException.class, "onSubscribe()");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteteNoSubscribeOnErrorCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, true, true, false);
            SafeObserver<Object> so = cd.toSafe();

            so.onComplete();

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, NullPointerException.class, "Subscription not set!");
            TestHelper.assertError(ce, 1, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

}
