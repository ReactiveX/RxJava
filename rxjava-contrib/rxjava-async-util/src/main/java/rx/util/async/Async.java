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

package rx.util.async;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Action2;
import rx.util.functions.Action3;
import rx.util.functions.Action4;
import rx.util.functions.Action5;
import rx.util.functions.Action6;
import rx.util.functions.Action7;
import rx.util.functions.Action8;
import rx.util.functions.Action9;
import rx.util.functions.ActionN;
import rx.util.functions.Actions;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.Func5;
import rx.util.functions.Func6;
import rx.util.functions.Func7;
import rx.util.functions.Func8;
import rx.util.functions.Func9;
import rx.util.functions.FuncN;

/**
 * Utility methods to convert functions and actions into asynchronous
 * operations through the Observable/Observer pattern.
 */
public final class Async {
    private Async() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Invokes the specified function asynchronously and returns an Observable
     * that emits the result.
     * <p>
     * Note: The function is called immediately and once, not whenever an
     * observer subscribes to the resulting Observable. Multiple subscriptions
     * to this Observable observe the same return value.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/start.png">
     * 
     * @param func
     *            function to run asynchronously
     * @return an Observable that emits the function's result value, or notifies
     *         observers of an exception
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#start">RxJava Wiki: start()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229036.aspx">MSDN: Observable.Start</a>
     */
    public static <T> Observable<T> start(Func0<T> func) {
        return Async.toAsync(func).call();
    }

