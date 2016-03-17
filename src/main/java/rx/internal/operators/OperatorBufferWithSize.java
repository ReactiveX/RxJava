/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import java.util.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable;
import rx.Observable.Operator;
import rx.exceptions.MissingBackpressureException;

/**
 * This operation takes
 * values from the specified {@link Observable} source and stores them in all active chunks until the buffer
 * contains a specified number of elements. The buffer is then emitted. Chunks are created after a certain
 * amount of values have been received. When the source {@link Observable} completes or produces an error,
 * the currently active chunks are emitted, and the event is propagated to all subscribed {@link Subscriber}s.
 * <p>
 * Note that this operation can produce <strong>non-connected, connected non-overlapping, or overlapping
 * chunks</strong> depending on the input parameters.
 * </p>

* @param <T> the buffered value type
 */
public final class OperatorBufferWithSize<T> implements Operator<List<T>, T> {
    final int count;
    final int skip;

    /**
     * @param count
     *            the number of elements a buffer should have before being emitted
     * @param skip
     *            the interval with which chunks have to be created. Note that when {@code skip == count} 
     *            the operator will produce non-overlapping chunks. If
     *            {@code skip < count}, this buffer operation will produce overlapping chunks and if
     *            {@code skip > count} non-overlapping chunks will be created and some values will not be pushed
     *            into a buffer at all!
     */
    public OperatorBufferWithSize(int count, int skip) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }
        if (skip <= 0) {
            throw new IllegalArgumentException("skip must be greater than 0");
        }
        this.count = count;
        this.skip = skip;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super List<T>> child) {
        if (skip == count) {
            BufferExact<T> parent = new BufferExact<T>(child, count);
            
            child.add(parent);
            child.setProducer(parent.createProducer());
            
            return parent;
        }
        if (skip > count) {
            BufferSkip<T> parent = new BufferSkip<T>(child, count, skip);
            
            child.add(parent);
            child.setProducer(parent.createProducer());
            
            return parent;
        }
        BufferOverlap<T> parent = new BufferOverlap<T>(child, count, skip);
        
        child.add(parent);
        child.setProducer(parent.createProducer());
        
        return parent;
    }
    
    static final class BufferExact<T> extends Subscriber<T> {
        final Subscriber<? super List<T>> actual;
        final int count;

        List<T> buffer;
        
        public BufferExact(Subscriber<? super List<T>> actual, int count) {
            this.actual = actual;
            this.count = count;
            this.request(0L);
        }
        
        @Override
        public void onNext(T t) {
            List<T> b = buffer;
            if (b == null) {
                b = new ArrayList<T>(count);
                buffer = b;
            }
            
            b.add(t);
            
            if (b.size() == count) {
                buffer = null;
                actual.onNext(b);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            buffer = null;
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            List<T> b = buffer;
            if (b != null) {
                actual.onNext(b);
            }
            actual.onCompleted();
        }
        
        Producer createProducer() {
            return new Producer() {
                @Override
                public void request(long n) {
                    if (n < 0L) {
                        throw new IllegalArgumentException("n >= required but it was " + n);
                    }
                    if (n != 0L) {
                        long u = BackpressureUtils.multiplyCap(n, count);
                        BufferExact.this.request(u);
                    }
                }
            };
        }
    }
    
    static final class BufferSkip<T> extends Subscriber<T> {
        final Subscriber<? super List<T>> actual;
        final int count;
        final int skip;
        
        long index;
        
        List<T> buffer;

        public BufferSkip(Subscriber<? super List<T>> actual, int count, int skip) {
            this.actual = actual;
            this.count = count;
            this.skip = skip;
            this.request(0L);
        }
        
        @Override
        public void onNext(T t) {
            long i = index;
            List<T> b = buffer;
            if (i == 0) {
                b = new ArrayList<T>(count);
                buffer = b;
            }
            i++;
            if (i == skip) {
                index = 0;
            } else {
                index = i;
            }
            
            if (b != null) {
                b.add(t);
                
                if (b.size() == count) {
                    buffer = null;
                    actual.onNext(b);
                }
            }
        }
        
        @Override
        public void onError(Throwable e) {
            buffer = null;
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            List<T> b = buffer;
            if (b != null) {
                buffer = null;
                actual.onNext(b);
            }
            actual.onCompleted();
        }
        
        Producer createProducer() {
            return new BufferSkipProducer();
        }
        
        final class BufferSkipProducer
        extends AtomicBoolean
        implements Producer {
            /** */
            private static final long serialVersionUID = 3428177408082367154L;

            @Override
            public void request(long n) {
                if (n < 0) {
                    throw new IllegalArgumentException("n >= 0 required but it was " + n);
                }
                if (n != 0) {
                    BufferSkip<T> parent = BufferSkip.this;
                    if (!get() && compareAndSet(false, true)) {
                        long u = BackpressureUtils.multiplyCap(n, parent.count);
                        long v = BackpressureUtils.multiplyCap(parent.skip - parent.count, n - 1);
                        long w = BackpressureUtils.addCap(u, v);
                        parent.request(w);
                    } else {
                        long u = BackpressureUtils.multiplyCap(n, parent.skip);
                        parent.request(u);
                    }
                }
            }
        }
    }
    
    static final class BufferOverlap<T> extends Subscriber<T> {
        final Subscriber<? super List<T>> actual;
        final int count;
        final int skip;
        
        long index;
        
        final ArrayDeque<List<T>> queue;
        
        final AtomicLong requested;
        
        long produced;

        public BufferOverlap(Subscriber<? super List<T>> actual, int count, int skip) {
            this.actual = actual;
            this.count = count;
            this.skip = skip;
            this.queue = new ArrayDeque<List<T>>();
            this.requested = new AtomicLong();
            this.request(0L);
        }

        @Override
        public void onNext(T t) {
            long i = index;
            if (i == 0) {
                List<T> b = new ArrayList<T>(count);
                queue.offer(b);
            }
            i++;
            if (i == skip) {
                index = 0;
            } else {
                index = i;
            }
            
            for (List<T> list : queue) {
                list.add(t);
            }
            
            List<T> b = queue.peek();
            if (b != null && b.size() == count) {
                queue.poll();
                produced++;
                actual.onNext(b);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            queue.clear();
            
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            long p = produced;
            
            if (p != 0L) {
                if (p > requested.get()) {
                    actual.onError(new MissingBackpressureException("More produced than requested? " + p));
                    return;
                }
                requested.addAndGet(-p);
            }
            
            BackpressureUtils.postCompleteDone(requested, queue, actual);
        }
        
        Producer createProducer() {
            return new BufferOverlapProducer();
        }
        
        final class BufferOverlapProducer extends AtomicBoolean implements Producer {

            /** */
            private static final long serialVersionUID = -4015894850868853147L;

            @Override
            public void request(long n) {
                BufferOverlap<T> parent = BufferOverlap.this;
                if (BackpressureUtils.postCompleteRequest(parent.requested, n, parent.queue, parent.actual)) {
                    if (n != 0L) {
                        if (!get() && compareAndSet(false, true)) {
                            long u = BackpressureUtils.multiplyCap(parent.skip, n - 1);
                            long v = BackpressureUtils.addCap(u, parent.count);
                            
                            parent.request(v);
                        } else {
                            long u = BackpressureUtils.multiplyCap(parent.skip, n);
                            parent.request(u);
                        }
                    }
                }
            }
            
        }
    }
}
