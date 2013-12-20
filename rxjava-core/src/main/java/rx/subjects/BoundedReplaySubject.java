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
package rx.subjects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.Timestamped;
import rx.util.functions.Action0;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

/**
 * A customizable replay subject with support for transformations
 * and a time/space bounded buffer.
 *
 * @param <TInput> the Observer side's value type
 * @param <TIntermediate> the type of the elements in the replay buffer
 * @param <TResult> the value type of the observers subscribing to this subject
 */
public final class BoundedReplaySubject<TInput, TIntermediate, TResult> extends Subject<TInput, TResult> {
    /**
     * Return a subject that retains all events and will replay them to an {@link Observer} that subscribes.
     * @param <T> the input and output type
     * @return a subject that retains all events and will replay them to an {@link Observer} that subscribes.
     */
    public static <T> Subject<T, T> create() {
        ReplayState<T, T> state = new ReplayState<T, T>(new VirtualArrayList<T>(), Functions.<T>identity());
        return new BoundedReplaySubject<T, T, T>(
                new CustomReplaySubjectSubscribeFunc<T, T>(state), state,
                Functions.<T>identity());
    }
    
    /**
     * Create a bounded replay subject with the given maximum buffer size.
     * @param <T> the input and output type
     * @param bufferSize the maximum size in number of onNext notifications
     * @return the bounded replay subject with the given buffer size
     */
    public static <T> Subject<T, T> createBuffered(int bufferSize) {
        ReplayState<T, T> state = new ReplayState<T, T>(new VirtualBoundedList<T>(bufferSize), Functions.<T>identity());
        return new BoundedReplaySubject<T, T, T>(
                new CustomReplaySubjectSubscribeFunc<T, T>(state), state,
                Functions.<T>identity());
    }
    /**
     * Create a CustomReplaySubject with the given time window length
     * and optional buffer size.
     *
     * @param <T> the source and return type
     * @param time the length of the time window
     * @param unit the unit of the time window length
     * @param scheduler the scheduler from where the current time is retrieved. The
     *                  observers will not observe on this scheduler.
     * @return a Subject with the required replay behavior
     */
    public static <T> Subject<T, T> createWindowed(long time, TimeUnit unit, final Scheduler scheduler) {
        return create(time, unit, -1, scheduler, Functions.<T>identity());
    }

    /**
     * Create a CustomReplaySubject with the given time window length
     * and optional buffer size.
     *
     * @param <T> the source and return type
     * @param time the length of the time window
     * @param unit the unit of the time window length
     * @param bufferSize the buffer size if >= 0, otherwise, the buffer will be unlimited
     * @param scheduler the scheduler from where the current time is retrieved. The
     *                  observers will not observe on this scheduler.
     * @return a Subject with the required replay behavior
     */
    public static <T> Subject<T, T> createWindowedAndBuffered(long time, TimeUnit unit, int bufferSize, final Scheduler scheduler) {
        if (bufferSize < 0) {
            throw new IllegalArgumentException("bufferSize >= 0 required");
        }
        return create(time, unit, bufferSize, scheduler, Functions.<T>identity());
    }
    /**
     * Create a CustomReplaySubject with the given time window length
     * and optional buffer size.
     *
     * @param <T> the source and return type
     * @param time the length of the time window
     * @param unit the unit of the time window length
     * @param bufferSize the buffer size if >= 0, otherwise, the buffer will be unlimited
     * @param scheduler the scheduler from where the current time is retrieved. The
     *                  observers will not observe on this scheduler.
     * @return a Subject with the required replay behavior
     */
    private static <T, U> BoundedReplaySubject<T, Timestamped<T>, U> create(
            long time, TimeUnit unit, int bufferSize, 
            final Scheduler scheduler,
            final Func1<? super T, ? extends U> selector) {
        final long ms = unit.toMillis(time);
        if (ms <= 0) {
            throw new IllegalArgumentException("The time window is less than 1 millisecond!");
        }
        Func1<T, Timestamped<T>> timestamp = new Func1<T, Timestamped<T>>() {
            @Override
            public Timestamped<T> call(T t1) {
                return new Timestamped<T>(scheduler.now(), t1);
            }
        };
        Func1<Timestamped<T>, U> untimestamp = new Func1<Timestamped<T>, U>() {
            @Override
            public U call(Timestamped<T> t1) {
                return selector.call(t1.getValue());
            }
        };
        
        ReplayState<Timestamped<T>, U> state;
        
        if (bufferSize >= 0) {
            state = new ReplayState<Timestamped<T>, U>(new VirtualBoundedList<Timestamped<T>>(bufferSize), untimestamp);
        } else {
            state = new ReplayState<Timestamped<T>, U>(new VirtualArrayList<Timestamped<T>>(), untimestamp);
        }
        final ReplayState<Timestamped<T>, U> fstate = state;
        // time based eviction when a value is added
        state.onValueAdded = new Action0() {
            @Override
            public void call() {
                long now = scheduler.now();
                long before = now - ms;
                for (int i = fstate.values.start(); i < fstate.values.end(); i++) {
                    Timestamped<T> v = fstate.values.get(i);
                    if (v.getTimestampMillis() >= before) {
                        fstate.values.removeBefore(i);
                        break;
                    }
                }
            }
        };
        // time based eviction when a client subscribes
        state.onSubscription = state.onValueAdded;
        
        final BoundedReplaySubject<T, Timestamped<T>, U> brs = new BoundedReplaySubject<T, Timestamped<T>, U>(
                new CustomReplaySubjectSubscribeFunc<Timestamped<T>, U>(state), state, timestamp
        );
        
        return brs;
    }
    
