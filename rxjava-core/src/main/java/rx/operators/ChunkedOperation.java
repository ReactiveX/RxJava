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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * The base class for operations that break observables into "chunks". Currently buffers and windows.
 */
public class ChunkedOperation {
    /**
     * This interface defines a way which specifies when to create a new internal {@link rx.operators.ChunkedOperation.Chunk} object.
     * 
     */
    protected interface ChunkCreator {
        /**
         * Signifies a onNext event.
         */
        void onValuePushed();

        /**
         * Signifies a onCompleted or onError event. Should be used to clean up open
         * subscriptions and other still running background tasks.
         */
        void stop();
    }

    /**
     * This class represents a single chunk: A sequence of recorded values.
     * 
     * @param <T>
     *            The type of objects which this {@link Chunk} can hold.
     * @param <C>
     *            The type of object being tracked by the {@link Chunk}
     */
    protected abstract static class Chunk<T, C> {
        protected final List<T> contents = new ArrayList<T>();

        /**
         * Appends a specified value to the {@link Chunk}.
         * 
         * @param value
         *            The value to append to the {@link Chunk}.
         */
        public void pushValue(T value) {
            contents.add(value);
        }

        /**
         * @return
         *         The mutable underlying {@code C} which contains all the
         *         recorded values in this {@link Chunk} object.
         */
        abstract public C getContents();

        /**
         * @return
         *         The size of the underlying {@link List} which contains all the
         *         recorded values in this {@link Chunk} object.
         */
        public int size() {
            return contents.size();
        }
    }

    /**
     * This class is an extension on the {@link rx.operators.ChunkedOperation.Chunks} class which only supports one
     * active (not yet emitted) internal {@link rx.operators.ChunkedOperation.Chunks} object.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunks} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class NonOverlappingChunks<T, C> extends Chunks<T, C> {

        private final Object lock = new Object();

        public NonOverlappingChunks(Observer<? super C> observer, Func0<? extends Chunk<T, C>> chunkMaker) {
            super(observer, chunkMaker);
        }

        public Chunk<T, C> emitAndReplaceChunk() {
            synchronized (lock) {
                emitChunk(getChunk());
                return createChunk();
            }
        }

        @Override
        public void pushValue(T value) {
            synchronized (lock) {
                super.pushValue(value);
            }
        }
    }

    /**
     * This class is an extension on the {@link rx.operators.ChunkedOperation.Chunks} class which actually has no additional
     * behavior than its super class. Classes extending this class, are expected to support
     * two or more active (not yet emitted) internal {@link rx.operators.ChunkedOperation.Chunks} objects.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunks} objects record.
     *            <C> The type of object being tracked by the {@link rx.operators.ChunkedOperation.Chunk}
     */
    protected static class OverlappingChunks<T, C> extends Chunks<T, C> {
        public OverlappingChunks(Observer<? super C> observer, Func0<? extends Chunk<T, C>> chunkMaker) {
            super(observer, chunkMaker);
        }
    }

