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

import static java.util.concurrent.Flow.*;
import org.testng.annotations.Test;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.exceptions.TestException;

@Test
public class FromSupplierTckTest extends BaseTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(final long elements) {
        return
                Flowable.fromSupplier(() -> 1L
                )
            ;
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return
                Flowable.fromSupplier(() -> {
                    throw new TestException();
                }
                )
            ;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1;
    }
}
