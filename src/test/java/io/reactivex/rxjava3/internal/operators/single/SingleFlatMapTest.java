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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.testsupport.*;

public class SingleFlatMapTest extends RxJavaTest {

    @Test
    public void normal() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return Completable.complete().doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        b[0] = true;
                    }
                });
            }
        })
        .test()
        .assertResult();

        assertTrue(b[0]);
    }

    @Test
    public void error() {
        final boolean[] b = { false };

        Single.<Integer>error(new TestException())
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return Completable.complete().doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        b[0] = true;
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(b[0]);
    }

    @Test
    public void mapperThrows() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(b[0]);
    }

    @Test
    public void mapperReturnsNull() {
        final boolean[] b = { false };

        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);

        assertFalse(b[0]);
    }

    @Test
    public void flatMapObservable() {
        Single.just(1).flatMapObservable(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 5);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void flatMapPublisher() {
        Single.just(1).flatMapPublisher(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(v, 5);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapPublisherMapperNull() {
        Single.just(1).flatMapPublisher(null);
    }

    @Test
    public void flatMapPublisherMapperThrows() {
        final TestException ex = new TestException();
        Single.just(1)
        .flatMapPublisher(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                throw ex;
            }
        })
        .test()
        .assertNoValues()
        .assertError(ex);
    }

    @Test
    public void flatMapPublisherSingleError() {
        final TestException ex = new TestException();
        Single.<Integer>error(ex)
        .flatMapPublisher(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.just(1);
            }
        })
        .test()
        .assertNoValues()
        .assertError(ex);
    }

    @Test
    public void flatMapPublisherCancelDuringSingle() {
        final AtomicBoolean disposed = new AtomicBoolean();
        TestSubscriberEx<Integer> ts = Single.<Integer>never()
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                disposed.set(true);
            }
        })
        .flatMapPublisher(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(v, 5);
            }
        })
        .to(TestHelper.<Integer>testConsumer())
        .assertNoValues()
        .assertNotTerminated();
        assertFalse(disposed.get());
        ts.cancel();
        assertTrue(disposed.get());
        ts.assertNotTerminated();
    }

    @Test
    public void flatMapPublisherCancelDuringFlowable() {
        final AtomicBoolean disposed = new AtomicBoolean();
        TestSubscriberEx<Integer> ts =
        Single.just(1)
        .flatMapPublisher(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.<Integer>never()
                        .doOnCancel(new Action() {
                            @Override
                            public void run() throws Exception {
                                disposed.set(true);
                            }
                        });
            }
        })
        .to(TestHelper.<Integer>testConsumer())
        .assertNoValues()
        .assertNotTerminated();
        assertFalse(disposed.get());
        ts.cancel();
        assertTrue(disposed.get());
        ts.assertNotTerminated();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNull() {
        Single.just(1)
            .flatMap(null);
    }

    @Test
    public void flatMapValue() {
        Single.just(1).flatMap(new Function<Integer, SingleSource<Integer>>() {
            @Override public SingleSource<Integer> apply(final Integer integer) throws Exception {
                if (integer == 1) {
                    return Single.just(2);
                }

                return Single.just(1);
            }
        })
            .test()
            .assertResult(2);
    }

    @Test
    public void flatMapValueDifferentType() {
        Single.just(1).flatMap(new Function<Integer, SingleSource<String>>() {
            @Override public SingleSource<String> apply(final Integer integer) throws Exception {
                if (integer == 1) {
                    return Single.just("2");
                }

                return Single.just("1");
            }
        })
            .test()
            .assertResult("2");
    }

    @Test
    public void flatMapValueNull() {
        Single.just(1).flatMap(new Function<Integer, SingleSource<Integer>>() {
            @Override public SingleSource<Integer> apply(final Integer integer) throws Exception {
                return null;
            }
        })
        .to(TestHelper.<Integer>testConsumer())
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("The single returned by the mapper is null");
    }

    @Test
    public void flatMapValueErrorThrown() {
        Single.just(1).flatMap(new Function<Integer, SingleSource<Integer>>() {
            @Override public SingleSource<Integer> apply(final Integer integer) throws Exception {
                throw new RuntimeException("something went terribly wrong!");
            }
        })
            .to(TestHelper.<Integer>testConsumer())
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("something went terribly wrong!");
    }

    @Test
    public void flatMapError() {
        RuntimeException exception = new RuntimeException("test");

        Single.error(exception).flatMap(new Function<Object, SingleSource<Object>>() {
            @Override public SingleSource<Object> apply(final Object integer) throws Exception {
                return Single.just(new Object());
            }
        })
            .test()
            .assertError(exception);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).flatMap(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(2);
            }
        }));
    }

    @Test
    public void mappedSingleOnError() {
        Single.just(1).flatMap(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Single<Object> s)
                    throws Exception {
                return s.flatMap(new Function<Object, SingleSource<? extends Object>>() {
                    @Override
                    public SingleSource<? extends Object> apply(Object v)
                            throws Exception {
                        return Single.just(v);
                    }
                });
            }
        });
    }
}
