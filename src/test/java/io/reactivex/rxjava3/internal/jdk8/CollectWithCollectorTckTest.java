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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.List;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.tck.BaseTck;

@Test
public class CollectWithCollectorTckTest extends BaseTck<List<Integer>> {

    @Override
    public Publisher<List<Integer>> createPublisher(final long elements) {
        return
                Flowable.range(0, (int)elements).collect(Collectors.toList()).toFlowable()
            ;
    }

    @Override
    public Publisher<List<Integer>> createFailedPublisher() {
        return Flowable.<Integer>error(new TestException()).collect(Collectors.toList()).toFlowable();
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1;
    }
}
