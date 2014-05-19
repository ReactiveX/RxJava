/**
 * Copyright 2014 Netflix, Inc.
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
package rx.util.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Action4;
import rx.functions.Action5;
import rx.functions.Action6;
import rx.functions.Action7;
import rx.functions.Action8;
import rx.functions.Action9;
import rx.functions.ActionN;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.SerialSubscription;
import rx.util.async.operators.Functionals;
import rx.util.async.operators.OperatorDeferFuture;
import rx.util.async.operators.OperatorForEachFuture;
import rx.util.async.operators.OperatorFromFunctionals;
import rx.util.async.operators.OperatorStartFuture;

/**
 * Utility methods to convert functions and actions into asynchronous operations through the Observable/Observer
 * pattern.
 */
public final class Async {
    
    private Async() {
        throw new IllegalStateException("No instances!");
    }
    
    /**
     * Invokes the specified function asynchronously and returns an Observable that emits the result.
     * <p>
     * Note: The function is called immediately and once, not whenever an observer subscribes to the resulting
     * Observable. Multiple subscriptions to this Observable observe the same return value.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/start.png">
     *
     * @param <T> the result value type
     * @param func function to run asynchronously
     * @return an Observable that emits the function's result value, or notifies observers of an exception
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-start">RxJava Wiki: start()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229036.aspx">MSDN: Observable.Start</a>
     */
    public static <T> Observable<T> start(Func0<T> func) {
        return Async.toAsync(func).call();
    }
    
