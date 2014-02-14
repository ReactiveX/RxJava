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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.BooleanSubscription;
import rx.util.Timestamped;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * A replay subject with size-bounded buffer.
 * @param <T> the buffered value type
 */
public class BoundedReplaySubject<T> extends Subject<T, T>  {
    /**
     * Create a replay subject with an unbounded capacity.
     * @param <T> the buffered value type
     * @return the created replay subject
     */
    public static <T> BoundedReplaySubject<T> create() {
        return create((Scheduler)null);
    }
    /**
     * Create a replay subject with the given capacity.
     * @param <T> the buffered value type
     * @param capacity the maximum number of values to retain
     * @return the created replay subject
     */
    public static <T> BoundedReplaySubject<T> create(int capacity) {
        return create(capacity, (Scheduler)null);
    }
    /**
     * Create a replay subject with an unbounded capacity.
     * @param <T> the buffered value type
     * @param scheduler the scheduler where the event emission is performed
     * @return the created replay subject
     */
    public static <T> BoundedReplaySubject<T> create(Scheduler scheduler) {
        return create(Integer.MAX_VALUE, scheduler);
    }
    /**
     * Create a replay subject with the given capacity.
     * @param <T> the buffered value type
     * @param capacity the maximum number of values to retain
     * @param scheduler the scheduler where the event emission is performed
     * @return the created replay subject
     */
    public static <T> BoundedReplaySubject<T> create(int capacity, Scheduler scheduler) {
        State s = new State(new BoundedBufferPolicy(capacity), scheduler == Schedulers.immediate() ? null : scheduler);
        return new BoundedReplaySubject<T>(new SubscribeAction<T>(s), s);
    }
    /**
     * Create a replay subject which doesn't replay values older than the given age
     * relative to the current time as provided by the immediate scheduler.
     * @param <T> the buffered value type
     * @param maxAge the maximum age
     * @param maxAgeUnit the unit of the maximum age
     * @return the created replay subject
     */
    public static <T> BoundedReplaySubject<T> create(long maxAge, TimeUnit maxAgeUnit) {
        return create(maxAge, maxAgeUnit, Schedulers.immediate());
    }
    /**
     * Create a replay subject with the given capacity which doesn't replay values older than the given age
     * relative to the current time as provided by the immediate scheduler.
     * @param <T> the buffered value type
     * @param capacity the maximum number of values to retain
     * @param maxAge the maximum age
     * @param maxAgeUnit the unit of the maximum age
     * @return the created replay subject
     */
    public static <T> BoundedReplaySubject<T> create(int capacity, long maxAge, TimeUnit maxAgeUnit) {
        return create(capacity, maxAge, maxAgeUnit, Schedulers.immediate());
    }
    /**
     * Create a replay subject which doesn't replay values older than the given age
     * relative to the current time as provided by the supplied scheduler.
     * @param <T> the buffered value type
     * @param maxAge the maximum age
     * @param maxAgeUnit the unit of the maximum age
     * @param scheduler the scheduler which provides the value of current time and a place to emit values
     * @return the created replay subject
     */
    public static <T> BoundedReplaySubject<T> create(long maxAge, TimeUnit maxAgeUnit, Scheduler scheduler) {
        return create(Integer.MAX_VALUE, maxAge, maxAgeUnit, scheduler);
    }
    /**
     * Create a replay subject with the given capacity which doesn't replay values older than the given age
     * relative to the current time as provided by the immediate scheduler.
     * @param <T> the buffered value type
     * @param capacity the maximum number of values to retain
     * @param maxAge the maximum age
     * @param maxAgeUnit the unit of the maximum age
     * @param scheduler the scheduler which provides the value of current time and a place to emit values
     * @return the created replay subject
     */
    public static <T> BoundedReplaySubject<T> create(int capacity, long maxAge, TimeUnit maxAgeUnit, Scheduler scheduler) {
        State s = new State(new TimedAndBoundedBufferPolicy(capacity, scheduler, maxAgeUnit.toMillis(maxAge)), 
                scheduler == Schedulers.immediate() ? null : scheduler);
        return new BoundedReplaySubject<T>(new SubscribeAction<T>(s), s);
    }
    // oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
    /** The internal state. */
    final State state;
    /** Null value indicator. */
    static final Object NULL_SENTINEL = new Object();
    /** Completion indicator. */
    static final Object COMPLETED_SENTINEL = new Object();
    /** Error indicator. */
    static final class ErrorSentinel {
        final Throwable e;
        public ErrorSentinel(Throwable e) {
            this.e = e;
        }
    }
    /** Subscription requested indicator. */
    static final class SubscribeSentinel {
        final Subscriber<?> actual;
        public SubscribeSentinel(Subscriber<?> actual) {
            this.actual = actual;
        }
    }
    /** Unsubscription requested indicator. */
    static final class UnsubscribeSentinel {
        final Subscriber<?> actual;
        public UnsubscribeSentinel(Subscriber<?> actual) {
            this.actual = actual;
        }
    }
    
