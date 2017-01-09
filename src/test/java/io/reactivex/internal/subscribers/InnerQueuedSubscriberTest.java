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

package io.reactivex.internal.subscribers;

import java.util.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.reactivestreams.Subscription;

public class InnerQueuedSubscriberTest {

    @Test
    public void requestInBatches() {
        InnerQueuedSubscriberSupport<Integer> support = new InnerQueuedSubscriberSupport<Integer>() {
            @Override
            public void innerNext(InnerQueuedSubscriber<Integer> inner, Integer value) {
            }
            @Override
            public void innerError(InnerQueuedSubscriber<Integer> inner, Throwable e) {
            }
            @Override
            public void innerComplete(InnerQueuedSubscriber<Integer> inner) {
            }
            @Override
            public void drain() {
            }
        };

        InnerQueuedSubscriber<Integer> inner = new InnerQueuedSubscriber<Integer>(support, 4);

        final List<Long> requests = new ArrayList<Long>();

        inner.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                requests.add(n);
            }
            @Override
            public void cancel() {
                // ignore
            }
        });

        inner.request(1);
        inner.request(1);
        inner.request(1);
        inner.request(1);
        inner.request(1);

        assertEquals(Arrays.asList(4L, 3L), requests);
    }
}
