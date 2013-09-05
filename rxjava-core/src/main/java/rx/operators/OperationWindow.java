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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

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

public final class OperationWindow extends ChunkedOperation {

    public static final Func0 WINDOW_MAKER = new Func0() {
        @Override
        public Object call() {
            return new Window();
        }
    };

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in a window until the {@link rx.Observable} constructed using the {@link rx.util.functions.Func0} argument, produces a
     * {@link rx.util.Closing} value. The window is then
     * emitted, and a new window is created to replace it. A new {@link rx.Observable} will be constructed using the
     * provided {@link rx.util.functions.Func0} object, which will determine when this new window is emitted. When the source {@link rx.Observable} completes or produces an error, the current window
     * is emitted, and the event is propagated
     * to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param windowClosingSelector
     *            A {@link rx.util.functions.Func0} object which produces {@link rx.Observable}s. These {@link rx.Observable}s determine when a window is emitted and replaced by simply
     *            producing an {@link rx.util.Closing} object.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<T> source, final Func0<Observable<Closing>> windowClosingSelector) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                NonOverlappingChunks<T, Observable<T>> windows = new NonOverlappingChunks<T, Observable<T>>(observer, WINDOW_MAKER);
                ChunkCreator creator = new ObservableBasedSingleChunkCreator<T, Observable<T>>(windows, windowClosingSelector);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(windows, observer, creator));
            }

        };
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in the currently active window. Initially
     * there are no windows active.</p>
     * 
     * <p>Windows can be created by pushing a {@link rx.util.Opening} value to the "windowOpenings" {@link rx.Observable}.
     * This creates a new window which will then start recording values which are produced by the "source" {@link rx.Observable}. Additionally the "windowClosingSelector" will be used to construct an
     * {@link rx.Observable} which can produce {@link rx.util.Closing} values. When it does so it will close this (and only this) newly created
     * window. When the source {@link rx.Observable} completes or produces an error, all windows are emitted, and the
     * event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that when using this operation <strong>multiple overlapping windows</strong>
     * could be active at any one point.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param windowOpenings
     *            An {@link rx.Observable} which when it produces a {@link rx.util.Opening} value will
     *            create a new window which instantly starts recording the "source" {@link rx.Observable}.
     * @param windowClosingSelector
     *            A {@link rx.util.functions.Func0} object which produces {@link rx.Observable}s. These {@link rx.Observable}s determine when a window is emitted and replaced by simply
     *            producing an {@link rx.util.Closing} object.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<T> source, final Observable<Opening> windowOpenings, final Func1<Opening, Observable<Closing>> windowClosingSelector) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                OverlappingChunks<T, Observable<T>> windows = new OverlappingChunks<T, Observable<T>>(observer, WINDOW_MAKER);
                ChunkCreator creator = new ObservableBasedMultiChunkCreator<T, Observable<T>>(windows, windowOpenings, windowClosingSelector);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(windows, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in a window until the window contains
     * a specified number of elements. The window is then emitted, and a new window is created to replace it.
     * When the source {@link rx.Observable} completes or produces an error, the current window is emitted, and
     * the event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param count
     *            The number of elements a window should have before being emitted and replaced.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(Observable<T> source, int count) {
        return window(source, count, count);
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in all active windows until the window
     * contains a specified number of elements. The window is then emitted. windows are created after a certain
     * amount of values have been received. When the source {@link rx.Observable} completes or produces an error, the
     * currently active windows are emitted, and the event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation can produce <strong>non-connected, connected non-overlapping, or overlapping
     * windows</strong> depending on the input parameters.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param count
     *            The number of elements a window should have before being emitted.
     * @param skip
     *            The interval with which windows have to be created. Note that when "skip" == "count"
     *            that this is the same as calling {@link rx.operators.OperationWindow#window(rx.Observable, int)}.
     *            If "skip" < "count", this window operation will produce overlapping windows and if "skip"
     *            > "count" non-overlapping windows will be created and some values will not be pushed
     *            into a window at all!
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<T> source, final int count, final int skip) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                Chunks<T, Observable<T>> chunks = new SizeBasedChunks<T, Observable<T>>(observer, WINDOW_MAKER, count);
                ChunkCreator creator = new SkippingChunkCreator<T, Observable<T>>(chunks, skip);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(chunks, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in a window. Periodically the window
     * is emitted and replaced with a new window. How often this is done depends on the specified timespan.
     * When the source {@link rx.Observable} completes or produces an error, the current window is emitted, and
     * the event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param timespan
     *            The amount of time all windows must be actively collect values before being emitted.
     * @param unit
     *            The {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(Observable<T> source, long timespan, TimeUnit unit) {
        return window(source, timespan, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in a window. Periodically the window
     * is emitted and replaced with a new window. How often this is done depends on the specified timespan.
     * When the source {@link rx.Observable} completes or produces an error, the current window is emitted, and
     * the event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param timespan
     *            The amount of time all windows must be actively collect values before being emitted.
     * @param unit
     *            The {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan.
     * @param scheduler
     *            The {@link rx.Scheduler} to use for timing windows.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<T> source, final long timespan, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                NonOverlappingChunks<T, Observable<T>> windows = new NonOverlappingChunks<T, Observable<T>>(observer, WINDOW_MAKER);
                ChunkCreator creator = new TimeBasedChunkCreator<T, Observable<T>>(windows, timespan, unit, scheduler);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(windows, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in a window. Periodically the window
     * is emitted and replaced with a new window. How often this is done depends on the specified timespan.
     * Additionally the window is automatically emitted once it reaches a specified number of elements.
     * When the source {@link rx.Observable} completes or produces an error, the current window is emitted, and
     * the event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param timespan
     *            The amount of time all windows must be actively collect values before being emitted.
     * @param unit
     *            The {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan.
     * @param count
     *            The maximum size of the window. Once a window reaches this size, it is emitted.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(Observable<T> source, long timespan, TimeUnit unit, int count) {
        return window(source, timespan, unit, count, Schedulers.threadPoolForComputation());
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in a window. Periodically the window
     * is emitted and replaced with a new window. How often this is done depends on the specified timespan.
     * Additionally the window is automatically emitted once it reaches a specified number of elements.
     * When the source {@link rx.Observable} completes or produces an error, the current window is emitted, and
     * the event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation only produces <strong>non-overlapping windows</strong>. At all times there is
     * exactly one window actively storing values.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param timespan
     *            The amount of time all windows must be actively collect values before being emitted.
     * @param unit
     *            The {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan.
     * @param count
     *            The maximum size of the window. Once a window reaches this size, it is emitted.
     * @param scheduler
     *            The {@link rx.Scheduler} to use for timing windows.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<T> source, final long timespan, final TimeUnit unit, final int count, final Scheduler scheduler) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                Chunks<T, Observable<T>> chunks = new TimeAndSizeBasedChunks<T, Observable<T>>(observer, WINDOW_MAKER, count, timespan, unit, scheduler);
                ChunkCreator creator = new SingleChunkCreator<T, Observable<T>>(chunks);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(chunks, observer, creator));
            }
        };
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in a window. Periodically the window
     * is emitted and replaced with a new window. How often this is done depends on the specified timespan.
     * The creation of windows is also periodical. How often this is done depends on the specified timeshift.
     * When the source {@link rx.Observable} completes or produces an error, the current window is emitted, and
     * the event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation can produce <strong>non-connected, or overlapping windows</strong> depending
     * on the input parameters.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param timespan
     *            The amount of time all windows must be actively collect values before being emitted.
     * @param timeshift
     *            The amount of time between creating windows.
     * @param unit
     *            The {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(Observable<T> source, long timespan, long timeshift, TimeUnit unit) {
        return window(source, timespan, timeshift, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * <p>This method creates a {@link rx.util.functions.Func1} object which represents the window operation. This operation takes
     * values from the specified {@link rx.Observable} source and stores them in a window. Periodically the window
     * is emitted and replaced with a new window. How often this is done depends on the specified timespan.
     * The creation of windows is also periodical. How often this is done depends on the specified timeshift.
     * When the source {@link rx.Observable} completes or produces an error, the current window is emitted, and
     * the event is propagated to all subscribed {@link rx.Observer}s.</p>
     * 
     * <p>Note that this operation can produce <strong>non-connected, or overlapping windows</strong> depending
     * on the input parameters.</p>
     * 
     * @param source
     *            The {@link rx.Observable} which produces values.
     * @param timespan
     *            The amount of time all windows must be actively collect values before being emitted.
     * @param timeshift
     *            The amount of time between creating windows.
     * @param unit
     *            The {@link java.util.concurrent.TimeUnit} defining the unit of time for the timespan.
     * @param scheduler
     *            The {@link rx.Scheduler} to use for timing windows.
     * @return
     *         the {@link rx.util.functions.Func1} object representing the specified window operation.
     */
    public static <T> OnSubscribeFunc<Observable<T>> window(final Observable<T> source, final long timespan, final long timeshift, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<Observable<T>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Observable<T>> observer) {
                OverlappingChunks<T, Observable<T>> windows = new TimeBasedChunks<T, Observable<T>>(observer, WINDOW_MAKER, timespan, unit, scheduler);
                ChunkCreator creator = new TimeBasedChunkCreator<T, Observable<T>>(windows, timeshift, unit, scheduler);
                return source.subscribe(new ChunkObserver<T, Observable<T>>(windows, observer, creator));
            }
        };
    }

    /**
     * This class represents a single window: A sequence of recorded values.
     * 
     * @param <T>
     *            The type of objects which this {@link Window} can hold.
     */
    protected static class Window<T> extends Chunk<T, Observable<T>> {
        /**
         * @return
         *         The mutable underlying {@link Observable} which contains all the
         *         recorded values in this {@link Window} object.
         */
        @Override
        public Observable<T> getContents() {
            return Observable.from(contents);
        }
    }

    public static class UnitTest {

        private TestScheduler scheduler;

        @Before
        @SuppressWarnings("unchecked")
        public void before() {
            scheduler = new TestScheduler();
        }

        private static <T> List<List<T>> toLists(Observable<Observable<T>> observable) {
            final List<T> list = new ArrayList<T>();
            final List<List<T>> lists = new ArrayList<List<T>>();

            observable.subscribe(new Action1<Observable<T>>() {
                @Override
                public void call(Observable<T> tObservable) {
                    tObservable.subscribe(new Action1<T>() {
                        @Override
                        public void call(T t) {
                            list.add(t);
                        }
                    });
                    lists.add(new ArrayList<T>(list));
                    list.clear();
                }
            });
            return lists;
        }

        @Test
        public void testNonOverlappingWindows() {
            Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
            Observable<Observable<String>> windowed = Observable.create(window(subject, 3));

            List<List<String>> windows = toLists(windowed);

            assertEquals(2, windows.size());
            assertEquals(list("one", "two", "three"), windows.get(0));
            assertEquals(list("four", "five"), windows.get(1));
        }

        @Test
        public void testSkipAndCountGaplessEindows() {
            Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
            Observable<Observable<String>> windowed = Observable.create(window(subject, 3, 3));

            List<List<String>> windows = toLists(windowed);

            assertEquals(2, windows.size());
            assertEquals(list("one", "two", "three"), windows.get(0));
            assertEquals(list("four", "five"), windows.get(1));
        }

        @Test
        public void testOverlappingWindows() {
            Observable<String> subject = Observable.from("zero", "one", "two", "three", "four", "five");
            Observable<Observable<String>> windowed = Observable.create(window(subject, 3, 1));

            List<List<String>> windows = toLists(windowed);

            assertEquals(6, windows.size());
            assertEquals(list("zero", "one", "two"), windows.get(0));
            assertEquals(list("one", "two", "three"), windows.get(1));
            assertEquals(list("two", "three", "four"), windows.get(2));
            assertEquals(list("three", "four", "five"), windows.get(3));
            assertEquals(list("four", "five"), windows.get(4));
            assertEquals(list("five"), windows.get(5));
        }

        @Test
        public void testSkipAndCountWindowsWithGaps() {
            Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
            Observable<Observable<String>> windowed = Observable.create(window(subject, 2, 3));

            List<List<String>> windows = toLists(windowed);

            assertEquals(2, windows.size());
            assertEquals(list("one", "two"), windows.get(0));
            assertEquals(list("four", "five"), windows.get(1));
        }

        @Test
        public void testTimedAndCount() {
            final List<String> list = new ArrayList<String>();
            final List<List<String>> lists = new ArrayList<List<String>>();

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

            Observable<Observable<String>> windowed = Observable.create(window(source, 100, TimeUnit.MILLISECONDS, 2, scheduler));
            windowed.subscribe(observeWindow(list, lists));

            scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS);
            assertEquals(1, lists.size());
            assertEquals(lists.get(0), list("one", "two"));

            scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS);
            assertEquals(2, lists.size());
            assertEquals(lists.get(1), list("three", "four"));

            scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
            assertEquals(3, lists.size());
            assertEquals(lists.get(2), list("five"));
        }

        @Test
        public void testTimed() {
            final List<String> list = new ArrayList<String>();
            final List<List<String>> lists = new ArrayList<List<String>>();

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

            Observable<Observable<String>> windowed = Observable.create(window(source, 100, TimeUnit.MILLISECONDS, scheduler));
            windowed.subscribe(observeWindow(list, lists));

            scheduler.advanceTimeTo(101, TimeUnit.MILLISECONDS);
            assertEquals(1, lists.size());
            assertEquals(lists.get(0), list("one", "two", "three"));

            scheduler.advanceTimeTo(201, TimeUnit.MILLISECONDS);
            assertEquals(2, lists.size());
            assertEquals(lists.get(1), list("four", "five"));
        }

        @Test
        public void testObservableBasedOpenerAndCloser() {
            final List<String> list = new ArrayList<String>();
            final List<List<String>> lists = new ArrayList<List<String>>();

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

            Observable<Observable<String>> windowed = Observable.create(window(source, openings, closer));
            windowed.subscribe(observeWindow(list, lists));

            scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
            assertEquals(2, lists.size());
            assertEquals(lists.get(0), list("two", "three"));
            assertEquals(lists.get(1), list("five"));
        }

        @Test
        public void testObservableBasedCloser() {
            final List<String> list = new ArrayList<String>();
            final List<List<String>> lists = new ArrayList<List<String>>();

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

            Observable<Observable<String>> windowed = Observable.create(window(source, closer));
            windowed.subscribe(observeWindow(list, lists));

            scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
            assertEquals(3, lists.size());
            assertEquals(lists.get(0), list("one", "two"));
            assertEquals(lists.get(1), list("three", "four"));
            assertEquals(lists.get(2), list("five"));
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

        private Action1<Observable<String>> observeWindow(final List<String> list, final List<List<String>> lists) {
            return new Action1<Observable<String>>() {
                @Override
                public void call(Observable<String> stringObservable) {
                    stringObservable.subscribe(new Observer<String>() {
                        @Override
                        public void onCompleted() {
                            lists.add(new ArrayList<String>(list));
                            list.clear();
                        }

                        @Override
                        public void onError(Throwable e) {
                            fail(e.getMessage());
                        }

                        @Override
                        public void onNext(String args) {
                            list.add(args);
                        }
                    });
                }
            };
        }

    }
}
