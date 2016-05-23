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
package rx.internal.util;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.exceptions.Exceptions;
import rx.functions.*;
import rx.internal.producers.SingleProducer;
import rx.internal.schedulers.EventLoopsScheduler;
import rx.observers.Subscribers;
import rx.plugins.*;

/**
 * An Observable that emits a single constant scalar value to Subscribers.
 * <p>
 * This is a direct implementation of the Observable class to allow identifying it
 * in flatMap and bypass the subscription to it altogether.
 *
 * @param <T> the value type
 */
public final class ScalarSynchronousObservable<T> extends Observable<T> {
    /** 
     * The execution hook instance. 
     * <p>
     * Can't be final to allow tests overriding it in place; if the class
     * has been initialized, the plugin reset has no effect because
     * how RxJavaPlugins was designed.
     */
    static RxJavaObservableExecutionHook hook = RxJavaPlugins.getInstance().getObservableExecutionHook();
    /**
     * Indicates that the Producer used by this Observable should be fully
     * threadsafe. It is possible, but unlikely that multiple concurrent
     * requests will arrive to just().
     */
    static final boolean STRONG_MODE;
    static {
        String wp = System.getProperty("rx.just.strong-mode", "false");
        STRONG_MODE = Boolean.valueOf(wp);
    }

    /**
     * Creates a scalar producer depending on the state of STRONG_MODE.
     * @param <T> the type of the scalar value
     * @param s the target subscriber
     * @param v the value to emit
     * @return the created Producer
     */
    static <T> Producer createProducer(Subscriber<? super T> s, T v) {
        if (STRONG_MODE) {
            return new SingleProducer<T>(s, v);
        }
        return new WeakSingleProducer<T>(s, v);
    }
    
    /**
     * Constructs a ScalarSynchronousObservable with the given constant value.
     * @param <T> the value type
     * @param t the value to emit when requested
     * @return the new Observable
     */
    public static <T> ScalarSynchronousObservable<T> create(T t) {
        return new ScalarSynchronousObservable<T>(t);
    }

    /** The constant scalar value to emit on request. */
    final T t;

    protected ScalarSynchronousObservable(final T t) {
        super(hook.onCreate(new JustOnSubscribe<T>(t)));
        this.t = t;
    }

    /**
     * Returns the scalar constant value directly.
     * @return the scalar constant value directly
     */
    public T get() {
        return t;
    }
    
    
    /**
     * Customized observeOn/subscribeOn implementation which emits the scalar
     * value directly or with less overhead on the specified scheduler.
     * @param scheduler the target scheduler
     * @return the new observable
     */
    public Observable<T> scalarScheduleOn(final Scheduler scheduler) {
        final Func1<Action0, Subscription> onSchedule;
        if (scheduler instanceof EventLoopsScheduler) {
            final EventLoopsScheduler els = (EventLoopsScheduler) scheduler;
            onSchedule = new Func1<Action0, Subscription>() {
                @Override
                public Subscription call(Action0 a) {
                    return els.scheduleDirect(a);
                }
            };
        } else {
            onSchedule = new Func1<Action0, Subscription>() {
                @Override
                public Subscription call(final Action0 a) {
                    final Scheduler.Worker w = scheduler.createWorker();
                    w.schedule(new Action0() {
                        @Override
                        public void call() {
                            try {
                                a.call();
                            } finally {
                                w.unsubscribe();
                            }
                        }
                    });
                    return w;
                }
            };
        }
        
        return create(new ScalarAsyncOnSubscribe<T>(t, onSchedule));
    }
    
    /** The OnSubscribe callback for the Observable constructor. */
    static final class JustOnSubscribe<T> implements OnSubscribe<T> {
        final T value;

        JustOnSubscribe(T value) {
            this.value = value;
        }

        @Override
        public void call(Subscriber<? super T> s) {
            s.setProducer(createProducer(s, value));
        }
    }

