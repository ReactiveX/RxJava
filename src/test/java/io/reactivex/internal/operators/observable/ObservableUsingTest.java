/**
 * Copyright 2016 Netflix, Inc.
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
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.Callable;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;

public class ObservableUsingTest {

    private interface Resource {
        public String getTextFromWeb();

        public void dispose();
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

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource res) {
                return Observable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Observer<String> NbpObserver = TestHelper.mockObserver();
        
        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("Hello");
        inOrder.verify(NbpObserver, times(1)).onNext("world!");
        inOrder.verify(NbpObserver, times(1)).onComplete();
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

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource res) {
                    return Observable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Observer<String> NbpObserver = TestHelper.mockObserver();

        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        o.subscribe(NbpObserver);
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        inOrder.verify(NbpObserver, times(1)).onNext("Hello");
        inOrder.verify(NbpObserver, times(1)).onNext("world!");
        inOrder.verify(NbpObserver, times(1)).onComplete();

        inOrder.verify(NbpObserver, times(1)).onNext("Hello");
        inOrder.verify(NbpObserver, times(1)).onNext("world!");
        inOrder.verify(NbpObserver, times(1)).onComplete();
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

        Function<Disposable, Observable<Integer>> observableFactory = new Function<Disposable, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Disposable s) {
                return Observable.empty();
            }
        };

        Observable.using(resourceFactory, observableFactory, disposeSubscription)
        .blockingLast();
    }

    @Test
    public void testUsingWithObservableFactoryError() {
        performTestUsingWithObservableFactoryError(false);
    }

    @Test
    public void testUsingWithObservableFactoryErrorDisposeEagerly() {
        performTestUsingWithObservableFactoryError(true);
    }

    private void performTestUsingWithObservableFactoryError(boolean disposeEagerly) {
        final Runnable unsubscribe = mock(Runnable.class);
        Callable<Disposable> resourceFactory = new Callable<Disposable>() {
            @Override
            public Disposable call() {
                return Disposables.fromRunnable(unsubscribe);
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
    @Ignore("subscribe() can't throw")
    public void testUsingWithObservableFactoryErrorInOnSubscribe() {
        performTestUsingWithObservableFactoryErrorInOnSubscribe(false);
    }

    @Test
    @Ignore("subscribe() can't throw")
    public void testUsingWithObservableFactoryErrorInOnSubscribeDisposeEagerly() {
        performTestUsingWithObservableFactoryErrorInOnSubscribe(true);
    }

    private void performTestUsingWithObservableFactoryErrorInOnSubscribe(boolean disposeEagerly) {
        final Runnable unsubscribe = mock(Runnable.class);
        Callable<Disposable> resourceFactory = new Callable<Disposable>() {
            @Override
            public Disposable call() {
                return Disposables.fromRunnable(unsubscribe);
            }
        };

        Function<Disposable, Observable<Integer>> observableFactory = new Function<Disposable, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Disposable subscription) {
                return Observable.unsafeCreate(new ObservableSource<Integer>() {
                    @Override
                    public void subscribe(Observer<? super Integer> t1) {
                        throw new TestException();
                    }
                });
            }
        };

        try {
            Observable
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

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Observer<String> NbpObserver = TestHelper.mockObserver();
        
        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnComplete(completion);
        
        o.safeSubscribe(NbpObserver);

        assertEquals(Arrays.asList("disposed", "completed" /* , "unsub" */), events);

    }

    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Callable<Resource> resourceFactory = createResourceFactory(events);
        final Action completion = createOnCompletedAction(events);
        final Action unsub = createUnsubAction(events);

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Observer<String> NbpObserver = TestHelper.mockObserver();
        
        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnComplete(completion);
        
        o.safeSubscribe(NbpObserver);

        assertEquals(Arrays.asList("completed", /*"unsub",*/ "disposed"), events);

    }

    
    
    @Test
    public void testUsingDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        Callable<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Action unsub = createUnsubAction(events);
        
        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Observable.<String>error(new RuntimeException()));
            }
        };

        Observer<String> NbpObserver = TestHelper.mockObserver();
        
        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnError(onError);
        
        o.safeSubscribe(NbpObserver);

        assertEquals(Arrays.asList("disposed", "error" /*, "unsub"*/), events);

    }
    
    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        final Callable<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Action unsub = createUnsubAction(events);
        
        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Observable.<String>error(new RuntimeException()));
            }
        };

        Observer<String> NbpObserver = TestHelper.mockObserver();
        
        Observable<String> o = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnError(onError);
        
        o.safeSubscribe(NbpObserver);

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
    
}
