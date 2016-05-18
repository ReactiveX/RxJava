/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.*;
import rx.Observable.Operator;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.*;
import rx.internal.operators.OperatorAny;
import rx.observables.ConnectableObservable;

/**
 * Holder of named utility classes factored out from Observable to save
 * source space and help with debugging with properly named objects.
 */
public enum InternalObservableUtils {
    ;
    
    /**
     * A BiFunction that expects an integer as its first parameter and returns +1.
     */
    public static final PlusOneFunc2 COUNTER = new PlusOneFunc2();

    static final class PlusOneFunc2 implements Func2<Integer, Object, Integer> {
        @Override
        public Integer call(Integer count, Object o) {
            return count + 1;
        }
    }
    
    /**
     * A BiFunction that expects a long as its first parameter and returns +1.
     */
    public static final PlusOneLongFunc2 LONG_COUNTER = new PlusOneLongFunc2();
    
    static final class PlusOneLongFunc2 implements Func2<Long, Object, Long> {
        @Override
        public Long call(Long count, Object o) {
            return count + 1;
        }
    }

    /**
     * A bifunction comparing two objects via null-safe equals.
     */
    public static final ObjectEqualsFunc2 OBJECT_EQUALS = new ObjectEqualsFunc2();

    static final class ObjectEqualsFunc2 implements Func2<Object, Object, Boolean> {
        @Override
        public Boolean call(Object first, Object second) {
            return first == second || (first != null && first.equals(second));
        }
    }

    /**
     * A function that converts a List of Observables into an array of Observables.
     */
    public static final ToArrayFunc1 TO_ARRAY = new ToArrayFunc1();
    
    static final class ToArrayFunc1 implements Func1<List<? extends Observable<?>>, Observable<?>[]> {
        @Override
        public Observable<?>[] call(List<? extends Observable<?>> o) {
            return o.toArray(new Observable<?>[o.size()]);
        }
    }
    
    /**
     * Returns a Func1 that checks if its argument is null-safe equals with the given
     * constant reference.
     * @param other the other object to check against (nulls allowed)
     * @return the comparison function
     */
    public static Func1<Object, Boolean> equalsWith(Object other) {
        return new EqualsWithFunc1(other);
    }
    
    static final class EqualsWithFunc1 implements Func1<Object, Boolean> {
        final Object other;
        
        public EqualsWithFunc1(Object other) {
            this.other = other;
        }
        
        @Override
        public Boolean call(Object t) {
            return t == other || (t != null && t.equals(other));
        }
    }

    /**
     * Returns a Func1 that checks if its argument is an instance of
     * the supplied class.
     * @param clazz the class to check against
     * @return the comparison function
     */
    public static Func1<Object, Boolean> isInstanceOf(Class<?> clazz) {
        return new IsInstanceOfFunc1(clazz);
    }
    
    static final class IsInstanceOfFunc1 implements Func1<Object, Boolean> {
        final Class<?> clazz;
        
        public IsInstanceOfFunc1(Class<?> other) {
            this.clazz = other;
        }
        
        @Override
        public Boolean call(Object t) {
            return clazz.isInstance(t);
        }
    }

    /**
     * Returns a function that dematerializes the notification signal from an Observable and calls
     * a notification handler with a null for non-terminal events.
     * @param notificationHandler the handler to notify with nulls
     * @return the Func1 instance
     */
    public static final Func1<Observable<? extends Notification<?>>, Observable<?>> createRepeatDematerializer(Func1<? super Observable<? extends Void>, ? extends Observable<?>> notificationHandler) {
        return new RepeatNotificationDematerializer(notificationHandler);
    }
    
    static final class RepeatNotificationDematerializer implements Func1<Observable<? extends Notification<?>>, Observable<?>> {
        
        final Func1<? super Observable<? extends Void>, ? extends Observable<?>> notificationHandler;
        
        public RepeatNotificationDematerializer(Func1<? super Observable<? extends Void>, ? extends Observable<?>> notificationHandler) {
            this.notificationHandler = notificationHandler;
        }
        
