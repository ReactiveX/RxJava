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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableUsingTest {

    private interface Resource {
        String getTextFromWeb();

        void dispose();
    }

    private static class DisposeAction implements Consumer<Resource> {

        @Override
        public void accept(Resource r) {
            r.dispose();
        }

    }

    private final Consumer<Disposable> disposeSubscription = new Consumer<Disposable>() {

        @Override
        public void accept(Disposable s) {
            s.dispose();
        }

    };

    @Test
    public void testUsing() {
        performTestUsing(false);
    }

    @Test
    public void testUsingEagerly() {
        performTestUsing(true);
    }

    private void performTestUsing(boolean disposeEagerly) {
        final Resource resource = mock(Resource.class);
        when(resource.getTextFromWeb()).thenReturn("Hello world!");

        Callable<Resource> resourceFactory = new Callable<Resource>() {
            @Override
            public Resource call() {
                return resource;
            }
        };

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource res) {
                return Flowable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();

        // The resouce should be closed
        verify(resource, times(1)).dispose();
    }

    @Test
    public void testUsingWithSubscribingTwice() {
        performTestUsingWithSubscribingTwice(false);
    }

    @Test
    public void testUsingWithSubscribingTwiceDisposeEagerly() {
        performTestUsingWithSubscribingTwice(true);
    }

    private void performTestUsingWithSubscribingTwice(boolean disposeEagerly) {
        // When subscribe is called, a new resource should be created.
        Callable<Resource> resourceFactory = new Callable<Resource>() {
            @Override
            public Resource call() {
                return new Resource() {

                    boolean first = true;

                    @Override
                    public String getTextFromWeb() {
                        if (first) {
                            first = false;
                            return "Hello world!";
                        }
                        return "Nothing";
                    }

                    @Override
                    public void dispose() {
                        // do nothing
                    }

                };
            }
        };

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource res) {
                    return Flowable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        observable.subscribe(observer);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onComplete();

        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = TestException.class)
    public void testUsingWithResourceFactoryError() {
        performTestUsingWithResourceFactoryError(false);
    }

    @Test(expected = TestException.class)
    public void testUsingWithResourceFactoryErrorDisposeEagerly() {
        performTestUsingWithResourceFactoryError(true);
    }

    private void performTestUsingWithResourceFactoryError(boolean disposeEagerly) {
        Callable<Disposable> resourceFactory = new Callable<Disposable>() {
            @Override
            public Disposable call() {
                throw new TestException();
            }
        };

        Function<Disposable, Flowable<Integer>> observableFactory = new Function<Disposable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Disposable s) {
                return Flowable.empty();
            }
        };

        Flowable.using(resourceFactory, observableFactory, disposeSubscription)
        .blockingLast();
    }

    @Test
    public void testUsingWithFlowableFactoryError() {
        performTestUsingWithFlowableFactoryError(false);
    }

    @Test
    public void testUsingWithFlowableFactoryErrorDisposeEagerly() {
        performTestUsingWithFlowableFactoryError(true);
    }

    private void performTestUsingWithFlowableFactoryError(boolean disposeEagerly) {
        final Runnable unsubscribe = mock(Runnable.class);
        Callable<Disposable> resourceFactory = new Callable<Disposable>() {
            @Override
            public Disposable call() {
                return Disposables.fromRunnable(unsubscribe);
            }
        };

        Function<Disposable, Flowable<Integer>> observableFactory = new Function<Disposable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Disposable subscription) {
                throw new TestException();
            }
        };

        try {
            Flowable.using(resourceFactory, observableFactory, disposeSubscription).blockingLast();
            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe, times(1)).run();
        }
    }

    @Test
    @Ignore("subscribe() can't throw")
    public void testUsingWithFlowableFactoryErrorInOnSubscribe() {
        performTestUsingWithFlowableFactoryErrorInOnSubscribe(false);
    }

    @Test
    @Ignore("subscribe() can't throw")
    public void testUsingWithFlowableFactoryErrorInOnSubscribeDisposeEagerly() {
        performTestUsingWithFlowableFactoryErrorInOnSubscribe(true);
    }

    private void performTestUsingWithFlowableFactoryErrorInOnSubscribe(boolean disposeEagerly) {
        final Runnable unsubscribe = mock(Runnable.class);
        Callable<Disposable> resourceFactory = new Callable<Disposable>() {
            @Override
            public Disposable call() {
                return Disposables.fromRunnable(unsubscribe);
            }
        };

        Function<Disposable, Flowable<Integer>> observableFactory = new Function<Disposable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Disposable subscription) {
                return Flowable.unsafeCreate(new Publisher<Integer>() {
                    @Override
                    public void subscribe(Subscriber<? super Integer> t1) {
                        throw new TestException();
                    }
                });
            }
        };

        try {
            Flowable
            .using(resourceFactory, observableFactory, disposeSubscription, disposeEagerly)
            .blockingLast();

            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe, times(1)).run();
        }
    }

    @Test
    public void testUsingDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Callable<Resource> resourceFactory = createResourceFactory(events);
        final Action completion = createOnCompletedAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnComplete(completion);

        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("disposed", "completed"), events);

    }

    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Callable<Resource> resourceFactory = createResourceFactory(events);
        final Action completion = createOnCompletedAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnComplete(completion);

        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("completed", "disposed"), events);

    }



    @Test
    public void testUsingDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        Callable<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Flowable.<String>error(new RuntimeException()));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnError(onError);

        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("disposed", "error"), events);

    }

    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        final Callable<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Flowable.<String>error(new RuntimeException()));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnError(onError);

        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("error", "disposed"), events);
    }

    private static Action createUnsubAction(final List<String> events) {
        return new Action() {
            @Override
            public void run() {
                events.add("unsub");
            }
        };
    }

    private static Consumer<Throwable> createOnErrorAction(final List<String> events) {
        return new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) {
                events.add("error");
            }
        };
    }

    private static Callable<Resource> createResourceFactory(final List<String> events) {
        return new Callable<Resource>() {
            @Override
            public Resource call() {
                return new Resource() {

                    @Override
                    public String getTextFromWeb() {
                        return "hello world";
                    }

                    @Override
                    public void dispose() {
                        events.add("disposed");
                    }
                };
            }
        };
    }

    private static Action createOnCompletedAction(final List<String> events) {
        return new Action() {
            @Override
            public void run() {
                events.add("completed");
            }
        };
    }

    @Test
    public void factoryThrows() {

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger count = new AtomicInteger();

        Flowable.<Integer, Integer>using(
                new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        return 1;
                    }
                },
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer v) {
                        throw new TestException("forced failure");
                    }
                },
                new Consumer<Integer>() {
                    @Override
                    public void accept(Integer c) {
                        count.incrementAndGet();
                    }
                }
        )
        .subscribe(ts);

        ts.assertError(TestException.class);

        Assert.assertEquals(1, count.get());
    }

    @Test
    public void nonEagerTermination() {

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger count = new AtomicInteger();

        Flowable.<Integer, Integer>using(
                new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        return 1;
                    }
                },
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer v) {
                        return Flowable.just(v);
                    }
                },
                new Consumer<Integer>() {
                    @Override
                    public void accept(Integer c) {
                        count.incrementAndGet();
                    }
                }, false
        )
        .subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertEquals(1, count.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.using(
                new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return 1;
                    }
                },
                new Function<Object, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> apply(Object v) throws Exception {
                        return Flowable.never();
                    }
                },
                Functions.emptyConsumer()
        ));
    }

    @Test
    public void supplierDisposerCrash() {
        TestSubscriber<Object> to = Flowable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Object v) throws Exception {
                throw new TestException("First");
            }
        }, new Consumer<Object>() {
            @Override
            public void accept(Object e) throws Exception {
                throw new TestException("Second");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "First");
        TestHelper.assertError(errors, 1, TestException.class, "Second");
    }

    @Test
    public void eagerOnErrorDisposerCrash() {
        TestSubscriber<Object> to = Flowable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Object v) throws Exception {
                return Flowable.error(new TestException("First"));
            }
        }, new Consumer<Object>() {
            @Override
            public void accept(Object e) throws Exception {
                throw new TestException("Second");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "First");
        TestHelper.assertError(errors, 1, TestException.class, "Second");
    }

    @Test
    public void eagerOnCompleteDisposerCrash() {
        Flowable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Object v) throws Exception {
                return Flowable.empty();
            }
        }, new Consumer<Object>() {
            @Override
            public void accept(Object e) throws Exception {
                throw new TestException("Second");
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "Second");
    }

    @Test
    public void nonEagerDisposerCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function<Object, Flowable<Object>>() {
                @Override
                public Flowable<Object> apply(Object v) throws Exception {
                    return Flowable.empty();
                }
            }, new Consumer<Object>() {
                @Override
                public void accept(Object e) throws Exception {
                    throw new TestException("Second");
                }
            }, false)
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void sourceSupplierReturnsNull() {
        Flowable.using(Functions.justCallable(1),
                Functions.justFunction((Publisher<Object>)null),
                Functions.emptyConsumer())
        .test()
        .assertFailureAndMessage(NullPointerException.class, "The sourceSupplier returned a null Publisher")
        ;
    }
}
