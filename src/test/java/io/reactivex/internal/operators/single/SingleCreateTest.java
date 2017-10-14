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

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Cancellable;
import io.reactivex.plugins.RxJavaPlugins;

public class SingleCreateTest {

    @Test(expected = NullPointerException.class)
    public void nullArgument() {
        Single.create(null);
    }

    @Test
    public void basic() {
        final Disposable d = Disposables.empty();

        Single.<Integer>create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onSuccess(1);
                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
            }
        })
        .test()
        .assertResult(1);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithCancellable() {
        final Disposable d1 = Disposables.empty();
        final Disposable d2 = Disposables.empty();

        Single.<Integer>create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> e) throws Exception {
                e.setDisposable(d1);
                e.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        d2.dispose();
                    }
                });

                e.onSuccess(1);
                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
            }
        })
        .test()
        .assertResult(1);

        assertTrue(d1.isDisposed());
        assertTrue(d2.isDisposed());
    }

    @Test
    public void basicWithError() {
        final Disposable d = Disposables.empty();

        Single.<Integer>create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertTrue(d.isDisposed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsafeCreate() {
        Single.unsafeCreate(Single.just(1));
    }

    @Test
    public void createCallbackThrows() {
        Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> s) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> s) throws Exception {
                s.onSuccess(1);
            }
        }));
    }

    @Test
    public void createNullSuccess() {
        Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> s) throws Exception {
                s.onSuccess(null);
                s.onSuccess(null);
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void createNullError() {
        Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> s) throws Exception {
                s.onError(null);
                s.onError(null);
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void createConsumerThrows() {
        Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> s) throws Exception {
                try {
                    s.onSuccess(1);
                    fail("Should have thrown");
                } catch (TestException ex) {
                    // expected
                }
            }
        })
        .subscribe(new SingleObserver<Object>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(Object value) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    @Test
    public void createConsumerThrowsResource() {
        Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> s) throws Exception {
                Disposable d = Disposables.empty();
                s.setDisposable(d);
                try {
                    s.onSuccess(1);
                    fail("Should have thrown");
                } catch (TestException ex) {
                    // expected
                }

                assertTrue(d.isDisposed());
            }
        })
        .subscribe(new SingleObserver<Object>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(Object value) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    @Test
    public void createConsumerThrowsOnError() {
        Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> s) throws Exception {
                try {
                    s.onError(new IOException());
                    fail("Should have thrown");
                } catch (TestException ex) {
                    // expected
                }
            }
        })
        .subscribe(new SingleObserver<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(Object value) {
            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }
        });
    }

    @Test
    public void createConsumerThrowsResourceOnError() {
        Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> s) throws Exception {
                Disposable d = Disposables.empty();
                s.setDisposable(d);
                try {
                    s.onError(new IOException());
                    fail("Should have thrown");
                } catch (TestException ex) {
                    // expected
                }

                assertTrue(d.isDisposed());
            }
        })
        .subscribe(new SingleObserver<Object>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(Object value) {
            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }
        });
    }

    @Test
    public void tryOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Boolean[] response = { null };
            Single.create(new SingleOnSubscribe<Object>() {
                @Override
                public void subscribe(SingleEmitter<Object> e) throws Exception {
                    e.onSuccess(1);
                    response[0] = e.tryOnError(new TestException());
                }
            })
            .test()
            .assertResult(1);

            assertFalse(response[0]);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
