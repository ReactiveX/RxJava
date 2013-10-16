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

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.Closing;
import rx.util.Closings;
import rx.util.Opening;
import rx.util.Openings;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

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
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer until the {@link Observable} constructed using the {@link Func0} argument, produces a {@link rx.util.Closing}
     * value. The buffer is then
     * emitted, and a new buffer is created to replace it. A new {@link Observable} will be constructed using the
     * provided {@link Func0} object, which will determine when this new buffer is emitted. When the source {@link Observable} completes or produces an error, the current buffer is emitted, and the
     * event is propagated
     * to all subscribed {@link Observer}s.</p>
     * 
     * <p>Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param bufferClosingSelector
     *            A {@link Func0} object which produces {@link Observable}s. These {@link Observable}s determine when a buffer is emitted and replaced by simply
     *            producing an {@link rx.util.Closing} object.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final Func0<? extends Observable<? extends Closing>> bufferClosingSelector) {
        return new OnSubscribeFunc<List<T>>() {

            @Override
            public Subscription onSubscribe(Observer<? super List<T>> observer) {
                NonOverlappingChunks<T, List<T>> buffers = new NonOverlappingChunks<T, List<T>>(observer, OperationBuffer.<T>bufferMaker());
                ChunkCreator creator = new ObservableBasedSingleChunkCreator<T, List<T>>(buffers, bufferClosingSelector);
                return source.subscribe(new ChunkObserver<T, List<T>>(buffers, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in the currently active chunks. Initially
     * there are no chunks active.</p>
     * 
     * <p>Chunks can be created by pushing a {@link rx.util.Opening} value to the "bufferOpenings" {@link Observable}.
     * This creates a new buffer which will then start recording values which are produced by the "source" {@link Observable}. Additionally the "bufferClosingSelector" will be used to construct an
     * {@link Observable} which can produce {@link rx.util.Closing} values. When it does so it will close this (and only this) newly created
     * buffer. When the source {@link Observable} completes or produces an error, all chunks are emitted, and the
     * event is propagated to all subscribed {@link Observer}s.</p>
     * 
     * <p>Note that when using this operation <strong>multiple overlapping chunks</strong>
     * could be active at any one point.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param bufferOpenings
     *            An {@link Observable} which when it produces a {@link rx.util.Opening} value will
     *            create a new buffer which instantly starts recording the "source" {@link Observable}.
     * @param bufferClosingSelector
     *            A {@link Func0} object which produces {@link Observable}s. These {@link Observable}s determine when a buffer is emitted and replaced by simply
     *            producing an {@link rx.util.Closing} object.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final Observable<? extends Opening> bufferOpenings, final Func1<Opening, ? extends Observable<? extends Closing>> bufferClosingSelector) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                OverlappingChunks<T, List<T>> buffers = new OverlappingChunks<T, List<T>>(observer, OperationBuffer.<T>bufferMaker());
                ChunkCreator creator = new ObservableBasedMultiChunkCreator<T, List<T>>(buffers, bufferOpenings, bufferClosingSelector);
                return source.subscribe(new ChunkObserver<T, List<T>>(buffers, observer, creator));
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
     * <p>Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param count
     *            The number of elements a buffer should have before being emitted and replaced.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(Observable<T> source, int count) {
        return buffer(source, count, count);
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in all active chunks until the buffer
     * contains a specified number of elements. The buffer is then emitted. Chunks are created after a certain
     * amount of values have been received. When the source {@link Observable} completes or produces an error, the
     * currently active chunks are emitted, and the event is propagated to all subscribed {@link Observer}s.</p>
     * 
     * <p>Note that this operation can produce <strong>non-connected, connected non-overlapping, or overlapping
     * chunks</strong> depending on the input parameters.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param count
     *            The number of elements a buffer should have before being emitted.
     * @param skip
     *            The interval with which chunks have to be created. Note that when "skip" == "count"
     *            that this is the same as calling {@link OperationBuffer#buffer(Observable, int)}.
     *            If "skip" < "count", this buffer operation will produce overlapping chunks and if "skip"
     *            > "count" non-overlapping chunks will be created and some values will not be pushed
     *            into a buffer at all!
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final int count, final int skip) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                Chunks<T, List<T>> chunks = new SizeBasedChunks<T, List<T>>(observer, OperationBuffer.<T>bufferMaker(), count);
                ChunkCreator creator = new SkippingChunkCreator<T, List<T>>(chunks, skip);
                return source.subscribe(new ChunkObserver<T, List<T>>(chunks, observer, creator));
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
     * <p>Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all chunks must be actively collect values before being emitted.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(Observable<T> source, long timespan, TimeUnit unit) {
        return buffer(source, timespan, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     * 
     * <p>Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all chunks must be actively collect values before being emitted.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @param scheduler
     *            The {@link Scheduler} to use for timing chunks.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final long timespan, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                NonOverlappingChunks<T, List<T>> buffers = new NonOverlappingChunks<T, List<T>>(observer, OperationBuffer.<T>bufferMaker());
                ChunkCreator creator = new TimeBasedChunkCreator<T, List<T>>(buffers, timespan, unit, scheduler);
                return source.subscribe(new ChunkObserver<T, List<T>>(buffers, observer, creator));
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
     * <p>Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all chunks must be actively collect values before being emitted.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @param count
     *            The maximum size of the buffer. Once a buffer reaches this size, it is emitted.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(Observable<T> source, long timespan, TimeUnit unit, int count) {
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
     * <p>Note that this operation only produces <strong>non-overlapping chunks</strong>. At all times there is
     * exactly one buffer actively storing values.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all chunks must be actively collect values before being emitted.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @param count
     *            The maximum size of the buffer. Once a buffer reaches this size, it is emitted.
     * @param scheduler
     *            The {@link Scheduler} to use for timing chunks.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final long timespan, final TimeUnit unit, final int count, final Scheduler scheduler) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                Chunks<T, List<T>> chunks = new TimeAndSizeBasedChunks<T, List<T>>(observer, OperationBuffer.<T>bufferMaker(), count, timespan, unit, scheduler);
                ChunkCreator creator = new SingleChunkCreator<T, List<T>>(chunks);
                return source.subscribe(new ChunkObserver<T, List<T>>(chunks, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * The creation of chunks is also periodical. How often this is done depends on the specified timeshift.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     * 
     * <p>Note that this operation can produce <strong>non-connected, or overlapping chunks</strong> depending
     * on the input parameters.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all chunks must be actively collect values before being emitted.
     * @param timeshift
     *            The amount of time between creating chunks.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(Observable<T> source, long timespan, long timeshift, TimeUnit unit) {
        return buffer(source, timespan, timeshift, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * <p>This method creates a {@link Func1} object which represents the buffer operation. This operation takes
     * values from the specified {@link Observable} source and stores them in a buffer. Periodically the buffer
     * is emitted and replaced with a new buffer. How often this is done depends on the specified timespan.
     * The creation of chunks is also periodical. How often this is done depends on the specified timeshift.
     * When the source {@link Observable} completes or produces an error, the current buffer is emitted, and
     * the event is propagated to all subscribed {@link Observer}s.</p>
     * 
     * <p>Note that this operation can produce <strong>non-connected, or overlapping chunks</strong> depending
     * on the input parameters.</p>
     * 
     * @param source
     *            The {@link Observable} which produces values.
     * @param timespan
     *            The amount of time all chunks must be actively collect values before being emitted.
     * @param timeshift
     *            The amount of time between creating chunks.
     * @param unit
     *            The {@link TimeUnit} defining the unit of time for the timespan.
     * @param scheduler
     *            The {@link Scheduler} to use for timing chunks.
     * @return
     *         the {@link Func1} object representing the specified buffer operation.
     */
    public static <T> OnSubscribeFunc<List<T>> buffer(final Observable<T> source, final long timespan, final long timeshift, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<List<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super List<T>> observer) {
                OverlappingChunks<T, List<T>> buffers = new TimeBasedChunks<T, List<T>>(observer, OperationBuffer.<T>bufferMaker(), timespan, unit, scheduler);
                ChunkCreator creator = new TimeBasedChunkCreator<T, List<T>>(buffers, timeshift, unit, scheduler);
                return source.subscribe(new ChunkObserver<T, List<T>>(buffers, observer, creator));
            }
        };
    }

    /**
     * This class represents a single buffer: A sequence of recorded values.
     * 
     * @param <T>
     *            The type of objects which this {@link Buffer} can hold.
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
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    observer.onCompleted();
                    return Subscriptions.empty();
                }
            });

            Observable<List<String>> buffered = Observable.create(buffer(source, 3, 3));
            buffered.subscribe(observer);

            Mockito.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
            Mockito.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
            Mockito.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testSkipAndCountOverlappingBuffers() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
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
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
            inOrder.verify(observer, Mockito.never()).onCompleted();
        }

        @Test
        public void testSkipAndCountGaplessBuffers() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
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
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testSkipAndCountBuffersWithGaps() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
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
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testTimedAndCount() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
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
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testTimed() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
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
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testObservableBasedOpenerAndCloser() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    push(observer, "one", 10);
                    push(observer, "two", 60);
                    push(observer, "three", 110);
                    push(observer, "four", 160);
                    push(observer, "five", 210);
                    complete(observer, 500);
                    return Subscriptions.empty();
                }
            });

            Observable<Opening> openings = Observable.create(new OnSubscribeFunc<Opening>() {
                @Override
                public Subscription onSubscribe(Observer<? super Opening> observer) {
                    push(observer, Openings.create(), 50);
                    push(observer, Openings.create(), 200);
                    complete(observer, 250);
                    return Subscriptions.empty();
                }
            });

            Func1<Opening, Observable<Closing>> closer = new Func1<Opening, Observable<Closing>>() {
                @Override
                public Observable<Closing> call(Opening opening) {
                    return Observable.create(new OnSubscribeFunc<Closing>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super Closing> observer) {
                            push(observer, Closings.create(), 100);
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
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testObservableBasedCloser() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    push(observer, "one", 10);
                    push(observer, "two", 60);
                    push(observer, "three", 110);
                    push(observer, "four", 160);
                    push(observer, "five", 210);
                    complete(observer, 250);
                    return Subscriptions.empty();
                }
            });

            Func0<Observable<Closing>> closer = new Func0<Observable<Closing>>() {
                @Override
                public Observable<Closing> call() {
                    return Observable.create(new OnSubscribeFunc<Closing>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super Closing> observer) {
                            push(observer, Closings.create(), 100);
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
            inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
            inOrder.verify(observer, Mockito.times(1)).onCompleted();
        }

        @Test
        public void testLongTimeAction() throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            LongTimeAction action = new LongTimeAction(latch);
            Observable.from(1).buffer(10, TimeUnit.MILLISECONDS, 10)
                    .subscribe(action);
            latch.await();
            assertFalse(action.fail);
        }

        private static class LongTimeAction implements Action1<List<Integer>> {

            CountDownLatch latch;
            boolean fail = false;

            public LongTimeAction(CountDownLatch latch) {
                this.latch = latch;
            }

            @Override
            public void call(List<Integer> t1) {
                try {
                    if (fail) {
                        return;
                    }
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    fail = true;
                } finally {
                    latch.countDown();
                }
            }
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
