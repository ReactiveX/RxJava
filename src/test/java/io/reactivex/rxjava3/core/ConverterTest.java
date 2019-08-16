/**
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

package io.reactivex.rxjava3.core;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.parallel.*;

public final class ConverterTest extends RxJavaTest {

    @Test
    public void flowableConverterThrows() {
        try {
            Flowable.just(1).to(new FlowableConverter<Integer, Integer>() {
                @Override
                public Integer apply(Flowable<Integer> v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void observableConverterThrows() {
        try {
            Observable.just(1).to(new ObservableConverter<Integer, Integer>() {
                @Override
                public Integer apply(Observable<Integer> v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void singleConverterThrows() {
        try {
            Single.just(1).to(new SingleConverter<Integer, Integer>() {
                @Override
                public Integer apply(Single<Integer> v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void maybeConverterThrows() {
        try {
            Maybe.just(1).to(new MaybeConverter<Integer, Integer>() {
                @Override
                public Integer apply(Maybe<Integer> v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void completableConverterThrows() {
        try {
            Completable.complete().to(new CompletableConverter<Completable>() {
                @Override
                public Completable apply(Completable v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    // Test demos for signature generics in compose() methods. Just needs to compile.

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void observableGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Observable.just(a).to((ObservableConverter)ConverterTest.testObservableConverterCreator());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void singleGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Single.just(a).to((SingleConverter)ConverterTest.<String>testSingleConverterCreator());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void maybeGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Maybe.just(a).to((MaybeConverter)ConverterTest.<String>testMaybeConverterCreator());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void flowableGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Flowable.just(a).to((FlowableConverter)ConverterTest.<String>testFlowableConverterCreator());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void parallelFlowableGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Flowable.just(a).parallel().to((ParallelFlowableConverter)ConverterTest.<String>testParallelFlowableConverterCreator());
    }

    @Test
    public void compositeTest() {
        CompositeConverter converter = new CompositeConverter();

        Flowable.just(1)
                .to(converter)
                .test()
                .assertValue(1);

        Observable.just(1)
                .to(converter)
                .test()
                .assertValue(1);

        Maybe.just(1)
                .to(converter)
                .test()
                .assertValue(1);

        Single.just(1)
                .to(converter)
                .test()
                .assertValue(1);

        Completable.complete()
                .to(converter)
                .test()
                .assertComplete();

        Flowable.just(1)
        .parallel()
        .to(converter)
        .test()
        .assertValue(1);
    }

    interface A<T, R> { }
    interface B<T> { }

    private static <T> ObservableConverter<A<T, ?>, B<T>> testObservableConverterCreator() {
        return new ObservableConverter<A<T, ?>, B<T>>() {
            @Override
            public B<T> apply(Observable<A<T, ?>> a) {
                return new B<T>() {
                };
            }
        };
    }

    private static <T> SingleConverter<A<T, ?>, B<T>> testSingleConverterCreator() {
        return new SingleConverter<A<T, ?>, B<T>>() {
            @Override
            public B<T> apply(Single<A<T, ?>> a) {
                return new B<T>() {
                };
            }
        };
    }

    private static <T> MaybeConverter<A<T, ?>, B<T>> testMaybeConverterCreator() {
        return new MaybeConverter<A<T, ?>, B<T>>() {
            @Override
            public B<T> apply(Maybe<A<T, ?>> a) {
                return new B<T>() {
                };
            }
        };
    }

    private static <T> FlowableConverter<A<T, ?>, B<T>> testFlowableConverterCreator() {
        return new FlowableConverter<A<T, ?>, B<T>>() {
            @Override
            public B<T> apply(Flowable<A<T, ?>> a) {
                return new B<T>() {
                };
            }
        };
    }

    private static <T> ParallelFlowableConverter<A<T, ?>, B<T>> testParallelFlowableConverterCreator() {
        return new ParallelFlowableConverter<A<T, ?>, B<T>>() {
            @Override
            public B<T> apply(ParallelFlowable<A<T, ?>> a) {
                return new B<T>() {
                };
            }
        };
    }

    static class CompositeConverter
    implements ObservableConverter<Integer, Flowable<Integer>>,
            ParallelFlowableConverter<Integer, Flowable<Integer>>,
            FlowableConverter<Integer, Observable<Integer>>,
            MaybeConverter<Integer, Flowable<Integer>>,
            SingleConverter<Integer, Flowable<Integer>>,
            CompletableConverter<Flowable<Integer>> {
        @Override
        public Flowable<Integer> apply(ParallelFlowable<Integer> upstream) {
            return upstream.sequential();
        }

        @Override
        public Flowable<Integer> apply(Completable upstream) {
            return upstream.toFlowable();
        }

        @Override
        public Observable<Integer> apply(Flowable<Integer> upstream) {
            return upstream.toObservable();
        }

        @Override
        public Flowable<Integer> apply(Maybe<Integer> upstream) {
            return upstream.toFlowable();
        }

        @Override
        public Flowable<Integer> apply(Observable<Integer> upstream) {
            return upstream.toFlowable(BackpressureStrategy.MISSING);
        }

        @Override
        public Flowable<Integer> apply(Single<Integer> upstream) {
            return upstream.toFlowable();
        }
    }
}
