/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.function.*;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.Observable;
import io.reactivex.TestHelper;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;

public class OnSubscribeUsingTest {

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

        Supplier<Resource> resourceFactory = () -> resource;

        Function<Resource, Observable<String>> observableFactory = res -> {
            return Observable.fromArray(res.getTextFromWeb().split(" "));
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
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

        Function<Resource, Observable<String>> observableFactory = res -> {
                return Observable.fromArray(res.getTextFromWeb().split(" "));
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
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
        Supplier<Disposable> resourceFactory = () -> {
            throw new TestException();
        };

        Function<Disposable, Observable<Integer>> observableFactory = s -> Observable.empty();

        Observable.using(resourceFactory, observableFactory, disposeSubscription)
        .toBlocking()
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
        final Runnable unsubscribe = mock(Runnable.class);
        Supplier<Disposable> resourceFactory = () -> Disposables.from(unsubscribe);

        Function<Disposable, Observable<Integer>> observableFactory = subscription -> {
            throw new TestException();
        };

        try {
            Observable.using(resourceFactory, observableFactory, disposeSubscription).toBlocking()
                    .last();
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
        Supplier<Disposable> resourceFactory = new Supplier<Disposable>() {
            @Override
            public Disposable get() {
                return Disposables.from(unsubscribe);
            }
        };

        Function<Disposable, Observable<Integer>> observableFactory = subscription -> {
            return Observable.create(t1 -> {
                throw new TestException();
            });
        };

        try {
            Observable
            .using(resourceFactory, observableFactory, disposeSubscription, disposeEagerly)
            .toBlocking().last();
            
            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe, times(1)).run();
        }
    }

    @Test
    public void testUsingDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<>();
        Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Runnable completion = createOnCompletedAction(events);
        final Runnable unsub =createUnsubAction(events);

        Function<Resource, Observable<String>> observableFactory = resource -> Observable.fromArray(resource.getTextFromWeb().split(" "));

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnComplete(completion);
        
        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("disposed", "completed", "unsub"), events);

    }

    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<>();
        Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Runnable completion = createOnCompletedAction(events);
        final Runnable unsub = createUnsubAction(events);

        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnComplete(completion);
        
        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("completed", "unsub", "disposed"), events);

    }

    
    
    @Test
    public void testUsingDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<>();
        Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Runnable unsub = createUnsubAction(events);
        
        Function<Resource, Observable<String>> observableFactory = new Function<Resource, Observable<String>>() {
            @Override
            public Observable<String> apply(Resource resource) {
                return Observable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Observable.<String>error(new RuntimeException()));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnError(onError);
        
        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("disposed", "error", "unsub"), events);

    }
    
    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<>();
        final Supplier<Resource> resourceFactory = createResourceFactory(events);
        final Consumer<Throwable> onError = createOnErrorAction(events);
        final Runnable unsub = createUnsubAction(events);
        
        Function<Resource, Observable<String>> observableFactory = resource -> {
            return Observable.fromArray(resource.getTextFromWeb().split(" "))
                    .concatWith(Observable.<String>error(new RuntimeException()));
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        Observable<String> observable = Observable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnError(onError);
        
        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("error", "unsub", "disposed"), events);
    }

    private static Runnable createUnsubAction(final List<String> events) {
        return () -> events.add("unsub");
    }

    private static Consumer<Throwable> createOnErrorAction(final List<String> events) {
        return t -> events.add("error");
    }

    private static Supplier<Resource> createResourceFactory(final List<String> events) {
        return () -> new Resource() {

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
    
    private static Runnable createOnCompletedAction(final List<String> events) {
        return () -> events.add("completed");
    }
    
}