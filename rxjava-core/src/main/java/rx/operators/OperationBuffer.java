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
package rx.operators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public final class OperationBuffer extends ChunkedOperation {

    private static <T> Func0<Buffer<T>> bufferMaker() {
        return new Func0<Buffer<T>>() {
            @Override
            public Buffer<T> call() {
                return new Buffer<T>();
            }
        };
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer until the
     * {@link Observable} constructed using the {@link Func0} argument, produces a value. The buffer is then
     * emitted, and a new buffer is created to replace it. A new {@link Observable} will be constructed using
     * the provided {@link Func0} object, which will determine when this new buffer is emitted. When the source
     * {@link Observable} completes or produces an error, the current buffer is emitted, and the event is
     * propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param bufferClosingSelector
     *            a {@link Func0} object which produces {@link Observable}s. These {@link Observable}s determine
     *            when a buffer is emitted and replaced by simply producing an object.
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T, TClosing> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final Func0<? extends Observable<? extends TClosing>> bufferClosingSelector) {
        return new OnSubscribeFunc<List<T>>() {

            @Override
            public Subscription onSubscribe(Observer<? super List<T>> observer) {
                NonOverlappingChunks<T, List<T>> buffers = new NonOverlappingChunks<T, List<T>>(observer, OperationBuffer.<T> bufferMaker());
                ChunkCreator creator = new ObservableBasedSingleChunkCreator<T, List<T>, TClosing>(buffers, bufferClosingSelector);
                return new CompositeSubscription(
                        new ChunkToSubscription(creator),
                        source.subscribe(new ChunkObserver<T, List<T>>(buffers, observer, creator)));
            }
        };
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in the currently active chunks.
     * Initially there are no chunks active.
     * <p>
     * Chunks can be created by pushing a {@link rx.util.TOpening} value to the "bufferOpenings"
     * {@link Observable}. This creates a new buffer which will then start recording values which are produced
     * by the "source" {@link Observable}. Additionally the "bufferClosingSelector" will be used to construct an
     * {@link Observable} which can produce values. When it does so it will close this (and only this) newly
     * created buffer. When the source {@link Observable} completes or produces an error, all chunks are
     * emitted, and the event is propagated to all subscribed {@link Observer}s.
     * </p><p>
     * Note that when using this operation <strong>multiple overlapping chunks</strong> could be active at any
     * one point.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param bufferOpenings
     *            an {@link Observable} which when it produces a {@link rx.util.TOpening} value will create a
     *            new buffer which instantly starts recording the "source" {@link Observable}
     * @param bufferClosingSelector
     *            a {@link Func0} object which produces {@link Observable}s. These {@link Observable}s determine
     *            when a buffer is emitted and replaced by simply producing an object.
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T, TOpening, TClosing> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final Observable<? extends TOpening> bufferOpenings, final Func1<? super TOpening, ? extends Observable<? extends TClosing>> bufferClosingSelector) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                OverlappingChunks<T, List<T>> buffers = new OverlappingChunks<T, List<T>>(observer, OperationBuffer.<T> bufferMaker());
                ChunkCreator creator = new ObservableBasedMultiChunkCreator<T, List<T>, TOpening, TClosing>(buffers, bufferOpenings, bufferClosingSelector);
                return new CompositeSubscription(
                        new ChunkToSubscription(creator),
                        source.subscribe(new ChunkObserver<T, List<T>>(buffers, observer, creator)));
            }
        };
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer until the buffer contains
     * a specified number of elements. The buffer is then emitted, and a new buffer is created to replace it.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param count
     *            the number of elements a buffer should have before being emitted and replaced
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(Observable<T> source, int count) {
        return buffer(source, count, count);
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in all active chunks until the buffer
     * contains a specified number of elements. The buffer is then emitted. Chunks are created after a certain
     * amount of values have been received. When the source {@link Observable} completes or produces an error,
     * the currently active chunks are emitted, and the event is propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation can produce <strong>non-connected, connected non-overlapping, or overlapping
     * chunks</strong> depending on the input parameters.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param count
     *            the number of elements a buffer should have before being emitted
     * @param skip
     *            the interval with which chunks have to be created. Note that when {@code skip == count} that
     *            this is the same as calling {@link OperationBuffer#buffer(Observable, int)}. If
     *            {@code skip < count}, this buffer operation will produce overlapping chunks and if
     *            {@code skip > count} non-overlapping chunks will be created and some values will not be pushed
     *            into a buffer at all!
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final int count, final int skip) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                Chunks<T, List<T>> chunks = new SizeBasedChunks<T, List<T>>(observer, OperationBuffer.<T> bufferMaker(), count);
                ChunkCreator creator = new SkippingChunkCreator<T, List<T>>(chunks, skip);
                return new CompositeSubscription(
                        new ChunkToSubscription(creator),
                        source.subscribe(new ChunkObserver<T, List<T>>(chunks, observer, creator)));
            }
        };
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param timespan
     *            the amount of time all chunks must be actively collect values before being emitted
     * @param unit
     *            the {@link TimeUnit} defining the unit of time for the timespan
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(Observable<T> source, long timespan, TimeUnit unit) {
        return buffer(source, timespan, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param timespan
     *            the amount of time all chunks must be actively collect values before being emitted
     * @param unit
     *            the {@link TimeUnit} defining the unit of time for the timespan
     * @param scheduler
     *            the {@link Scheduler} to use for timing chunks
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final long timespan, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                NonOverlappingChunks<T, List<T>> buffers = new NonOverlappingChunks<T, List<T>>(observer, OperationBuffer.<T> bufferMaker());
                ChunkCreator creator = new TimeBasedChunkCreator<T, List<T>>(buffers, timespan, unit, scheduler);
                return new CompositeSubscription(
                        new ChunkToSubscription(creator),
                        source.subscribe(new ChunkObserver<T, List<T>>(buffers, observer, creator)));
            }
        };
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * Additionally the buffer is automatically emitted once it reaches a specified number of elements.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param timespan
     *            the amount of time all chunks must be actively collect values before being emitted
     * @param unit
     *            the {@link TimeUnit} defining the unit of time for the timespan
     * @param count
     *            the maximum size of the buffer. Once a buffer reaches this size, it is emitted
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(Observable<T> source, long timespan, TimeUnit unit, int count) {
        return buffer(source, timespan, unit, count, Schedulers.threadPoolForComputation());
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * Additionally the buffer is automatically emitted once it reaches a specified number of elements.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param timespan
     *            the amount of time all chunks must be actively collect values before being emitted
     * @param unit
     *            the {@link TimeUnit} defining the unit of time for the timespan
     * @param count
     *            the maximum size of the buffer. Once a buffer reaches this size, it is emitted
     * @param scheduler
     *            the {@link Scheduler} to use for timing chunks
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final long timespan, final TimeUnit unit, final int count, final Scheduler scheduler) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                TimeAndSizeBasedChunks<T, List<T>> chunks = new TimeAndSizeBasedChunks<T, List<T>>(observer, OperationBuffer.<T> bufferMaker(), count, timespan, unit, scheduler);
                ChunkCreator creator = new SingleChunkCreator<T, List<T>>(chunks);
                return new CompositeSubscription(
                        chunks,
                        new ChunkToSubscription(creator),
                        source.subscribe(new ChunkObserver<T, List<T>>(chunks, observer, creator)));
            }
        };
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * The creation of chunks is also periodical. How often this is done depends on the specified timeshift.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation can produce <strong>non-connected, or overlapping chunks</strong> depending
     * on the input parameters.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param timespan
     *            the amount of time all chunks must be actively collect values before being emitted
     * @param timeshift
     *            the amount of time between creating chunks
     * @param unit
     *            the {@link TimeUnit} defining the unit of time for the timespan
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(Observable<T> source, long timespan, long timeshift, TimeUnit unit) {
        return buffer(source, timespan, timeshift, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * The creation of chunks is also periodical. How often this is done depends on the specified timeshift.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.
     * <p>
     * Note that this operation can produce <strong>non-connected, or overlapping chunks</strong> depending
     * on the input parameters.
     * </p>
     * 
     * @param source
     *            the {@link Observable} which produces values
     * @param timespan
     *            the amount of time all chunks must be actively collect values before being emitted
     * @param timeshift
     *            the amount of time between creating chunks
     * @param unit
     *            the {@link TimeUnit} defining the unit of time for the timespan
     * @param scheduler
     *            the {@link Scheduler} to use for timing chunks
     * @return
     *         the {@link Func1} object representing the specified buffer operation
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final long timespan, final long timeshift, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                TimeBasedChunks<T, List<T>> buffers = new TimeBasedChunks<T, List<T>>(observer, OperationBuffer.<T> bufferMaker(), timespan, unit, scheduler);
                ChunkCreator creator = new TimeBasedChunkCreator<T, List<T>>(buffers, timeshift, unit, scheduler);
                return new CompositeSubscription(
                        buffers,
                        new ChunkToSubscription(creator),
                        source.subscribe(new ChunkObserver<T, List<T>>(buffers, observer, creator)));
            }
        };
    }

    /**
     * This class represents a single buffer: A sequence of recorded values.
     * 
     * @param <T>
     *            the type of objects which this {@link Buffer} can hold
     */
    protected static class Buffer<T> extends Chunk<T, List<T>> {
        /**
         * @return
         *         The mutable underlying {@link List} which contains all the
         *         recorded values in this {@link Buffer} object.
         */
        @Override
        public List<T> getContents() {
            return contents;
        }
    }

    /**
     * Converts a chunk creator into a subscription which stops the chunk.
     */
    private static class ChunkToSubscription implements Subscription {
        private ChunkCreator cc;
        private final AtomicBoolean done;

        public ChunkToSubscription(ChunkCreator cc) {
            this.cc = cc;
            this.done = new AtomicBoolean();
        }

        @Override
        public void unsubscribe() {
            if (done.compareAndSet(false, true)) {
                ChunkCreator cc0 = cc;
                cc = null;
                cc0.stop();
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return done.get();
        }
    }

    /**
     * Create a buffer operator with the given observable sequence as the buffer boundary.
     *
     * @param source
     * @param boundary
     * @return
     */
    public static <T, B> OnSubscribeFunc<List<T>> bufferWithBoundaryObservable(Observable<? extends T> source, Observable<B> boundary) {
        return new BufferWithObservableBoundary<T, B>(source, boundary, 16);
    }

    /**
     * Create a buffer operator with the given observable sequence as the buffer boundary and
     * with the given initial capacity for buffers.
     *
     * @param source
     * @param boundary
     * @param initialCapacity
     * @return
     */
    public static <T, B> OnSubscribeFunc<List<T>> bufferWithBoundaryObservable(Observable<? extends T> source, Observable<B> boundary, int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity > 0 required");
        }
        return new BufferWithObservableBoundary<T, B>(source, boundary, initialCapacity);
    }

    /**
     * Buffer until an element is emitted from a helper observable.
     * 
     * @param <T>
     *            the buffered value type
     */
    private static final class BufferWithObservableBoundary<T, B> implements OnSubscribeFunc<List<T>> {
        final Observable<? extends T> source;
        final Observable<B> boundary;
        final int initialCapacity;

        public BufferWithObservableBoundary(Observable<? extends T> source, Observable<B> boundary, int initialCapacity) {
            this.source = source;
            this.boundary = boundary;
            this.initialCapacity = initialCapacity;
        }

        @Override
        public Subscription onSubscribe(Observer<? super List<T>> t1) {
            CompositeSubscription csub = new CompositeSubscription();

            SourceObserver<T> so = new SourceObserver<T>(t1, initialCapacity, csub);
            csub.add(source.subscribe(so));
            csub.add(boundary.subscribe(new BoundaryObserver<B>(so)));

            return csub;
        }

        /**
         * Observes the source.
         */
        private static final class SourceObserver<T> implements Observer<T> {
            final Observer<? super List<T>> observer;
            /** The buffer, if null, that indicates a terminal state. */
            List<T> buffer;
            final int initialCapacity;
            final Object guard;
            final Subscription cancel;

            public SourceObserver(Observer<? super List<T>> observer, int initialCapacity, Subscription cancel) {
                this.observer = observer;
                this.initialCapacity = initialCapacity;
                this.guard = new Object();
                this.cancel = cancel;
                buffer = new ArrayList<T>(initialCapacity);
            }

            @Override
            public void onNext(T args) {
                synchronized (guard) {
                    buffer.add(args);
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    if (buffer == null) {
                        return;
                    }
                    buffer = null;
                }
                observer.onError(e);
                cancel.unsubscribe();
            }

            @Override
            public void onCompleted() {
                emitAndComplete();
                cancel.unsubscribe();
            }

            void emitAndReplace() {
                List<T> buf;
                synchronized (guard) {
                    if (buffer == null) {
                        return;
                    }
                    buf = buffer;
                    buffer = new ArrayList<T>(initialCapacity);
                }
                observer.onNext(buf);
            }

            void emitAndComplete() {
                List<T> buf;
                synchronized (guard) {
                    if (buffer == null) {
                        return;
                    }
                    buf = buffer;
                    buffer = null;
                }
                observer.onNext(buf);
                observer.onCompleted();
            }
        }

        /**
         * Observes the boundary.
         */
        private static final class BoundaryObserver<T> implements Observer<T> {
            final SourceObserver so;

            public BoundaryObserver(SourceObserver so) {
                this.so = so;
            }

            @Override
            public void onNext(T args) {
                so.emitAndReplace();
            }

            @Override
            public void onError(Throwable e) {
                so.onError(e);
            }

            @Override
            public void onCompleted() {
                so.onCompleted();
            }
        }
    }
}
