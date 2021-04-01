/*
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

package io.reactivex.rxjava3.subscribers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class SafeSubscriberTest extends RxJavaTest {

    /**
     * Ensure onNext can not be called after onError.
     */
    @Test
    public void onNextAfterOnError() {
        TestObservable t = new TestObservable();
        Flowable<String> st = Flowable.unsafeCreate(t);

        Subscriber<String> w = TestHelper.mockSubscriber();
        st.subscribe(new SafeSubscriber<>(new TestSubscriber<>(w)));

        t.sendOnNext("one");
        t.sendOnError(new RuntimeException("bad"));
        t.sendOnNext("two");

        verify(w, times(1)).onNext("one");
        verify(w, times(1)).onError(any(Throwable.class));
        verify(w, Mockito.never()).onNext("two");
    }

    /**
     * Ensure onComplete can not be called after onError.
     */
    @Test
    public void onCompletedAfterOnError() {
        TestObservable t = new TestObservable();
        Flowable<String> st = Flowable.unsafeCreate(t);

        Subscriber<String> w = TestHelper.mockSubscriber();

        st.subscribe(new SafeSubscriber<>(new TestSubscriber<>(w)));

        t.sendOnNext("one");
        t.sendOnError(new RuntimeException("bad"));
        t.sendOnCompleted();

        verify(w, times(1)).onNext("one");
        verify(w, times(1)).onError(any(Throwable.class));
        verify(w, Mockito.never()).onComplete();
    }

    /**
     * Ensure onNext can not be called after onComplete.
     */
    @Test
    public void onNextAfterOnCompleted() {
        TestObservable t = new TestObservable();
        Flowable<String> st = Flowable.unsafeCreate(t);

        Subscriber<String> w = TestHelper.mockSubscriber();
        st.subscribe(new SafeSubscriber<>(new TestSubscriber<>(w)));

        t.sendOnNext("one");
        t.sendOnCompleted();
        t.sendOnNext("two");

        verify(w, times(1)).onNext("one");
        verify(w, Mockito.never()).onNext("two");
        verify(w, times(1)).onComplete();
        verify(w, Mockito.never()).onError(any(Throwable.class));
    }

    /**
     * Ensure onError can not be called after onComplete.
     */
    @Test
    @SuppressUndeliverable
    public void onErrorAfterOnCompleted() {
        TestObservable t = new TestObservable();
        Flowable<String> st = Flowable.unsafeCreate(t);

        Subscriber<String> w = TestHelper.mockSubscriber();
        st.subscribe(new SafeSubscriber<>(new TestSubscriber<>(w)));

        t.sendOnNext("one");
        t.sendOnCompleted();
        t.sendOnError(new RuntimeException("bad"));

        verify(w, times(1)).onNext("one");
        verify(w, times(1)).onComplete();
        verify(w, Mockito.never()).onError(any(Throwable.class));
    }

    /**
     * An Observable that doesn't do the right thing on UnSubscribe/Error/etc in that it will keep sending events down the pipe regardless of what happens.
     */
    private static class TestObservable implements Publisher<String> {

        Subscriber<? super String> subscriber;

        /* used to simulate subscription */
        public void sendOnCompleted() {
            subscriber.onComplete();
        }

        /* used to simulate subscription */
        public void sendOnNext(String value) {
            subscriber.onNext(value);
        }

        /* used to simulate subscription */
        public void sendOnError(Throwable e) {
            subscriber.onError(e);
        }

        @Override
        public void subscribe(Subscriber<? super String> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Subscription() {

                @Override
                public void cancel() {
                    // going to do nothing to pretend I'm a bad Observable that keeps allowing events to be sent
                    System.out.println("==> SynchronizeTest unsubscribe that does nothing!");
                }

                @Override
                public void request(long n) {

                }

            });
        }

    }

    @Test
    public void onNextFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<>();
        try {
            OBSERVER_ONNEXT_FAIL(onError).onNext("one");
            fail("expects exception to be thrown");
        } catch (Exception e) {
            assertNull(onError.get());
            assertTrue(e instanceof SafeSubscriberTestException);
            assertEquals("onNextFail", e.getMessage());
        }
    }

    @Test
    public void onNextFailureSafe() {
        AtomicReference<Throwable> onError = new AtomicReference<>();
        try {
            SafeSubscriber<String> safeObserver = new SafeSubscriber<>(OBSERVER_ONNEXT_FAIL(onError));
            safeObserver.onSubscribe(new BooleanSubscription());
            safeObserver.onNext("one");
            assertNotNull(onError.get());
            assertTrue(onError.get() instanceof SafeSubscriberTestException);
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
            assertTrue(e instanceof SafeSubscriberTestException);
            assertEquals("onCompleteFail", e.getMessage());
        }
    }

    @Test
    public void onErrorFailure() {
        try {
            subscriberOnErrorFail().onError(new SafeSubscriberTestException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            assertTrue(e instanceof SafeSubscriberTestException);
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
            assertTrue(e instanceof SafeSubscriberTestException);
            assertEquals("onNextFail", e.getMessage());
        }
    }

    static final Subscription THROWING_DISPOSABLE = new Subscription() {

        @Override
        public void cancel() {
            // break contract by throwing exception
            throw new SafeSubscriberTestException("failure from unsubscribe");
        }

        @Override
        public void request(long n) {
            // ignored
        }
    };

    private static Subscriber<String> OBSERVER_ONNEXT_FAIL(final AtomicReference<Throwable> onError) {
        return new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                onError.set(e);
            }

            @Override
            public void onNext(String args) {
                throw new SafeSubscriberTestException("onNextFail");
            }
        };

    }

    private static Subscriber<String> OBSERVER_ONNEXT_ONERROR_FAIL() {
        return new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                throw new SafeSubscriberTestException("onErrorFail");
            }

            @Override
            public void onNext(String args) {
                throw new SafeSubscriberTestException("onNextFail");
            }

        };
    }

    private static Subscriber<String> subscriberOnErrorFail() {
        return new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                throw new SafeSubscriberTestException("onErrorFail");
            }

            @Override
            public void onNext(String args) {

            }

        };
    }

    private static Subscriber<String> OBSERVER_ONCOMPLETED_FAIL(final AtomicReference<Throwable> onError) {
        return new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {
                throw new SafeSubscriberTestException("onCompleteFail");
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
    private static class SafeSubscriberTestException extends RuntimeException {
        SafeSubscriberTestException(String message) {
            super(message);
        }
    }

    @Test
    public void actual() {
        Subscriber<Integer> actual = new DefaultSubscriber<Integer>() {
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

        assertSame(actual, s.downstream);
    }

    @Test
    public void dispose() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        SafeSubscriber<Integer> so = new SafeSubscriber<>(ts);

        BooleanSubscription bs = new BooleanSubscription();

        so.onSubscribe(bs);

        ts.dispose();

        assertTrue(bs.isCancelled());

//        assertTrue(so.isDisposed());
    }

    @Test
    @SuppressUndeliverable
    public void onNextAfterComplete() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        SafeSubscriber<Integer> so = new SafeSubscriber<>(ts);

        BooleanSubscription bs = new BooleanSubscription();

        so.onSubscribe(bs);

        so.onComplete();

        so.onNext(1);

        so.onError(new TestException());

        so.onComplete();

        ts.assertResult();
    }

    @Test
    public void onNextNull() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        SafeSubscriber<Integer> so = new SafeSubscriber<>(ts);

        BooleanSubscription bs = new BooleanSubscription();

        so.onSubscribe(bs);

        so.onNext(null);

        ts.assertFailure(NullPointerException.class);
    }

    @Test
    public void onNextWithoutOnSubscribe() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        SafeSubscriber<Integer> so = new SafeSubscriber<>(ts);

        so.onNext(1);

        ts.assertFailureAndMessage(NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onErrorWithoutOnSubscribe() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        SafeSubscriber<Integer> so = new SafeSubscriber<>(ts);

        so.onError(new TestException());

        ts.assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class);
        TestHelper.assertError(ts, 1, NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onCompleteWithoutOnSubscribe() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        SafeSubscriber<Integer> so = new SafeSubscriber<>(ts);

        so.onComplete();

        ts.assertFailureAndMessage(NullPointerException.class, "Subscription not set!");
    }

    @Test
    public void onNextNormal() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        SafeSubscriber<Integer> so = new SafeSubscriber<>(ts);

        BooleanSubscription bs = new BooleanSubscription();

        so.onSubscribe(bs);

        so.onNext(1);
        so.onComplete();

        ts.assertResult(1);
    }

    static final class CrashDummy implements FlowableSubscriber<Object>, Subscription {
        final boolean crashOnSubscribe;

        int crashOnNext;

        final boolean crashOnError;

        final boolean crashOnComplete;

        final boolean crashDispose;

        final boolean crashRequest;

        Throwable error;

        CrashDummy(boolean crashOnSubscribe, int crashOnNext,
                boolean crashOnError, boolean crashOnComplete, boolean crashDispose, boolean crashRequest) {
            this.crashOnSubscribe = crashOnSubscribe;
            this.crashOnNext = crashOnNext;
            this.crashOnError = crashOnError;
            this.crashOnComplete = crashOnComplete;
            this.crashDispose = crashDispose;
            this.crashRequest = crashRequest;
        }

        @Override
        public void cancel() {
            if (crashDispose) {
                throw new TestException("cancel()");
            }
        }

        @Override
        public void request(long n) {
            if (crashRequest) {
                throw new TestException("request()");
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
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

        public SafeSubscriber<Object> toSafe() {
            return new SafeSubscriber<>(this);
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
            CrashDummy cd = new CrashDummy(false, 1, true, false, false, false);
            SafeSubscriber<Object> so = cd.toSafe();
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
        CrashDummy cd = new CrashDummy(false, 1, false, false, true, false);
        SafeSubscriber<Object> so = cd.toSafe();
        so.onSubscribe(cd);

        so.onNext(1);

        cd.assertError(CompositeException.class);
        cd.assertInnerError(0, TestException.class, "onNext(1)");
        cd.assertInnerError(1, TestException.class, "cancel()");
    }

    @Test
    public void onSubscribeTwice() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, false, false, false, false);
            SafeSubscriber<Object> so = cd.toSafe();
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
            CrashDummy cd = new CrashDummy(true, 1, false, false, false, false);
            SafeSubscriber<Object> so = cd.toSafe();
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
            CrashDummy cd = new CrashDummy(true, 1, false, false, true, false);
            SafeSubscriber<Object> so = cd.toSafe();
            so.onSubscribe(cd);

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, TestException.class, "onSubscribe()");
            TestHelper.assertError(ce, 1, TestException.class, "cancel()");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextOnSubscribeCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(true, 1, false, false, false, false);
            SafeSubscriber<Object> so = cd.toSafe();

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
        CrashDummy cd = new CrashDummy(false, 1, false, false, true, false);
        SafeSubscriber<Object> so = cd.toSafe();
        so.onSubscribe(cd);

        so.onNext(null);

        cd.assertInnerError(0, NullPointerException.class);
        cd.assertInnerError(1, TestException.class, "cancel()");
    }

    @Test
    public void noSubscribeOnErrorCrashes() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, true, false, false, false);
            SafeSubscriber<Object> so = cd.toSafe();

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
        CrashDummy cd = new CrashDummy(false, 1, false, false, false, false);
        SafeSubscriber<Object> so = cd.toSafe();
        so.onSubscribe(cd);

        so.onError(null);

        cd.assertError(NullPointerException.class);
    }

    @Test
    public void onErrorNoSubscribeCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(true, 1, false, false, false, false);
            SafeSubscriber<Object> so = cd.toSafe();

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
            CrashDummy cd = new CrashDummy(false, 1, true, false, false, false);
            SafeSubscriber<Object> so = cd.toSafe();

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
            CrashDummy cd = new CrashDummy(false, 1, false, true, false, false);
            SafeSubscriber<Object> so = cd.toSafe();

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
            CrashDummy cd = new CrashDummy(true, 1, false, true, false, false);
            SafeSubscriber<Object> so = cd.toSafe();

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
            CrashDummy cd = new CrashDummy(false, 1, true, true, false, false);
            SafeSubscriber<Object> so = cd.toSafe();

            so.onComplete();

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(ce, 0, NullPointerException.class, "Subscription not set!");
            TestHelper.assertError(ce, 1, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void requestCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, false, false, false, true);
            SafeSubscriber<Object> so = cd.toSafe();
            so.onSubscribe(cd);

            so.request(1);

            TestHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, false, false, true, false);
            SafeSubscriber<Object> so = cd.toSafe();
            so.onSubscribe(cd);

            so.cancel();

            TestHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void requestCancelCrash() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            CrashDummy cd = new CrashDummy(false, 1, false, false, true, true);
            SafeSubscriber<Object> so = cd.toSafe();
            so.onSubscribe(cd);

            so.request(1);

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> ce = TestHelper.compositeList(list.get(0));
           TestHelper.assertError(ce, 0, TestException.class, "request()");
            TestHelper.assertError(ce, 1, TestException.class, "cancel()");
        } finally {
            RxJavaPlugins.reset();
        }
    }

}
