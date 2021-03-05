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

package io.reactivex.rxjava3.internal.fuseable;

import static org.junit.Assert.*;
import org.junit.Test;

import io.reactivex.rxjava3.testsupport.TestHelper;

public class CancellableQueueFuseableTest {

    @Test
    public void offer() {
        TestHelper.assertNoOffer(new CancellableQueueFuseable<>());
    }

    @Test
    public void pollClear() throws Throwable {
        CancellableQueueFuseable<Object> qs = new CancellableQueueFuseable<>();

        assertNull(qs.poll());

        qs.clear();
        assertNull(qs.poll());
    }

    @Test
    public void cancel() {
        CancellableQueueFuseable<Object> qs = new CancellableQueueFuseable<>();

        assertFalse(qs.isDisposed());

        qs.cancel();

        assertTrue(qs.isDisposed());

        qs.cancel();

        assertTrue(qs.isDisposed());
    }

    @Test
    public void dispose() {
        CancellableQueueFuseable<Object> qs = new CancellableQueueFuseable<>();

        assertFalse(qs.isDisposed());

        qs.dispose();

        assertTrue(qs.isDisposed());

        qs.dispose();

        assertTrue(qs.isDisposed());
    }

    @Test
    public void cancel2() {
        AbstractEmptyQueueFuseable<Object> qs = new AbstractEmptyQueueFuseable<Object>() { };

        assertFalse(qs.isDisposed());

        qs.cancel();
    }

    @Test
    public void dispose2() {
        AbstractEmptyQueueFuseable<Object> qs = new AbstractEmptyQueueFuseable<Object>() { };

        assertFalse(qs.isDisposed());

        qs.dispose();
    }
}
