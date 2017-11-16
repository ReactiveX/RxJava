package io.reactivex;

import io.reactivex.exceptions.TestException;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ConverterTest {

    @Test
    public void flowableConverterThrows() {
        try {
            Flowable.just(1).as(new FlowableConverter<Integer, Integer>() {
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
            Observable.just(1).as(new ObservableConverter<Integer, Integer>() {
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
            Single.just(1).as(new SingleConverter<Integer, Integer>() {
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
            Maybe.just(1).as(new MaybeConverter<Integer, Integer>() {
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
            Completable.complete().as(new CompletableConverter() {
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

    @Test
    public void observableGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Observable.just(a).as(ConverterTest.<Integer>testObservableConverterCreator());
    }

    @Test
    public void singleGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Single.just(a).as(ConverterTest.<Integer>testSingleConverterCreator());
    }

    @Test
    public void maybeGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Maybe.just(a).as(ConverterTest.<Integer>testMaybeConverterCreator());
    }

    @Test
    public void flowableGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Flowable.just(a).as(ConverterTest.<Integer>testFlowableConverterCreator());
    }

    @Test
    public void compositeTest() {
        CompositeConverter converter = new CompositeConverter();

        Flowable.just(1)
                .as(converter)
                .test()
                .assertValue(1);

        Observable.just(1)
                .as(converter)
                .test()
                .assertValue(1);

        Maybe.just(1)
                .as(converter)
                .test()
                .assertValue(1);

        Single.just(1)
                .as(converter)
                .test()
                .assertValue(1);

        Completable.complete()
                .as(converter)
                .test()
                .assertComplete();
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

    private static class CompositeConverter implements ObservableConverter<Integer, Flowable<Integer>>,
            FlowableConverter<Integer, Observable<Integer>>,
            MaybeConverter<Integer, Flowable<Integer>>,
            SingleConverter<Integer, Flowable<Integer>>,
            CompletableConverter<Flowable<Integer>> {
        @Override
        public Flowable<Integer> apply(Completable upstream) throws Exception {
            return upstream.toFlowable();
        }

        @Override
        public Observable<Integer> apply(Flowable<Integer> upstream) throws Exception {
            return upstream.toObservable();
        }

        @Override
        public Flowable<Integer> apply(Maybe<Integer> upstream) throws Exception {
            return upstream.toFlowable();
        }

        @Override
        public Flowable<Integer> apply(Observable<Integer> upstream) throws Exception {
            return upstream.toFlowable(BackpressureStrategy.MISSING);
        }

        @Override
        public Flowable<Integer> apply(Single<Integer> upstream) throws Exception {
            return upstream.toFlowable();
        }
    }
}
