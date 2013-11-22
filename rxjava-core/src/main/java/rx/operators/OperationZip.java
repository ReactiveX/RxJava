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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.Func5;
import rx.util.functions.Func6;
import rx.util.functions.Func7;
import rx.util.functions.Func8;
import rx.util.functions.Func9;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

/**
 * Returns an Observable that emits the results of a function applied to sets of items emitted, in
 * sequence, by two or more other Observables.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/zip.png">
 * <p>
 * The zip operation applies this function in strict sequence, so the first item emitted by the new
 * Observable will be the result of the function applied to the first item emitted by each zipped
 * Observable; the second item emitted by the new Observable will be the result of the function
 * applied to the second item emitted by each zipped Observable; and so forth.
 * <p>
 * The resulting Observable returned from zip will invoke <code>onNext</code> as many times as the
 * number of <code>onNext</code> invocations of the source Observable that emits the fewest items.
 */
public final class OperationZip {
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, final Func2<? super T1, ? super T2, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2), Functions.fromFunc(zipFunction));
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, final Func3<? super T1, ? super T2, ? super T3, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3), Functions.fromFunc(zipFunction));
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, final Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4), Functions.fromFunc(zipFunction));
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, final Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5), Functions.fromFunc(zipFunction));
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6,
            final Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5, o6), Functions.fromFunc(zipFunction));
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7,
            final Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5, o6, o7), Functions.fromFunc(zipFunction));
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7, Observable<? extends T8> o8,
            final Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5, o6, o7, o8), Functions.fromFunc(zipFunction));
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> OnSubscribeFunc<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Observable<? extends T4> o4, Observable<? extends T5> o5, Observable<? extends T6> o6, Observable<? extends T7> o7, Observable<? extends T8> o8,
            Observable<? extends T9> o9, final Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipFunction) {
        return zip(Arrays.asList(o1, o2, o3, o4, o5, o6, o7, o8, o9), Functions.fromFunc(zipFunction));
    }
    
    public static <R> OnSubscribeFunc<R> zip(Iterable<? extends Observable<?>> ws, final FuncN<? extends R> zipFunction) {
        ManyObservables<?, R> a = new ManyObservables<Object, R>(ws, zipFunction);
        return a;
    }
    
    /*
    * ThreadSafe
    */
    /* package accessible for unit tests */static class ZipObserver<R, T> implements Observer<T> {
        final Observable<? extends T> w;
        final Aggregator<R> a;
        private final SafeObservableSubscription subscription = new SafeObservableSubscription();
        private final AtomicBoolean subscribed = new AtomicBoolean(false);
        
        public ZipObserver(Aggregator<R> a, Observable<? extends T> w) {
            this.a = a;
            this.w = w;
        }
        
        public void startWatching() {
            if (subscribed.compareAndSet(false, true)) {
                // only subscribe once even if called more than once
                subscription.wrap(w.subscribe(this));
            }
        }
        
        @Override
        public void onCompleted() {
            a.complete(this);
        }
        
        @Override
        public void onError(Throwable e) {
            a.error(this, e);
        }
        
        @Override
        public void onNext(T args) {
            try {
                a.next(this, args);
            } catch (Throwable e) {
                onError(e);
            }
        }
    }
    
    /**
     * Receive notifications from each of the Observables we are reducing and execute the zipFunction whenever we have received events from all Observables.
     *
     * This class is thread-safe.
     *
     * @param <T>
     */
    /* package accessible for unit tests */static class Aggregator<T> implements OnSubscribeFunc<T> {
        
        private volatile SynchronizedObserver<T> observer;
        private final FuncN<? extends T> zipFunction;
        private final AtomicBoolean started = new AtomicBoolean(false);
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final ConcurrentHashMap<ZipObserver<T, ?>, Boolean> completed = new ConcurrentHashMap<ZipObserver<T, ?>, Boolean>();
        
        /* we use ConcurrentHashMap despite synchronization of methods because stop() does NOT use synchronization and this map is used by it and can be called by other threads */
        private ConcurrentHashMap<ZipObserver<T, ?>, ConcurrentLinkedQueue<Object>> receivedValuesPerObserver = new ConcurrentHashMap<ZipObserver<T, ?>, ConcurrentLinkedQueue<Object>>();
        /* we use a ConcurrentLinkedQueue to retain ordering (I'd like to just use a ConcurrentLinkedHashMap for 'receivedValuesPerObserver' but that doesn't exist in standard java */
        private ConcurrentLinkedQueue<ZipObserver<T, ?>> observers = new ConcurrentLinkedQueue<ZipObserver<T, ?>>();
        
        public Aggregator(FuncN<? extends T> zipFunction) {
            this.zipFunction = zipFunction;
        }
        
        /**
         * Receive notification of a Observer starting (meaning we should require it for aggregation)
         *
         * Thread Safety => Invoke ONLY from the static factory methods at top of this class which are always an atomic execution by a single thread.
         *
         * @param w
         */
        void addObserver(ZipObserver<T, ?> w) {
            // initialize this ZipObserver
            observers.add(w);
            receivedValuesPerObserver.put(w, new ConcurrentLinkedQueue<Object>());
        }
        
        /**
         * Receive notification of a Observer completing its iterations.
         *
         * @param w
         */
        void complete(ZipObserver<T, ?> w) {
            // store that this ZipObserver is completed
            completed.put(w, Boolean.TRUE);
            // if all ZipObservers are completed, we mark the whole thing as completed
            if (completed.size() == observers.size()) {
                if (running.compareAndSet(true, false)) {
                    // this thread succeeded in setting running=false so let's propagate the completion
                    // mark ourselves as done
                    observer.onCompleted();
                }
            }
        }
        
        /**
         * Receive error for a Observer. Throw the error up the chain and stop processing.
         *
         * @param w
         */
        void error(ZipObserver<T, ?> w, Throwable e) {
            if (running.compareAndSet(true, false)) {
                // this thread succeeded in setting running=false so let's propagate the error
                observer.onError(e);
                /* since we receive an error we want to tell everyone to stop */
                stop();
            }
        }
        
        /**
         * Receive the next value from a Observer.
         * <p>
         * If we have received values from all Observers, trigger the zip function, otherwise store the value and keep waiting.
         *
         * @param w
         * @param arg
         */
        void next(ZipObserver<T, ?> w, Object arg) {
            if (observer == null) {
                throw new RuntimeException("This shouldn't be running if a Observer isn't registered");
            }
            
            /* if we've been 'unsubscribed' don't process anything further even if the things we're watching keep sending (likely because they are not responding to the unsubscribe call) */
            if (!running.get()) {
                return;
            }
            
            // store the value we received and below we'll decide if we are to send it to the Observer
            receivedValuesPerObserver.get(w).add(arg);
            
            // define here so the variable is out of the synchronized scope
            Object[] argsToZip = new Object[observers.size()];
            
            /* we have to synchronize here despite using concurrent data structures because the compound logic here must all be done atomically */
            synchronized (this) {
                // if all ZipObservers in 'receivedValues' map have a value, invoke the zipFunction
                for (ZipObserver<T, ?> rw : receivedValuesPerObserver.keySet()) {
                    if (receivedValuesPerObserver.get(rw).peek() == null) {
                        // we have a null meaning the queues aren't all populated so won't do anything
                        return;
                    }
                }
                // if we get to here this means all the queues have data
                int i = 0;
                for (ZipObserver<T, ?> rw : observers) {
                    argsToZip[i++] = receivedValuesPerObserver.get(rw).remove();
                }
            }
            // if we did not return above from the synchronized block we can now invoke the zipFunction with all of the args
            // we do this outside the synchronized block as it is now safe to call this concurrently and don't need to block other threads from calling
            // this 'next' method while another thread finishes calling this zipFunction
            observer.onNext(zipFunction.call(argsToZip));
        }
        
        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            if (started.compareAndSet(false, true)) {
                SafeObservableSubscription subscription = new SafeObservableSubscription();
                this.observer = new SynchronizedObserver<T>(observer, subscription);
                /* start the Observers */
                for (ZipObserver<T, ?> rw : observers) {
                    rw.startWatching();
                }
                
                return subscription.wrap(new Subscription() {
                    
                    @Override
                    public void unsubscribe() {
                        stop();
                    }
                    
                });
            } else {
                /* a Observer already has subscribed so blow up */
                throw new IllegalStateException("Only one Observer can subscribe to this Observable.");
            }
        }
        
        /*
        * Do NOT synchronize this because it gets called via unsubscribe which can occur on other threads
        * and result in deadlocks. (http://jira/browse/API-4060)
        *
        * AtomicObservableSubscription uses compareAndSet instead of locking to avoid deadlocks but ensure single-execution.
        *
        * We do the same in the implementation of this method.
        *
        * ThreadSafety of this method is provided by:
        * - AtomicBoolean[running].compareAndSet
        * - ConcurrentLinkedQueue[Observers]
        * - ZipObserver.subscription being an AtomicObservableSubscription
        */
        private void stop() {
            /* tell ourselves to stop processing onNext events by setting running=false */
            if (running.compareAndSet(true, false)) {
                /* propogate to all Observers to unsubscribe if this thread succeeded in setting running=false */
                for (ZipObserver<T, ?> o : observers) {
                    if (o.subscription != null) {
                        o.subscription.unsubscribe();
                    }
                }
            }
        }
        
    }
    /**
     * Merges the values across multiple sources and applies the selector
     * function.
     * <p>The resulting sequence terminates if no more pairs can be
     * established, i.e., streams of length 1 and 2 zipped will produce
     * only 1 item.</p>
     * <p>Exception semantics: errors from the source observable are
     * propagated as-is.</p>
     * @param <T> the common element type
     * @param <U> the result element type
     */
    public static class ManyObservables<T, U> implements OnSubscribeFunc<U> {
        /** */
        protected final Iterable<? extends Observable<? extends T>> sources;
        /** */
        protected final FuncN<? extends U> selector;
        /**
         * Constructor.
         * @param sources the sources
         * @param selector the result selector
         */
        public ManyObservables(
                Iterable<? extends Observable<? extends T>> sources,
                FuncN<? extends U> selector) {
            this.sources = sources;
            this.selector = selector;
        }
        
        @Override
        public Subscription onSubscribe(final Observer<? super U> observer) {
            
            final CompositeSubscription composite = new CompositeSubscription();
            
            final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
            
            final List<ItemObserver<T>> all = new ArrayList<ItemObserver<T>>();
            
            Observer<List<T>> o2 = new Observer<List<T>>() {
                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }
                @Override
                public void onError(Throwable t) {
                    observer.onError(t);
                }
                @Override
                public void onNext(List<T> value) {
                    observer.onNext(selector.call(value.toArray(new Object[value.size()])));
                }
            };
            
            for (Observable<? extends T> o : sources) {
                
                ItemObserver<T> io = new ItemObserver<T>(
                        rwLock, all, o, o2, composite);
                composite.add(io);
                all.add(io);
            }
            
            for (ItemObserver<T> io : all) {
                io.connect();
            }
            
            return composite;
        }
        /**
         * The individual line's observer.
         * @author akarnokd, 2013.01.14.
         * @param <T> the element type
         */
        public static class ItemObserver<T> implements Observer<T>, Subscription {
            /** Reader-writer lock. */
            protected  final ReadWriteLock rwLock;
            /** The queue. */
            public final Queue<Object> queue = new LinkedList<Object>();
            /** The list of the other observers. */
            public final List<ItemObserver<T>> all;
            /** The null sentinel value. */
            protected static final Object NULL_SENTINEL = new Object();
            /** The global cancel. */
            protected final Subscription cancel;
            /** The subscription to the source. */
            protected final SerialSubscription toSource = new SerialSubscription();
            /** Indicate completion of this stream. */
            protected boolean done;
            /** The source. */
            protected final Observable<? extends T> source;
            /** The observer. */
            protected final Observer<? super List<T>> observer;
            /**
             * Constructor.
             * @param rwLock the reader-writer lock to use
             * @param all all observers
             * @param source the source sequence
             * @param observer the output observer
             * @param cancel the cancellation handler
             */
            public ItemObserver(
                    ReadWriteLock rwLock,
                    List<ItemObserver<T>> all,
                    Observable<? extends T> source,
                    Observer<? super List<T>> observer,
                    Subscription cancel) {
                this.rwLock = rwLock;
                this.all = all;
                this.source = source;
                this.observer = observer;
                this.cancel = cancel;
            }
            @SuppressWarnings("unchecked")
            @Override
            public void onNext(T value) {
                rwLock.readLock().lock();
                try {
                    if (done) {
                        return;
                    }
                    queue.add(value != null ? value : NULL_SENTINEL);
                } finally {
                    rwLock.readLock().unlock();
                }
                // run collector
                if (rwLock.writeLock().tryLock()) {
                    try {
                        while (true) {
                            List<T> values = new ArrayList<T>(all.size());
                            for (ItemObserver<T> io : all) {
                                if (io.queue.isEmpty()) {
                                    if (io.done) {
                                        observer.onCompleted();
                                        cancel.unsubscribe();
                                    }
                                    return;
                                }
                                Object v = io.queue.peek();
                                if (v == NULL_SENTINEL) {
                                    v = null;
                                }
                                values.add((T)v);
                            }
                            if (values.size() == all.size()) {
                                for (ItemObserver<T> io : all) {
                                    io.queue.poll();
                                }
                                observer.onNext(values);
                            }
                        }
                    } finally {
                        rwLock.writeLock().unlock();
                    }
                }
            }
            
            @Override
            public void onError(Throwable ex) {
                boolean c = false;
                rwLock.writeLock().lock();
                try {
                    if (done) {
                        return;
                    }
                    done = true;
                    c = true;
                    observer.onError(ex);
                    cancel.unsubscribe();
                } finally {
                    rwLock.writeLock().unlock();
                }
                if (c) {
                    unsubscribe();
                }
            }
            
            @Override
            public void onCompleted() {
                boolean c = false;
                rwLock.readLock().lock();
                try {
                    done = true;
                    c = true;
                } finally {
                    rwLock.readLock().unlock();
                }
                if (rwLock.writeLock().tryLock()) {
                    try {
                        for (ItemObserver<T> io : all) {
                            if (io.queue.isEmpty() && io.done) {
                                observer.onCompleted();
                                cancel.unsubscribe();
                                return;
                            }
                        }
                    } finally {
                        rwLock.writeLock().unlock();
                    }
                }
                if (c) {
                    unsubscribe();
                }
            }
            /** Connect to the source observable. */
            public void connect() {
                toSource.setSubscription(source.subscribe(this));
            }
            @Override
            public void unsubscribe() {
                toSource.unsubscribe();
            }
            
        }
    }
}
