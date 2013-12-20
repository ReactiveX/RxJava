/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.operators;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Generates an observable sequence by iterating a state from an initial state 
 * until the condition returns false.
 * <p>
 * Behaves like a generalized for loop.
 */
public final class OperationGenerate {
    /** Utility class. */
    private OperationGenerate() { throw new IllegalStateException("No instances!"); }
    /**
     * Generates an observable sequence by iterating a state from an initial 
     * state until the condition returns false.
     * @param <TState> the state value
     * @param <R> the result type
     */
    public static <TState, R> OnSubscribeFunc<R> generate(
            final TState initialState,
            final Func1<? super TState, Boolean> condition,
            final Func1<? super TState, ? extends TState> iterate,
            final Func1<? super TState, ? extends R> resultSelector,
            final Scheduler scheduler) {
        return new GenerateSubscribeFunc<TState, R>(initialState, condition, iterate, resultSelector, scheduler);
    }
    /**
     * Generates an observable sequence by iterating a state, in relative timed fashion,
     * from an initial state until the condition fails.
     * @param <TState> the state value
     * @param <R> the result type
     */
    public static <TState, R> OnSubscribeFunc<R> generateTimed(
    final TState initialState,
            final Func1<? super TState, Boolean> condition,
            final Func1<? super TState, ? extends TState> iterate,
            final Func1<? super TState, ? extends R> resultSelector,
            final Func1<? super TState, Long> timeSelector,
            final TimeUnit unit,
            final Scheduler scheduler) {
        return new GenerateTimedSubscribeFunc<TState, R>(initialState, condition, iterate, resultSelector, timeSelector, unit, scheduler);
    }
    /**
     * Generates an observable sequence by iterating a state, in absolute timed fashion,
     * from an initial state until the condition fails.
     * @param <TState> the state value
     * @param <R> the result type
     */
    /* public */ static <TState, R> OnSubscribeFunc<R> generateAbsoluteTime(
            final TState initialState,
            final Func1<? super TState, Boolean> condition,
            final Func1<? super TState, ? extends TState> iterate,
            final Func1<? super TState, ? extends R> resultSelector,
            final Func1<? super TState, ? extends Date> timeSelector,
            final Scheduler scheduler) {
        return new GenerateAbsoluteTimedSubscribeFunc<TState, R>(
                initialState, condition, iterate, resultSelector, timeSelector, scheduler);
    }

    /** Generate timed subscriber function. */
    private static final class GenerateTimedSubscribeFunc<TState, R> implements OnSubscribeFunc<R> {
        private final TState initialState;
        private final Func1<? super TState, Boolean> condition;
        private final Func1<? super TState, ? extends TState> iterate;
        private final Func1<? super TState, ? extends R> resultSelector;
        private final Func1<? super TState, Long> timeSelector;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        public GenerateTimedSubscribeFunc(
                TState initialState,
                Func1<? super TState, Boolean> condition,
                Func1<? super TState, ? extends TState> iterate,
                Func1<? super TState, ? extends R> resultSelector,
                Func1<? super TState, Long> timeSelector,
                TimeUnit unit,
                Scheduler scheduler) {
            this.initialState = initialState;
            this.condition = condition;
            this.iterate = iterate;
            this.resultSelector = resultSelector;
            this.timeSelector = timeSelector;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super R> observer) {
            Long first;
            try {
                first = timeSelector.call(initialState);
            } catch (Throwable t) {
                observer.onError(t);
                return Subscriptions.empty();
            }
            
            return scheduler.schedule(initialState,
                    new LoopBody(observer), first, unit);
        }

        /** The iteration loop body. */
        private final class LoopBody implements Func2<Scheduler, TState, Subscription> {

            private final Observer<? super R> observer;

            public LoopBody(Observer<? super R> observer) {
                this.observer = observer;
            }

            @Override
            public Subscription call(Scheduler s, TState state) {
                boolean hasNext;
                try {
                    hasNext = condition.call(state);
                } catch (Throwable t) {
                    observer.onError(t);
                    return Subscriptions.empty();
                }
                if (hasNext) {
                    R result;
                    try {
                        result = resultSelector.call(state);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return Subscriptions.empty();
                    }
                    observer.onNext(result);
                    
                    TState nextState;
                    try {
                        nextState = iterate.call(state);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return Subscriptions.empty();
                    }
                    
                    Long nextDate;
                    try {
                        nextDate = timeSelector.call(initialState);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return Subscriptions.empty();
                    }
                    
                    return s.schedule(nextState, this, nextDate, unit);
                }
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }
    }