    /**
     * Create an unbounded BoundedReplaySubject with mapping capability.
     * @param <TInput> the input value type
     * @param <TResult> the output value type
     * @param selector the value selector that maps the inputs to the outputs
     * @return an unbounded BoundedReplaySubject with mapping capability 
     */
    public static <TInput, TResult> Subject<TInput, TResult> createMapped(
            Func1<? super TInput, ? extends TResult> selector
    ) {
        ReplayState<TResult, TResult> state = new ReplayState<TResult, TResult>(
                new VirtualArrayList<TResult>(), Functions.<TResult>identity());
        return new BoundedReplaySubject<TInput, TResult, TResult>(
                new CustomReplaySubjectSubscribeFunc<TResult, TResult>(state), state,
                selector);
    }

    /**
     * Create a bounded BoundedReplaySubject with mapping capability.
     * @param <TInput> the input value type
     * @param <TResult> the output value type
     * @param bufferSize the maximum number of elements to buffer
     * @param selector the value selector that maps the inputs to the outputs
     * @return an unbounded BoundedReplaySubject with mapping capability 
     */
    public static <TInput, TResult> Subject<TInput, TResult> createMappedAndBuffered(int bufferSize,
            Func1<? super TInput, ? extends TResult> selector
    ) {
        ReplayState<TResult, TResult> state = new ReplayState<TResult, TResult>(
                new VirtualBoundedList<TResult>(bufferSize), Functions.<TResult>identity());
        return new BoundedReplaySubject<TInput, TResult, TResult>(
                new CustomReplaySubjectSubscribeFunc<TResult, TResult>(state), state,
                selector);
    }
    
    /**
     * Create an unbounded BoundedReplaySubject with mapping capability.
     * @param <TInput> the input value type
     * @param <TResult> the output value type
     * @param time the length of the time window
     * @param unit the unit of the time window length
     * @param scheduler the scheduler from where the current time is retrieved. The
     *                  observers will not observe on this scheduler.
     * @param selector the value selector that maps the inputs to the outputs
     * @return an unbounded BoundedReplaySubject with mapping capability 
     */
    public static <TInput, TResult> Subject<TInput, TResult> createMappedAndWindowed(
            long time, TimeUnit unit, Scheduler scheduler,
            Func1<? super TInput, ? extends TResult> selector
    ) {
        return create(time, unit, -1, scheduler, selector);
    }

    /**
     * Create a bounded BoundedReplaySubject with mapping capability.
     * @param <TInput> the input value type
     * @param <TResult> the output value type
     * @param bufferSize the maximum number of elements to buffer
     * @param time the length of the time window
     * @param unit the unit of the time window length
     * @param scheduler the scheduler from where the current time is retrieved. The
     *                  observers will not observe on this scheduler.
     * @param selector the value selector that maps the inputs to the outputs
     * @return an unbounded BoundedReplaySubject with mapping capability 
     */
    public static <TInput, TResult> Subject<TInput, TResult> createMappedWindowedAndBuffered(int bufferSize,
            long time, TimeUnit unit, Scheduler scheduler,
            Func1<? super TInput, ? extends TResult> selector
    ) {
        if (bufferSize < 0) {
            throw new IllegalArgumentException("bufferSize >= 0 required");
        }
        return create(time, unit, bufferSize, scheduler, selector);
    }

