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

package io.reactivex.rxjava4.internal.virtual.tck;

import java.io.IOException;
import java.util.concurrent.Flow.Publisher;

import org.testng.annotations.Test;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.tck.BaseTck;

@Test
public class FlowableVirtualTransformVirtual3TckTest extends BaseTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(final long elements) {
        var half = elements >> 1;
        var rest = elements - half;
        return Flowable.rangeLong(0, rest)
                .virtualTransform((v, emitter, _) -> {
                    emitter.emit(v);
                    if (v < rest - 1 || half == rest) {
                        emitter.emit(v);
                    }
                }, Schedulers.virtual(), Flowable.bufferSize());
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return Flowable.error(new IOException())
                .virtualTransform((_, _, _) -> {
                }, Schedulers.virtual(), Flowable.bufferSize());
    }
}
