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

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public final class OperationWindow extends ChunkedOperation {

    public static <T> Func0<Window<T>> windowMaker() {
        return new Func0<Window<T>>() {
            @Override
            public Window<T> call() {
                return new Window<T>();
            }
        };
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in a window until
     * the {@link rx.Observable} constructed using the {@link rx.functions.Func0} argument, produces a value.
     * The window is then emitted, and a new window is created to replace it. A new {@link rx.Observable} will
     * be constructed using the provided {@link rx.functions.Func0} object, which will determine when this new
     * window is emitted. When the source {@link rx.Observable} completes or produces an error, the current
     * window is emitted, and the event is propagated to all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param windowClosingSelector
     *            a {@link rx.functions.Func0} object that produces {@link rx.Observable}s. These
     *            {@link rx.Observable}s determine when a window is emitted and replaced by simply
     *            producing an object.
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T, TClosing> OnSubscribeFunc<Observable<T>> window(final Observable<? extends T> source, final Func0<? extends Observable<? extends TClosing>> windowClosingSelector) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                NonOverlappingChunks<T, Observable<T>> windows = new NonOverlappingChunks<T, Observable<T>>(observer, OperationWindow.<T> windowMaker());
                ChunkCreator creator = new ObservableBasedSingleChunkCreator<T, Observable<T>, TClosing>(windows, windowClosingSelector);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(windows, observer, creator));
            }

        };
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in the currently
     * active window. Initially there are no windows active.
     * <p>
     * Windows can be created by pushing a {@link rx.util.TOpening} value to the {@code windowOpenings}
     * {@link rx.Observable}. This creates a new window which will then start recording values which are
     * produced by the {@code source} {@link rx.Observable}. Additionally the {@code windowClosingSelector}
     * will be used to construct an {@link rx.Observable} which can produce values. When it does so it will
     * close this (and only this) newly created window. When the source {@link rx.Observable} completes or
     * produces an error, all windows are emitted, and the event is propagated to all subscribed
     * {@link rx.Observer}s.
     * </p><p>
     * Note that when using this operation <strong>multiple overlapping windows</strong> could be active at any
     * one point.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param windowOpenings
     *            an {@link rx.Observable} which when it produces a {@link rx.util.TOpening} value will create a
     *            new window which instantly starts recording the {@code source} {@link rx.Observable}
     * @param windowClosingSelector
     *            a {@link rx.functions.Func0} object that produces {@link rx.Observable}s. These
     *            {@link rx.Observable}s determine when a window is emitted and replaced by simply producing an
     *            object.
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T, TOpening, TClosing> OnSubscribeFunc<Observable<T>> window(final Observable<? extends T> source, final Observable<? extends TOpening> windowOpenings, final Func1<? super TOpening, ? extends Observable<? extends TClosing>> windowClosingSelector) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                OverlappingChunks<T, Observable<T>> windows = new OverlappingChunks<T, Observable<T>>(observer, OperationWindow.<T> windowMaker());
                ChunkCreator creator = new ObservableBasedMultiChunkCreator<T, Observable<T>, TOpening, TClosing>(windows, windowOpenings, windowClosingSelector);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(windows, observer, creator));
            }
        };
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in a window until
     * the window contains a specified number of elements. The window is then emitted, and a new window is
     * created to replace it. When the source {@link rx.Observable} completes or produces an error, the current
     * window is emitted, and the event is propagated to all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param count
     *            the number of elements a window should have before being emitted and replaced
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(Observable<? extends T> source, int count) {
        return window(source, count, count);
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in all active
     * windows until the window contains a specified number of elements. The window is then emitted. Windows are
     * created after a certain amount of values have been received. When the source {@link rx.Observable}
     * completes or produces an error, the currently active windows are emitted, and the event is propagated to
     * all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation can produce <strong>non-connected, connected non-overlapping, or overlapping
     * windows</strong> depending on the input parameters.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param count
     *            the number of elements a window should have before being emitted
     * @param skip
     *            the interval with which windows have to be created. Note that when {@code skip == count} that
     *            this is the same as calling {@link rx.operators.OperationWindow#window(rx.Observable, int)}.
     *            If {@code skip < count}, this window operation will produce overlapping windows and if
     *            {@code skip > count} non-overlapping windows will be created and some values will not be
     *            pushed into a window at all!
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<? extends T> source, final int count, final int skip) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                Chunks<T, Observable<T>> chunks = new SizeBasedChunks<T, Observable<T>>(observer, OperationWindow.<T> windowMaker(), count);
                ChunkCreator creator = new SkippingChunkCreator<T, Observable<T>>(chunks, skip);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(chunks, observer, creator));
            }
        };
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in a window.
     * Periodically the window is emitted and replaced with a new window. How often this is done depends on the
     * specified timespan. When the source {@link rx.Observable} completes or produces an error, the current
     * window is emitted, and the event is propagated to all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param timespan
     *            the amount of time all windows must be actively collect values before being emitted
     * @param unit
     *            the {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(Observable<? extends T> source, long timespan, TimeUnit unit) {
        return window(source, timespan, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in a window.
     * Periodically the window is emitted and replaced with a new window. How often this is done depends on the
     * specified timespan. When the source {@link rx.Observable} completes or produces an error, the current
     * window is emitted, and the event is propagated to all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param timespan
     *            the amount of time all windows must be actively collect values before being emitted
     * @param unit
     *            the {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan
     * @param scheduler
     *            the {@link rx.Scheduler} to use for timing windows
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<? extends T> source, final long timespan, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                NonOverlappingChunks<T, Observable<T>> windows = new NonOverlappingChunks<T, Observable<T>>(observer, OperationWindow.<T> windowMaker());
                ChunkCreator creator = new TimeBasedChunkCreator<T, Observable<T>>(windows, timespan, unit, scheduler);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(windows, observer, creator));
            }
        };
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in a window.
     * Periodically the window is emitted and replaced with a new window. How often this is done depends on the
     * specified timespan. Additionally the window is automatically emitted once it reaches a specified number
     * of elements. When the source {@link rx.Observable} completes or produces an error, the current window is
     * emitted, and the event is propagated to all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param timespan
     *            the amount of time all windows must be actively collect values before being emitted
     * @param unit
     *            the {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan
     * @param count
     *            the maximum size of the window. Once a window reaches this size, it is emitted
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(Observable<? extends T> source, long timespan, TimeUnit unit, int count) {
        return window(source, timespan, unit, count, Schedulers.threadPoolForComputation());
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in a window.
     * Periodically the window is emitted and replaced with a new window. How often this is done depends on the
     * specified timespan. Additionally the window is automatically emitted once it reaches a specified number
     * of elements. When the source {@link rx.Observable} completes or produces an error, the current window is
     * emitted, and the event is propagated to all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param timespan
     *            the amount of time all windows must be actively collect values before being emitted
     * @param unit
     *            the {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan
     * @param count
     *            the maximum size of the window. Once a window reaches this size, it is emitted
     * @param scheduler
     *            the {@link rx.Scheduler} to use for timing windows
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<? extends T> source, final long timespan, final TimeUnit unit, final int count, final Scheduler scheduler) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                Chunks<T, Observable<T>> chunks = new TimeAndSizeBasedChunks<T, Observable<T>>(observer, OperationWindow.<T> windowMaker(), count, timespan, unit, scheduler);
                ChunkCreator creator = new SingleChunkCreator<T, Observable<T>>(chunks);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(chunks, observer, creator));
            }
        };
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in a window.
     * Periodically the window is emitted and replaced with a new window. How often this is done depends on the
     * specified timespan. The creation of windows is also periodical. How often this is done depends on the
     * specified timeshift. When the source {@link rx.Observable} completes or produces an error, the current
     * window is emitted, and the event is propagated to all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation can produce <strong>non-connected, or overlapping windows</strong> depending on
     * the input parameters.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param timespan
     *            the amount of time all windows must be actively collect values before being emitted
     * @param timeshift
     *            the amount of time between creating windows
     * @param unit
     *            the {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(Observable<? extends T> source, long timespan, long timeshift, TimeUnit unit) {
        return window(source, timespan, timeshift, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * This method creates a {@link rx.functions.Func1} object which represents the window operation. This
     * operation takes values from the specified {@link rx.Observable} source and stores them in a window.
     * Periodically the window is emitted and replaced with a new window. How often this is done depends on the
     * specified timespan. The creation of windows is also periodical. How often this is done depends on the
     * specified timeshift. When the source {@link rx.Observable} completes or produces an error, the current
     * window is emitted, and the event is propagated to all subscribed {@link rx.Observer}s.
     * <p>
     * Note that this operation can produce <strong>non-connected, or overlapping windows</strong> depending on
     * the input parameters.
     * </p>
     * 
     * @param source
     *            the {@link rx.Observable} which produces values
     * @param timespan
     *            the amount of time all windows must be actively collect values before being emitted
     * @param timeshift
     *            the amount of time between creating windows
     * @param unit
     *            the {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan
     * @param scheduler
     *            the {@link rx.Scheduler} to use for timing windows
     * @return
     *         the {@link rx.functions.Func1} object representing the specified window operation
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<? extends T> source, final long timespan, final long timeshift, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                OverlappingChunks<T, Observable<T>> windows = new TimeBasedChunks<T, Observable<T>>(observer, OperationWindow.<T> windowMaker(), timespan, unit, scheduler);
                ChunkCreator creator = new TimeBasedChunkCreator<T, Observable<T>>(windows, timeshift, unit, scheduler);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(windows, observer, creator));
            }
        };
    }

    /**
     * This class represents a single window: A sequence of recorded values.
     * 
     * @param <T>
     *            the type of objects which this {@link Window} can hold
     */
    protected static class Window<T> extends Chunk<T, Observable<T>> {
        /**
         * @return
         *         the mutable underlying {@link Observable} which contains all the recorded values in this
         *         {@link Window} object
         */
        @Override
        public Observable<T> getContents() {
            return Observable.from(contents);
        }
    }

    /**
     * Emits windows of values of the source Observable where the window boundary is determined by the items of
     * the boundary Observable.
     *
     * @param source
     * @param boundary
     * @return
     */
    public static <T, U> OnSubscribeFunc<Observable<T>> window(Observable<? extends T> source, Observable<U> boundary) {
        return new WindowViaObservable<T, U>(source, boundary);
    }

    /**
     * Create non-overlapping windows from the source values by using another observable's values as to when to
     * replace a window.
     */
    private static final class WindowViaObservable<T, U> implements OnSubscribeFunc<Observable<T>> {
        final Observable<? extends T> source;
        final Observable<U> boundary;

        public WindowViaObservable(Observable<? extends T> source, Observable<U> boundary) {
            this.source = source;
            this.boundary = boundary;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Observable<T>> t1) {
            CompositeSubscription csub = new CompositeSubscription();

            final SourceObserver<T> so = new SourceObserver<T>(t1, csub);
            try {
                t1.onNext(so.subject);
            } catch (Throwable t) {
                t1.onError(t);
                return Subscriptions.empty();
            }
            csub.add(source.subscribe(so));

            if (!csub.isUnsubscribed()) {
                csub.add(boundary.subscribe(new BoundaryObserver<T, U>(so)));
            }

            return csub;
        }

        /**
         * Observe the source and emit the values into the current window.
         */
        private static final class SourceObserver<T> implements Observer<T> {
            final Observer<? super Observable<T>> observer;
            final Subscription cancel;
            final Object guard;
            Subject<T, T> subject;

            public SourceObserver(Observer<? super Observable<T>> observer, Subscription cancel) {
                this.observer = observer;
                this.cancel = cancel;
                this.guard = new Object();
                this.subject = create();
            }

            Subject<T, T> create() {
                return PublishSubject.create();
            }

            @Override
            public void onNext(T args) {
                synchronized (guard) {
                    if (subject == null) {
                        return;
                    }
                    subject.onNext(args);
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    if (subject == null) {
                        return;
                    }
                    Subject<T, T> s = subject;
                    subject = null;

                    s.onError(e);
                    observer.onError(e);
                }
                cancel.unsubscribe();
            }

            @Override
            public void onCompleted() {
                synchronized (guard) {
                    if (subject == null) {
                        return;
                    }
                    Subject<T, T> s = subject;
                    subject = null;

                    s.onCompleted();
                    observer.onCompleted();
                }
                cancel.unsubscribe();
            }

            public void replace() {
                try {
                    synchronized (guard) {
                        if (subject == null) {
                            return;
                        }
                        Subject<T, T> s = subject;
                        s.onCompleted();

                        subject = create();
                        observer.onNext(subject);
                    }
                } catch (Throwable t) {
                    onError(t);
                }
            }
        }

        /**
         * Observe the boundary and replace the window on each item.
         */
        private static final class BoundaryObserver<T, U> implements Observer<U> {
            final SourceObserver<T> so;

            public BoundaryObserver(SourceObserver<T> so) {
                this.so = so;
            }

            @Override
            public void onNext(U args) {
                so.replace();
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
