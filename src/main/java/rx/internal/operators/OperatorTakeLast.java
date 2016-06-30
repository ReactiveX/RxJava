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
import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.Observable.Operator;
import rx.functions.Func1;

/**
 * Returns an Observable that emits the at most the last <code>count</code> items emitted by the source Observable.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/last.png" alt="">
 * 
 * @param <T> the value type
 */
public final class OperatorTakeLast<T> implements Operator<T, T> {

    final int count;

    public OperatorTakeLast(int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("count cannot be negative");
        }
        this.count = count;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        final TakeLastSubscriber<T> parent = new TakeLastSubscriber<T>(subscriber, count);
        
        subscriber.add(parent);
        subscriber.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        
        return parent;
    }
    
    static final class TakeLastSubscriber<T> extends Subscriber<T> implements Func1<Object, T> {
        final Subscriber<? super T> actual;
        final AtomicLong requested;
        final ArrayDeque<Object> queue;
        final int count;
        final NotificationLite<T> nl;
        
        public TakeLastSubscriber(Subscriber<? super T> actual, int count) {
            this.actual = actual;
            this.count = count;
            this.requested = new AtomicLong();
            this.queue = new ArrayDeque<Object>();
            this.nl = NotificationLite.instance();
        }
        
        @Override
        public void onNext(T t) {
            if (queue.size() == count) {
                queue.poll();
            }
            queue.offer(nl.next(t));
        }
        
        @Override
        public void onError(Throwable e) {
            queue.clear();
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            BackpressureUtils.postCompleteDone(requested, queue, actual, this);
        }
        
        @Override
        public T call(Object t) {
            return nl.getValue(t);
        }
        
        void requestMore(long n) {
            if (n > 0L) {
                BackpressureUtils.postCompleteRequest(requested, n, queue, actual, this);
            }
        }
    }
}
