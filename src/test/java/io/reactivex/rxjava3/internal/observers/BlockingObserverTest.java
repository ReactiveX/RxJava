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

package io.reactivex.rxjava3.internal.observers;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;

public class BlockingObserverTest extends RxJavaTest {

    @Test
    public void dispose() {
        Queue<Object> q = new ArrayDeque<>();

        BlockingObserver<Object> bo = new BlockingObserver<>(q);

        bo.dispose();

        assertEquals(BlockingObserver.TERMINATED, q.poll());

        bo.dispose();

        assertNull(q.poll());
    }
}
