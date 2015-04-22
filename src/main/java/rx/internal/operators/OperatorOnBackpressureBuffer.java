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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.internal.util.BackpressureDrainManager;

public class OperatorOnBackpressureBuffer<T> implements Operator<T, T> {

    private final Long capacity;
    private final Action0 onOverflow;

    private static class Holder {
        static final OperatorOnBackpressureBuffer<?> INSTANCE = new OperatorOnBackpressureBuffer<Object>();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> OperatorOnBackpressureBuffer<T> instance() {
        return (OperatorOnBackpressureBuffer<T>) Holder.INSTANCE;
    }
    
    private OperatorOnBackpressureBuffer() {
        this.capacity = null;
        this.onOverflow = null;
    }

    public OperatorOnBackpressureBuffer(long capacity) {
        this(capacity, null);
    }

    public OperatorOnBackpressureBuffer(long capacity, Action0 onOverflow) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be > 0");
        }
        this.capacity = capacity;
        this.onOverflow = onOverflow;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {

        // don't pass through subscriber as we are async and doing queue draining
        // a parent being unsubscribed should not affect the children
        BufferSubscriber<T> parent = new BufferSubscriber<T>(child, capacity, onOverflow);

        // if child unsubscribes it should unsubscribe the parent, but not the other way around
        child.add(parent);
        child.setProducer(parent.manager());

        return parent;
    }
    private static final class BufferSubscriber<T> extends Subscriber<T> implements BackpressureDrainManager.BackpressureQueueCallback {
        // TODO get a different queue implementation
        private final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<Object>();
        private final Long baseCapacity;
        private final AtomicLong capacity;
        private final Subscriber<? super T> child;
        private final AtomicBoolean saturated = new AtomicBoolean(false);
        private final BackpressureDrainManager manager;
        private final NotificationLite<T> on = NotificationLite.instance();
        private final Action0 onOverflow;
        
        public BufferSubscriber(final Subscriber<? super T> child, Long capacity, Action0 onOverflow) {
            this.child = child;
            this.baseCapacity = capacity;
            this.capacity = capacity != null ? new AtomicLong(capacity) : null;
            this.onOverflow = onOverflow;
            this.manager = new BackpressureDrainManager(this);
        }
        @Override
        public void onStart() {
            request(Long.MAX_VALUE);
        }

        @Override
        public void onCompleted() {
            if (!saturated.get()) {
                manager.terminateAndDrain();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!saturated.get()) {
                manager.terminateAndDrain(e);
            }
        }

        @Override
        public void onNext(T t) {
            if (!assertCapacity()) {
                return;
            }
            queue.offer(on.next(t));
            manager.drain();
        }

        @Override
        public boolean accept(Object value) {
            return on.accept(child, value);
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
            Object value = queue.poll();
            if (capacity != null && value != null) {
                capacity.incrementAndGet();
            }
            return value;
        }
        
        private boolean assertCapacity() {
            if (capacity == null) {
                return true;
            }

            long currCapacity;
            do {
                currCapacity = capacity.get();
                if (currCapacity <= 0) {
                    if (saturated.compareAndSet(false, true)) {
                        unsubscribe();
                        child.onError(new MissingBackpressureException(
                                "Overflowed buffer of "
                                        + baseCapacity));
                        if (onOverflow != null) {
                            onOverflow.call();
                        }
                    }
                    return false;
                }
                // ensure no other thread stole our slot, or retry
            } while (!capacity.compareAndSet(currCapacity, currCapacity - 1));
            return true;
        }
        protected Producer manager() {
            return manager;
        }
    }
}
