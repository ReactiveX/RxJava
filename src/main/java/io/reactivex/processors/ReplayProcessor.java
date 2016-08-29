/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.processors;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Scheduler;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Replays events to Subscribers.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.ReplaySubject.png" alt="">
 * 
 * <p>
 * The ReplayProcessor can be created in bounded and unbounded mode. It can be bounded by
 * size (maximum number of elements retained at most) and/or time (maximum age of elements replayed).
 * 
 * <p>This Processor respects the backpressure behavior of its Subscribers (individually) but
 * does not coordinate their request amounts towards the upstream (because there might not be any).
 * 
 * <p>Note that Subscribers receive a continuous sequence of values after they subscribed even 
 * if an individual item gets delayed due to backpressure.
 * 
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  ReplayProcessor<Object> processor = new ReplayProcessor<T>();
  processor.onNext("one");
  processor.onNext("two");
  processor.onNext("three");
  processor.onCompleted();

  // both of the following will get the onNext/onComplete calls from above
  processor.subscribe(subscriber1);
  processor.subscribe(subscriber2);

  } </pre>
 * 
 * @param <T> the value type
 */
public final class ReplayProcessor<T> extends FlowableProcessor<T> {
    /** An empty array to avoid allocation in getValues(). */
    private static final Object[] EMPTY_ARRAY = new Object[0];

    final ReplayBuffer<T> buffer;
    
    boolean done;
    
    final AtomicReference<ReplaySubscription<T>[]> subscribers;
    
    @SuppressWarnings("rawtypes")
    static final ReplaySubscription[] EMPTY = new ReplaySubscription[0];

    @SuppressWarnings("rawtypes")
    static final ReplaySubscription[] TERMINATED = new ReplaySubscription[0];
    
    /**
     * Creates an unbounded ReplayProcessor.
     * <p>
     * The internal buffer is backed by an {@link ArrayList} and starts with an initial capacity of 16. Once the
     * number of items reaches this capacity, it will grow as necessary (usually by 50%). However, as the
     * number of items grows, this causes frequent array reallocation and copying, and may hurt performance
     * and latency. This can be avoided with the {@link #create(int)} overload which takes an initial capacity
     * parameter and can be tuned to reduce the array reallocation frequency as needed.
     *
     * @param <T>
     *          the type of items observed and emitted by the ReplayProcessor
     * @return the created ReplayProcessor
     */
    public static <T> ReplayProcessor<T> create() {
        return new ReplayProcessor<T>(new UnboundedReplayBuffer<T>(16));
    }

    /**
     * Creates an unbounded ReplayProcessor with the specified initial buffer capacity.
     * <p>
     * Use this method to avoid excessive array reallocation while the internal buffer grows to accommodate new
     * items. For example, if you know that the buffer will hold 32k items, you can ask the
     * {@code ReplayProcessor} to preallocate its internal array with a capacity to hold that many items. Once
     * the items start to arrive, the internal array won't need to grow, creating less garbage and no overhead
     * due to frequent array-copying.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param capacityHint
     *          the initial buffer capacity
     * @return the created subject
     */
    public static <T> ReplayProcessor<T> create(int capacityHint) {
        return new ReplayProcessor<T>(new UnboundedReplayBuffer<T>(capacityHint));
    }

    /**
     * Creates a size-bounded ReplayProcessor.
     * <p>
     * In this setting, the {@code ReplayProcessor} holds at most {@code size} items in its internal buffer and
     * discards the oldest item.
     * <p>
     * When observers subscribe to a terminated {@code ReplayProcessor}, they are guaranteed to see at most
     * {@code size} {@code onNext} events followed by a termination event. 
     * <p>
     * If an observer subscribes while the {@code ReplayProcessor} is active, it will observe all items in the
     * buffer at that point in time and each item observed afterwards, even if the buffer evicts items due to
     * the size constraint in the mean time. In other words, once an Observer subscribes, it will receive items
     * without gaps in the sequence.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param maxSize
     *          the maximum number of buffered items
     * @return the created subject
     */
    public static <T> ReplayProcessor<T> createWithSize(int maxSize) {
        return new ReplayProcessor<T>(new SizeBoundReplayBuffer<T>(maxSize));
    }