    private BoundedReplaySubject(OnSubscribe<T> sub, State s) {
        super(sub);
        this.state = s;
    }
    @Override
    public void onNext(T t) {
        state.tick(t != null ? t : NULL_SENTINEL);
    }
    
    @Override
    public void onError(Throwable e) {
        state.tick(new ErrorSentinel(e));
    }
    
    @Override
    public void onCompleted() {
        state.tick(COMPLETED_SENTINEL);
    }
    static final Object[] EMPTY = new Object[0];
    /** The state of the replaysubject. */
    static final class State implements Action1<Scheduler.Inner> {
        /** The current array of subscribers. */
        volatile Object[] subscribers;
        /** The buffering policy. */
        final BufferPolicy buffer;
        /** Queue of tasks. */
        //final ConcurrentLinkedQueue<Object> queue;
        final MPSCQueue queue;
        /** Tasks to perform. */
        final AtomicInteger wip;
        /** Indicates a completed state. */
        volatile boolean done;
        /** The first onError or null. */
        volatile Throwable exception;
        /** The optional emission scheduler. */
        final Scheduler emissionScheduler;
        /** The captured inner scheduler. */
        volatile Scheduler.Inner inner;
        
        public State(BufferPolicy buffer, Scheduler emissionScheduler) {
            this.subscribers = EMPTY;
            this.buffer = buffer;
            this.emissionScheduler = emissionScheduler;
//            this.queue = new ConcurrentLinkedQueue<Object>();
            this.queue = new MPSCQueue();
            this.wip = new AtomicInteger();
        }
        /** Queue an event or action and execute it in a serialized way. */
        void tick(Object action) {
            queue.offer(action);
            if (wip.getAndIncrement() == 0) {
                if (emissionScheduler == null) {
                    emissionLoop();
                } else {
                    if (inner == null) {
                        emissionScheduler.schedule(this);
                    } else {
                        inner.schedule(this);
                    }
                }
            }
        }

        @Override
        public void call(Scheduler.Inner t1) {
            inner = t1;
            emissionLoop();
        }
        
        /** Process the queued actions. */
        void emissionLoop() {
            do {
                Object o = queue.poll();
                valueDispatch(o);
            } while (wip.decrementAndGet() > 0);
        }
        
        void valueDispatch(Object o) {
            if (o instanceof SubscribeSentinel) {
                SubscribeSentinel subs = (SubscribeSentinel)o;
                if (done) {
                    if (replay(subs.actual)) {
                        if (exception != null) {
                            subs.actual.onError(exception);
                        } else {
                            subs.actual.onCompleted();
                        }
                    }
                } else {
                    addSubscriber(subs.actual);
                }
            } else
            if (o instanceof UnsubscribeSentinel) {
                UnsubscribeSentinel uns = (UnsubscribeSentinel)o;
                removeSubscriber(uns.actual);
            } else
            if (o instanceof ErrorSentinel) {
                if (!done) {
                    ErrorSentinel es = (ErrorSentinel)o;
                    done = true;
                    exception = es.e;
                    dispatchError(es.e);
                }
            } else
            if (o == COMPLETED_SENTINEL) {
                if (!done) {
                    done = true;
                    dispatchCompleted();
                }
            } else {
                if (o == NULL_SENTINEL) {
                    o = null;
                }
                dispatchNext(o);
            }
        }
        
