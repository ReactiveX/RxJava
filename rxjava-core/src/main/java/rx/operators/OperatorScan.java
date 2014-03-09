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
package rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func2;

/**
 * Returns an Observable that applies a function to the first item emitted by a source Observable, then feeds
 * the result of that function along with the second item emitted by an Observable into the same function, and
 * so on until all items have been emitted by the source Observable, emitting the result of each of these
 * iterations.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/scan.png">
 * <p>
 * This sort of function is sometimes called an accumulator.
 * <p>
 * Note that when you pass a seed to <code>scan()</code> the resulting Observable will emit that seed as its
 * first emitted item.
 */
public final class OperatorScan<R, T> implements Operator<R, T> {

    private final R initialValue;
    private final Func2<R, ? super T, R> accumulator;
    // sentinel if we don't receive an initial value
    private static final Object NO_INITIAL_VALUE = new Object();

    /**
     * Applies an accumulator function over an observable sequence and returns each intermediate result with the
     * specified source and accumulator.
     * 
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each element from the sequence
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212007.aspx">Observable.Scan(TSource, TAccumulate) Method (IObservable(TSource), TAccumulate, Func(TAccumulate, TSource,
     *      TAccumulate))</a>
     */
    public OperatorScan(R initialValue, Func2<R, ? super T, R> accumulator) {
        this.initialValue = initialValue;
        this.accumulator = accumulator;
    }

    /**
     * Applies an accumulator function over an observable sequence and returns each intermediate result with the
     * specified source and accumulator.
     * 
     * @param accumulator
     *            an accumulator function to be invoked on each element from the sequence
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665.aspx">Observable.Scan(TSource) Method (IObservable(TSource), Func(TSource, TSource, TSource))</a>
     */
    @SuppressWarnings("unchecked")
    public OperatorScan(final Func2<R, ? super T, R> accumulator) {
        this((R) NO_INITIAL_VALUE, accumulator);
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> observer) {
        if (initialValue != NO_INITIAL_VALUE) {
            observer.onNext(initialValue);
        }
        return new Subscriber<T>(observer) {
            private R value = initialValue;

            @SuppressWarnings("unchecked")
            @Override
            public void onNext(T value) {
                if (this.value == NO_INITIAL_VALUE) {
                    // if there is NO_INITIAL_VALUE then we know it is type T for both so cast T to R
                    this.value = (R) value;
                } else {
                    try {
                        this.value = accumulator.call(this.value, value);
                    } catch (Throwable e) {
                        observer.onError(OnErrorThrowable.addValueAsLastCause(e, value));
                    }
                }
                observer.onNext(this.value);
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }
        };
    }
}
