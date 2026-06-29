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

package io.reactivex.rxjava4.completable;

import static org.testng.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.schedulers.Schedulers;

/**
 * Test Completable methods and operators.
 */
@Isolated
public class CompletableIsolatedTest extends RxJavaTest {

    @org.junit.jupiter.api.Test
    public void repeatNormal() {
        final AtomicReference<Throwable> err = new AtomicReference<>();
        final AtomicInteger calls = new AtomicInteger();

        Completable c = Completable.fromCallable(() -> {
            calls.getAndIncrement();
            Thread.sleep(200);
            return null;
        }).repeat();

        c.subscribe(new CompletableObserver() /* NFI */ {
            @Override
            public void onSubscribe(final Disposable d) {
                Schedulers.single().scheduleDirect(d::dispose, 1100, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onError(Throwable e) {
                err.set(e);
            }

            @Override
            public void onComplete() {

            }
        });

        assertEquals(6, calls.get(), "calls count mismatch");
        assertNull(err.get(), "error present");
    }
}
