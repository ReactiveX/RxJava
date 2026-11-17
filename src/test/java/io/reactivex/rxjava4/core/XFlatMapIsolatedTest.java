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

package io.reactivex.rxjava4.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Flow.Publisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class XFlatMapIsolatedTest extends RxJavaTest {

    static final int SLEEP_AFTER_CANCEL = 500;

    final CyclicBarrier cb = new CyclicBarrier(2);

    void sleep() throws Exception {
        cb.await();
        try {
            long before = System.currentTimeMillis();
            Thread.sleep(5000);
            throw new IllegalStateException("Was not interrupted in time?! " + (System.currentTimeMillis() - before));
        } catch (InterruptedException ex) {
            // ignored here
        }
    }

    void beforeCancelSleep(TestSubscriber<?> ts) throws Exception {
        long before = System.currentTimeMillis();
        Thread.sleep(50);
        if (System.currentTimeMillis() - before > 100) {
            ts.cancel();
            throw new IllegalStateException("Overslept?" + (System.currentTimeMillis() - before));
        }
    }

    @Test
    public void singlePublisher() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestSubscriber<Integer> ts = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapPublisher((Function<Integer, Publisher<Integer>>) _ -> {
                    sleep();
                    return Flowable.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(ts);

                ts.cancel();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                ts.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }
}
