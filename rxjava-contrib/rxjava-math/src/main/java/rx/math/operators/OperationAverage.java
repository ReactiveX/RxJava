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
package rx.math.operators;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * A few operators for implementing the averaging operation.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.average%28v=vs.103%29.aspx">MSDN: Observable.Average</a>
 */
public final class OperationAverage {
    private static final class Tuple2<T> {
        private final T current;
        private final Integer count;

        private Tuple2(T v1, Integer v2) {
            current = v1;
            count = v2;
        }
    }

    public static Observable<Integer> average(Observable<Integer> source) {
        return source.reduce(new Tuple2<Integer>(0, 0), new Func2<Tuple2<Integer>, Integer, Tuple2<Integer>>() {
            @Override
            public Tuple2<Integer> call(Tuple2<Integer> accu, Integer next) {
                return new Tuple2<Integer>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Integer>, Integer>() {
            @Override
            public Integer call(Tuple2<Integer> result) {
                if (result.count == 0) {
                    throw new IllegalArgumentException("Sequence contains no elements");
                }
                return result.current / result.count;
            }
        });
    }

    public static Observable<Long> averageLongs(Observable<Long> source) {
        return source.reduce(new Tuple2<Long>(0L, 0), new Func2<Tuple2<Long>, Long, Tuple2<Long>>() {
            @Override
            public Tuple2<Long> call(Tuple2<Long> accu, Long next) {
                return new Tuple2<Long>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Long>, Long>() {
            @Override
            public Long call(Tuple2<Long> result) {
                if (result.count == 0) {
                    throw new IllegalArgumentException("Sequence contains no elements");
                }
                return result.current / result.count;
            }
        });
    }

    public static Observable<Float> averageFloats(Observable<Float> source) {
        return source.reduce(new Tuple2<Float>(0.0f, 0), new Func2<Tuple2<Float>, Float, Tuple2<Float>>() {
            @Override
            public Tuple2<Float> call(Tuple2<Float> accu, Float next) {
                return new Tuple2<Float>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Float>, Float>() {
            @Override
            public Float call(Tuple2<Float> result) {
                if (result.count == 0) {
                    throw new IllegalArgumentException("Sequence contains no elements");
                }
                return result.current / result.count;
            }
        });
    }

    public static Observable<Double> averageDoubles(Observable<Double> source) {
        return source.reduce(new Tuple2<Double>(0.0d, 0), new Func2<Tuple2<Double>, Double, Tuple2<Double>>() {
            @Override
            public Tuple2<Double> call(Tuple2<Double> accu, Double next) {
                return new Tuple2<Double>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Double>, Double>() {
            @Override
            public Double call(Tuple2<Double> result) {
                if (result.count == 0) {
                    throw new IllegalArgumentException("Sequence contains no elements");
                }
                return result.current / result.count;
            }
        });
    }

    /**
     * Compute the average by extracting integer values from the source via an
     * extractor function.
     * 
     * @param <T>
     *            the source value type
     */
    public static final class AverageIntegerExtractor<T> implements OnSubscribeFunc<Integer> {
        final Observable<? extends T> source;
        final Func1<? super T, Integer> valueExtractor;

        public AverageIntegerExtractor(Observable<? extends T> source, Func1<? super T, Integer> valueExtractor) {
            this.source = source;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Integer> t1) {
            return source.subscribe(new AverageObserver(t1));
        }

        /** Computes the average. */
        private final class AverageObserver implements Observer<T> {
            final Observer<? super Integer> observer;
            int sum;
            int count;

            public AverageObserver(Observer<? super Integer> observer) {
                this.observer = observer;
            }

            @Override
            public void onNext(T args) {
                sum += valueExtractor.call(args);
                count++;
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                if (count > 0) {
                    try {
                        observer.onNext(sum / count);
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
     * Compute the average by extracting long values from the source via an
     * extractor function.
     * 
     * @param <T>
     *            the source value type
     */
    public static final class AverageLongExtractor<T> implements OnSubscribeFunc<Long> {
        final Observable<? extends T> source;
        final Func1<? super T, Long> valueExtractor;

        public AverageLongExtractor(Observable<? extends T> source, Func1<? super T, Long> valueExtractor) {
            this.source = source;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Long> t1) {
            return source.subscribe(new AverageObserver(t1));
        }

        /** Computes the average. */
        private final class AverageObserver implements Observer<T> {
            final Observer<? super Long> observer;
            long sum;
            int count;

            public AverageObserver(Observer<? super Long> observer) {
                this.observer = observer;
            }

            @Override
            public void onNext(T args) {
                sum += valueExtractor.call(args);
                count++;
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                if (count > 0) {
                    try {
                        observer.onNext(sum / count);
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
     * Compute the average by extracting float values from the source via an
     * extractor function.
     * 
     * @param <T>
     *            the source value type
     */
    public static final class AverageFloatExtractor<T> implements OnSubscribeFunc<Float> {
        final Observable<? extends T> source;
        final Func1<? super T, Float> valueExtractor;

        public AverageFloatExtractor(Observable<? extends T> source, Func1<? super T, Float> valueExtractor) {
            this.source = source;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Float> t1) {
            return source.subscribe(new AverageObserver(t1));
        }

        /** Computes the average. */
        private final class AverageObserver implements Observer<T> {
            final Observer<? super Float> observer;
            float sum;
            int count;

            public AverageObserver(Observer<? super Float> observer) {
                this.observer = observer;
            }

            @Override
            public void onNext(T args) {
                sum += valueExtractor.call(args);
                count++;
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                if (count > 0) {
                    try {
                        observer.onNext(sum / count);
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
     * Compute the average by extracting double values from the source via an
     * extractor function.
     * 
     * @param <T>
     *            the source value type
     */
    public static final class AverageDoubleExtractor<T> implements OnSubscribeFunc<Double> {
        final Observable<? extends T> source;
        final Func1<? super T, Double> valueExtractor;

        public AverageDoubleExtractor(Observable<? extends T> source, Func1<? super T, Double> valueExtractor) {
            this.source = source;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Double> t1) {
            return source.subscribe(new AverageObserver(t1));
        }

        /** Computes the average. */
        private final class AverageObserver implements Observer<T> {
            final Observer<? super Double> observer;
            double sum;
            int count;

            public AverageObserver(Observer<? super Double> observer) {
                this.observer = observer;
            }

            @Override
            public void onNext(T args) {
                sum += valueExtractor.call(args);
                count++;
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                if (count > 0) {
                    try {
                        observer.onNext(sum / count);
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
