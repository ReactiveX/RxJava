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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableUsingTest extends RxJavaTest {

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
        public void accept(Disposable d) {
            d.dispose();
        }

    };

    @Test
    public void using() {
        performTestUsing(false);
    }

    @Test
    public void usingEagerly() {
        performTestUsing(true);
    }

    private void performTestUsing(boolean disposeEagerly) {
        final Resource resource = mock(Resource.class);
        when(resource.getTextFromWeb()).thenReturn("Hello world!");

        Supplier<Resource> resourceFactory = new Supplier<Resource>() {
            @Override
            public Resource get() {
                return resource;
            }
        };

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource res) {
                return Flowable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> flowable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext("Hello");
        inOrder.verify(subscriber, times(1)).onNext("world!");
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();

        // The resouce should be closed
        verify(resource, times(1)).dispose();
    }

    @Test
    public void usingWithSubscribingTwice() {
        performTestUsingWithSubscribingTwice(false);
    }

    @Test
    public void usingWithSubscribingTwiceDisposeEagerly() {
        performTestUsingWithSubscribingTwice(true);
    }

    private void performTestUsingWithSubscribingTwice(boolean disposeEagerly) {
        // When subscribe is called, a new resource should be created.
        Supplier<Resource> resourceFactory = new Supplier<Resource>() {
            @Override
            public Resource get() {
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

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> flowable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        flowable.subscribe(subscriber);
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);

        inOrder.verify(subscriber, times(1)).onNext("Hello");
        inOrder.verify(subscriber, times(1)).onNext("world!");
        inOrder.verify(subscriber, times(1)).onComplete();

        inOrder.verify(subscriber, times(1)).onNext("Hello");
        inOrder.verify(subscriber, times(1)).onNext("world!");
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = TestException.class)
    public void usingWithResourceFactoryError() {
        performTestUsingWithResourceFactoryError(false);
    }

    @Test(expected = TestException.class)
    public void usingWithResourceFactoryErrorDisposeEagerly() {
        performTestUsingWithResourceFactoryError(true);
    }

    private void performTestUsingWithResourceFactoryError(boolean disposeEagerly) {
        Supplier<Disposable> resourceFactory = new Supplier<Disposable>() {
            @Override
            public Disposable get() {
                throw new TestException();
            }
        };

        Function<Disposable, Flowable<Integer>> observableFactory = new Function<Disposable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Disposable d) {
                return Flowable.empty();
            }
        };

        Flowable.using(resourceFactory, observableFactory, disposeSubscription)
        .blockingLast();
    }

    @Test
    public void usingWithFlowableFactoryError() {
        performTestUsingWithFlowableFactoryError(false);
    }

    @Test
    public void usingWithFlowableFactoryErrorDisposeEagerly() {
        performTestUsingWithFlowableFactoryError(true);
    }

    private void performTestUsingWithFlowableFactoryError(boolean disposeEagerly) {
        final Runnable unsubscribe = mock(Runnable.class);
        Supplier<Disposable> resourceFactory = new Supplier<Disposable>() {
            @Override
            public Disposable get() {
                return Disposable.fromRunnable(unsubscribe);
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
    public void usingDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<>();
        Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Action completion = createOnCompletedAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> flowable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnComplete(completion);

        flowable.safeSubscribe(subscriber);

        assertEquals(Arrays.asList("disposed", "completed"), events);

    }

    @Test
    public void usingDoesNotDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<>();
        Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Action completion = createOnCompletedAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> flowable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnComplete(completion);

        flowable.safeSubscribe(subscriber);

        assertEquals(Arrays.asList("completed", "disposed"), events);

    }

    @Test
    public void usingDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<>();
        Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Flowable.<String>error(new RuntimeException()));
            }
        };

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> flowable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnError(onError);

        flowable.safeSubscribe(subscriber);

        assertEquals(Arrays.asList("disposed", "error"), events);

    }

    @Test
    public void usingDoesNotDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<>();
        final Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Flowable<String>> observableFactory = new Function<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Flowable.<String>error(new RuntimeException()));
            }
        };

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        Flowable<String> flowable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnError(onError);

        flowable.safeSubscribe(subscriber);

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

    private static Supplier<Resource> createResourceFactory(final List<String> events) {
        return new Supplier<Resource>() {
            @Override
            public Resource get() {
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
                new Supplier<Integer>() {
                    @Override
                    public Integer get() {
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
                new Supplier<Integer>() {
                    @Override
                    public Integer get() {
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
                new Supplier<Object>() {
                    @Override
                    public Object get() throws Exception {
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
        TestSubscriberEx<Object> ts = Flowable.using(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
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
        .to(TestHelper.<Object>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(ts.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "First");
        TestHelper.assertError(errors, 1, TestException.class, "Second");
    }

    @Test
    public void eagerOnErrorDisposerCrash() {
        TestSubscriberEx<Object> ts = Flowable.using(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
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
        .to(TestHelper.<Object>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(ts.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "First");
        TestHelper.assertError(errors, 1, TestException.class, "Second");
    }

    @Test
    public void eagerOnCompleteDisposerCrash() {
        Flowable.using(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
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
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "Second");
    }

    @Test
    public void nonEagerDisposerCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.using(new Supplier<Object>() {
                @Override
                public Object get() throws Exception {
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
        Flowable.using(Functions.justSupplier(1),
                Functions.justFunction((Publisher<Object>)null),
                Functions.emptyConsumer())
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The sourceSupplier returned a null Publisher")
        ;
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f)
                    throws Exception {
                return Flowable.using(Functions.justSupplier(1), Functions.justFunction(f), Functions.emptyConsumer());
            }
        });
    }

    @Test
    public void eagerDisposedOnComplete() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.using(Functions.justSupplier(1), Functions.justFunction(new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                ts.cancel();
                subscriber.onComplete();
            }
        }), Functions.emptyConsumer(), true)
        .subscribe(ts);
    }

    @Test
    public void eagerDisposedOnError() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.using(Functions.justSupplier(1), Functions.justFunction(new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                ts.cancel();
                subscriber.onError(new TestException());
            }
        }), Functions.emptyConsumer(), true)
        .subscribe(ts);
    }

    @Test
    public void eagerDisposeResourceThenDisposeUpstream() {
        final StringBuilder sb = new StringBuilder();

        Flowable.using(Functions.justSupplier(1),
            new Function<Integer, Flowable<Integer>>() {
                @Override
                public Flowable<Integer> apply(Integer t) throws Throwable {
                    return Flowable.range(1, 2)
                            .doOnCancel(new Action() {
                                @Override
                                public void run() throws Throwable {
                                    sb.append("Cancel");
                                }
                            })
                            ;
                }
            }, new Consumer<Integer>() {
                @Override
                public void accept(Integer t) throws Throwable {
                    sb.append("Resource");
                }
            }, true)
        .take(1)
        .test()
        .assertResult(1);

        assertEquals("ResourceCancel", sb.toString());
    }

    @Test
    public void nonEagerDisposeUpstreamThenDisposeResource() {
        final StringBuilder sb = new StringBuilder();

        Flowable.using(Functions.justSupplier(1),
            new Function<Integer, Flowable<Integer>>() {
                @Override
                public Flowable<Integer> apply(Integer t) throws Throwable {
                    return Flowable.range(1, 2)
                            .doOnCancel(new Action() {
                                @Override
                                public void run() throws Throwable {
                                    sb.append("Cancel");
                                }
                            });
                }
            }, new Consumer<Integer>() {
                @Override
                public void accept(Integer t) throws Throwable {
                    sb.append("Resource");
                }
            }, false)
        .take(1)
        .test()
        .assertResult(1);

        assertEquals("CancelResource", sb.toString());
    }
}
