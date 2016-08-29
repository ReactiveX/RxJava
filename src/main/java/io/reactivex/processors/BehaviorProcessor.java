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
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Processor that emits the most recent item it has observed and all subsequent observed items to each subscribed
 * {@link Subscriber}.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.BehaviorProcessor.png" alt="">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  // observer will receive all events.
  BehaviorProcessor<Object> processor = BehaviorProcessor.create("default");
  processor.subscribe(observer);
  processor.onNext("one");
  processor.onNext("two");
  processor.onNext("three");

  // observer will receive the "one", "two" and "three" events, but not "zero"
  BehaviorProcessor<Object> processor = BehaviorProcessor.create("default");
  processor.onNext("zero");
  processor.onNext("one");
  processor.subscribe(observer);
  processor.onNext("two");
  processor.onNext("three");

  // observer will receive only onCompleted
  BehaviorProcessor<Object> processor = BehaviorProcessor.create("default");
  processor.onNext("zero");
  processor.onNext("one");
  processor.onCompleted();
  processor.subscribe(observer);
  
  // observer will receive only onError
  BehaviorProcessor<Object> processor = BehaviorProcessor.create("default");
  processor.onNext("zero");
  processor.onNext("one");
  processor.onError(new RuntimeException("error"));
  processor.subscribe(observer);
  } </pre>
 * 
 * @param <T>
 *          the type of item expected to be observed and emitted by the Processor
 */
public final class BehaviorProcessor<T> extends FlowableProcessor<T> {
    final AtomicReference<BehaviorSubscription<T>[]> subscribers;
    
    static final Object[] EMPTY_ARRAY = new Object[0];
    
    @SuppressWarnings("rawtypes")
    static final BehaviorSubscription[] EMPTY = new BehaviorSubscription[0];

    @SuppressWarnings("rawtypes")
    static final BehaviorSubscription[] TERMINATED = new BehaviorSubscription[0];
    
    final ReadWriteLock lock;
    final Lock readLock;
    final Lock writeLock;

    final AtomicReference<Object> value;
    
    boolean done;

    long index;

    /**
     * Creates a {@link BehaviorProcessor} without a default item.
     *
     * @param <T>
     *            the type of item the Subject will emit
     * @return the constructed {@link BehaviorProcessor}
     */
    public static <T> BehaviorProcessor<T> create() {
        return new BehaviorProcessor<T>();
    }
    
    /**
     * Creates a {@link BehaviorProcessor} that emits the last item it observed and all subsequent items to each
     * {@link Subscriber} that subscribes to it.
     * 
     * @param <T>
     *            the type of item the Subject will emit
     * @param defaultValue
     *            the item that will be emitted first to any {@link Subscriber} as long as the
     *            {@link BehaviorProcessor} has not yet observed any items from its source {@code Observable}
     * @return the constructed {@link BehaviorProcessor}
     */
    public static <T> BehaviorProcessor<T> createDefault(T defaultValue) {
        ObjectHelper.requireNonNull(defaultValue, "defaultValue is null");
        return new BehaviorProcessor<T>(defaultValue);
    }
    
