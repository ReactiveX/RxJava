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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableUsingTest extends RxJavaTest {

    interface Resource {
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

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource res) {
                return Observable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onComplete();
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

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource res) {
                    return Observable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        o.subscribe(observer);
        o.subscribe(observer);

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

        Function<Disposable, Observable<Integer>> observableFactory = new Function<Disposable, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Disposable d) {
                return Observable.empty();
            }
        };

        Observable.using(resourceFactory, observableFactory, disposeSubscription)
        .blockingLast();
    }

    @Test
    public void usingWithObservableFactoryError() {
        performTestUsingWithObservableFactoryError(false);
    }

    @Test
    public void usingWithObservableFactoryErrorDisposeEagerly() {
        performTestUsingWithObservableFactoryError(true);
    }

    private void performTestUsingWithObservableFactoryError(boolean disposeEagerly) {
        final Runnable unsubscribe = mock(Runnable.class);
        Supplier<Disposable> resourceFactory = new Supplier<Disposable>() {
            @Override
            public Disposable get() {
                return Disposable.fromRunnable(unsubscribe);
            }
        };

        Function<Disposable, Observable<Integer>> observableFactory = new Function<Disposable, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Disposable subscription) {
                throw new TestException();
            }
        };

        try {
            Observable.using(resourceFactory, observableFactory, disposeSubscription).blockingLast();
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

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnDispose(unsub)
        .doOnComplete(completion);

        o.safeSubscribe(observer);

        assertEquals(Arrays.asList("disposed", "completed" /* , "unsub" */), events);

    }

    @Test
    public void usingDoesNotDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<>();
        Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Action completion = createOnCompletedAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnDispose(unsub)
        .doOnComplete(completion);

        o.safeSubscribe(observer);

        assertEquals(Arrays.asList("completed", /*"unsub",*/ "disposed"), events);

    }

    @Test
    public void usingDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<>();
        Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Observable.<String>error(new RuntimeException()));
            }
        };

        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnDispose(unsub)
        .doOnError(onError);

        o.safeSubscribe(observer);

        assertEquals(Arrays.asList("disposed", "error" /*, "unsub"*/), events);

    }

    @Test
    public void usingDoesNotDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<>();
        final Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Observable.<String>error(new RuntimeException()));
            }
        };

        Observer<String> observer = TestHelper.mockObserver();

        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnDispose(unsub)
        .doOnError(onError);

        o.safeSubscribe(observer);

        assertEquals(Arrays.asList("error", /* "unsub",*/ "disposed"), events);
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
    public void dispose() {
        TestHelper.checkDisposed(Observable.using(
                new Supplier<Object>() {
                    @Override
                    public Object get() throws Exception {
                        return 1;
                    }
                },
                new Function<Object, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Object v) throws Exception {
                        return Observable.never();
                    }
                },
                Functions.emptyConsumer()
        ));
    }

    @Test
    public void supplierDisposerCrash() {
        TestObserverEx<Object> to = Observable.using(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        }, new Function<Object, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Object v) throws Exception {
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

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "First");
        TestHelper.assertError(errors, 1, TestException.class, "Second");
    }

    @Test
    public void eagerOnErrorDisposerCrash() {
        TestObserverEx<Object> to = Observable.using(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        }, new Function<Object, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Object v) throws Exception {
                return Observable.error(new TestException("First"));
            }
        }, new Consumer<Object>() {
            @Override
            public void accept(Object e) throws Exception {
                throw new TestException("Second");
            }
        })
        .to(TestHelper.<Object>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "First");
        TestHelper.assertError(errors, 1, TestException.class, "Second");
    }

    @Test
    public void eagerOnCompleteDisposerCrash() {
        Observable.using(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        }, new Function<Object, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Object v) throws Exception {
                return Observable.empty();
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
            Observable.using(new Supplier<Object>() {
                @Override
                public Object get() throws Exception {
                    return 1;
                }
            }, new Function<Object, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Object v) throws Exception {
                    return Observable.empty();
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
        Observable.using(Functions.justSupplier(1),
                Functions.justFunction((Observable<Object>)null),
                Functions.emptyConsumer())
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The sourceSupplier returned a null ObservableSource")
        ;
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o)
                    throws Exception {
                return Observable.using(Functions.justSupplier(1), Functions.justFunction(o), Functions.emptyConsumer());
            }
        });
    }

    @Test
    public void eagerDisposedOnComplete() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.using(Functions.justSupplier(1), Functions.justFunction(new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                to.dispose();
                observer.onComplete();
            }
        }), Functions.emptyConsumer(), true)
        .subscribe(to);
    }

    @Test
    public void eagerDisposedOnError() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.using(Functions.justSupplier(1), Functions.justFunction(new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                to.dispose();
                observer.onError(new TestException());
            }
        }), Functions.emptyConsumer(), true)
        .subscribe(to);
    }

    @Test
    public void eagerDisposeResourceThenDisposeUpstream() {
        final StringBuilder sb = new StringBuilder();

        Observable.using(Functions.justSupplier(1),
            new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) throws Throwable {
                    return Observable.range(1, 2)
                            .doOnDispose(new Action() {
                                @Override
                                public void run() throws Throwable {
                                    sb.append("Dispose");
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

        assertEquals("ResourceDispose", sb.toString());
    }

    @Test
    public void nonEagerDisposeUpstreamThenDisposeResource() {
        final StringBuilder sb = new StringBuilder();

        Observable.using(Functions.justSupplier(1),
            new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) throws Throwable {
                    return Observable.range(1, 2)
                            .doOnDispose(new Action() {
                                @Override
                                public void run() throws Throwable {
                                    sb.append("Dispose");
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

        assertEquals("DisposeResource", sb.toString());
    }
}
