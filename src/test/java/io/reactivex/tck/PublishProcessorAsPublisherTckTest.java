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

package io.reactivex.tck;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

@Test
public class PublishProcessorAsPublisherTckTest extends BaseTck<Integer> {

    public PublishProcessorAsPublisherTckTest() {
        super(100);
    }

    @Override
    public Publisher<Integer> createPublisher(final long elements) {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        Schedulers.io().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                while (!pp.hasSubscribers()) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        return;
                    }

                    if (System.currentTimeMillis() - start > 200) {
                        return;
                    }
                }

                for (int i = 0; i < elements; i++) {
                    while (!pp.offer(i)) {
                        Thread.yield();
                        if (System.currentTimeMillis() - start > 1000) {
                            return;
                        }
                    }
                }
                pp.onComplete();
            }
        });
        return pp;
    }
}