        @Override
        public Observable<?> call(Observable<? extends Notification<?>> notifications) {
            return notificationHandler.call(notifications.map(RETURNS_VOID));
        }
    };
    
    static final ReturnsVoidFunc1 RETURNS_VOID = new ReturnsVoidFunc1();
    
    static final class ReturnsVoidFunc1 implements Func1<Object, Void> {
        @Override
        public Void call(Object t) {
            return null;
        }
    }

    /**
     * Creates a Func1 which calls the selector function with the received argument, applies an
     * observeOn on the result and returns the resulting Observable.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param selector the selector function
     * @param scheduler the scheduler to apply on the output of the selector
     * @return the new Func1 instance
     */
    public static <T, R> Func1<Observable<T>, Observable<R>> createReplaySelectorAndObserveOn(
            Func1<? super Observable<T>, ? extends Observable<R>> selector, 
                    Scheduler scheduler) {
        return new SelectorAndObserveOn<T, R>(selector, scheduler);
    }
    
    static final class SelectorAndObserveOn<T, R> implements Func1<Observable<T>, Observable<R>> {
        final Func1<? super Observable<T>, ? extends Observable<R>> selector;
        final Scheduler scheduler;

        public SelectorAndObserveOn(Func1<? super Observable<T>, ? extends Observable<R>> selector,
                Scheduler scheduler) {
            super();
            this.selector = selector;
            this.scheduler = scheduler;
        }



        @Override
        public Observable<R> call(Observable<T> t) {
            return selector.call(t).observeOn(scheduler);
        }
    }

    /**
     * Returns a function that dematerializes the notification signal from an Observable and calls
     * a notification handler with the Throwable.
     * @param notificationHandler the handler to notify with Throwables
     * @return the Func1 instance
     */
    public static final Func1<Observable<? extends Notification<?>>, Observable<?>> createRetryDematerializer(Func1<? super Observable<? extends Throwable>, ? extends Observable<?>> notificationHandler) {
        return new RetryNotificationDematerializer(notificationHandler);
    }

    static final class RetryNotificationDematerializer implements Func1<Observable<? extends Notification<?>>, Observable<?>> {
        final Func1<? super Observable<? extends Throwable>, ? extends Observable<?>> notificationHandler;
        
        public RetryNotificationDematerializer(Func1<? super Observable<? extends Throwable>, ? extends Observable<?>> notificationHandler) {
            this.notificationHandler = notificationHandler;
        }
        
        @Override
        public Observable<?> call(Observable<? extends Notification<?>> notifications) {
            return notificationHandler.call(notifications.map(ERROR_EXTRACTOR));
        }
    }
    
    static final NotificationErrorExtractor ERROR_EXTRACTOR = new NotificationErrorExtractor();
    
    static final class NotificationErrorExtractor implements Func1<Notification<?>, Throwable> {
        @Override
        public Throwable call(Notification<?> t) {
            return t.getThrowable();
        }
    }
    
    /**
     * Returns a Func0 that supplies the ConnectableObservable returned by calling replay() on the source.
     * @param <T> the input value type
     * @param source the source to call replay on by the supplier function
     * @return the new Func0 instance
     */
    public static <T> Func0<ConnectableObservable<T>> createReplaySupplier(final Observable<T> source) {
        return new ReplaySupplierNoParams<T>(source);
    }

    private static final class ReplaySupplierNoParams<T> implements Func0<ConnectableObservable<T>> {
        private final Observable<T> source;

        private ReplaySupplierNoParams(Observable<T> source) {
            this.source = source;
        }

        @Override
        public ConnectableObservable<T> call() {
            return source.replay();
        }
    }
    /**
     * Returns a Func0 that supplies the ConnectableObservable returned by calling a parameterized replay() on the source.
     * @param <T> the input value type
     * @param source the source to call replay on by the supplier function
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable observable can replay
     * @return the new Func0 instance
     */
    public static <T> Func0<ConnectableObservable<T>> createReplaySupplier(final Observable<T> source, final int bufferSize) {
        return new ReplaySupplierBuffer<T>(source, bufferSize);
    }