    /**
     * Creates an unbounded ReplayProcessor with the bounded-implementation for testing purposes.
     * <p>
     * This variant behaves like the regular unbounded {@code ReplayProcessor} created via {@link #create()} but
     * uses the structures of the bounded-implementation. This is by no means intended for the replacement of
     * the original, array-backed and unbounded {@code ReplayProcessor} due to the additional overhead of the
     * linked-list based internal buffer. The sole purpose is to allow testing and reasoning about the behavior
     * of the bounded implementations without the interference of the eviction policies.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @return the created subject
     */
    /* test */ static <T> ReplayProcessor<T> createUnbounded() {
        return new ReplayProcessor<T>(new SizeBoundReplayBuffer<T>(Integer.MAX_VALUE));
    }

    /**
     * Creates a time-bounded ReplayProcessor.
     * <p>
     * In this setting, the {@code ReplayProcessor} internally tags each observed item with a timestamp value
     * supplied by the {@link Scheduler} and keeps only those whose age is less than the supplied time value
     * converted to milliseconds. For example, an item arrives at T=0 and the max age is set to 5; at T&gt;=5
     * this first item is then evicted by any subsequent item or termination event, leaving the buffer empty. 
     * <p>
     * Once the subject is terminated, observers subscribing to it will receive items that remained in the
     * buffer after the terminal event, regardless of their age. 
     * <p>
     * If an observer subscribes while the {@code ReplayProcessor} is active, it will observe only those items
     * from within the buffer that have an age less than the specified time, and each item observed thereafter,
     * even if the buffer evicts items due to the time constraint in the mean time. In other words, once an
     * observer subscribes, it observes items without gaps in the sequence except for any outdated items at the
     * beginning of the sequence.
     * <p>
     * Note that terminal notifications ({@code onError} and {@code onCompleted}) trigger eviction as well. For
     * example, with a max age of 5, the first item is observed at T=0, then an {@code onCompleted} notification
     * arrives at T=10. If an observer subscribes at T=11, it will find an empty {@code ReplayProcessor} with just
     * an {@code onCompleted} notification.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param maxAge
     *          the maximum age of the contained items
     * @param unit
     *          the time unit of {@code time}
     * @param scheduler
     *          the {@link Scheduler} that provides the current time
     * @return the created subject
     */
    public static <T> ReplayProcessor<T> createWithTime(long maxAge, TimeUnit unit, Scheduler scheduler) {
        return new ReplayProcessor<T>(new SizeAndTimeBoundReplayBuffer<T>(Integer.MAX_VALUE, maxAge, unit, scheduler));
    }

    /**
     * Creates a time- and size-bounded ReplayProcessor.
     * <p>
     * In this setting, the {@code ReplayProcessor} internally tags each received item with a timestamp value
     * supplied by the {@link Scheduler} and holds at most {@code size} items in its internal buffer. It evicts
     * items from the start of the buffer if their age becomes less-than or equal to the supplied age in
     * milliseconds or the buffer reaches its {@code size} limit.
     * <p>
     * When observers subscribe to a terminated {@code ReplayProcessor}, they observe the items that remained in
     * the buffer after the terminal notification, regardless of their age, but at most {@code size} items.
     * <p>
     * If an observer subscribes while the {@code ReplayProcessor} is active, it will observe only those items
     * from within the buffer that have age less than the specified time and each subsequent item, even if the
     * buffer evicts items due to the time constraint in the mean time. In other words, once an observer
     * subscribes, it observes items without gaps in the sequence except for the outdated items at the beginning
     * of the sequence.
     * <p>
     * Note that terminal notifications ({@code onError} and {@code onCompleted}) trigger eviction as well. For
     * example, with a max age of 5, the first item is observed at T=0, then an {@code onCompleted} notification
     * arrives at T=10. If an observer subscribes at T=11, it will find an empty {@code ReplayProcessor} with just
     * an {@code onCompleted} notification.
     *
     * @param <T>
     *          the type of items observed and emitted by the Subject
     * @param maxAge
     *          the maximum age of the contained items
     * @param unit
     *          the time unit of {@code time}
     * @param maxSize
     *          the maximum number of buffered items
     * @param scheduler
     *          the {@link Scheduler} that provides the current time
     * @return the created subject
     */
    public static <T> ReplayProcessor<T> createWithTimeAndSize(long maxAge, TimeUnit unit, Scheduler scheduler, int maxSize) {
        return new ReplayProcessor<T>(new SizeAndTimeBoundReplayBuffer<T>(maxSize, maxAge, unit, scheduler));
    }
    
