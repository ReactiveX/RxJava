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

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * Returns an Observable that emits the last <code>count</code> items emitted by the source Observable.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/last.png" alt="">
 */
public final class OperatorTakeLastTimed<T> implements Operator<T, T> {

    private final long ageMillis;
    private final Scheduler scheduler;
    private final int count;

    public OperatorTakeLastTimed(long time, TimeUnit unit, Scheduler scheduler) {
        this.ageMillis = unit.toMillis(time);
        this.scheduler = scheduler;
        this.count = -1;
    }

    public OperatorTakeLastTimed(int count, long time, TimeUnit unit, Scheduler scheduler) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("count could not be negative");
        }
        this.ageMillis = unit.toMillis(time);
        this.scheduler = scheduler;
        this.count = count;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        final Deque<Object> buffer = new ArrayDeque<Object>();
        final Deque<Long> timestampBuffer = new ArrayDeque<Long>();
        final NotificationLite<T> notification = NotificationLite.instance();
        final TakeLastQueueProducer<T> producer = new TakeLastQueueProducer<T>(notification, buffer, subscriber);
        subscriber.setProducer(producer);
        return new Subscriber<T>(subscriber) {

            protected void runEvictionPolicy(long now) {
                // trim size
                while (count >= 0 && buffer.size() > count) {
                    timestampBuffer.pollFirst();
                    buffer.pollFirst();
                }
                // remove old entries
                while (!buffer.isEmpty()) {
                    long v = timestampBuffer.peekFirst();
                    if (v < now - ageMillis) {
                        timestampBuffer.pollFirst();
                        buffer.pollFirst();
                    } else {
                        break;
                    }
                }
            }

            // no backpressure up as it wants to receive and discard all but the last
            @Override
            public void onStart() {
                // we do this to break the chain of the child subscriber being passed through
                request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(T args) {
                long t = scheduler.now();
                timestampBuffer.add(t);
                buffer.add(notification.next(args));
                runEvictionPolicy(t);
            }

            @Override
            public void onError(Throwable e) {
                timestampBuffer.clear();
                buffer.clear();
                subscriber.onError(e);
            }

            @Override
            public void onCompleted() {
                runEvictionPolicy(scheduler.now());
                timestampBuffer.clear();
                buffer.offer(notification.completed());
                producer.startEmitting();
            }
        };
    }
}
