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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.internal.util.BackpressureDrainManager;

/**
 * Operator that blocks the producer thread in case a backpressure is needed.
 */
public class OperatorOnBackpressureBlock<T> implements Operator<T, T> {
    final int max;
    public OperatorOnBackpressureBlock(int max) {
        this.max = max;
    }
    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        BlockingSubscriber<T> s = new BlockingSubscriber<T>(max, child);
        s.init();
        return s;
    }
    
    static final class BlockingSubscriber<T> extends Subscriber<T> implements BackpressureDrainManager.BackpressureQueueCallback {
        final NotificationLite<T> nl = NotificationLite.instance();
        final BlockingQueue<Object> queue;
        final Subscriber<? super T> child;
        final BackpressureDrainManager manager;
        public BlockingSubscriber(int max, Subscriber<? super T> child) {
            this.queue = new ArrayBlockingQueue<Object>(max);
            this.child = child;
            this.manager = new BackpressureDrainManager(this);
        }
        void init() {
            child.add(this);
            child.setProducer(manager);
        }
        @Override
        public void onNext(T t) {
            try {
                queue.put(nl.next(t));
                manager.drain();
            } catch (InterruptedException ex) {
                if (!isUnsubscribed()) {
                    onError(ex);
                }
            }
        }
        @Override
        public void onError(Throwable e) {
            manager.terminateAndDrain(e);
        }
        @Override
        public void onCompleted() {
            manager.terminateAndDrain();
        }
        @Override
        public boolean accept(Object value) {
            return nl.accept(child, value);
        }
        @Override
        public void complete(Throwable exception) {
            if (exception != null) {
                child.onError(exception);
            } else {
                child.onCompleted();
            }
        }
        @Override
        public Object peek() {
            return queue.peek();
        }
        @Override
        public Object poll() {
            return queue.poll();
        }
    }
}
