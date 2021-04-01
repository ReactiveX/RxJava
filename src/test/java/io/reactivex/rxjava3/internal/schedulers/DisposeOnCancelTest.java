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

package io.reactivex.rxjava3.internal.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;

public class DisposeOnCancelTest extends RxJavaTest {

    @Test
    public void basicCoverage() throws Exception {
        Disposable d = Disposable.empty();

        DisposeOnCancel doc = new DisposeOnCancel(d);

        assertFalse(doc.cancel(true));

        assertFalse(doc.isCancelled());

        assertFalse(doc.isDone());

        assertNull(doc.get());

        assertNull(doc.get(1, TimeUnit.SECONDS));
    }
}
