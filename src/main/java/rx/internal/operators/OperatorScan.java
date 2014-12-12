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
package rx.internal.operators;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.internal.util.UtilityFunctions;

/**
 * Returns an Observable that applies a function to the first item emitted by a source Observable, then feeds
 * the result of that function along with the second item emitted by an Observable into the same function, and
 * so on until all items have been emitted by the source Observable, emitting the result of each of these
 * iterations.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/scan.png" alt="">
 * <p>
 * This sort of function is sometimes called an accumulator.
 * <p>
 * Note that when you pass a seed to {@code scan} the resulting Observable will emit that seed as its
 * first emitted item.
 */
public final class OperatorScan<R, T> implements Operator<R, T> {

    private final Func0<R> initialValueFactory;
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
    public OperatorScan(final R initialValue, Func2<R, ? super T, R> accumulator) {
        this(new Func0<R>() {

            @Override
            public R call() {
                return initialValue;
            }
            
        }, accumulator);
    }
    
    public OperatorScan(Func0<R> initialValueFactory, Func2<R, ? super T, R> accumulator) {
        this.initialValueFactory = initialValueFactory;
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
    public Subscriber<? super T> call(final Subscriber<? super R> child) {
        return new Subscriber<T>(child) {
            private final R initialValue = initialValueFactory.call();
            private R value = initialValue;
            boolean initialized = false;

            @SuppressWarnings("unchecked")
            @Override
            public void onNext(T currentValue) {
                emitInitialValueIfNeeded(child);

                if (this.value == NO_INITIAL_VALUE) {
                    // if there is NO_INITIAL_VALUE then we know it is type T for both so cast T to R
                    this.value = (R) currentValue;
                } else {
                    try {
                        this.value = accumulator.call(this.value, currentValue);
                    } catch (Throwable e) {
                        child.onError(OnErrorThrowable.addValueAsLastCause(e, currentValue));
                    }
                }
                child.onNext(this.value);
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                emitInitialValueIfNeeded(child);
                child.onCompleted();
            }
            
            private void emitInitialValueIfNeeded(final Subscriber<? super R> child) {
                if (!initialized) {
                    initialized = true;
                    // we emit first time through if we have an initial value
                    if (initialValue != NO_INITIAL_VALUE) {
                        child.onNext(initialValue);
                    }
                }
            }
            
            /**
             * We want to adjust the requested value by subtracting 1 if we have an initial value
             */
            @Override
            public void setProducer(final Producer producer) {
                child.setProducer(new Producer() {

                    final AtomicBoolean once = new AtomicBoolean();

                    final AtomicBoolean excessive = new AtomicBoolean();

                    @Override
                    public void request(long n) {
                        if (once.compareAndSet(false, true)) {
                            if (initialValue == NO_INITIAL_VALUE || n == Long.MAX_VALUE) {
                                producer.request(n);
                            } else if (n == 1) {
                                excessive.set(true);
                                producer.request(1); // request at least 1
                            } else {
                                // n != Long.MAX_VALUE && n != 1
                                producer.request(n - 1);
                            }
                        } else {
                            // pass-thru after first time
                            if (n > 1 // avoid to request 0
                                    && excessive.compareAndSet(true, false) && n != Long.MAX_VALUE) {
                                producer.request(n - 1);
                            } else {
                                producer.request(n);
                            }
                        }
                    }
                });
            }
        };
    }
}
