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

public final class OverlappingBufferProducer implements Producer {

    private final Producer producer;
    private final int count;
    private final int skip;

    OverlappingBufferProducer(Producer parentProducer, int count, int skip) {
        this.producer = parentProducer;
        this.count = count;
        this.skip = skip;
    }

    private volatile boolean firstRequest = true;
    private volatile boolean infinite = false;

    private void requestInfinite() {
        infinite = true;
        producer.request(Long.MAX_VALUE);
    }

    @Override
    public void request(long n) {
        if (infinite) {
            return;
        }
        if (n == Long.MAX_VALUE) {
            requestInfinite();
            return;
        } else {
            if (firstRequest) {
                firstRequest = false;
                if (n - 1 >= (Long.MAX_VALUE - count) / skip) {
                    // count + skip * (n - 1) >= Long.MAX_VALUE
                    requestInfinite();
                    return;
                }
                // count = 5, skip = 2, n = 3
                // * * * * *
                //     * * * * *
                //         * * * * *
                // request = 5 + 2 * ( 3 - 1)
                producer.request(count + skip * (n - 1));
            } else {
                if (n >= Long.MAX_VALUE / skip) {
                    // skip * n >= Long.MAX_VALUE
                    requestInfinite();
                    return;
                }
                // count = 5, skip = 2, n = 3
                // (* * *) * *
                // (    *) * * * *
                //           * * * * *
                // request = skip * n
                // "()" means the items already emitted before this request
                producer.request(skip * n);
            }
        }
    }
}