    static final class ReplaySupplierBuffer<T> implements Func0<ConnectableObservable<T>> {
        private final Observable<T> source;
        private final int bufferSize;

        private ReplaySupplierBuffer(Observable<T> source, int bufferSize) {
            this.source = source;
            this.bufferSize = bufferSize;
        }

        @Override
        public ConnectableObservable<T> call() {
            return source.replay(bufferSize);
        }
    }

    /**
     * Returns a Func0 that supplies the ConnectableObservable returned by calling a parameterized replay() on the source.
     * @param <T> the input value type
     * @param source the source to call replay on by the supplier function
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler the scheduler to use for timing information
     * @return the new Func0 instance
     */
    public static <T> Func0<ConnectableObservable<T>> createReplaySupplier(final Observable<T> source, 
            final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new ReplaySupplierBufferTime<T>(source, time, unit, scheduler);
    }

    static final class ReplaySupplierBufferTime<T> implements Func0<ConnectableObservable<T>> {
        private final TimeUnit unit;
        private final Observable<T> source;
        private final long time;
        private final Scheduler scheduler;

        private ReplaySupplierBufferTime(Observable<T> source, long time, TimeUnit unit, Scheduler scheduler) {
            this.unit = unit;
            this.source = source;
            this.time = time;
            this.scheduler = scheduler;
        }

        @Override
        public ConnectableObservable<T> call() {
            return source.replay(time, unit, scheduler);
        }
    }

    /**
     * Returns a Func0 that supplies the ConnectableObservable returned by calling a parameterized replay() on the source.
     * @param <T> the input value type
     * @param source the source to call replay on by the supplier function
     * @param bufferSize
     *            the buffer size that limits the number of items the connectable observable can replay
     * @param time
     *            the duration of the window in which the replayed items must have been emitted
     * @param unit
     *            the time unit of {@code time}
     * @param scheduler the scheduler to use for timing information
     * @return the new Func0 instance
     */
    public static <T> Func0<ConnectableObservable<T>> createReplaySupplier(final Observable<T> source, 
            final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new ReplaySupplierTime<T>(source, bufferSize, time, unit, scheduler);
    }

    static final class ReplaySupplierTime<T> implements Func0<ConnectableObservable<T>> {
        private final long time;
        private final TimeUnit unit;
        private final Scheduler scheduler;
        private final int bufferSize;
        private final Observable<T> source;

        private ReplaySupplierTime(Observable<T> source, int bufferSize, long time, TimeUnit unit,
                Scheduler scheduler) {
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
            this.bufferSize = bufferSize;
            this.source = source;
        }

        @Override
        public ConnectableObservable<T> call() {
            return source.replay(bufferSize, time, unit, scheduler);
        }
    }

    /**
     * Returns a Func2 which calls a collector with its parameters and returns the first (R) parameter.
     * @param <T> the input value type
     * @param <R> the result value type
     * @param collector the collector action to call
     * @return the new Func2 instance
     */
    public static <T, R> Func2<R, T, R> createCollectorCaller(Action2<R, ? super T> collector) {
        return new CollectorCaller<T, R>(collector);
    }
    
    static final class CollectorCaller<T, R> implements Func2<R, T, R> {
        final Action2<R, ? super T> collector;
        
        public CollectorCaller(Action2<R, ? super T> collector) {
            this.collector = collector;
        }
        
        @Override
        public R call(R state, T value) {
            collector.call(state, value);
            return state;
        }
    }

    /**
     * Throws an OnErrorNotImplementedException when called.
     */
    public static final Action1<Throwable> ERROR_NOT_IMPLEMENTED = new ErrorNotImplementedAction();
    
    static final class ErrorNotImplementedAction implements Action1<Throwable> {
        @Override
        public void call(Throwable t) {
            throw new OnErrorNotImplementedException(t);
        }
    }
    
    public static final Operator<Boolean, Object> IS_EMPTY = new OperatorAny<Object>(UtilityFunctions.alwaysTrue(), true);
}
