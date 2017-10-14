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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;

public class ObservableIntervalRangeTest {

    @Test
    public void simple() throws Exception {
        Observable.intervalRange(5, 5, 50, 50, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(5L, 6L, 7L, 8L, 9L);
    }

    @Test
    public void customScheduler() {
        Observable.intervalRange(1, 5, 1, 1, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void countZero() {
        Observable.intervalRange(1, 0, 1, 1, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void countNegative() {
        try {
            Observable.intervalRange(1, -1, 1, 1, TimeUnit.MILLISECONDS);
            fail("Should have thrown!");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -1", ex.getMessage());
        }
    }

    @Test
    public void longOverflow() {
        Observable.intervalRange(Long.MAX_VALUE - 1, 2, 1, 1, TimeUnit.MILLISECONDS);

        Observable.intervalRange(Long.MIN_VALUE, Long.MAX_VALUE, 1, 1, TimeUnit.MILLISECONDS);

        try {
            Observable.intervalRange(Long.MAX_VALUE - 1, 3, 1, 1, TimeUnit.MILLISECONDS);
            fail("Should have thrown!");
        } catch (IllegalArgumentException ex) {
            assertEquals("Overflow! start + count is bigger than Long.MAX_VALUE", ex.getMessage());
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.intervalRange(1, 2, 1, 1, TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 2000)
    public void cancel() {
        Observable.intervalRange(0, 20, 1, 1, TimeUnit.MILLISECONDS, Schedulers.trampoline())
        .take(10)
        .test()
        .assertResult(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }
}