    /** Simple generate subsciption function. */
    private static final class GenerateSubscribeFunc<TState, R> implements OnSubscribeFunc<R> {

        private final TState initialState;
        private final Func1<? super TState, Boolean> condition;
        private final Func1<? super TState, ? extends TState> iterate;
        private final Func1<? super TState, ? extends R> resultSelector;
        private final Scheduler scheduler;

        public GenerateSubscribeFunc(
                TState initialState, 
                Func1<? super TState, Boolean> condition, 
                Func1<? super TState, ? extends TState> iterate,
                Func1<? super TState, ? extends R> resultSelector, 
                Scheduler scheduler
        ) {
            this.initialState = initialState;
            this.condition = condition;
            this.iterate = iterate;
            this.resultSelector = resultSelector;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super R> observer) {
            return scheduler.schedule(initialState, new LoopBody(observer));
        }
        
        /** The iteration loop body. */
        private final class LoopBody implements Func2<Scheduler, TState, Subscription> {

            private final Observer<? super R> observer;

            public LoopBody(Observer<? super R> observer) {
                this.observer = observer;
            }

            @Override
            public Subscription call(Scheduler s, TState state) {
                boolean hasNext;
                try {
                    hasNext = condition.call(state);
                } catch (Throwable t) {
                    observer.onError(t);
                    return Subscriptions.empty();
                }
                if (hasNext) {
                    R result;
                    try {
                        result = resultSelector.call(state);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return Subscriptions.empty();
                    }
                    observer.onNext(result);
                    
                    TState nextState;
                    try {
                        nextState = iterate.call(state);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return Subscriptions.empty();
                    }
                    
                    return s.schedule(nextState, this);
                }
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }
    }

    /** Generate with absolute time information. */
    private static final class GenerateAbsoluteTimedSubscribeFunc<TState, R> implements OnSubscribeFunc<R> {

        private final TState initialState;
        private final Func1<? super TState, Boolean> condition;
        private final Func1<? super TState, ? extends TState> iterate;
        private final Func1<? super TState, ? extends R> resultSelector;
        private final Func1<? super TState, ? extends Date> timeSelector;
        private final Scheduler scheduler;

        public GenerateAbsoluteTimedSubscribeFunc(
                TState initialState, 
                Func1<? super TState, Boolean> condition, 
                Func1<? super TState, ? extends TState> iterate,
                Func1<? super TState, ? extends R> resultSelector, 
                Func1<? super TState, ? extends Date> timeSelector, 
                Scheduler scheduler
        ) {
            this.initialState = initialState;
            this.condition = condition;
            this.iterate = iterate;
            this.resultSelector = resultSelector;
            this.timeSelector = timeSelector;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super R> observer) {
            Date first;
            try {
                first = timeSelector.call(initialState);
            } catch (Throwable t) {
                observer.onError(t);
                return Subscriptions.empty();
            }
            
            long delta = Math.max(0, first.getTime() - scheduler.now());
            
            return scheduler.schedule(initialState,
                    new LoopBody(observer), delta, TimeUnit.MILLISECONDS);
        }

        /** The iteration loop body. */
        private final class LoopBody implements Func2<Scheduler, TState, Subscription> {

            private final Observer<? super R> observer;

            public LoopBody(Observer<? super R> observer) {
                this.observer = observer;
            }

            @Override
            public Subscription call(Scheduler s, TState state) {
                boolean hasNext;
                try {
                    hasNext = condition.call(state);
                } catch (Throwable t) {
                    observer.onError(t);
                    return Subscriptions.empty();
                }
                if (hasNext) {
                    R result;
                    try {
                        result = resultSelector.call(state);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return Subscriptions.empty();
                    }
                    observer.onNext(result);
                    
                    TState nextState;
                    try {
                        nextState = iterate.call(state);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return Subscriptions.empty();
                    }
                    
                    Date nextDate;
                    try {
                        nextDate = timeSelector.call(initialState);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return Subscriptions.empty();
                    }
                    
                    long deltaNext = Math.max(0, nextDate.getTime() - s.now());
                    return s.schedule(nextState, this, deltaNext, TimeUnit.MILLISECONDS);
                }
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }
    }
}
