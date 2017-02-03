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

package io.reactivex.internal.observers;

import io.reactivex.annotations.Nullable;
import org.junit.Test;

public class BasicQueueDisposableTest {

    BasicQueueDisposable<Integer> q = new BasicQueueDisposable<Integer>() {

        @Override
        public boolean isDisposed() {
            return false;
        }

        @Override
        public void dispose() {

        }

        @Nullable
        @Override
        public Integer poll() throws Exception {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public int requestFusion(int mode) {
            return 0;
        }
    };

    @Test(expected = UnsupportedOperationException.class)
    public void offer() {
        q.offer(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void offer2() {
        q.offer(1, 2);
    }
}