    /** The replay state. */
    protected final ReplayState<TIntermediate, TResult> state;
    /** The result selector. */
    protected final Func1<? super TInput, ? extends TIntermediate> intermediateSelector;
    
    protected BoundedReplaySubject(
            Observable.OnSubscribeFunc<TResult> onSubscribe,
            ReplayState<TIntermediate, TResult> state,
            Func1<? super TInput, ? extends TIntermediate> intermediateSelector) {
        super(onSubscribe);
        this.state = state;
        this.intermediateSelector = intermediateSelector;
    }
    
    
    @Override
    public void onCompleted() {
        state.lockWrite();
        try {
            if (state.done) {
                return;
            }
            state.done = true;
            state.onCompletedAdded.call();
        } finally {
            state.unlockWrite();
        }
        state.replayValues();
    }
    
    @Override
    public void onError(Throwable e) {
        state.lockWrite();
        try {
            if (state.done) {
                return;
            }
            state.done = true;
            state.error = e;
            state.onErrorAdded.call();
        } finally {
            state.unlockWrite();
        }
        state.replayValues();
    }
    
    @Override
    public void onNext(TInput args) {
        state.lockWrite();
        try {
            if (state.done) {
                return;
            }
            state.add(intermediateSelector.call(args));
            state.onValueAdded.call();
        } finally {
            state.unlockWrite();
        }
        state.replayValues();
    }
    /**
     * The subscription function.
     * @param <TIntermediate> the type of the elements in the replay buffer
     * @param <TResult> the value type of the observers subscribing to this subject
     */
    protected static final class CustomReplaySubjectSubscribeFunc<TIntermediate, TResult>
    implements Observable.OnSubscribeFunc<TResult> {
        
        protected final ReplayState<TIntermediate, TResult> state;
        protected CustomReplaySubjectSubscribeFunc(ReplayState<TIntermediate, TResult> state) {
            this.state = state;
        }
        
        @Override
        public Subscription onSubscribe(Observer<? super TResult> t1) {
            VirtualList<TIntermediate> values;
            Throwable error;
            state.lockRead();
            try {
                if (!state.done) {
                    state.onSubscription.call();
                    return state.addReplayer(t1);
                }
                values = state.values;
                error = state.error;
            } finally {
                state.unlockRead();
            }
            // fully replay the completed subject
            for (int i = values.start(); i < values.end(); i++) {
                try {
                    t1.onNext(state.resultSelector.call(values.get(i)));
                } catch (Throwable t) {
                    t1.onError(t);
                    return Subscriptions.empty();
                }
            }
            if (error != null) {
                t1.onError(error);
            } else {
                t1.onCompleted();
            }
            return Subscriptions.empty();
        }
    }
    /**
     * The state class.
     * @param <TIntermediate> the intermediate type stored in the values buffer
     * @param <TResult> the result type transformed via the resultSelector
     */
    static final class ReplayState<TIntermediate, TResult> extends BaseState {
        /** The values observed so far. */
        final VirtualList<TIntermediate> values;
        /** The result selector. */
        final Func1<? super TIntermediate, ? extends TResult> resultSelector;
        /** The received error. */
        Throwable error;
        /** General completion indicator. */
        boolean done;
        /** The map of replayers. */
        final Map<Subscription, Replayer> replayers = new ConcurrentHashMap<Subscription, Replayer>();
        /**
         * Callback once a value has been added but before it is replayed
         * (I.e, run a time based eviction policy).
         * <p>
         * Called while holding the state lock.
         */
        protected Action0 onValueAdded = new Action0() {
            @Override
            public void call() {
            }
        };
        /**
         * Callback once an error has been called but before it is replayed
         * (I.e, run a time based eviction policy).
         * <p>
         * Called while holding the state lock.
         */
        protected Action0 onErrorAdded = new Action0() {
            @Override
            public void call() {
            }
        };
        /**
         * Callback once completed has been called but before it is replayed
         * (I.e, run a time based eviction policy).
         * <p>
         * Called while holding the state lock.
         */
        protected Action0 onCompletedAdded = new Action0() {
            @Override
            public void call() {
            }
        };
        /**
         * Callback to pre-manage the values if an observer unsubscribes
         * (I.e, run a time based eviction policy).
         * <p>
         * Called while holding the state lock.
         */
        protected Action0 onSubscription = new Action0() {
            @Override
            public void call() {
            }
        };
        /**
         * Construct a ReplayState with the supplied buffer and result selectors.
         * @param values
         * @param resultSelector
         */
        public ReplayState(final VirtualList<TIntermediate> values,
                final Func1<? super TIntermediate, ? extends TResult> resultSelector) {
            this.values = values;
            this.resultSelector = resultSelector;
        }
        /**
         * Returns a live collection of the observers.
         * @return
         */
        Collection<Replayer> replayers() {
            return new ArrayList<Replayer>(replayers.values());
        }
        /**
         * Add a replayer to the replayers and create a Subscription for it.
         * <p>
         * Caller should hold the read lock.
         *
         * @param obs
         * @return
         */
        Subscription addReplayer(Observer<? super TResult> obs) {
            Subscription s = new Subscription() {
                final AtomicBoolean once = new AtomicBoolean();
                @Override
                public void unsubscribe() {
                    if (once.compareAndSet(false, true)) {
                        remove(this);
                    }
                }
                
            };
            Replayer rp = new Replayer(obs, s);
            rp.replayTill(values.start() + values.size());
            replayers.put(s, rp); // make it visible to replayValues()
            return s;
        }
        /**
         * Replay values up to the current index.
         * <p>
         * Caller should hold the read lock.
         */
        protected void replayValues() {
            lockRead();
            try {
                int s = values.start() + values.size();
                for (Replayer rp : replayers()) {
                    rp.replayTill(s);
                }
            } finally {
                unlockRead();
            }
        }
        /** The replayer that holds a value where the given observer is currently at. */
        final class Replayer {
            protected final Observer<? super TResult> wrapped;
            /** Where this replayer was in reading the list. */
            protected int index;
            /** To cancel and unsubscribe this replayer and observer. */
            protected final Subscription cancel;
            protected Replayer(Observer<? super TResult> wrapped, Subscription cancel) {
                this.wrapped = wrapped;
                this.cancel = cancel;
            }
            /**
             * Replay up to the given index
             * <p>
             * Caller should hold the read lock.
             * @param limit
             */
            void replayTill(int limit) {
                int si = values.start();
                if (index < si) {
                    index = si;
                }
                while (index < limit) {
                    TIntermediate value = values.get(index);
                    index++;
                    try {
                        wrapped.onNext(resultSelector.call(value));
                    } catch (Throwable t) {
                        replayers.remove(cancel);
                        wrapped.onError(t);
                        return;
                    }
                }
                if (done) {
                    if (error != null) {
                        wrapped.onError(error);
                    } else {
                        wrapped.onCompleted();
                    }
                }
            }
        }
        /**
         * Remove the subscription.
         * @param s
         */
        void remove(Subscription s) {
            replayers.remove(s);
        }
        /**
         * Add a notification value and limit the size of values.
         * <p>
         * Caller should hold the lock.
         * @param value
         */
        void add(TIntermediate value) {
            values.add(value);
        }
        /** Clears the value list. */
        void clearValues() {
            lockWrite();
            try {
                values.clear();
            } finally {
                unlockWrite();
            }
        }
    }
    /** Base state with lock. */
    static class BaseState {
        /** The lock to protect the other fields. */
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        /** The read lock. */
        private final Lock read = rwLock.readLock();
        /** The write lock. */
        private final Lock write = rwLock.writeLock();
        /** Lock read. */
        public void lockRead() {
            read.lock();
        }
        /** Unlock read. */
        public void unlockRead() {
            read.unlock();
        }
        /** Lock write. */
        public void lockWrite() {
            write.lock();
        }
        /** Unlock write. */
        public void unlockWrite() {
            write.unlock();
        }
    }
    /**
     * Base interface for logically indexing a list.
     * @param <T> the value type
     */
    public interface VirtualList<T> {
        /** @return the number of elements in this list */
        int size();
        /**
         * Add an element to the list.
         * @param value the value to add
         */
        void add(T value);
        /**
         * Retrieve an element at the specified logical index.
         * @param index
         * @return
         */
        T get(int index);
        /**
         * Remove elements up before the given logical index and move
         * the start() to this index.
         * <p>
         * For example, a list contains 3 items. Calling removeUntil 2 will
         * remove the first two items.
         * @param index
         */
        void removeBefore(int index);
        /**
         * Clear the elements of this list and increase the
         * start by the number of elements.
         */
        void clear();
        /**
         * Returns the current head index of this list.
         * @return
         */
        int start();
        /**
         * Returns the current tail index of this list (where the next value would appear).
         * @return
         */
        int end();
        /**
         * Clears and resets the indexes of the list.
         */
        void reset();
        /**
         * Returns the current content as a list.
         * @return
         */
        List<T> toList();
    }
    /**
     * Behaves like a normal, unbounded ArrayList but with virtual index.
     * @param <T> the element type
     */
    public static final class VirtualArrayList<T> implements VirtualList<T> {
        /** The backing list .*/
        final List<T> list = new ArrayList<T>();
        /** The virtual start index of the list. */
        int startIndex;
        @Override
        public int size() {
            return list.size();
        }
        @Override
        public void add(T value) {
            list.add(value);
        }
        
