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
package rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

public class OnSubscribeUsingTest {

    private interface Resource {
        public String getTextFromWeb();

        public void dispose();
    }

    private static class DisposeAction implements Action1<Resource> {

        @Override
        public void call(Resource r) {
            r.dispose();
        }

    }

    private final Action1<Subscription> disposeSubscription = new Action1<Subscription>() {

        @Override
        public void call(Subscription s) {
            s.unsubscribe();
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

        Func0<Resource> resourceFactory = new Func0<Resource>() {
            @Override
            public Resource call() {
                return resource;
            }
        };

        Func1<Resource, Observable<String>> observableFactory = new Func1<Resource, Observable<String>>() {
            @Override
            public Observable<String> call(Resource resource) {
                return Observable.from(resource.getTextFromWeb().split(" "));
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onCompleted();
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
        Func0<Resource> resourceFactory = new Func0<Resource>() {
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

        Func1<Resource, Observable<String>> observableFactory = new Func1<Resource, Observable<String>>() {
            @Override
            public Observable<String> call(Resource resource) {
                return Observable.from(resource.getTextFromWeb().split(" "));
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        observable.subscribe(observer);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onCompleted();

        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onCompleted();
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
        Func0<Subscription> resourceFactory = new Func0<Subscription>() {
            @Override
            public Subscription call() {
                throw new TestException();
            }
        };

        Func1<Subscription, Observable<Integer>> observableFactory = new Func1<Subscription, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Subscription subscription) {
                return Observable.empty();
            }
        };

        Observable.using(resourceFactory, observableFactory, disposeSubscription).toBlocking()
                .last();
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
        final Action0 unsubscribe = mock(Action0.class);
        Func0<Subscription> resourceFactory = new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return Subscriptions.create(unsubscribe);
            }
        };

        Func1<Subscription, Observable<Integer>> observableFactory = new Func1<Subscription, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Subscription subscription) {
                throw new TestException();
            }
        };

        try {
            Observable.using(resourceFactory, observableFactory, disposeSubscription).toBlocking()
                    .last();
            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe, times(1)).call();
        }
    }

    @Test
    public void testUsingWithObservableFactoryErrorInOnSubscribe() {
        performTestUsingWithObservableFactoryErrorInOnSubscribe(false);
    }

    @Test
    public void testUsingWithObservableFactoryErrorInOnSubscribeDisposeEagerly() {
        performTestUsingWithObservableFactoryErrorInOnSubscribe(true);
    }

    private void performTestUsingWithObservableFactoryErrorInOnSubscribe(boolean disposeEagerly) {
        final Action0 unsubscribe = mock(Action0.class);
        Func0<Subscription> resourceFactory = new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return Subscriptions.create(unsubscribe);
            }
        };

        Func1<Subscription, Observable<Integer>> observableFactory = new Func1<Subscription, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Subscription subscription) {
                return Observable.create(new OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> t1) {
                        throw new TestException();
                    }
                });
            }
        };

        try {
            Observable
                    .using(resourceFactory, observableFactory, disposeSubscription, disposeEagerly)
                    .toBlocking().last();
            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe, times(1)).call();
        }
    }

    @Test
    public void testUsingDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Func0<Resource> resourceFactory = createResourceFactory(events);
        final Action0 completion = createOnCompletedAction(events);
        final Action0 unsub =createUnsubAction(events);

        Func1<Resource, Observable<String>> observableFactory = new Func1<Resource, Observable<String>>() {
            @Override
            public Observable<String> call(Resource resource) {
                return Observable.from(resource.getTextFromWeb().split(" "));
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), true).doOnUnsubscribe(unsub)
                .doOnCompleted(completion);
        observable.subscribe(observer);

        assertEquals(Arrays.asList("disposed", "completed", "unsub"), events);

    }

    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Func0<Resource> resourceFactory = createResourceFactory(events);
        final Action0 completion = createOnCompletedAction(events);
        final Action0 unsub =createUnsubAction(events);

        Func1<Resource, Observable<String>> observableFactory = new Func1<Resource, Observable<String>>() {
            @Override
            public Observable<String> call(Resource resource) {
                return Observable.from(resource.getTextFromWeb().split(" "));
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), false).doOnUnsubscribe(unsub)
                .doOnCompleted(completion);
        observable.subscribe(observer);

        assertEquals(Arrays.asList("completed", "unsub", "disposed"), events);

    }

    
    
    @Test
    public void testUsingDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        Func0<Resource> resourceFactory = createResourceFactory(events);
        final Action1<Throwable> onError = createOnErrorAction(events);
        final Action0 unsub = createUnsubAction(events);
        
        Func1<Resource, Observable<String>> observableFactory = new Func1<Resource, Observable<String>>() {
            @Override
            public Observable<String> call(Resource resource) {
                return Observable.from(resource.getTextFromWeb().split(" ")).concatWith(Observable.<String>error(new RuntimeException()));
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), true).doOnUnsubscribe(unsub)
                .doOnError(onError);
        observable.subscribe(observer);

        assertEquals(Arrays.asList("disposed", "error", "unsub"), events);

    }
    
    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        Func0<Resource> resourceFactory = createResourceFactory(events);
        final Action1<Throwable> onError = createOnErrorAction(events);
        final Action0 unsub = createUnsubAction(events);
        
        Func1<Resource, Observable<String>> observableFactory = new Func1<Resource, Observable<String>>() {
            @Override
            public Observable<String> call(Resource resource) {
                return Observable.from(resource.getTextFromWeb().split(" ")).concatWith(Observable.<String>error(new RuntimeException()));
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), false).doOnUnsubscribe(unsub)
                .doOnError(onError);
        observable.subscribe(observer);

        assertEquals(Arrays.asList("error", "unsub", "disposed"), events);
    }

    private static Action0 createUnsubAction(final List<String> events) {
        return new Action0() {
            @Override
            public void call() {
                events.add("unsub");
            }
        };
    }

    private static Action1<Throwable> createOnErrorAction(final List<String> events) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                events.add("error");
            }
        };
    }

    private static Func0<Resource> createResourceFactory(final List<String> events) {
        return new Func0<Resource>() {
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
    
    private static Action0 createOnCompletedAction(final List<String> events) {
        return new Action0() {
            @Override
            public void call() {
                events.add("completed");
            }
        };
    }
    
}
