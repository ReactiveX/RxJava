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

package io.reactivex.rxjava3.internal.util;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class BlockingHelperTest extends RxJavaTest {

    @Test
    public void emptyEnum() {
        TestHelper.checkUtilityClass(BlockingHelper.class);
    }

    @Test
    public void interrupted() {
        CountDownLatch cdl = new CountDownLatch(1);
        Disposable d = Disposable.empty();

        Thread.currentThread().interrupt();

        try {
            BlockingHelper.awaitForComplete(cdl, d);
        } catch (IllegalStateException ex) {
            // expected
        }
        assertTrue(d.isDisposed());
        assertTrue(Thread.interrupted());
    }

    @Test
    public void unblock() {
        final CountDownLatch cdl = new CountDownLatch(1);
        Disposable d = Disposable.empty();

        Schedulers.computation().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        }, 100, TimeUnit.MILLISECONDS);

        BlockingHelper.awaitForComplete(cdl, d);

        assertFalse(d.isDisposed());
    }
}
