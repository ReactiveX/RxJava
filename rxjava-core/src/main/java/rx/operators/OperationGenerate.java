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
import rx.util.TimeSpan;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Generates an observable sequence by iterating a state from an initial state 
 * until the condition returns false.
 * <p>
 * Behaves like a generalized for loop.
 */
public final class OperationGenerate {
    /**
     * Generates an observable sequence by iterating a state from an initial 
     * state until the condition returns false.
     */
    public static <TState, R> OnSubscribeFunc<R> generate(
            final TState initialState,
            final Func1<TState, Boolean> condition,
            final Func1<TState, TState> iterate,
            final Func1<TState, R> resultSelector,
            final Scheduler scheduler) {
        return new OnSubscribeFunc<R>() {
            @Override
            public Subscription onSubscribe(final Observer<? super R> observer) {
                return scheduler.schedule(initialState, new Func2<Scheduler, TState, Subscription>() {
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
                });
            }
        };
    }
    /**
     * Generates an observable sequence by iterating a state, in relative timed fashion,
     * from an initial state until the condition fails.
     */
    public static <TState, R> OnSubscribeFunc<R> generate(
    final TState initialState,
            final Func1<TState, Boolean> condition,
            final Func1<TState, TState> iterate,
            final Func1<TState, R> resultSelector,
            final Func1<TState, TimeSpan> timeSelector,
            final Scheduler scheduler) {
        return new OnSubscribeFunc<R>() {
            @Override
            public Subscription onSubscribe(final Observer<? super R> observer) {
                TimeSpan first;
                try {
                    first = timeSelector.call(initialState);
                } catch (Throwable t) {
                    observer.onError(t);
                    return Subscriptions.empty();
                }
                
                return scheduler.schedule(initialState, 
                        new Func2<Scheduler, TState, Subscription>() {
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
                            
                            TimeSpan nextDate;
                            try {
                                nextDate = timeSelector.call(initialState);
                            } catch (Throwable t) {
                                observer.onError(t);
                                return Subscriptions.empty();
                            }

                            return s.schedule(nextState, this, nextDate.value(), nextDate.unit());
                        }
                        observer.onCompleted();
                        return Subscriptions.empty();
                    }
                }, first.value(), first.unit());
            }
        };
    }
    /**
     * Generates an observable sequence by iterating a state, in absolute timed fashion,
     * from an initial state until the condition fails.
     */
    public static <TState, R> OnSubscribeFunc<R> generateAbsoluteTime(
    final TState initialState,
            final Func1<TState, Boolean> condition,
            final Func1<TState, TState> iterate,
            final Func1<TState, R> resultSelector,
            final Func1<TState, Date> timeSelector,
            final Scheduler scheduler) {
        return new OnSubscribeFunc<R>() {
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
                        new Func2<Scheduler, TState, Subscription>() {
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
                }, delta, TimeUnit.MILLISECONDS);
            }
        };
    }
}
