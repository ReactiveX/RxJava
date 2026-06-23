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

package io.reactivex.rxjava4.internal.operators.observable;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.testsupport.*;

@Isolated
public class ObservableWindowWithSizeIsolatedTest extends RxJavaTest {

    @Test
    public void windowUnsubscribeNonOverlappingAsyncSource() {
        TestObserverEx<Integer> to = new TestObserverEx<>();

        final AtomicInteger count = new AtomicInteger();
        Observable.merge(Observable.range(1, 100000)
                        .doOnNext(_ -> {
                            if (count.incrementAndGet() == 50000) {
                                // give it a small break halfway through
                                try {
                                    Thread.sleep(75);
                                } catch (InterruptedException _) {
                                    // ignored
                                }
                            }
                        })
                        .observeOn(Schedulers.computation())
                        .window(5)
                        .take(2))
                .subscribe(to);

        to.awaitDone(1000, TimeUnit.MILLISECONDS);
        to.assertTerminated();
        to.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // make sure we don't emit all values ... unsubscribe should propagate
        assertTrue(count.get() < 100000, "count: " + count.get());
    }
}