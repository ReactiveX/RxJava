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

package io.reactivex.rxjava3.tck;

import java.util.concurrent.Callable;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.TestException;

@Test
public class FromCallableTckTest extends BaseTck<Long> {

    @Override
    public Publisher<Long> createPublisher(final long elements) {
        return
                Flowable.fromCallable(new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        return 1L;
                    }
                }
                )
            ;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return
                Flowable.fromCallable(new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        throw new TestException();
                    }
                }
                )
            ;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1;
    }
}