        /** Dispatch an exception and clear subscribers. */
        void dispatchError(Throwable t) {
            Object[] localSubscribers = subscribers;
            subscribers = EMPTY;
            for (Object o : localSubscribers) {
                Subscriber<?> s = ((Subscriber<?>)o);
                if (!s.isUnsubscribed()) {
                    try {
                        s.onError(t);
                    } catch (Throwable e) {
                        // TODO ignored?
                    }
                }
            }
        }
        /** Dispatch completed events. */
        void dispatchCompleted() {
            Object[] localSubscribers = subscribers;
            subscribers = EMPTY;
            for (Object o : localSubscribers) {
                Subscriber<?> s = ((Subscriber<?>)o);
                if (!s.isUnsubscribed()) {
                    try {
                        s.onCompleted();
                    } catch (Throwable t) {
                        // TODO ignored?
                    }
                }
            }
        }
        /** Dispatch a regular value. */
        void dispatchNext(Object value) {
            buffer.add(value);
            Object[] localSubscribers = subscribers;
            for (Object o : localSubscribers) {
                @SuppressWarnings("unchecked")
                Subscriber<Object> s = ((Subscriber<Object>)o);
                if (!s.isUnsubscribed()) {
                    try {
                        s.onNext(value);
                    } catch (Throwable t) {
                        try {
                            s.onError(t);
                        } catch (Throwable t2) {
                            // ignored?
                        }
                    }
                }
            }
        }
        /** 
         * Replay the buffered values to the subscriber. 
         * @return false if the replay crashed the subscriber
         */
        boolean replay(Subscriber<?> o) {
            @SuppressWarnings("unchecked")
            Subscriber<Object> s = ((Subscriber<Object>)o);
            return buffer.replay(s);
        }

        /** Remove all instances of the specified subscriber. */
        void removeSubscriber(Subscriber<?> s) {
            Object[] localSubscribers = subscribers;
            final int n = localSubscribers.length;
            if (n == 0) {
                subscribers = EMPTY;
                return;
            } else
            if (n == 1) {
                if (localSubscribers[0].equals(s)) {
                    subscribers = EMPTY;
                }
                return; // not in this list
            }
            Object[] newSubscribers = new Object[n - 1];
            int j = 0;
            for (Object o : localSubscribers) {
                if (!o.equals(s)) {
                    if (j == n) {
                        return; // not in this list
                    }
                    newSubscribers[j] = o;
                    j++;
                }
            }
            if (j == 0) {
                subscribers = EMPTY;
            } else
            if (j < newSubscribers.length) {
                Object[] newSub2 = new Object[j];
                System.arraycopy(newSubscribers, 0, newSub2, 0, j);
                subscribers = newSub2;
            } else {
                subscribers = newSubscribers;
            }
        }
        /**
         * Add a new subscriber but only if it doesn't throw while replaying.
         * @param s 
         */
        void addSubscriber(Subscriber<?> s) {
            if (replay(s)) {
                Object[] localSubscribers = subscribers;
                final int n = localSubscribers.length;
                Object[] newSubscribers = new Object[n + 1];
                newSubscribers[n] = s;
                subscribers = newSubscribers;
                s.add(BooleanSubscription.create(new UnsubscribeAction(this, s)));
            }
        }
    }
    /** Enqueues an unsubscription action. */
    static final class UnsubscribeAction implements Action0 {
        final State state;
        final Subscriber s;

        public UnsubscribeAction(State state, Subscriber s) {
            this.state = state;
            this.s = s;
        }

        @Override
        public void call() {
            state.tick(new UnsubscribeSentinel(s));
        }
    }
    /** The subscribe action. */
    static final class SubscribeAction<T> implements OnSubscribe<T> {
        final State s;
        
