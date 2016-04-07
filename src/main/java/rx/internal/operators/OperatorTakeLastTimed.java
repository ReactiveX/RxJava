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

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.Observable.Operator;
import rx.functions.Func1;

/**
 * Returns an Observable that emits the last <code>count</code> items emitted by the source Observable.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/last.png" alt="">
 * 
 * @param <T> the value type
 */
public final class OperatorTakeLastTimed<T> implements Operator<T, T> {

    final long ageMillis;
    final Scheduler scheduler;
    final int count;

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
        final TakeLastTimedSubscriber<T> parent = new TakeLastTimedSubscriber<T>(subscriber, count, ageMillis, scheduler);
        
        subscriber.add(parent);
        subscriber.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        
        return parent;
    }
    
    static final class TakeLastTimedSubscriber<T> extends Subscriber<T> implements Func1<Object, T> {
        final Subscriber<? super T> actual;
        final long ageMillis;
        final Scheduler scheduler;
        final int count;
        final AtomicLong requested;
        final ArrayDeque<Object> queue;
        final ArrayDeque<Long> queueTimes;
        final NotificationLite<T> nl;

        public TakeLastTimedSubscriber(Subscriber<? super T> actual, int count, long ageMillis, Scheduler scheduler) {
            this.actual = actual;
            this.count = count;
            this.ageMillis = ageMillis;
            this.scheduler = scheduler;
            this.requested = new AtomicLong();
            this.queue = new ArrayDeque<Object>();
            this.queueTimes = new ArrayDeque<Long>();
            this.nl = NotificationLite.instance();
        }
        
        @Override
        public void onNext(T t) {
            if (count != 0) {
                long now = scheduler.now();
    
                if (queue.size() == count) {
                    queue.poll();
                    queueTimes.poll();
                }
                
                evictOld(now);
                
                queue.offer(nl.next(t));
                queueTimes.offer(now);
            }
        }

        protected void evictOld(long now) {
            long minTime = now - ageMillis;
            for (;;) {
                Long time = queueTimes.peek();
                if (time == null || time >= minTime) {
                    break;
                }
                queue.poll();
                queueTimes.poll();
            }
        }
        
        @Override
        public void onError(Throwable e) {
            queue.clear();
            queueTimes.clear();
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            evictOld(scheduler.now());
            
            queueTimes.clear();
            
            BackpressureUtils.postCompleteDone(requested, queue, actual, this);
        }
        
        @Override
        public T call(Object t) {
            return nl.getValue(t);
        }
        
        void requestMore(long n) {
            BackpressureUtils.postCompleteRequest(requested, n, queue, actual, this);
        }
    }
}
