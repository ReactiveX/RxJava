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

package io.reactivex.rxjava3.observers;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class SafeObserverTest extends RxJavaTest {

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
            SafeObserver<String> safeObserver = new SafeObserver<>(OBSERVER_ONNEXT_FAIL(onError));
            safeObserver.onSubscribe(Disposable.empty());
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
        AtomicReference<Throwable> onError = new AtomicReference<>();
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
    public void actual() {
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
        SafeObserver<Integer> observer = new SafeObserver<>(actual);

        assertSame(actual, observer.downstream);
    }

    @Test
    public void dispose() {
        TestObserver<Integer> to = new TestObserver<>();

        SafeObserver<Integer> so = new SafeObserver<>(to);

        Disposable d = Disposable.empty();

        so.onSubscribe(d);

        to.dispose();

        assertTrue(d.isDisposed());

        assertTrue(so.isDisposed());
    }

    @Test
    public void onNextAfterComplete() {
        TestObserver<Integer> to = new TestObserver<>();

        SafeObserver<Integer> so = new SafeObserver<>(to);

        Disposable d = Disposable.empty();

        so.onSubscribe(d);

        so.onComplete();

        so.onNext(1);

        so.onError(new TestException());

        so.onComplete();

        to.assertResult();
    }

    @Test
    public void onNextNull() {
        TestObserver<Integer> to = new TestObserver<>();

        SafeObserver<Integer> so = new SafeObserver<>(to);

        Disposable d = Disposable.empty();

        so.onSubscribe(d);

        so.onNext(null);

        to.assertFailure(NullPointerException.class);
    }

    @Test
    public void onNextWithoutOnSubscribe() {
        TestObserverEx<Integer> to = new TestObserverEx<>();

        SafeObserver<Integer> so = new SafeObserver<>(to);

        so.onNext(1);

        to.assertFailureAndMessage(NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onErrorWithoutOnSubscribe() {
        TestObserverEx<Integer> to = new TestObserverEx<>();

        SafeObserver<Integer> so = new SafeObserver<>(to);

        so.onError(new TestException());

        to.assertFailure(CompositeException.class);

        TestHelper.assertError(to, 0, TestException.class);
        TestHelper.assertError(to, 1, NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onCompleteWithoutOnSubscribe() {
        TestObserverEx<Integer> to = new TestObserverEx<>();

        SafeObserver<Integer> so = new SafeObserver<>(to);

        so.onComplete();

        to.assertFailureAndMessage(NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onNextNormal() {
        TestObserver<Integer> to = new TestObserver<>();

        SafeObserver<Integer> so = new SafeObserver<>(to);

        Disposable d = Disposable.empty();

        so.onSubscribe(d);

        so.onNext(1);
        so.onComplete();

        to.assertResult(1);
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
            return new SafeObserver<>(this);
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
            TestHelper.assertError(ce, 1, TestException.class, "onError(io.reactivex.rxjava3.exceptions.TestException: onNext(1))");
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