        @Override
        public T get(int index) {
            return list.get(index - startIndex);
        }
        
        @Override
        public void removeBefore(int index) {
            int j = index - startIndex;
            if (j > 0 && j <= list.size()) {
                list.subList(0, j).clear();
            }
            startIndex = index;
        }
        
        @Override
        public void clear() {
            startIndex += list.size();
            list.clear();
        }
        
        @Override
        public int start() {
            return startIndex;
        }
        
        @Override
        public int end() {
            return startIndex + list.size();
        }
        
        @Override
        public void reset() {
            list.clear();
            startIndex = 0;
        }
        @Override
        public List<T> toList() {
            return new ArrayList<T>(list);
        }
        
    }
    /**
     * A bounded list which increases its size up to a maximum capacity, then
     * behaves like a circular buffer with virtual indexes.
     * @param <T> the element type
     */
    public static final class VirtualBoundedList<T> implements VirtualList<T> {
        /** A list that grows up to maxSize. */
        private final List<T> list = new ArrayList<T>();
        /** The maximum allowed size. */
        private final int maxSize;
        /** The logical start index of the list. */
        int startIndex;
        /** The head index inside the list, where the first readable value sits. */
        int head;
        /** The tail index inside the list, where the next value will be added. */
        int tail;
        /** The number of items in the list. */
        int count;
        /**
         * Construct a VirtualBoundedList with the given maximum number of elements.
         * @param maxSize
         */
        public VirtualBoundedList(int maxSize) {
            if (maxSize < 0) {
                throw new IllegalArgumentException("maxSize < 0");
            }
            this.maxSize = maxSize;
        }
        @Override
        public int start() {
            return startIndex;
        }
        
