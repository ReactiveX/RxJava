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
package rx.subjects;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.operators.NotificationLite;
import rx.schedulers.Timestamped;
import rx.subjects.ReplaySubject.WeakLinkedList.Node;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

/**
 * Subject that retains all events and will replay them to an {@link Observer} that subscribes.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.ReplaySubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * ReplaySubject<Object> subject = ReplaySubject.create();
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");
  subject.onCompleted();

  // both of the following will get the onNext/onCompleted calls from above
  subject.subscribe(observer1);
  subject.subscribe(observer2);

  } </pre>
 * 
 * @param <T>
 */
public final class ReplaySubject<T> extends Subject<T, T> {
    /**
     * Create an unbounded replay subject with an initial buffer capacity of 16.
     * @param <T> The input and output types
     * @return the created subject
     */
    public static <T> ReplaySubject<T> create() {
        return create(16);
    }
    /**
     * Create an unbounded replay subject with the specified initial buffer capacity.
     * @param <T> The input and output types
     * @param capacity the initial buffer capacity
     * @return the created subject
     */
    public static <T> ReplaySubject<T> create(int capacity) {
        final UnboundedReplayState<T> state = new UnboundedReplayState<T>(capacity);
        SubjectSubscriptionManager<T> ssm = new SubjectSubscriptionManager<T>();
        OnSubscribe<T> onSubscribe = ssm.getOnSubscribeFunc(
            /**
             * This function executes at beginning of subscription.
             * We want to replay history with the subscribing thread
             * before the Observer gets registered.
             * 
             * This will always run, even if Subject is in terminal state.
             */
            new Action1<SubjectObserver<? super T>>() {

                @Override
                public void call(SubjectObserver<? super T> o) {
                    // replay history for this observer using the subscribing thread
                    int lastIndex = state.replayObserverFromIndex(0, o);

                    // now that it is caught up add to observers
                    state.replayState.put(o, lastIndex);
                }
            },
            /**
             * This function executes if the Subject is terminated.
             */
            new Action1<SubjectObserver<? super T>>() {

                @Override
                public void call(SubjectObserver<? super T> o) {
                    Integer idx = state.replayState.remove(o);
                    // we will finish replaying if there is anything left
                    state.replayObserverFromIndex(idx, o);
                }
            },
            new Action1<SubjectObserver<? super T>>() {
                @Override
                public void call(SubjectObserver<? super T> o) {
                    state.replayState.remove(o);
                }
            }

        );
        
        return new ReplaySubject<T>(onSubscribe, ssm, state);
    }
    /**
     * Create an unbounded replay subject with the bounded-implementation for testing purposes.
     * @param <T> the input and output types
     * @param size the maximum buffer size
     * @return the created subject
     */
    /* public */ static <T> ReplaySubject<T> createUnbounded() {
        final BoundedState<T> state = new BoundedState<T>(
            new EmptyEvictionPolicy(),
            Functions.identity(),
            Functions.identity()
        );
        return createWithState(state, new DefaultOnAdd<T>(state));
    }
    /**
     * Create a size-bounded replay subject.
     * @param <T> the input and output types
     * @param size the maximum number of buffered items
     * @return the created subject
     */
    public static <T> ReplaySubject<T> createWithSize(int size) {
        final BoundedState<T> state = new BoundedState<T>(
            new SizeEvictionPolicy(size),
            Functions.identity(),
            Functions.identity()
        );
        return createWithState(state, new DefaultOnAdd<T>(state));
    }
    /**
     * Create a time-bounded replay subject.
     * @param <T> the input and output types
     * @param time the maximum age of the contained items
     * @param unit the time unit
     * @param scheduler the scheduler providing the current time
     * @return the created subject
     */
    public static <T> ReplaySubject<T> createWithTime(long time, TimeUnit unit, final Scheduler scheduler) {
        final BoundedState<T> state = new BoundedState<T>(
                new TimeEvictionPolicy(unit.toMillis(time), scheduler),
                new AddTimestamped(scheduler),
                new RemoveTimestamped()
        );
        return createWithState(state, new TimedOnAdd<T>(state, scheduler));
    }
    /**
     * Create a time- and size-bounded replay subject.
     * @param <T> the input and output types
     * @param time the maximum age of the contained items
     * @param unit the time unit
     * @param size the maximum number of buffered items
     * @param scheduler the scheduler providing the current time
     * @return the created subject
     */
    public static <T> ReplaySubject<T> createWithTimeAndSize(long time, TimeUnit unit, int size, final Scheduler scheduler) {
        final BoundedState<T> state = new BoundedState<T>(
                new PairEvictionPolicy(
                        new SizeEvictionPolicy(size),
                        new TimeEvictionPolicy(unit.toMillis(time), scheduler)
                ),
                new AddTimestamped(scheduler),
                new RemoveTimestamped()
        );
        return createWithState(state, new TimedOnAdd<T>(state, scheduler));
    }
    /**
     * Create a bounded replay subject wiht the given state shared between the subject
     * and the OnSubscribe functions.
     * @param <T> the result value type
     * @param state the shared state
     * @return the subject created
     */
    static final <T> ReplaySubject<T> createWithState(final BoundedState<T> state,
            Action1<SubjectObserver<? super T>> onAdd) {
        SubjectSubscriptionManager<T> ssm = new SubjectSubscriptionManager<T>();
        OnSubscribe<T> onSubscribe = ssm.getOnSubscribeFunc(
            // onAdded
            onAdd,
            // onTerminated
            new Action1<SubjectObserver<? super T>>() {

                @Override
                public void call(SubjectObserver<? super T> t1) {
                    WeakLinkedList.Node<Object> l = state.removeState(t1);
                    state.replayObserverFromIndex(l, t1);
                }

            },
            // onUnsubscribed
            new Action1<SubjectObserver<? super T>>() {

                @Override
                public void call(SubjectObserver<? super T> t1) {
                    state.removeState(t1);
                }

            }

        );
        
        return new ReplaySubject<T>(onSubscribe, ssm, state);
    }
    /** The state storing the history and the references. */
    final ReplayState<T, ?> state;
    /** The manager of subscribers. */
    final SubjectSubscriptionManager<T> ssm;
    ReplaySubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> ssm, ReplayState<T, ?> state) {
        super(onSubscribe);
        this.ssm = ssm;
        this.state = state;
    }
    
    @Override
    public void onNext(T t) {
        if (state.terminated()) {
            return;
        }
        state.next(t);
        for (SubjectSubscriptionManager.SubjectObserver<? super T> o : ssm.rawSnapshot()) {
            if (caughtUp(o)) {
                o.onNext(t);
            }
        }
    }
    
    @Override
    public void onError(final Throwable e) {
        Collection<SubjectObserver<? super T>> observers = ssm.terminate(new Action0() {
            
            @Override
            public void call() {
                state.error(e);
            }
        });
        if (observers != null) {
            for (SubjectObserver<? super T> o : observers) {
                if (caughtUp(o)) {
                    o.onError(e);
                }
            }
        }
    }
    
    @Override
    public void onCompleted() {
        Collection<SubjectObserver<? super T>> observers = ssm.terminate(new Action0() {
            
            @Override
            public void call() {
                state.complete();
            }
        });
        if (observers != null) {
            for (SubjectObserver<? super T> o : observers) {
                if (caughtUp(o)) {
                    o.onCompleted();
                }
            }
        }
    }
    /**
     * @return Returns the number of subscribers.
     */
    /* Support test. */int subscriberCount() {
        return state.replayStateSize();
    }
    
    private boolean caughtUp(SubjectObserver<? super T> o) {
        if (!o.caughtUp) {
            o.caughtUp = true;
            state.replayObserver(o);
            return false;
        } else {
            // it was caught up so proceed the "raw route"
            return true;
        }
    }
    
    // *********************
    // State implementations
    // *********************
    
    /**
     * The unbounded replay state.
     * @param <T> the input and output type
     */
    static final class UnboundedReplayState<T> implements ReplayState<T, Integer> {
        /** Each Observer is tracked here for what events they have received. */
        final ConcurrentHashMap<Observer<? super T>, Integer> replayState;
        private final NotificationLite<T> nl = NotificationLite.instance();
        /** The size of the buffer. */
        private final AtomicInteger index;
        /** The buffer. */
        private final ArrayList<Object> list;
        /** The termination flag. */
        private volatile boolean terminated;
        public UnboundedReplayState(int initialCapacity) {
            index = new AtomicInteger(0);
            list = new ArrayList<Object>(initialCapacity);
            replayState = new ConcurrentHashMap<Observer<? super T>, Integer>();
        }

        @Override
        public void next(T n) {
            if (!terminated) {
                list.add(nl.next(n));
                index.getAndIncrement();
            }
        }

        public void accept(Observer<? super T> o, int idx) {
            nl.accept(o, list.get(idx));
        }
        
        @Override
        public void complete() {
            if (!terminated) {
                terminated = true;
                list.add(nl.completed());
                index.getAndIncrement();
            }
        }
        @Override
        public void error(Throwable e) {
            if (!terminated) {
                terminated = true;
                list.add(nl.error(e));
                index.getAndIncrement();
            }
        }

        @Override
        public boolean terminated() {
            return terminated;
        }

        @Override
        public void replayObserver(SubjectObserver<? super T> observer) {
            Integer lastEmittedLink = replayState.get(observer);
            if (lastEmittedLink != null) {
                int l = replayObserverFromIndex(lastEmittedLink, observer);
                replayState.put(observer, l);
            } else {
                throw new IllegalStateException("failed to find lastEmittedLink for: " + observer);
            }
        }

        @Override
        public Integer replayObserverFromIndex(Integer idx, SubjectObserver<? super T> observer) {
            int i = idx;
            while (i < index.get()) {
                accept(observer, i);
                i++;
            }

            return i;
        }

        @Override
        public Integer replayObserverFromIndexTest(Integer idx, SubjectObserver<? super T> observer, long now) {
            return replayObserverFromIndex(idx, observer);
        }

        @Override
        public int replayStateSize() {
            return replayState.size();
        }
        
    }
    
    
    /** 
     * The bounded replay state. 
     * @param <T> the input and output type
     */
    static final class BoundedState<T> implements ReplayState<T, WeakLinkedList.Node<Object>> {
        final WeakLinkedList<Object> list;
        final AtomicReference<WeakLinkedList.Node<Object>> tail;
        final ConcurrentHashMap<Observer<? super T>, WeakLinkedList.Node<Object>> replayState;
        final EvictionPolicy evictionPolicy;
        final Func1<Object, Object> enterTransform;
        final Func1<Object, Object> leaveTransform;
        final NotificationLite<T> nl = NotificationLite.instance();
        volatile boolean terminated;
        public BoundedState(EvictionPolicy evictionPolicy, Func1<Object, Object> enterTransform, 
                Func1<Object, Object> leaveTransform) {
            this.list = new WeakLinkedList<Object>();
            this.tail = new AtomicReference<WeakLinkedList.Node<Object>>(list.tail);
            this.replayState = new ConcurrentHashMap<Observer<? super T>, WeakLinkedList.Node<Object>>();
            this.evictionPolicy = evictionPolicy;
            this.enterTransform = enterTransform;
            this.leaveTransform = leaveTransform;
        }
        @Override
        public void next(T value) {
            if (!terminated) {
                list.addLast(enterTransform.call(nl.next(value)));
                evictionPolicy.evict(list);
                tail.set(list.tail);
            }
        }
        @Override
        public void complete() {
            if (!terminated) {
                terminated = true;
                // don't evict the terminal value
                evictionPolicy.evict(list);
                // so add it later
                list.addLast(enterTransform.call(nl.completed()));
                tail.set(list.tail);
            }
            
        }
        @Override
        public void error(Throwable e) {
            if (!terminated) {
                terminated = true;
                // don't evict the terminal value
                evictionPolicy.evict(list);
                // so add it later
                list.addLast(enterTransform.call(nl.error(e)));
                tail.set(list.tail);
            }
        }
        public void accept(Observer<? super T> o, WeakLinkedList.Node<Object> node) {
            nl.accept(o, leaveTransform.call(node.value()));
        }
        /**
         * Accept only non-stale nodes.
         * @param o the target observer
         * @param node the node to accept or reject
         * @param now the current time
         */
        public void acceptTest(Observer<? super T> o, WeakLinkedList.Node<Object> node, long now) {
            Object v = node.value();
            if (!evictionPolicy.test(v, now)) {
                nl.accept(o, leaveTransform.call(v));
            }
        }
        public Node<Object> head() {
            return list.head;
        }
        public Node<Object> tail() {
            return tail.get();
        }
        public Node<Object> removeState(SubjectObserver<? super T> o) {
            return replayState.remove(o);
        }
        public void addState(SubjectObserver<? super T> o, Node<Object> state) {
            if (state == null) {
                // replayState.remove(o);
                throw new IllegalStateException("Null state!");
            } else {
                replayState.put(o, state);
            }
        }
        @Override
        public void replayObserver(SubjectObserver<? super T> observer) {
            WeakLinkedList.Node<Object> lastEmittedLink = replayState.get(observer);
            WeakLinkedList.Node<Object> l = replayObserverFromIndex(lastEmittedLink, observer);
            addState(observer, l);
        }

        @Override
        public WeakLinkedList.Node<Object> replayObserverFromIndex(
                WeakLinkedList.Node<Object> l, SubjectObserver<? super T> observer) {
            while (l != tail()) {
                accept(observer, l.next);
                l = l.next;
            }
            return l;
        }
        @Override
        public WeakLinkedList.Node<Object> replayObserverFromIndexTest(
                WeakLinkedList.Node<Object> l, SubjectObserver<? super T> observer, long now) {
            while (l != tail()) {
                acceptTest(observer, l.next, now);
                l = l.next;
            }
            return l;
        }

        @Override
        public boolean terminated() {
            return terminated;
        }

        @Override
        public int replayStateSize() {
            return replayState.size();
        }
    }
    
    // **************
    // API interfaces
    // **************
    
    /**
     * General API for replay state management.
     * @param <T> the input and output type
     * @param <I> the index type
     */
    interface ReplayState<T, I> {
        /** @return true if the subject has reached a terminal state. */
        boolean terminated();
        void replayObserver(SubjectObserver<? super T> observer);
        /**
         * Replay the buffered values from an index position and return a new index
         * @param idx the current index position
         * @param observer the receiver of events
         * @return the new index position
         */
        I replayObserverFromIndex(
                I idx, SubjectObserver<? super T> observer);
        /**
         * Replay the buffered values from an index position while testing for stale entries and return a new index
         * @param idx the current index position
         * @param observer the receiver of events
         * @return the new index position
         */
        I replayObserverFromIndexTest(
                I idx, SubjectObserver<? super T> observer, long now);
        /**
         * @return the size of the replay state map for testing purposes.
         */
        int replayStateSize();
        /**
         * Add an OnNext value to the buffer
         * @param value the value to add
         */
        void next(T value);
        /**
         * Add an OnError exception and terminate the subject
         * @param e the exception to add
         */
        void error(Throwable e);
        /**
         * Add an OnCompleted exception and terminate the subject
         */
        void complete();
    }
    
    /** Interface to manage eviction checking. */
    interface EvictionPolicy {
        /**
         * Subscribe-time checking for stale entries.
         * @param value the value to test
         * @param now the current time
         * @return true if the value may be evicted
         */
        boolean test(Object value, long now);
        /**
         * Evict values from the list
         * @param list 
         */
        void evict(WeakLinkedList<Object> list);
    }

    
    // ************************
    // Callback implementations
    // ************************
    
    /**
     * Remove elements from the beginning of the list if the size exceeds some threshold.
     */
    static final class SizeEvictionPolicy implements EvictionPolicy {
        final int maxSize;
        
        public SizeEvictionPolicy(int maxSize) {
            this.maxSize = maxSize;
        }
        
        @Override
        public void evict(WeakLinkedList<Object> t1) {
            while (t1.size() > maxSize) {
                t1.removeFirst();
            }
        }

        @Override
        public boolean test(Object value, long now) {
            return true; // size gets never stale
        }
    }
    /**
     * Remove elements from the beginning of the list if the Timestamped value is older than
     * a threshold.
     */
    static final class TimeEvictionPolicy implements EvictionPolicy {
        final long maxAgeMillis;
        final Scheduler scheduler;
        
        public TimeEvictionPolicy(long maxAgeMillis, Scheduler scheduler) {
            this.maxAgeMillis = maxAgeMillis;
            this.scheduler = scheduler;
        }
        
        @Override
        public void evict(WeakLinkedList<Object> t1) {
            long now = scheduler.now();
            while (!t1.isEmpty()) {
                WeakLinkedList.Node<Object> n = t1.head.next;
                if (test(n.value(), now)) {
                    t1.removeFirst();
                } else {
                    break;
                }
            }
        }

        @Override
        public boolean test(Object value, long now) {
            Timestamped<?> ts = (Timestamped<?>)value;
            return ts.getTimestampMillis() <= now - maxAgeMillis;
        }
        
    }
    /**
     * Pairs up two eviction policy callbacks.
     */
    static final class PairEvictionPolicy implements EvictionPolicy {
        final EvictionPolicy first;
        final EvictionPolicy second;
        
        public PairEvictionPolicy(EvictionPolicy first, EvictionPolicy second) {
            this.first = first;
            this.second = second;
        }
        
        @Override
        public void evict(WeakLinkedList<Object> t1) {
            first.evict(t1);
            second.evict(t1);
        }

        @Override
        public boolean test(Object value, long now) {
            return first.test(value, now) || second.test(value, now);
        }
    };
    
    /** Maps the values to Timestamped. */
    static final class AddTimestamped implements Func1<Object, Object> {
        final Scheduler scheduler;

        public AddTimestamped(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public Object call(Object t1) {
            return new Timestamped<Object>(scheduler.now(), t1);
        }
    }
    /** Maps timestamped values back to raw objects. */
    static final class RemoveTimestamped implements Func1<Object, Object> {
        @Override
        @SuppressWarnings("unchecked")
        public Object call(Object t1) {
            return ((Timestamped<Object>)t1).getValue();
        }
    }
    /**
     * Default action of simply replaying the buffer on subscribe.
     * @param <T> the input and output value type
     */
    static final class DefaultOnAdd<T> implements Action1<SubjectObserver<? super T>> {
        final BoundedState<T> state;

        public DefaultOnAdd(BoundedState<T> state) {
            this.state = state;
        }
        
        @Override
        public void call(SubjectObserver<? super T> t1) {
            WeakLinkedList.Node<Object> l = state.replayObserverFromIndex(state.head(), t1);
            state.addState(t1, l);
        }
        
    }
    /**
     * Action of replaying non-stale entries of the buffer on subscribe
     * @param <T> the input and output value
     */
    static final class TimedOnAdd<T> implements Action1<SubjectObserver<? super T>> {
        final BoundedState<T> state;
        final Scheduler scheduler;

        public TimedOnAdd(BoundedState<T> state, Scheduler scheduler) {
            this.state = state;
            this.scheduler = scheduler;
        }
        
        @Override
        public void call(SubjectObserver<? super T> t1) {
            WeakLinkedList.Node<Object> l;
            if (!state.terminated) {
                // ignore stale entries if still active
                l = state.replayObserverFromIndexTest(state.head(), t1, scheduler.now());
            }  else {
                // accept all if terminated
                l = state.replayObserverFromIndex(state.head(), t1);
            }
            state.addState(t1, l);
        }
        
    }
    /**
     * A doubly-linked list that has a weak reference backwards.
     * @param <T> the value type
     */
    static final class WeakLinkedList<T> {
        /**
         * The node containing the value and references to neighbours.
         * @param <T> the value type
         */
        static final class Node<T> {
            /** The managed value. */
            final T value;
            /**
             * The weak reference back to the previous node. If that node is not
             * hard-referenced, it will be eventually GCd.
             */
            final WeakReference<Node<T>> previous;
            /** The hard reference to the next node. */
            volatile Node<T> next;
            Node(T value, Node<T> previous) {
                this.value = value;
                this.previous = new WeakReference<Node<T>>(previous);
            }
            /**
             * @return returns the next node in the list or null if this is the last node up to now
             */
            public Node<T> next() {
                return next;
            }
            /** @return the managed value. */
            public T value() {
                return value;
            }
        }
        /** The head of the list. */
        final Node<T> head = new Node<T>(null, null);
        /** The tail of the list. */
        Node<T> tail = head;
        /** The number of elements in the list. */
        int size;
        
        public void addLast(T value) {
            Node<T> t = tail;
            Node<T> t2 = new Node<T>(value, t);
            t.next = t2;
            tail = t2;
            size++;
        }
        public T removeFirst() {
            if (head.next == null) {
                throw new IllegalStateException("Empty!");
            }
            Node<T> t = head.next;
            head.next = t.next;
            if (head.next == null) {
                tail = head;
            }
            size--;
            return t.value();
        }
        public boolean isEmpty() {
            return size == 0;
        }
        public int size() {
            return size;
        }
        public void clear() {
            tail = head;
            size = 0;
        }
    }
    /** Empty eviction policy. */
    static final class EmptyEvictionPolicy implements EvictionPolicy {
        @Override
        public boolean test(Object value, long now) {
            return true;
        }
        @Override
        public void evict(WeakLinkedList<Object> list) {
        }
        
    }    
}