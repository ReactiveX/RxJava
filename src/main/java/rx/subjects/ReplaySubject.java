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

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observer;
import rx.annotations.Beta;
import rx.exceptions.Exceptions;
import rx.internal.operators.BackpressureUtils;
import rx.internal.util.RxJavaPluginUtils;

/**
 * Subject that buffers all items it observes and replays them to any {@link Observer} that subscribes.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.ReplaySubject.png" alt="">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  ReplaySubject<Object> subject = ReplaySubject.create();
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
 *          the type of items observed and emitted by the Subject
 */
public final class ReplaySubject<T> extends Subject<T, T> {
    /**
     * Creates an unbounded replay subject.
     * <p>
     * The internal buffer is backed by an {@link ArrayList} and starts with an initial capacity of 16. Once the
     * number of items reaches this capacity, it will grow as necessary (usually by 50%). However, as the
     * number of items grows, this causes frequent array reallocation and copying, and may hurt performance
     * and latency. This can be avoided with the {@link #create(int)} overload which takes an initial capacity
     * parameter and can be tuned to reduce the array reallocation frequency as needed.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @return the created subject
     */
    public static <T> ReplaySubject<T> create() {
        return create(16);
    }

    /**
     * Creates an unbounded replay subject with the specified initial buffer capacity.
     * <p>
     * Use this method to avoid excessive array reallocation while the internal buffer grows to accommodate new
     * items. For example, if you know that the buffer will hold 32k items, you can ask the
     * {@code ReplaySubject} to preallocate its internal array with a capacity to hold that many items. Once
     * the items start to arrive, the internal array won't need to grow, creating less garbage and no overhead
     * due to frequent array-copying.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param capacity
     *          the initial buffer capacity
     * @return the created subject
     */
    public static <T> ReplaySubject<T> create(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity > 0 required but it was " + capacity);
        }
        ReplayBuffer<T> buffer = new ReplayUnboundedBuffer<T>(capacity);
        ReplayState<T> state = new ReplayState<T>(buffer);
        return new ReplaySubject<T>(state);
    }
    /**
     * Creates an unbounded replay subject with the bounded-implementation for testing purposes.
     * <p>
     * This variant behaves like the regular unbounded {@code ReplaySubject} created via {@link #create()} but
     * uses the structures of the bounded-implementation. This is by no means intended for the replacement of
     * the original, array-backed and unbounded {@code ReplaySubject} due to the additional overhead of the
     * linked-list based internal buffer. The sole purpose is to allow testing and reasoning about the behavior
     * of the bounded implementations without the interference of the eviction policies.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @return the created subject
     */
    /* public */ static <T> ReplaySubject<T> createUnbounded() {
        ReplayBuffer<T> buffer = new ReplaySizeBoundBuffer<T>(Integer.MAX_VALUE);
        ReplayState<T> state = new ReplayState<T>(buffer);
        return new ReplaySubject<T>(state);
    }
    /**
     * Creates a size-bounded replay subject.
     * <p>
     * In this setting, the {@code ReplaySubject} holds at most {@code size} items in its internal buffer and
     * discards the oldest item.
     * <p>
     * When observers subscribe to a terminated {@code ReplaySubject}, they are guaranteed to see at most
     * {@code size} {@code onNext} events followed by a termination event. 
     * <p>
     * If an observer subscribes while the {@code ReplaySubject} is active, it will observe all items in the
     * buffer at that point in time and each item observed afterwards, even if the buffer evicts items due to
     * the size constraint in the mean time. In other words, once an Observer subscribes, it will receive items
     * without gaps in the sequence.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param size
     *          the maximum number of buffered items
     * @return the created subject
     */
    public static <T> ReplaySubject<T> createWithSize(int size) {
        ReplayBuffer<T> buffer = new ReplaySizeBoundBuffer<T>(size);
        ReplayState<T> state = new ReplayState<T>(buffer);
        return new ReplaySubject<T>(state);
    }
    /**
     * Creates a time-bounded replay subject.
     * <p>
     * In this setting, the {@code ReplaySubject} internally tags each observed item with a timestamp value
     * supplied by the {@link Scheduler} and keeps only those whose age is less than the supplied time value
     * converted to milliseconds. For example, an item arrives at T=0 and the max age is set to 5; at T&gt;=5
     * this first item is then evicted by any subsequent item or termination event, leaving the buffer empty. 
     * <p>
     * Once the subject is terminated, observers subscribing to it will receive items that remained in the
     * buffer after the terminal event, regardless of their age. 
     * <p>
     * If an observer subscribes while the {@code ReplaySubject} is active, it will observe only those items
     * from within the buffer that have an age less than the specified time, and each item observed thereafter,
     * even if the buffer evicts items due to the time constraint in the mean time. In other words, once an
     * observer subscribes, it observes items without gaps in the sequence except for any outdated items at the
     * beginning of the sequence.
     * <p>
     * Note that terminal notifications ({@code onError} and {@code onCompleted}) trigger eviction as well. For
     * example, with a max age of 5, the first item is observed at T=0, then an {@code onCompleted} notification
     * arrives at T=10. If an observer subscribes at T=11, it will find an empty {@code ReplaySubject} with just
     * an {@code onCompleted} notification.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param time
     *          the maximum age of the contained items
     * @param unit
     *          the time unit of {@code time}
     * @param scheduler
     *          the {@link Scheduler} that provides the current time
     * @return the created subject
     */
    public static <T> ReplaySubject<T> createWithTime(long time, TimeUnit unit, final Scheduler scheduler) {
        return createWithTimeAndSize(time, unit, Integer.MAX_VALUE, scheduler);
    }
    /**
     * Creates a time- and size-bounded replay subject.
     * <p>
     * In this setting, the {@code ReplaySubject} internally tags each received item with a timestamp value
     * supplied by the {@link Scheduler} and holds at most {@code size} items in its internal buffer. It evicts
     * items from the start of the buffer if their age becomes less-than or equal to the supplied age in
     * milliseconds or the buffer reaches its {@code size} limit.
     * <p>
     * When observers subscribe to a terminated {@code ReplaySubject}, they observe the items that remained in
     * the buffer after the terminal notification, regardless of their age, but at most {@code size} items.
     * <p>
     * If an observer subscribes while the {@code ReplaySubject} is active, it will observe only those items
     * from within the buffer that have age less than the specified time and each subsequent item, even if the
     * buffer evicts items due to the time constraint in the mean time. In other words, once an observer
     * subscribes, it observes items without gaps in the sequence except for the outdated items at the beginning
     * of the sequence.
     * <p>
     * Note that terminal notifications ({@code onError} and {@code onCompleted}) trigger eviction as well. For
     * example, with a max age of 5, the first item is observed at T=0, then an {@code onCompleted} notification
     * arrives at T=10. If an observer subscribes at T=11, it will find an empty {@code ReplaySubject} with just
     * an {@code onCompleted} notification.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param time
     *          the maximum age of the contained items
     * @param unit
     *          the time unit of {@code time}
     * @param size
     *          the maximum number of buffered items
     * @param scheduler
     *          the {@link Scheduler} that provides the current time
     * @return the created subject
     */
    public static <T> ReplaySubject<T> createWithTimeAndSize(long time, TimeUnit unit, int size, final Scheduler scheduler) {
        ReplayBuffer<T> buffer = new ReplaySizeAndTimeBoundBuffer<T>(size, unit.toMillis(time), scheduler);
        ReplayState<T> state = new ReplayState<T>(buffer);
        return new ReplaySubject<T>(state);
    }

    /** The state storing the history and the references. */
    final ReplayState<T> state;

    ReplaySubject(ReplayState<T> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public void onNext(T t) {
        state.onNext(t);
    }
    
    @Override
    public void onError(final Throwable e) {
        state.onError(e);
    }
    
    @Override
    public void onCompleted() {
        state.onCompleted();
    }
    /**
     * @return Returns the number of subscribers.
     */
    /* Support test. */int subscriberCount() {
        return state.get().length;
    }

    @Override
    public boolean hasObservers() {
        return state.get().length != 0;
    }

    /**
     * Check if the Subject has terminated with an exception.
     * @return true if the subject has received a throwable through {@code onError}.
     */
    @Beta
    public boolean hasThrowable() {
        return state.isTerminated() && state.buffer.error() != null;
    }
    /**
     * Check if the Subject has terminated normally.
     * @return true if the subject completed normally via {@code onCompleted}
     */
    @Beta
    public boolean hasCompleted() {
        return state.isTerminated() && state.buffer.error() == null;
    }
    /**
     * Returns the Throwable that terminated the Subject.
     * @return the Throwable that terminated the Subject or {@code null} if the
     * subject hasn't terminated yet or it terminated normally.
     */
    @Beta
    public Throwable getThrowable() {
        if (state.isTerminated()) {
            return state.buffer.error();
        }
        return null;
    }
    /**
     * Returns the current number of items (non-terminal events) available for replay.
     * @return the number of items available
     */
    @Beta
    public int size() {
        return state.buffer.size();
    }
    /**
     * @return true if the Subject holds at least one non-terminal event available for replay
     */
    @Beta
    public boolean hasAnyValue() {
        return !state.buffer.isEmpty();
    }
    @Beta
    public boolean hasValue() {
        return hasAnyValue();
    }
    /**
     * Returns a snapshot of the currently buffered non-terminal events into 
     * the provided {@code a} array or creates a new array if it has not enough capacity.
     * @param a the array to fill in
     * @return the array {@code a} if it had enough capacity or a new array containing the available values 
     */
    @Beta
    public T[] getValues(T[] a) {
        return state.buffer.toArray(a);
    }
    
    /** An empty array to trigger getValues() to return a new array. */
    private static final Object[] EMPTY_ARRAY = new Object[0];
    
    /**
     * Returns a snapshot of the currently buffered non-terminal events.
     * <p>The operation is threadsafe.
     *
     * @return a snapshot of the currently buffered non-terminal events.
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @SuppressWarnings("unchecked")
    @Beta
    public Object[] getValues() {
        T[] r = getValues((T[])EMPTY_ARRAY);
        if (r == EMPTY_ARRAY) {
            return new Object[0]; // don't leak the default empty array.
        }
        return r;
    }
    
    @Beta
    public T getValue() {
        return state.buffer.last();
    }
    
    /**
     * Holds onto the array of Subscriber-wrapping ReplayProducers and
     * the buffer that holds values to be replayed; it manages
     * subscription and signal dispatching.
     *
     * @param <T> the value type
     */
    static final class ReplayState<T> 
    extends AtomicReference<ReplayProducer<T>[]>
    implements OnSubscribe<T>, Observer<T> {

        /** */
        private static final long serialVersionUID = 5952362471246910544L;
        
        final ReplayBuffer<T> buffer;
        
        @SuppressWarnings("rawtypes")
        static final ReplayProducer[] EMPTY = new ReplayProducer[0];
        @SuppressWarnings("rawtypes")
        static final ReplayProducer[] TERMINATED = new ReplayProducer[0];
        
        @SuppressWarnings("unchecked")
        public ReplayState(ReplayBuffer<T> buffer) {
            this.buffer = buffer;
            lazySet(EMPTY);
        }
        
        @Override
        public void call(Subscriber<? super T> t) {
            ReplayProducer<T> rp = new ReplayProducer<T>(t, this);
            t.add(rp);
            t.setProducer(rp);
            
            if (add(rp)) {
                if (rp.isUnsubscribed()) {
                    remove(rp);
                    return;
                }
            }
            buffer.drain(rp);
        }
        
        boolean add(ReplayProducer<T> rp) {
            for (;;) {
                ReplayProducer<T>[] a = get();
                if (a == TERMINATED) {
                    return false;
                }
                
                int n = a.length;
                
                @SuppressWarnings("unchecked")
                ReplayProducer<T>[] b = new ReplayProducer[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = rp;
                
                if (compareAndSet(a, b)) {
                    return true;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        void remove(ReplayProducer<T> rp) {
            for (;;) {
                ReplayProducer<T>[] a = get();
                if (a == TERMINATED || a == EMPTY) {
                    return;
                }
                
                int n = a.length;
                
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i] == rp) {
                        j = i;
                        break;
                    }
                }
                
                if (j < 0) {
                    return;
                }
                
                ReplayProducer<T>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new ReplayProducer[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (compareAndSet(a, b)) {
                    return;
                }
            }
        }

        @Override
        public void onNext(T t) {
            ReplayBuffer<T> b = buffer;
            
            b.next(t);
            for (ReplayProducer<T> rp : get()) {
                if (rp.caughtUp) {
                    rp.actual.onNext(t);
                } else {
                    if (b.drain(rp)) {
                        rp.caughtUp = true;
                        rp.node = null;
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onError(Throwable e) {
            ReplayBuffer<T> b = buffer;
            
            b.error(e);
            List<Throwable> errors = null;
            for (ReplayProducer<T> rp : getAndSet(TERMINATED)) {
                try {
                    if (rp.caughtUp) {
                        rp.actual.onError(e);
                    } else {
                        if (b.drain(rp)) {
                            rp.caughtUp = true;
                            rp.node = null;
                        }
                    }
                } catch (Throwable ex) {
                    if (errors == null) {
                        errors = new ArrayList<Throwable>();
                    }
                    errors.add(ex);
                }
            }
            
            Exceptions.throwIfAny(errors);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCompleted() {
            ReplayBuffer<T> b = buffer;
            
            b.complete();
            for (ReplayProducer<T> rp : getAndSet(TERMINATED)) {
                if (rp.caughtUp) {
                    rp.actual.onCompleted();
                } else {
                    if (b.drain(rp)) {
                        rp.caughtUp = true;
                        rp.node = null;
                    }
                }
            }
        }
        
        
        boolean isTerminated() {
            return get() == TERMINATED;
        }
    }
    
    /**
     * The base interface for buffering signals to be replayed to individual
     * Subscribers.
     *
     * @param <T> the value type
     */
    interface ReplayBuffer<T> {
        
        void next(T t);
        
        void error(Throwable e);
        
        void complete();
        
        boolean drain(ReplayProducer<T> rp);
        
        boolean isComplete();
        
        Throwable error();
        
        T last();
        
        int size();
        
        boolean isEmpty();
        
        T[] toArray(T[] a);
    }
    
    /**
     * An unbounded ReplayBuffer implementation that uses linked-arrays
     * to avoid copy-on-grow situation with ArrayList.
     *
     * @param <T> the value type
     */
    static final class ReplayUnboundedBuffer<T> implements ReplayBuffer<T> {
        final int capacity;
        
        volatile int size;
        
        final Object[] head;
        
        Object[] tail;
        
        int tailIndex;
        
        volatile boolean done;
        Throwable error;
        
        public ReplayUnboundedBuffer(int capacity) {
            this.capacity = capacity;
            this.tail = this.head = new Object[capacity + 1];
        }

        @Override
        public void next(T t) {
            if (done) {
                return;
            }
            int i = tailIndex;
            Object[] a = tail;
            if (i == a.length - 1) {
                Object[] b = new Object[a.length];
                b[0] = t;
                tailIndex = 1;
                a[i] = b;
                tail = b;
            } else {
                a[i] = t;
                tailIndex = i + 1;
            }
            size++;
            
        }

        @Override
        public void error(Throwable e) {
            if (done) {
                RxJavaPluginUtils.handleException(e);
                return;
            }
            error = e;
            done = true;
        }

        @Override
        public void complete() {
            done = true;
        }

        @Override
        public boolean drain(ReplayProducer<T> rp) {
            if (rp.getAndIncrement() != 0) {
                return false;
            }
            
            int missed = 1;
            
            final Subscriber<? super T> a = rp.actual;
            final int n = capacity;
            
            for (;;) {
                
                long r = rp.requested.get();
                long e = 0L;
                
                Object[] node = (Object[])rp.node;
                if (node == null) {
                    node = head;
                }
                int tailIndex = rp.tailIndex;
                int index = rp.index;
                
                while (e != r) {
                    if (a.isUnsubscribed()) {
                        rp.node = null;
                        return false;
                    }
                    
                    boolean d = done;
                    boolean empty = index == size;

                    if (d && empty) {
                        rp.node = null;
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onCompleted();
                        }
                        return false;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    if (tailIndex == n) {
                        node = (Object[])node[tailIndex];
                        tailIndex = 0;
                    }
                    
                    @SuppressWarnings("unchecked")
                    T v = (T)node[tailIndex];
                    
                    a.onNext(v);
                    
                    e++;
                    tailIndex++;
                    index++;
                }
                
                if (e == r) {
                    if (a.isUnsubscribed()) {
                        rp.node = null;
                        return false;
                    }
                    
                    boolean d = done;
                    boolean empty = index == size;

                    if (d && empty) {
                        rp.node = null;
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onCompleted();
                        }
                        return false;
                    }
                }
                
                if (e != 0L) {
                    if (r != Long.MAX_VALUE) {
                        BackpressureUtils.produced(rp.requested, e);
                    }
                }
                
                rp.index = index;
                rp.tailIndex = tailIndex;
                rp.node = node;
                
                missed = rp.addAndGet(-missed);
                if (missed == 0) {
                    return r == Long.MAX_VALUE;
                }
            }
        }

        @Override
        public boolean isComplete() {
            return done;
        }

        @Override
        public Throwable error() {
            return error;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T last() {
            // we don't have a volatile read on tail and tailIndex
            // so we have to traverse the linked structure up until
            // we read size / capacity nodes and index into the array
            // via size % capacity
            int s = size;
            if (s == 0) {
                return null;
            }
            Object[] h = head;
            int n = capacity;
            
            while (s >= n) {
                h = (Object[])h[n];
                s -= n;
            }
            
            return (T)h[s - 1];
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T[] toArray(T[] a) {
            int s = size;
            if (a.length < s) {
                a = (T[])Array.newInstance(a.getClass().getComponentType(), s);
            }

            Object[] h = head;
            int n = capacity;

            int j = 0;

            while (j + n < s) {
                System.arraycopy(h, 0, a, j, n);
                j += n;
                h = (Object[])h[n];
            }
            
            System.arraycopy(h, 0, a, j, s - j);
            
            if (a.length > s) {
                a[s] = null;
            }
            
            return a;
        }
    }
    
    static final class ReplaySizeBoundBuffer<T> implements ReplayBuffer<T> {
        final int limit;
        
        volatile Node<T> head;
        
        Node<T> tail;

        int size;

        volatile boolean done;
        Throwable error;
        
        public ReplaySizeBoundBuffer(int limit) {
            this.limit = limit;
            Node<T> n = new Node<T>(null);
            this.tail = n;
            this.head = n;
        }

        @Override
        public void next(T value) {
            Node<T> n = new Node<T>(value);
            tail.set(n);
            tail = n;
            int s = size;
            if (s == limit) {
                head = head.get();
            } else {
                size = s + 1;
            }
        }

        @Override
        public void error(Throwable ex) {
            error = ex;
            done = true;
        }

        @Override
        public void complete() {
            done = true;
        }

        @Override
        public boolean drain(ReplayProducer<T> rp) {
            if (rp.getAndIncrement() != 0) {
                return false;
            }
            
            final Subscriber<? super T> a = rp.actual;
            
            int missed = 1;
            
            for (;;) {
                
                long r = rp.requested.get();
                long e = 0L;
                
                @SuppressWarnings("unchecked")
                Node<T> node = (Node<T>)rp.node;
                if (node == null) {
                    node = head;
                }
                
                while (e != r) {
                    if (a.isUnsubscribed()) {
                        rp.node = null;
                        return false;
                    }
                    
                    boolean d = done;
                    Node<T> next = node.get();
                    boolean empty = next == null;
                    
                    if (d && empty) {
                        rp.node = null;
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onCompleted();
                        }
                        return false;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    a.onNext(next.value);
                    
                    e++;
                    node = next;
                }
                
                if (e == r) {
                    if (a.isUnsubscribed()) {
                        rp.node = null;
                        return false;
                    }
                    
                    boolean d = done;
                    boolean empty = node.get() == null;
                    
                    if (d && empty) {
                        rp.node = null;
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onCompleted();
                        }
                        return false;
                    }
                }
                
                if (e != 0L) {
                    if (r != Long.MAX_VALUE) {
                        BackpressureUtils.produced(rp.requested, e);
                    }
                }
                
                rp.node = node;
                
                missed = rp.addAndGet(-missed);
                if (missed == 0) {
                    return r == Long.MAX_VALUE;
                }
            }
        }

        static final class Node<T> extends AtomicReference<Node<T>> {
            /** */
            private static final long serialVersionUID = 3713592843205853725L;
            
            final T value;
            
            public Node(T value) {
                this.value = value;
            }
        }

        @Override
        public boolean isComplete() {
            return done;
        }

        @Override
        public Throwable error() {
            return error;
        }

        @Override
        public T last() {
            Node<T> h = head;
            Node<T> n;
            while ((n = h.get()) != null) {
                h = n;
            }
            return h.value;
        }

        @Override
        public int size() {
            int s = 0;
            Node<T> n = head.get();
            while (n != null && s != Integer.MAX_VALUE) {
                n = n.get();
                s++;
            }
            return s;
        }

        @Override
        public boolean isEmpty() {
            return head.get() == null;
        }

        @Override
        public T[] toArray(T[] a) {
            List<T> list = new ArrayList<T>();
            
            Node<T> n = head.get();
            while (n != null) {
                list.add(n.value);
                n = n.get();
            }
            return list.toArray(a);
        }

    }

    static final class ReplaySizeAndTimeBoundBuffer<T> implements ReplayBuffer<T> {
        final int limit;
        
        final long maxAgeMillis;
        
        final Scheduler scheduler;
        
        volatile TimedNode<T> head;
        
        TimedNode<T> tail;

        int size;

        volatile boolean done;
        Throwable error;
        
        public ReplaySizeAndTimeBoundBuffer(int limit, long maxAgeMillis, Scheduler scheduler) {
            this.limit = limit;
            TimedNode<T> n = new TimedNode<T>(null, 0L);
            this.tail = n;
            this.head = n;
            this.maxAgeMillis = maxAgeMillis;
            this.scheduler = scheduler;
        }

        @Override
        public void next(T value) {
            long now = scheduler.now();
            
            TimedNode<T> n = new TimedNode<T>(value, now);
            tail.set(n);
            tail = n;
            
            now -= maxAgeMillis;

            int s = size;
            TimedNode<T> h0 = head;
            TimedNode<T> h = h0;
            
            if (s == limit) {
                h = h.get();
            } else {
                s++;
            }
            
            while ((n = h.get()) != null) {
                if (n.timestamp > now) {
                    break;
                }
                h = n;
                s--;
            }
            
            size = s;
            if (h != h0) {
                head = h;
            }
        }
        @Override
        public void error(Throwable ex) {
            evictFinal();
            error = ex;
            done = true;
        }

        @Override
        public void complete() {
            evictFinal();
            done = true;
        }
        
        void evictFinal() {
            long now = scheduler.now() - maxAgeMillis;
            
            TimedNode<T> h0 = head;
            TimedNode<T> h = h0;
            TimedNode<T> n;
            
            while ((n = h.get()) != null) {
                if (n.timestamp > now) {
                    break;
                }
                h = n;
            }
            
            if (h0 != h) {
                head = h;
            }
        }

        TimedNode<T> latestHead() {
            long now = scheduler.now() - maxAgeMillis;
            TimedNode<T> h = head;
            TimedNode<T> n;
            while ((n = h.get()) != null) {
                if (n.timestamp > now) {
                    break;
                }
                h = n;
            }
            return h;
        }
        
        @Override
        public boolean drain(ReplayProducer<T> rp) {
            if (rp.getAndIncrement() != 0) {
                return false;
            }
            
            final Subscriber<? super T> a = rp.actual;
            
            int missed = 1;
            
            for (;;) {
                
                long r = rp.requested.get();
                long e = 0L;
                
                @SuppressWarnings("unchecked")
                TimedNode<T> node = (TimedNode<T>)rp.node;
                if (node == null) {
                    node = latestHead();
                }
                
                while (e != r) {
                    if (a.isUnsubscribed()) {
                        rp.node = null;
                        return false;
                    }
                    
                    boolean d = done;
                    TimedNode<T> next = node.get();
                    boolean empty = next == null;
                    
                    if (d && empty) {
                        rp.node = null;
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onCompleted();
                        }
                        return false;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    a.onNext(next.value);
                    
                    e++;
                    node = next;
                }
                
                if (e == r) {
                    if (a.isUnsubscribed()) {
                        rp.node = null;
                        return false;
                    }
                    
                    boolean d = done;
                    boolean empty = node.get() == null;
                    
                    if (d && empty) {
                        rp.node = null;
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onCompleted();
                        }
                        return false;
                    }
                }
                
                if (e != 0L) {
                    if (r != Long.MAX_VALUE) {
                        BackpressureUtils.produced(rp.requested, e);
                    }
                }
                
                rp.node = node;
                
                missed = rp.addAndGet(-missed);
                if (missed == 0) {
                    return r == Long.MAX_VALUE;
                }
            }
        }

        static final class TimedNode<T> extends AtomicReference<TimedNode<T>> {
            /** */
            private static final long serialVersionUID = 3713592843205853725L;
            
            final T value;
            
            final long timestamp;
            
            public TimedNode(T value, long timestamp) {
                this.value = value;
                this.timestamp = timestamp;
            }
        }

        @Override
        public boolean isComplete() {
            return done;
        }

        @Override
        public Throwable error() {
            return error;
        }

        @Override
        public T last() {
            TimedNode<T> h = latestHead();
            TimedNode<T> n;
            while ((n = h.get()) != null) {
                h = n;
            }
            return h.value;
        }

        @Override
        public int size() {
            int s = 0;
            TimedNode<T> n = latestHead().get();
            while (n != null && s != Integer.MAX_VALUE) {
                n = n.get();
                s++;
            }
            return s;
        }

        @Override
        public boolean isEmpty() {
            return latestHead().get() == null;
        }

        @Override
        public T[] toArray(T[] a) {
            List<T> list = new ArrayList<T>();
            
            TimedNode<T> n = latestHead().get();
            while (n != null) {
                list.add(n.value);
                n = n.get();
            }
            return list.toArray(a);
        }

    }

    /**
     * A producer and subscription implementation that tracks the current
     * replay position of a particular subscriber.
     * <p>
     * The this holds the current work-in-progress indicator used by serializing
     * replays.
     * 
     * @param <T> the value type
     */
    static final class ReplayProducer<T> 
    extends AtomicInteger
    implements Producer, Subscription {
        /** */
        private static final long serialVersionUID = -5006209596735204567L;

        /** The wrapped Subscriber instance. */
        final Subscriber<? super T> actual;
        
        /** Holds the current requested amount. */
        final AtomicLong requested;

        /** Holds the back-reference to the replay state object. */
        final ReplayState<T> state;

        /** 
         * Indicates this Subscriber runs unbounded and the <b>source</b>-triggered
         * buffer.drain() has emitted all available values.
         * <p>
         * This field has to be read and written from the source emitter's thread only.
         */
        boolean caughtUp;
        
        /** 
         * Unbounded buffer.drain() uses this field to remember the absolute index of
         * values replayed to this Subscriber.
         */
        int index;
        
        /** 
         * Unbounded buffer.drain() uses this index within its current node to indicate
         * how many items were replayed from that particular node so far.
         */
        int tailIndex;
        
        /** 
         * Stores the current replay node of the buffer to be used by buffer.drain().
         */
        Object node;
        
        public ReplayProducer(Subscriber<? super T> actual, ReplayState<T> state) {
            this.actual = actual;
            this.requested = new AtomicLong();
            this.state = state;
        }
        
        @Override
        public void unsubscribe() {
            state.remove(this);
        }

        @Override
        public boolean isUnsubscribed() {
            return actual.isUnsubscribed();
        }

        @Override
        public void request(long n) {
            if (n > 0L) {
                BackpressureUtils.getAndAddRequest(requested, n);
                state.buffer.drain(this);
            } else if (n < 0L) {
                throw new IllegalArgumentException("n >= required but it was " + n);
            }
        }
        
    }
}
