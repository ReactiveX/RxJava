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

    private final int startIndex;
    private final int endIndex;

    public OnSubscribeRange(int start, int end) {
        this.startIndex = start;
        this.endIndex = end;
    }

    @Override
    public void call(final Subscriber<? super Integer> childSubscriber) {
        childSubscriber.setProducer(new RangeProducer(childSubscriber, startIndex, endIndex));
    }

    private static final class RangeProducer extends AtomicLong implements Producer {
        /** */
        private static final long serialVersionUID = 4114392207069098388L;
        
        private final Subscriber<? super Integer> childSubscriber;
        private final int endOfRange;
        private long currentIndex;

        RangeProducer(Subscriber<? super Integer> childSubscriber, int startIndex, int endIndex) {
            this.childSubscriber = childSubscriber;
            this.currentIndex = startIndex;
            this.endOfRange = endIndex;
        }

        @Override
        public void request(long requestedAmount) {
            if (get() == Long.MAX_VALUE) {
                // already started with fast-path
                return;
            }
            if (requestedAmount == Long.MAX_VALUE && compareAndSet(0L, Long.MAX_VALUE)) {
                // fast-path without backpressure
                fastpath();
            } else if (requestedAmount > 0L) {
                long c = BackpressureUtils.getAndAddRequest(this, requestedAmount);
                if (c == 0L) {
                    // backpressure is requested
                    slowpath(requestedAmount);
                }
            }
        }

        /**
         * Emits as many values as requested or remaining from the range, whichever is smaller.
         */
        void slowpath(long requestedAmount) {
            long emitted = 0L;
            long endIndex = endOfRange + 1L;
            long index = currentIndex;
            
            final Subscriber<? super Integer> childSubscriber = this.childSubscriber;
            
            for (;;) {
                
                while (emitted != requestedAmount && index != endIndex) {
                    if (childSubscriber.isUnsubscribed()) {
                        return;
                    }
                    
                    childSubscriber.onNext((int)index);
                    
                    index++;
                    emitted++;
                }
                
                if (childSubscriber.isUnsubscribed()) {
                    return;
                }
                
                if (index == endIndex) {
                    childSubscriber.onCompleted();
                    return;
                }
                
                requestedAmount = get();
                
                if (requestedAmount == emitted) {
                    currentIndex = index;
                    requestedAmount = addAndGet(-emitted);
                    if (requestedAmount == 0L) {
                        break;
                    }
                    emitted = 0L;
                }
            }
        }

        /**
         * Emits all remaining values without decrementing the requested amount.
         */
        void fastpath() {
            final long endIndex = this.endOfRange + 1L;
            final Subscriber<? super Integer> childSubscriber = this.childSubscriber;
            for (long index = currentIndex; index != endIndex; index++) {
                if (childSubscriber.isUnsubscribed()) {
                    return;
                }
                childSubscriber.onNext((int) index);
            }
            if (!childSubscriber.isUnsubscribed()) {
                childSubscriber.onCompleted();
            }
        }
    }

}