    /**
     * The OnSubscribe implementation that creates the ScalarAsyncProducer for each
     * incoming subscriber.
     *
     * @param <T> the value type
     */
    static final class ScalarAsyncOnSubscribe<T> implements OnSubscribe<T> {
        final T value;
        final Func1<Action0, Subscription> onSchedule;

        ScalarAsyncOnSubscribe(T value, Func1<Action0, Subscription> onSchedule) {
            this.value = value;
            this.onSchedule = onSchedule;
        }

        @Override
        public void call(Subscriber<? super T> s) {
            s.setProducer(new ScalarAsyncProducer<T>(s, value, onSchedule));
        }
    }

    /**
     * Represents a producer which schedules the emission of a scalar value on
     * the first positive request via the given scheduler callback.
     *
     * @param <T> the value type
     */
    static final class ScalarAsyncProducer<T> extends AtomicBoolean implements Producer, Action0 {
        /** */
        private static final long serialVersionUID = -2466317989629281651L;
        final Subscriber<? super T> actual;
        final T value;
        final Func1<Action0, Subscription> onSchedule;
        
        public ScalarAsyncProducer(Subscriber<? super T> actual, T value, Func1<Action0, Subscription> onSchedule) {
            this.actual = actual;
            this.value = value;
            this.onSchedule = onSchedule;
        }

        @Override
        public void request(long n) {
            if (n < 0L) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            }
            if (n != 0 && compareAndSet(false, true)) {
                actual.add(onSchedule.call(this));
            }
        }
        
        @Override
        public void call() {
            Subscriber<? super T> a = actual;
            if (a.isUnsubscribed()) {
                return;
            }
            T v = value;
            try {
                a.onNext(v);
            } catch (Throwable e) {
                Exceptions.throwOrReport(e, a, v);
                return;
            }
            if (a.isUnsubscribed()) {
                return;
            }
            a.onCompleted();
        }
        
        @Override
        public String toString() {
            return "ScalarAsyncProducer[" + value + ", " + get() + "]";
        }
    }
    
    /**
     * Given this scalar source as input to a flatMap, avoid one step of subscription
     * and subscribes to the single Observable returned by the function.
     * <p>
     * If the functions returns another scalar, no subscription happens and this inner
     * scalar value will be emitted once requested.
     * @param <R> the result type
     * @param func the mapper function that returns an Observable for the scalar value of this
     * @return the new observable
     */
    public <R> Observable<R> scalarFlatMap(final Func1<? super T, ? extends Observable<? extends R>> func) {
        return create(new OnSubscribe<R>() {
            @Override
            public void call(final Subscriber<? super R> child) {
                Observable<? extends R> o = func.call(t);
                if (o instanceof ScalarSynchronousObservable) {
                    child.setProducer(createProducer(child, ((ScalarSynchronousObservable<? extends R>)o).t));
                } else {
                    o.unsafeSubscribe(Subscribers.wrap(child));
                }
            }
        });
    }
    
    /**
     * This is the weak version of SingleProducer that uses plain fields
     * to avoid reentrancy and as such is not threadsafe for concurrent
     * request() calls.
     *
     * @param <T> the value type
     */
    static final class WeakSingleProducer<T> implements Producer {
        final Subscriber<? super T> actual;
        final T value;
        boolean once;
        
        public WeakSingleProducer(Subscriber<? super T> actual, T value) {
            this.actual = actual;
            this.value = value;
        }
        
        @Override
        public void request(long n) {
            if (once) {
                return;
            }
            if (n < 0L) {
                throw new IllegalStateException("n >= required but it was " + n);
            }
            if (n == 0L) {
                return;
            }
            once = true;
            Subscriber<? super T> a = actual;
            if (a.isUnsubscribed()) {
                return;
            }
            T v = value;
            try {
                a.onNext(v);
            } catch (Throwable e) {
                Exceptions.throwOrReport(e, a, v);
                return;
            }

            if (a.isUnsubscribed()) {
                return;
            }
            a.onCompleted();
        }
    }
}