    /**
     * Constructs an empty BehaviorProcessor.
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    BehaviorProcessor() {
        this.value = new AtomicReference<Object>();
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.subscribers = new AtomicReference<BehaviorSubscription<T>[]>(EMPTY);
    }
    
    /**
     * Constructs a BehaviorProcessor with the given initial value.
     * @param defaultValue the initial value, not null (verified)
     * @throws NullPointerException if {@code defaultValue} is null
     * @since 2.0
     */
    BehaviorProcessor(T defaultValue) {
        this();
        this.value.lazySet(ObjectHelper.requireNonNull(defaultValue, "defaultValue is null"));
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        BehaviorSubscription<T> bs = new BehaviorSubscription<T>(s, this);
        s.onSubscribe(bs);
        if (!bs.cancelled) {
            if (add(bs)) {
                if (bs.cancelled) {
                    remove(bs);
                } else {
                    bs.emitFirst();
                }
            } else {
                Object o = value.get();
                if (NotificationLite.isComplete(o)) {
                    s.onComplete();
                } else {
                    s.onError(NotificationLite.getError(o));
                }
            }
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
        Object o = NotificationLite.next(t);
        setCurrent(o);
        for (BehaviorSubscription<T> bs : subscribers.get()) {
            bs.emitNext(o, index);
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
        for (BehaviorSubscription<T> bs : terminate(o)) {
            bs.emitNext(o, index);
        }
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;
        Object o = NotificationLite.complete();
        for (BehaviorSubscription<T> bs : terminate(o)) {
            bs.emitNext(o, index);  // relaxed read okay since this is the only mutator thread
        }
    }

    @Override
    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }
    
    
    /* test support*/ int subscriberCount() {
        return subscribers.get().length;
    }

    @Override
    public Throwable getThrowable() {
        Object o = value.get();
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
        Object o = value.get();
        if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
            return null;
        }
        return NotificationLite.getValue(o);
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
    @SuppressWarnings("unchecked")
    public T[] getValues(T[] array) {
        Object o = value.get();
        if (o == null || NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
            if (array.length != 0) {
                array[0] = null;
            }
            return array;
        }
        T v = NotificationLite.getValue(o);
        if (array.length != 0) {
            array[0] = v;
            if (array.length != 1) {
                array[1] = null;
            }
        } else {
            array = (T[])Array.newInstance(array.getClass().getComponentType(), 1);
            array[0] = v;
        }
        return array;
    }
    
    @Override
    public boolean hasComplete() {
        Object o = value.get();
        return NotificationLite.isComplete(o);
    }
    
    @Override
    public boolean hasThrowable() {
        Object o = value.get();
        return NotificationLite.isError(o);
    }
    
    /**
     * Returns true if the subject has any value.
     * <p>The method is thread-safe.
     * @return true if the subject has any value
     */
    public boolean hasValue() {
        Object o = value.get();
        return o != null && !NotificationLite.isComplete(o) && !NotificationLite.isError(o);
    }
    
    
    boolean add(BehaviorSubscription<T> rs) {
        for (;;) {
            BehaviorSubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int len = a.length;
            @SuppressWarnings("unchecked")
            BehaviorSubscription<T>[] b = new BehaviorSubscription[len + 1];
            System.arraycopy(a, 0, b, 0, len);
            b[len] = rs;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    void remove(BehaviorSubscription<T> rs) {
        for (;;) {
            BehaviorSubscription<T>[] a = subscribers.get();
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
            BehaviorSubscription<T>[] b;
            if (len == 1) {
                b = EMPTY;
            } else {
                b = new BehaviorSubscription[len - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, len - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    BehaviorSubscription<T>[] terminate(Object terminalValue) {
        
        BehaviorSubscription<T>[] a = subscribers.get();
        if (a != TERMINATED) {
            a = subscribers.getAndSet(TERMINATED);
            if (a != TERMINATED) {
                // either this or atomics with lots of allocation
                setCurrent(terminalValue);
            }
        }
        
        return a;
    }
    
    void setCurrent(Object o) {
        writeLock.lock();
        try {
            index++;
            value.lazySet(o);
        } finally {
            writeLock.unlock();
        }
    }
    
    static final class BehaviorSubscription<T> extends AtomicLong implements Subscription, Predicate<Object> {
        /** */
        private static final long serialVersionUID = 3293175281126227086L;
        
        final Subscriber<? super T> actual;
        final BehaviorProcessor<T> state;
        
        boolean next;
        boolean emitting;
        AppendOnlyLinkedArrayList<Object> queue;
        
        boolean fastPath;
        
        volatile boolean cancelled;
        
        long index;

        public BehaviorSubscription(Subscriber<? super T> actual, BehaviorProcessor<T> state) {
            this.actual = actual;
            this.state = state;
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(this, n);
            }
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                
                state.remove(this);
            }
        }

        void emitFirst() {
            if (cancelled) {
                return;
            }
            Object o;
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                if (next) {
                    return;
                }
                
                BehaviorProcessor<T> s = state;
                
                Lock readLock = s.readLock;
                readLock.lock();
                try {
                    index = s.index;
                    o = s.value.get();
                } finally {
                    readLock.unlock();
                }
                
                emitting = o != null;
                next = true;
            }
            
            if (o != null) {
                if (test(o)) {
                    return;
                }
            
                emitLoop();
            }
        }
        
        void emitNext(Object value, long stateIndex) {
            if (cancelled) {
                return;
            }
            if (!fastPath) {
                synchronized (this) {
                    if (cancelled) {
                        return;
                    }
                    if (index == stateIndex) {
                        return;
                    }
                    if (emitting) {
                        AppendOnlyLinkedArrayList<Object> q = queue;
                        if (q == null) {
                            q = new AppendOnlyLinkedArrayList<Object>(4);
                            queue = q;
                        }
                        q.add(value);
                        return;
                    }
                    next = true;
                }
                fastPath = true;
            }

            test(value);
        }

        @Override
        public boolean test(Object o) {
            if (cancelled) {
                return true;
            }
            
            if (NotificationLite.isComplete(o)) {
                cancel();
                actual.onComplete();
                return true;
            } else
            if (NotificationLite.isError(o)) {
                cancel();
                actual.onError(NotificationLite.getError(o));
                return true;
            }
            
            long r = get();
            if (r != 0L) {
                actual.onNext(NotificationLite.<T>getValue(o));
                if (r != Long.MAX_VALUE) {
                    decrementAndGet();
                }
                return false;
            }
            cancel();
            actual.onError(new IllegalStateException("Could not deliver value due to lack of requests"));
            return true;
        }
        
        void emitLoop() {
            for (;;) {
                if (cancelled) {
                    return;
                }
                AppendOnlyLinkedArrayList<Object> q;
                synchronized (this) {
                    q = queue;
                    if (q == null) {
                        emitting = false;
                        return;
                    }
                    queue = null;
                }
                
                try {
                    q.forEachWhile(this);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    actual.onError(ex);
                    return;
                }
            }
        }
    }
}