    /**
     * Constructs a ReplayProcessor with the given custom ReplayBuffer instance.
     * @param buffer the ReplayBuffer instance, not null (not verified)
     */
    @SuppressWarnings("unchecked")
    ReplayProcessor(ReplayBuffer<T> buffer) {
        this.buffer = buffer;
        this.subscribers = new AtomicReference<ReplaySubscription<T>[]>(EMPTY);
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        ReplaySubscription<T> rs = new ReplaySubscription<T>(s, this);
        s.onSubscribe(rs);
        
        if (!rs.cancelled) {
            if (add(rs)) {
                if (rs.cancelled) {
                    remove(rs);
                    return;
                }
            }
            buffer.replay(rs);
        }
    }
    
    @Override
    public void onSubscribe(Subscription s) {
        if (done) {
            s.cancel();
            return;
        }
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        if (t == null) {
            onError(new NullPointerException());
            return;
        }
        if (done) {
            return;
        }

        ReplayBuffer<T> b = buffer;
        b.add(t);
        
        for (ReplaySubscription<T> rs : subscribers.get()) {
            b.replay(rs);
        }
    }

    @Override
    public void onError(Throwable t) {
        if (t == null) {
            t = new NullPointerException();
        }
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        done = true;

        Object o = NotificationLite.error(t);
        
        ReplayBuffer<T> b = buffer;
        
        b.addFinal(o);
        
        for (ReplaySubscription<T> rs : terminate(o)) {
            b.replay(rs);
        }
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;

        Object o = NotificationLite.complete();
        
        ReplayBuffer<T> b = buffer;
        
        b.addFinal(o);
        
        for (ReplaySubscription<T> rs : terminate(o)) {
            b.replay(rs);
        }
    }

