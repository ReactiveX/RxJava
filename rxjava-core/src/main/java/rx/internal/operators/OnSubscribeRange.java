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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.internal.util.RxSpscRingBuffer;

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
        // need +1 as this is inclusive
        if ((end - start + 1) < RxSpscRingBuffer.SIZE) {
            int index = start;
            while (index <= end) {
                if (o.isUnsubscribed()) {
                    return;
                }
                o.onNext(index++);
            }
            o.onCompleted();
        } else {
            // otherwise we do it via the producer to support backpressure
            o.setProducer(new RangeProducer(o, start, end));
        }

    }

    private static final class RangeProducer implements Producer {
        private final Subscriber<? super Integer> o;
        @SuppressWarnings("unused")
        // accessed by REQUESTED_UPDATER
        private volatile int requested;
        private static final AtomicIntegerFieldUpdater<RangeProducer> REQUESTED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(RangeProducer.class, "requested");
        private volatile int index;
        private final int end;

        private RangeProducer(Subscriber<? super Integer> o, int start, int end) {
            this.o = o;
            this.index = start;
            this.end = end;
        }

        @Override
        public void request(int n) {
            int _c = REQUESTED_UPDATER.getAndAdd(this, n);
            if (_c == 0) {
                while (index <= end) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    o.onNext(index++);
                    if (REQUESTED_UPDATER.decrementAndGet(this) == 0) {
                        // we're done emitting the number requested so return
                        return;
                    }
                }
                o.onCompleted();
            }
        }
    }

}
