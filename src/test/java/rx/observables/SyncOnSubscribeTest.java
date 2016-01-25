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

package rx.observables;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.*;

import rx.*;
import rx.Observable;
import rx.Observable.*;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.*;

/**
 * Test if SyncOnSubscribe adheres to the usual unsubscription and backpressure contracts.
 */
public class SyncOnSubscribeTest {
    
    @Test
    public void testObservableJustEquivalent() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                subscriber.onNext(1);
                subscriber.onCompleted();
            }});
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Observable.create(os).subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1));
    }

    @Test
    public void testStateAfterTerminal() {
        final AtomicInteger finalStateValue = new AtomicInteger(-1);
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
            new Func2<Integer, Observer<? super Integer>, Integer>() {
                @Override
                public Integer call(Integer state, Observer<? super Integer> subscriber) {
                    subscriber.onNext(state);
                    subscriber.onCompleted();
                    return state + 1;
                }}, 
            new Action1<Integer>() {
                @Override
                public void call(Integer t) {
                    finalStateValue.set(t);
                }});
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Observable.create(os).subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertValue(1);
        assertEquals(2, finalStateValue.get());
    }

    @Test
    public void testMultipleOnNextValuesCallsOnError() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                subscriber.onNext(1);
                subscriber.onNext(2);
                subscriber.onCompleted();
            }});
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Observable.create(os).subscribe(o);

        verify(o, times(1)).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(any(IllegalStateException.class));
    }

    @Test
    public void testMultipleOnCompleted() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                subscriber.onNext(1);
                subscriber.onCompleted();
                subscriber.onCompleted();
            }});
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Observable.create(os).subscribe(o);

        verify(o, times(1)).onNext(1);
        verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testOnNextAfterOnComplete() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                subscriber.onNext(1);
                subscriber.onCompleted();
                subscriber.onNext(1);
            }});
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Observable.create(os).subscribe(o);

        verify(o, times(1)).onNext(1);
        verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @SuppressWarnings("serial")
    private static class FooException extends RuntimeException {
        public FooException(String string) {
            super(string);
        }
    }

    @Test
    public void testMultipleOnErrors() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                subscriber.onNext(1);
                subscriber.onError(new TestException("Forced failure 1"));
                subscriber.onError(new FooException("Should not see this error."));
            }});
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Observable.create(os).subscribe(o);

        verify(o, times(1)).onNext(1);
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(isA(TestException.class));
        verify(o, never()).onError(isA(FooException.class));
    }

    @Test
    public void testEmpty() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                subscriber.onCompleted();
            }});

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Observable.create(os).subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onCompleted();
    }

    @Test
    public void testNever() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                
            }});


        Observable<Integer> neverObservable = Observable.create(os).subscribeOn(Schedulers.newThread());
        Observable<? extends Number> merged = Observable.amb(neverObservable, Observable.timer(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.newThread()));
        Iterator<? extends Number> values = merged.toBlocking().toIterable().iterator();

        assertTrue((values.hasNext()));
        assertEquals(0l, values.next());
    }

    @Test
    public void testThrows() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                throw new TestException("Forced failure");
            }});

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Observable.create(os).subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testThrowAfterCompleteFastPath() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                subscriber.onCompleted();
                throw new TestException("Forced failure");
            }});
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Observable.create(os).subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testThrowsSlowPath() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                throw new TestException("Forced failure");
            }});

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        TestSubscriber<Object> ts = new TestSubscriber<Object>(o) {
            @Override
            public void onStart() {
                requestMore(0); // don't start right away
            }
        };

        Observable.create(os).subscribe(ts);

        ts.requestMore(1);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testError() {
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(new Action1<Observer<? super Integer>>() {
            @Override
            public void call(Observer<? super Integer> subscriber) {
                subscriber.onError(new TestException("Forced failure"));
            }});

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Observable.create(os).subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o).onError(any(TestException.class));
        verify(o, never()).onCompleted();
    }

    @Test
    public void testRange() {
        final int start = 1;
        final int count = 4000;
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return start;
            }}, 
            new Func2<Integer, Observer<? super Integer>, Integer>() {
                @Override
                public Integer call(Integer state, Observer<? super Integer> subscriber) {
                    subscriber.onNext(state);
                    if (state == count) {
                        subscriber.onCompleted();
                    }
                    return state + 1;
                }
            });

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        Observable.create(os).subscribe(o);

        verify(o, never()).onError(any(TestException.class));
        inOrder.verify(o, times(count)).onNext(any(Integer.class));
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFromIterable() {
        int n = 400;
        final List<Integer> source = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            source.add(i);
        }
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(
            new Func0<Iterator<Integer>>() {
                @Override
                public Iterator<Integer> call() {
                    return source.iterator();
                }}, 
            new Func2<Iterator<Integer>, Observer<? super Integer>, Iterator<Integer>>() {
                @Override
                public Iterator<Integer> call(Iterator<Integer> it, Observer<? super Integer> observer) {
                    if (it.hasNext()) {
                        observer.onNext(it.next());
                    }
                    if (!it.hasNext()) {
                        observer.onCompleted();
                    }
                    return it;
                }});


        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        Observable.create(os).subscribe(o);

        verify(o, never()).onError(any(TestException.class));
        inOrder.verify(o, times(n)).onNext(any());
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testInfiniteTake() {
        final int start = 0;
        final int finalCount = 4000;
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(
                new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return start;
                    }}, 
                new Func2<Integer, Observer<? super Integer>, Integer>() {
                    @Override
                    public Integer call(Integer state, Observer<? super Integer> observer) {
                        observer.onNext(state);
                        return state + 1;
                    }});
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        Observable.create(os).take(finalCount).subscribe(o);

        verify(o, never()).onError(any(Throwable.class));
        inOrder.verify(o, times(finalCount)).onNext(any());
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testInfiniteRequestSome() {
        final int finalCount = 4000;
        final int start = 0;
        
        @SuppressWarnings("unchecked")
        Action1<? super Integer> onUnSubscribe = mock(Action1.class);
        
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(
                new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return start;
                    }}, 
                new Func2<Integer, Observer<? super Integer>, Integer>() {
                    @Override
                    public Integer call(Integer state, Observer<? super Integer> observer) {
                        observer.onNext(state);
                        return state + 1;
                    }},
                onUnSubscribe);

        TestSubscriber<Object> ts = new TestSubscriber<Object>(0);
        Observable.create(os).subscribe(ts);

        ts.requestMore(finalCount);

        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertValueCount(finalCount);
        // unsubscribe does not take place because subscriber is still in process of requesting
        verify(onUnSubscribe, never()).call(any(Integer.class));
    }

    @Test
    public void testUnsubscribeDownstream() {
        @SuppressWarnings("unchecked")
        Action1<? super Integer> onUnSubscribe = mock(Action1.class);
        
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(
                new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return null;
                    }}, 
                new Func2<Integer, Observer<? super Integer>, Integer>() {
                    @Override
                    public Integer call(Integer state, Observer<? super Integer> observer) {
                        observer.onNext(state);
                        return state;
                    }},
                onUnSubscribe);

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        TestSubscriber<Object> ts = new TestSubscriber<Object>(o);

        Observable.create(os).take(1).subscribe(ts);

        verify(o, never()).onError(any(Throwable.class));
        verify(onUnSubscribe, times(1)).call(any(Integer.class));
    }

    @Test
    public void testConcurrentRequestsLoop() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (i % 10 == 0) {
                System.out.println("testConcurrentRequestsLoop >> " + i);
            }
            testConcurrentRequests();
        }
    }
    
    @Test
    public void testConcurrentRequests() throws InterruptedException {
        final int count1 = 1000;
        final int count2 = 1000;
        final int finalCount = count1 + count2;
        final int start = 1;
        final CountDownLatch l1 = new CountDownLatch(1);
        final CountDownLatch l2 = new CountDownLatch(1);
        
        final CountDownLatch l3 = new CountDownLatch(1);

        final Action1<Object> onUnSubscribe = new Action1<Object>() {
            @Override
            public void call(Object t) {
                l3.countDown();
            }
        };
        
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(
                new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return start;
                    }}, 
                new Func2<Integer, Observer<? super Integer>, Integer>() {
                    @Override
                    public Integer call(Integer state, Observer<? super Integer> observer) {
                        // countdown so the other thread is certain to make a concurrent request
                        l2.countDown();
                        // wait until the 2nd request returns then proceed
                        try {
                            if (!l1.await(2, TimeUnit.SECONDS)) {
                                observer.onError(new TimeoutException());
                                return state + 1;
                            }
                        } catch (InterruptedException e) {
                            observer.onError(e);
                            return state + 1;
                        }
                        observer.onNext(state);
                        
                        if (state == finalCount) {
                            observer.onCompleted();
                        }
                        
                        return state + 1;
                    }},
                onUnSubscribe);

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        final TestSubscriber<Object> ts = new TestSubscriber<Object>(o);
        Observable.create(os).subscribeOn(Schedulers.newThread()).subscribe(ts);

        // wait until the first request has started processing
        if (!l2.await(2, TimeUnit.SECONDS)) {
            fail("SyncOnSubscribe failed to countDown in time");
        }
        // make a concurrent request, this should return
        ts.requestMore(count2);
        // unblock the 1st thread to proceed fulfilling requests
        l1.countDown();

        ts.awaitTerminalEvent(10, TimeUnit.SECONDS);
        ts.assertNoErrors();

        inOrder.verify(o, times(finalCount)).onNext(any());
        inOrder.verify(o, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        if (!l3.await(2, TimeUnit.SECONDS)) {
            fail("SyncOnSubscribe failed to countDown onUnSubscribe latch");
        }
    }

    @Test
    public void testUnsubscribeOutsideOfLoop() throws InterruptedException {
        final AtomicInteger calledUnsubscribe = new AtomicInteger(0);
        final AtomicBoolean currentlyEvaluating = new AtomicBoolean(false);
        
        OnSubscribe<Void> os = SyncOnSubscribe.createStateless(
                new Action1<Observer<? super Void>>() {
                    @Override
                    public void call(Observer<? super Void> observer) {
                        currentlyEvaluating.set(true);
                        observer.onNext(null);
                        currentlyEvaluating.set(false);
                    }},
                new Action0(){
                    @Override
                    public void call() {
                        calledUnsubscribe.incrementAndGet();
                        assertFalse(currentlyEvaluating.get());                        
                    }});

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        final CountDownLatch latch = new CountDownLatch(1);
        final TestSubscriber<Object> ts = new TestSubscriber<Object>(o);
        Observable.create(os).lift(new Operator<Void, Void>(){
            @Override
            public Subscriber<? super Void> call(final Subscriber<? super Void> subscriber) {
                return new Subscriber<Void>(subscriber){
                    @Override
                    public void setProducer(Producer p) {
                        p.request(1);
                    }
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(final Void t) {
                        subscriber.onNext(t);
                        new Thread(new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    latch.await(1, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                unsubscribe();
                                subscriber.onCompleted();
                                latch.countDown();
                            }}).start();
                    }};
            }}).subscribe(ts);
        latch.countDown();
        ts.awaitTerminalEventAndUnsubscribeOnTimeout(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertUnsubscribed();
        assertEquals(1, calledUnsubscribe.get());
    }
    
    @Test
    public void testIndependentStates() {
        int count = 100;
        final ConcurrentHashMap<Object, Object> subscribers = new ConcurrentHashMap<Object, Object>();
        
        @SuppressWarnings("unchecked")
        Action1<? super Map<Object, Object>> onUnSubscribe = mock(Action1.class);
        
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(
            new Func0<Map<Object, Object>>() {
                @Override
                public Map<Object, Object> call() {
                    return subscribers;
                }}, 
            new Func2<Map<Object, Object>, Observer<? super Integer>, Map<Object, Object>>() {
                @Override
                public Map<Object, Object> call(Map<Object, Object> state, Observer<? super Integer> observer) {
                    state.put(observer, observer);
                    observer.onCompleted();
                    return state;
                }},
            onUnSubscribe);
        
        Observable<Integer> source = Observable.create(os);
        for (int i = 0; i < count; i++) {
            source.subscribe();
        }

        assertEquals(count, subscribers.size());
        verify(onUnSubscribe, times(count)).call(Matchers.<Map<Object, Object>>any());
    }

    @Test(timeout = 3000)
    public void testSubscribeOn() {
        final int start = 1;
        final int count = 400;
        final AtomicInteger countUnsubscribe = new AtomicInteger(0);
        final int numSubscribers = 4;
        
        OnSubscribe<Integer> os = SyncOnSubscribe.<Integer, Integer>createStateful(
                new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return start;
                    }}, 
                new Func2<Integer, Observer<? super Integer>, Integer>() {
                    @Override
                    public Integer call(Integer calls, Observer<? super Integer> observer) {
                        if (calls > count) {
                            observer.onCompleted();
                        } else {
                            observer.onNext(calls);
                        }
                        return calls + 1;
                    }},
                new Action1<Integer>() {
                    @Override
                    public void call(Integer t) {
                        countUnsubscribe.incrementAndGet();
                    }});

        List<TestSubscriber<Object>> subs = new ArrayList<TestSubscriber<Object>>(numSubscribers);
        for (int i = 0; i < numSubscribers; i++) {
            TestSubscriber<Object> ts = new TestSubscriber<Object>();
            subs.add(ts);
        }
        TestScheduler scheduler = new TestScheduler();
        Observable<Integer> o2 = Observable.create(os).subscribeOn(scheduler);
        for (Subscriber<Object> ts : subs) {
            o2.subscribe(ts);
        }
        scheduler.triggerActions();
        for (TestSubscriber<Object> ts : subs) {
            ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertValueCount(count);
            ts.assertCompleted();
        }
        
        assertEquals(numSubscribers, countUnsubscribe.get());
    }

    @Test(timeout = 10000)
    public void testObserveOn() {
        final int start = 1;
        final int count = 4000;
        
        @SuppressWarnings("unchecked")
        Action1<? super Integer> onUnSubscribe = mock(Action1.class);
        @SuppressWarnings("unchecked")
        Func0<Integer> generator = mock(Func0.class);
        Mockito.when(generator.call()).thenReturn(start);
        
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateful(generator, 
                new Func2<Integer, Observer<? super Integer>, Integer>() {
                    @Override
                    public Integer call(Integer calls, Observer<? super Integer> observer) {
                        observer.onNext(calls);
                        if (calls == count)
                            observer.onCompleted();
                        return calls + 1;
                    }},
                onUnSubscribe);

        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        TestScheduler scheduler = new TestScheduler();
        Observable.create(os).observeOn(scheduler).subscribe(ts);

        scheduler.triggerActions();
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValueCount(count);
        verify(generator, times(1)).call();

        List<Object> events = ts.getOnNextEvents();
        for (int i = 0; i < events.size(); i++) {
            assertEquals(i + 1, events.get(i));
        }
        verify(onUnSubscribe, times(1)).call(any(Integer.class));
    }

    @Test
    public void testCanRequestInOnNext() {
        Action0 onUnSubscribe = mock(Action0.class);
        
        OnSubscribe<Integer> os = SyncOnSubscribe.createStateless(
                new Action1<Observer<? super Integer>>() {
                    @Override
                    public void call(Observer<? super Integer> observer) {
                        observer.onNext(1);
                        observer.onCompleted();
                    }},
                onUnSubscribe);
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        Observable.create(os).subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                exception.set(e);
            }

            @Override
            public void onNext(Integer t) {
                request(1);
            }
        });
        if (exception.get() != null) {
            exception.get().printStackTrace();
        }
        assertNull(exception.get());
        verify(onUnSubscribe, times(1)).call();
    }

    @Test
    public void testExtendingBase() {
        final AtomicReference<Object> lastState = new AtomicReference<Object>();
        final AtomicInteger countUnsubs = new AtomicInteger(0);
        SyncOnSubscribe<Object, Object> sos = new SyncOnSubscribe<Object, Object>() {
            @Override
            protected Object generateState() {
                Object o = new Object();
                lastState.set(o);
                return o;
            }
            
            @Override
            protected Object next(Object state, Observer<? super Object> observer) {
                observer.onNext(lastState.get());
                assertEquals(lastState.get(), state);
                Object o = new Object();
                lastState.set(o);
                return o;
            }
            
            @Override
            protected void onUnsubscribe(Object state) {
                countUnsubs.incrementAndGet();
                assertEquals(lastState.get(), state);
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        TestSubscriber<Object> ts = new TestSubscriber<Object>(o);

        int count = 10;
        Observable.create(sos).take(count).subscribe(ts);

        verify(o, never()).onError(any(Throwable.class));
        verify(o, times(count)).onNext(any(Object.class));
        verify(o).onCompleted();
        assertEquals(1, countUnsubs.get());
    }
    
    private interface FooQux {}
    private static class Foo implements FooQux {}
    private interface BarQux extends FooQux {}
    private static class Bar extends Foo implements BarQux {}
    
    @Test
    public void testGenericsCreateSingleState() {
        Func0<? extends BarQux> generator = new Func0<Bar>() {
            @Override
            public Bar call() {
                return new Bar();
            }};
        Action2<? super BarQux, Observer<? super FooQux>> next = new Action2<BarQux, Observer<? super FooQux>>() {
            @Override
            public void call(BarQux state, Observer<? super FooQux> observer) {
                observer.onNext(state);
                observer.onCompleted();
            }};
            assertJustBehavior(SyncOnSubscribe.createSingleState(generator, next));
    }

    @Test
    public void testGenericsCreateSingleStateWithUnsub() {
        Func0<? extends BarQux> generator = new Func0<Bar>() {
            @Override
            public Bar call() {
                return new Bar();
            }};
        Action2<? super BarQux, Observer<? super FooQux>> next = new Action2<BarQux, Observer<? super FooQux>>() {
            @Override
            public void call(BarQux state, Observer<? super FooQux> observer) {
                observer.onNext(state);
                observer.onCompleted();
            }};
        Action1<? super BarQux> unsub = new Action1<FooQux>() {
            @Override
            public void call(FooQux t) {
                
            }};
            assertJustBehavior(SyncOnSubscribe.createSingleState(generator, next, unsub));
    }

    @Test
    public void testGenericsCreateStateful() {
        Func0<? extends BarQux> generator = new Func0<Bar>() {
            @Override
            public Bar call() {
                return new Bar();
            }};
        Func2<? super BarQux, Observer<? super FooQux>, ? extends BarQux> next = new Func2<BarQux, Observer<? super FooQux>, BarQux>() {
            @Override
            public BarQux call(BarQux state, Observer<? super FooQux> observer) {
                observer.onNext(state);
                observer.onCompleted();
                return state;
            }};
            assertJustBehavior(SyncOnSubscribe.createStateful(generator, next));
    }

    @Test
    public void testGenericsCreateStatefulWithUnsub() {
        Func0<? extends BarQux> generator = new Func0<Bar>() {
            @Override
            public Bar call() {
                return new Bar();
            }};
        Func2<? super BarQux, Observer<? super FooQux>, ? extends BarQux> next = new Func2<BarQux, Observer<? super FooQux>, BarQux>() {
            @Override
            public BarQux call(BarQux state, Observer<? super FooQux> observer) {
                observer.onNext(state);
                observer.onCompleted();
                return state;
            }};
        Action1<? super BarQux> unsub = new Action1<FooQux>() {
            @Override
            public void call(FooQux t) {
                
            }};
        OnSubscribe<FooQux> os = SyncOnSubscribe.createStateful(generator, next, unsub);
        assertJustBehavior(os);
    }
    
    @Test
    public void testGenericsCreateStateless() {
        Action1<Observer<? super FooQux>> next = new Action1<Observer<? super FooQux>>() {
            @Override
            public void call(Observer<? super FooQux> observer) {
                observer.onNext(new Foo());
                observer.onCompleted();
            }};
        OnSubscribe<FooQux> os = SyncOnSubscribe.createStateless(next);
        assertJustBehavior(os);
    }

    @Test
    public void testGenericsCreateStatelessWithUnsub() {
        Action1<Observer<? super FooQux>> next = new Action1<Observer<? super FooQux>>() {
            @Override
            public void call(Observer<? super FooQux> observer) {
                observer.onNext(new Foo());
                observer.onCompleted();
            }};
        Action0 unsub = new Action0() {
            @Override
            public void call() {
                
            }};
        OnSubscribe<FooQux> os = SyncOnSubscribe.createStateless(next, unsub);
        assertJustBehavior(os);
    }

    private void assertJustBehavior(OnSubscribe<FooQux> os) {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        os.call(ts);
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }
    
    @Test
    public void testConcurrentUnsubscribe3000Iterations() throws InterruptedException, BrokenBarrierException, ExecutionException{
        ExecutorService exec = null;
        try {
            exec = Executors.newSingleThreadExecutor(); 
            for (int i = 0; i < 3000; i++) {
                final AtomicInteger wip = new AtomicInteger();
                
                Func0<AtomicInteger> func0 = new Func0<AtomicInteger>() {
                    @Override
                    public AtomicInteger call() {
                        return wip;
                    }
                };
                Func2<AtomicInteger, Observer<? super Integer>, AtomicInteger> func2 = 
                        new Func2<AtomicInteger, Observer<? super Integer>, AtomicInteger>() {
                    @Override
                    public AtomicInteger call(AtomicInteger s, Observer<? super Integer> o) { 
                        o.onNext(1);
                        return s;
                    }
                };
                Action1<? super AtomicInteger> action1 = new Action1<AtomicInteger>() {
                    @Override
                    public void call(AtomicInteger s) {
                        s.getAndIncrement();
                    }
                };
                Observable<Integer> source = Observable.create(
                        SyncOnSubscribe.createStateful( 
                        func0, 
                        func2, action1
                ));
            
            
                final TestSubscriber<Integer> ts = TestSubscriber.create(0);
                source.subscribe(ts);
            
                final CyclicBarrier cb = new CyclicBarrier(2);
            
                Future<?> f = exec.submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        cb.await();
                        ts.requestMore(1);
                        return null;
                    }
                });
            
                cb.await();
                ts.unsubscribe();
                f.get();
                assertEquals("Unsubscribe supposed to be called once", 1, wip.get());
            }
        } finally {
            if (exec != null) exec.shutdownNow();
        }
    }
    
    @Test
    public void testStateThrows() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        
        SyncOnSubscribe.<Object, Object>createSingleState(
                new Func0<Object>() {
                    @Override
                    public Object call() {
                        throw new TestException();
                    }
                }
        , new Action2<Object, Observer<Object>>() {
            @Override
            public void call(Object s, Observer<? super Object> o) { 
                
            }
        }).call(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
}