        @Override
        public int end() {
            return startIndex + count;
        }
        
        @Override
        public void clear() {
            startIndex += count;
            list.clear();
            head = 0;
            tail = 0;
            count = 0;
        }
        @Override
        public int size() {
            return count;
        }
        
        @Override
        public void add(T value) {
            if (list.size() == maxSize) {
                list.set(tail, value);
                head = (head + 1) % maxSize;
                tail = (tail + 1) % maxSize;
                startIndex++;
            } else {
                list.add(value);
                tail = (tail + 1) % maxSize;
                count++;
            }
        }
        
        @Override
        public T get(int index) {
            if (index < start() || index >= end()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            int idx = (head + (index - startIndex)) % maxSize;
            return list.get(idx);
        }
        
        @Override
        public void removeBefore(int index) {
            if (index <= start()) {
                return;
            }
            if (index >= end()) {
                clear();
                startIndex = index;
                return;
            }
            int rc = index - startIndex;
            int head2 = head + rc;
            for (int i = head; i < head2; i++) {
                list.set(i % maxSize, null);
                count--;
            }
            startIndex = index;
            head = head2 % maxSize;
        }
        @Override
        public List<T> toList() {
            List<T> r = new ArrayList<T>(list.size() + 1);
            for (int i = head; i < head + count; i++) {
                int idx = i % maxSize;
                r.add(list.get(idx));
            }
            return r;
        }
        
        @Override
        public void reset() {
            list.clear();
            count = 0;
            head = 0;
            tail = 0;
        }
    }
}
