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

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observer;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.operators.NotificationLite;
import rx.schedulers.Timestamped;
import rx.subjects.ReplaySubject.NodeList.Node;
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
 * @param <T> the input and output type
 */
public final class ReplaySubject<T> extends Subject<T, T> {
    /**
     * Create an unbounded replay subject.
     * <p>The internal buffer is backed by an {@link ArrayList} and starts with
     * an initial capacity of 16. Once the number of elements reaches this capacity,
     * it will grow as necessary (usually by 50%). However, as the number of
     * elements grows, this causes frequent array reallocation and copying, and
     * may hurt performance and latency. This can be avoided with the {@link #create(int)}
     * overload which takes an initial capacity parameter and can be tuned to
     * reduce the array reallocation frequency as needed.
     * @param <T> The input and output types
     * @return the created subject
     */
    public static <T> ReplaySubject<T> create() {
        return create(16);
    }
    /**
     * Create an unbounded replay subject with the specified initial buffer capacity.
     * <p>Use this method to avoid excessive array reallocation while the internal
     * buffer grows to accomodate new elements. For example, if it is known that the
     * buffer will hold 32k elements, one can ask the ReplaySubject to preallocate
     * it internal array with a capacity to hold that many elements. Once the elements
     * start to arrive, the internal array won't need to grow, creating less garbage and
     * no overhead due to frequent array-copying.
     * @param <T> The input and output types
     * @param capacity the initial buffer capacity
     * @return the created subject
     */
    public static <T> ReplaySubject<T> create(int capacity) {
        final UnboundedReplayState<T> state = new UnboundedReplayState<T>(capacity);
        SubjectSubscriptionManager<T> ssm = new SubjectSubscriptionManager<T>();
        ssm.onStart = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> o) {
                // replay history for this observer using the subscribing thread
                int lastIndex = state.replayObserverFromIndex(0, o);

                // now that it is caught up add to observers
                state.replayState.put(o, lastIndex);
            }
        };
        ssm.onTerminated = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> o) {
                Integer idx = state.replayState.remove(o);
                if (idx == null) {
                    idx = 0;
                }
                // we will finish replaying if there is anything left
                state.replayObserverFromIndex(idx, o);
            }
        };
        ssm.onUnsubscribed = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> o) {
                state.replayState.remove(o);
            }
        };
        
        return new ReplaySubject<T>(ssm, ssm, state);
    }
    /**
     * Create an unbounded replay subject with the bounded-implementation for testing purposes.
     * <p>This variant behaves like the regular unbounded ReplaySubject created via {@link #create()}
     * but uses the structures of the bounded-implementation. This is by no means intended
     * for the replacement of the original, array-backed and unbounded ReplaySubject due to
     * the additional overhead of the linked-list based internal buffer. The sole purpose
     * is to allow testing and reasoning about the behavior of the bounded implementations
     * without the interference of the eviction policies.
     * @param <T> the input and output types
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
     * <p>In this setting, the ReplaySubject holds at most {@code size} elements in its
     * internal buffer and discards the oldest element. 
     * <p>When observers subscribe to a terminated
     * ReplaySubject, they are guaranteed to see at most {@code size} onNext events followed by 
     * a termination event. 
     * <p>In case an observer subscribes while the ReplaySubject is active, it
     * will receive all events from within the buffer at that point in time and each event afterwards,
     * even if the buffer evicts elements due to the size constraint in the mean time. 
     * In other terms, once an Observer subscribes, it will receive events without gaps in the sequence.
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
     * <p>In this setting, the ReplaySubject tags each received onNext event internally with a timestamp
     * value supplied by the {@link Scheduler} and keeps only those whose age is less than
     * the supplied time value converted to milliseconds. For example, a value arrives at T=0 and the max age
     * is set to 5; at T&gt;=5 this first value is then evicted by any subsequent value or termination event, 
     * leaving the buffer empty. 
     * <p>Once the subject is terminated, observers subscribing to it will receive events that
     * remained in the buffer after the terminal event, regardless of their age. 
     * <p>In case an observer subscribes while the ReplaySubject is active, it
     * will receive only those events from within the buffer, which have age less than the specified time and
     * each event afterwards, even if the buffer evicts elements due to the time constraint in the mean time. 
     * In other terms, once an Observer subscribes, it receives events without gaps in the sequence except the outdated events
     * at the beginning of the sequence.
     * <p>Note that terminal events (onError and onCompleted) trigger eviction as well. For example, with a max age of
     * 5, the first value arrives at T=0, then an onCompleted arrives at T=10. If an observer subscribes at T=11, it will
     * find an empty ReplaySubject with just an onCompleted event.
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
     * <p>In this setting, the ReplaySubject tags each received onNext event internally with a timestamp
     * value supplied by the {@link Scheduler} and holds at most {@code size} elements in the internal buffer.
     * Elements are evicted from the start of the buffer if their age becomes less-than or equal 
     * to the supplied age in milliseconds or the buffer reaches its {@code size} limit.
     * <p>When observers subscribe to a terminated ReplaySubject, receive the events that
     * remained in the buffer after the terminal event, regardless of their age, but at most
     * {@code size} elements.
     * <p>In case an observer subscribes while the ReplaySubject is active, it
     * will receive only those events from within the buffer, which have age less than the specified time and
     * each event afterwards, even if the buffer evicts elements due to the time constraint in the mean time. 
     * In other terms, once an Observer subscribes, it receives events without gaps in the sequence except the outdated events
     * at the beginning of the sequence.
     * <p>Note that terminal events (onError and onCompleted) trigger eviction as well. For example, with a max age of
     * 5, the first value arrives at T=0, then an onCompleted arrives at T=10. If an observer subscribes at T=11, it will
     * find an empty ReplaySubject with just an onCompleted event.
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
     * Create a bounded replay subject with the given state shared between the subject
     * and the OnSubscribe functions.
     * @param <T> the result value type
     * @param state the shared state
     * @return the subject created
     */
    static final <T> ReplaySubject<T> createWithState(final BoundedState<T> state,
            Action1<SubjectObserver<T>> onStart) {
        SubjectSubscriptionManager<T> ssm = new SubjectSubscriptionManager<T>();
        ssm.onStart = onStart;
        ssm.onTerminated = new Action1<SubjectObserver<T>>() {

            @Override
            public void call(SubjectObserver<T> t1) {
                NodeList.Node<Object> l = state.removeState(t1);
                if (l == null) {
                    l = state.head();
                }
                state.replayObserverFromIndex(l, t1);
            }

        };
        ssm.onUnsubscribed = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> t1) {
                state.removeState(t1);
            }

        };
        
        return new ReplaySubject<T>(ssm, ssm, state);
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
        if (ssm.active) {
            state.next(t);
            for (SubjectSubscriptionManager.SubjectObserver<? super T> o : ssm.observers()) {
                if (caughtUp(o)) {
                    o.onNext(t);
                }
            }
        }
    }
    
    @Override
    public void onError(final Throwable e) {
        if (ssm.active) {
            state.error(e);
            for (SubjectObserver<? super T> o : ssm.terminate(NotificationLite.instance().error(e))) {
                if (caughtUp(o)) {
                    o.onError(e);
                }
            }
        }
    }
    
    @Override
    public void onCompleted() {
        if (ssm.active) {
            state.complete();
            for (SubjectObserver<? super T> o : ssm.terminate(NotificationLite.instance().completed())) {
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
    static final class BoundedState<T> implements ReplayState<T, NodeList.Node<Object>> {
        final NodeList<Object> list;
        final AtomicReference<NodeList.Node<Object>> tail;
        final ConcurrentHashMap<Observer<? super T>, NodeList.Node<Object>> replayState;
        final EvictionPolicy evictionPolicy;
        final Func1<Object, Object> enterTransform;
        final Func1<Object, Object> leaveTransform;
        final NotificationLite<T> nl = NotificationLite.instance();
        volatile boolean terminated;
        public BoundedState(EvictionPolicy evictionPolicy, Func1<Object, Object> enterTransform, 
                Func1<Object, Object> leaveTransform) {
            this.list = new NodeList<Object>();
            this.tail = new AtomicReference<NodeList.Node<Object>>(list.tail);
            this.replayState = new ConcurrentHashMap<Observer<? super T>, NodeList.Node<Object>>();
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
        public void accept(Observer<? super T> o, NodeList.Node<Object> node) {
            nl.accept(o, leaveTransform.call(node.value));
        }
        /**
         * Accept only non-stale nodes.
         * @param o the target observer
         * @param node the node to accept or reject
         * @param now the current time
         */
        public void acceptTest(Observer<? super T> o, NodeList.Node<Object> node, long now) {
            Object v = node.value;
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
                throw new IllegalStateException("Null state!");
            } else {
                replayState.put(o, state);
            }
        }
        @Override
        public void replayObserver(SubjectObserver<? super T> observer) {
            NodeList.Node<Object> lastEmittedLink = replayState.get(observer);
            NodeList.Node<Object> l = replayObserverFromIndex(lastEmittedLink, observer);
            addState(observer, l);
        }

        @Override
        public NodeList.Node<Object> replayObserverFromIndex(
                NodeList.Node<Object> l, SubjectObserver<? super T> observer) {
            while (l != tail()) {
                accept(observer, l.next);
                l = l.next;
            }
            return l;
        }
        @Override
        public NodeList.Node<Object> replayObserverFromIndexTest(
                NodeList.Node<Object> l, SubjectObserver<? super T> observer, long now) {
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
        void evict(NodeList<Object> list);
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
        public void evict(NodeList<Object> t1) {
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
        public void evict(NodeList<Object> t1) {
            long now = scheduler.now();
            while (!t1.isEmpty()) {
                NodeList.Node<Object> n = t1.head.next;
                if (test(n.value, now)) {
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
        public void evict(NodeList<Object> t1) {
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
    static final class DefaultOnAdd<T> implements Action1<SubjectObserver<T>> {
        final BoundedState<T> state;

        public DefaultOnAdd(BoundedState<T> state) {
            this.state = state;
        }
        
        @Override
        public void call(SubjectObserver<T> t1) {
            NodeList.Node<Object> l = state.replayObserverFromIndex(state.head(), t1);
            state.addState(t1, l);
        }
        
    }
    /**
     * Action of replaying non-stale entries of the buffer on subscribe
     * @param <T> the input and output value
     */
    static final class TimedOnAdd<T> implements Action1<SubjectObserver<T>> {
        final BoundedState<T> state;
        final Scheduler scheduler;

        public TimedOnAdd(BoundedState<T> state, Scheduler scheduler) {
            this.state = state;
            this.scheduler = scheduler;
        }
        
        @Override
        public void call(SubjectObserver<T> t1) {
            NodeList.Node<Object> l;
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
     * A singly-linked list with volatile next node pointer.
     * @param <T> the value type
     */
    static final class NodeList<T> {
        /**
         * The node containing the value and references to neighbours.
         * @param <T> the value type
         */
        static final class Node<T> {
            /** The managed value. */
            final T value;
            /** The hard reference to the next node. */
            volatile Node<T> next;
            Node(T value) {
                this.value = value;
            }
        }
        /** The head of the list. */
        final Node<T> head = new Node<T>(null);
        /** The tail of the list. */
        Node<T> tail = head;
        /** The number of elements in the list. */
        int size;
        
        public void addLast(T value) {
            Node<T> t = tail;
            Node<T> t2 = new Node<T>(value);
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
            return t.value;
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
        public void evict(NodeList<Object> list) {
        }
        
    }    
}