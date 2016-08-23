/**
 * Copyright 2016 Netflix, Inc.
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

import io.reactivex.Observable;
import io.reactivex.functions.*;
import io.reactivex.internal.fuseable.*;

/**
 * Utility methods that return functional interfaces to support assertions regarding fusion
 * in a TestObserver.
 * <p>Don't move this class as it needs package-private access to TestObserver's internals.
 */
public enum ObserverFusion {
    ;
    
    /**
     * Returns a function that takes a Flowable and returns a TestObserver that
     * is set up according to the parameters and is subscribed to the Flowable.
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueDisposable.ANY, false))
     * .assertResult(0);
     * </pre>
     * @param <T> the value type
     * @param mode the fusion mode to request, see {@link QueueDisposable} constants.
     * @param cancelled should the TestObserver cancelled before the subscription even happens?
     * @return the new Function instance
     */
    public static <T> Function<Observable<T>, TestObserver<T>> test(
            final int mode, final boolean cancelled) {
        return new Function<Observable<T>, TestObserver<T>>() {
            @Override
            public TestObserver<T> apply(Observable<T> t) throws Exception {
                TestObserver<T> ts = new TestObserver<T>();
                ts.setInitialFusionMode(mode);
                if (cancelled) {
                    ts.cancel();
                }
                t.subscribe(ts);
                return ts;
            }
        };
    }
    
    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
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
    public static <T> Consumer<TestObserver<T>> assertFuseable() {
        return (Consumer)AssertFuseable.INSTANCE;
    }
    
    enum AssertFuseable implements Consumer<TestObserver<Object>> {
        INSTANCE;
        @Override
        public void accept(TestObserver<Object> ts) throws Exception {
            ts.assertFuseable();
        }
    }

    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
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
    public static <T> Consumer<TestObserver<T>> assertNotFuseable() {
        return (Consumer)AssertNotFuseable.INSTANCE;
    }

    enum AssertNotFuseable implements Consumer<TestObserver<Object>> {
        INSTANCE;
        @Override
        public void accept(TestObserver<Object> ts) throws Exception {
            ts.assertNotFuseable();
        }
    }

    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
     * the upstream is Fuseable (sent a QueueDisposable subclass in onSubscribe)
     * and the established the given fusion mode.
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueDisposable.ANY, false))
     * .assertOf(ObserverFusion.assertFusionMode(QueueDisposable.SYNC));
     * </pre>
     * @param <T> the value type
     * @param mode the expected established fusion mode, see {@link QueueDisposable} constants.
     * @return the new Consumer instance
     */
    public static <T> Consumer<TestObserver<T>> assertFusionMode(final int mode) {
        return new Consumer<TestObserver<T>>() {
            @Override
            public void accept(TestObserver<T> ts) throws Exception {
                ts.assertFusionMode(mode);
            }
        };
    }


    /**
     * Constructs a TestObserver with the given required fusion mode.
     * @param <T> the value type
     * @param mode the requested fusion mode, see {@link QueueSubscription} constants
     * @return the new TestSubscriber
     */
    public static <T> TestObserver<T> newTest(int mode) {
        TestObserver<T> ts = new TestObserver<T>();
        ts.setInitialFusionMode(mode);
        return ts;
    }}
