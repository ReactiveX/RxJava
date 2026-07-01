/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava4.internal.jdk8;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.Observer;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.processors.*;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class ObservableCollectWithCollectorTest extends RxJavaTest {

    @Test
    public void basic() {
        Observable.range(1, 5)
        .collect(Collectors.toList())
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void empty() {
        Observable.empty()
        .collect(Collectors.toList())
        .test()
        .assertResult(Collections.emptyList());
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .collect(Collectors.toList())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorSupplierCrash() {
        Observable.range(1, 5)
        .collect(new Collector<Integer, Integer, Integer>() /* NFI */ {

            @Override
            public Supplier<Integer> supplier() {
                throw new TestException();
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (_, _) -> { };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return Integer::sum;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return a -> a;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorAccumulatorCrash() {
        BehaviorProcessor<Integer> source = BehaviorProcessor.createDefault(1);

        source
        .collect(new Collector<Integer, Integer, Integer>() /* NFI */ {

            @Override
            public Supplier<Integer> supplier() {
                return () -> 1;
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (_, _) -> { throw new TestException(); };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return Integer::sum;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return a -> a;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void collectorFinisherCrash() {
        Observable.range(1, 5)
        .collect(new Collector<Integer, Integer, Integer>() /* NFI */ {

            @Override
            public Supplier<Integer> supplier() {
                return () -> 1;
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (_, _) -> {  };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return Integer::sum;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return _ -> { throw new TestException(); };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorAccumulatorDropSignals() throws Throwable {
        withErrorTracking(errors -> {
            var source = new Observable<Integer>() /* NFI */ {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new IOException());
                    observer.onComplete();
                }
            };

            source
            .collect(new Collector<Integer, Integer, Integer>() /* NFI */ {

                @Override
                public Supplier<Integer> supplier() {
                    return () -> 1;
                }

                @Override
                public BiConsumer<Integer, Integer> accumulator() {
                    return (_, _) -> { throw new TestException(); };
                }

                @Override
                public BinaryOperator<Integer> combiner() {
                    return Integer::sum;
                }

                @Override
                public Function<Integer, Integer> finisher() {
                    return a -> a;
                }

                @Override
                public Set<Characteristics> characteristics() {
                    return Collections.emptySet();
                }
            })
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        });
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create()
                .collect(Collectors.toList()));
    }

    @Test
    public void onSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservableToSingle(f -> f.collect(Collectors.toList()));
    }

    @Test
    public void basicToObservable() {
        Observable.range(1, 5)
        .collect(Collectors.toList())
        .toObservable()
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void emptyToObservable() {
        Observable.empty()
        .collect(Collectors.toList())
        .toObservable()
        .test()
        .assertResult(Collections.emptyList());
    }

    @Test
    public void errorToObservable() {
        Observable.error(new TestException())
        .collect(Collectors.toList())
        .toObservable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorSupplierCrashToObservable() {
        Observable.range(1, 5)
        .collect(new Collector<Integer, Integer, Integer>() /* NFI */ {

            @Override
            public Supplier<Integer> supplier() {
                throw new TestException();
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (_, _) -> { };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return Integer::sum;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return a -> a;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .toObservable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorAccumulatorCrashToObservable() {
        BehaviorProcessor<Integer> source = BehaviorProcessor.createDefault(1);

        source
        .collect(new Collector<Integer, Integer, Integer>() /* NFI */ {

            @Override
            public Supplier<Integer> supplier() {
                return () -> 1;
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (_, _) -> { throw new TestException(); };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return Integer::sum;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return a -> a;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .toObservable()
        .test()
        .assertFailure(TestException.class);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void collectorFinisherCrashToObservable() {
        Observable.range(1, 5)
        .collect(new Collector<Integer, Integer, Integer>() /* NFI */ {

            @Override
            public Supplier<Integer> supplier() {
                return () -> 1;
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (_, _) -> {  };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return Integer::sum;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return _ -> { throw new TestException(); };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .toObservable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorAccumulatorDropSignalsToObservable() throws Throwable {
        withErrorTracking(errors -> {
            var source = new Observable<Integer>() /* NFI */ {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new IOException());
                    observer.onComplete();
                }
            };

            source
            .collect(new Collector<Integer, Integer, Integer>() /* NFI */ {

                @Override
                public Supplier<Integer> supplier() {
                    return () -> 1;
                }

                @Override
                public BiConsumer<Integer, Integer> accumulator() {
                    return (_, _) -> { throw new TestException(); };
                }

                @Override
                public BinaryOperator<Integer> combiner() {
                    return Integer::sum;
                }

                @Override
                public Function<Integer, Integer> finisher() {
                    return a -> a;
                }

                @Override
                public Set<Characteristics> characteristics() {
                    return Collections.emptySet();
                }
            })
            .toObservable()
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        });
    }

    @Test
    public void disposeToObservable() {
        TestHelper.checkDisposed(PublishProcessor.create()
                .collect(Collectors.toList()).toObservable());
    }

    @Test
    public void onSubscribeToObservable() {
        TestHelper.checkDoubleOnSubscribeObservable(f -> f.collect(Collectors.toList()).toObservable());
    }

    @Test
    public void toObservableTake() {
        Observable.range(1, 5)
        .collect(Collectors.toList())
        .toObservable()
        .take(1)
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void disposeBeforeEnd() {
        TestObserver<List<Integer>> to = Observable.range(1, 5).concatWith(Observable.never())
        .collect(Collectors.toList())
        .test();

        to.dispose();

        to.assertEmpty();
    }
}
