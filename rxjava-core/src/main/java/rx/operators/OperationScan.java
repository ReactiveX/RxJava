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
import rx.util.functions.Func2;

/**
 * Returns an Observable that applies a function to the first item emitted by a source Observable,
 * then feeds the result of that function along with the second item emitted by an Observable into
 * the same function, and so on until all items have been emitted by the source Observable,
 * emitting the result of each of these iterations.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/scan.png">
 * <p>
 * This sort of function is sometimes called an accumulator.
 * <p>
 * Note that when you pass a seed to <code>scan()</code> the resulting Observable will emit that
 * seed as its first emitted item.
 */
public final class OperationScan {
    /**
     * Applies an accumulator function over an observable sequence and returns each intermediate
     * result with the specified source and accumulator.
     * 
     * @param sequence
     *            An observable sequence of elements to project.
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator function to be invoked on each element from the sequence.
     * 
     * @return An observable sequence whose elements are the result of accumulating the output from
     *         the list of Observables.
     * @see <a
     *      href="http://msdn.microsoft.com/en-us/library/hh212007%28v=vs.103%29.aspx">Observable.Scan(TSource,
     *      TAccumulate) Method (IObservable(TSource), TAccumulate, Func(TAccumulate, TSource,
     *      TAccumulate))</a>
     */
    public static <T, R> Operator<R, T> scan(final R initialValue, final Func2<R, ? super T, R> accumulator) {
        return new Operator<R, T>() {
            @Override
            public Subscriber<T> call(final Subscriber<? super R> observer) {
                observer.onNext(initialValue);
                return new Subscriber<T>(observer) {
                    private R value = initialValue;

                    @Override
                    public void onNext(T value) {
                        try {
                            this.value = accumulator.call(this.value, value);
                        } catch (Throwable e) {
                            observer.onError(e);
                            observer.unsubscribe();
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
        };
    }

    /**
     * Applies an accumulator function over an observable sequence and returns each intermediate
     * result with the specified source and accumulator.
     * 
     * @param sequence
     *            An observable sequence of elements to project.
     * @param accumulator
     *            An accumulator function to be invoked on each element from the sequence.
     * 
     * @return An observable sequence whose elements are the result of accumulating the output from
     *         the list of Observables.
     * @see <a
     *      href="http://msdn.microsoft.com/en-us/library/hh211665(v=vs.103).aspx">Observable.Scan(TSource)
     *      Method (IObservable(TSource), Func(TSource, TSource, TSource))</a>
     */
    public static <T> Operator<T, T> scan(final Func2<T, T, T> accumulator) {
        return new Operator<T, T>() {
            @Override
            public Subscriber<T> call(final Subscriber<? super T> observer) {
                return new Subscriber<T>(observer) {
                    private boolean first = true;
                    private T value;

                    @Override
                    public void onNext(T value) {
                        if (first) {
                            this.value = value;
                            first = false;
                        }
                        else {
                            try {
                                this.value = accumulator.call(this.value, value);
                            } catch (Throwable e) {
                                observer.onError(e);
                                observer.unsubscribe();
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
        };
    }
}
