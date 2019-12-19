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

import java.util.stream.*;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.tck.BaseTck;

@Test
public class FlatMapStream2TckTest extends BaseTck<Integer> {

    @Override
    public Publisher<Integer> createPublisher(final long elements) {
        if (elements % 2 == 0) {
            return Flowable.range(0, (int)elements / 2).flatMapStream(v -> Stream.of(v, v + 1));
        }
        return
                Flowable.range(-1, 1 + (int)elements / 2).flatMapStream(v -> {
                    if (v != -1) {
                        return Stream.of(v, v + 1);
                    }
                    return Stream.of(v);
                });
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        Stream<Integer> stream = Stream.of(1);
        stream.forEach(v -> { });
        return Flowable.just(1).flatMapStream(v -> stream);
    }
}