    /**
     * This class is an extension on the {@link rx.operators.ChunkedOperation.Chunks} class. Every internal chunk has
     * a has a maximum time to live and a maximum internal capacity. When the chunk has
     * reached the end of its life, or reached its maximum internal capacity it is
     * automatically emitted.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class TimeAndSizeBasedChunks<T, C> extends Chunks<T, C> implements Subscription {

        private final ConcurrentMap<Chunk<T, C>, Subscription> subscriptions = new ConcurrentHashMap<Chunk<T, C>, Subscription>();

        private final Scheduler scheduler;
        private final long maxTime;
        private final TimeUnit unit;
        private final int maxSize;
        private volatile boolean unsubscribed = false;

        public TimeAndSizeBasedChunks(Observer<? super C> observer, Func0<? extends Chunk<T, C>> chunkMaker, int maxSize, long maxTime, TimeUnit unit, Scheduler scheduler) {
            super(observer, chunkMaker);
            this.maxSize = maxSize;
            this.maxTime = maxTime;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Chunk<T, C> createChunk() {
            final Chunk<T, C> chunk = super.createChunk();
            subscriptions.put(chunk, scheduler.schedule(new Action1<Inner>() {
                @Override
                public void call(Inner inner) {
                    emitChunk(chunk);
                }
            }, maxTime, unit));
            return chunk;
        }

        @Override
        public void emitChunk(Chunk<T, C> chunk) {
            Subscription subscription = subscriptions.remove(chunk);
            if (subscription == null) {
                // Chunk was already emitted.
                return;
            }

            // Fixed issue 428.
            // As unsubscribe will cancel the Future, and the currrent thread's interrupt status
            // will be set. So we need to emit the chunk before unsubscribe.
            super.emitChunk(chunk);
            subscription.unsubscribe();
            createChunk();
        }

        @Override
        public void pushValue(T value) {
            super.pushValue(value);

            Chunk<T, C> chunk;
            while ((chunk = getChunk()) != null) {
                if (chunk.size() >= maxSize) {
                    emitChunk(chunk);
                } else {
                    // Chunk is not at full capacity yet, and neither will remaining chunks be so we can terminate.
                    break;
                }
            }
        }

        @Override
        public void unsubscribe() {
            unsubscribed = true;
            for (Subscription s : subscriptions.values()) {
                s.unsubscribe();
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }
    }

    /**
     * This class is an extension on the {@link rx.operators.ChunkedOperation.Chunks} class. Every internal chunk has
     * a has a maximum time to live. When the chunk has reached the end of its life it is
     * automatically emitted.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class TimeBasedChunks<T, C> extends OverlappingChunks<T, C> implements Subscription {

        private final ConcurrentMap<Chunk<T, C>, Subscription> subscriptions = new ConcurrentHashMap<Chunk<T, C>, Subscription>();

        private final Scheduler scheduler;
        private final long time;
        private final TimeUnit unit;
        private volatile boolean unsubscribed = false;

        public TimeBasedChunks(Observer<? super C> observer, Func0<? extends Chunk<T, C>> chunkMaker, long time, TimeUnit unit, Scheduler scheduler) {
            super(observer, chunkMaker);
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Chunk<T, C> createChunk() {
            final Chunk<T, C> chunk = super.createChunk();
            subscriptions.put(chunk, scheduler.schedule(new Action1<Inner>() {
                @Override
                public void call(Inner inner) {
                    emitChunk(chunk);
                }
            }, time, unit));
            return chunk;
        }

        @Override
        public void emitChunk(Chunk<T, C> chunk) {
            subscriptions.remove(chunk);
            super.emitChunk(chunk);
        }

        @Override
        public void unsubscribe() {
            unsubscribed = true;
            for (Subscription s : subscriptions.values()) {
                s.unsubscribe();
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }

    }

    /**
     * This class is an extension on the {@link rx.operators.ChunkedOperation.Chunks} class. Every internal chunk has
     * a fixed maximum capacity. When the chunk has reached its maximum capacity it is
     * automatically emitted.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class SizeBasedChunks<T, C> extends Chunks<T, C> {

        private final int size;

        public SizeBasedChunks(Observer<? super C> observer, Func0<? extends Chunk<T, C>> chunkMaker, int size) {
            super(observer, chunkMaker);
            this.size = size;
        }

        @Override
        public void pushValue(T value) {
            super.pushValue(value);

            Chunk<T, C> chunk;
            while ((chunk = getChunk()) != null) {
                if (chunk.size() >= size) {
                    emitChunk(chunk);
                } else {
                    // Chunk is not at full capacity yet, and neither will remaining chunks be so we can terminate.
                    break;
                }
            }
        }
    }

    /**
     * This class represents an object which contains and manages multiple {@link rx.operators.ChunkedOperation.Chunk} objects.
     * 
     * @param <T>
     *            The type of objects which the internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class Chunks<T, C> {

        private final Queue<Chunk<T, C>> chunks = new ConcurrentLinkedQueue<Chunk<T, C>>();
        private final Observer<? super C> observer;
        private final Func0<? extends Chunk<T, C>> chunkMaker;

        /**
         * Constructs a new {@link ChunkedOperation.Chunks} object for the specified {@link rx.Observer}.
         * 
         * @param observer
         *            The {@link rx.Observer} to which this object will emit its internal {@link rx.operators.ChunkedOperation.Chunk} objects to when requested.
         */
        public Chunks(Observer<? super C> observer, Func0<? extends Chunk<T, C>> chunkMaker) {
            this.observer = observer;
            this.chunkMaker = chunkMaker;
        }

        /**
         * This method will instantiate a new {@link rx.operators.ChunkedOperation.Chunk} object and register it internally.
         * 
         * @return
         *         The constructed empty {@link rx.operators.ChunkedOperation.Chunk} object.
         */
        public Chunk<T, C> createChunk() {
            Chunk<T, C> chunk = chunkMaker.call();
            chunks.add(chunk);
            return chunk;
        }

