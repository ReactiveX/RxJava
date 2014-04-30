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
package rx.operators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.SerializedSubscriber;

/**
 * This operation takes
 * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
 * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
 * The creation of chunks is also periodical. How often this is done depends on the specified timeshift.
 * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
 * the event is propagated to all subscribed {@link Subscriber}s.
 * <p>
 * Note that this operation can produce <strong>non-connected, or overlapping chunks</strong> depending
 * on the input parameters.
 * </p>
 * 
 * @param <T> the buffered value type
 */
public final class OperatorBufferWithTime<T> implements Operator<List<T>, T> {
    final long timespan;
    final long timeshift;
    final TimeUnit unit;
    final int count;
    final Scheduler scheduler;

    /**
     * @param timespan
     *            the amount of time all chunks must be actively collect values before being emitted
     * @param timeshift
     *            the amount of time between creating chunks
     * @param unit
     *            the {@link TimeUnit} defining the unit of time for the timespan
     * @param count
     *            the maximum size of the buffer. Once a buffer reaches this size, it is emitted
     * @param scheduler
     *            the {@link Scheduler} to use for timing chunks
     */
    public OperatorBufferWithTime(long timespan, long timeshift, TimeUnit unit, int count, Scheduler scheduler) {
        this.timespan = timespan;
        this.timeshift = timeshift;
        this.unit = unit;
        this.count = count;
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super List<T>> child) {
        final Worker inner = scheduler.createWorker();
        child.add(inner);
        
        if (timespan == timeshift) {
            ExactSubscriber bsub = new ExactSubscriber(new SerializedSubscriber<List<T>>(child), inner);
            bsub.scheduleExact();
            return bsub;
        }
        
        InexactSubscriber bsub = new InexactSubscriber(new SerializedSubscriber<List<T>>(child), inner);
        bsub.startNewChunk();
        bsub.scheduleChunk();
        return bsub;
    }
    /** Subscriber when the buffer chunking time and lenght differ. */
    final class InexactSubscriber extends Subscriber<T> {
        final Subscriber<? super List<T>> child;
        final Worker inner;
        /** Guarded by this. */
        final List<List<T>> chunks;
        /** Guarded by this. */
        boolean done;
        public InexactSubscriber(Subscriber<? super List<T>> child, Worker inner) {
            super(child);
            this.child = child;
            this.inner = inner;
            this.chunks = new LinkedList<List<T>>();
        }

        @Override
        public void onNext(T t) {
            List<List<T>> sizeReached = null;
            synchronized (this) {
                if (done) {
                    return;
                }
                Iterator<List<T>> it = chunks.iterator();
                while (it.hasNext()) {
                    List<T> chunk = it.next();
                    chunk.add(t);
                    if (chunk.size() == count) {
                        it.remove();
                        if (sizeReached == null) {
                            sizeReached = new LinkedList<List<T>>();
                        }
                        sizeReached.add(chunk);
                    }
                }
            }
            if (sizeReached != null) {
                for (List<T> chunk : sizeReached) {
                    child.onNext(chunk);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            synchronized (this) {
                if (done) {
                    return;
                }
                done = true;
                chunks.clear();
            }
            child.onError(e);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            try {
                List<List<T>> sizeReached;
                synchronized (this) {
                    if (done) {
                        return;
                    }
                    done = true;
                    sizeReached = new LinkedList<List<T>>(chunks);
                    chunks.clear();
                }
                for (List<T> chunk : sizeReached) {
                    child.onNext(chunk);
                }
            } catch (Throwable t) {
                child.onError(t);
                return;
            }
            child.onCompleted();
            unsubscribe();
        }
        void scheduleChunk() {
            inner.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    startNewChunk();
                }
            }, timeshift, timeshift, unit);
        }
        void startNewChunk() {
            final List<T> chunk = new ArrayList<T>();
            synchronized (this) {
                if (done) {
                    return;
                }
                chunks.add(chunk);
            }
            inner.schedule(new Action0() {
                @Override
                public void call() {
                    emitChunk(chunk);
                }
            }, timespan, unit);
        }
        void emitChunk(List<T> chunkToEmit) {
            boolean emit = false;
            synchronized (this) {
                if (done) {
                    return;
                }
                Iterator<List<T>> it = chunks.iterator();
                while (it.hasNext()) {
                    List<T> chunk = it.next();
                    if (chunk == chunkToEmit) {
                        it.remove();
                        emit = true;
                        break;
                    }
                }
            }
            if (emit) {
                try {
                    child.onNext(chunkToEmit);
                } catch (Throwable t) {
                    onError(t);
                }
            }
        }
    }
    /** Subscriber when exact timed chunking is required. */
    final class ExactSubscriber extends Subscriber<T> {
        final Subscriber<? super List<T>> child;
        final Worker inner;
        /** Guarded by this. */
        List<T> chunk;
        /** Guarded by this. */
        boolean done;
        public ExactSubscriber(Subscriber<? super List<T>> child, Worker inner) {
            super(child);
            this.child = child;
            this.inner = inner;
            this.chunk = new ArrayList<T>();
        }

        @Override
        public void onNext(T t) {
            List<T> toEmit = null;
            synchronized (this) {
                if (done) {
                    return;
                }
                chunk.add(t);
                if (chunk.size() == count) {
                    toEmit = chunk;
                    chunk = new ArrayList<T>();
                }
            }
            if (toEmit != null) {
                child.onNext(toEmit);
            }
        }

        @Override
        public void onError(Throwable e) {
            synchronized (this) {
                if (done) {
                    return;
                }
                done = true;
                chunk = null;
            }
            child.onError(e);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            try {
                inner.unsubscribe();
                List<T> toEmit;
                synchronized (this) {
                    if (done) {
                        return;
                    }
                    done = true;
                    toEmit = chunk;
                    chunk = null;
                }
                child.onNext(toEmit);
            } catch (Throwable t) {
                child.onError(t);
                return;
            }
            child.onCompleted();
            unsubscribe();
        }
        void scheduleExact() {
            inner.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    emit();
                }
            }, timespan, timespan, unit);
        }
        void emit() {
            List<T> toEmit;
            synchronized (this) {
                if (done) {
                    return;
                }
                toEmit = chunk;
                chunk = new ArrayList<T>();
            }
            try {
                child.onNext(toEmit);
            } catch (Throwable t) {
                onError(t);
            }
        }
    }
}
