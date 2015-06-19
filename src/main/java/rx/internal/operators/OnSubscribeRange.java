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

import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.OnSubscribe;
import rx.*;

/**
 * Emit ints from start to end inclusive.
 */
public final class OnSubscribeRange implements OnSubscribe<Integer> {

    private final int start;
    private final int end;

    public OnSubscribeRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void call(final Subscriber<? super Integer> o) {
        o.setProducer(new RangeProducer(o, start, end));
    }

    private static final class RangeProducer extends AtomicLong implements Producer {
        /** */
        private static final long serialVersionUID = -3226360080946653231L;
        private final Subscriber<? super Integer> o;
        // accessed by REQUESTED_UPDATER
        private long index;
        private final int end;
        private final long count;

        private RangeProducer(Subscriber<? super Integer> o, int start, int end) {
            this.o = o;
            this.index = start;
            this.end = end;
            this.count = (long)(end - start) + 1;
        }

        @Override
        public void request(long n) {
            long c = count;
            if (get() >= c) {
                // already started with fast-path
                return;
            }
            if (n >= c && compareAndSet(0, c)) {
                // fast-path without backpressure
                for (long i = index; i <= end; i++) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    o.onNext((int) i);
                }
                if (!o.isUnsubscribed()) {
                    o.onCompleted();
                }
            } else if (n > 0) {
                // backpressure is requested
                long _c = BackpressureUtils.getAndAddRequest(this, n);
                if (_c == 0) {
                    while (true) {
                        /*
                         * This complicated logic is done to avoid touching the volatile `index` and `requested` values
                         * during the loop itself. If they are touched during the loop the performance is impacted significantly.
                         */
                        long r = get();
                        long idx = index;
                        long numLeft = end - idx + 1;
                        long e = Math.min(numLeft, r);
                        boolean completeOnFinish = numLeft <= r;
                        long stopAt = e + idx;
                        for (long i = idx; i < stopAt; i++) {
                            if (o.isUnsubscribed()) {
                                return;
                            }
                            o.onNext((int) i);
                        }
                        index = stopAt;
                        
                        if (completeOnFinish) {
                            o.onCompleted();
                            return;
                        }
                        if (addAndGet(-e) == 0) {
                            // we're done emitting the number requested so return
                            return;
                        }
                    }
                }
            }
        }
    }

}
