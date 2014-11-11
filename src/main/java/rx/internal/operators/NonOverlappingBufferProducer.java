/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import rx.Producer;

public final class NonOverlappingBufferProducer implements Producer {

    private final Producer producer;
    private final int count;

    NonOverlappingBufferProducer(Producer parentProducer, int count) {
        this.producer = parentProducer;
        this.count = count;
    }

    private volatile boolean infinite = false;

    @Override
    public void request(long n) {
        if (infinite) {
            return;
        }
        if (n >= Long.MAX_VALUE / count) {
            // n == Long.MAX_VALUE or n * count >= Long.MAX_VALUE
            infinite = true;
            producer.request(Long.MAX_VALUE);
        } else {
            producer.request(n * count);
        }
    }
}
