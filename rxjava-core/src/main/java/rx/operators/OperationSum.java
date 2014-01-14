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

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * A few operators for implementing the sum operation.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.sum%28v=vs.103%29.aspx">MSDN: Observable.Sum</a>
 */
public final class OperationSum {
    public static Observable<Integer> sum(Observable<Integer> source) {
        return source.reduce(0, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer accu, Integer next) {
                return accu + next;
            }
        });
    }

    public static Observable<Long> sumLongs(Observable<Long> source) {
        return source.reduce(0L, new Func2<Long, Long, Long>() {
            @Override
            public Long call(Long accu, Long next) {
                return accu + next;
            }
        });
    }

    public static Observable<Float> sumFloats(Observable<Float> source) {
        return source.reduce(0.0f, new Func2<Float, Float, Float>() {
            @Override
            public Float call(Float accu, Float next) {
                return accu + next;
            }
        });
    }

    public static Observable<Double> sumDoubles(Observable<Double> source) {
        return source.reduce(0.0d, new Func2<Double, Double, Double>() {
            @Override
            public Double call(Double accu, Double next) {
                return accu + next;
            }
        });
    }

    /**
     * Compute the sum by extracting integer values from the source via an
     * extractor function.
     * 
     * @param <T>
     *            the source value type
     */
    public static final class SumIntegerExtractor<T> implements Observable.OnSubscribeFunc<Integer> {
        final Observable<? extends T> source;
        final Func1<? super T, Integer> valueExtractor;

        public SumIntegerExtractor(Observable<? extends T> source, Func1<? super T, Integer> valueExtractor) {
            this.source = source;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Integer> t1) {
            return source.subscribe(new SumObserver(t1));
        }

        /** Computes the average. */
        private final class SumObserver implements Observer<T> {
            final Observer<? super Integer> observer;
            int sum;
            boolean hasValue;

            public SumObserver(Observer<? super Integer> observer) {
                this.observer = observer;
            }

            @Override
            public void onNext(T args) {
                sum += valueExtractor.call(args);
                hasValue = true;
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                if (hasValue) {
                    try {
                        observer.onNext(sum);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return;
                    }
                    observer.onCompleted();
                } else {
                    observer.onError(new IllegalArgumentException("Sequence contains no elements"));
                }
            }

        }
    }

    /**
     * Compute the sum by extracting long values from the source via an
     * extractor function.
     * 
     * @param <T>
     *            the source value type
     */
    public static final class SumLongExtractor<T> implements Observable.OnSubscribeFunc<Long> {
        final Observable<? extends T> source;
        final Func1<? super T, Long> valueExtractor;

        public SumLongExtractor(Observable<? extends T> source, Func1<? super T, Long> valueExtractor) {
            this.source = source;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Long> t1) {
            return source.subscribe(new SumObserver(t1));
        }

        /** Computes the average. */
        private final class SumObserver implements Observer<T> {
            final Observer<? super Long> observer;
            long sum;
            boolean hasValue;

            public SumObserver(Observer<? super Long> observer) {
                this.observer = observer;
            }

            @Override
            public void onNext(T args) {
                sum += valueExtractor.call(args);
                hasValue = true;
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                if (hasValue) {
                    try {
                        observer.onNext(sum);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return;
                    }
                    observer.onCompleted();
                } else {
                    observer.onError(new IllegalArgumentException("Sequence contains no elements"));
                }
            }

        }
    }

    /**
     * Compute the sum by extracting float values from the source via an
     * extractor function.
     * 
     * @param <T>
     *            the source value type
     */
    public static final class SumFloatExtractor<T> implements Observable.OnSubscribeFunc<Float> {
        final Observable<? extends T> source;
        final Func1<? super T, Float> valueExtractor;

        public SumFloatExtractor(Observable<? extends T> source, Func1<? super T, Float> valueExtractor) {
            this.source = source;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Float> t1) {
            return source.subscribe(new SumObserver(t1));
        }

        /** Computes the average. */
        private final class SumObserver implements Observer<T> {
            final Observer<? super Float> observer;
            float sum;
            boolean hasValue;

            public SumObserver(Observer<? super Float> observer) {
                this.observer = observer;
            }

            @Override
            public void onNext(T args) {
                sum += valueExtractor.call(args);
                hasValue = true;
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                if (hasValue) {
                    try {
                        observer.onNext(sum);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return;
                    }
                    observer.onCompleted();
                } else {
                    observer.onError(new IllegalArgumentException("Sequence contains no elements"));
                }
            }

        }
    }

    /**
     * Compute the sum by extracting float values from the source via an
     * extractor function.
     * 
     * @param <T>
     *            the source value type
     */
    public static final class SumDoubleExtractor<T> implements Observable.OnSubscribeFunc<Double> {
        final Observable<? extends T> source;
        final Func1<? super T, Double> valueExtractor;

        public SumDoubleExtractor(Observable<? extends T> source, Func1<? super T, Double> valueExtractor) {
            this.source = source;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Double> t1) {
            return source.subscribe(new SumObserver(t1));
        }

        /** Computes the average. */
        private final class SumObserver implements Observer<T> {
            final Observer<? super Double> observer;
            double sum;
            boolean hasValue;

            public SumObserver(Observer<? super Double> observer) {
                this.observer = observer;
            }

            @Override
            public void onNext(T args) {
                sum += valueExtractor.call(args);
                hasValue = true;
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                if (hasValue) {
                    try {
                        observer.onNext(sum);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return;
                    }
                    observer.onCompleted();
                } else {
                    observer.onError(new IllegalArgumentException("Sequence contains no elements"));
                }
            }

        }
    }

}
