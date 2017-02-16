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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;

public class SingleDoOnTest {

    @Test
    public void doOnDispose() {
        final int[] count = { 0 };

        Single.never().doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                count[0]++;
            }
        }).test(true);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnError() {
        final Object[] event = { null };

        Single.error(new TestException()).doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                event[0] = e;
            }
        })
        .test();

        assertTrue(event[0].toString(), event[0] instanceof TestException);
    }

    @Test
    public void doOnSubscribe() {
        final int[] count = { 0 };

        Single.never().doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) throws Exception {
                count[0]++;
            }
        }).test();

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSuccess() {
        final Object[] event = { null };

        Single.just(1).doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                event[0] = e;
            }
        })
        .test();

        assertEquals(1, event[0]);
    }

    @Test
    public void doOnSubscribeNormal() {
        final int[] count = { 0 };

        Single.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
                count[0]++;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSubscribeError() {
        final int[] count = { 0 };

        Single.error(new TestException()).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
                count[0]++;
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSubscribeJustCrash() {

        Single.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnSubscribeErrorCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Single.error(new TestException("Outer")).doOnSubscribe(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable s) throws Exception {
                    throw new TestException("Inner");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "Inner");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Outer");
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void onErrorSuccess() {
        final int[] call = { 0 };

        Single.just(1)
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable v) throws Exception {
                call[0]++;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(0, call[0]);
    }

    @Test
    public void onErrorCrashes() {
        TestObserver<Object> to = Single.error(new TestException("Outer"))
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable v) throws Exception {
                throw new TestException("Inner");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void doOnEventThrowsSuccess() {
        Single.just(1)
        .doOnEvent(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnEventThrowsError() {
        TestObserver<Integer> to = Single.<Integer>error(new TestException("Main"))
        .doOnEvent(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Main");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void doOnDisposeDispose() {
        final int[] calls = { 0 };
        TestHelper.checkDisposed(PublishSubject.create().singleOrError().doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                calls[0]++;
            }
        }));

        assertEquals(1, calls[0]);
    }

    @Test
    public void doOnDisposeSuccess() {
        final int[] calls = { 0 };

        Single.just(1)
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                calls[0]++;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(0, calls[0]);
    }

    @Test
    public void doOnDisposeError() {
        final int[] calls = { 0 };

        Single.error(new TestException())
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                calls[0]++;
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, calls[0]);
    }

    @Test
    public void doOnDisposeDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Single<Object> s) throws Exception {
                return s.doOnDispose(Functions.EMPTY_ACTION);
            }
        });
    }

    @Test
    public void doOnDisposeCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            PublishSubject<Integer> ps = PublishSubject.create();

            ps.singleOrError().doOnDispose(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .test()
            .cancel();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doOnSuccessErrors() {
        final int[] call = { 0 };

        Single.error(new TestException())
        .doOnSuccess(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                call[0]++;
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void doOnSuccessCrash() {
        Single.just(1)
        .doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Disposable bs = Disposables.empty();

            new Single<Integer>() {
                @Override
                protected void subscribeActual(SingleObserver<? super Integer> s) {
                    s.onSubscribe(bs);
                    s.onError(new TestException("Second"));
                    s.onSuccess(1);
                }
            }
            .doOnSubscribe(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable s) throws Exception {
                    throw new TestException("First");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
