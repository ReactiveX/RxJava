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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Test;
import org.mockito.InOrder;

import rx.*;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.subscriptions.Subscriptions;

public class SingleOnSubscribeUsingTest {

    private interface Resource {
        String getTextFromWeb();
        
        void dispose();
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
    public void nonEagerly() {
        performTestUsing(false);
    }

    @Test
    public void eagerly() {
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

        Func1<Resource, Single<String>> observableFactory = new Func1<Resource, Single<String>>() {
            @Override
            public Single<String> call(Resource resource) {
                return Single.just(resource.getTextFromWeb());
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Single<String> observable = Single.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext("Hello world!");
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();

        // The resource should be closed
        verify(resource).dispose();
    }

    @Test
    public void withSubscribingTwice() {
        performTestUsingWithSubscribingTwice(false);
    }

    @Test
    public void withSubscribingTwiceDisposeEagerly() {
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

        Func1<Resource, Single<String>> observableFactory = new Func1<Resource, Single<String>>() {
            @Override
            public Single<String> call(Resource resource) {
                return Single.just(resource.getTextFromWeb());
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Single<String> observable = Single.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        observable.subscribe(observer);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer).onNext("Hello world!");
        inOrder.verify(observer).onCompleted();

        inOrder.verify(observer).onNext("Hello world!");
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = TestException.class)
    public void withResourceFactoryError() {
        performTestUsingWithResourceFactoryError(false);
    }

    @Test(expected = TestException.class)
    public void withResourceFactoryErrorDisposeEagerly() {
        performTestUsingWithResourceFactoryError(true);
    }

    private void performTestUsingWithResourceFactoryError(boolean disposeEagerly) {
        Func0<Subscription> resourceFactory = new Func0<Subscription>() {
            @Override
            public Subscription call() {
                throw new TestException();
            }
        };

        Func1<Subscription, Single<Integer>> observableFactory = new Func1<Subscription, Single<Integer>>() {
            @Override
            public Single<Integer> call(Subscription subscription) {
                return Single.just(1);
            }
        };

        Single.using(resourceFactory, observableFactory, disposeSubscription)
        .toBlocking().value();
    }

    @Test
    public void withSingleFactoryError() {
        performTestUsingWithSingleFactoryError(false);
    }

    @Test
    public void withSingleFactoryErrorDisposeEagerly() {
        performTestUsingWithSingleFactoryError(true);
    }

    private void performTestUsingWithSingleFactoryError(boolean disposeEagerly) {
        final Action0 unsubscribe = mock(Action0.class);
        Func0<Subscription> resourceFactory = new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return Subscriptions.create(unsubscribe);
            }
        };

        Func1<Subscription, Single<Integer>> observableFactory = new Func1<Subscription, Single<Integer>>() {
            @Override
            public Single<Integer> call(Subscription subscription) {
                throw new TestException();
            }
        };

        try {
            Single.using(resourceFactory, observableFactory, disposeSubscription)
            .toBlocking().value();
            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe).call();
        }
    }

    @Test
    public void withSingleFactoryErrorInOnSubscribe() {
        performTestUsingWithSingleFactoryErrorInOnSubscribe(false);
    }

    @Test
    public void withSingleFactoryErrorInOnSubscribeDisposeEagerly() {
        performTestUsingWithSingleFactoryErrorInOnSubscribe(true);
    }

    private void performTestUsingWithSingleFactoryErrorInOnSubscribe(boolean disposeEagerly) {
        final Action0 unsubscribe = mock(Action0.class);
        Func0<Subscription> resourceFactory = new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return Subscriptions.create(unsubscribe);
            }
        };

        Func1<Subscription, Single<Integer>> observableFactory = new Func1<Subscription, Single<Integer>>() {
            @Override
            public Single<Integer> call(Subscription subscription) {
                return Single.create(new Single.OnSubscribe<Integer>() {
                    @Override
                    public void call(SingleSubscriber<? super Integer> t1) {
                        throw new TestException();
                    }
                });
            }
        };

