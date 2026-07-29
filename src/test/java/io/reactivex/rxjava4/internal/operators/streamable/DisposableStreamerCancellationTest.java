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

package io.reactivex.rxjava4.internal.operators.streamable;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.disposables.NeverDisposableStreamerCancellation;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class DisposableStreamerCancellationTest extends StreamableBaseTest {

    @Test
    public void never() throws Throwable {
        TestHelper.checkEnum(NeverDisposableStreamerCancellation.class);

        var ndsc = DisposableStreamerCancellation.never();

        assertTrue(ndsc.add(Disposable.empty()));
        assertTrue(ndsc.remove(Disposable.empty()));
        assertTrue(ndsc.delete(Disposable.empty()));
        assertFalse(ndsc.isDisposed());
        ndsc.dispose();
        assertFalse(ndsc.isDisposed());
        assertSame(ndsc, ndsc.derive());
    }

}
