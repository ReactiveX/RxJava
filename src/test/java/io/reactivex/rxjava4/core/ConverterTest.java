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

package io.reactivex.rxjava4.core;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.parallel.*;

public final class ConverterTest extends RxJavaTest {

    @Test
    public void flowableConverterThrows() {
        try {
            Flowable.just(1).to(_ -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void observableConverterThrows() {
        try {
            Observable.just(1).to(_ -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void singleConverterThrows() {
        try {
            Single.just(1).to(_ -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void maybeConverterThrows() {
        try {
            Maybe.just(1).to(_ -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void completableConverterThrows() {
        try {
            Completable.complete().to(_ -> {
                throw new TestException("Forced failure");
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
        var a = new A<String, Integer>() /* NFI */ { };

        Observable.just(a).to((ObservableConverter)ConverterTest.testObservableConverterCreator());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void singleGenericsSignatureTest() {
        var a = new A<String, Integer>() /* NFI */ { };

        Single.just(a).to((SingleConverter)ConverterTest.<String>testSingleConverterCreator());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void maybeGenericsSignatureTest() {
        var a = new A<String, Integer>() /* NFI */ { };

        Maybe.just(a).to((MaybeConverter)ConverterTest.<String>testMaybeConverterCreator());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void flowableGenericsSignatureTest() {
        var a = new A<String, Integer>() /* NFI */ { };

        Flowable.just(a).to((FlowableConverter)ConverterTest.<String>testFlowableConverterCreator());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void parallelFlowableGenericsSignatureTest() {
        var a = new A<String, Integer>() /* NFI */ { };

        Flowable.just(a).parallel().to((ParallelFlowableConverter)ConverterTest.<String>testParallelFlowableConverterCreator());
    }

    @Test
    public void compositeTest() {
        var converter = new CompositeConverter();

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

    /**
     * Two argument type.
     * @param <T> the input type
     * @param <R> the output type
     */
    interface A<T, R> { }

    /**
     * One argument type.
     * @param <T> the type
     */
    interface B<T> { }

    private static <T> ObservableConverter<A<T, ?>, B<T>> testObservableConverterCreator() {
        return _ -> new B<>() /* NFI */ {
        };
    }

    private static <T> SingleConverter<A<T, ?>, B<T>> testSingleConverterCreator() {
        return _ -> new B<>() /* NFI */ {
        };
    }

    private static <T> MaybeConverter<A<T, ?>, B<T>> testMaybeConverterCreator() {
        return _ -> new B<>() /* NFI */ {
        };
    }

    private static <T> FlowableConverter<A<T, ?>, B<T>> testFlowableConverterCreator() {
        return _ -> new B<>() /* NFI */ {
        };
    }

    private static <T> ParallelFlowableConverter<A<T, ?>, B<T>> testParallelFlowableConverterCreator() {
        return _ -> new B<>() /* NFI */ {
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
