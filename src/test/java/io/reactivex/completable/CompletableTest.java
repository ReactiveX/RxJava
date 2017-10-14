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

package io.reactivex.completable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Test Completable methods and operators.
 */
public class CompletableTest {
    /**
     * Iterable that returns an Iterator that throws in its hasNext method.
     */
    static final class IterableIteratorNextThrows implements Iterable<Completable> {
        @Override
        public Iterator<Completable> iterator() {
            return new Iterator<Completable>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Completable next() {
                    throw new TestException();
                }

                @Override
                public void remove() {
                    // TODO Auto-generated method stub

                }
            };
        }
    }

    /**
     * Iterable that returns an Iterator that throws in its next method.
     */
    static final class IterableIteratorHasNextThrows implements Iterable<Completable> {
        @Override
        public Iterator<Completable> iterator() {
            return new Iterator<Completable>() {
                @Override
                public boolean hasNext() {
                    throw new TestException();
                }

                @Override
                public Completable next() {
                    return null;
                }

                @Override
                public void remove() {
                    // TODO Auto-generated method stub

                }
            };
        }
    }

    /**
     * A class containing a completable instance and counts the number of subscribers.
     */
    static final class NormalCompletable extends AtomicInteger {

        private static final long serialVersionUID = 7192337844700923752L;

        public final Completable completable = Completable.unsafeCreate(new CompletableSource() {
            @Override
            public void subscribe(CompletableObserver s) {
                getAndIncrement();
                EmptyDisposable.complete(s);
            }
        });

        /**
         * Asserts the given number of subscriptions happened.
         * @param n the expected number of subscriptions
         */
        public void assertSubscriptions(int n) {
            Assert.assertEquals(n, get());
        }
    }

    /**
     * A class containing a completable instance that emits a TestException and counts
     * the number of subscribers.
     */
    static final class ErrorCompletable extends AtomicInteger {

        private static final long serialVersionUID = 7192337844700923752L;

        public final Completable completable = Completable.unsafeCreate(new CompletableSource() {
            @Override
            public void subscribe(CompletableObserver s) {
                getAndIncrement();
                EmptyDisposable.error(new TestException(), s);
            }
        });

        /**
         * Asserts the given number of subscriptions happened.
         * @param n the expected number of subscriptions
         */
        public void assertSubscriptions(int n) {
            Assert.assertEquals(n, get());
        }
    }

    /** A normal Completable object. */
    final NormalCompletable normal = new NormalCompletable();

    /** An error Completable object. */
    final ErrorCompletable error = new ErrorCompletable();

    @Test(timeout = 5000)
    public void complete() {
        Completable c = Completable.complete();

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void concatNull() {
        Completable.concatArray((Completable[])null);
    }

    @Test(timeout = 5000)
    public void concatEmpty() {
        Completable c = Completable.concatArray();

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void concatSingleSource() {
        Completable c = Completable.concatArray(normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void concatSingleSourceThrows() {
        Completable c = Completable.concatArray(error.completable);

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void concatMultipleSources() {
        Completable c = Completable.concatArray(normal.completable, normal.completable, normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void concatMultipleOneThrows() {
        Completable c = Completable.concatArray(normal.completable, error.completable, normal.completable);

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void concatMultipleOneIsNull() {
        Completable c = Completable.concatArray(normal.completable, null);

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void concatIterableEmpty() {
        Completable c = Completable.concat(Collections.<Completable>emptyList());

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableNull() {
        Completable.concat((Iterable<Completable>)null);
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Completable c = Completable.concat(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return null;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void concatIterableWithNull() {
        Completable c = Completable.concat(Arrays.asList(normal.completable, (Completable)null));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void concatIterableSingle() {
        Completable c = Completable.concat(Collections.singleton(normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void concatIterableMany() {
        Completable c = Completable.concat(Arrays.asList(normal.completable, normal.completable, normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void concatIterableOneThrows() {
        Completable c = Completable.concat(Collections.singleton(error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void concatIterableManyOneThrows() {
        Completable c = Completable.concat(Arrays.asList(normal.completable, error.completable));

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void concatIterableIterableThrows() {
        Completable c = Completable.concat(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                throw new TestException();
            }
        });

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void concatIterableIteratorHasNextThrows() {
        Completable c = Completable.concat(new IterableIteratorHasNextThrows());

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void concatIterableIteratorNextThrows() {
        Completable c = Completable.concat(new IterableIteratorNextThrows());

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void concatObservableEmpty() {
        Completable c = Completable.concat(Flowable.<Completable>empty());

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void concatObservableError() {
        Completable c = Completable.concat(Flowable.<Completable>error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new TestException();
            }
        }));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void concatObservableSingle() {
        Completable c = Completable.concat(Flowable.just(normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void concatObservableSingleThrows() {
        Completable c = Completable.concat(Flowable.just(error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void concatObservableMany() {
        Completable c = Completable.concat(Flowable.just(normal.completable).repeat(3));

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void concatObservableManyOneThrows() {
        Completable c = Completable.concat(Flowable.just(normal.completable, error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void concatObservablePrefetch() {
        final List<Long> requested = new ArrayList<Long>();
        Flowable<Completable> cs = Flowable
                .just(normal.completable)
                .repeat(10)
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long v) {
                        requested.add(v);
                    }
                });

        Completable c = Completable.concat(cs, 5);

        c.blockingAwait();

        Assert.assertEquals(Arrays.asList(5L, 4L, 4L), requested);
    }

    @Test(expected = NullPointerException.class)
    public void createNull() {
        Completable.unsafeCreate(null);
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void createOnSubscribeThrowsNPE() {
        Completable c = Completable.unsafeCreate(new CompletableSource() {
            @Override
            public void subscribe(CompletableObserver s) { throw new NullPointerException(); }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void createOnSubscribeThrowsRuntimeException() {
        try {
            Completable c = Completable.unsafeCreate(new CompletableSource() {
                @Override
                public void subscribe(CompletableObserver s) {
                    throw new TestException();
                }
            });

            c.blockingAwait();

            Assert.fail("Did not throw exception");
        } catch (NullPointerException ex) {
            if (!(ex.getCause() instanceof TestException)) {
                ex.printStackTrace();
                Assert.fail("Did not wrap the TestException but it returned: " + ex);
            }
        }
    }

    @Test(timeout = 5000)
    public void defer() {
        Completable c = Completable.defer(new Callable<Completable>() {
            @Override
            public Completable call() {
                return normal.completable;
            }
        });

        normal.assertSubscriptions(0);

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(expected = NullPointerException.class)
    public void deferNull() {
        Completable.defer(null);
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void deferReturnsNull() {
        Completable c = Completable.defer(new Callable<Completable>() {
            @Override
            public Completable call() {
                return null;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void deferFunctionThrows() {
        Completable c = Completable.defer(new Callable<Completable>() {
            @Override
            public Completable call() { throw new TestException(); }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void deferErrorSource() {
        Completable c = Completable.defer(new Callable<Completable>() {
            @Override
            public Completable call() {
                return error.completable;
            }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void errorSupplierNull() {
        Completable.error((Callable<Throwable>)null);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void errorSupplierNormal() {
        Completable c = Completable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new TestException();
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void errorSupplierReturnsNull() {
        Completable c = Completable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return null;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void errorSupplierThrows() {
        Completable c = Completable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() { throw new TestException(); }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void errorNull() {
        Completable.error((Throwable)null);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void errorNormal() {
        Completable c = Completable.error(new TestException());

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableNull() {
        Completable.fromCallable(null);
    }

    @Test(timeout = 5000)
    public void fromCallableNormal() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return calls.getAndIncrement();
            }
        });

        c.blockingAwait();

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void fromCallableThrows() {
        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception { throw new TestException(); }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void fromFlowableNull() {
        Completable.fromPublisher(null);
    }

    @Test(timeout = 5000)
    public void fromFlowableEmpty() {
        Completable c = Completable.fromPublisher(Flowable.empty());

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void fromFlowableSome() {
        for (int n = 1; n < 10000; n *= 10) {
            Completable c = Completable.fromPublisher(Flowable.range(1, n));

            c.blockingAwait();
        }
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void fromFlowableError() {
        Completable c = Completable.fromPublisher(Flowable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new TestException();
            }
        }));

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void fromObservableNull() {
        Completable.fromObservable(null);
    }

    @Test(timeout = 5000)
    public void fromObservableEmpty() {
        Completable c = Completable.fromObservable(Observable.empty());

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void fromObservableSome() {
        for (int n = 1; n < 10000; n *= 10) {
            Completable c = Completable.fromObservable(Observable.range(1, n));

            c.blockingAwait();
        }
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void fromObservableError() {
        Completable c = Completable.fromObservable(Observable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new TestException();
            }
        }));

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void fromActionNull() {
        Completable.fromAction(null);
    }

    @Test(timeout = 5000)
    public void fromActionNormal() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = Completable.fromAction(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        c.blockingAwait();

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void fromActionThrows() {
        Completable c = Completable.fromAction(new Action() {
            @Override
            public void run() { throw new TestException(); }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void fromSingleNull() {
        Completable.fromSingle(null);
    }

    @Test(timeout = 5000)
    public void fromSingleNormal() {
        Completable c = Completable.fromSingle(Single.just(1));

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void fromSingleThrows() {
        Completable c = Completable.fromSingle(Single.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new TestException();
            }
        }));

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void mergeNull() {
        Completable.mergeArray((Completable[])null);
    }

    @Test(timeout = 5000)
    public void mergeEmpty() {
        Completable c = Completable.mergeArray();

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeSingleSource() {
        Completable c = Completable.mergeArray(normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeSingleSourceThrows() {
        Completable c = Completable.mergeArray(error.completable);

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeMultipleSources() {
        Completable c = Completable.mergeArray(normal.completable, normal.completable, normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeMultipleOneThrows() {
        Completable c = Completable.mergeArray(normal.completable, error.completable, normal.completable);

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void mergeMultipleOneIsNull() {
        Completable c = Completable.mergeArray(normal.completable, null);

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeIterableEmpty() {
        Completable c = Completable.merge(Collections.<Completable>emptyList());

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableNull() {
        Completable.merge((Iterable<Completable>)null);
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void mergeIterableIteratorNull() {
        Completable c = Completable.merge(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return null;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void mergeIterableWithNull() {
        Completable c = Completable.merge(Arrays.asList(normal.completable, (Completable)null));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeIterableSingle() {
        Completable c = Completable.merge(Collections.singleton(normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void mergeIterableMany() {
        Completable c = Completable.merge(Arrays.asList(normal.completable, normal.completable, normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeIterableOneThrows() {
        Completable c = Completable.merge(Collections.singleton(error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeIterableManyOneThrows() {
        Completable c = Completable.merge(Arrays.asList(normal.completable, error.completable));

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void mergeIterableIterableThrows() {
        Completable c = Completable.merge(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                throw new TestException();
            }
        });

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void mergeIterableIteratorHasNextThrows() {
        Completable c = Completable.merge(new IterableIteratorHasNextThrows());

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void mergeIterableIteratorNextThrows() {
        Completable c = Completable.merge(new IterableIteratorNextThrows());

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeObservableEmpty() {
        Completable c = Completable.merge(Flowable.<Completable>empty());

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeObservableError() {
        Completable c = Completable.merge(Flowable.<Completable>error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new TestException();
            }
        }));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeObservableSingle() {
        Completable c = Completable.merge(Flowable.just(normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeObservableSingleThrows() {
        Completable c = Completable.merge(Flowable.just(error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeObservableMany() {
        Completable c = Completable.merge(Flowable.just(normal.completable).repeat(3));

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeObservableManyOneThrows() {
        Completable c = Completable.merge(Flowable.just(normal.completable, error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeObservableMaxConcurrent() {
        final List<Long> requested = new ArrayList<Long>();
        Flowable<Completable> cs = Flowable
                .just(normal.completable)
                .repeat(10)
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long v) {
                        requested.add(v);
                    }
                });

        Completable c = Completable.merge(cs, 5);

        c.blockingAwait();

        // FIXME this request pattern looks odd because all 10 completions trigger 1 requests
        Assert.assertEquals(Arrays.asList(5L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), requested);
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorNull() {
        Completable.mergeArrayDelayError((Completable[])null);
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorEmpty() {
        Completable c = Completable.mergeArrayDelayError();

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorSingleSource() {
        Completable c = Completable.mergeArrayDelayError(normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeDelayErrorSingleSourceThrows() {
        Completable c = Completable.mergeArrayDelayError(error.completable);

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorMultipleSources() {
        Completable c = Completable.mergeArrayDelayError(normal.completable, normal.completable, normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorMultipleOneThrows() {
        Completable c = Completable.mergeArrayDelayError(normal.completable, error.completable, normal.completable);

        try {
            c.blockingAwait();
        } catch (TestException ex) {
            normal.assertSubscriptions(2);
        }
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void mergeDelayErrorMultipleOneIsNull() {
        Completable c = Completable.mergeArrayDelayError(normal.completable, null);

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorIterableEmpty() {
        Completable c = Completable.mergeDelayError(Collections.<Completable>emptyList());

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableNull() {
        Completable.mergeDelayError((Iterable<Completable>)null);
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void mergeDelayErrorIterableIteratorNull() {
        Completable c = Completable.mergeDelayError(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return null;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void mergeDelayErrorIterableWithNull() {
        Completable c = Completable.mergeDelayError(Arrays.asList(normal.completable, (Completable)null));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorIterableSingle() {
        Completable c = Completable.mergeDelayError(Collections.singleton(normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorIterableMany() {
        Completable c = Completable.mergeDelayError(Arrays.asList(normal.completable, normal.completable, normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeDelayErrorIterableOneThrows() {
        Completable c = Completable.mergeDelayError(Collections.singleton(error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorIterableManyOneThrows() {
        Completable c = Completable.mergeDelayError(Arrays.asList(normal.completable, error.completable, normal.completable));

        try {
            c.blockingAwait();
        } catch (TestException ex) {
            normal.assertSubscriptions(2);
        }
    }

    @Test(expected = TestException.class)
    public void mergeDelayErrorIterableIterableThrows() {
        Completable c = Completable.mergeDelayError(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                throw new TestException();
            }
        });

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void mergeDelayErrorIterableIteratorHasNextThrows() {
        Completable c = Completable.mergeDelayError(new IterableIteratorHasNextThrows());

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void mergeDelayErrorIterableIteratorNextThrows() {
        Completable c = Completable.mergeDelayError(new IterableIteratorNextThrows());

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorObservableEmpty() {
        Completable c = Completable.mergeDelayError(Flowable.<Completable>empty());

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeDelayErrorObservableError() {
        Completable c = Completable.mergeDelayError(Flowable.<Completable>error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return new TestException();
            }
        }));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorObservableSingle() {
        Completable c = Completable.mergeDelayError(Flowable.just(normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeDelayErrorObservableSingleThrows() {
        Completable c = Completable.mergeDelayError(Flowable.just(error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorObservableMany() {
        Completable c = Completable.mergeDelayError(Flowable.just(normal.completable).repeat(3));

        c.blockingAwait();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeDelayErrorObservableManyOneThrows() {
        Completable c = Completable.mergeDelayError(Flowable.just(normal.completable, error.completable));

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorObservableMaxConcurrent() {
        final List<Long> requested = new ArrayList<Long>();
        Flowable<Completable> cs = Flowable
                .just(normal.completable)
                .repeat(10)
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long v) {
                        requested.add(v);
                    }
                });

        Completable c = Completable.mergeDelayError(cs, 5);

        c.blockingAwait();

        // FIXME this request pattern looks odd because all 10 completions trigger 1 requests
        Assert.assertEquals(Arrays.asList(5L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), requested);
    }

    @Test(timeout = 5000)
    public void never() {
        final AtomicBoolean onSubscribeCalled = new AtomicBoolean();
        final AtomicInteger calls = new AtomicInteger();
        Completable.never().subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                onSubscribeCalled.set(true);
            }

            @Override
            public void onError(Throwable e) {
                calls.getAndIncrement();
            }

            @Override
            public void onComplete() {
                calls.getAndIncrement();
            }
        });

        Assert.assertTrue("onSubscribe not called", onSubscribeCalled.get());
        Assert.assertEquals("There were calls to onXXX methods", 0, calls.get());
    }

    @Test(timeout = 1500)
    public void timer() {
        Completable c = Completable.timer(500, TimeUnit.MILLISECONDS);

        c.blockingAwait();
    }

    @Test(timeout = 1500)
    public void timerNewThread() {
        Completable c = Completable.timer(500, TimeUnit.MILLISECONDS, Schedulers.newThread());

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void timerTestScheduler() {
        TestScheduler scheduler = new TestScheduler();

        Completable c = Completable.timer(250, TimeUnit.MILLISECONDS, scheduler);

        final AtomicInteger calls = new AtomicInteger();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onComplete() {
                calls.getAndIncrement();
            }

            @Override
            public void onError(Throwable e) {
                RxJavaPlugins.onError(e);
            }
        });

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        Assert.assertEquals(0, calls.get());

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 2000)
    public void timerCancel() throws InterruptedException {
        Completable c = Completable.timer(250, TimeUnit.MILLISECONDS);

        final SequentialDisposable sd = new SequentialDisposable();
        final AtomicInteger calls = new AtomicInteger();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                sd.replace(d);
            }

            @Override
            public void onError(Throwable e) {
                calls.getAndIncrement();
            }

            @Override
            public void onComplete() {
                calls.getAndIncrement();
            }
        });

        Thread.sleep(100);

        sd.dispose();

        Thread.sleep(200);

        Assert.assertEquals(0, calls.get());
    }

    @Test(expected = NullPointerException.class)
    public void timerUnitNull() {
        Completable.timer(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timerSchedulerNull() {
        Completable.timer(1, TimeUnit.SECONDS, null);
    }

    @Test(timeout = 5000)
    public void usingNormalEager() {
        final AtomicInteger dispose = new AtomicInteger();

        Completable c = Completable.using(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }, new Function<Object, Completable>() {
            @Override
            public Completable apply(Object v) {
                return normal.completable;
            }
        }, new Consumer<Integer>() {
            @Override
            public void accept(Integer d) {
                dispose.set(d);
            }
        });

        final AtomicBoolean disposedFirst = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                error.lazySet(e);
            }

            @Override
            public void onComplete() {
                disposedFirst.set(dispose.get() != 0);
            }
        });

        Assert.assertEquals(1, dispose.get());
        Assert.assertTrue("Not disposed first", disposedFirst.get());
        Assert.assertNull(error.get());
    }

    @Test(timeout = 5000)
    public void usingNormalLazy() {
        final AtomicInteger dispose = new AtomicInteger();

        Completable c = Completable.using(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }, new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer v) {
                return normal.completable;
            }
        }, new Consumer<Integer>() {
            @Override
            public void accept(Integer d) {
                dispose.set(d);
            }
        }, false);

        final AtomicBoolean disposedFirst = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                error.lazySet(e);
            }

            @Override
            public void onComplete() {
                disposedFirst.set(dispose.get() != 0);
            }
        });

        Assert.assertEquals(1, dispose.get());
        Assert.assertFalse("Disposed first", disposedFirst.get());
        Assert.assertNull(error.get());
    }

    @Test(timeout = 5000)
    public void usingErrorEager() {
        final AtomicInteger dispose = new AtomicInteger();

        Completable c = Completable.using(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }, new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer v) {
                return error.completable;
            }
        }, new Consumer<Integer>() {
            @Override
            public void accept(Integer d) {
                dispose.set(d);
            }
        });

        final AtomicBoolean disposedFirst = new AtomicBoolean();
        final AtomicBoolean complete = new AtomicBoolean();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                disposedFirst.set(dispose.get() != 0);
            }

            @Override
            public void onComplete() {
                complete.set(true);
            }
        });

        Assert.assertEquals(1, dispose.get());
        Assert.assertTrue("Not disposed first", disposedFirst.get());
        Assert.assertFalse(complete.get());
    }

    @Test(timeout = 5000)
    public void usingErrorLazy() {
        final AtomicInteger dispose = new AtomicInteger();

        Completable c = Completable.using(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }, new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer v) {
                return error.completable;
            }
        }, new Consumer<Integer>() {
            @Override
            public void accept(Integer d) {
                dispose.set(d);
            }
        }, false);

        final AtomicBoolean disposedFirst = new AtomicBoolean();
        final AtomicBoolean complete = new AtomicBoolean();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                disposedFirst.set(dispose.get() != 0);
            }

            @Override
            public void onComplete() {
                complete.set(true);
            }
        });

        Assert.assertEquals(1, dispose.get());
        Assert.assertFalse("Disposed first", disposedFirst.get());
        Assert.assertFalse(complete.get());
    }

    @Test(expected = NullPointerException.class)
    public void usingResourceSupplierNull() {
        Completable.using(null, new Function<Object, Completable>() {
            @Override
            public Completable apply(Object v) {
                return normal.completable;
            }
        }, new Consumer<Object>() {
            @Override
            public void accept(Object v) { }
        });
    }

    @Test(expected = NullPointerException.class)
    public void usingMapperNull() {
        Completable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null, new Consumer<Object>() {
            @Override
            public void accept(Object v) { }
        });
    }

    @Test(expected = NullPointerException.class)
    public void usingMapperReturnsNull() {
        Completable c = Completable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function<Object, Completable>() {
            @Override
            public Completable apply(Object v) {
                return null;
            }
        }, new Consumer<Object>() {
            @Override
            public void accept(Object v) { }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void usingDisposeNull() {
        Completable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function<Object, Completable>() {
            @Override
            public Completable apply(Object v) {
                return normal.completable;
            }
        }, null);
    }

    @Test(expected = TestException.class)
    public void usingResourceThrows() {
        Completable c = Completable.using(new Callable<Object>() {
            @Override
            public Object call() { throw new TestException(); }
        },
                new Function<Object, Completable>() {
                    @Override
                    public Completable apply(Object v) {
                        return normal.completable;
                    }
                }, new Consumer<Object>() {
                    @Override
                    public void accept(Object v) { }
                });

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void usingMapperThrows() {
        Completable c = Completable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        },
                new Function<Object, Completable>() {
                    @Override
                    public Completable apply(Object v) { throw new TestException(); }
                }, new Consumer<Object>() {
                    @Override
                    public void accept(Object v) { }
                });

        c.blockingAwait();
    }

    @Test(expected = TestException.class)
    public void usingDisposerThrows() {
        Completable c = Completable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        },
                new Function<Object, Completable>() {
                    @Override
                    public Completable apply(Object v) {
                        return normal.completable;
                    }
                }, new Consumer<Object>() {
                    @Override
                    public void accept(Object v) { throw new TestException(); }
                });

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void composeNormal() {
        Completable c = error.completable.compose(new CompletableTransformer() {
            @Override
            public Completable apply(Completable n) {
                return n.onErrorComplete();
            }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void composeNull() {
        error.completable.compose(null);
    }

    @Test(timeout = 5000)
    public void concatWithNormal() {
        Completable c = normal.completable.concatWith(normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(2);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void concatWithError() {
        Completable c = normal.completable.concatWith(error.completable);

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void concatWithNull() {
        normal.completable.concatWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void delayUnitNull() {
        normal.completable.delay(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySchedulerNull() {
        normal.completable.delay(1, TimeUnit.SECONDS, null);
    }

    @Test(timeout = 5000)
    public void delayNormal() throws InterruptedException {
        Completable c = normal.completable.delay(250, TimeUnit.MILLISECONDS);

        final AtomicBoolean done = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                error.set(e);
            }

            @Override
            public void onComplete() {
                done.set(true);
            }
        });

        Thread.sleep(100);

        Assert.assertFalse("Already done", done.get());

        int timeout = 10;

        while (timeout-- > 0 && !done.get()) {
            Thread.sleep(100);
        }

        Assert.assertTrue("Not done", done.get());

        Assert.assertNull(error.get());
    }

    @Test(timeout = 5000)
    public void delayErrorImmediately() throws InterruptedException {
        final TestScheduler scheduler = new TestScheduler();
        final Completable c = error.completable.delay(250, TimeUnit.MILLISECONDS, scheduler);

        final AtomicBoolean done = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                error.set(e);
            }

            @Override
            public void onComplete() {
                done.set(true);
            }
        });

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        Assert.assertTrue(error.get().toString(), error.get() instanceof TestException);
        Assert.assertFalse("Already done", done.get());

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        Assert.assertFalse("Already done", done.get());
    }

    @Test(timeout = 5000)
    public void delayErrorToo() throws InterruptedException {
        Completable c = error.completable.delay(250, TimeUnit.MILLISECONDS, Schedulers.computation(), true);

        final AtomicBoolean done = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {
                error.set(e);
            }

            @Override
            public void onComplete() {
                done.set(true);
            }
        });

        Thread.sleep(100);

        Assert.assertFalse("Already done", done.get());
        Assert.assertNull(error.get());

        Thread.sleep(200);

        Assert.assertFalse("Already done", done.get());
        Assert.assertTrue(error.get() instanceof TestException);
    }

    @Test(timeout = 5000)
    public void doOnCompleteNormal() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = normal.completable.doOnComplete(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        c.blockingAwait();

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 5000)
    public void doOnCompleteError() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = error.completable.doOnComplete(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        try {
            c.blockingAwait();
            Assert.fail("Failed to throw TestException");
        } catch (TestException ex) {
            // expected
        }

        Assert.assertEquals(0, calls.get());
    }

    @Test(expected = NullPointerException.class)
    public void doOnCompleteNull() {
        normal.completable.doOnComplete(null);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void doOnCompleteThrows() {
        Completable c = normal.completable.doOnComplete(new Action() {
            @Override
            public void run() { throw new TestException(); }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void doOnDisposeNormalDoesntCall() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = normal.completable.doOnDispose(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        c.blockingAwait();

        Assert.assertEquals(0, calls.get());
    }

    @Test(timeout = 5000)
    public void doOnDisposeErrorDoesntCall() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = error.completable.doOnDispose(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        try {
            c.blockingAwait();
            Assert.fail("No exception thrown");
        } catch (TestException ex) {
            // expected
        }
        Assert.assertEquals(0, calls.get());
    }

    @Test(timeout = 5000)
    public void doOnDisposeChildCancels() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = normal.completable.doOnDispose(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                d.dispose();
            }

            @Override
            public void onError(Throwable e) {
                // ignored
            }

            @Override
            public void onComplete() {
                // ignored
            }
        });

        Assert.assertEquals(1, calls.get());
    }

    @Test(expected = NullPointerException.class)
    public void doOnDisposeNull() {
        normal.completable.doOnDispose(null);
    }

    @Test(timeout = 5000)
    public void doOnDisposeThrows() {
        Completable c = normal.completable.doOnDispose(new Action() {
            @Override
            public void run() { throw new TestException(); }
        });

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                d.dispose();
            }

            @Override
            public void onError(Throwable e) {
                // ignored
            }

            @Override
            public void onComplete() {
                // ignored
            }
        });
    }

    @Test(timeout = 5000)
    public void doOnErrorNoError() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        Completable c = normal.completable.doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                error.set(e);
            }
        });

        c.blockingAwait();

        Assert.assertNull(error.get());
    }

    @Test(timeout = 5000)
    public void doOnErrorHasError() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();

        Completable c = error.completable.doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                err.set(e);
            }
        });

        try {
            c.blockingAwait();
            Assert.fail("Did not throw exception");
        } catch (Throwable e) {
            // expected
        }

        Assert.assertTrue(err.get() instanceof TestException);
    }

    @Test(expected = NullPointerException.class)
    public void doOnErrorNull() {
        normal.completable.doOnError(null);
    }

    @Test(timeout = 5000)
    public void doOnErrorThrows() {
        Completable c = error.completable.doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) { throw new IllegalStateException(); }
        });

        try {
            c.blockingAwait();
        } catch (CompositeException ex) {
            List<Throwable> a = ex.getExceptions();
            Assert.assertEquals(2, a.size());
            Assert.assertTrue(a.get(0) instanceof TestException);
            Assert.assertTrue(a.get(1) instanceof IllegalStateException);
        }
    }

    @Test(timeout = 5000)
    public void doOnSubscribeNormal() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = normal.completable.doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) {
                calls.getAndIncrement();
            }
        });

        for (int i = 0; i < 10; i++) {
            c.blockingAwait();
        }

        Assert.assertEquals(10, calls.get());
    }

    @Test(expected = NullPointerException.class)
    public void doOnSubscribeNull() {
        normal.completable.doOnSubscribe(null);
    }

    @Test(expected = TestException.class)
    public void doOnSubscribeThrows() {
        Completable c = normal.completable.doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) { throw new TestException(); }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void doOnTerminateNormal() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = normal.completable.doOnTerminate(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        c.blockingAwait();

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 5000)
    public void doOnTerminateError() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = error.completable.doOnTerminate(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        try {
            c.blockingAwait();
            Assert.fail("Did dot throw exception");
        } catch (TestException ex) {
            // expected
        }

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 5000)
    public void getNormal() {
        Assert.assertNull(normal.completable.blockingGet());
    }

    @Test(timeout = 5000)
    public void getError() {
        Assert.assertTrue(error.completable.blockingGet() instanceof TestException);
    }

    @Test(timeout = 5000)
    public void getTimeout() {
        try {
            Completable.never().blockingGet(100, TimeUnit.MILLISECONDS);
        } catch (RuntimeException ex) {
            if (!(ex.getCause() instanceof TimeoutException)) {
                Assert.fail("Wrong exception cause: " + ex.getCause());
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void getNullUnit() {
        normal.completable.blockingGet(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void liftNull() {
        normal.completable.lift(null);
    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    public void liftReturnsNull() {
        Completable c = normal.completable.lift(new CompletableOperator() {
            @Override
            public CompletableObserver apply(CompletableObserver v) {
                return null;
            }
        });

        c.blockingAwait();
    }

    static final class CompletableOperatorSwap implements CompletableOperator {
        @Override
        public CompletableObserver apply(final CompletableObserver v) {
            return new CompletableObserver() {

                @Override
                public void onComplete() {
                    v.onError(new TestException());
                }

                @Override
                public void onError(Throwable e) {
                    v.onComplete();
                }

                @Override
                public void onSubscribe(Disposable d) {
                    v.onSubscribe(d);
                }

            };
        }
    }
    @Test(timeout = 5000, expected = TestException.class)
    public void liftOnCompleteError() {
        Completable c = normal.completable.lift(new CompletableOperatorSwap());

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void liftOnErrorComplete() {
        Completable c = error.completable.lift(new CompletableOperatorSwap());

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void mergeWithNull() {
        normal.completable.mergeWith(null);
    }

    @Test(timeout = 5000)
    public void mergeWithNormal() {
        Completable c = normal.completable.mergeWith(normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(2);
    }

    @Test(expected = NullPointerException.class)
    public void observeOnNull() {
        normal.completable.observeOn(null);
    }

    @Test(timeout = 5000)
    public void observeOnNormal() throws InterruptedException {
        final AtomicReference<String> name = new AtomicReference<String>();
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);

        Completable c = normal.completable.observeOn(Schedulers.computation());

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onComplete() {
                name.set(Thread.currentThread().getName());
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err.set(e);
                cdl.countDown();
            }
        });

        cdl.await();

        Assert.assertNull(err.get());
        Assert.assertTrue(name.get().startsWith("RxComputation"));
    }

    @Test(timeout = 5000)
    public void observeOnError() throws InterruptedException {
        final AtomicReference<String> name = new AtomicReference<String>();
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);

        Completable c = error.completable.observeOn(Schedulers.computation());

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onComplete() {
                name.set(Thread.currentThread().getName());
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                name.set(Thread.currentThread().getName());
                err.set(e);
                cdl.countDown();
            }
        });

        cdl.await();

        Assert.assertTrue(err.get() instanceof TestException);
        Assert.assertTrue(name.get().startsWith("RxComputation"));
    }

    @Test(timeout = 5000)
    public void onErrorComplete() {
        Completable c = error.completable.onErrorComplete();

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void onErrorCompleteFalse() {
        Completable c = error.completable.onErrorComplete(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) {
                return e instanceof IllegalStateException;
            }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void onErrorCompleteNull() {
        error.completable.onErrorComplete(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextNull() {
        error.completable.onErrorResumeNext(null);
    }

    @Test(timeout = 5000)
    public void onErrorResumeNextFunctionReturnsNull() {
        Completable c = error.completable.onErrorResumeNext(new Function<Throwable, Completable>() {
            @Override
            public Completable apply(Throwable e) {
                return null;
            }
        });

        try {
            c.blockingAwait();
            Assert.fail("Did not throw an exception");
        } catch (NullPointerException ex) {
            Assert.assertTrue(ex.getCause() instanceof TestException);
        }
    }

    @Test(timeout = 5000)
    public void onErrorResumeNextFunctionThrows() {
        Completable c = error.completable.onErrorResumeNext(new Function<Throwable, Completable>() {
            @Override
            public Completable apply(Throwable e) { throw new TestException(); }
        });

        try {
            c.blockingAwait();
            Assert.fail("Did not throw an exception");
        } catch (CompositeException ex) {
            List<Throwable> a = ex.getExceptions();

            Assert.assertEquals(2, a.size());
            Assert.assertTrue(a.get(0) instanceof TestException);
            Assert.assertTrue(a.get(1) instanceof TestException);
        }
    }

    @Test(timeout = 5000)
    public void onErrorResumeNextNormal() {
        Completable c = error.completable.onErrorResumeNext(new Function<Throwable, Completable>() {
            @Override
            public Completable apply(Throwable v) {
                return normal.completable;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void onErrorResumeNextError() {
        Completable c = error.completable.onErrorResumeNext(new Function<Throwable, Completable>() {
            @Override
            public Completable apply(Throwable v) {
                return error.completable;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 2000)
    public void repeatNormal() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final AtomicInteger calls = new AtomicInteger();

        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.getAndIncrement();
                Thread.sleep(100);
                return null;
            }
        }).repeat();

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(final Disposable d) {
                Schedulers.single().scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        d.dispose();
                    }
                }, 550, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onError(Throwable e) {
                err.set(e);
            }

            @Override
            public void onComplete() {

            }
        });

        Assert.assertEquals(6, calls.get());
        Assert.assertNull(err.get());
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void repeatError() {
        Completable c = error.completable.repeat();

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void repeat5Times() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.getAndIncrement();
                return null;
            }
        }).repeat(5);

        c.blockingAwait();

        Assert.assertEquals(5, calls.get());
    }

    @Test(timeout = 5000)
    public void repeat1Time() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.getAndIncrement();
                return null;
            }
        }).repeat(1);

        c.blockingAwait();

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 5000)
    public void repeat0Time() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.getAndIncrement();
                return null;
            }
        }).repeat(0);

        c.blockingAwait();

        Assert.assertEquals(0, calls.get());
    }

    @Test(timeout = 5000)
    public void repeatUntilNormal() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger times = new AtomicInteger(5);

        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.getAndIncrement();
                return null;
            }
        }).repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return times.decrementAndGet() == 0;
            }
        });

        c.blockingAwait();

        Assert.assertEquals(5, calls.get());
    }

    @Test(expected = NullPointerException.class)
    public void repeatUntilNull() {
        normal.completable.repeatUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenNull() {
        normal.completable.repeatWhen(null);
    }

    @Test(timeout = 5000)
    public void retryNormal() {
        Completable c = normal.completable.retry();

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void retry5Times() {
        final AtomicInteger calls = new AtomicInteger(5);
        Completable c = Completable.fromAction(new Action() {
            @Override
            public void run() {
                if (calls.decrementAndGet() != 0) {
                    throw new TestException();
                }
            }
        }).retry();

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void retryBiPredicate5Times() {
        Completable c = error.completable.retry(new BiPredicate<Integer, Throwable>() {
            @Override
            public boolean test(Integer n, Throwable e) {
                return n < 5;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void retryTimes5Error() {
        Completable c = error.completable.retry(5);

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void retryTimes5Normal() {
        final AtomicInteger calls = new AtomicInteger(5);

        Completable c = Completable.fromAction(new Action() {
            @Override
            public void run() {
                if (calls.decrementAndGet() != 0) {
                    throw new TestException();
                }
            }
        }).retry(5);

        c.blockingAwait();
    }

    @Test(expected = IllegalArgumentException.class)
    public void retryNegativeTimes() {
        normal.completable.retry(-1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void retryPredicateError() {
        Completable c = error.completable.retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) {
                return false;
            }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void retryPredicateNull() {
        error.completable.retry((Predicate<Throwable>)null);
    }

    @Test(timeout = 5000)
    public void retryPredicate5Times() {
        final AtomicInteger calls = new AtomicInteger(5);

        Completable c = Completable.fromAction(new Action() {
            @Override
            public void run() {
                if (calls.decrementAndGet() != 0) {
                    throw new TestException();
                }
            }
        }).retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) {
                return true;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void retryWhen5Times() {
        final AtomicInteger calls = new AtomicInteger(5);

        Completable c = Completable.fromAction(new Action() {
            @Override
            public void run() {
                if (calls.decrementAndGet() != 0) {
                    throw new TestException();
                }
            }
        }).retryWhen(new Function<Flowable<? extends Throwable>, Publisher<Object>>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public Publisher<Object> apply(Flowable<? extends Throwable> o) {
                return (Publisher)o;
            }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void subscribe() throws InterruptedException {
        final AtomicBoolean complete = new AtomicBoolean();

        Completable c = normal.completable
                .delay(100, TimeUnit.MILLISECONDS)
                .doOnComplete(new Action() {
                    @Override
                    public void run() {
                        complete.set(true);
                    }
                });

        Disposable d = c.subscribe();

        assertFalse(d.isDisposed());

        Thread.sleep(150);

        Assert.assertTrue("Not completed", complete.get());

        assertTrue(d.isDisposed());
    }

    @Test(timeout = 5000)
    public void subscribeDispose() throws InterruptedException {
        final AtomicBoolean complete = new AtomicBoolean();

        Completable c = normal.completable
                .delay(200, TimeUnit.MILLISECONDS)
                .doOnComplete(new Action() {
                    @Override
                    public void run() {
                        complete.set(true);
                    }
                });

        Disposable d = c.subscribe();

        Thread.sleep(100);

        d.dispose();

        Thread.sleep(150);

        Assert.assertFalse("Completed", complete.get());
    }

    @Test(timeout = 5000)
    public void subscribeTwoCallbacksNormal() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final AtomicBoolean complete = new AtomicBoolean();
        normal.completable.subscribe(new Action() {
            @Override
            public void run() {
                complete.set(true);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                err.set(e);
            }
        });

        Assert.assertNull(err.get());
        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 5000)
    public void subscribeTwoCallbacksError() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final AtomicBoolean complete = new AtomicBoolean();
        error.completable.subscribe(new Action() {
            @Override
            public void run() {
                complete.set(true);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                err.set(e);
            }
        });

        Assert.assertTrue(err.get() instanceof TestException);
        Assert.assertFalse("Not completed", complete.get());
    }

    @Test(expected = NullPointerException.class)
    public void subscribeTwoCallbacksFirstNull() {
        normal.completable.subscribe(new Action() {
            @Override
            public void run() { }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeTwoCallbacksSecondNull() {
        normal.completable.subscribe(new Action() {
            @Override
            public void run() { }
        }, null);
    }

    @Test(timeout = 5000)
    public void subscribeTwoCallbacksCompleteThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
            normal.completable.subscribe(new Action() {
                @Override
                public void run() { throw new TestException(); }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) {
                    err.set(e);
                }
            });

            Assert.assertNull(err.get());
            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test(timeout = 5000)
    public void subscribeTwoCallbacksOnErrorThrows() {
        error.completable.subscribe(new Action() {
            @Override
            public void run() { }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) { throw new TestException(); }
        });
    }

    @Test(timeout = 5000)
    public void subscribeObserverNormal() {
        TestObserver<Object> ts = new TestObserver<Object>();

        normal.completable.toObservable().subscribe(ts);

        ts.assertComplete();
        ts.assertNoValues();
        ts.assertNoErrors();
    }

    @Test(timeout = 5000)
    public void subscribeObserverError() {
        TestObserver<Object> ts = new TestObserver<Object>();

        error.completable.toObservable().subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoValues();
        ts.assertError(TestException.class);
    }

    @Test(timeout = 5000)
    public void subscribeActionNormal() {
        final AtomicBoolean run = new AtomicBoolean();

        normal.completable.subscribe(new Action() {
            @Override
            public void run() {
                run.set(true);
            }
        });

        Assert.assertTrue("Not completed", run.get());
    }

    @Test(timeout = 5000)
    public void subscribeActionError() {
        final AtomicBoolean run = new AtomicBoolean();

        error.completable.subscribe(new Action() {
            @Override
            public void run() {
                run.set(true);
            }
        });

        Assert.assertFalse("Completed", run.get());
    }

    @Test(expected = NullPointerException.class)
    public void subscribeActionNull() {
        normal.completable.subscribe((Action)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeSubscriberNull() {
        normal.completable.toFlowable().subscribe((Subscriber<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeObserverNull() {
        normal.completable.toObservable().subscribe((Observer<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeCompletableSubscriberNull() {
        normal.completable.subscribe((CompletableObserver)null);
    }

    @Test(timeout = 5000)
    public void subscribeSubscriberNormal() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        normal.completable.toFlowable().subscribe(ts);

        ts.assertComplete();
        ts.assertNoValues();
        ts.assertNoErrors();
    }

    @Test(timeout = 5000)
    public void subscribeSubscriberError() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        error.completable.toFlowable().subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoValues();
        ts.assertError(TestException.class);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNull() {
        normal.completable.subscribeOn(null);
    }

    @Test(timeout = 5000)
    public void subscribeOnNormal() {
        final AtomicReference<String> name = new  AtomicReference<String>();

        Completable c = Completable.unsafeCreate(new CompletableSource() {
            @Override
            public void subscribe(CompletableObserver s) {
                name.set(Thread.currentThread().getName());
                EmptyDisposable.complete(s);
            }
        }).subscribeOn(Schedulers.computation());

        c.blockingAwait();

        Assert.assertTrue(name.get().startsWith("RxComputation"));
    }

    @Test(timeout = 5000)
    public void subscribeOnError() {
        final AtomicReference<String> name = new  AtomicReference<String>();

        Completable c = Completable.unsafeCreate(new CompletableSource() {
            @Override
            public void subscribe(CompletableObserver s) {
                name.set(Thread.currentThread().getName());
                EmptyDisposable.error(new TestException(), s);
            }
        }).subscribeOn(Schedulers.computation());

        try {
            c.blockingAwait();
            Assert.fail("No exception thrown");
        } catch (TestException ex) {
            // expected
        }

        Assert.assertTrue(name.get().startsWith("RxComputation"));
    }

    @Test(timeout = 5000)
    public void timeoutEmitError() {
        Throwable e = Completable.never().timeout(100, TimeUnit.MILLISECONDS).blockingGet();

        Assert.assertTrue(e instanceof TimeoutException);
    }

    @Test(timeout = 5000)
    public void timeoutSwitchNormal() {
        Completable c = Completable.never().timeout(100, TimeUnit.MILLISECONDS, normal.completable);

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void timeoutTimerCancelled() throws InterruptedException {
        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Thread.sleep(50);
                return null;
            }
        }).timeout(100, TimeUnit.MILLISECONDS, normal.completable);

        c.blockingAwait();

        Thread.sleep(100);

        normal.assertSubscriptions(0);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutUnitNull() {
        normal.completable.timeout(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSchedulerNull() {
        normal.completable.timeout(1, TimeUnit.SECONDS, (Scheduler)null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutOtherNull() {
        normal.completable.timeout(1, TimeUnit.SECONDS, (Completable)null);
    }

    @Test(timeout = 5000)
    public void toNormal() {
        Flowable<Object> flow = normal.completable.to(new Function<Completable, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Completable c) {
                return c.toFlowable();
            }
        });

        flow.blockingForEach(new Consumer<Object>() {
            @Override
            public void accept(Object e) { }
        });
    }

    @Test(expected = NullPointerException.class)
    public void toNull() {
        normal.completable.to(null);
    }

    @Test(timeout = 5000)
    public void toFlowableNormal() {
        normal.completable.toFlowable().blockingForEach(Functions.emptyConsumer());
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void toFlowableError() {
        error.completable.toFlowable().blockingForEach(Functions.emptyConsumer());
    }

    @Test(timeout = 5000)
    public void toObservableNormal() {
        normal.completable.toObservable().blockingForEach(Functions.emptyConsumer());
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void toObservableError() {
        error.completable.toObservable().blockingForEach(Functions.emptyConsumer());
    }

    @Test(timeout = 5000)
    public void toSingleSupplierNormal() {
        Assert.assertEquals(1, normal.completable.toSingle(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }).blockingGet());
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void toSingleSupplierError() {
        error.completable.toSingle(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toSingleSupplierNull() {
        normal.completable.toSingle(null);
    }

    @Test(expected = NullPointerException.class)
    public void toSingleSupplierReturnsNull() {
        normal.completable.toSingle(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = TestException.class)
    public void toSingleSupplierThrows() {
        normal.completable.toSingle(new Callable<Object>() {
            @Override
            public Object call() { throw new TestException(); }
        }).blockingGet();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void toSingleDefaultError() {
        error.completable.toSingleDefault(1).blockingGet();
    }

    @Test(timeout = 5000)
    public void toSingleDefaultNormal() {
        Assert.assertEquals((Integer)1, normal.completable.toSingleDefault(1).blockingGet());
    }

    @Test(expected = NullPointerException.class)
    public void toSingleDefaultNull() {
        normal.completable.toSingleDefault(null);
    }

    @Test(timeout = 5000)
    public void unsubscribeOnNormal() throws InterruptedException {
        final AtomicReference<String> name = new AtomicReference<String>();
        final CountDownLatch cdl = new CountDownLatch(1);

        normal.completable.delay(1, TimeUnit.SECONDS)
        .doOnDispose(new Action() {
            @Override
            public void run() {
                name.set(Thread.currentThread().getName());
                cdl.countDown();
            }
        })
        .unsubscribeOn(Schedulers.computation())
        .subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(final Disposable d) {
                Schedulers.single().scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        d.dispose();
                    }
                }, 100, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        cdl.await();

        Assert.assertTrue(name.get().startsWith("RxComputation"));
    }

    @Test(expected = NullPointerException.class)
    public void ambArrayNull() {
        Completable.ambArray((Completable[])null);
    }

    @Test(timeout = 5000)
    public void ambArrayEmpty() {
        Completable c = Completable.ambArray();

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void ambArraySingleNormal() {
        Completable c = Completable.ambArray(normal.completable);

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void ambArraySingleError() {
        Completable.ambArray(error.completable)
                .test()
                .assertError(TestException.class);
    }

    @Test(timeout = 5000)
    public void ambArrayOneFires() {
        PublishProcessor<Object> ps1 = PublishProcessor.create();
        PublishProcessor<Object> ps2 = PublishProcessor.create();

        Completable c1 = Completable.fromPublisher(ps1);

        Completable c2 = Completable.fromPublisher(ps2);

        Completable c = Completable.ambArray(c1, c2);

        final AtomicBoolean complete = new AtomicBoolean();

        c.subscribe(new Action() {
            @Override
            public void run() {
                complete.set(true);
            }
        });

        Assert.assertTrue("First subject no subscribers", ps1.hasSubscribers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasSubscribers());

        ps1.onComplete();

        Assert.assertFalse("First subject has subscribers", ps1.hasSubscribers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasSubscribers());

        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 5000)
    public void ambArrayOneFiresError() {
        PublishProcessor<Object> ps1 = PublishProcessor.create();
        PublishProcessor<Object> ps2 = PublishProcessor.create();

        Completable c1 = Completable.fromPublisher(ps1);

        Completable c2 = Completable.fromPublisher(ps2);

        Completable c = Completable.ambArray(c1, c2);

        final AtomicReference<Throwable> complete = new AtomicReference<Throwable>();

        c.subscribe(Functions.EMPTY_ACTION, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable v) {
                complete.set(v);
            }
        });

        Assert.assertTrue("First subject no subscribers", ps1.hasSubscribers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasSubscribers());

        ps1.onError(new TestException());

        Assert.assertFalse("First subject has subscribers", ps1.hasSubscribers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasSubscribers());

        Assert.assertTrue("Not completed", complete.get() instanceof TestException);
    }

    @Test(timeout = 5000)
    public void ambArraySecondFires() {
        PublishProcessor<Object> ps1 = PublishProcessor.create();
        PublishProcessor<Object> ps2 = PublishProcessor.create();

        Completable c1 = Completable.fromPublisher(ps1);

        Completable c2 = Completable.fromPublisher(ps2);

        Completable c = Completable.ambArray(c1, c2);

        final AtomicBoolean complete = new AtomicBoolean();

        c.subscribe(new Action() {
            @Override
            public void run() {
                complete.set(true);
            }
        });

        Assert.assertTrue("First subject no subscribers", ps1.hasSubscribers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasSubscribers());

        ps2.onComplete();

        Assert.assertFalse("First subject has subscribers", ps1.hasSubscribers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasSubscribers());

        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 5000)
    public void ambArraySecondFiresError() {
        PublishProcessor<Object> ps1 = PublishProcessor.create();
        PublishProcessor<Object> ps2 = PublishProcessor.create();

        Completable c1 = Completable.fromPublisher(ps1);

        Completable c2 = Completable.fromPublisher(ps2);

        Completable c = Completable.ambArray(c1, c2);

        final AtomicReference<Throwable> complete = new AtomicReference<Throwable>();

        c.subscribe(Functions.EMPTY_ACTION, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable v) {
                complete.set(v);
            }
        });

        Assert.assertTrue("First subject no subscribers", ps1.hasSubscribers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasSubscribers());

        ps2.onError(new TestException());

        Assert.assertFalse("First subject has subscribers", ps1.hasSubscribers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasSubscribers());

        Assert.assertTrue("Not completed", complete.get() instanceof TestException);
    }

    @Test(timeout = 5000)
    public void ambMultipleOneIsNull() {
        Completable.ambArray(null, normal.completable)
                .test()
                .assertError(NullPointerException.class);
    }

    @Test(timeout = 5000)
    public void ambIterableEmpty() {
        Completable c = Completable.amb(Collections.<Completable>emptyList());

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void ambIterableNull() {
        Completable.amb((Iterable<Completable>)null);
    }

    @Test(timeout = 5000)
    public void ambIterableIteratorNull() {
        Completable.amb(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return null;
            }
        }).test().assertError(NullPointerException.class);
    }

    @Test(timeout = 5000)
    public void ambIterableWithNull() {
        Completable.amb(Arrays.asList(null, normal.completable))
            .test()
            .assertError(NullPointerException.class);
    }

    @Test(timeout = 5000)
    public void ambIterableSingle() {
        Completable c = Completable.amb(Collections.singleton(normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void ambIterableMany() {
        Completable c = Completable.amb(Arrays.asList(normal.completable, normal.completable, normal.completable));

        c.blockingAwait();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void ambIterableOneThrows() {
        Completable.amb(Collections.singleton(error.completable))
                .test()
                .assertError(TestException.class);
    }

    @Test(timeout = 5000)
    public void ambIterableManyOneThrows() {
        Completable.amb(Arrays.asList(error.completable, normal.completable))
                .test()
                .assertError(TestException.class);
    }

    @Test
    public void ambIterableIterableThrows() {
        Completable.amb(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                throw new TestException();
            }
        }).test().assertError(TestException.class);
    }

    @Test
    public void ambIterableIteratorHasNextThrows() {
        Completable.amb(new IterableIteratorHasNextThrows())
                .test()
                .assertError(TestException.class);
    }

    @Test
    public void ambIterableIteratorNextThrows() {
        Completable.amb(new IterableIteratorNextThrows())
                .test()
                .assertError(TestException.class);
    }

    @Test(expected = NullPointerException.class)
    public void ambWithNull() {
        normal.completable.ambWith(null);
    }

    @Test(timeout = 5000)
    public void ambWithArrayOneFires() {
        PublishProcessor<Object> ps1 = PublishProcessor.create();
        PublishProcessor<Object> ps2 = PublishProcessor.create();

        Completable c1 = Completable.fromPublisher(ps1);

        Completable c2 = Completable.fromPublisher(ps2);

        Completable c = c1.ambWith(c2);

        final AtomicBoolean complete = new AtomicBoolean();

        c.subscribe(new Action() {
            @Override
            public void run() {
                complete.set(true);
            }
        });

        Assert.assertTrue("First subject no subscribers", ps1.hasSubscribers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasSubscribers());

        ps1.onComplete();

        Assert.assertFalse("First subject has subscribers", ps1.hasSubscribers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasSubscribers());

        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 5000)
    public void ambWithArrayOneFiresError() {
        PublishProcessor<Object> ps1 = PublishProcessor.create();
        PublishProcessor<Object> ps2 = PublishProcessor.create();

        Completable c1 = Completable.fromPublisher(ps1);

        Completable c2 = Completable.fromPublisher(ps2);

        Completable c = c1.ambWith(c2);

        final AtomicReference<Throwable> complete = new AtomicReference<Throwable>();

        c.subscribe(Functions.EMPTY_ACTION, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable v) {
                complete.set(v);
            }
        });

        Assert.assertTrue("First subject no subscribers", ps1.hasSubscribers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasSubscribers());

        ps1.onError(new TestException());

        Assert.assertFalse("First subject has subscribers", ps1.hasSubscribers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasSubscribers());

        Assert.assertTrue("Not completed", complete.get() instanceof TestException);
    }

    @Test(timeout = 5000)
    public void ambWithArraySecondFires() {
        PublishProcessor<Object> ps1 = PublishProcessor.create();
        PublishProcessor<Object> ps2 = PublishProcessor.create();

        Completable c1 = Completable.fromPublisher(ps1);

        Completable c2 = Completable.fromPublisher(ps2);

        Completable c = c1.ambWith(c2);

        final AtomicBoolean complete = new AtomicBoolean();

        c.subscribe(new Action() {
            @Override
            public void run() {
                complete.set(true);
            }
        });

        Assert.assertTrue("First subject no subscribers", ps1.hasSubscribers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasSubscribers());

        ps2.onComplete();

        Assert.assertFalse("First subject has subscribers", ps1.hasSubscribers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasSubscribers());

        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 5000)
    public void ambWithArraySecondFiresError() {
        PublishProcessor<Object> ps1 = PublishProcessor.create();
        PublishProcessor<Object> ps2 = PublishProcessor.create();

        Completable c1 = Completable.fromPublisher(ps1);

        Completable c2 = Completable.fromPublisher(ps2);

        Completable c = c1.ambWith(c2);

        final AtomicReference<Throwable> complete = new AtomicReference<Throwable>();

        c.subscribe(Functions.EMPTY_ACTION, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable v) {
                complete.set(v);
            }
        });

        Assert.assertTrue("First subject no subscribers", ps1.hasSubscribers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasSubscribers());

        ps2.onError(new TestException());

        Assert.assertFalse("First subject has subscribers", ps1.hasSubscribers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasSubscribers());

        Assert.assertTrue("Not completed", complete.get() instanceof TestException);
    }

    @Test(timeout = 5000)
    public void startWithCompletableNormal() {
        final AtomicBoolean run = new AtomicBoolean();
        Completable c = normal.completable
                .startWith(Completable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        run.set(normal.get() == 0);
                        return null;
                    }
                }));

        c.blockingAwait();

        Assert.assertTrue("Did not start with other", run.get());
        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void startWithCompletableError() {
        Completable c = normal.completable.startWith(error.completable);

        try {
            c.blockingAwait();
            Assert.fail("Did not throw TestException");
        } catch (TestException ex) {
            normal.assertSubscriptions(0);
            error.assertSubscriptions(1);
        }
    }

    @Test(timeout = 5000)
    public void startWithFlowableNormal() {
        final AtomicBoolean run = new AtomicBoolean();
        Flowable<Object> c = normal.completable
                .startWith(Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        run.set(normal.get() == 0);
                        return 1;
                    }
                }));

        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        c.subscribe(ts);

        Assert.assertTrue("Did not start with other", run.get());
        normal.assertSubscriptions(1);

        ts.assertValue(1);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test(timeout = 5000)
    public void startWithFlowableError() {
        Flowable<Object> c = normal.completable
                .startWith(Flowable.error(new TestException()));

        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        c.subscribe(ts);

        normal.assertSubscriptions(0);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test(timeout = 5000)
    public void startWithObservableNormal() {
        final AtomicBoolean run = new AtomicBoolean();
        Observable<Object> c = normal.completable
                .startWith(Observable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        run.set(normal.get() == 0);
                        return 1;
                    }
                }));

        TestObserver<Object> ts = new TestObserver<Object>();

        c.subscribe(ts);

        Assert.assertTrue("Did not start with other", run.get());
        normal.assertSubscriptions(1);

        ts.assertValue(1);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test(timeout = 5000)
    public void startWithObservableError() {
        Observable<Object> c = normal.completable
                .startWith(Observable.error(new TestException()));

        TestObserver<Object> ts = new TestObserver<Object>();

        c.subscribe(ts);

        normal.assertSubscriptions(0);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test(expected = NullPointerException.class)
    public void startWithCompletableNull() {
        normal.completable.startWith((Completable)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithFlowableNull() {
        normal.completable.startWith((Flowable<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithObservableNull() {
        normal.completable.startWith((Observable<Object>)null);
    }

    @Test
    public void andThen() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        Completable.complete().andThen(Flowable.just("foo")).subscribe(ts);
        ts.request(1);
        ts.assertValue("foo");
        ts.assertComplete();
        ts.assertNoErrors();

        TestObserver<String> to = new TestObserver<String>();
        Completable.complete().andThen(Observable.just("foo")).subscribe(to);
        to.assertValue("foo");
        to.assertComplete();
        to.assertNoErrors();
    }

    private static void expectUncaughtTestException(Action action) {
        Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        CapturingUncaughtExceptionHandler handler = new CapturingUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        try {
            action.run();
            assertEquals("Should have received exactly 1 exception", 1, handler.count);
            Throwable caught = handler.caught;
            while (caught != null) {
                if (caught instanceof TestException) { break; }
                if (caught == caught.getCause()) { break; }
                caught = caught.getCause();
            }
            assertTrue("A TestException should have been delivered to the handler",
                    caught instanceof TestException);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
    }

    @Test
    public void subscribeOneActionThrowFromOnCompleted() {
        expectUncaughtTestException(new Action() {
            @Override
            public void run() {
                normal.completable.subscribe(new Action() {
                    @Override
                    public void run() {
                        throw new TestException();
                    }
                });
            }
        });
    }

    @Test
    public void subscribeTwoActionsThrowFromOnError() {
        expectUncaughtTestException(new Action() {
            @Override
            public void run() {
                error.completable.subscribe(
                new Action() {
                    @Override
                    public void run() {
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throw new TestException();
                    }
                });
            }
        });
    }

    @Test
    public void propagateExceptionSubscribeOneAction() {
        expectUncaughtTestException(new Action() {
            @Override
            public void run() {
                error.completable.toSingleDefault(1).subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
                    }
                });
            }
        });
    }

    @Test
    public void usingFactoryReturnsNullAndDisposerThrows() {
        Consumer<Integer> onDispose = new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                throw new TestException();
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Completable.using(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        },
        new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) {
                return null;
            }
        }, onDispose).<Integer>toFlowable().subscribe(ts);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(CompositeException.class);

        CompositeException ex = (CompositeException)ts.errors().get(0);

        List<Throwable> listEx = ex.getExceptions();

        assertEquals(2, listEx.size());

        assertTrue(listEx.get(0).toString(), listEx.get(0) instanceof NullPointerException);
        assertTrue(listEx.get(1).toString(), listEx.get(1) instanceof TestException);
    }

    @Test
    public void subscribeReportsUnsubscribedOnError() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        Disposable completableSubscription = completable.subscribe();

        stringSubject.onError(new TestException());

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
    }

    @Test
    public void subscribeActionReportsUnsubscribed() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        Disposable completableSubscription = completable.subscribe(new Action() {
            @Override
            public void run() {

            }
        });

        stringSubject.onComplete();

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
    }

    @Test
    public void subscribeActionReportsUnsubscribedAfter() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        final AtomicReference<Disposable> subscriptionRef = new AtomicReference<Disposable>();
        Disposable completableSubscription = completable.subscribe(new Action() {
            @Override
            public void run() {
                if (subscriptionRef.get().isDisposed()) {
                    subscriptionRef.set(null);
                }
            }
        });
        subscriptionRef.set(completableSubscription);

        stringSubject.onComplete();

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
        assertNotNull("Unsubscribed before the call to onComplete", subscriptionRef.get());
    }

    @Test
    public void subscribeActionReportsUnsubscribedOnError() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        Disposable completableSubscription = completable.subscribe(new Action() {
            @Override
            public void run() {
            }
        });

        stringSubject.onError(new TestException());

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
    }

    @Test
    public void subscribeAction2ReportsUnsubscribed() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        Disposable completableSubscription = completable.subscribe(new Action() {
            @Override
            public void run() {

            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) {

            }
        });

        stringSubject.onComplete();

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
    }

    @Test
    public void subscribeAction2ReportsUnsubscribedOnError() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        Disposable completableSubscription = completable.subscribe(new Action() {
            @Override
            public void run() { }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) { }
        });

        stringSubject.onError(new TestException());

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
    }

    @Test
    public void andThenSubscribeOn() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        TestScheduler scheduler = new TestScheduler();
        Completable.complete().andThen(Flowable.just("foo").delay(1, TimeUnit.SECONDS, scheduler)).subscribe(ts);

        ts.request(1);
        ts.assertNoValues();
        ts.assertNotTerminated();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValue("foo");
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void andThenSingleNever() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        Completable.never().andThen(Single.just("foo")).toFlowable().subscribe(ts);
        ts.request(1);
        ts.assertNoValues();
        ts.assertNotTerminated();
    }

    @Test
    public void andThenSingleError() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        final AtomicBoolean hasRun = new AtomicBoolean(false);
        final Exception e = new Exception();
        Completable.error(e)
            .andThen(new Single<String>() {
                @Override
                public void subscribeActual(SingleObserver<? super String> s) {
                    hasRun.set(true);
                    s.onSuccess("foo");
                }
            })
            .toFlowable().subscribe(ts);
        ts.assertNoValues();
        ts.assertError(e);
        Assert.assertFalse("Should not have subscribed to single when completable errors", hasRun.get());
    }

    @Test
    public void andThenSingleSubscribeOn() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        TestScheduler scheduler = new TestScheduler();
        Completable.complete().andThen(Single.just("foo").delay(1, TimeUnit.SECONDS, scheduler)).toFlowable().subscribe(ts);

        ts.request(1);
        ts.assertNoValues();
        ts.assertNotTerminated();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValue("foo");
        ts.assertComplete();
        ts.assertNoErrors();
    }

    private Function<Completable, Completable> onCreate;

    private BiFunction<Completable, CompletableObserver, CompletableObserver> onStart;

    @Before
    public void setUp() throws Exception {
        onCreate = spy(new Function<Completable, Completable>() {
            @Override
            public Completable apply(Completable t) {
                return t;
            }
        });

        RxJavaPlugins.setOnCompletableAssembly(onCreate);

        onStart = spy(new BiFunction<Completable, CompletableObserver, CompletableObserver>() {
            @Override
            public CompletableObserver apply(Completable t1, CompletableObserver t2) {
                return t2;
            }
        });

        RxJavaPlugins.setOnCompletableSubscribe(onStart);
    }

    @After
    public void after() {
        RxJavaPlugins.reset();
    }

    @Test
    public void testHookCreate() throws Exception {
        CompletableSource subscriber = mock(CompletableSource.class);
        Completable create = Completable.unsafeCreate(subscriber);

        verify(onCreate, times(1)).apply(create);
    }

    @Test(timeout = 5000)
    public void doOnCompletedNormal() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = normal.completable.doOnComplete(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        c.blockingAwait();

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 5000)
    public void doOnCompletedError() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = error.completable.doOnComplete(new Action() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        try {
            c.blockingAwait();
            Assert.fail("Failed to throw TestException");
        } catch (TestException ex) {
            // expected
        }

        Assert.assertEquals(0, calls.get());
    }

    @Test(expected = NullPointerException.class)
    public void doOnCompletedNull() {
        normal.completable.doOnComplete(null);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void doOnCompletedThrows() {
        Completable c = normal.completable.doOnComplete(new Action() {
            @Override
            public void run() { throw new TestException(); }
        });

        c.blockingAwait();
    }

    @Test(timeout = 5000)
    public void doAfterTerminateNormal() {
        final AtomicBoolean doneAfter = new AtomicBoolean();
        final AtomicBoolean complete = new AtomicBoolean();

        Completable c = normal.completable.doAfterTerminate(new Action() {
            @Override
            public void run() {
                doneAfter.set(complete.get());
            }
        });

        c.subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {
                complete.set(true);
            }
        });

        c.blockingAwait();

        Assert.assertTrue("Not completed", complete.get());
        Assert.assertTrue("Closure called before onComplete", doneAfter.get());
    }

    @Test
    public void doAfterTerminateWithError() {
        final AtomicBoolean doneAfter = new AtomicBoolean();

        Completable c = error.completable.doAfterTerminate(new Action() {
            @Override
            public void run() {
                doneAfter.set(true);
            }
        });

        try {
            c.blockingAwait(5, TimeUnit.SECONDS);
            Assert.fail("Did not throw TestException");
        } catch (TestException ex) {
            // expected
        }

        Assert.assertTrue("Closure not called", doneAfter.get());
    }

    @Test(expected = NullPointerException.class)
    public void doAfterTerminateNull() {
        normal.completable.doAfterTerminate(null);
    }

    @Test
    public void subscribeEmptyOnError() {
        expectUncaughtTestException(new Action() {
            @Override public void run() {
                error.completable.subscribe();
            }
        });
    }

    @Test
    public void subscribeOneActionOnError() {
        expectUncaughtTestException(new Action() {
            @Override
            public void run() {
                error.completable.subscribe(new Action() {
                    @Override
                    public void run() {
                    }
                });
            }
        });
    }

    @Test
    public void propagateExceptionSubscribeEmpty() {
        expectUncaughtTestException(new Action() {
            @Override
            public void run() {
                error.completable.toSingleDefault(0).subscribe();
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void andThenCompletableNull() {
        normal.completable.andThen((Completable)null);
    }

    @Test(expected = NullPointerException.class)
    public void andThenFlowableNull() {
        normal.completable.andThen((Observable<Object>)null);
    }

    @Test(timeout = 5000)
    public void andThenCompletableNormal() {
        final AtomicBoolean run = new AtomicBoolean();
        Completable c = normal.completable
                .andThen(Completable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        run.set(normal.get() == 0);
                        return null;
                    }
                }));

        c.blockingAwait();

        Assert.assertFalse("Start with other", run.get());
        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000)
    public void andThenCompletableError() {
        Completable c = normal.completable.andThen(error.completable);

        try {
            c.blockingAwait();
            Assert.fail("Did not throw TestException");
        } catch (TestException ex) {
            normal.assertSubscriptions(1);
            error.assertSubscriptions(1);
        }
    }

    @Test(timeout = 5000)
    public void andThenFlowableNormal() {
        final AtomicBoolean run = new AtomicBoolean();
        Flowable<Object> c = normal.completable
                .andThen(Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        run.set(normal.get() == 0);
                        return 1;
                    }
                }));

        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        c.subscribe(ts);

        Assert.assertFalse("Start with other", run.get());
        normal.assertSubscriptions(1);

        ts.assertValue(1);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test(timeout = 5000)
    public void andThenFlowableError() {
        Flowable<Object> c = normal.completable
                .andThen(Flowable.error(new TestException()));

        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        c.subscribe(ts);

        normal.assertSubscriptions(1);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void usingFactoryThrows() throws Exception {
        @SuppressWarnings("unchecked")
        Consumer<Integer> onDispose = mock(Consumer.class);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Completable.using(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        },
        new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) {
                throw new TestException();
            }
        }, onDispose).<Integer>toFlowable().subscribe(ts);

        verify(onDispose).accept(1);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void usingFactoryAndDisposerThrow() {
        Consumer<Integer> onDispose = new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                throw new TestException();
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Completable.using(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        },
        new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) {
                throw new TestException();
            }
        }, onDispose).<Integer>toFlowable().subscribe(ts);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(CompositeException.class);

        CompositeException ex = (CompositeException)ts.errors().get(0);

        List<Throwable> listEx = ex.getExceptions();

        assertEquals(2, listEx.size());

        assertTrue(listEx.get(0).toString(), listEx.get(0) instanceof TestException);
        assertTrue(listEx.get(1).toString(), listEx.get(1) instanceof TestException);
    }

    @Test
    public void usingFactoryReturnsNull() throws Exception {
        @SuppressWarnings("unchecked")
        Consumer<Integer> onDispose = mock(Consumer.class);

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Completable.using(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        },
        new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) {
                return null;
            }
        }, onDispose).<Integer>toFlowable().subscribe(ts);

        verify(onDispose).accept(1);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(NullPointerException.class);
    }

    @Test
    public void subscribeReportsUnsubscribed() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        Disposable completableSubscription = completable.subscribe();

        stringSubject.onComplete();

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
    }

    @Ignore("onXXX methods are not allowed to throw")
    @Test
    public void safeOnCompleteThrows() {
        /*
        try {
            normal.completable.subscribe(new CompletableSubscriber() {

                @Override
                public void onComplete() {
                    throw new TestException("Forced failure");
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onSubscribe(Subscription d) {

                }

            });
            Assert.fail("Did not propagate exception!");
        } catch (OnCompletedFailedException ex) {
            Throwable c = ex.getCause();
            Assert.assertNotNull(c);

            Assert.assertEquals("Forced failure", c.getMessage());
        }
        */
    }

    @Ignore("onXXX methods are not allowed to throw")
    @Test
    public void safeOnErrorThrows() {
        /*
        try {
            error.completable.subscribe(new CompletableSubscriber() {

                @Override
                public void onComplete() {
                }

                @Override
                public void onError(Throwable e) {
                    throw new TestException("Forced failure");
                }

                @Override
                public void onSubscribe(Subscription d) {

                }

            });
            Assert.fail("Did not propagate exception!");
        } catch (OnErrorFailedException ex) {
            Throwable c = ex.getCause();
            Assert.assertTrue("" + c, c instanceof CompositeException);

            CompositeException ce = (CompositeException)c;

            List<Throwable> list = ce.getExceptions();

            Assert.assertEquals(2, list.size());

            Assert.assertTrue("" + list.get(0), list.get(0) instanceof TestException);
            Assert.assertNull(list.get(0).getMessage());

            Assert.assertTrue("" + list.get(1), list.get(1) instanceof TestException);
            Assert.assertEquals("Forced failure", list.get(1).getMessage());
        }
        */
    }

    @Test
    public void testHookSubscribeStart() throws Exception {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Completable completable = Completable.unsafeCreate(new CompletableSource() {
            @Override public void subscribe(CompletableObserver s) {
                s.onComplete();
            }
        });
        completable.<String>toFlowable().subscribe(ts);

        verify(onStart, times(1)).apply(eq(completable), any(CompletableObserver.class));
    }

    @Ignore("No unsafeSubscribe")
    @Test
    public void testHookUnsafeSubscribeStart() {
        /*
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Completable completable = Completable.create(new CompletableOnSubscribe() {
            @Override public void call(CompletableSubscriber s) {
                s.onComplete();
            }
        });
        completable.unsafeSubscribe(ts);

        verify(onStart, times(1)).call(eq(completable), any(Completable.CompletableOnSubscribe.class));
        */
    }

    @Test
    public void onStartCalledSafe() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>() {
            @Override
            public void onStart() {
                onNext(1);
            }
        };

        normal.completable.<Object>toFlowable().subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Ignore("No unsafeSubscribe")
    @Test
    public void onStartCalledUnsafeSafe() {
        /*
        TestSubscriber<Object> ts = new TestSubscriber<Object>() {
            @Override
            public void onStart() {
                onNext(1);
            }
        };

        normal.completable.unsafeSubscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
        */
    }

    @Test
    public void onErrorCompleteFunctionThrows() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        error.completable.onErrorComplete(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable t) {
                throw new TestException("Forced inner failure");
            }
        }).<String>toFlowable().subscribe(ts);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(CompositeException.class);

        CompositeException composite = (CompositeException)ts.errors().get(0);

        List<Throwable> errors = composite.getExceptions();
        Assert.assertEquals(2, errors.size());

        Assert.assertTrue(errors.get(0).toString(), errors.get(0) instanceof TestException);
        Assert.assertEquals(errors.get(0).toString(), null, errors.get(0).getMessage());
        Assert.assertTrue(errors.get(1).toString(), errors.get(1) instanceof TestException);
        Assert.assertEquals(errors.get(1).toString(), "Forced inner failure", errors.get(1).getMessage());
    }

    @Test
    public void subscribeAction2ReportsUnsubscribedAfter() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        final AtomicReference<Disposable> subscriptionRef = new AtomicReference<Disposable>();
        Disposable completableSubscription = completable.subscribe(new Action() {
            @Override
            public void run() {
                if (subscriptionRef.get().isDisposed()) {
                    subscriptionRef.set(null);
                }
            }
        }, Functions.emptyConsumer());
        subscriptionRef.set(completableSubscription);

        stringSubject.onComplete();

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
        assertNotNull("Unsubscribed before the call to onComplete", subscriptionRef.get());
    }

    @Test
    public void subscribeAction2ReportsUnsubscribedOnErrorAfter() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.ignoreElements();

        final AtomicReference<Disposable> subscriptionRef = new AtomicReference<Disposable>();
        Disposable completableSubscription = completable.subscribe(Functions.EMPTY_ACTION,
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                if (subscriptionRef.get().isDisposed()) {
                    subscriptionRef.set(null);
                }
            }
        });
        subscriptionRef.set(completableSubscription);

        stringSubject.onError(new TestException());

        assertTrue("Not unsubscribed?", completableSubscription.isDisposed());
        assertNotNull("Unsubscribed before the call to onError", subscriptionRef.get());
    }

    @Ignore("onXXX methods are not allowed to throw")
    @Test
    public void safeOnCompleteThrowsRegularSubscriber() {
        /*
        try {
            normal.completable.subscribe(new FlowableSubscriber<Object>() {

                @Override
                public void onComplete() {
                    throw new TestException("Forced failure");
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Object t) {

                }
            });
            Assert.fail("Did not propagate exception!");
        } catch (OnCompletedFailedException ex) {
            Throwable c = ex.getCause();
            Assert.assertNotNull(c);

            Assert.assertEquals("Forced failure", c.getMessage());
        }
        */
    }

    @Ignore("onXXX methods are not allowed to throw")
    @Test
    public void safeOnErrorThrowsRegularSubscriber() {
        /*
        try {
            error.completable.subscribe(new FlowableSubscriber<Object>() {

                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable e) {
                    throw new TestException("Forced failure");
                }

                @Override
                public void onNext(Object t) {

                }
            });
            Assert.fail("Did not propagate exception!");
        } catch (OnErrorFailedException ex) {
            Throwable c = ex.getCause();
            Assert.assertTrue("" + c, c instanceof CompositeException);

            CompositeException ce = (CompositeException)c;

            List<Throwable> list = ce.getExceptions();

            Assert.assertEquals(2, list.size());

            Assert.assertTrue("" + list.get(0), list.get(0) instanceof TestException);
            Assert.assertNull(list.get(0).getMessage());

            Assert.assertTrue("" + list.get(1), list.get(1) instanceof TestException);
            Assert.assertEquals("Forced failure", list.get(1).getMessage());
        }
        */
    }

    @Test
    public void propagateExceptionSubscribeOneActionThrowFromOnSuccess() {
        expectUncaughtTestException(new Action() {
            @Override
            public void run() {
                normal.completable.toSingleDefault(1).subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
                        throw new TestException();
                    }
                });
            }
        });
    }

    @Test
    public void andThenNever() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        Completable.never().andThen(Flowable.just("foo")).subscribe(ts);
        ts.request(1);
        ts.assertNoValues();
        ts.assertNotTerminated();
    }

    @Test
    public void andThenError() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        final AtomicBoolean hasRun = new AtomicBoolean(false);
        final Exception e = new Exception();
        Completable.unsafeCreate(new CompletableSource() {
                @Override
                public void subscribe(CompletableObserver cs) {
                    cs.onError(e);
                }
            })
            .andThen(Flowable.<String>unsafeCreate(new Publisher<String>() {
                @Override
                public void subscribe(Subscriber<? super String> s) {
                    hasRun.set(true);
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext("foo");
                    s.onComplete();
                }
            }))
            .subscribe(ts);
        ts.assertNoValues();
        ts.assertError(e);
        Assert.assertFalse("Should not have subscribed to observable when completable errors", hasRun.get());
    }

    @Test
    public void andThenSingle() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        Completable.complete().andThen(Single.just("foo")).toFlowable().subscribe(ts);
        ts.request(1);
        ts.assertValue("foo");
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureNull() {
        Completable.fromFuture(null);
    }

    @Test(timeout = 5000)
    public void fromFutureNormal() {
        ExecutorService exec = Executors.newSingleThreadExecutor();

        try {
            Completable c = Completable.fromFuture(exec.submit(new Runnable() {
                @Override
                public void run() {
                    // no action
                }
            }));

            c.blockingAwait();
        } finally {
            exec.shutdown();
        }
    }

    @Test(timeout = 5000)
    public void fromFutureThrows() {
        ExecutorService exec = Executors.newSingleThreadExecutor();

        Completable c = Completable.fromFuture(exec.submit(new Runnable() {
            @Override
            public void run() {
                throw new TestException();
            }
        }));

        try {
            c.blockingAwait();
            Assert.fail("Failed to throw Exception");
        } catch (RuntimeException ex) {
            if (!((ex.getCause() instanceof ExecutionException) && (ex.getCause().getCause() instanceof TestException))) {
                ex.printStackTrace();
                Assert.fail("Wrong exception received");
            }
        } finally {
            exec.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void fromRunnableNull() {
        Completable.fromRunnable(null);
    }

    @Test(timeout = 5000)
    public void fromRunnableNormal() {
        final AtomicInteger calls = new AtomicInteger();

        Completable c = Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                calls.getAndIncrement();
            }
        });

        c.blockingAwait();

        Assert.assertEquals(1, calls.get());
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void fromRunnableThrows() {
        Completable c = Completable.fromRunnable(new Runnable() {
            @Override
            public void run() { throw new TestException(); }
        });

        c.blockingAwait();
    }

    @Test(expected = NullPointerException.class)
    public void doOnErrorNullValue() {
        Completable.complete().doOnError(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnSubscribeNullValue() {
        Completable.complete().doOnSubscribe(null);
    }

    @Test(expected = NullPointerException.class)
    public void doAfterTerminateNullValue() {
        Completable.complete().doAfterTerminate(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnTerminateNullValue() {
        Completable.complete().doOnTerminate(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnCompleteNullValue() {
        Completable.complete().doOnComplete(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnDisposeNullValue() {
        Completable.complete().doOnDispose(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnEventNullValue() {
        Completable.complete().doOnEvent(null);
    }

    @Test
    public void doOnEventComplete() {
        final AtomicInteger atomicInteger = new AtomicInteger(0);

        Completable.complete().doOnEvent(new Consumer<Throwable>() {
            @Override
            public void accept(final Throwable throwable) throws Exception {
                if (throwable == null) {
                    atomicInteger.incrementAndGet();
                }
            }
        }).subscribe();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void doOnEventError() {
        final AtomicInteger atomicInteger = new AtomicInteger(0);

        Completable.error(new RuntimeException()).doOnEvent(new Consumer<Throwable>() {
            @Override
            public void accept(final Throwable throwable) throws Exception {
                if (throwable != null) {
                    atomicInteger.incrementAndGet();
                }
            }
        }).subscribe();

        assertEquals(1, atomicInteger.get());
    }


    @Test(timeout = 5000)
    public void subscribeTwoCallbacksDispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        Disposable d = pp.ignoreElements().subscribe(Functions.EMPTY_ACTION, Functions.emptyConsumer());

        assertFalse(d.isDisposed());
        assertTrue(pp.hasSubscribers());

        d.dispose();

        assertTrue(d.isDisposed());
        assertFalse(pp.hasSubscribers());
    }

}
