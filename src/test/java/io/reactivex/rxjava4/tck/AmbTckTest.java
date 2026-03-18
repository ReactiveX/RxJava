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

package io.reactivex.rxjava4.tck;

import java.util.Arrays;

import static java.util.concurrent.Flow.*;
import org.testng.annotations.Test;

import io.reactivex.rxjava4.core.Flowable;

@Test
public class AmbTckTest extends BaseTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return
            Flowable.amb(Arrays.asList(
                    Flowable.fromIterable(iterate(elements)),
                    Flowable.<Long>never()
                )
            )
        ;
    }
}