    /**
     * Invokes the specified function asynchronously on the specified scheduler
     * and returns an Observable that emits the result.
     * <p>
     * Note: The function is called immediately and once, not whenever an
     * observer subscribes to the resulting Observable. Multiple subscriptions
     * to this Observable observe the same return value.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/start.s.png">
     * 
     * @param func
     *            function to run asynchronously
     * @param scheduler
     *            scheduler to run the function on
     * @return an Observable that emits the function's result value, or notifies
     *         observers of an exception
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#start">RxJava Wiki: start()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211721.aspx">MSDN: Observable.Start</a>
     */
    public static <T> Observable<T> start(Func0<T> func, Scheduler scheduler) {
        return Async.toAsync(func, scheduler).call();
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229868.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static Func0<Observable<Void>> toAsync(Action0 action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229182.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <R> Func0<Observable<R>> toAsync(Func0<? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229657.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1> Func1<T1, Observable<Void>> toAsync(Action1<? super T1> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229755.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, R> Func1<T1, Observable<R>> toAsync(Func1<? super T1, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh211875.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2> Func2<T1, T2, Observable<Void>> toAsync(Action2<? super T1, ? super T2> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229851.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, R> Func2<T1, T2, Observable<R>> toAsync(Func2<? super T1, ? super T2, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229336.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3> Func3<T1, T2, T3, Observable<Void>> toAsync(Action3<? super T1, ? super T2, ? super T3> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229450.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, R> Func3<T1, T2, T3, Observable<R>> toAsync(Func3<? super T1, ? super T2, ? super T3, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229769.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4> Func4<T1, T2, T3, T4, Observable<Void>> toAsync(Action4<? super T1, ? super T2, ? super T3, ? super T4> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229911.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, R> Func4<T1, T2, T3, T4, Observable<R>> toAsync(Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229577.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5> Func5<T1, T2, T3, T4, T5, Observable<Void>> toAsync(Action5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229571.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, R> Func5<T1, T2, T3, T4, T5, Observable<R>> toAsync(Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh211773.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6> Func6<T1, T2, T3, T4, T5, T6, Observable<Void>> toAsync(Action6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229716.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, R> Func6<T1, T2, T3, T4, T5, T6, Observable<R>> toAsync(Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh211812.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<Void>> toAsync(Action7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229773.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<R>> toAsync(Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh228993.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<Void>> toAsync(Action8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229910.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<R>> toAsync(Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh211702.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<Void>> toAsync(Action9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9> action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh212074.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<R>> toAsync(Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     */
    public static FuncN<Observable<Void>> toAsync(ActionN action) {
        return toAsync(action, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     */
    public static <R> FuncN<Observable<R>> toAsync(FuncN<? extends R> func) {
        return toAsync(func, Schedulers.threadPoolForComputation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229439.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static Func0<Observable<Void>> toAsync(final Action0 action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh211792.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <R> Func0<Observable<R>> toAsync(final Func0<? extends R> func, final Scheduler scheduler) {
        return new Func0<Observable<R>>() {
            @Override
            public Observable<R> call() {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call();
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229822.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1> Func1<T1, Observable<Void>> toAsync(final Action1<? super T1> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229731.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, R> Func1<T1, Observable<R>> toAsync(final Func1<? super T1, ? extends R> func, final Scheduler scheduler) {
        return new Func1<T1, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229722.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2> Func2<T1, T2, Observable<Void>> toAsync(final Action2<? super T1, ? super T2> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229327.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, R> Func2<T1, T2, Observable<R>> toAsync(final Func2<? super T1, ? super T2, ? extends R> func, final Scheduler scheduler) {
        return new Func2<T1, T2, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh211787.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3> Func3<T1, T2, T3, Observable<Void>> toAsync(final Action3<? super T1, ? super T2, ? super T3> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229287.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, R> Func3<T1, T2, T3, Observable<R>> toAsync(final Func3<? super T1, ? super T2, ? super T3, ? extends R> func, final Scheduler scheduler) {
        return new Func3<T1, T2, T3, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229370.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4> Func4<T1, T2, T3, T4, Observable<Void>> toAsync(final Action4<? super T1, ? super T2, ? super T3, ? super T4> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229560.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, R> Func4<T1, T2, T3, T4, Observable<R>> toAsync(final Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func, final Scheduler scheduler) {
        return new Func4<T1, T2, T3, T4, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh212149.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5> Func5<T1, T2, T3, T4, T5, Observable<Void>> toAsync(final Action5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229606.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, R> Func5<T1, T2, T3, T4, T5, Observable<R>> toAsync(final Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> func, final Scheduler scheduler) {
        return new Func5<T1, T2, T3, T4, T5, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh212138.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6> Func6<T1, T2, T3, T4, T5, T6, Observable<Void>> toAsync(final Action6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229630.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, R> Func6<T1, T2, T3, T4, T5, T6, Observable<R>> toAsync(final Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> func, final Scheduler scheduler) {
        return new Func6<T1, T2, T3, T4, T5, T6, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229808.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<Void>> toAsync(final Action7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229794.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<R>> toAsync(final Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> func, final Scheduler scheduler) {
        return new Func7<T1, T2, T3, T4, T5, T6, T7, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229361.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<Void>> toAsync(final Action8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh228956.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<R>> toAsync(final Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> func, final Scheduler scheduler) {
        return new Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7, t8);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229662.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<Void>> toAsync(final Action9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229008.aspx'>MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<R>> toAsync(final Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> func, final Scheduler scheduler) {
        return new Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8, final T9 t9) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     */
    public static FuncN<Observable<Void>> toAsync(final ActionN action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     */
    public static <R> FuncN<Observable<R>> toAsync(final FuncN<? extends R> func, final Scheduler scheduler) {
        return new FuncN<Observable<R>>() {
            @Override
            public Observable<R> call(final Object... args) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(args);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        }
                        subject.onNext(result);
                        subject.onCompleted();
                    }
                });
                return subject;
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * <p>
     * Alias for toAsync(ActionN) intended for dynamic languages.
     * 
     * @param action
     *            the action to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     */
    public static FuncN<Observable<Void>> asyncAction(final ActionN action) {
        return toAsync(action);
    }

    /**
     * Convert a synchronous action call into an asynchronous function
     * call through an Observable sequence.
     * <p>
     * Alias for toAsync(ActionN, Scheduler) intended for dynamic languages.
     * 
     * @param action
     *            the action to convert
     * @param scheduler
     *            the scheduler used to execute the {@code action}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code action} and emits {@code null}.
     * 
     */
    public static FuncN<Observable<Void>> asyncAction(final ActionN action, final Scheduler scheduler) {
        return toAsync(action, scheduler);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * <p>
     * Alias for toAsync(FuncN) intended for dynamic languages.
     * 
     * @param func
     *            the function to convert
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     */
    public static <R> FuncN<Observable<R>> asyncFunc(final FuncN<? extends R> func) {
        return toAsync(func);
    }

    /**
     * Convert a synchronous function call into an asynchronous function
     * call through an Observable sequence.
     * <p>
     * Alias for toAsync(FuncN, Scheduler) intended for dynamic languages.
     * 
     * @param func
     *            the function to convert
     * @param scheduler
     *            the scheduler used to call the {@code func}
     * 
     * @return a function which returns an observable sequence which
     *         executes the {@code func} and emits its returned value.
     * 
     */
    public static <R> FuncN<Observable<R>> asyncFunc(final FuncN<? extends R> func, final Scheduler scheduler) {
        return toAsync(func, scheduler);
    }
}