    /**
     * Invokes the specified function asynchronously on the specified Scheduler and returns an Observable that
     * emits the result.
     * <p>
     * Note: The function is called immediately and once, not whenever an observer subscribes to the resulting
     * Observable. Multiple subscriptions to this Observable observe the same return value.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/start.s.png">
     *
     * @param <T> the result value type
     * @param func function to run asynchronously
     * @param scheduler Scheduler to run the function on
     * @return an Observable that emits the function's result value, or notifies observers of an exception
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-start">RxJava Wiki: start()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211721.aspx">MSDN: Observable.Start</a>
     */
    public static <T> Observable<T> start(Func0<T> func, Scheduler scheduler) {
        return Async.toAsync(func, scheduler).call();
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229868.aspx">MSDN: Observable.ToAsync</a>
     */
    public static Func0<Observable<Void>> toAsync(Action0 action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <R> the result value type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229182.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <R> Func0<Observable<R>> toAsync(Func0<? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> first parameter type of the action
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229657.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1> Func1<T1, Observable<Void>> toAsync(Action1<? super T1> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> first parameter type of the action
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229755.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, R> Func1<T1, Observable<R>> toAsync(Func1<? super T1, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211875.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2> Func2<T1, T2, Observable<Void>> toAsync(Action2<? super T1, ? super T2> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229851.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, R> Func2<T1, T2, Observable<R>> toAsync(Func2<? super T1, ? super T2, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229336.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3> Func3<T1, T2, T3, Observable<Void>> toAsync(Action3<? super T1, ? super T2, ? super T3> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229450.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, R> Func3<T1, T2, T3, Observable<R>> toAsync(Func3<? super T1, ? super T2, ? super T3, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229769.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4> Func4<T1, T2, T3, T4, Observable<Void>> toAsync(Action4<? super T1, ? super T2, ? super T3, ? super T4> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229911.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, R> Func4<T1, T2, T3, T4, Observable<R>> toAsync(Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229577.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5> Func5<T1, T2, T3, T4, T5, Observable<Void>> toAsync(Action5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229571.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, R> Func5<T1, T2, T3, T4, T5, Observable<R>> toAsync(Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211773.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6> Func6<T1, T2, T3, T4, T5, T6, Observable<Void>> toAsync(Action6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229716.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, R> Func6<T1, T2, T3, T4, T5, T6, Observable<R>> toAsync(Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211812.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<Void>> toAsync(Action7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229773.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<R>> toAsync(Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh228993.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<Void>> toAsync(Action8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229910.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<R>> toAsync(Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <T9> the ninth parameter type
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211702.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<Void>> toAsync(Action9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9> action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <T9> the ninth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212074.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<R>> toAsync(Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.an.png">
     *
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     */
    public static FuncN<Observable<Void>> toAsync(ActionN action) {
        return toAsync(action, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.png">
     *
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     */
    public static <R> FuncN<Observable<R>> toAsync(FuncN<? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229439.aspx">MSDN: Observable.ToAsync</a>
     */
    public static Func0<Observable<Void>> toAsync(final Action0 action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211792.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <R> Func0<Observable<R>> toAsync(final Func0<? extends R> func, final Scheduler scheduler) {
        return new Func0<Observable<R>>() {
            @Override
            public Observable<R> call() {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call();
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param action the Action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229822.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1> Func1<T1, Observable<Void>> toAsync(final Action1<? super T1> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229731.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, R> Func1<T1, Observable<R>> toAsync(final Func1<? super T1, ? extends R> func, final Scheduler scheduler) {
        return new Func1<T1, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229722.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2> Func2<T1, T2, Observable<Void>> toAsync(final Action2<? super T1, ? super T2> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229327.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, R> Func2<T1, T2, Observable<R>> toAsync(final Func2<? super T1, ? super T2, ? extends R> func, final Scheduler scheduler) {
        return new Func2<T1, T2, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211787.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3> Func3<T1, T2, T3, Observable<Void>> toAsync(final Action3<? super T1, ? super T2, ? super T3> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229287.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, R> Func3<T1, T2, T3, Observable<R>> toAsync(final Func3<? super T1, ? super T2, ? super T3, ? extends R> func, final Scheduler scheduler) {
        return new Func3<T1, T2, T3, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229370.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4> Func4<T1, T2, T3, T4, Observable<Void>> toAsync(final Action4<? super T1, ? super T2, ? super T3, ? super T4> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229560.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, R> Func4<T1, T2, T3, T4, Observable<R>> toAsync(final Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func, final Scheduler scheduler) {
        return new Func4<T1, T2, T3, T4, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212149.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5> Func5<T1, T2, T3, T4, T5, Observable<Void>> toAsync(final Action5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229606.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, R> Func5<T1, T2, T3, T4, T5, Observable<R>> toAsync(final Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> func, final Scheduler scheduler) {
        return new Func5<T1, T2, T3, T4, T5, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212138.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6> Func6<T1, T2, T3, T4, T5, T6, Observable<Void>> toAsync(final Action6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229630.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, R> Func6<T1, T2, T3, T4, T5, T6, Observable<R>> toAsync(final Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> func, final Scheduler scheduler) {
        return new Func6<T1, T2, T3, T4, T5, T6, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229808.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<Void>> toAsync(final Action7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229794.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> Func7<T1, T2, T3, T4, T5, T6, T7, Observable<R>> toAsync(final Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> func, final Scheduler scheduler) {
        return new Func7<T1, T2, T3, T4, T5, T6, T7, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229361.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<Void>> toAsync(final Action8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh228956.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<R>> toAsync(final Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> func, final Scheduler scheduler) {
        return new Func8<T1, T2, T3, T4, T5, T6, T7, T8, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7, t8);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <T9> the ninth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229662.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<Void>> toAsync(final Action9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9> action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <T9> the ninth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229008.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<R>> toAsync(final Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> func, final Scheduler scheduler) {
        return new Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Observable<R>>() {
            @Override
            public Observable<R> call(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8, final T9 t9) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.ans.png">
     *
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     */
    public static FuncN<Observable<Void>> toAsync(final ActionN action, final Scheduler scheduler) {
        return toAsync(Actions.toFunc(action), scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toAsync.s.png">
     *
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     */
    public static <R> FuncN<Observable<R>> toAsync(final FuncN<? extends R> func, final Scheduler scheduler) {
        return new FuncN<Observable<R>>() {
            @Override
            public Observable<R> call(final Object... args) {
                final AsyncSubject<R> subject = AsyncSubject.create();
                final Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        R result;
                        try {
                            result = func.call(args);
                        } catch (Throwable t) {
                            subject.onError(t);
                            return;
                        } finally {
                            inner.unsubscribe();
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
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/asyncAction.n.png">
     * <p>
     * Alias for toAsync(ActionN) intended for dynamic languages.
     *
     * @param action the action to convert
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: asyncAction()</a>
     */
    public static FuncN<Observable<Void>> asyncAction(final ActionN action) {
        return toAsync(action);
    }
    
    /**
     * Convert a synchronous action call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/asyncAction.ns.png">
     * <p>
     * Alias for toAsync(ActionN, Scheduler) intended for dynamic languages.
     *
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Observable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: asyncAction()</a>
     */
    public static FuncN<Observable<Void>> asyncAction(final ActionN action, final Scheduler scheduler) {
        return toAsync(action, scheduler);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/asyncFunc.png">
     * <p>
     * Alias for toAsync(FuncN) intended for dynamic languages.
     *
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: asyncFunc()</a>
     */
    public static <R> FuncN<Observable<R>> asyncFunc(final FuncN<? extends R> func) {
        return toAsync(func);
    }
    
    /**
     * Convert a synchronous function call into an asynchronous function call through an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/asyncFunc.s.png">
     * <p>
     * Alias for {@code toAsync(FuncN, Scheduler)} intended for dynamic languages.
     *
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Observable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: asyncFunc()</a>
     */
    public static <R> FuncN<Observable<R>> asyncFunc(final FuncN<? extends R> func, final Scheduler scheduler) {
        return toAsync(func, scheduler);
    }
    
    /**
     * Invokes the asynchronous function immediately, surfacing the result through an Observable.
     * <p>
     * <em>Important note</em> subscribing to the resulting Observable blocks until the future completes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/startFuture.png">
     *
     * @param <T> the result type
     * @param functionAsync the asynchronous function to run
     * @return an Observable that surfaces the result of the future
     * @see #startFuture(rx.functions.Func0, rx.Scheduler)
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-startfuture">RxJava Wiki: startFuture()</a>
     */
    public static <T> Observable<T> startFuture(Func0<? extends Future<? extends T>> functionAsync) {
        return OperatorStartFuture.startFuture(functionAsync);
    }
    
    /**
     * Invokes the asynchronous function immediately, surfacing the result through an Observable and waits on
     * the specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/startFuture.s.png">
     *
     * @param <T> the result type
     * @param functionAsync the asynchronous function to run
     * @param scheduler the Scheduler where the completion of the Future is awaited
     * @return an Observable that surfaces the result of the future
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-startfuture">RxJava Wiki: startFuture()</a>
     */
    public static <T> Observable<T> startFuture(Func0<? extends Future<? extends T>> functionAsync,
        Scheduler scheduler) {
        return OperatorStartFuture.startFuture(functionAsync, scheduler);
    }
    
    /**
     * Returns an Observable that starts the specified asynchronous factory function whenever a new observer
     * subscribes.
     * <p>
     * <em>Important note</em> subscribing to the resulting Observable blocks until the future completes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/deferFuture.png">
     *
     * @param <T> the result type
     * @param observableFactoryAsync the asynchronous function to start for each observer
     * @return the Observable emitting items produced by the asynchronous observer produced by the factory
     * @see #deferFuture(rx.functions.Func0, rx.Scheduler)
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-deferfuture">RxJava Wiki: deferFuture()</a>
     */
    public static <T> Observable<T> deferFuture(Func0<? extends Future<? extends Observable<? extends T>>> observableFactoryAsync) {
        return OperatorDeferFuture.deferFuture(observableFactoryAsync);
    }
    
    /**
     * Returns an Observable that starts the specified asynchronous factory function whenever a new observer
     * subscribes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/deferFuture.s.png">
     *
     * @param <T> the result type
     * @param observableFactoryAsync the asynchronous function to start for each observer
     * @param scheduler the Scheduler where the completion of the Future is awaited
     * @return the Observable emitting items produced by the asynchronous observer produced by the factory
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-deferfuture">RxJava Wiki: deferFuture()</a>
     */
    public static <T> Observable<T> deferFuture(
        Func0<? extends Future<? extends Observable<? extends T>>> observableFactoryAsync,
        Scheduler scheduler) {
        return OperatorDeferFuture.deferFuture(observableFactoryAsync, scheduler);
    }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless the {@code run()} method is called
     * or the task is scheduled on an Executor.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/forEachFuture.png">
     *
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(rx.Observable, rx.functions.Action1, rx.Scheduler)
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> FutureTask<Void> forEachFuture(
        Observable<? extends T> source,
        Action1<? super T> onNext) {
        return OperatorForEachFuture.forEachFuture(source, onNext);
    }
    
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless the {@code run()} method is called
     * or the task is scheduled on an Executor.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/forEachFuture.png">
     *
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(rx.Observable, rx.functions.Action1, rx.functions.Action1, rx.Scheduler)
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> FutureTask<Void> forEachFuture(
        Observable<? extends T> source,
        Action1<? super T> onNext,
        Action1<? super Throwable> onError) {
        return OperatorForEachFuture.forEachFuture(source, onNext, onError);
    }
    
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless the {@code run()} method is called
     * or the task is scheduled on an Executor.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/forEachFuture.png">
     *
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param onCompleted the action to call when the source completes
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(rx.Observable, rx.functions.Action1, rx.functions.Action1, rx.functions.Action0, rx.Scheduler)
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> FutureTask<Void> forEachFuture(
        Observable<? extends T> source,
        Action1<? super T> onNext,
        Action1<? super Throwable> onError,
        Action0 onCompleted) {
        return OperatorForEachFuture.forEachFuture(source, onNext, onError, onCompleted);
    }
    
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future, scheduled on the given scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/forEachFuture.s.png">
     *
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param scheduler the Scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> FutureTask<Void> forEachFuture(
            Observable<? extends T> source,
            Action1<? super T> onNext,
            Scheduler scheduler) {
        FutureTask<Void> task = OperatorForEachFuture.forEachFuture(source, onNext);
        final Worker inner = scheduler.createWorker();
        inner.schedule(Functionals.fromRunnable(task, inner));
        return task;
    }
    
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future, scheduled on the given Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/forEachFuture.s.png">
     *
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param scheduler the Scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> FutureTask<Void> forEachFuture(
            Observable<? extends T> source,
            Action1<? super T> onNext,
            Action1<? super Throwable> onError,
            Scheduler scheduler) {
        FutureTask<Void> task = OperatorForEachFuture.forEachFuture(source, onNext, onError);
        final Worker inner = scheduler.createWorker();
        inner.schedule(Functionals.fromRunnable(task, inner));
        return task;
    }
    
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future, scheduled on the given Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/forEachFuture.s.png">
     *
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param onCompleted the action to call when the source completes
     * @param scheduler the Scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> FutureTask<Void> forEachFuture(
            Observable<? extends T> source,
            Action1<? super T> onNext,
            Action1<? super Throwable> onError,
            Action0 onCompleted,
            Scheduler scheduler) {
        FutureTask<Void> task = OperatorForEachFuture.forEachFuture(source, onNext, onError, onCompleted);
        final Worker inner = scheduler.createWorker();
        inner.schedule(Functionals.fromRunnable(task, inner));
        return task;
    }
    
    /**
     * Return an Observable that calls the given action and emits the given result when an Observer subscribes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/fromAction.png">
     * <p>
     * The action is run on the default thread pool for computation.
     *
     * @param <R> the return type
     * @param action the action to invoke on each subscription
     * @param result the result to emit to observers
     * @return an Observable that calls the given action and emits the given result when an Observer subscribes
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-fromaction">RxJava Wiki: fromAction()</a>
     */
    public static <R> Observable<R> fromAction(Action0 action, R result) {
        return fromAction(action, result, Schedulers.computation());
    }
    
    /**
     * Return an Observable that calls the given Callable and emits its result or Exception when an Observer
     * subscribes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/fromCallable.png">
     * <p>
     * The Callable is called on the default thread pool for computation.
     * 
     * @param <R> the return type
     * @param callable the callable to call on each subscription
     * @return an Observable that calls the given Callable and emits its result or Exception when an Observer
     *         subscribes
     * @see #start(rx.functions.Func0) 
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-fromcallable">RxJava Wiki: fromCallable()</a>
     */
    public static <R> Observable<R> fromCallable(Callable<? extends R> callable) {
        return fromCallable(callable, Schedulers.computation());
    }
    
    /**
     * Return an Observable that calls the given Runnable and emits the given result when an Observer
     * subscribes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/fromRunnable.png">
     * <p>
     * The Runnable is called on the default thread pool for computation.
     * 
     * @param <R> the return type
     * @param run the runnable to invoke on each subscription
     * @param result the result to emit to observers
     * @return an Observable that calls the given Runnable and emits the given result when an Observer
     *         subscribes
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-fromrunnable">RxJava Wiki: fromRunnable()</a>
     */
    public static <R> Observable<R> fromRunnable(final Runnable run, final R result) {
        return fromRunnable(run, result, Schedulers.computation());
    }
    
    /**
     * Return an Observable that calls the given action and emits the given result when an Observer subscribes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/fromAction.s.png">
     * 
     * @param <R> the return type
     * @param action the action to invoke on each subscription
     * @param scheduler the Scheduler where the function is called and the result is emitted
     * @param result the result to emit to observers
     * @return an Observable that calls the given action and emits the given result when an Observer subscribes
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-fromaction">RxJava Wiki: fromAction()</a>
     */
    public static <R> Observable<R> fromAction(Action0 action, R result, Scheduler scheduler) {
        return Observable.create(OperatorFromFunctionals.fromAction(action, result)).subscribeOn(scheduler);
    }
    
    /**
     * Return an Observable that calls the given Callable and emits its result or Exception when an Observer
     * subscribes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/fromCallable.s.png">
     * 
     * @param <R> the return type
     * @param callable the callable to call on each subscription
     * @param scheduler the Scheduler where the function is called and the result is emitted
     * @return an Observable that calls the given Callable and emits its result or Exception when an Observer
     *         subscribes
     * @see #start(rx.functions.Func0) 
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-fromcallable">RxJava Wiki: fromCallable()</a>
     */
    public static <R> Observable<R> fromCallable(Callable<? extends R> callable, Scheduler scheduler) {
        return Observable.create(OperatorFromFunctionals.fromCallable(callable)).subscribeOn(scheduler);
    }
    
    /**
     * Return an Observable that calls the given Runnable and emits the given result when an Observer
     * subscribes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/fromRunnable.s.png">
     * 
     * @param <R> the return type
     * @param run the runnable to invoke on each subscription
     * @param scheduler the Scheduler where the function is called and the result is emitted
     * @param result the result to emit to observers
     * @return an Observable that calls the given Runnable and emits the given result when an Observer
     *         subscribes
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-fromrunnable">RxJava Wiki: fromRunnable()</a>
     */
    public static <R> Observable<R> fromRunnable(final Runnable run, final R result, Scheduler scheduler) {
        return Observable.create(OperatorFromFunctionals.fromRunnable(run, result)).subscribeOn(scheduler);
    }

    /**
     * Runs the provided action on the given scheduler and allows propagation of multiple events to the
     * observers of the returned StoppableObservable. The action is immediately executed and unobserved values
     * will be lost.
     *
     * @param <T> the output value type
     * @param scheduler the Scheduler where the action is executed
     * @param action the action to execute, receives an Observer where the events can be pumped and a
     *               Subscription which lets it check for cancellation condition
     * @return an Observable that provides a Subscription interface to cancel the action
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-runasync">RxJava Wiki: runAsync()</a>
     */
    public static <T> StoppableObservable<T> runAsync(Scheduler scheduler, 
            final Action2<? super Observer<? super T>, ? super Subscription> action) {
        return runAsync(scheduler, PublishSubject.<T>create(), action);
    }

    /**
     * Runs the provided action on the given scheduler and allows propagation of multiple events to the
     * observers of the returned StoppableObservable. The action is immediately executed and unobserved values
     * might be lost, depending on the Subject type used.
     *
     * @param <T> the output value of the action
     * @param <U> the output type of the observable sequence
     * @param scheduler the Scheduler where the action is executed
     * @param subject the subject to use to distribute values emitted by the action
     * @param action the action to execute, receives an Observer where the events can be pumped and a
     *               Subscription which lets it check for cancellation condition
     * @return an Observable that provides a Subscription interface to cancel the action
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-runasync">RxJava Wiki: runAsync()</a>
     */
    public static <T, U> StoppableObservable<U> runAsync(Scheduler scheduler,
            final Subject<T, U> subject, 
            final Action2<? super Observer<? super T>, ? super Subscription> action) {
        final SerialSubscription csub = new SerialSubscription();
        
        StoppableObservable<U> co = new StoppableObservable<U>(new Observable.OnSubscribe<U>() {
            @Override
            public void call(Subscriber<? super U> t1) {
                subject.subscribe(t1);
            }
        }, csub);
        
        final Worker inner = scheduler.createWorker();
        csub.set(inner);
        
        inner.schedule(new Action0() {
            @Override
            public void call() {
                if (!csub.isUnsubscribed()) {
                    action.call(subject, csub);
                }
            }
        });
        
        return co;
    }
}
