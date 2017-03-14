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

import io.reactivex.Flowable;
import io.reactivex.functions.*;
import io.reactivex.internal.fuseable.*;

/**
 * Utility methods that return functional interfaces to support assertions regarding fusion
 * in a TestSubscriber.
 * <p>Don't move this class as it needs package-private access to TestSubscriber's internals.
 */
public enum SubscriberFusion {
    ;

    /**
     * Returns a function that takes a Flowable and returns a TestSubscriber that
     * is set up according to the parameters and is subscribed to the Flowable.
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(SubscriberFusion.test(0, QueueSubscription.ANY, false))
     * .assertResult(0);
     * </pre>
     * @param <T> the value type
     * @param initialRequest the initial request amount, non-negative
     * @param mode the fusion mode to request, see {@link QueueSubscription} constants.
     * @param cancelled should the TestSubscriber cancelled before the subscription even happens?
     * @return the new Function instance
     */
    public static <T> Function<Flowable<T>, TestSubscriber<T>> test(
            final long initialRequest, final int mode, final boolean cancelled) {
        return new TestFusionCheckFunction<T>(mode, cancelled, initialRequest);
    }
    /**
     * Returns a Consumer that asserts on its TestSubscriber parameter that
     * the upstream is Fuseable (sent a QueueDisposable subclass in onSubscribe).
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueDisposable.ANY, false))
     * .assertOf(ObserverFusion.assertFuseable());
     * </pre>
     * @param <T> the value type
     * @return the new Consumer instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Consumer<TestSubscriber<T>> assertFuseable() {
        return (Consumer)AssertFuseable.INSTANCE;
    }

    static final class AssertFusionConsumer<T> implements Consumer<TestSubscriber<T>> {
        private final int mode;

        AssertFusionConsumer(int mode) {
            this.mode = mode;
        }

        @Override
        public void accept(TestSubscriber<T> ts) throws Exception {
            ts.assertFusionMode(mode);
        }
    }

    static final class TestFusionCheckFunction<T> implements Function<Flowable<T>, TestSubscriber<T>> {
        private final int mode;
        private final boolean cancelled;
        private final long initialRequest;

        TestFusionCheckFunction(int mode, boolean cancelled, long initialRequest) {
            this.mode = mode;
            this.cancelled = cancelled;
            this.initialRequest = initialRequest;
        }

        @Override
        public TestSubscriber<T> apply(Flowable<T> t) throws Exception {
            TestSubscriber<T> ts = new TestSubscriber<T>(initialRequest);
            ts.setInitialFusionMode(mode);
            if (cancelled) {
                ts.cancel();
            }
            t.subscribe(ts);
            return ts;
        }
    }

    enum AssertFuseable implements Consumer<TestSubscriber<Object>> {
        INSTANCE;
        @Override
        public void accept(TestSubscriber<Object> ts) throws Exception {
            ts.assertFuseable();
        }
    }

    /**
     * Returns a Consumer that asserts on its TestSubscriber parameter that
     * the upstream is not Fuseable (didn't sent a QueueDisposable subclass in onSubscribe).
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueDisposable.ANY, false))
     * .assertOf(ObserverFusion.assertNotFuseable());
     * </pre>
     * @param <T> the value type
     * @return the new Consumer instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Consumer<TestSubscriber<T>> assertNotFuseable() {
        return (Consumer)AssertNotFuseable.INSTANCE;
    }

    enum AssertNotFuseable implements Consumer<TestSubscriber<Object>> {
        INSTANCE;
        @Override
        public void accept(TestSubscriber<Object> ts) throws Exception {
            ts.assertNotFuseable();
        }
    }

    /**
     * Returns a Consumer that asserts on its TestSubscriber parameter that
     * the upstream is Fuseable (sent a QueueSubscription subclass in onSubscribe)
     * and the established the given fusion mode.
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(SubscriberFusion.test(0, QueueSubscription.ANY, false))
     * .assertOf(SubscriberFusion.assertFusionMode(QueueSubscription.SYNC));
     * </pre>
     * @param <T> the value type
     * @param mode the expected established fusion mode, see {@link QueueSubscription} constants.
     * @return the new Consumer instance
     */
    public static <T> Consumer<TestSubscriber<T>> assertFusionMode(final int mode) {
        return new AssertFusionConsumer<T>(mode);
    }

    /**
     * Constructs a TestSubscriber with the given initial request and required fusion mode.
     * @param <T> the value type
     * @param initialRequest the initial request, non-negative
     * @param mode the requested fusion mode, see {@link QueueSubscription} constants
     * @return the new TestSubscriber
     */
    public static <T> TestSubscriber<T> newTest(long initialRequest, int mode) {
        TestSubscriber<T> ts = new TestSubscriber<T>(initialRequest);
        ts.setInitialFusionMode(mode);
        return ts;
    }

    /**
     * Constructs a TestSubscriber with the given required fusion mode.
     * @param <T> the value type
     * @param mode the requested fusion mode, see {@link QueueSubscription} constants
     * @return the new TestSubscriber
     */
    public static <T> TestSubscriber<T> newTest(int mode) {
        TestSubscriber<T> ts = new TestSubscriber<T>();
        ts.setInitialFusionMode(mode);
        return ts;
    }

    /**
     * Assert that the TestSubscriber received a fuseabe QueueSubscription and
     * is in the given fusion mode.
     * @param <T> the value type
     * @param ts the TestSubscriber instance
     * @param mode the expected mode
     * @return the TestSubscriber
     */
    public static <T> TestSubscriber<T> assertFusion(TestSubscriber<T> ts, int mode) {
        return ts.assertOf(SubscriberFusion.<T>assertFuseable())
                .assertOf(SubscriberFusion.<T>assertFusionMode(mode));
    }
}
