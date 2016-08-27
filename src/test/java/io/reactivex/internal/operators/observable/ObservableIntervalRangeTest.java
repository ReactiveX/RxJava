/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.Observable;

public class ObservableIntervalRangeTest {

    @Test
    public void simple() throws Exception {
        Observable.intervalRange(5, 5, 50, 50, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(5L, 6L, 7L, 8L, 9L);
    }

    @Test
    public void noOverflow() {
        Observable.intervalRange(Long.MAX_VALUE - 1, 2, 1, 1, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void longOverflow() {
        Observable.intervalRange(Long.MAX_VALUE - 1, 3, 1, 1, TimeUnit.SECONDS);
    }
}