    @Override
    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }

    /* test */ int subscriberCount() {
        return subscribers.get().length;
    }

    @Override
    public Throwable getThrowable() {
        Object o = buffer.get();
        if (NotificationLite.isError(o)) {
            return NotificationLite.getError(o);
        }
        return null;
    }
    
    /**
     * Returns a single value the Subject currently has or null if no such value exists.
     * <p>The method is thread-safe.
     * @return a single value the Subject currently has or null if no such value exists
     */
    public T getValue() {
        return buffer.getValue();
    }
    
    /**
     * Returns an Object array containing snapshot all values of the Subject.
     * <p>The method is thread-safe.
     * @return the array containing the snapshot of all values of the Subject
     */
    public Object[] getValues() {
        @SuppressWarnings("unchecked")
        T[] a = (T[])EMPTY_ARRAY;
        T[] b = getValues(a);
        if (b == EMPTY_ARRAY) {
            return new Object[0];
        }
        return b;
            
    }
    
    /**
     * Returns a typed array containing a snapshot of all values of the Subject.
     * <p>The method follows the conventions of Collection.toArray by setting the array element
     * after the last value to null (if the capacity permits).
     * <p>The method is thread-safe.
     * @param array the target array to copy values into if it fits
     * @return the given array if the values fit into it or a new array containing all values
     */
    public T[] getValues(T[] array) {
        return buffer.getValues(array);
    }
    
    @Override
    public boolean hasComplete() {
        Object o = buffer.get();
        return NotificationLite.isComplete(o);
    }
    
    @Override
    public boolean hasThrowable() {
        Object o = buffer.get();
        return NotificationLite.isError(o);
    }
    
    /**
     * Returns true if the subject has any value.
     * <p>The method is thread-safe.
     * @return true if the subject has any value
     */
    public boolean hasValue() {
        return buffer.size() != 0; // NOPMD
    }
    
    /* test*/ int size() {
        return buffer.size();
    }

    boolean add(ReplaySubscription<T> rs) {
        for (;;) {
            ReplaySubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int len = a.length;
            @SuppressWarnings("unchecked")
            ReplaySubscription<T>[] b = new ReplaySubscription[len + 1];
            System.arraycopy(a, 0, b, 0, len);
            b[len] = rs;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    void remove(ReplaySubscription<T> rs) {
        for (;;) {
            ReplaySubscription<T>[] a = subscribers.get();
            if (a == TERMINATED || a == EMPTY) {
                return;
            }
            int len = a.length;
            int j = -1;
            for (int i = 0; i < len; i++) {
                if (a[i] == rs) {
                    j = i;
                    break;
                }
            }
            
            if (j < 0) {
                return;
            }
            ReplaySubscription<T>[] b;
            if (len == 1) {
                b = EMPTY;
            } else {
                b = new ReplaySubscription[len - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, len - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    ReplaySubscription<T>[] terminate(Object terminalValue) {
        if (buffer.compareAndSet(null, terminalValue)) {
            return subscribers.getAndSet(TERMINATED);
        }
        return TERMINATED;
    }

    /**
     * Abstraction over a buffer that receives events and replays them to
     * individual Subscribers.
     *
     * @param <T> the value type
     */
    interface ReplayBuffer<T> {
        
        void add(T value);
        
        void addFinal(Object notificationLite);
        
        void replay(ReplaySubscription<T> rs);
        
        int size();
        
        T getValue();
        
        T[] getValues(T[] array);
        
        /**
         * Returns the terminal NotificationLite object or null if not yet terminated.
         * @return the terminal NotificationLite object or null if not yet terminated
         */
        Object get();
        
        /**
         * Atomically compares and sets the next terminal NotificationLite object if the
         * current equals to the expected NotificationLite object.
         * @param expected the expected NotificationLite object
         * @param next the next NotificationLite object
         * @return true if successful
         */
        boolean compareAndSet(Object expected, Object next);
    }
    
    static final class ReplaySubscription<T> extends AtomicInteger implements Subscription {
        /** */
        private static final long serialVersionUID = 466549804534799122L;
        final Subscriber<? super T> actual;
        final ReplayProcessor<T> state;
        
        Object index;
        
        final AtomicLong requested;
        
        volatile boolean cancelled;
        
        public ReplaySubscription(Subscriber<? super T> actual, ReplayProcessor<T> state) {
            this.actual = actual;
            this.state = state;
            this.requested = new AtomicLong();
        }
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                state.buffer.replay(this);
            }
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                state.remove(this);
            }
        }
    }
    
    static final class UnboundedReplayBuffer<T> 
    extends AtomicReference<Object>
    implements ReplayBuffer<T> {
        /** */
        private static final long serialVersionUID = -4457200895834877300L;

        final List<Object> buffer;
        
        volatile boolean done;
        
        volatile int size;
        
        public UnboundedReplayBuffer(int capacityHint) {
            this.buffer = new ArrayList<Object>(verifyPositive(capacityHint, "capacityHint"));
        }
        
        @Override
        public void add(T value) {
            buffer.add(value);
            size++;
        }
        
        @Override
        public void addFinal(Object notificationLite) {
            buffer.add(notificationLite);
            size++;
            done = true;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T getValue() {
            int s = size;
            if (s != 0) {
                List<Object> b = buffer;
                Object o = b.get(s - 1);
                if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                    if (s == 1) {
                        return null;
                    }
                    return (T)b.get(s - 2);
                }
                return (T)o;
            }
            return null;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            int s = size;
            if (s == 0) {
                if (array.length != 0) {
                    array[0] = null;
                }
                return array;
            }
            List<Object> b = buffer;
            Object o = b.get(s - 1);
            
            if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                s--;
                if (s == 0) {
                    if (array.length != 0) {
                        array[0] = null;
                    }
                    return array;
                }
            }
            
            
            if (array.length < s) {
                array = (T[])Array.newInstance(array.getClass().getComponentType(), s);
            }
            for (int i = 0; i < s; i++) {
                array[i] = (T)b.get(i);
            }
            if (array.length > s) {
                array[s] = null;
            }
            
            return array;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public void replay(ReplaySubscription<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            final List<Object> b = buffer;
            final Subscriber<? super T> a = rs.actual;

            Integer indexObject = (Integer)rs.index;
            int index;
            if (indexObject != null) {
                index = indexObject;
            } else {
                index = 0;
                rs.index = 0;
            }

            for (;;) {

                if (rs.cancelled) {
                    rs.index = null;
                    return;
                }

                int s = size;
                long r = rs.requested.get();
                long e = 0L;
                
                while (s != index) {
                    
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }
                    
                    Object o = b.get(index);
                    
                    if (done) {
                        if (index + 1 == s) {
                            s = size;
                            if (index + 1 == s) {
                                if (NotificationLite.isComplete(o)) {
                                    a.onComplete();
                                } else {
                                    a.onError(NotificationLite.getError(o));
                                }
                                rs.index = null;
                                rs.cancelled = true;
                                return;
                            }
                        }
                    }
                    
                    if (r == 0) {
                        r = rs.requested.get() + e;
                        if (r == 0) {
                            break;
                        }
                    }

                    a.onNext((T)o);
                    r--;
                    e--;
                    index++;
                }
                
                if (e != 0L) {
                    if (rs.requested.get() != Long.MAX_VALUE) {
                        r = rs.requested.addAndGet(e);
                    }
                }
                if (index != size && r != 0L) {
                    continue;
                }
                
                rs.index = index;
                
                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        @Override
        public int size() {
            int s = size;
            if (s != 0) {
                Object o = buffer.get(s - 1);
                if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                    return s - 1;
                }
                return s;
            }
            return 0;
        }
    }
    
    static final class Node<T> extends AtomicReference<Node<T>> {
        /** */
        private static final long serialVersionUID = 6404226426336033100L;
        
        final T value;
        
        public Node(T value) {
            this.value = value;
        }
    }
    
    static final class TimedNode<T> extends AtomicReference<TimedNode<T>> {
        /** */
        private static final long serialVersionUID = 6404226426336033100L;
        
        final T value;
        final long time;
        
        public TimedNode(T value, long time) {
            this.value = value;
            this.time = time;
        }
    }
    
    static final class SizeBoundReplayBuffer<T> 
    extends AtomicReference<Object>
    implements ReplayBuffer<T> {
        /** */
        private static final long serialVersionUID = 3027920763113911982L;
        final int maxSize;
        int size;
        
        volatile Node<Object> head;
        
        Node<Object> tail;
        
        volatile boolean done;
        
        public SizeBoundReplayBuffer(int maxSize) {
            this.maxSize = verifyPositive(maxSize, "maxSize");
            Node<Object> h = new Node<Object>(null);
            this.tail = h;
            this.head = h;
        }

        void trim() {
            if (size > maxSize) {
                size--;
                Node<Object> h = head;
                head = h.get();
            }
        }
        
        @Override
        public void add(T value) {
            Node<Object> n = new Node<Object>(value);
            Node<Object> t = tail;

            tail = n;
            size++;
            t.set(n); // releases both the tail and size
            
            trim();
        }
        
        @Override
        public void addFinal(Object notificationLite) {
            Node<Object> n = new Node<Object>(notificationLite);
            Node<Object> t = tail;

            tail = n;
            size++;
            t.set(n); // releases both the tail and size
            
            done = true;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T getValue() {
            Node<Object> prev = null;
            Node<Object> h = head;

            for (;;) {
                Node<Object> next = h.get();
                if (next == null) {
                    break;
                }
                prev = h;
                h = next;
            }
            
            Object v = h.value;
            if (v == null) {
                return null;
            }
            if (NotificationLite.isComplete(v) || NotificationLite.isError(v)) {
                return (T)prev.value;
            }
            
            return (T)v;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            Node<Object> h = head;
            int s = size();
            
            if (s == 0) {
                if (array.length != 0) {
                    array[0] = null;
                }
            } else {
                if (array.length < s) {
                    array = (T[])Array.newInstance(array.getClass().getComponentType(), s);
                }

                int i = 0;
                while (i != s) {
                    Node<Object> next = h.get();
                    array[i] = (T)next.value;
                    i++;
                    h = next;
                }
                if (array.length > s) {
                    array[s] = null;
                }
            }
            
            return array;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public void replay(ReplaySubscription<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            final Subscriber<? super T> a = rs.actual;

            Node<Object> index = (Node<Object>)rs.index;
            if (index == null) {
                index = head;
            }

            for (;;) {

                if (rs.cancelled) {
                    rs.index = null;
                    return;
                }

                long r = rs.requested.get();
                long e = 0;
                
                for (;;) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }
                    
                    Node<Object> n = index.get();
                    
                    if (n == null) {
                        break;
                    }
                    
                    Object o = n.value;
                    
                    if (done) {
                        if (n.get() == null) {
                            
                            if (NotificationLite.isComplete(o)) {
                                a.onComplete();
                            } else {
                                a.onError(NotificationLite.getError(o));
                            }
                            rs.index = null;
                            rs.cancelled = true;
                            return;
                        }
                    }
                    
                    if (r == 0) {
                        r = rs.requested.get() + e;
                        if (r == 0) {
                            break;
                        }
                    }

                    a.onNext((T)o);
                    r--;
                    e--;
                    
                    index = n;
                }
                
                if (e != 0L) {
                    if (rs.requested.get() != Long.MAX_VALUE) {
                        r = rs.requested.addAndGet(e);
                    }
                }
                
                if (index.get() != null && r != 0L) {
                    continue;
                }
                
                rs.index = index;
                
                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        @Override
        public int size() {
            int s = 0;
            Node<Object> h = head;
            while (s != Integer.MAX_VALUE) {
                Node<Object> next = h.get();
                if (next == null) {
                    Object o = h.value;
                    if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                        s--;
                    }
                    break;
                }
                s++;
                h = next;
            }
            
            return s;
        }
    }
    
    static final class SizeAndTimeBoundReplayBuffer<T> 
    extends AtomicReference<Object>
    implements ReplayBuffer<T> {
        /** */
        private static final long serialVersionUID = 1242561386470847675L;
        
        final int maxSize;
        final long maxAge;
        final TimeUnit unit;
        final Scheduler scheduler;
        int size;
        
        volatile TimedNode<Object> head;
        
        TimedNode<Object> tail;
        
        volatile boolean done;
        
        
        public SizeAndTimeBoundReplayBuffer(int maxSize, long maxAge, TimeUnit unit, Scheduler scheduler) {
            this.maxSize = verifyPositive(maxSize, "maxSize");
            this.maxAge = verifyPositive(maxAge, "maxAge");
            this.unit = ObjectHelper.requireNonNull(unit, "unit is null");
            this.scheduler = ObjectHelper.requireNonNull(scheduler, "scheduler is null");
            TimedNode<Object> h = new TimedNode<Object>(null, 0L);
            this.tail = h;
            this.head = h;
        }

        void trim() {
            if (size > maxSize) {
                size--;
                TimedNode<Object> h = head;
                head = h.get();
            }
            long limit = scheduler.now(unit) - maxAge;
            
            TimedNode<Object> h = head;
            
            for (;;) {
                TimedNode<Object> next = h.get();
                if (next == null) {
                    head = h;
                    break;
                }
                
                if (next.time > limit) {
                    head = h;
                    break;
                }
                
                h = next;
            }
            
        }
        
        void trimFinal() {
            long limit = scheduler.now(unit) - maxAge;
            
            TimedNode<Object> h = head;
            
            for (;;) {
                TimedNode<Object> next = h.get();
                if (next.get() == null) {
                    head = h;
                    break;
                }
                
                if (next.time > limit) {
                    head = h;
                    break;
                }
                
                h = next;
            }
        }
        
        @Override
        public void add(T value) {
            TimedNode<Object> n = new TimedNode<Object>(value, scheduler.now(unit));
            TimedNode<Object> t = tail;

            tail = n;
            size++;
            t.set(n); // releases both the tail and size
            
            trim();
        }
        
        @Override
        public void addFinal(Object notificationLite) {
            TimedNode<Object> n = new TimedNode<Object>(notificationLite, Long.MAX_VALUE);
            TimedNode<Object> t = tail;

            tail = n;
            size++;
            t.set(n); // releases both the tail and size
            trimFinal();
            
            done = true;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T getValue() {
            TimedNode<Object> prev = null;
            TimedNode<Object> h = head;

            for (;;) {
                TimedNode<Object> next = h.get();
                if (next == null) {
                    break;
                }
                prev = h;
                h = next;
            }
            
            Object v = h.value;
            if (v == null) {
                return null;
            }
            if (NotificationLite.isComplete(v) || NotificationLite.isError(v)) {
                return (T)prev.value;
            }
            
            return (T)v;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T[] getValues(T[] array) {
            TimedNode<Object> h = head;
            int s = size();
            
            if (s == 0) {
                if (array.length != 0) {
                    array[0] = null;
                }
            } else {
                if (array.length < s) {
                    array = (T[])Array.newInstance(array.getClass().getComponentType(), s);
                }

                int i = 0;
                while (i != s) {
                    TimedNode<Object> next = h.get();
                    array[i] = (T)next.value;
                    i++;
                    h = next;
                }
                if (array.length > s) {
                    array[s] = null;
                }
            }
            
            return array;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public void replay(ReplaySubscription<T> rs) {
            if (rs.getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            final Subscriber<? super T> a = rs.actual;

            TimedNode<Object> index = (TimedNode<Object>)rs.index;
            if (index == null) {
                index = head;
                if (!done) {
                    // skip old entries
                    long limit = scheduler.now(unit) - maxAge;
                    TimedNode<Object> next = index.get();
                    while (next != null) {
                        long ts = next.time;
                        if (ts > limit) {
                            break;
                        }
                        index = next;
                        next = index.get();
                    }
                }
            }

            for (;;) {

                if (rs.cancelled) {
                    rs.index = null;
                    return;
                }

                long r = rs.requested.get();
                long e = 0;
                
                for (;;) {
                    if (rs.cancelled) {
                        rs.index = null;
                        return;
                    }
                    
                    TimedNode<Object> n = index.get();
                    
                    if (n == null) {
                        break;
                    }
                    
                    Object o = n.value;
                    
                    if (done) {
                        if (n.get() == null) {
                            
                            if (NotificationLite.isComplete(o)) {
                                a.onComplete();
                            } else {
                                a.onError(NotificationLite.getError(o));
                            }
                            rs.index = null;
                            rs.cancelled = true;
                            return;
                        }
                    }
                    
                    if (r == 0) {
                        r = rs.requested.get() + e;
                        if (r == 0) {
                            break;
                        }
                    }

                    a.onNext((T)o);
                    r--;
                    e--;
                    
                    index = n;
                }
                
                if (e != 0L) {
                    if (rs.requested.get() != Long.MAX_VALUE) {
                        r = rs.requested.addAndGet(e);
                    }
                }
                
                if (index.get() != null && r != 0L) {
                    continue;
                }
                
                rs.index = index;
                
                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        @Override
        public int size() {
            int s = 0;
            TimedNode<Object> h = head;
            while (s != Integer.MAX_VALUE) {
                TimedNode<Object> next = h.get();
                if (next == null) {
                    Object o = h.value;
                    if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                        s--;
                    }
                    break;
                }
                s++;
                h = next;
            }
            
            return s;
        }
    }
}