        try {
            Single
                    .using(resourceFactory, observableFactory, disposeSubscription, disposeEagerly)
                    .toBlocking().value();
            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe).call();
        }
    }

    @Test
    public void disposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Func0<Resource> resourceFactory = createResourceFactory(events);
        final Action1<String> completion = createOnSuccessAction(events);
        final Action0 unsub =createUnsubAction(events);

        Func1<Resource, Single<String>> observableFactory = new Func1<Resource, Single<String>>() {
            @Override
            public Single<String> call(Resource resource) {
                return Single.just(resource.getTextFromWeb());
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Single<String> observable = Single.using(resourceFactory, observableFactory,
                new DisposeAction(), true).doOnUnsubscribe(unsub)
                .doOnSuccess(completion);
        observable.subscribe(observer);

        assertEquals(Arrays.asList("disposed", "completed", "unsub"), events);

    }

    @Test
    public void doesNotDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Func0<Resource> resourceFactory = createResourceFactory(events);
        final Action1<String> completion = createOnSuccessAction(events);
        final Action0 unsub =createUnsubAction(events);

        Func1<Resource, Single<String>> observableFactory = new Func1<Resource, Single<String>>() {
            @Override
            public Single<String> call(Resource resource) {
                return Single.just(resource.getTextFromWeb());
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Single<String> observable = Single.using(resourceFactory, observableFactory,
                new DisposeAction(), false).doOnUnsubscribe(unsub)
                .doOnSuccess(completion);
        observable.subscribe(observer);

        assertEquals(Arrays.asList("completed", "unsub", "disposed"), events);

    }

    
    
    @Test
    public void disposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        Func0<Resource> resourceFactory = createResourceFactory(events);
        final Action1<Throwable> onError = createOnErrorAction(events);
        final Action0 unsub = createUnsubAction(events);
        
        Func1<Resource, Single<String>> observableFactory = new Func1<Resource, Single<String>>() {
            @Override
            public Single<String> call(Resource resource) {
                return Single.<String>error(new RuntimeException());
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Single<String> observable = Single.using(resourceFactory, observableFactory,
                new DisposeAction(), true).doOnUnsubscribe(unsub)
                .doOnError(onError);
        observable.subscribe(observer);

        assertEquals(Arrays.asList("disposed", "error", "unsub"), events);

    }
    
    @Test
    public void doesNotDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        Func0<Resource> resourceFactory = createResourceFactory(events);
        final Action1<Throwable> onError = createOnErrorAction(events);
        final Action0 unsub = createUnsubAction(events);
        
        Func1<Resource, Single<String>> observableFactory = new Func1<Resource, Single<String>>() {
            @Override
            public Single<String> call(Resource resource) {
                return Single.<String>error(new RuntimeException());
            }
        };

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Single<String> observable = Single.using(resourceFactory, observableFactory,
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
    
    private static Action1<String> createOnSuccessAction(final List<String> events) {
        return new Action1<String>() {
            @Override
            public void call(String s) {
                events.add("completed");
            }
        };
    }

    @Test
    public void nullResourceFactory() {
        try {
            final Resource resource = mock(Resource.class);
            when(resource.getTextFromWeb()).thenReturn("Hello world!");

            Func1<Resource, Single<String>> observableFactory = new Func1<Resource, Single<String>>() {
                @Override
                public Single<String> call(Resource resource) {
                    return Single.just(resource.getTextFromWeb());
                }
            };

            Single.using(null, observableFactory,
                    new DisposeAction(), false);
            
            fail("Failed to throw NullPointerException");
        } catch (NullPointerException ex) {
            assertEquals("resourceFactory is null", ex.getMessage());
        }
    }

    @Test
    public void nullSingeFactory() {
        try {
            final Resource resource = mock(Resource.class);
            when(resource.getTextFromWeb()).thenReturn("Hello world!");

            Func0<Resource> resourceFactory = new Func0<Resource>() {
                @Override
                public Resource call() {
                    return resource;
                }
            };

            Single.using(resourceFactory, null,
                    new DisposeAction(), false);
            
            fail("Failed to throw NullPointerException");
        } catch (NullPointerException ex) {
            assertEquals("singleFactory is null", ex.getMessage());
        }
    }

    @Test
    public void nullDisposeAction() {
        try {
            final Resource resource = mock(Resource.class);
            when(resource.getTextFromWeb()).thenReturn("Hello world!");

            Func0<Resource> resourceFactory = new Func0<Resource>() {
                @Override
                public Resource call() {
                    return resource;
                }
            };

            Func1<Resource, Single<String>> observableFactory = new Func1<Resource, Single<String>>() {
                @Override
                public Single<String> call(Resource resource) {
                    return Single.just(resource.getTextFromWeb());
                }
            };

            Single.using(resourceFactory, observableFactory,
                    null, false);
            
            fail("Failed to throw NullPointerException");
        } catch (NullPointerException ex) {
            assertEquals("disposeAction is null", ex.getMessage());
        }
    }

}
