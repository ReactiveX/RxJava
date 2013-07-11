/**
 * Copyright 2013 Netflix, Inc.
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.BufferClosing;
import rx.util.BufferClosings;
import rx.util.BufferOpening;
import rx.util.BufferOpenings;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;


public final class OperationBuffer {

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer until the {@link Observable}
     * constructed using the {@link Func0} argument, produces a {@link BufferClosing} value. The buffer is then
     * emitted, and a new buffer is created to replace it. A new {@link Observable} will be constructed using the
     * provided {@link Func0} object, which will determine when this new buffer is emitted. When the source
     * {@link Observable} completes or produces an error, the current buffer is emitted, and the event is propagated
     * to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation only produces <strong>non-overlapping buffers</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param bufferClosingSelector
     *            A {@link Func0} object which produces {@link Observable}s. These
     *            {@link Observable}s determine when a buffer is emitted and replaced by simply
     *            producing an {@link BufferClosing} object.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(final Observable<T> source, final Func0<Observable<BufferClosing>> bufferClosingSelector) {
        return new Func1<Observer<List<T>>, Subscription>() {
            @Override
            public Subscription call(final Observer<List<T>> observer) {
                NonOverlappingBuffers<T> buffers = new NonOverlappingBuffers<T>(observer);
                BufferCreator<T> creator = new ObservableBasedSingleBufferCreator<T>(buffers, bufferClosingSelector);
                return source.subscribe(new BufferObserver<T>(buffers, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in the currently active buffers. Initially
     * there are no buffers active.</p>
     *
     * <p>Buffers can be created by pushing a {@link BufferOpening} value to the "bufferOpenings" {@link Observable}.
     * This creates a new buffer which will then start recording values which are produced by the "source"
     * {@link Observable}. Additionally the "bufferClosingSelector" will be used to construct an {@link Observable}
     * which can produce {@link BufferClosing} values. When it does so it will close this (and only this) newly created
     * buffer. When the source {@link Observable} completes or produces an error, all buffers are emitted, and the
     * event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that when using this operation <strong>multiple overlapping buffers</strong>
     * could be active at any one point.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param bufferOpenings
     *            An {@link Observable} which when it produces a {@link BufferOpening} value will
     *            create a new buffer which instantly starts recording the "source" {@link Observable}.
     * @param bufferClosingSelector
     *            A {@link Func0} object which produces {@link Observable}s. These
     *            {@link Observable}s determine when a buffer is emitted and replaced by simply
     *            producing an {@link BufferClosing} object.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(final Observable<T> source, final Observable<BufferOpening> bufferOpenings, final Func1<BufferOpening, Observable<BufferClosing>> bufferClosingSelector) {
        return new Func1<Observer<List<T>>, Subscription>() {
            @Override
            public Subscription call(final Observer<List<T>> observer) {
                OverlappingBuffers<T> buffers = new OverlappingBuffers<T>(observer);
                BufferCreator<T> creator = new ObservableBasedMultiBufferCreator<T>(buffers, bufferOpenings, bufferClosingSelector);
                return source.subscribe(new BufferObserver<T>(buffers, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer until the buffer contains
     * a specified number of elements. The buffer is then emitted, and a new buffer is created to replace it.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation only produces <strong>non-overlapping buffers</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param count
     *            The number of elements a buffer should have before being emitted and replaced.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(Observable<T> source, int count) {
        return buffer(source, count, count);
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in all active buffers until the buffer
     * contains a specified number of elements. The buffer is then emitted. Buffers are created after a certain
     * amount of values have been received. When the source {@link Observable} completes or produces an error, the
     * currently active buffers are emitted, and the event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation can produce <strong>non-connected, connected non-overlapping, or overlapping
     * buffers</strong> depending on the input parameters.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param count
     *            The number of elements a buffer should have before being emitted.
     * @param skip
     *            The interval with which buffers have to be created. Note that when "skip" == "count"
     *            that this is the same as calling {@link OperationBuffer#buffer(Observable, int)}.
     *            If "skip" < "count", this buffer operation will produce overlapping buffers and if "skip"
     *            > "count" non-overlapping buffers will be created and some values will not be pushed
     *            into a buffer at all!
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(final Observable<T> source, final int count, final int skip) {
        return new Func1<Observer<List<T>>, Subscription>() {
            @Override
            public Subscription call(final Observer<List<T>> observer) {
                Buffers<T> buffers = new SizeBasedBuffers<T>(observer, count);
                BufferCreator<T> creator = new SkippingBufferCreator<T>(buffers, skip);
                return source.subscribe(new BufferObserver<T>(buffers, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation only produces <strong>non-overlapping buffers</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all buffers must be actively collect values before being emitted.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(Observable<T> source, long timespan, TimeUnit unit) {
        return buffer(source, timespan, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation only produces <strong>non-overlapping buffers</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all buffers must be actively collect values before being emitted.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @param scheduler
     *            The {@link Scheduler} to use for timing buffers.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(final Observable<T> source, final long timespan, final TimeUnit unit, final Scheduler scheduler) {
        return new Func1<Observer<List<T>>, Subscription>() {
            @Override
            public Subscription call(final Observer<List<T>> observer) {
                NonOverlappingBuffers<T> buffers = new NonOverlappingBuffers<T>(observer);
                BufferCreator<T> creator = new TimeBasedBufferCreator<T>(buffers, timespan, unit, scheduler);
                return source.subscribe(new BufferObserver<T>(buffers, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * Additionally the buffer is automatically emitted once it reaches a specified number of elements.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation only produces <strong>non-overlapping buffers</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all buffers must be actively collect values before being emitted.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @param count
     *            The maximum size of the buffer. Once a buffer reaches this size, it is emitted.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(Observable<T> source, long timespan, TimeUnit unit, int count) {
        return buffer(source, timespan, unit, count, Schedulers.threadPoolForComputation());
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * Additionally the buffer is automatically emitted once it reaches a specified number of elements.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation only produces <strong>non-overlapping buffers</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all buffers must be actively collect values before being emitted.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @param count
     *            The maximum size of the buffer. Once a buffer reaches this size, it is emitted.
     * @param scheduler
     *            The {@link Scheduler} to use for timing buffers.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(final Observable<T> source, final long timespan, final TimeUnit unit, final int count, final Scheduler scheduler) {
        return new Func1<Observer<List<T>>, Subscription>() {
            @Override
            public Subscription call(final Observer<List<T>> observer) {
                Buffers<T> buffers = new TimeAndSizeBasedBuffers<T>(observer, count, timespan, unit, scheduler);
                BufferCreator<T> creator = new SingleBufferCreator<T>(buffers);
                return source.subscribe(new BufferObserver<T>(buffers, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * The creation of buffers is also periodical. How often this is done depends on the specified timeshift.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation can produce <strong>non-connected, or overlapping buffers</strong> depending
     * on the input parameters.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all buffers must be actively collect values before being emitted.
     * @param timeshift
     *            The amount of time between creating buffers.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(Observable<T> source, long timespan, long timeshift, TimeUnit unit) {
        return buffer(source, timespan, timeshift, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * The creation of buffers is also periodical. How often this is done depends on the specified timeshift.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     *
     * <p>Note that this operation can produce <strong>non-connected, or overlapping buffers</strong> depending
     * on the input parameters.</p>
     *
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all buffers must be actively collect values before being emitted.
     * @param timeshift
     *            The amount of time between creating buffers.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @param scheduler
     *            The {@link Scheduler} to use for timing buffers.
     * @return
     *            the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> Func1<Observer<List<T>>, Subscription> buffer(final Observable<T> source, final long timespan, final long timeshift, final TimeUnit unit, final Scheduler scheduler) {
        return new Func1<Observer<List<T>>, Subscription>() {
            @Override
            public Subscription call(final Observer<List<T>> observer) {
                OverlappingBuffers<T> buffers = new TimeBasedBuffers<T>(observer, timespan, unit, scheduler);
                BufferCreator<T> creator = new TimeBasedBufferCreator<T>(buffers, timeshift, unit, scheduler);
                return source.subscribe(new BufferObserver<T>(buffers, observer, creator));
            }
        };
    }

    /**
     * This {@link BufferObserver} object can be constructed using a {@link Buffers} object,
     * a {@link Observer} object, and a {@link BufferCreator} object. The {@link BufferCreator}
     * will manage the creation, and in some rare cases emission of internal {@link Buffer} objects
     * in the specified {@link Buffers} object. Under normal circumstances the {@link Buffers}
     * object specifies when a created {@link Buffer} is emitted.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class BufferObserver<T> implements Observer<T> {

        private final Buffers<T> buffers;
        private final Observer<List<T>> observer;
        private final BufferCreator<T> creator;

        public BufferObserver(Buffers<T> buffers, Observer<List<T>> observer, BufferCreator<T> creator) {
            this.observer = observer;
            this.creator = creator;
            this.buffers = buffers;
        }

        @Override
        public void onCompleted() {
            creator.stop();
            buffers.emitAllBuffers();
            observer.onCompleted();
        }

        @Override
        public void onError(Exception e) {
            creator.stop();
            buffers.emitAllBuffers();
            observer.onError(e);
        }

        @Override
        public void onNext(T args) {
            creator.onValuePushed();
            buffers.pushValue(args);
        }
    }

    /**
     * This interface defines a way which specifies when to create a new internal {@link Buffer} object.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private interface BufferCreator<T> {
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
     * This {@link BufferCreator} creates a new {@link Buffer} when it is initialized, but
     * provides no additional functionality. This class should primarily be used when the
     * internal {@link Buffer} is closed externally.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class SingleBufferCreator<T> implements BufferCreator<T> {

        public SingleBufferCreator(Buffers<T> buffers) {
            buffers.createBuffer();
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
     * This {@link BufferCreator} creates a new {@link Buffer} whenever it receives an
     * object from the provided {@link Observable} created with the
     * bufferClosingSelector {@link Func0}.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class ObservableBasedSingleBufferCreator<T> implements BufferCreator<T> {

        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        private final Func0<Observable<BufferClosing>> bufferClosingSelector;
        private final NonOverlappingBuffers<T> buffers;

        public ObservableBasedSingleBufferCreator(NonOverlappingBuffers<T> buffers, Func0<Observable<BufferClosing>> bufferClosingSelector) {
            this.buffers = buffers;
            this.bufferClosingSelector = bufferClosingSelector;

            buffers.createBuffer();
            listenForBufferEnd();
        }

        private void listenForBufferEnd() {
            Observable<BufferClosing> closingObservable = bufferClosingSelector.call();
            closingObservable.subscribe(new Action1<BufferClosing>() {
                @Override
                public void call(BufferClosing closing) {
                    buffers.emitAndReplaceBuffer();
                    listenForBufferEnd();
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
     * This {@link BufferCreator} creates a new {@link Buffer} whenever it receives
     * an object from the provided bufferOpenings {@link Observable}, and closes the corresponding
     * {@link Buffer} object when it receives an object from the provided {@link Observable} created
     * with the bufferClosingSelector {@link Func1}.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class ObservableBasedMultiBufferCreator<T> implements BufferCreator<T> {

        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

        public ObservableBasedMultiBufferCreator(final OverlappingBuffers<T> buffers, Observable<BufferOpening> bufferOpenings, final Func1<BufferOpening, Observable<BufferClosing>> bufferClosingSelector) {
            subscription.wrap(bufferOpenings.subscribe(new Action1<BufferOpening>() {
                @Override
                public void call(BufferOpening opening) {
                    final Buffer<T> buffer = buffers.createBuffer();
                    Observable<BufferClosing> closingObservable = bufferClosingSelector.call(opening);

                    closingObservable.subscribe(new Action1<BufferClosing>() {
                        @Override
                        public void call(BufferClosing closing) {
                            buffers.emitBuffer(buffer);
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
     * This {@link BufferCreator} creates a new {@link Buffer} every time after a fixed
     * period of time has elapsed.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class TimeBasedBufferCreator<T> implements BufferCreator<T> {

        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

        public TimeBasedBufferCreator(final NonOverlappingBuffers<T> buffers, long time, TimeUnit unit, Scheduler scheduler) {
            this.subscription.wrap(scheduler.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    buffers.emitAndReplaceBuffer();
                }
            }, 0, time, unit));
        }

        public TimeBasedBufferCreator(final OverlappingBuffers<T> buffers, long time, TimeUnit unit, Scheduler scheduler) {
            this.subscription.wrap(scheduler.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    buffers.createBuffer();
                }
            }, 0, time, unit));
        }

        @Override
        public void onValuePushed() {
            // Do nothing: buffers are created periodically.
        }

        @Override
        public void stop() {
            subscription.unsubscribe();
        }
    }

    /**
     * This {@link BufferCreator} creates a new {@link Buffer} every time after it has
     * seen a certain amount of elements.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class SkippingBufferCreator<T> implements BufferCreator<T> {

        private final AtomicInteger skipped = new AtomicInteger(1);
        private final Buffers<T> buffers;
        private final int skip;

        public SkippingBufferCreator(Buffers<T> buffers, int skip) {
            this.buffers = buffers;
            this.skip = skip;
        }

        @Override
        public void onValuePushed() {
            if (skipped.decrementAndGet() == 0) {
                skipped.set(skip);
                buffers.createBuffer();
            }
        }

        @Override
        public void stop() {
            // Nothing to stop: we're not using a Scheduler.
        }
    }

    /**
     * This class is an extension on the {@link Buffers} class which only supports one
     * active (not yet emitted) internal {@link Buffer} object.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class NonOverlappingBuffers<T> extends Buffers<T> {

        private final Object lock = new Object();

        public NonOverlappingBuffers(Observer<List<T>> observer) {
            super(observer);
        }

        public Buffer<T> emitAndReplaceBuffer() {
            synchronized (lock) {
                emitBuffer(getBuffer());
                return createBuffer();
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
     * This class is an extension on the {@link Buffers} class which actually has no additional
     * behavior than its super class. Classes extending this class, are expected to support
     * two or more active (not yet emitted) internal {@link Buffer} objects.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class OverlappingBuffers<T> extends Buffers<T> {
        public OverlappingBuffers(Observer<List<T>> observer) {
            super(observer);
        }
    }

    /**
     * This class is an extension on the {@link Buffers} class. Every internal buffer has
     * a has a maximum time to live and a maximum internal capacity. When the buffer has
     * reached the end of its life, or reached its maximum internal capacity it is
     * automatically emitted.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class TimeAndSizeBasedBuffers<T> extends Buffers<T> {

        private final ConcurrentMap<Buffer<T>, Subscription> subscriptions = new ConcurrentHashMap<Buffer<T>, Subscription>();

        private final Scheduler scheduler;
        private final long maxTime;
        private final TimeUnit unit;
        private final int maxSize;

        public TimeAndSizeBasedBuffers(Observer<List<T>> observer, int maxSize, long maxTime, TimeUnit unit, Scheduler scheduler) {
            super(observer);
            this.maxSize = maxSize;
            this.maxTime = maxTime;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Buffer<T> createBuffer() {
            final Buffer<T> buffer = super.createBuffer();
            subscriptions.put(buffer, scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    emitBuffer(buffer);
                }
            }, maxTime, unit));
            return buffer;
        }

        @Override
        public void emitBuffer(Buffer<T> buffer) {
            Subscription subscription = subscriptions.remove(buffer);
            if (subscription == null) {
                // Buffer was already emitted.
                return;
            }

            subscription.unsubscribe();
            super.emitBuffer(buffer);
            createBuffer();
        }

        @Override
        public void pushValue(T value) {
            super.pushValue(value);

            Buffer<T> buffer;
            while ((buffer = getBuffer()) != null) {
                if (buffer.contents.size() >= maxSize) {
                    emitBuffer(buffer);
                } else {
                    // Buffer is not at full capacity yet, and neither will remaining buffers be so we can terminate.
                    break;
                }
            }
        }
    }

    /**
     * This class is an extension on the {@link Buffers} class. Every internal buffer has
     * a has a maximum time to live. When the buffer has reached the end of its life it is
     * automatically emitted.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class TimeBasedBuffers<T> extends OverlappingBuffers<T> {

        private final ConcurrentMap<Buffer<T>, Subscription> subscriptions = new ConcurrentHashMap<Buffer<T>, Subscription>();

        private final Scheduler scheduler;
        private final long time;
        private final TimeUnit unit;

        public TimeBasedBuffers(Observer<List<T>> observer, long time, TimeUnit unit, Scheduler scheduler) {
            super(observer);
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Buffer<T> createBuffer() {
            final Buffer<T> buffer = super.createBuffer();
            subscriptions.put(buffer, scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    emitBuffer(buffer);
                }
            }, time, unit));
            return buffer;
        }

        @Override
        public void emitBuffer(Buffer<T> buffer) {
            subscriptions.remove(buffer);
            super.emitBuffer(buffer);
        }
    }

    /**
     * This class is an extension on the {@link Buffers} class. Every internal buffer has
     * a fixed maximum capacity. When the buffer has reached its maximum capacity it is
     * automatically emitted.
     *
     * @param <T> The type of object all internal {@link Buffer} objects record.
     */
    private static class SizeBasedBuffers<T> extends Buffers<T> {

        private final int size;

        public SizeBasedBuffers(Observer<List<T>> observer, int size) {
            super(observer);
            this.size = size;
        }

        @Override
        public void pushValue(T value) {
            super.pushValue(value);

            Buffer<T> buffer;
            while ((buffer = getBuffer()) != null) {
                if (buffer.contents.size() >= size) {
                    emitBuffer(buffer);
                } else {
                    // Buffer is not at full capacity yet, and neither will remaining buffers be so we can terminate.
                    break;
                }
            }
        }
    }

    /**
     * This class represents an object which contains and manages multiple {@link Buffer} objects.
     * 
     * @param <T> The type of objects which the internal {@link Buffer} objects record.
     */
    private static class Buffers<T> {

        private final Queue<Buffer<T>> buffers = new ConcurrentLinkedQueue<Buffer<T>>();
        private final Observer<List<T>> observer;

        /**
         * Constructs a new {@link Buffers} object for the specified {@link Observer}.
         * 
         * @param observer
         *            The {@link Observer} to which this object will emit its internal
         *            {@link Buffer} objects to when requested.
         */
        public Buffers(Observer<List<T>> observer) {
            this.observer = observer;
        }

        /**
         * This method will instantiate a new {@link Buffer} object and register it internally.
         * 
         * @return
         *            The constructed empty {@link Buffer} object.
         */
        public Buffer<T> createBuffer() {
            Buffer<T> buffer = new Buffer<T>();
            buffers.add(buffer);
            return buffer;
        }

        /**
         * This method emits all not yet emitted {@link Buffer} objects.
         */
        public void emitAllBuffers() {
            Buffer<T> buffer;
            while ((buffer = buffers.poll()) != null) {
                observer.onNext(buffer.getContents());
            }
        }

        /**
         * This method emits the specified {@link Buffer} object.
         * 
         * @param buffer
         *            The {@link Buffer} to emit.
         */
        public void emitBuffer(Buffer<T> buffer) {
            if (!buffers.remove(buffer)) {
                // Concurrency issue: Buffer is already emitted!
                return;
            }
            observer.onNext(buffer.getContents());
        }

        /**
         * @return
         *            The oldest (in case there are multiple) {@link Buffer} object.
         */
        public Buffer<T> getBuffer() {
            return buffers.peek();
        }

        /**
         * This method pushes a value to all not yet emitted {@link Buffer} objects.
         * 
         * @param value
         *            The value to push to all not yet emitted {@link Buffer} objects.
         */
        public void pushValue(T value) {
            List<Buffer<T>> copy = new ArrayList<Buffer<T>>(buffers);
            for (Buffer<T> buffer : copy) {
                buffer.pushValue(value);
            }
        }
    }

    /**
     * This class represents a single buffer: A sequence of recorded values.
     * 
     * @param <T> The type of objects which this {@link Buffer} can hold.
     */
    private static class Buffer<T> {
        private final List<T> contents = new ArrayList<T>();

        /**
         * Appends a specified value to the {@link Buffer}.
         * 
         * @param value
         *            The value to append to the {@link Buffer}.
         */
        public void pushValue(T value) {
            contents.add(value);
        }

        /**
         * @return
         *            The mutable underlying {@link List} which contains all the
         *            recorded values in this {@link Buffer} object.
         */
        public List<T> getContents() {
            return contents;
        }
    }

    public static class UnitTest {

        private Observer<List<String>> observer;
        private TestScheduler scheduler;

        @Before
        @SuppressWarnings("unchecked")
        public void before() {
            observer = Mockito.mock(Observer.class);
            scheduler = new TestScheduler();
        }

        @Test
        public void testComplete() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>() {
                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onCompleted();
                    return Subscriptions.empty();
                }
            });

            Observable<List<String>> buffered = Observable.create(buffer(source, 3, 3));
            buffered.subscribe(observer);

            Mockito.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            Mockito.verify(observer, Mockito.never()).onError(Mockito.any(Exception.class));
            Mockito.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testSkipAndCountOverlappingBuffers() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>() {
                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onNext("one");
                    observer.onNext("two");
                    observer.onNext("three");
                    observer.onNext("four");
                    observer.onNext("five");
                    return Subscriptions.empty();
                }
            });

            Observable<List<String>> buffered = Observable.create(buffer(source, 3, 1));
            buffered.subscribe(observer);

            InOrder inOrder = Mockito.inOrder(observer);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two", "three"));
            inOrder.verify(observer, Mockito.times(1)).onNext(list("two", "three", "four"));
            inOrder.verify(observer, Mockito.times(1)).onNext(list("three", "four", "five"));
            inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Exception.class));
            inOrder.verify(observer, Mockito.never()).onCompleted();
        }

        @Test
        public void testSkipAndCountGaplessBuffers() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>() {
                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onNext("one");
                    observer.onNext("two");
                    observer.onNext("three");
                    observer.onNext("four");
                    observer.onNext("five");
                    observer.onCompleted();
                    return Subscriptions.empty();
                }
            });

            Observable<List<String>> buffered = Observable.create(buffer(source, 3, 3));
            buffered.subscribe(observer);

            InOrder inOrder = Mockito.inOrder(observer);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two", "three"));
            inOrder.verify(observer, Mockito.times(1)).onNext(list("four", "five"));
            inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Exception.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testSkipAndCountBuffersWithGaps() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>() {
                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onNext("one");
                    observer.onNext("two");
                    observer.onNext("three");
                    observer.onNext("four");
                    observer.onNext("five");
                    observer.onCompleted();
                    return Subscriptions.empty();
                }
            });

            Observable<List<String>> buffered = Observable.create(buffer(source, 2, 3));
            buffered.subscribe(observer);

            InOrder inOrder = Mockito.inOrder(observer);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two"));
            inOrder.verify(observer, Mockito.times(1)).onNext(list("four", "five"));
            inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Exception.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testTimedAndCount() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>() {
                @Override
                public Subscription call(Observer<String> observer) {
                    push(observer, "one", 10);
                    push(observer, "two", 90);
                    push(observer, "three", 110);
                    push(observer, "four", 190);
                    push(observer, "five", 210);
                    complete(observer, 250);
                    return Subscriptions.empty();
                }
            });

            Observable<List<String>> buffered = Observable.create(buffer(source, 100, TimeUnit.MILLISECONDS, 2, scheduler));
            buffered.subscribe(observer);

            InOrder inOrder = Mockito.inOrder(observer);
            scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two"));

            scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("three", "four"));

            scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("five"));
            inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Exception.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testTimed() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>() {
                @Override
                public Subscription call(Observer<String> observer) {
                    push(observer, "one", 98);
                    push(observer, "two", 99);
                    push(observer, "three", 100);
                    push(observer, "four", 101);
                    push(observer, "five", 102);
                    complete(observer, 150);
                    return Subscriptions.empty();
                }
            });

            Observable<List<String>> buffered = Observable.create(buffer(source, 100, TimeUnit.MILLISECONDS, scheduler));
            buffered.subscribe(observer);

            InOrder inOrder = Mockito.inOrder(observer);
            scheduler.advanceTimeTo(101, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two", "three"));

            scheduler.advanceTimeTo(201, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("four", "five"));
            inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Exception.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testObservableBasedOpenerAndCloser() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>() {
                @Override
                public Subscription call(Observer<String> observer) {
                    push(observer, "one", 10);
                    push(observer, "two", 60);
                    push(observer, "three", 110);
                    push(observer, "four", 160);
                    push(observer, "five", 210);
                    complete(observer, 500);
                    return Subscriptions.empty();
                }
            });

            Observable<BufferOpening> openings = Observable.create(new Func1<Observer<BufferOpening>, Subscription>() {
                @Override
                public Subscription call(Observer<BufferOpening> observer) {
                    push(observer, BufferOpenings.create(), 50);
                    push(observer, BufferOpenings.create(), 200);
                    complete(observer, 250);
                    return Subscriptions.empty();
                }
            });

            Func1<BufferOpening, Observable<BufferClosing>> closer = new Func1<BufferOpening, Observable<BufferClosing>>() {
                @Override
                public Observable<BufferClosing> call(BufferOpening opening) {
                    return Observable.create(new Func1<Observer<BufferClosing>, Subscription>() {
                        @Override
                        public Subscription call(Observer<BufferClosing> observer) {
                            push(observer, BufferClosings.create(), 100);
                            complete(observer, 101);
                            return Subscriptions.empty();
                        }
                    });
                }
            };

            Observable<List<String>> buffered = Observable.create(buffer(source, openings, closer));
            buffered.subscribe(observer);

            InOrder inOrder = Mockito.inOrder(observer);
            scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("two", "three"));
            inOrder.verify(observer, Mockito.times(1)).onNext(list("five"));
            inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Exception.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testObservableBasedCloser() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>() {
                @Override
                public Subscription call(Observer<String> observer) {
                    push(observer, "one", 10);
                    push(observer, "two", 60);
                    push(observer, "three", 110);
                    push(observer, "four", 160);
                    push(observer, "five", 210);
                    complete(observer, 250);
                    return Subscriptions.empty();
                }
            });

            Func0<Observable<BufferClosing>> closer = new Func0<Observable<BufferClosing>>() {
                @Override
                public Observable<BufferClosing> call() {
                    return Observable.create(new Func1<Observer<BufferClosing>, Subscription>() {
                        @Override
                        public Subscription call(Observer<BufferClosing> observer) {
                            push(observer, BufferClosings.create(), 100);
                            complete(observer, 101);
                            return Subscriptions.empty();
                        }
                    });
                }
            };

            Observable<List<String>> buffered = Observable.create(buffer(source, closer));
            buffered.subscribe(observer);

            InOrder inOrder = Mockito.inOrder(observer);
            scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two"));
            inOrder.verify(observer, Mockito.times(1)).onNext(list("three", "four"));
            inOrder.verify(observer, Mockito.times(1)).onNext(list("five"));
            inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Exception.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        private List<String> list(String... args) {
            List<String> list = new ArrayList<String>();
            for (String arg : args) {
                list.add(arg);
            }
            return list;
        }

        private <T> void push(final Observer<T> observer, final T value, int delay) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onNext(value);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private void complete(final Observer<?> observer, int delay) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onCompleted();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
}
