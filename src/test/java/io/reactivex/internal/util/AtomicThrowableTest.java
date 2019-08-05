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

package io.reactivex.internal.util;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;
import io.reactivex.testsupport.TestHelper;

public class AtomicThrowableTest extends RxJavaTest {

    @Test
    public void isTerminated() {
        AtomicThrowable ex = new AtomicThrowable();

        assertFalse(ex.isTerminated());

        assertNull(ex.terminate());

        assertTrue(ex.isTerminated());
    }

    @Test
    public void tryTerminateAndReportNull() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            AtomicThrowable ex = new AtomicThrowable();
            ex.tryTerminateAndReport();

            assertTrue("" + errors, errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void tryTerminateAndReportAlreadyTerminated() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            AtomicThrowable ex = new AtomicThrowable();
            ex.terminate();

            ex.tryTerminateAndReport();

            assertTrue("" + errors, errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void tryTerminateAndReportHasError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            AtomicThrowable ex = new AtomicThrowable();
            ex.set(new TestException());

            ex.tryTerminateAndReport();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);

            assertEquals(1, errors.size());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void tryTerminateConsumerSubscriberNoError() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        ts.onSubscribe(new BooleanSubscription());

        AtomicThrowable ex = new AtomicThrowable();
        ex.tryTerminateConsumer(ts);
        ts.assertResult();
    }

    @Test
    public void tryTerminateConsumerSubscriberError() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        ts.onSubscribe(new BooleanSubscription());

        AtomicThrowable ex = new AtomicThrowable();
        ex.set(new TestException());
        ex.tryTerminateConsumer(ts);
        ts.assertFailure(TestException.class);
    }

    @Test
    public void tryTerminateConsumerSubscriberTerminated() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        ts.onSubscribe(new BooleanSubscription());

        AtomicThrowable ex = new AtomicThrowable();
        ex.terminate();
        ex.tryTerminateConsumer(ts);
        ts.assertEmpty();
    }

    @Test
    public void tryTerminateConsumerObserverNoError() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.tryTerminateConsumer((Observer<Object>)to);
        to.assertResult();
    }

    @Test
    public void tryTerminateConsumerObserverError() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.set(new TestException());
        ex.tryTerminateConsumer((Observer<Object>)to);
        to.assertFailure(TestException.class);
    }

    @Test
    public void tryTerminateConsumerObserverTerminated() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.terminate();
        ex.tryTerminateConsumer((Observer<Object>)to);
        to.assertEmpty();
    }

    @Test
    public void tryTerminateConsumerMaybeObserverNoError() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.tryTerminateConsumer((MaybeObserver<Object>)to);
        to.assertResult();
    }

    @Test
    public void tryTerminateConsumerMaybeObserverError() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.set(new TestException());
        ex.tryTerminateConsumer((MaybeObserver<Object>)to);
        to.assertFailure(TestException.class);
    }

    @Test
    public void tryTerminateConsumerMaybeObserverTerminated() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.terminate();
        ex.tryTerminateConsumer((MaybeObserver<Object>)to);
        to.assertEmpty();
    }

    @Test
    public void tryTerminateConsumerSingleNoError() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.tryTerminateConsumer((SingleObserver<Object>)to);
        to.assertEmpty();
    }

    @Test
    public void tryTerminateConsumerSingleError() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.set(new TestException());
        ex.tryTerminateConsumer((SingleObserver<Object>)to);
        to.assertFailure(TestException.class);
    }

    @Test
    public void tryTerminateConsumerSingleTerminated() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.terminate();
        ex.tryTerminateConsumer((SingleObserver<Object>)to);
        to.assertEmpty();
    }

    @Test
    public void tryTerminateConsumerCompletableObserverNoError() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.tryTerminateConsumer((CompletableObserver)to);
        to.assertResult();
    }

    @Test
    public void tryTerminateConsumerCompletableObserverError() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.set(new TestException());
        ex.tryTerminateConsumer((CompletableObserver)to);
        to.assertFailure(TestException.class);
    }

    @Test
    public void tryTerminateConsumerCompletableObserverTerminated() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onSubscribe(Disposables.empty());

        AtomicThrowable ex = new AtomicThrowable();
        ex.terminate();
        ex.tryTerminateConsumer((CompletableObserver)to);
        to.assertEmpty();
    }
}
