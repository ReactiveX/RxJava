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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.schedulers.Timestamped;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

/**
 * Replay with limited buffer and/or time constraints.
 * 
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.replay.aspx'>MSDN: Observable.Replay overloads</a>
 */
public final class OperationReplay {
    /** Utility class. */
    private OperationReplay() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Create a BoundedReplaySubject with the given buffer size.
     */
    public static <T> Subject<T, T> replayBuffered(int bufferSize) {
        return CustomReplaySubject.create(bufferSize);
    }

    /**
     * Creates a subject whose client observers will observe events
     * propagated through the given wrapped subject.
     */
    public static <T> Subject<T, T> createScheduledSubject(Subject<T, T> subject, Scheduler scheduler) {
        final Observable<T> observedOn = subject.observeOn(scheduler);
        SubjectWrapper<T> s = new SubjectWrapper<T>(new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> o) {
                // TODO HACK between OnSubscribeFunc and Action1
                subscriberOf(observedOn).onSubscribe(o);
            }

        }, subject);
        return s;
    }

    /**
     * Create a CustomReplaySubject with the given time window length
     * and optional buffer size.
     * 
     * @param <T>
     *            the source and return type
     * @param time
     *            the length of the time window
     * @param unit
     *            the unit of the time window length
     * @param bufferSize
     *            the buffer size if >= 0, otherwise, the buffer will be unlimited
     * @param scheduler
     *            the scheduler from where the current time is retrieved. The
     *            observers will not observe on this scheduler.
     * @return a Subject with the required replay behavior
     */
    public static <T> Subject<T, T> replayWindowed(long time, TimeUnit unit, int bufferSize, final Scheduler scheduler) {
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
        Func1<Timestamped<T>, T> untimestamp = new Func1<Timestamped<T>, T>() {
            @Override
            public T call(Timestamped<T> t1) {
                return t1.getValue();
            }
        };

        ReplayState<Timestamped<T>, T> state;

        if (bufferSize >= 0) {
            state = new ReplayState<Timestamped<T>, T>(new VirtualBoundedList<Timestamped<T>>(bufferSize), untimestamp);
        } else {
            state = new ReplayState<Timestamped<T>, T>(new VirtualArrayList<Timestamped<T>>(), untimestamp);
        }
        final ReplayState<Timestamped<T>, T> fstate = state;
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

        final CustomReplaySubject<T, Timestamped<T>, T> brs = new CustomReplaySubject<T, Timestamped<T>, T>(
                new CustomReplaySubjectSubscribeFunc<Timestamped<T>, T>(state), state, timestamp
                );

        return brs;
    }

    /**
     * Return an OnSubscribeFunc which delegates the subscription to the given observable.
     */
    public static <T> OnSubscribeFunc<T> subscriberOf(final Observable<T> target) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> t1) {
                return target.subscribe(t1);
            }
        };
    }

