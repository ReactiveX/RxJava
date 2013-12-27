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

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;

/**
 * Aggregate overloads with index and selector functions.
 */
public final class OperationAggregate {
    /** Utility class. */
    private OperationAggregate() { throw new IllegalStateException("No instances!"); }
    
    /**
     * Aggregate and emit a value after running it through a selector.
     * @param <T> the input value type
     * @param <U> the intermediate value type
     * @param <V> the result value type
     */
    public static final class AggregateSelector<T, U, V> implements OnSubscribeFunc<V> {
        final Observable<? extends T> source;
        final U seed;
        final Func2<U, ? super T, U> aggregator;
        final Func1<? super U, ? extends V> resultSelector;

        public AggregateSelector(
                Observable<? extends T> source, U seed, 
                Func2<U, ? super T, U> aggregator, 
                Func1<? super U, ? extends V> resultSelector) {
            this.source = source;
            this.seed = seed;
            this.aggregator = aggregator;
            this.resultSelector = resultSelector;
        }

        @Override
        public Subscription onSubscribe(Observer<? super V> t1) {
            return source.subscribe(new AggregatorObserver(t1, seed));
        }
        /** The aggregator observer of the source. */
        private final class AggregatorObserver implements Observer<T> {
            final Observer<? super V> observer;
            U accumulator;
            public AggregatorObserver(Observer<? super V> observer, U seed) {
                this.observer = observer;
                this.accumulator = seed;
            }

            @Override
            public void onNext(T args) {
                accumulator = aggregator.call(accumulator, args);
            }

            @Override
            public void onError(Throwable e) {
                accumulator = null;
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                U a = accumulator;
                accumulator = null;
                try {
                    observer.onNext(resultSelector.call(a));
                } catch (Throwable t) {
                    observer.onError(t);
                    return;
                }
                observer.onCompleted();
            }
        }
    }
    /**
     * Indexed aggregate and emit a value after running it through an indexed selector.
     * @param <T> the input value type
     * @param <U> the intermediate value type
     * @param <V> the result value type
     */
    public static final class AggregateIndexedSelector<T, U, V> implements OnSubscribeFunc<V> {
        final Observable<? extends T> source;
        final U seed;
        final Func3<U, ? super T, ? super Integer, U> aggregator;
        final Func2<? super U, ? super Integer, ? extends V> resultSelector;

        public AggregateIndexedSelector(
                Observable<? extends T> source, 
                U seed, 
                Func3<U, ? super T, ? super Integer, U> aggregator, 
                Func2<? super U, ? super Integer, ? extends V> resultSelector) {
            this.source = source;
            this.seed = seed;
            this.aggregator = aggregator;
            this.resultSelector = resultSelector;
        }

        
        
        @Override
        public Subscription onSubscribe(Observer<? super V> t1) {
            return source.subscribe(new AggregatorObserver(t1, seed));
        }
        /** The aggregator observer of the source. */
        private final class AggregatorObserver implements Observer<T> {
            final Observer<? super V> observer;
            U accumulator;
            int index;
            public AggregatorObserver(Observer<? super V> observer, U seed) {
                this.observer = observer;
                this.accumulator = seed;
            }

            @Override
            public void onNext(T args) {
                accumulator = aggregator.call(accumulator, args, index++);
            }

            @Override
            public void onError(Throwable e) {
                accumulator = null;
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                U a = accumulator;
                accumulator = null;
                try {
                    observer.onNext(resultSelector.call(a, index));
                } catch (Throwable t) {
                    observer.onError(t);
                    return;
                }
                observer.onCompleted();
            }
        }
    }
}
