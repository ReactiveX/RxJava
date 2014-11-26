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
import rx.Producer;
import rx.Subscriber;

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
    
    static final class BlockingSubscriber<T> extends Subscriber<T> {
        final NotificationLite<T> nl = NotificationLite.instance();
        final BlockingQueue<Object> queue;
        final Subscriber<? super T> child;
        /** Guarded by this. */
        long requestedCount;
        /** Guarded by this. */
        boolean emitting;
        volatile boolean terminated;
        /** Set before terminated, read after terminated. */
        Throwable exception;
        public BlockingSubscriber(int max, Subscriber<? super T> child) {
            this.queue = new ArrayBlockingQueue<Object>(max);
            this.child = child;
        }
        void init() {
            child.add(this);
            child.setProducer(new Producer() {
                @Override
                public void request(long n) {
                    synchronized (BlockingSubscriber.this) {
                        if (n == Long.MAX_VALUE || requestedCount == Long.MAX_VALUE) {
                            requestedCount = Long.MAX_VALUE;
                        } else {
                            requestedCount += n;
                        }
                    }
                    drain();
                }
            });
        }
        @Override
        public void onNext(T t) {
            try {
                queue.put(nl.next(t));
                drain();
            } catch (InterruptedException ex) {
                if (!isUnsubscribed()) {
                    onError(ex);
                }
            }
        }
        @Override
        public void onError(Throwable e) {
            if (!terminated) {
                exception = e;
                terminated = true;
                drain();
            }
        }
        @Override
        public void onCompleted() {
            terminated = true;
            drain();
        }
        void drain() {
            long n;
            synchronized (this) {
                if (emitting) {
                    return;
                }
                emitting = true;
                n = requestedCount;
            }
            boolean skipFinal = false;
            try {
                while (true) {
                    int emitted = 0;
                    while (n > 0) {
                        Object o = queue.poll();
                        if (o == null) {
                            if (terminated) {
                                if (exception != null) {
                                    child.onError(exception);
                                } else {
                                    child.onCompleted();
                                }
                                return;
                            }
                            break;
                        } else {
                            child.onNext(nl.getValue(o));
                            n--;
                            emitted++;
                        }
                    }
                    synchronized (this) {
                        // if no backpressure below
                        if (requestedCount == Long.MAX_VALUE) {
                            // no new data arrived since the last poll
                            if (queue.peek() == null) {
                                skipFinal = true;
                                emitting = false;
                                return;
                            }
                            n = Long.MAX_VALUE;
                        } else {
                            if (emitted == 0) {
                                skipFinal = true;
                                emitting = false;
                                return;
                            }
                            requestedCount -= emitted;
                            n = requestedCount;
                        }
                    }
                }
            } finally {
                if (!skipFinal) {
                    synchronized (this) {
                        emitting = false;
                    }
                }
            }
        }
    }
}