//    /**
//     * Subject that wraps another subject and uses a mapping function
//     * to transform the received values.
//     */
//    public static final class MappingSubject<T, R> extends Subject<T, T> {
//        private final Subject<T, R> subject;
//        private final Func1<T, R> selector;
//        private final OnSubscribe<R> func;
//
//        public MappingSubject(OnSubscribe<R> func, Subject<T, R> subject, Func1<T, R> selector) {
//            this.func = func;
//            this.subject = subject;
//            this.selector = selector;
//        }
//
//        @Override
//        public Observable<R> toObservable() {
//            return Observable.create(func);
//        }
//
//        @Override
//        public void onNext(T args) {
//            subject.onNext(selector.call(args));
//        }
//
//        @Override
//        public void onError(Throwable e) {
//            subject.onError(e);
//        }
//
//        @Override
//        public void onCompleted() {
//            subject.onCompleted();
//        }
//
//    }

    /**
     * A subject that wraps another subject.
     */
    public static final class SubjectWrapper<T> extends Subject<T, T> {
        /** The wrapped subject. */
        final Subject<T, T> subject;

        public SubjectWrapper(OnSubscribe<T> func, Subject<T, T> subject) {
            super(func);
            this.subject = subject;
        }

        @Override
        public void onNext(T args) {
            subject.onNext(args);
        }

        @Override
        public void onError(Throwable e) {
            subject.onError(e);
        }

        @Override
        public void onCompleted() {
            subject.onCompleted();
        }

    }

    /** Base state with lock. */
    static class BaseState {
        /** The lock to protect the other fields. */
        private final Lock lock = new ReentrantLock();

        /** Lock. */
        public void lock() {
            lock.lock();
        }

        /** Unlock. */
        public void unlock() {
            lock.unlock();
        }

    }

    /**
     * Base interface for logically indexing a list.
     * 
     * @param <T>
     *            the value type
     */
    public interface VirtualList<T> {
        /** @return the number of elements in this list */
        int size();

        /**
         * Add an element to the list.
         * 
         * @param value
         *            the value to add
         */
        void add(T value);

        /**
         * Retrieve an element at the specified logical index.
         * 
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
         * 
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
         * 
         * @return
         */
        int start();

        /**
         * Returns the current tail index of this list (where the next value would appear).
         * 
         * @return
         */
        int end();

        /**
         * Clears and resets the indexes of the list.
         */
        void reset();

        /**
         * Returns the current content as a list.
         * 
         * @return
         */
        List<T> toList();
    }

    /**
     * Behaves like a normal, unbounded ArrayList but with virtual index.
     */
    public static final class VirtualArrayList<T> implements VirtualList<T> {
        /** The backing list . */
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
         * 
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

    /**
     * The state class.
     * 
     * @param <TIntermediate>
     *            the intermediate type stored in the values buffer
     * @param <TResult>
     *            the result type transformed via the resultSelector
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
        final Map<Subscription, Replayer> replayers = new LinkedHashMap<Subscription, Replayer>();
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
         * 
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
         * <p>
         * Caller should hold the lock.
         * 
         * @return
         */
        Collection<Replayer> replayers() {
            return new ArrayList<Replayer>(replayers.values());
        }

        /**
         * Add a replayer to the replayers and create a Subscription for it.
         * <p>
         * Caller should hold the lock.
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

                @Override
                public boolean isUnsubscribed() {
                    return once.get();
                }

            };
            Replayer rp = new Replayer(obs, s);
            replayers.put(s, rp);
            rp.replayTill(values.start() + values.size());
            return s;
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
             * 
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
         * 
         * @param s
         */
        void remove(Subscription s) {
            lock();
            try {
                replayers.remove(s);
            } finally {
                unlock();
            }
        }

        /**
         * Add a notification value and limit the size of values.
         * <p>
         * Caller should hold the lock.
         * 
         * @param value
         */
        void add(TIntermediate value) {
            values.add(value);
        }

        /** Clears the value list. */
        void clearValues() {
            lock();
            try {
                values.clear();
            } finally {
                unlock();
            }
        }
    }

    /**
     * A customizable replay subject with support for transformations.
     * 
     * @param <TInput>
     *            the Observer side's value type
     * @param <TIntermediate>
     *            the type of the elements in the replay buffer
     * @param <TResult>
     *            the value type of the observers subscribing to this subject
     */
    public static final class CustomReplaySubject<TInput, TIntermediate, TResult> extends Subject<TInput, TResult> {
        /**
         * Return a subject that retains all events and will replay them to an {@link Observer} that subscribes.
         * 
         * @return a subject that retains all events and will replay them to an {@link Observer} that subscribes.
         */
        public static <T> CustomReplaySubject<T, T, T> create() {
            ReplayState<T, T> state = new ReplayState<T, T>(new VirtualArrayList<T>(), Functions.<T> identity());
            return new CustomReplaySubject<T, T, T>(
                    new CustomReplaySubjectSubscribeFunc<T, T>(state), state,
                    Functions.<T> identity());
        }

        /**
         * Create a bounded replay subject with the given maximum buffer size.
         * 
         * @param maxSize
         *            the maximum size in number of onNext notifications
         * @return
         */
        public static <T> CustomReplaySubject<T, T, T> create(int maxSize) {
            ReplayState<T, T> state = new ReplayState<T, T>(new VirtualBoundedList<T>(maxSize), Functions.<T> identity());
            return new CustomReplaySubject<T, T, T>(
                    new CustomReplaySubjectSubscribeFunc<T, T>(state), state,
                    Functions.<T> identity());
        }

        /** The replay state. */
        protected final ReplayState<TIntermediate, TResult> state;
        /** The result selector. */
        protected final Func1<? super TInput, ? extends TIntermediate> intermediateSelector;

        private CustomReplaySubject(
                final OnSubscribeFunc<TResult> onSubscribe,
                ReplayState<TIntermediate, TResult> state,
                Func1<? super TInput, ? extends TIntermediate> intermediateSelector) {
            super(new OnSubscribe<TResult>() {

                @Override
                public void call(Subscriber<? super TResult> sub) {
                    onSubscribe.onSubscribe(sub);
                }

            });
            this.state = state;
            this.intermediateSelector = intermediateSelector;
        }

        @Override
        public void onCompleted() {
            state.lock();
            try {
                if (state.done) {
                    return;
                }
                state.done = true;
                state.onCompletedAdded.call();
                replayValues();
            } finally {
                state.unlock();
            }
        }

        @Override
        public void onError(Throwable e) {
            state.lock();
            try {
                if (state.done) {
                    return;
                }
                state.done = true;
                state.error = e;
                state.onErrorAdded.call();
                replayValues();
            } finally {
                state.unlock();
            }
        }

        @Override
        public void onNext(TInput args) {
            state.lock();
            try {
                if (state.done) {
                    return;
                }
                state.add(intermediateSelector.call(args));
                state.onValueAdded.call();
                replayValues();
            } finally {
                state.unlock();
            }
        }

        /**
         * Replay values up to the current index.
         */
        protected void replayValues() {
            int s = state.values.start() + state.values.size();
            for (ReplayState.Replayer rp : state.replayers()) {
                rp.replayTill(s);
            }
        }
    }

    /**
     * The subscription function.
     * 
     * @param <TIntermediate>
     *            the type of the elements in the replay buffer
     * @param <TResult>
     *            the value type of the observers subscribing to this subject
     */
    protected static final class CustomReplaySubjectSubscribeFunc<TIntermediate, TResult>
            implements Observable.OnSubscribeFunc<TResult> {

        private final ReplayState<TIntermediate, TResult> state;

        protected CustomReplaySubjectSubscribeFunc(ReplayState<TIntermediate, TResult> state) {
            this.state = state;
        }

        @Override
        public Subscription onSubscribe(Observer<? super TResult> t1) {
            VirtualList<TIntermediate> values;
            Throwable error;
            state.lock();
            try {
                if (!state.done) {
                    state.onSubscription.call();
                    return state.addReplayer(t1);
                }
                values = state.values;
                error = state.error;
            } finally {
                state.unlock();
            }
            // fully replay the subject
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
}
