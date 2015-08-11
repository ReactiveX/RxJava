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

import rx.*;
import rx.Observable.OnSubscribe;

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
        private static final long serialVersionUID = 4114392207069098388L;
        
        private final Subscriber<? super Integer> o;
        private final int end;
        private long index;

        private RangeProducer(Subscriber<? super Integer> o, int start, int end) {
            this.o = o;
            this.index = start;
            this.end = end;
        }

        @Override
        public void request(long n) {
            if (get() == Long.MAX_VALUE) {
                // already started with fast-path
                return;
            }
            if (n == Long.MAX_VALUE && compareAndSet(0L, Long.MAX_VALUE)) {
                // fast-path without backpressure
                fastpath();
            } else if (n > 0L) {
                long c = BackpressureUtils.getAndAddRequest(this, n);
                if (c == 0L) {
                    // backpressure is requested
                    slowpath(n);
                }
            }
        }

        /**
         * 
         */
        void slowpath(long r) {
            long idx = index;
            while (true) {
                /*
                 * This complicated logic is done to avoid touching the volatile `index` and `requested` values
                 * during the loop itself. If they are touched during the loop the performance is impacted significantly.
                 */
                long fs = end - idx + 1;
                long e = Math.min(fs, r);
                final boolean complete = fs <= r;

                fs = e + idx;
                final Subscriber<? super Integer> o = this.o;
                
                for (long i = idx; i != fs; i++) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    o.onNext((int) i);
                }
                
                if (complete) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    o.onCompleted();
                    return;
                }
                
                idx = fs;
                index = fs;
                
                r = addAndGet(-e);
                if (r == 0L) {
                    // we're done emitting the number requested so return
                    return;
                }
            }
        }

        /**
         * 
         */
        void fastpath() {
            final long end = this.end + 1L;
            final Subscriber<? super Integer> o = this.o;
            for (long i = index; i != end; i++) {
                if (o.isUnsubscribed()) {
                    return;
                }
                o.onNext((int) i);
            }
            if (!o.isUnsubscribed()) {
                o.onCompleted();
            }
        }
    }

}
