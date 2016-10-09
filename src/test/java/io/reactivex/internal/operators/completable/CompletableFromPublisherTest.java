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

package io.reactivex.internal.operators.completable;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import org.junit.Test;

public class CompletableFromPublisherTest {
    @Test(expected = NullPointerException.class)
    public void fromPublisherNull() {
        Completable.fromPublisher(null);
    }

    @Test
    public void fromPublisher() {
        Completable.fromPublisher(Flowable.just(1))
            .test()
            .assertResult();
    }

    @Test
    public void fromPublisherEmpty() {
        Completable.fromPublisher(Flowable.empty())
            .test()
            .assertResult();
    }

    @Test
    public void fromPublisherThrows() {
        Completable.fromPublisher(Flowable.error(new UnsupportedOperationException()))
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }
}
