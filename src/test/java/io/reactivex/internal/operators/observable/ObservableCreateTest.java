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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Cancellable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class ObservableCreateTest {

    @Test(expected = NullPointerException.class)
    public void nullArgument() {
        Observable.create(null);
    }

    @Test
    public void basic() {
        final Disposable d = Disposables.empty();

        Observable.<Integer>create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
                e.onError(new TestException());
                e.onNext(4);
                e.onError(new TestException());
                e.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithCancellable() {
        final Disposable d1 = Disposables.empty();
        final Disposable d2 = Disposables.empty();

        Observable.<Integer>create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.setDisposable(d1);
                e.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        d2.dispose();
                    }
                });

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
                e.onError(new TestException());
                e.onNext(4);
                e.onError(new TestException());
                e.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3);

        assertTrue(d1.isDisposed());
        assertTrue(d2.isDisposed());
    }

    @Test
    public void basicWithError() {
        final Disposable d = Disposables.empty();

        Observable.<Integer>create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onError(new TestException());
                e.onComplete();
                e.onNext(4);
                e.onError(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class, 1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicSerialized() {
        final Disposable d = Disposables.empty();

        Observable.<Integer>create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e = e.serialize();

                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
                e.onError(new TestException());
                e.onNext(4);
                e.onError(new TestException());
                e.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithErrorSerialized() {
        final Disposable d = Disposables.empty();

        Observable.<Integer>create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e = e.serialize();

                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onError(new TestException());
                e.onComplete();
                e.onNext(4);
                e.onError(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class, 1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void wrap() {
        Observable.wrap(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onNext(1);
                observer.onNext(2);
                observer.onNext(3);
                observer.onNext(4);
                observer.onNext(5);
                observer.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void unsafe() {
        Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onNext(1);
                observer.onNext(2);
                observer.onNext(3);
                observer.onNext(4);
                observer.onNext(5);
                observer.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsafeWithObservable() {
        Observable.unsafeCreate(Observable.just(1));
    }

    @Test
    public void createNullValue() {
        final Throwable[] error = { null };

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        })
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueSerialized() {
        final Throwable[] error = { null };

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e = e.serialize();
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        })
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void callbackThrows() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void nullValue() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                e.onNext(null);
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void nullThrowable() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                e.onError(null);
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void nullValueSync() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                e.serialize().onNext(null);
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void nullThrowableSync() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                e.serialize().onError(null);
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void onErrorCrash() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                Disposable d = Disposables.empty();
                e.setDisposable(d);
                try {
                    e.onError(new IOException());
                    fail("Should have thrown");
                } catch (TestException ex) {
                    // expected
                }
                assertTrue(d.isDisposed());
            }
        })
        .subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(Object value) {
            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void onCompleteCrash() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                Disposable d = Disposables.empty();
                e.setDisposable(d);
                try {
                    e.onComplete();
                    fail("Should have thrown");
                } catch (TestException ex) {
                    // expected
                }
                assertTrue(d.isDisposed());
            }
        })
        .subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(Object value) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
                throw new TestException();
            }
        });
    }

    @Test
    public void serialized() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.create(new ObservableOnSubscribe<Object>() {
                @Override
                public void subscribe(ObservableEmitter<Object> e) throws Exception {
                    ObservableEmitter<Object> f = e.serialize();

                    assertSame(f, f.serialize());

                    assertFalse(f.isDisposed());

                    final int[] calls = { 0 };

                    f.setCancellable(new Cancellable() {
                        @Override
                        public void cancel() throws Exception {
                            calls[0]++;
                        }
                    });

                    e.onComplete();

                    assertTrue(f.isDisposed());

                    assertEquals(1, calls[0]);
                }
            })
            .test()
            .assertResult();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void serializedConcurrentOnNext() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                final ObservableEmitter<Object> f = e.serialize();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 1000; i++) {
                            f.onNext(1);
                        }
                    }
                };

                TestHelper.race(r1, r1, Schedulers.single());
            }
        })
        .take(1000)
        .test()
        .assertSubscribed().assertValueCount(1000).assertComplete().assertNoErrors();
    }

    @Test
    public void serializedConcurrentOnNextOnError() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                final ObservableEmitter<Object> f = e.serialize();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 1000; i++) {
                            f.onNext(1);
                        }
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 100; i++) {
                            f.onNext(1);
                        }
                        f.onError(new TestException());
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());
            }
        })
        .test()
        .assertSubscribed().assertNotComplete()
        .assertError(TestException.class);
    }

    @Test
    public void serializedConcurrentOnNextOnComplete() {
        TestObserver<Object> to = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                final ObservableEmitter<Object> f = e.serialize();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 1000; i++) {
                            f.onNext(1);
                        }
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 100; i++) {
                            f.onNext(1);
                        }
                        f.onComplete();
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());
            }
        })
        .test()
        .assertSubscribed().assertComplete()
        .assertNoErrors();

        int c = to.valueCount();
        assertTrue("" + c, c >= 100);
    }

    @Test
    public void onErrorRace() {
        Observable<Object> source = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                final ObservableEmitter<Object> f = e.serialize();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        f.onError(null);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        f.onError(ex);
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());
            }
        });

        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            for (int i = 0; i < 500; i++) {
                source
                .test()
                .assertFailure(Throwable.class);
            }
        } finally {
            RxJavaPlugins.reset();
        }
        assertFalse(errors.isEmpty());
    }

    @Test
    public void onCompleteRace() {
        Observable<Object> source = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                final ObservableEmitter<Object> f = e.serialize();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        f.onComplete();
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        f.onComplete();
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());
            }
        });

        for (int i = 0; i < 500; i++) {
            source
            .test()
            .assertResult();
        }
    }

    @Test
    public void tryOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Boolean[] response = { null };
            Observable.create(new ObservableOnSubscribe<Object>() {
                @Override
                public void subscribe(ObservableEmitter<Object> e) throws Exception {
                    e.onNext(1);
                    response[0] = e.tryOnError(new TestException());
                }
            })
            .take(1)
            .test()
            .assertResult(1);

            assertFalse(response[0]);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void tryOnErrorSerialized() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Boolean[] response = { null };
            Observable.create(new ObservableOnSubscribe<Object>() {
                @Override
                public void subscribe(ObservableEmitter<Object> e) throws Exception {
                    e = e.serialize();
                    e.onNext(1);
                    response[0] = e.tryOnError(new TestException());
                }
            })
            .take(1)
            .test()
            .assertResult(1);

            assertFalse(response[0]);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