        /**
         * This method emits all not yet emitted {@link rx.operators.ChunkedOperation.Chunk} objects.
         */
        public void emitAllChunks() {
            Chunk<T, C> chunk;
            while ((chunk = chunks.poll()) != null) {
                observer.onNext(chunk.getContents());
            }
        }

        /**
         * This method emits the specified {@link rx.operators.ChunkedOperation.Chunk} object.
         * 
         * @param chunk
         *            The {@link rx.operators.ChunkedOperation.Chunk} to emit.
         */
        public void emitChunk(Chunk<T, C> chunk) {
            if (!chunks.remove(chunk)) {
                // Concurrency issue: Chunk is already emitted!
                return;
            }
            observer.onNext(chunk.getContents());
        }

        /**
         * @return
         *         The oldest (in case there are multiple) {@link rx.operators.ChunkedOperation.Chunk} object.
         */
        public Chunk<T, C> getChunk() {
            return chunks.peek();
        }

        /**
         * This method pushes a value to all not yet emitted {@link rx.operators.ChunkedOperation.Chunk} objects.
         * 
         * @param value
         *            The value to push to all not yet emitted {@link rx.operators.ChunkedOperation.Chunk} objects.
         */
        public void pushValue(T value) {
            List<Chunk<T, C>> copy = new ArrayList<Chunk<T, C>>(chunks);
            for (Chunk<T, C> chunk : copy) {
                chunk.pushValue(value);
            }
        }
    }

    /**
     * This {@link rx.operators.ChunkedOperation.ChunkObserver} object can be constructed using a {@link rx.operators.ChunkedOperation.Chunks} object,
     * a {@link rx.Observer} object, and a {@link rx.operators.ChunkedOperation.ChunkCreator} object. The {@link rx.operators.ChunkedOperation.ChunkCreator} will manage the creation, and in some rare
     * cases emission of internal {@link rx.operators.ChunkedOperation.Chunk} objects
     * in the specified {@link rx.operators.ChunkedOperation.Chunks} object. Under normal circumstances the {@link rx.operators.ChunkedOperation.Chunks} object specifies when a created
     * {@link rx.operators.ChunkedOperation.Chunk} is emitted.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class ChunkObserver<T, C> implements Observer<T> {

        private final Chunks<T, C> chunks;
        private final Observer<? super C> observer;
        private final ChunkCreator creator;

        public ChunkObserver(Chunks<T, C> chunks, Observer<? super C> observer, ChunkCreator creator) {
            this.observer = observer;
            this.creator = creator;
            this.chunks = chunks;
        }

        @Override
        public void onCompleted() {
            creator.stop();
            chunks.emitAllChunks();
            observer.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            creator.stop();
            chunks.emitAllChunks();
            observer.onError(e);
        }

        @Override
        public void onNext(T args) {
            creator.onValuePushed();
            chunks.pushValue(args);
        }
    }

    /**
     * This {@link rx.operators.ChunkedOperation.ChunkCreator} creates a new {@link rx.operators.ChunkedOperation.Chunk} when it is initialized, but
     * provides no additional functionality. This class should primarily be used when the
     * internal {@link rx.operators.ChunkedOperation.Chunk} is closed externally.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class SingleChunkCreator<T, C> implements ChunkCreator {

        public SingleChunkCreator(Chunks<T, C> chunks) {
            chunks.createChunk();
        }

        @Override
        public void onValuePushed() {
            // Do nothing.
        }

        @Override
        public void stop() {
            // Do nothing.
        }
    }

    /**
     * This {@link rx.operators.ChunkedOperation.ChunkCreator} creates a new {@link rx.operators.ChunkedOperation.Chunk} whenever it receives an
     * object from the provided {@link rx.Observable} created with the
     * chunkClosingSelector {@link rx.functions.Func0}.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class ObservableBasedSingleChunkCreator<T, C, TClosing> implements ChunkCreator {

        private final SafeObservableSubscription subscription = new SafeObservableSubscription();
        private final Func0<? extends Observable<? extends TClosing>> chunkClosingSelector;
        private final NonOverlappingChunks<T, C> chunks;

        public ObservableBasedSingleChunkCreator(NonOverlappingChunks<T, C> chunks, Func0<? extends Observable<? extends TClosing>> chunkClosingSelector) {
            this.chunks = chunks;
            this.chunkClosingSelector = chunkClosingSelector;

            chunks.createChunk();
            listenForChunkEnd();
        }

        private void listenForChunkEnd() {
            Observable<? extends TClosing> closingObservable = chunkClosingSelector.call();
            closingObservable.subscribe(new Action1<TClosing>() {
                @Override
                public void call(TClosing closing) {
                    chunks.emitAndReplaceChunk();
                    listenForChunkEnd();
                }
            });
        }

        @Override
        public void onValuePushed() {
            // Ignore value pushes.
        }

        @Override
        public void stop() {
            subscription.unsubscribe();
        }
    }

    /**
     * This {@link rx.operators.ChunkedOperation.ChunkCreator} creates a new {@link rx.operators.ChunkedOperation.Chunk} whenever it receives
     * an object from the provided chunkOpenings {@link rx.Observable}, and closes the corresponding {@link rx.operators.ChunkedOperation.Chunk} object when it receives an object from the provided
     * {@link rx.Observable} created
     * with the chunkClosingSelector {@link rx.functions.Func1}.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class ObservableBasedMultiChunkCreator<T, C, TOpening, TClosing> implements ChunkCreator {

        private final SafeObservableSubscription subscription = new SafeObservableSubscription();

        public ObservableBasedMultiChunkCreator(final OverlappingChunks<T, C> chunks, Observable<? extends TOpening> openings, final Func1<? super TOpening, ? extends Observable<? extends TClosing>> chunkClosingSelector) {
            subscription.wrap(openings.subscribe(new Action1<TOpening>() {
                @Override
                public void call(TOpening opening) {
                    final Chunk<T, C> chunk = chunks.createChunk();
                    Observable<? extends TClosing> closingObservable = chunkClosingSelector.call(opening);

                    closingObservable.subscribe(new Action1<TClosing>() {
                        @Override
                        public void call(TClosing closing) {
                            chunks.emitChunk(chunk);
                        }
                    });
                }
            }));
        }

        @Override
        public void onValuePushed() {
            // Ignore value pushes.
        }

        @Override
        public void stop() {
            subscription.unsubscribe();
        }
    }

    /**
     * This {@link rx.operators.ChunkedOperation.ChunkCreator} creates a new {@link rx.operators.ChunkedOperation.Chunk} every time after a fixed
     * period of time has elapsed.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class TimeBasedChunkCreator<T, C> implements ChunkCreator {

        private final SafeObservableSubscription subscription = new SafeObservableSubscription();

        public TimeBasedChunkCreator(final NonOverlappingChunks<T, C> chunks, long time, TimeUnit unit, Scheduler scheduler) {
            this.subscription.wrap(scheduler.schedulePeriodically(new Action1<Inner>() {
                @Override
                public void call(Inner inner) {
                    chunks.emitAndReplaceChunk();
                }
            }, 0, time, unit));
        }

        public TimeBasedChunkCreator(final OverlappingChunks<T, C> chunks, long time, TimeUnit unit, Scheduler scheduler) {
            this.subscription.wrap(scheduler.schedulePeriodically(new Action1<Inner>() {
                @Override
                public void call(Inner inner) {
                    chunks.createChunk();
                }
            }, 0, time, unit));
        }

        @Override
        public void onValuePushed() {
            // Do nothing: chunks are created periodically.
        }

        @Override
        public void stop() {
            subscription.unsubscribe();
        }
    }

    /**
     * This {@link rx.operators.ChunkedOperation.ChunkCreator} creates a new {@link rx.operators.ChunkedOperation.Chunk} every time after it has
     * seen a certain amount of elements.
     * 
     * @param <T>
     *            The type of object all internal {@link rx.operators.ChunkedOperation.Chunk} objects record.
     *            <C> The type of object being tracked by the {@link Chunk}
     */
    protected static class SkippingChunkCreator<T, C> implements ChunkCreator {

        private final AtomicInteger skipped = new AtomicInteger(1);
        private final Chunks<T, C> chunks;
        private final int skip;

        public SkippingChunkCreator(Chunks<T, C> chunks, int skip) {
            this.chunks = chunks;
            this.skip = skip;
        }

        @Override
        public void onValuePushed() {
            if (skipped.decrementAndGet() == 0) {
                skipped.set(skip);
                chunks.createChunk();
            }
        }

        @Override
        public void stop() {
            // Nothing to stop: we're not using a Scheduler.
        }
    }
}
