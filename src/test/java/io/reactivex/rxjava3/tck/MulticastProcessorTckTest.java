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

package io.reactivex.rxjava3.tck;

import java.util.concurrent.*;

import org.reactivestreams.*;
import org.reactivestreams.tck.*;
import org.testng.annotations.Test;

import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.processors.MulticastProcessor;

@Test
public class MulticastProcessorTckTest extends IdentityProcessorVerification<Integer> {

    public MulticastProcessorTckTest() {
        super(new TestEnvironment(50));
    }

    @Override
    public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
        MulticastProcessor<Integer> mp = MulticastProcessor.create();
        return new RefCountProcessor<>(mp);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        MulticastProcessor<Integer> mp = MulticastProcessor.create();
        mp.start();
        mp.onError(new TestException());
        return mp;
    }

    @Override
    public ExecutorService publisherExecutorService() {
        return Executors.newCachedThreadPool();
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }

    @Override
    public long maxSupportedSubscribers() {
        return 1;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