        public SubscribeAction(State s) {
            this.s = s;
        }
        @Override
        public void call(Subscriber<? super T> t1) {
            s.tick(new SubscribeSentinel(t1));
        }
    }
    /**
     * Base class for buffering policies.
     */
    static abstract class BufferPolicy {
        /** Add a new value to the buffer. */
        abstract void add(Object o);
        /** Test if the value from the buffer is still valid. */
        abstract boolean test(Object o);
        /** Iterate over the buffer. */
        abstract Iterator<Object> buffer();
        /** Perfrom any transformation on the buffered value before emission. */
        abstract Object transform(Object v);
        /** Replay the contents of the buffer to the subscriber. */
        final boolean replay(Subscriber<Object> s) {
            Iterator<Object> it = buffer();
            while (it.hasNext()) {
                Object v = it.next();
                if (test(v)) {
                    if (!s.isUnsubscribed()) {
                        try {
                            v = transform(v);
                            s.onNext(v);
                        } catch (Throwable t) {
                            try {
                                s.onError(t);
                                return false;
                            } catch (Throwable t2) {
                                // ignored?
                            }
                        }
                    }
                } else {
                    it.remove();
                }
            }
            return !s.isUnsubscribed();            
        }
    }
    /**
     * Buffering policy based on capacity constraints.
     */
    static final class BoundedBufferPolicy extends BufferPolicy {
        final LinkedList<Object> buffer;
        final int capacity;

        public BoundedBufferPolicy(int capacity) {
            this.buffer = new LinkedList<Object>();
            this.capacity = capacity;
        }

        @Override
        void add(Object o) {
            if (capacity == 0) {
                return;
            }
            buffer.addLast(o);
            if (buffer.size() > capacity) {
                buffer.removeFirst();
            }
        }

        @Override
        Iterator<Object> buffer() {
            return buffer.iterator();
        }

        @Override
        boolean test(Object o) {
            return true;
        }

        @Override
        Object transform(Object v) {
            return v;
        }
        
    }
    /**
     * Timed and bounded buffering policy.
     */
    static final class TimedAndBoundedBufferPolicy extends BufferPolicy {
        final LinkedList<Object> buffer;
        final int capacity;
        final Scheduler timesource;
        final long maxAgeMillis;

        public TimedAndBoundedBufferPolicy(int capacity, Scheduler timesource, long maxAgeMillis) {
            this.buffer = new LinkedList<Object>();
            this.capacity = capacity;
            this.timesource = timesource;
            this.maxAgeMillis = maxAgeMillis;
        }

        @Override
        void add(Object o) {
            if (capacity == 0) {
                return;
            }
            long now = timesource.now();
            buffer.addLast(new Timestamped<Object>(now, o));
            // evict overcapacity
            if (buffer.size() > capacity) {
                buffer.removeFirst();
            }
            // evict too old
            Iterator<Object> it = buffer();
            while (it.hasNext()) {
                if (test(it.next(), now)) {
                    break;
                } else {
                    it.remove();
                }
            }
        }

        @Override
        Iterator<Object> buffer() {
            return buffer.iterator();
        }

        boolean test(Object o, long now) {
            Timestamped<?> ts = (Timestamped<?>)o;
            return ts.getTimestampMillis() >= now - maxAgeMillis; // FIXME > or >= is correct?
        }
        
        @Override
        boolean test(Object o) {
            return test(o, timesource.now());
        }

        @Override
        Object transform(Object v) {
            return ((Timestamped<?>)v).getValue();
        }
    }
    
    public static final class MPSCQueue {
        static final class N {
            final Object o;
            N next;
            public N(Object o) {
                this.o = o;
            }
        }
        N head;
        N tail;
        public synchronized void offer(Object o) {
            N n = new N(o);
            
            if (head == null) {
                head = n;
                tail = n;
            } else {
                tail.next = n;
                tail = n;
            }
        }
        // make sure poll is never called without a guaranteed offer before it!
        public synchronized Object poll() {
            N n = head;
            
            head = n.next;
            if (head == null) {
                tail = null;
            }
            
            return n.o;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append('[');
            N h = head;
            while (h != null) {
                b.append(h.o);
                if (h.next != null) {
                    b.append(", ");
                }
                h = h.next;
            }
            b.append(']');
            return b.toString();
        }
        
    }
}
