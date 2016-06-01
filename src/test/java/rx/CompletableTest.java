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

package rx;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import rx.Completable.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.*;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.plugins.*;
import rx.schedulers.*;
import rx.subjects.PublishSubject;
import rx.subscriptions.*;

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
                    throw new UnsupportedOperationException();
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
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * A class containing a completable instance and counts the number of subscribers.
     */
    static final class NormalCompletable extends AtomicInteger {
        /** */
        private static final long serialVersionUID = 7192337844700923752L;
        
        public final Completable completable = Completable.create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) {
                getAndIncrement();
                s.onSubscribe(Subscriptions.unsubscribed());
                s.onCompleted();
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
        /** */
        private static final long serialVersionUID = 7192337844700923752L;
        
        public final Completable completable = Completable.create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) {
                getAndIncrement();
                s.onSubscribe(Subscriptions.unsubscribed());
                s.onError(new TestException());
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

    @Test(timeout = 1000)
    public void complete() {
        Completable c = Completable.complete();
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void concatNull() {
        Completable.concat((Completable[])null);
    }
    
    @Test(timeout = 1000)
    public void concatEmpty() {
        Completable c = Completable.concat();
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void concatSingleSource() {
        Completable c = Completable.concat(normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void concatSingleSourceThrows() {
        Completable c = Completable.concat(error.completable);
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void concatMultipleSources() {
        Completable c = Completable.concat(normal.completable, normal.completable, normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void concatMultipleOneThrows() {
        Completable c = Completable.concat(normal.completable, error.completable, normal.completable);
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void concatMultipleOneIsNull() {
        Completable c = Completable.concat(normal.completable, null);
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void concatIterableEmpty() {
        Completable c = Completable.concat(Collections.<Completable>emptyList());
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void concatIterableNull() {
        Completable.concat((Iterable<Completable>)null);
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Completable c = Completable.concat(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return null;
            }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void concatIterableWithNull() {
        Completable c = Completable.concat(Arrays.asList(normal.completable, (Completable)null));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void concatIterableSingle() {
        Completable c = Completable.concat(Collections.singleton(normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000)
    public void concatIterableMany() {
        Completable c = Completable.concat(Arrays.asList(normal.completable, normal.completable, normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void concatIterableOneThrows() {
        Completable c = Completable.concat(Collections.singleton(error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void concatIterableManyOneThrows() {
        Completable c = Completable.concat(Arrays.asList(normal.completable, error.completable));
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void concatIterableIterableThrows() {
        Completable c = Completable.concat(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                throw new TestException();
            }
        });
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void concatIterableIteratorHasNextThrows() {
        Completable c = Completable.concat(new IterableIteratorHasNextThrows());
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void concatIterableIteratorNextThrows() {
        Completable c = Completable.concat(new IterableIteratorNextThrows());
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void concatObservableEmpty() {
        Completable c = Completable.concat(Observable.<Completable>empty());
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void concatObservableError() {
        Completable c = Completable.concat(Observable.<Completable>error(new TestException()));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void concatObservableSingle() {
        Completable c = Completable.concat(Observable.just(normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void concatObservableSingleThrows() {
        Completable c = Completable.concat(Observable.just(error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void concatObservableMany() {
        Completable c = Completable.concat(Observable.just(normal.completable).repeat(3));
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void concatObservableManyOneThrows() {
        Completable c = Completable.concat(Observable.just(normal.completable, error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void concatObservablePrefetch() {
        final List<Long> requested = new ArrayList<Long>();
        Observable<Completable> cs = Observable
                .just(normal.completable)
                .repeat(10)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long v) {
                        requested.add(v);
                    }
                });
        
        Completable c = Completable.concat(cs, 5);
        
        c.await();
        
        // FIXME this request pattern looks odd because all 10 completions trigger 1 requests
        Assert.assertEquals(Arrays.asList(5L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), requested);
    }
    
    @Test
    public void andThen() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        Completable.complete().andThen(Observable.just("foo")).subscribe(ts);
        ts.requestMore(1);
        ts.assertValue("foo");
        ts.assertCompleted();
        ts.assertNoErrors();
    }
    
    @Test
    public void andThenNever() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        Completable.never().andThen(Observable.just("foo")).subscribe(ts);
        ts.requestMore(1);
        ts.assertNoValues();
        ts.assertNoTerminalEvent();
    }
    
    @Test
    public void andThenError() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        final AtomicBoolean hasRun = new AtomicBoolean(false);
        final Exception e = new Exception();
        Completable.create(new CompletableOnSubscribe() {
                @Override
                public void call(CompletableSubscriber cs) {
                    cs.onError(e);
                }
            })
            .andThen(Observable.<String>create(new OnSubscribe<String>() {
                @Override
                public void call(Subscriber<? super String> s) {
                    hasRun.set(true);
                    s.onNext("foo");
                    s.onCompleted();
                }
            }))
            .subscribe(ts);
        ts.assertNoValues();
        ts.assertError(e);
        Assert.assertFalse("Should not have subscribed to observable when completable errors", hasRun.get());
    }
    
    @Test
    public void andThenSubscribeOn() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        TestScheduler scheduler = new TestScheduler();
        Completable.complete().andThen(Observable.just("foo").delay(1, TimeUnit.SECONDS, scheduler)).subscribe(ts);
        ts.requestMore(1);
        ts.assertNoValues();
        ts.assertNoTerminalEvent();
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.assertValue("foo");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void andThenSingle() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        Completable.complete().andThen(Single.just("foo")).subscribe(ts);
        ts.requestMore(1);
        ts.assertValue("foo");
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertUnsubscribed();
    }

    @Test
    public void andThenSingleNever() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        Completable.never().andThen(Single.just("foo")).subscribe(ts);
        ts.requestMore(1);
        ts.assertNoValues();
        ts.assertNoTerminalEvent();
    }

    @Test
    public void andThenSingleError() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        final AtomicBoolean hasRun = new AtomicBoolean(false);
        final Exception e = new Exception();
        Completable.error(e)
            .andThen(Single.<String>create(new Single.OnSubscribe<String>() {
                @Override
                public void call(SingleSubscriber<? super String> s) {
                    hasRun.set(true);
                    s.onSuccess("foo");
                }
            }))
            .subscribe(ts);
        ts.assertNoValues();
        ts.assertError(e);
        ts.assertUnsubscribed();
        Assert.assertFalse("Should not have subscribed to single when completable errors", hasRun.get());
    }

    @Test
    public void andThenSingleSubscribeOn() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);
        TestScheduler scheduler = new TestScheduler();
        Completable.complete().andThen(Single.just("foo").delay(1, TimeUnit.SECONDS, scheduler)).subscribe(ts);
        ts.requestMore(1);
        ts.assertNoValues();
        ts.assertNoTerminalEvent();
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.assertValue("foo");
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertUnsubscribed();
    }
    
    @Test(expected = NullPointerException.class)
    public void createNull() {
        Completable.create(null);
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void createOnSubscribeThrowsNPE() {
        Completable c = Completable.create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) { throw new NullPointerException(); }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void createOnSubscribeThrowsRuntimeException() {
        try {
            Completable c = Completable.create(new CompletableOnSubscribe() {
                @Override
                public void call(CompletableSubscriber s) {
                    throw new TestException();
                }
            });
            
            c.await();
            
            Assert.fail("Did not throw exception");
        } catch (NullPointerException ex) {
            if (!(ex.getCause() instanceof TestException)) {
                ex.printStackTrace();
                Assert.fail("Did not wrap the TestException but it returned: " + ex);
            }
        }
    }
    
    @Test(timeout = 1000)
    public void defer() {
        Completable c = Completable.defer(new Func0<Completable>() {
            @Override
            public Completable call() {
                return normal.completable;
            }
        });
        
        normal.assertSubscriptions(0);
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(expected = NullPointerException.class)
    public void deferNull() {
        Completable.defer(null);
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void deferReturnsNull() {
        Completable c = Completable.defer(new Func0<Completable>() {
            @Override
            public Completable call() {
                return null;
            }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void deferFunctionThrows() {
        Completable c = Completable.defer(new Func0<Completable>() {
            @Override
            public Completable call() { throw new TestException(); }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void deferErrorSource() {
        Completable c = Completable.defer(new Func0<Completable>() {
            @Override
            public Completable call() {
                return error.completable;
            }
        });
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void errorNull() {
        Completable.error((Throwable)null);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void errorNormal() {
        Completable c = Completable.error(new TestException());
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void fromCallableNull() {
        Completable.fromCallable(null);
    }
    
    @Test(timeout = 1000)
    public void fromCallableNormal() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return calls.getAndIncrement();
            }
        });
        
        c.await();
        
        Assert.assertEquals(1, calls.get());
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void fromCallableThrows() {
        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception { throw new TestException(); }
        });
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void fromObservableNull() {
        Completable.fromObservable(null);
    }
    
    @Test(timeout = 1000)
    public void fromObservableEmpty() {
        Completable c = Completable.fromObservable(Observable.empty());
        
        c.await();
    }

    @Test(timeout = 5000)
    public void fromObservableSome() {
        for (int n = 1; n < 10000; n *= 10) {
            Completable c = Completable.fromObservable(Observable.range(1, n));
            
            c.await();
        }
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void fromObservableError() {
        Completable c = Completable.fromObservable(Observable.error(new TestException()));
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void fromFutureNull() {
        Completable.fromFuture(null);
    }
    
    @Test(timeout = 1000)
    public void fromFutureNormal() {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        
        try {
            Completable c = Completable.fromFuture(exec.submit(new Runnable() {
                @Override
                public void run() { 
                    // no action
                }
            }));
            
            c.await();
        } finally {
            exec.shutdown();
        }
    }
    
    @Test(timeout = 1000)
    public void fromFutureThrows() {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        
        Completable c = Completable.fromFuture(exec.submit(new Runnable() {
            @Override
            public void run() { 
                throw new TestException();
            }
        }));
        
        try {
            c.await();
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
    public void fromActionNull() {
        Completable.fromAction(null);
    }
    
    @Test(timeout = 1000)
    public void fromActionNormal() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = Completable.fromAction(new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        });
        
        c.await();
        
        Assert.assertEquals(1, calls.get());
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void fromActionThrows() {
        Completable c = Completable.fromAction(new Action0() {
            @Override
            public void call() { throw new TestException(); }
        });
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void fromSingleNull() {
        Completable.fromSingle(null);
    }
    
    @Test(timeout = 1000)
    public void fromSingleNormal() {
        Completable c = Completable.fromSingle(Single.just(1));
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void fromSingleThrows() {
        Completable c = Completable.fromSingle(Single.error(new TestException()));
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void mergeNull() {
        Completable.merge((Completable[])null);
    }
    
    @Test(timeout = 1000)
    public void mergeEmpty() {
        Completable c = Completable.merge();
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeSingleSource() {
        Completable c = Completable.merge(normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeSingleSourceThrows() {
        Completable c = Completable.merge(error.completable);
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeMultipleSources() {
        Completable c = Completable.merge(normal.completable, normal.completable, normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeMultipleOneThrows() {
        Completable c = Completable.merge(normal.completable, error.completable, normal.completable);
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void mergeMultipleOneIsNull() {
        Completable c = Completable.merge(normal.completable, null);
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeIterableEmpty() {
        Completable c = Completable.merge(Collections.<Completable>emptyList());
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void mergeIterableNull() {
        Completable.merge((Iterable<Completable>)null);
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void mergeIterableIteratorNull() {
        Completable c = Completable.merge(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return null;
            }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void mergeIterableWithNull() {
        Completable c = Completable.merge(Arrays.asList(normal.completable, (Completable)null));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeIterableSingle() {
        Completable c = Completable.merge(Collections.singleton(normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000)
    public void mergeIterableMany() {
        Completable c = Completable.merge(Arrays.asList(normal.completable, normal.completable, normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeIterableOneThrows() {
        Completable c = Completable.merge(Collections.singleton(error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeIterableManyOneThrows() {
        Completable c = Completable.merge(Arrays.asList(normal.completable, error.completable));
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void mergeIterableIterableThrows() {
        Completable c = Completable.merge(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                throw new TestException();
            }
        });
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void mergeIterableIteratorHasNextThrows() {
        Completable c = Completable.merge(new IterableIteratorHasNextThrows());
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void mergeIterableIteratorNextThrows() {
        Completable c = Completable.merge(new IterableIteratorNextThrows());
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeObservableEmpty() {
        Completable c = Completable.merge(Observable.<Completable>empty());
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeObservableError() {
        Completable c = Completable.merge(Observable.<Completable>error(new TestException()));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeObservableSingle() {
        Completable c = Completable.merge(Observable.just(normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeObservableSingleThrows() {
        Completable c = Completable.merge(Observable.just(error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeObservableMany() {
        Completable c = Completable.merge(Observable.just(normal.completable).repeat(3));
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeObservableManyOneThrows() {
        Completable c = Completable.merge(Observable.just(normal.completable, error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeObservableMaxConcurrent() {
        final List<Long> requested = new ArrayList<Long>();
        Observable<Completable> cs = Observable
                .just(normal.completable)
                .repeat(10)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long v) {
                        requested.add(v);
                    }
                });
        
        Completable c = Completable.merge(cs, 5);
        
        c.await();
        
        // FIXME this request pattern looks odd because all 10 completions trigger 1 requests
        Assert.assertEquals(Arrays.asList(5L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), requested);
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorNull() {
        Completable.mergeDelayError((Completable[])null);
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorEmpty() {
        Completable c = Completable.mergeDelayError();
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorSingleSource() {
        Completable c = Completable.mergeDelayError(normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeDelayErrorSingleSourceThrows() {
        Completable c = Completable.mergeDelayError(error.completable);
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorMultipleSources() {
        Completable c = Completable.mergeDelayError(normal.completable, normal.completable, normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorMultipleOneThrows() {
        Completable c = Completable.mergeDelayError(normal.completable, error.completable, normal.completable);
        
        try {
            c.await();
        } catch (TestException ex) {
            normal.assertSubscriptions(2);
        }
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void mergeDelayErrorMultipleOneIsNull() {
        Completable c = Completable.mergeDelayError(normal.completable, null);
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorIterableEmpty() {
        Completable c = Completable.mergeDelayError(Collections.<Completable>emptyList());
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableNull() {
        Completable.mergeDelayError((Iterable<Completable>)null);
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void mergeDelayErrorIterableIteratorNull() {
        Completable c = Completable.mergeDelayError(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return null;
            }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void mergeDelayErrorIterableWithNull() {
        Completable c = Completable.mergeDelayError(Arrays.asList(normal.completable, (Completable)null));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorIterableSingle() {
        Completable c = Completable.mergeDelayError(Collections.singleton(normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorIterableMany() {
        Completable c = Completable.mergeDelayError(Arrays.asList(normal.completable, normal.completable, normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeDelayErrorIterableOneThrows() {
        Completable c = Completable.mergeDelayError(Collections.singleton(error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorIterableManyOneThrows() {
        Completable c = Completable.mergeDelayError(Arrays.asList(normal.completable, error.completable, normal.completable));
        
        try {
            c.await();
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
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void mergeDelayErrorIterableIteratorHasNextThrows() {
        Completable c = Completable.mergeDelayError(new IterableIteratorHasNextThrows());
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void mergeDelayErrorIterableIteratorNextThrows() {
        Completable c = Completable.mergeDelayError(new IterableIteratorNextThrows());
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorObservableEmpty() {
        Completable c = Completable.mergeDelayError(Observable.<Completable>empty());
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeDelayErrorObservableError() {
        Completable c = Completable.mergeDelayError(Observable.<Completable>error(new TestException()));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorObservableSingle() {
        Completable c = Completable.mergeDelayError(Observable.just(normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeDelayErrorObservableSingleThrows() {
        Completable c = Completable.mergeDelayError(Observable.just(error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorObservableMany() {
        Completable c = Completable.mergeDelayError(Observable.just(normal.completable).repeat(3));
        
        c.await();
        
        normal.assertSubscriptions(3);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void mergeDelayErrorObservableManyOneThrows() {
        Completable c = Completable.mergeDelayError(Observable.just(normal.completable, error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void mergeDelayErrorObservableMaxConcurrent() {
        final List<Long> requested = new ArrayList<Long>();
        Observable<Completable> cs = Observable
                .just(normal.completable)
                .repeat(10)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long v) {
                        requested.add(v);
                    }
                });
        
        Completable c = Completable.mergeDelayError(cs, 5);
        
        c.await();
        
        // FIXME this request pattern looks odd because all 10 completions trigger 1 requests
        Assert.assertEquals(Arrays.asList(5L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), requested);
    }

    @Test(timeout = 1000)
    public void never() {
        final AtomicBoolean onSubscribeCalled = new AtomicBoolean();
        final AtomicInteger calls = new AtomicInteger();
        Completable.never().unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                onSubscribeCalled.set(true);
            }
            
            @Override
            public void onError(Throwable e) {
                calls.getAndIncrement();
            }
            
            @Override
            public void onCompleted() {
                calls.getAndIncrement();
            }
        });
        
        Assert.assertTrue("onSubscribe not called", onSubscribeCalled.get());
        Assert.assertEquals("There were calls to onXXX methods", 0, calls.get());
    }
    
    @Test(timeout = 1500)
    public void timer() {
        Completable c = Completable.timer(500, TimeUnit.MILLISECONDS);
        
        c.await();
    }
    
    @Test(timeout = 1500)
    public void timerNewThread() {
        Completable c = Completable.timer(500, TimeUnit.MILLISECONDS, Schedulers.newThread());
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void timerTestScheduler() {
        TestScheduler scheduler = Schedulers.test();
        
        Completable c = Completable.timer(250, TimeUnit.MILLISECONDS, scheduler);
        
        final AtomicInteger calls = new AtomicInteger();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onCompleted() {
                calls.getAndIncrement();
            }
            
            @Override
            public void onError(Throwable e) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
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
        
        final MultipleAssignmentSubscription mad = new MultipleAssignmentSubscription();
        final AtomicInteger calls = new AtomicInteger();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                mad.set(d);
            }
            
            @Override
            public void onError(Throwable e) {
                calls.getAndIncrement();
            }
            
            @Override
            public void onCompleted() {
                calls.getAndIncrement();
            }
        });
        
        Thread.sleep(100);
        
        mad.unsubscribe();
        
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
    
    @Test(timeout = 1000)
    public void usingNormalEager() {
        final AtomicInteger unsubscribe = new AtomicInteger();
        
        Completable c = Completable.using(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }, new Func1<Object, Completable>() {
            @Override
            public Completable call(Object v) {
                return normal.completable;
            }
        }, new Action1<Integer>() {
            @Override
            public void call(Integer d) {
                unsubscribe.set(d);
            }
        });
        
        final AtomicBoolean unsubscribedFirst = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onError(Throwable e) {
                error.lazySet(e);
            }
            
            @Override
            public void onCompleted() {
                unsubscribedFirst.set(unsubscribe.get() != 0);
            }
        });
        
        Assert.assertEquals(1, unsubscribe.get());
        Assert.assertTrue("Not unsubscribed first", unsubscribedFirst.get());
        Assert.assertNull(error.get());
    }
    
    @Test(timeout = 1000)
    public void usingNormalLazy() {
        final AtomicInteger unsubscribe = new AtomicInteger();
        
        Completable c = Completable.using(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }, new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return normal.completable;
            }
        }, new Action1<Integer>() {
            @Override
            public void call(Integer d) {
                unsubscribe.set(d);
            }
        }, false);
        
        final AtomicBoolean unsubscribedFirst = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onError(Throwable e) {
                error.lazySet(e);
            }
            
            @Override
            public void onCompleted() {
                unsubscribedFirst.set(unsubscribe.get() != 0);
            }
        });
        
        Assert.assertEquals(1, unsubscribe.get());
        Assert.assertFalse("Disposed first", unsubscribedFirst.get());
        Assert.assertNull(error.get());
    }
    
    @Test(timeout = 1000)
    public void usingErrorEager() {
        final AtomicInteger unsubscribe = new AtomicInteger();
        
        Completable c = Completable.using(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }, new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return error.completable;
            }
        }, new Action1<Integer>() {
            @Override
            public void call(Integer d) {
                unsubscribe.set(d);
            }
        });
        
        final AtomicBoolean unsubscribedFirst = new AtomicBoolean();
        final AtomicBoolean complete = new AtomicBoolean();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onError(Throwable e) {
                unsubscribedFirst.set(unsubscribe.get() != 0);
            }
            
            @Override
            public void onCompleted() {
                complete.set(true);
            }
        });
        
        Assert.assertEquals(1, unsubscribe.get());
        Assert.assertTrue("Not unsubscribed first", unsubscribedFirst.get());
        Assert.assertFalse(complete.get());
    }
    
    @Test(timeout = 1000)
    public void usingErrorLazy() {
        final AtomicInteger unsubscribe = new AtomicInteger();
        
        Completable c = Completable.using(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }, new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return error.completable;
            }
        }, new Action1<Integer>() {
            @Override
            public void call(Integer d) {
                unsubscribe.set(d);
            }
        }, false);
        
        final AtomicBoolean unsubscribedFirst = new AtomicBoolean();
        final AtomicBoolean complete = new AtomicBoolean();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onError(Throwable e) {
                unsubscribedFirst.set(unsubscribe.get() != 0);
            }
            
            @Override
            public void onCompleted() {
                complete.set(true);
            }
        });
        
        Assert.assertEquals(1, unsubscribe.get());
        Assert.assertFalse("Disposed first", unsubscribedFirst.get());
        Assert.assertFalse(complete.get());
    }
    
    @Test(expected = NullPointerException.class)
    public void usingResourceSupplierNull() {
        Completable.using(null, new Func1<Object, Completable>() {
            @Override
            public Completable call(Object v) {
                return normal.completable;
            }
        }, new Action1<Object>() {
            @Override
            public void call(Object v) { }
        });
    }

    @Test(expected = NullPointerException.class)
    public void usingMapperNull() {
        Completable.using(new Func0<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null, new Action1<Object>() {
            @Override
            public void call(Object v) { }
        });
    }

    @Test(expected = NullPointerException.class)
    public void usingMapperReturnsNull() {
        Completable c = Completable.using(new Func0<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Func1<Object, Completable>() {
            @Override
            public Completable call(Object v) {
                return null;
            }
        }, new Action1<Object>() {
            @Override
            public void call(Object v) { }
        });
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void usingDisposeNull() {
        Completable.using(new Func0<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Func1<Object, Completable>() {
            @Override
            public Completable call(Object v) {
                return normal.completable;
            }
        }, null);
    }
    
    @Test(expected = TestException.class)
    public void usingResourceThrows() {
        Completable c = Completable.using(new Func0<Object>() {
            @Override
            public Object call() { throw new TestException(); }
        }, 
                new Func1<Object, Completable>() {
                    @Override
                    public Completable call(Object v) {
                        return normal.completable;
                    }
                }, new Action1<Object>() {
                    @Override
                    public void call(Object v) { }
                });
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void usingMapperThrows() {
        Completable c = Completable.using(new Func0<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, 
                new Func1<Object, Completable>() {
                    @Override
                    public Completable call(Object v) { throw new TestException(); }
                }, new Action1<Object>() {
                    @Override
                    public void call(Object v) { }
                });
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void usingDisposerThrows() {
        Completable c = Completable.using(new Func0<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, 
                new Func1<Object, Completable>() {
                    @Override
                    public Completable call(Object v) {
                        return normal.completable;
                    }
                }, new Action1<Object>() {
                    @Override
                    public void call(Object v) { throw new TestException(); }
                });
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void composeNormal() {
        Completable c = error.completable.compose(new CompletableTransformer() {
            @Override
            public Completable call(Completable n) {
                return n.onErrorComplete();
            }
        });
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void composeNull() {
        error.completable.compose(null);
    }
    
    @Test(timeout = 1000)
    public void concatWithNormal() {
        Completable c = normal.completable.concatWith(normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(2);
    }

    @Test(timeout = 1000, expected = TestException.class)
    public void concatWithError() {
        Completable c = normal.completable.concatWith(error.completable);
        
        c.await();
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
    
    @Test(timeout = 1000)
    public void delayNormal() throws InterruptedException {
        Completable c = normal.completable.delay(250, TimeUnit.MILLISECONDS);
        
        final AtomicBoolean done = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onError(Throwable e) {
                error.set(e);
            }
            
            @Override
            public void onCompleted() {
                done.set(true);
            }
        });
        
        Thread.sleep(100);
        
        Assert.assertFalse("Already done", done.get());
        
        Thread.sleep(200);
        
        Assert.assertTrue("Not done", done.get());
        
        Assert.assertNull(error.get());
    }
    
    @Test(timeout = 1000)
    public void delayErrorImmediately() throws InterruptedException {
        Completable c = error.completable.delay(250, TimeUnit.MILLISECONDS);
        
        final AtomicBoolean done = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onError(Throwable e) {
                error.set(e);
            }
            
            @Override
            public void onCompleted() {
                done.set(true);
            }
        });

        Assert.assertTrue(error.get().toString(), error.get() instanceof TestException);
        Assert.assertFalse("Already done", done.get());

        Thread.sleep(100);
        
        Assert.assertFalse("Already done", done.get());
        
        Thread.sleep(200);
    }
    
    @Test(timeout = 1000)
    public void delayErrorToo() throws InterruptedException {
        Completable c = error.completable.delay(250, TimeUnit.MILLISECONDS, Schedulers.computation(), true);
        
        final AtomicBoolean done = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onError(Throwable e) {
                error.set(e);
            }
            
            @Override
            public void onCompleted() {
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
    
    @Test(timeout = 1000)
    public void doOnCompletedNormal() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = normal.completable.doOnCompleted(new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        });
        
        c.await();
        
        Assert.assertEquals(1, calls.get());
    }
    
    @Test(timeout = 1000)
    public void doOnCompletedError() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = error.completable.doOnCompleted(new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        });
        
        try {
            c.await();
            Assert.fail("Failed to throw TestException");
        } catch (TestException ex) {
            // expected
        }
        
        Assert.assertEquals(0, calls.get());
    }
    
    @Test(expected = NullPointerException.class)
    public void doOnCompletedNull() {
        normal.completable.doOnCompleted(null);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void doOnCompletedThrows() {
        Completable c = normal.completable.doOnCompleted(new Action0() {
            @Override
            public void call() { throw new TestException(); }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void doOnDisposeNormalDoesntCall() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = normal.completable.doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        });
        
        c.await();
        
        Assert.assertEquals(0, calls.get());
    }
    
    @Test(timeout = 1000)
    public void doOnDisposeErrorDoesntCall() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = error.completable.doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        });
        
        try {
            c.await();
            Assert.fail("No exception thrown");
        } catch (TestException ex) {
            // expected
        }
        Assert.assertEquals(0, calls.get());
    }
    
    @Test(timeout = 1000)
    public void doOnDisposeChildCancels() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = normal.completable.doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        });
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                d.unsubscribe();
            }
            
            @Override
            public void onError(Throwable e) {
                // ignored
            }
            
            @Override
            public void onCompleted() {
                // ignored
            }
        });
        
        Assert.assertEquals(1, calls.get());
    }
    
    @Test(expected = NullPointerException.class)
    public void doOnDisposeNull() {
        normal.completable.doOnUnsubscribe(null);
    }
    
    @Test(timeout = 1000)
    public void doOnDisposeThrows() {
        Completable c = normal.completable.doOnUnsubscribe(new Action0() {
            @Override
            public void call() { throw new TestException(); }
        });
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                d.unsubscribe();
            }
            
            @Override
            public void onError(Throwable e) {
                // ignored
            }
            
            @Override
            public void onCompleted() {
                // ignored
            }
        });
    }
    
    @Test(timeout = 1000)
    public void doOnErrorNoError() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        
        Completable c = normal.completable.doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                error.set(e);
            }
        });
        
        c.await();
        
        Assert.assertNull(error.get());
    }
    
    @Test(timeout = 1000)
    public void doOnErrorHasError() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        
        Completable c = error.completable.doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                err.set(e);
            }
        });
        
        try {
            c.await();
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
    
    @Test(timeout = 1000)
    public void doOnErrorThrows() {
        Completable c = error.completable.doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) { throw new IllegalStateException(); }
        });
        
        try {
            c.await();
        } catch (CompositeException ex) {
            List<Throwable> a = ex.getExceptions();
            Assert.assertEquals(2, a.size());
            Assert.assertTrue(a.get(0) instanceof TestException);
            Assert.assertTrue(a.get(1) instanceof IllegalStateException);
        }
    }
    
    @Test(timeout = 1000)
    public void doOnSubscribeNormal() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = normal.completable.doOnSubscribe(new Action1<Subscription>() {
            @Override
            public void call(Subscription s) {
                calls.getAndIncrement();
            }
        });
        
        for (int i = 0; i < 10; i++) {
            c.await();
        }
        
        Assert.assertEquals(10, calls.get());
    }
    
    @Test(expected = NullPointerException.class)
    public void doOnSubscribeNull() {
        normal.completable.doOnSubscribe(null);
    }
    
    @Test(expected = TestException.class)
    public void doOnSubscribeThrows() {
        Completable c = normal.completable.doOnSubscribe(new Action1<Subscription>() {
            @Override
            public void call(Subscription d) { throw new TestException(); }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void doOnTerminateNormal() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = normal.completable.doOnTerminate(new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        });
        
        c.await();
        
        Assert.assertEquals(1, calls.get());
    }
    
    @Test(timeout = 1000)
    public void doOnTerminateError() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = error.completable.doOnTerminate(new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        });
        
        try {
            c.await();
            Assert.fail("Did dot throw exception");
        } catch (TestException ex) {
            // expected
        }
        
        Assert.assertEquals(1, calls.get());
    }
    
    @Test(timeout = 1000)
    public void doAfterTerminateNormal() {
        final AtomicBoolean doneAfter = new AtomicBoolean();
        final AtomicBoolean complete = new AtomicBoolean();
        
        Completable c = normal.completable.doAfterTerminate(new Action0() {
            @Override
            public void call() {
                doneAfter.set(complete.get());
            }
        });
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }

            @Override
            public void onError(Throwable e) {
                
            }
            
            @Override
            public void onCompleted() {
                complete.set(true);
            }
        });
        
        c.await();
        
        Assert.assertTrue("Not completed", complete.get());
        Assert.assertTrue("Closure called before onComplete", doneAfter.get());
    }
    
    @Test(timeout = 1000)
    public void doAfterTerminateWithError() {
        final AtomicBoolean doneAfter = new AtomicBoolean();
        
        Completable c = error.completable.doAfterTerminate(new Action0() {
            @Override
            public void call() {
                doneAfter.set(true);
            }
        });
        
        try {
            c.await();
            Assert.fail("Did not throw TestException");
        } catch (TestException ex) {
            // expected
        }
        
        Assert.assertFalse("Closure called", doneAfter.get());
    }
    
    @Test(expected = NullPointerException.class)
    public void doAfterTerminateNull() {
        normal.completable.doAfterTerminate(null);
    }
    
    @Test(timeout = 1000)
    public void getNormal() {
        Assert.assertNull(normal.completable.get());
    }
    
    @Test(timeout = 1000)
    public void getError() {
        Assert.assertTrue(error.completable.get() instanceof TestException);
    }
    
    @Test(timeout = 1000)
    public void getTimeout() {
        try {
            Completable.never().get(100, TimeUnit.MILLISECONDS);
        } catch (RuntimeException ex) {
            if (!(ex.getCause() instanceof TimeoutException)) {
                Assert.fail("Wrong exception cause: " + ex.getCause());
            }
        }
    }
    
    @Test(expected = NullPointerException.class)
    public void getNullUnit() {
        normal.completable.get(1, null);
    }
    
    @Test(expected = NullPointerException.class)
    public void liftNull() {
        normal.completable.lift(null);
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void liftReturnsNull() {
        Completable c = normal.completable.lift(new CompletableOperator() {
            @Override
            public CompletableSubscriber call(CompletableSubscriber v) {
                return null;
            }
        });
        
        c.await();
    }

    final static class CompletableOperatorSwap implements CompletableOperator {
        @Override
        public CompletableSubscriber call(final CompletableSubscriber v) {
            return new CompletableSubscriber() {

                @Override
                public void onCompleted() {
                    v.onError(new TestException());
                }

                @Override
                public void onError(Throwable e) {
                    v.onCompleted();
                }

                @Override
                public void onSubscribe(Subscription d) {
                    v.onSubscribe(d);
                }
                
            };
        }
    }
    @Test(timeout = 1000, expected = TestException.class)
    public void liftOnCompleteError() {
        Completable c = normal.completable.lift(new CompletableOperatorSwap());
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void liftOnErrorComplete() {
        Completable c = error.completable.lift(new CompletableOperatorSwap());
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void mergeWithNull() {
        normal.completable.mergeWith(null);
    }
    
    @Test(timeout = 1000)
    public void mergeWithNormal() {
        Completable c = normal.completable.mergeWith(normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(2);
    }
    
    @Test(expected = NullPointerException.class)
    public void observeOnNull() {
        normal.completable.observeOn(null);
    }
    
    @Test(timeout = 1000)
    public void observeOnNormal() throws InterruptedException {
        final AtomicReference<String> name = new AtomicReference<String>();
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);
        
        Completable c = normal.completable.observeOn(Schedulers.computation());
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onCompleted() {
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
    
    @Test(timeout = 1000)
    public void observeOnError() throws InterruptedException {
        final AtomicReference<String> name = new AtomicReference<String>();
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);
        
        Completable c = error.completable.observeOn(Schedulers.computation());
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(Subscription d) {
                
            }
            
            @Override
            public void onCompleted() {
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
    
    @Test(timeout = 1000)
    public void onErrorComplete() {
        Completable c = error.completable.onErrorComplete();
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void onErrorCompleteFalse() {
        Completable c = error.completable.onErrorComplete(new Func1<Throwable, Boolean>() {
            @Override
            public Boolean call(Throwable e) {
                return e instanceof IllegalStateException;
            }
        });
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void onErrorCompleteNull() {
        error.completable.onErrorComplete(null);
    }
    
    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextNull() {
        error.completable.onErrorResumeNext(null);
    }
    
    @Test(timeout = 1000)
    public void onErrorResumeNextFunctionReturnsNull() {
        Completable c = error.completable.onErrorResumeNext(new Func1<Throwable, Completable>() {
            @Override
            public Completable call(Throwable e) {
                return null;
            }
        });
        
        try {
            c.await();
            Assert.fail("Did not throw an exception");
        } catch (CompositeException ex) {
            List<Throwable> a = ex.getExceptions();
            Assert.assertEquals(2, a.size());
            Assert.assertTrue(a.get(0) instanceof TestException);
            Assert.assertTrue(a.get(1) instanceof NullPointerException);
        }
    }
    
    @Test(timeout = 1000)
    public void onErrorResumeNextFunctionThrows() {
        Completable c = error.completable.onErrorResumeNext(new Func1<Throwable, Completable>() {
            @Override
            public Completable call(Throwable e) { throw new TestException(); }
        });
        
        try {
            c.await();
            Assert.fail("Did not throw an exception");
        } catch (CompositeException ex) {
            List<Throwable> a = ex.getExceptions();
            Assert.assertEquals(2, a.size());
            Assert.assertTrue(a.get(0) instanceof TestException);
            Assert.assertTrue(a.get(1) instanceof TestException);
        }
    }
    
    @Test(timeout = 1000)
    public void onErrorResumeNextNormal() {
        Completable c = error.completable.onErrorResumeNext(new Func1<Throwable, Completable>() {
            @Override
            public Completable call(Throwable v) {
                return normal.completable;
            }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void onErrorResumeNextError() {
        Completable c = error.completable.onErrorResumeNext(new Func1<Throwable, Completable>() {
            @Override
            public Completable call(Throwable v) {
                return error.completable;
            }
        });
        
        c.await();
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
        
        c.unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(final Subscription d) {
                final Scheduler.Worker w = Schedulers.io().createWorker();
                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            d.unsubscribe();
                        } finally {
                            w.unsubscribe();
                        }
                    }
                }, 550, TimeUnit.MILLISECONDS);
            }
            
            @Override
            public void onError(Throwable e) {
                err.set(e);
            }
            
            @Override
            public void onCompleted() {
                
            }
        });
        
        Assert.assertEquals(6, calls.get());
        Assert.assertNull(err.get());
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void repeatError() {
        Completable c = error.completable.repeat();
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void repeat5Times() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.getAndIncrement();
                return null;
            }
        }).repeat(5);
        
        c.await();
        
        Assert.assertEquals(5, calls.get());
    }
    
    @Test(timeout = 1000)
    public void repeat1Time() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.getAndIncrement();
                return null;
            }
        }).repeat(1);
        
        c.await();
        
        Assert.assertEquals(1, calls.get());
    }
    
    @Test(timeout = 1000)
    public void repeat0Time() {
        final AtomicInteger calls = new AtomicInteger();
        
        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.getAndIncrement();
                return null;
            }
        }).repeat(0);
        
        c.await();
        
        Assert.assertEquals(0, calls.get());
    }
    
    @Test(expected = NullPointerException.class)
    public void repeatWhenNull() {
        normal.completable.repeatWhen(null);
    }
    
    @Test(timeout = 1000)
    public void retryNormal() {
        Completable c = normal.completable.retry();
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000)
    public void retry5Times() {
        final AtomicInteger calls = new AtomicInteger(5);
        
        Completable c = Completable.fromAction(new Action0() {
            @Override
            public void call() {
                if (calls.decrementAndGet() != 0) {
                    throw new TestException();
                }
            }
        }).retry();
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void retryBiPredicate5Times() {
        Completable c = error.completable.retry(new Func2<Integer, Throwable, Boolean>() {
            @Override
            public Boolean call(Integer n, Throwable e) {
                return n < 5;
            }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void retryTimes5Error() {
        Completable c = error.completable.retry(5);
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void retryTimes5Normal() {
        final AtomicInteger calls = new AtomicInteger(5);

        Completable c = Completable.fromAction(new Action0() {
            @Override
            public void call() {
                if (calls.decrementAndGet() != 0) {
                    throw new TestException();
                }
            }
        }).retry(5);
        
        c.await();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void retryNegativeTimes() {
        normal.completable.retry(-1);
    }
    
    @Test(timeout = 1000)
    public void retryWhen5Times() {
        final AtomicInteger calls = new AtomicInteger(5);

        Completable c = Completable.fromAction(new Action0() {
            @Override
            public void call() {
                if (calls.decrementAndGet() != 0) {
                    throw new TestException();
                }
            }
        }).retryWhen(new Func1<Observable<? extends Throwable>, Observable<Object>>() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Observable<Object> call(Observable<? extends Throwable> o) {
                return (Observable)o;
            }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void subscribe() throws InterruptedException {
        final AtomicBoolean complete = new AtomicBoolean();
        
        Completable c = normal.completable
                .delay(100, TimeUnit.MILLISECONDS)
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        complete.set(true);
                    }
                });
        
        c.subscribe();
        
        Thread.sleep(150);
        
        Assert.assertTrue("Not completed", complete.get());
    }
    
    @Test(timeout = 1000)
    public void subscribeDispose() throws InterruptedException {
        final AtomicBoolean complete = new AtomicBoolean();
        
        Completable c = normal.completable
                .delay(200, TimeUnit.MILLISECONDS)
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        complete.set(true);
                    }
                });
        
        Subscription d = c.subscribe();
        
        Thread.sleep(100);
        
        d.unsubscribe();
        
        Thread.sleep(150);
        
        Assert.assertFalse("Completed", complete.get());
    }
    
    @Test(timeout = 1000)
    public void subscribeTwoCallbacksNormal() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final AtomicBoolean complete = new AtomicBoolean();
        normal.completable.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                err.set(e);
            }
        }, new Action0() {
            @Override
            public void call() {
                complete.set(true);
            }
        });
        
        Assert.assertNull(err.get());
        Assert.assertTrue("Not completed", complete.get());
    }
    
    @Test(timeout = 1000)
    public void subscribeTwoCallbacksError() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final AtomicBoolean complete = new AtomicBoolean();
        error.completable.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                err.set(e);
            }
        }, new Action0() {
            @Override
            public void call() {
                complete.set(true);
            }
        });
        
        Assert.assertTrue(err.get() instanceof TestException);
        Assert.assertFalse("Not completed", complete.get());
    }
    
    @Test(expected = NullPointerException.class)
    public void subscribeTwoCallbacksFirstNull() {
        normal.completable.subscribe(null, new Action0() {
            @Override
            public void call() { }
        });
    }
    
    @Test(expected = NullPointerException.class)
    public void subscribeTwoCallbacksSecondNull() {
        normal.completable.subscribe(null, new Action0() {
            @Override
            public void call() { }
        });
    }
    
    @Test(timeout = 1000)
    public void subscribeTwoCallbacksCompleteThrows() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        normal.completable.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                err.set(e);
            }
        }, new Action0() {
            @Override
            public void call() { throw new TestException(); }
        });
        
        Assert.assertTrue(String.valueOf(err.get()), err.get() instanceof TestException);
    }
    
    @Test(timeout = 1000)
    public void subscribeTwoCallbacksOnErrorThrows() {
        error.completable.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) { throw new TestException(); }
        }, new Action0() {
            @Override
            public void call() { }
        });
    }
    
    @Test(timeout = 1000)
    public void subscribeActionNormal() {
        final AtomicBoolean run = new AtomicBoolean();
        
        normal.completable.subscribe(new Action0() {
            @Override
            public void call() {
                run.set(true);
            }
        });
        
        Assert.assertTrue("Not completed", run.get());
    }

    @Test(timeout = 1000)
    public void subscribeActionError() {
        final AtomicBoolean run = new AtomicBoolean();
        
        error.completable.subscribe(new Action0() {
            @Override
            public void call() {
                run.set(true);
            }
        });
        
        Assert.assertFalse("Completed", run.get());
    }
    
    @Test(expected = NullPointerException.class)
    public void subscribeActionNull() {
        normal.completable.subscribe((Action0)null);
    }
    
    @Test(expected = NullPointerException.class)
    public void subscribeSubscriberNull() {
        normal.completable.unsafeSubscribe((Subscriber<Object>)null);
    }
    
    @Test(expected = NullPointerException.class)
    public void subscribeCompletableSubscriberNull() {
        normal.completable.unsafeSubscribe((CompletableSubscriber)null);
    }

    @Test(timeout = 1000)
    public void subscribeSubscriberNormal() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        
        normal.completable.unsafeSubscribe(ts);
        
        ts.assertCompleted();
        ts.assertNoValues();
        ts.assertNoErrors();
    }

    @Test(timeout = 1000)
    public void subscribeSubscriberError() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        
        error.completable.unsafeSubscribe(ts);
        
        ts.assertNotCompleted();
        ts.assertNoValues();
        ts.assertError(TestException.class);
    }
    
    @Test(expected = NullPointerException.class)
    public void subscribeOnNull() {
        normal.completable.subscribeOn(null);
    }
    
    @Test(timeout = 1000)
    public void subscribeOnNormal() {
        final AtomicReference<String> name = new  AtomicReference<String>();
        
        Completable c = Completable.create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) { 
                name.set(Thread.currentThread().getName());
                s.onSubscribe(Subscriptions.unsubscribed());
                s.onCompleted();
            }
        }).subscribeOn(Schedulers.computation());
        
        c.await();
        
        Assert.assertTrue(name.get().startsWith("RxComputation"));
    }
    
    @Test(timeout = 1000)
    public void subscribeOnError() {
        final AtomicReference<String> name = new  AtomicReference<String>();
        
        Completable c = Completable.create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) { 
                name.set(Thread.currentThread().getName());
                s.onSubscribe(Subscriptions.unsubscribed());
                s.onError(new TestException());
            }
        }).subscribeOn(Schedulers.computation());
        
        try {
            c.await();
            Assert.fail("No exception thrown");
        } catch (TestException ex) {
            // expected
        }
        
        Assert.assertTrue(name.get().startsWith("RxComputation"));
    }

    @Test
    public void subscribeEmptyOnError() {
        expectUncaughtTestException(new Action0() {
            @Override public void call() {
                error.completable.subscribe();
            }
        });
    }

    @Test
    public void subscribeOneActionOnError() {
        expectUncaughtTestException(new Action0() {
            @Override
            public void call() {
                error.completable.subscribe(new Action0() {
                    @Override
                    public void call() {
                    }
                });
            }
        });
    }

    @Test
    public void subscribeOneActionThrowFromOnCompleted() {
        expectUncaughtTestException(new Action0() {
            @Override
            public void call() {
                normal.completable.subscribe(new Action0() {
                    @Override
                    public void call() {
                        throw new TestException();
                    }
                });
            }
        });
    }

    @Test
    public void subscribeTwoActionsThrowFromOnError() {
        expectUncaughtTestException(new Action0() {
            @Override
            public void call() {
                error.completable.subscribe(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throw new TestException();
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                    }
                });
            }
        });
    }

    @Test(expected = OnErrorNotImplementedException.class)
    public void propagateExceptionSubscribeEmpty() {
        error.completable.toSingleDefault(0).subscribe();
    }

    @Test(expected = OnErrorNotImplementedException.class)
    public void propagateExceptionSubscribeOneAction() {
        error.completable.toSingleDefault(1).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
            }
        });
    }

    @Test(expected = OnErrorNotImplementedException.class)
    public void propagateExceptionSubscribeOneActionThrowFromOnSuccess() {
        normal.completable.toSingleDefault(1).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                throw new TestException();
            }
        });
    }

    @Test(timeout = 1000)
    public void timeoutEmitError() {
        Throwable e = Completable.never().timeout(100, TimeUnit.MILLISECONDS).get();
        
        Assert.assertTrue(e instanceof TimeoutException);
    }
    
    @Test(timeout = 1000)
    public void timeoutSwitchNormal() {
        Completable c = Completable.never().timeout(100, TimeUnit.MILLISECONDS, normal.completable);
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000)
    public void timeoutTimerCancelled() throws InterruptedException {
        Completable c = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Thread.sleep(50);
                return null;
            }
        }).timeout(100, TimeUnit.MILLISECONDS, normal.completable);
        
        c.await();
        
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
    
    @Test(timeout = 1000)
    public void toNormal() {
        Observable<?> flow = normal.completable.to(new Func1<Completable, Observable<Object>>() {
            @Override
            public Observable<Object> call(Completable c) {
                return c.toObservable();
            }
        });
        
        flow.toBlocking().forEach(new Action1<Object>(){
            @Override 
            public void call(Object e){ }
        });
    }
    
    @Test(expected = NullPointerException.class)
    public void toNull() {
        normal.completable.to(null);
    }
    
    @Test(timeout = 1000)
    public void toObservableNormal() {
        normal.completable.toObservable().toBlocking().forEach(new Action1<Object>() {
            @Override
            public void call(Object e) { }
        });
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void toObservableError() {
        error.completable.toObservable().toBlocking().forEach(new Action1<Object>() {
            @Override
            public void call(Object e) { }
        });
    }

    static <T> T get(Single<T> single) {
        final CountDownLatch cdl = new CountDownLatch(1);
        
        final AtomicReference<T> v = new AtomicReference<T>();
        final AtomicReference<Throwable> e = new AtomicReference<Throwable>();
        
        single.subscribe(new SingleSubscriber<T>() {

            @Override
            public void onSuccess(T value) {
                v.set(value);
                cdl.countDown();
            }

            @Override
            public void onError(Throwable error) {
                e.set(error);
                cdl.countDown();
            }
        });
        
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            Exceptions.propagate(ex);
        }
        
        if (e.get() != null) {
            Exceptions.propagate(e.get());
        }
        return v.get();
    }
    
    @Test(timeout = 1000)
    public void toSingleSupplierNormal() {
        int v = get(normal.completable.toSingle(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }));
        
        Assert.assertEquals(1, v);
    }

    @Test(timeout = 1000, expected = TestException.class)
    public void toSingleSupplierError() {
        get(error.completable.toSingle(new Func0<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }));
    }

    @Test(expected = NullPointerException.class)
    public void toSingleSupplierNull() {
        normal.completable.toSingle(null);
    }
    
    @Test(expected = NullPointerException.class)
    public void toSingleSupplierReturnsNull() {
        get(normal.completable.toSingle(new Func0<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }));
    }

    @Test(expected = TestException.class)
    public void toSingleSupplierThrows() {
        get(normal.completable.toSingle(new Func0<Object>() {
            @Override
            public Object call() { throw new TestException(); }
        }));
    }

    @Test(timeout = 1000, expected = TestException.class)
    public void toSingleDefaultError() {
        get(error.completable.toSingleDefault(1));
    }
    
    @Test(timeout = 1000)
    public void toSingleDefaultNormal() {
        Assert.assertEquals((Integer)1, get(normal.completable.toSingleDefault(1)));
    }
    
    @Test(expected = NullPointerException.class)
    public void toSingleDefaultNull() {
        normal.completable.toSingleDefault(null);
    }
    
    @Test(timeout = 1000)
    public void unsubscribeOnNormal() throws InterruptedException {
        final AtomicReference<String> name = new AtomicReference<String>();
        final CountDownLatch cdl = new CountDownLatch(1);
        
        normal.completable.delay(1, TimeUnit.SECONDS)
        .doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                name.set(Thread.currentThread().getName());
                cdl.countDown();
            }
        })
        .unsubscribeOn(Schedulers.computation())
        .unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onSubscribe(final Subscription d) {
                final Scheduler.Worker w = Schedulers.io().createWorker();
                
                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            d.unsubscribe();
                        } finally {
                            w.unsubscribe();
                        }
                    }
                }, 100, TimeUnit.MILLISECONDS);
            }
            
            @Override
            public void onError(Throwable e) {
                
            }
            
            @Override
            public void onCompleted() {
                
            }
        });
        
        cdl.await();
        
        Assert.assertTrue(name.get().startsWith("RxComputation"));
    }
    
    @Test(expected = NullPointerException.class)
    public void ambArrayNull() {
        Completable.amb((Completable[])null);
    }

    @Test(timeout = 1000)
    public void ambArrayEmpty() {
        Completable c = Completable.amb();
                
        c.await();
    }

    @Test(timeout = 1000)
    public void ambArraySingleNormal() {
        Completable c = Completable.amb(normal.completable);
                
        c.await();
    }

    @Test(timeout = 1000, expected = TestException.class)
    public void ambArraySingleError() {
        Completable c = Completable.amb(error.completable);
                
        c.await();
    }
    
    @Test(timeout = 1000)
    public void ambArrayOneFires() {
        PublishSubject<Object> ps1 = PublishSubject.create();
        PublishSubject<Object> ps2 = PublishSubject.create();
        
        Completable c1 = Completable.fromObservable(ps1);

        Completable c2 = Completable.fromObservable(ps2);
        
        Completable c = Completable.amb(c1, c2);
        
        final AtomicBoolean complete = new AtomicBoolean();
        
        c.subscribe(new Action0() {
            @Override
            public void call() {
                complete.set(true);
            }
        });
        
        Assert.assertTrue("First subject no subscribers", ps1.hasObservers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasObservers());
        
        ps1.onCompleted();
        
        Assert.assertFalse("First subject has subscribers", ps1.hasObservers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasObservers());
        
        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 1000)
    public void ambArrayOneFiresError() {
        PublishSubject<Object> ps1 = PublishSubject.create();
        PublishSubject<Object> ps2 = PublishSubject.create();
        
        Completable c1 = Completable.fromObservable(ps1);

        Completable c2 = Completable.fromObservable(ps2);
        
        Completable c = Completable.amb(c1, c2);
        
        final AtomicReference<Throwable> complete = new AtomicReference<Throwable>();
        
        c.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                complete.set(e);
            }
        }, new Action0() {
            @Override
            public void call() { }
        });
        
        Assert.assertTrue("First subject no subscribers", ps1.hasObservers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasObservers());
        
        ps1.onError(new TestException());
        
        Assert.assertFalse("First subject has subscribers", ps1.hasObservers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasObservers());
        
        Assert.assertTrue("Not completed", complete.get() instanceof TestException);
    }
    
    @Test(timeout = 1000)
    public void ambArraySecondFires() {
        PublishSubject<Object> ps1 = PublishSubject.create();
        PublishSubject<Object> ps2 = PublishSubject.create();
        
        Completable c1 = Completable.fromObservable(ps1);

        Completable c2 = Completable.fromObservable(ps2);
        
        Completable c = Completable.amb(c1, c2);
        
        final AtomicBoolean complete = new AtomicBoolean();
        
        c.subscribe(new Action0() {
            @Override
            public void call() {
                complete.set(true);
            }
        });
        
        Assert.assertTrue("First subject no subscribers", ps1.hasObservers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasObservers());
        
        ps2.onCompleted();
        
        Assert.assertFalse("First subject has subscribers", ps1.hasObservers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasObservers());
        
        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 1000)
    public void ambArraySecondFiresError() {
        PublishSubject<Object> ps1 = PublishSubject.create();
        PublishSubject<Object> ps2 = PublishSubject.create();
        
        Completable c1 = Completable.fromObservable(ps1);

        Completable c2 = Completable.fromObservable(ps2);
        
        Completable c = Completable.amb(c1, c2);
        
        final AtomicReference<Throwable> complete = new AtomicReference<Throwable>();
        
        c.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                complete.set(e);
            }
        }, new Action0() {
            @Override
            public void call() { }
        });
        
        Assert.assertTrue("First subject no subscribers", ps1.hasObservers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasObservers());
        
        ps2.onError(new TestException());
        
        Assert.assertFalse("First subject has subscribers", ps1.hasObservers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasObservers());
        
        Assert.assertTrue("Not completed", complete.get() instanceof TestException);
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void ambMultipleOneIsNull() {
        Completable c = Completable.amb(null, normal.completable);
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void ambIterableEmpty() {
        Completable c = Completable.amb(Collections.<Completable>emptyList());
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void ambIterableNull() {
        Completable.amb((Iterable<Completable>)null);
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void ambIterableIteratorNull() {
        Completable c = Completable.amb(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return null;
            }
        });
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = NullPointerException.class)
    public void ambIterableWithNull() {
        Completable c = Completable.amb(Arrays.asList(null, normal.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000)
    public void ambIterableSingle() {
        Completable c = Completable.amb(Collections.singleton(normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000)
    public void ambIterableMany() {
        Completable c = Completable.amb(Arrays.asList(normal.completable, normal.completable, normal.completable));
        
        c.await();
        
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void ambIterableOneThrows() {
        Completable c = Completable.amb(Collections.singleton(error.completable));
        
        c.await();
    }
    
    @Test(timeout = 1000, expected = TestException.class)
    public void ambIterableManyOneThrows() {
        Completable c = Completable.amb(Arrays.asList(error.completable, normal.completable));
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void ambIterableIterableThrows() {
        Completable c = Completable.amb(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                throw new TestException();
            }
        });
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void ambIterableIteratorHasNextThrows() {
        Completable c = Completable.amb(new IterableIteratorHasNextThrows());
        
        c.await();
    }
    
    @Test(expected = TestException.class)
    public void ambIterableIteratorNextThrows() {
        Completable c = Completable.amb(new IterableIteratorNextThrows());
        
        c.await();
    }
    
    @Test(expected = NullPointerException.class)
    public void ambWithNull() {
        normal.completable.ambWith(null);
    }
    
    @Test(timeout = 1000)
    public void ambWithArrayOneFires() {
        PublishSubject<Object> ps1 = PublishSubject.create();
        PublishSubject<Object> ps2 = PublishSubject.create();
        
        Completable c1 = Completable.fromObservable(ps1);

        Completable c2 = Completable.fromObservable(ps2);
        
        Completable c = c1.ambWith(c2);
        
        final AtomicBoolean complete = new AtomicBoolean();
        
        c.subscribe(new Action0() {
            @Override
            public void call() {
                complete.set(true);
            }
        });
        
        Assert.assertTrue("First subject no subscribers", ps1.hasObservers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasObservers());
        
        ps1.onCompleted();
        
        Assert.assertFalse("First subject has subscribers", ps1.hasObservers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasObservers());
        
        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 1000)
    public void ambWithArrayOneFiresError() {
        PublishSubject<Object> ps1 = PublishSubject.create();
        PublishSubject<Object> ps2 = PublishSubject.create();
        
        Completable c1 = Completable.fromObservable(ps1);

        Completable c2 = Completable.fromObservable(ps2);
        
        Completable c = c1.ambWith(c2);
        
        final AtomicReference<Throwable> complete = new AtomicReference<Throwable>();
        
        c.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                complete.set(e);
            }
        }, new Action0() {
            @Override
            public void call() { }
        });
        
        Assert.assertTrue("First subject no subscribers", ps1.hasObservers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasObservers());
        
        ps1.onError(new TestException());
        
        Assert.assertFalse("First subject has subscribers", ps1.hasObservers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasObservers());
        
        Assert.assertTrue("Not completed", complete.get() instanceof TestException);
    }
    
    @Test(timeout = 1000)
    public void ambWithArraySecondFires() {
        PublishSubject<Object> ps1 = PublishSubject.create();
        PublishSubject<Object> ps2 = PublishSubject.create();
        
        Completable c1 = Completable.fromObservable(ps1);

        Completable c2 = Completable.fromObservable(ps2);
        
        Completable c = c1.ambWith(c2);
        
        final AtomicBoolean complete = new AtomicBoolean();
        
        c.subscribe(new Action0() {
            @Override
            public void call() {
                complete.set(true);
            }
        });
        
        Assert.assertTrue("First subject no subscribers", ps1.hasObservers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasObservers());
        
        ps2.onCompleted();
        
        Assert.assertFalse("First subject has subscribers", ps1.hasObservers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasObservers());
        
        Assert.assertTrue("Not completed", complete.get());
    }

    @Test(timeout = 1000)
    public void ambWithArraySecondFiresError() {
        PublishSubject<Object> ps1 = PublishSubject.create();
        PublishSubject<Object> ps2 = PublishSubject.create();
        
        Completable c1 = Completable.fromObservable(ps1);

        Completable c2 = Completable.fromObservable(ps2);
        
        Completable c = c1.ambWith(c2);
        
        final AtomicReference<Throwable> complete = new AtomicReference<Throwable>();
        
        c.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                complete.set(e);
            }
        }, new Action0() {
            @Override
            public void call() { }
        });
        
        Assert.assertTrue("First subject no subscribers", ps1.hasObservers());
        Assert.assertTrue("Second subject no subscribers", ps2.hasObservers());
        
        ps2.onError(new TestException());
        
        Assert.assertFalse("First subject has subscribers", ps1.hasObservers());
        Assert.assertFalse("Second subject has subscribers", ps2.hasObservers());
        
        Assert.assertTrue("Not completed", complete.get() instanceof TestException);
    }
    
    @Test(timeout = 1000)
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
        
        c.await();
        
        Assert.assertTrue("Did not start with other", run.get());
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000)
    public void startWithCompletableError() {
        Completable c = normal.completable.startWith(error.completable);
        
        try {
            c.await();
            Assert.fail("Did not throw TestException");
        } catch (TestException ex) {
            normal.assertSubscriptions(0);
            error.assertSubscriptions(1);
        }
    }
    
    @Test(timeout = 1000)
    public void startWithFlowableNormal() {
        final AtomicBoolean run = new AtomicBoolean();
        Observable<Object> c = normal.completable
                .startWith(Observable.fromCallable(new Callable<Object>() {
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
        ts.assertCompleted();
        ts.assertNoErrors();
    }
    
    @Test(timeout = 1000)
    public void startWithFlowableError() {
        Observable<Object> c = normal.completable
                .startWith(Observable.error(new TestException()));
        
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        
        c.subscribe(ts);
        
        normal.assertSubscriptions(0);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test(expected = NullPointerException.class)
    public void startWithCompletableNull() {
        normal.completable.startWith((Completable)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithFlowableNull() {
        normal.completable.startWith((Observable<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void andThenCompletableNull() {
        normal.completable.andThen((Completable)null);
    }

    @Test(expected = NullPointerException.class)
    public void andThenFlowableNull() {
        normal.completable.andThen((Observable<Object>)null);
    }

    @Test(timeout = 1000)
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
        
        c.await();
        
        Assert.assertFalse("Start with other", run.get());
        normal.assertSubscriptions(1);
    }
    
    @Test(timeout = 1000)
    public void andThenCompletableError() {
        Completable c = normal.completable.andThen(error.completable);
        
        try {
            c.await();
            Assert.fail("Did not throw TestException");
        } catch (TestException ex) {
            normal.assertSubscriptions(1);
            error.assertSubscriptions(1);
        }
    }
    
    @Test(timeout = 1000)
    public void andThenFlowableNormal() {
        final AtomicBoolean run = new AtomicBoolean();
        Observable<Object> c = normal.completable
                .andThen(Observable.fromCallable(new Callable<Object>() {
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
        ts.assertCompleted();
        ts.assertNoErrors();
    }
    
    @Test(timeout = 1000)
    public void andThenFlowableError() {
        Observable<Object> c = normal.completable
                .andThen(Observable.error(new TestException()));
        
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        
        c.subscribe(ts);
        
        normal.assertSubscriptions(1);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test
    public void usingFactoryThrows() {
        @SuppressWarnings("unchecked")
        Action1<Integer> onDispose = mock(Action1.class);
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Completable.using(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        },
        new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer t) {
                throw new TestException();
            }
        }, onDispose).unsafeSubscribe(ts);
        
        verify(onDispose).call(1);
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }

    @Test
    public void usingFactoryAndDisposerThrow() {
        Action1<Integer> onDispose = new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                throw new TestException();
            }
        };
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Completable.using(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        },
        new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer t) {
                throw new TestException();
            }
        }, onDispose).unsafeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(CompositeException.class);
        
        CompositeException ex = (CompositeException)ts.getOnErrorEvents().get(0);
        
        List<Throwable> listEx = ex.getExceptions();
        
        assertEquals(2, listEx.size());
        
        assertTrue(listEx.get(0).toString(), listEx.get(0) instanceof TestException);
        assertTrue(listEx.get(1).toString(), listEx.get(1) instanceof TestException);
    }

    @Test
    public void usingFactoryReturnsNull() {
        @SuppressWarnings("unchecked")
        Action1<Integer> onDispose = mock(Action1.class);
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Completable.using(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        },
        new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer t) {
                return null;
            }
        }, onDispose).unsafeSubscribe(ts);
        
        verify(onDispose).call(1);
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(NullPointerException.class);
    }

    @Test
    public void usingFactoryReturnsNullAndDisposerThrows() {
        Action1<Integer> onDispose = new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                throw new TestException();
            }
        };
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Completable.using(new Func0<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        },
        new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer t) {
                return null;
            }
        }, onDispose).unsafeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(CompositeException.class);
        
        CompositeException ex = (CompositeException)ts.getOnErrorEvents().get(0);
        
        List<Throwable> listEx = ex.getExceptions();
        
        assertEquals(2, listEx.size());
        
        assertTrue(listEx.get(0).toString(), listEx.get(0) instanceof NullPointerException);
        assertTrue(listEx.get(1).toString(), listEx.get(1) instanceof TestException);
    }

    @Test
    public void subscribeReportsUnsubscribed() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        Subscription completableSubscription = completable.subscribe();
        
        stringSubject.onCompleted();
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
    }

    @Test
    public void subscribeReportsUnsubscribedOnError() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        Subscription completableSubscription = completable.subscribe();
        
        stringSubject.onError(new TestException());
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
    }

    @Test
    public void subscribeActionReportsUnsubscribed() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        Subscription completableSubscription = completable.subscribe(Actions.empty());
        
        stringSubject.onCompleted();
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
    }

    @Test
    public void subscribeActionReportsUnsubscribedAfter() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        final AtomicReference<Subscription> subscriptionRef = new AtomicReference<Subscription>();
        Subscription completableSubscription = completable.subscribe(new Action0() {
            @Override
            public void call() {
                if (subscriptionRef.get().isUnsubscribed()) {
                    subscriptionRef.set(null);
                }
            }
        });
        subscriptionRef.set(completableSubscription);
        
        stringSubject.onCompleted();
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
        assertNotNull("Unsubscribed before the call to onCompleted", subscriptionRef.get());
    }

    @Test
    public void subscribeActionReportsUnsubscribedOnError() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        Subscription completableSubscription = completable.subscribe(Actions.empty());
        
        stringSubject.onError(new TestException());
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
    }

    @Test
    public void subscribeAction2ReportsUnsubscribed() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        Subscription completableSubscription = completable.subscribe(Actions.empty(), Actions.empty());
        
        stringSubject.onCompleted();
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
    }

    @Test
    public void subscribeAction2ReportsUnsubscribedOnError() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        Subscription completableSubscription = completable.subscribe(Actions.empty(), Actions.empty());
        
        stringSubject.onError(new TestException());
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
    }

    @Test
    public void subscribeAction2ReportsUnsubscribedAfter() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        final AtomicReference<Subscription> subscriptionRef = new AtomicReference<Subscription>();
        Subscription completableSubscription = completable.subscribe(Actions.empty(), new Action0() {
            @Override
            public void call() {
                if (subscriptionRef.get().isUnsubscribed()) {
                    subscriptionRef.set(null);
                }
            }
        });
        subscriptionRef.set(completableSubscription);
        
        stringSubject.onCompleted();
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
        assertNotNull("Unsubscribed before the call to onCompleted", subscriptionRef.get());
    }

    @Test
    public void subscribeAction2ReportsUnsubscribedOnErrorAfter() {
        PublishSubject<String> stringSubject = PublishSubject.create();
        Completable completable = stringSubject.toCompletable();
        
        final AtomicReference<Subscription> subscriptionRef = new AtomicReference<Subscription>();
        Subscription completableSubscription = completable.subscribe(new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                if (subscriptionRef.get().isUnsubscribed()) {
                    subscriptionRef.set(null);
                }
            }
        }, Actions.empty());
        subscriptionRef.set(completableSubscription);
        
        stringSubject.onError(new TestException());
        
        assertTrue("Not unsubscribed?", completableSubscription.isUnsubscribed());
        assertNotNull("Unsubscribed before the call to onError", subscriptionRef.get());
    }

    private static void expectUncaughtTestException(Action0 action) {
        Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        CapturingUncaughtExceptionHandler handler = new CapturingUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        try {
            action.call();
            assertEquals("Should have received exactly 1 exception", 1, handler.count);
            Throwable caught = handler.caught;
            while (caught != null) {
                if (caught instanceof TestException) break;
                if (caught == caught.getCause()) break;
                caught = caught.getCause();
            }
            assertTrue("A TestException should have been delivered to the handler",
                    caught instanceof TestException);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
    }

    @Test
    public void safeOnCompleteThrows() {
        try {
            normal.completable.subscribe(new CompletableSubscriber() {
    
                @Override
                public void onCompleted() {
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
    }

    @Test
    public void safeOnCompleteThrowsRegularSubscriber() {
        try {
            normal.completable.subscribe(new Subscriber<Object>() {
    
                @Override
                public void onCompleted() {
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
    }

    @Test
    public void safeOnErrorThrows() {
        try {
            error.completable.subscribe(new CompletableSubscriber() {
    
                @Override
                public void onCompleted() {
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
    }

    @Test
    public void safeOnErrorThrowsRegularSubscriber() {
        try {
            error.completable.subscribe(new Subscriber<Object>() {
    
                @Override
                public void onCompleted() {

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
    }

    private static RxJavaCompletableExecutionHook hookSpy;

    @Before
    public void setUp() throws Exception {
        hookSpy = spy(
                new RxJavaPluginsTest.RxJavaCompletableExecutionHookTestImpl());
        Completable.HOOK = hookSpy;
    }

    @Test
    public void testHookCreate() {
        CompletableOnSubscribe subscriber = mock(CompletableOnSubscribe.class);
        Completable.create(subscriber);

        verify(hookSpy, times(1)).onCreate(subscriber);
    }

    @Test
    public void testHookSubscribeStart() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Completable completable = Completable.create(new CompletableOnSubscribe() {
            @Override public void call(CompletableSubscriber s) {
                s.onCompleted();
            }
        });
        completable.subscribe(ts);

        verify(hookSpy, times(1)).onSubscribeStart(eq(completable), any(Completable.CompletableOnSubscribe.class));
    }

    @Test
    public void testHookUnsafeSubscribeStart() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Completable completable = Completable.create(new CompletableOnSubscribe() {
            @Override public void call(CompletableSubscriber s) {
                s.onCompleted();
            }
        });
        completable.unsafeSubscribe(ts);

        verify(hookSpy, times(1)).onSubscribeStart(eq(completable), any(Completable.CompletableOnSubscribe.class));
    }

    @Test
    public void onStartCalledSafe() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>() {
            @Override
            public void onStart() {
                onNext(1);
            }
        };
        
        normal.completable.subscribe(ts);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void onStartCalledUnsafeSafe() {
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
    }

}