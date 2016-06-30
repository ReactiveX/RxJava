/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.observables;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscription;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func3;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class AsyncOnSubscribeTest {

    @Mock
    public Observer<Integer> o;

    TestSubscriber<Integer> subscriber;
    
    @Before
    public void setup() {
        subscriber = new TestSubscriber<Integer>(o, 0L);
    }
    
    @Test
    public void testSerializesConcurrentObservables() throws InterruptedException {
        final TestScheduler scheduler = new TestScheduler();
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                if (state == 1) {
                    Observable<Integer> o1 = Observable.just(1, 2, 3, 4)
                            .delay(100, TimeUnit.MILLISECONDS, scheduler);
                    observer.onNext(o1);
                }
                else if (state == 2) {
                    Observable<Integer> o = Observable.just(5, 6, 7, 8);
                    observer.onNext(o);
                }
                else 
                    observer.onCompleted();
                return state + 1;
            }});
        // initial request emits [[1, 2, 3, 4]] on delay
        Observable.create(os).subscribe(subscriber);
        // next request emits [[5, 6, 7, 8]] firing immediately
        subscriber.requestMore(2);
        // triggers delayed observable
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        subscriber.assertNoErrors();
        subscriber.assertValues(1, 2);
        // final request completes
        subscriber.requestMore(3); 
        subscriber.assertNoErrors();
        subscriber.assertValues(1, 2, 3, 4, 5);
        
        subscriber.requestMore(3);
        
        subscriber.assertNoErrors();
        subscriber.assertValues(1, 2, 3, 4, 5, 6, 7, 8);
        subscriber.assertCompleted();
    }
    
    @Test
    public void testSubscribedByBufferingOperator() {
        final TestScheduler scheduler = new TestScheduler();
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateless(
        new Action2<Long, Observer<Observable<? extends Integer>>>(){
            @Override
            public void call(Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onNext(Observable.range(1, requested.intValue()));
            }});
        Observable.create(os).observeOn(scheduler).subscribe(subscriber);
        subscriber.requestMore(RxRingBuffer.SIZE);
        scheduler.advanceTimeBy(10, TimeUnit.DAYS);
        subscriber.assertNoErrors();
        subscriber.assertValueCount(RxRingBuffer.SIZE);
        subscriber.assertNotCompleted();
    }
    
    @Test
    public void testOnUnsubscribeHasCorrectState() throws InterruptedException {
        final AtomicInteger lastState = new AtomicInteger(-1);
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                if (state < 3) {
                    observer.onNext(Observable.just(state));
                }
                else 
                    observer.onCompleted();
                return state + 1;
            }}, 
        new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                lastState.set(t);
            }});
        Observable.create(os).subscribe(subscriber); 
        subscriber.requestMore(1); // [[1]], state = 1
        subscriber.requestMore(2); // [[1]], state = 2
        subscriber.requestMore(3); // onComplete, state = 3
        subscriber.assertNoErrors();
        subscriber.assertReceivedOnNext(Arrays.asList(new Integer[] {1, 2}));
        subscriber.assertCompleted();
        assertEquals("Final state when unsubscribing is not correct", 4, lastState.get());
    }

    @Test
    public void testOnCompleteOuter() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateless(new Action2<Long, Observer<Observable<? extends Integer>>>(){
            @Override
            public void call(Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onCompleted();
            }});
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void testTryOnNextTwice() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateless(new Action2<Long, Observer<Observable<? extends Integer>>>(){
            @Override
            public void call(Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onNext(Observable.just(1));
                observer.onNext(Observable.just(2));
            }
        });
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertError(IllegalStateException.class);
        subscriber.assertNotCompleted();
        subscriber.assertReceivedOnNext(Arrays.asList(new Integer[] {1}));
    }

    @Test
    public void testThrowException() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateless(
        new Action2<Long, Observer<Observable<? extends Integer>>>(){
            @Override
            public void call(Long requested, Observer<Observable<? extends Integer>> observer) {
                throw new TestException();
            }});
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertError(TestException.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void testThrowExceptionAfterTerminal() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onCompleted();
                throw new TestException();
            }});
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void testOnNextAfterCompleted() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onCompleted();
                observer.onNext(Observable.just(1));
                return 1;
            }});
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void testOnNextAfterError() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onError(new TestException());
                observer.onNext(Observable.just(1));
                return 1;
            }});
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertError(TestException.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void testEmittingEmptyObservable() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onNext(Observable.<Integer>empty());
                observer.onCompleted();
                return state;
            }});
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void testOnErrorOuter() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onError(new TestException());
                return state;
            }
        });
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertError(TestException.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }
    
    @Test
    public void testOnCompleteFollowedByOnErrorOuter() throws InterruptedException {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                observer.onCompleted();
                observer.onError(new TestException());
                return state;
            }
        });
        Observable.create(os).subscribe(subscriber);
        subscriber.requestMore(1);
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        subscriber.assertNoValues();
    }

    @Test
    public void testUnsubscribesFromAllSelfTerminatedObservables() throws InterruptedException {
        final AtomicInteger l1 = new AtomicInteger();
        final AtomicInteger l2 = new AtomicInteger();
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                Observable<Integer> o1;
                switch (state) {
                case 1:
                    o1 = Observable.just(1)
                            .doOnUnsubscribe(new Action0(){
                                @Override
                                public void call() {
                                    l1.incrementAndGet();
                                }});
                    break;
                case 2:
                    o1 = Observable.just(2)
                            .doOnUnsubscribe(new Action0(){
                                @Override
                                public void call() {
                                    l2.incrementAndGet();
                                }});
                    break;
                default:
                    observer.onCompleted();
                    return null;
                }
                observer.onNext(o1);
                return state + 1;
            }});
        Observable.create(os).subscribe(subscriber); 
        subscriber.requestMore(1); // [[1]]
        subscriber.requestMore(2); // [[2]]
        subscriber.requestMore(2); // onCompleted
        subscriber.awaitTerminalEventAndUnsubscribeOnTimeout(100, TimeUnit.MILLISECONDS);
        assertEquals("did not unsub from first observable after terminal", 1, l1.get());
        assertEquals("did not unsub from second observable after terminal", 1, l2.get());
        List<Integer> onNextEvents = subscriber.getOnNextEvents();
        assertEquals(2, onNextEvents.size());
        assertEquals(new Integer(1), onNextEvents.get(0));
        assertEquals(new Integer(2), onNextEvents.get(1));
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
    }
    
    @Test
    public void testUnsubscribesFromAllNonTerminatedObservables() throws InterruptedException {
        final AtomicInteger l1 = new AtomicInteger();
        final AtomicInteger l2 = new AtomicInteger();
        final TestScheduler scheduler = new TestScheduler();
        final AtomicReference<Subscription> sub = new AtomicReference<Subscription>();
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
                @Override
                public Integer call() {
                    return 1;
                }}, 
            new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
                @Override
                public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                    switch (state) {
                    case 1:
                        observer.onNext(Observable.range(1, requested.intValue())
                                .subscribeOn(scheduler)
                                .doOnUnsubscribe(new Action0(){
                                    @Override
                                    public void call() {
                                        l1.incrementAndGet();
                                    }}));
                        break;
                    case 2:
                        observer.onNext(Observable.just(1)
                                .concatWith(Observable.<Integer>never())
                                .subscribeOn(scheduler)
                                .doOnUnsubscribe(new Action0(){
                                    @Override
                                    public void call() {
                                        l2.incrementAndGet();
                                    }}));
                        break;
                    case 3:
                        sub.get().unsubscribe();
                    }
                    return state + 1;
                }});
        Subscription subscription = Observable.create(os)
            .observeOn(scheduler, 1)
            .subscribe(subscriber);
        sub.set(subscription);
        subscriber.assertNoValues();
        subscriber.requestMore(1);
        scheduler.triggerActions();
        subscriber.requestMore(1);
        scheduler.triggerActions();
        subscriber.assertValueCount(2);
        subscriber.assertNotCompleted();
        subscriber.assertNoErrors();
        assertEquals("did not unsub from 1st observable after terminal", 1, l1.get());
        assertEquals("did not unsub from Observable.never() inner obs", 1, l2.get());
    }
    
    private static class Foo {}
    private static class Bar extends Foo {}
    
    @Test
    public void testGenerics() {
        AsyncOnSubscribe.createStateless(new Action2<Long, Observer<Observable<? extends Foo>>>(){
            @Override
            public void call(Long state, Observer<Observable<? extends Foo>> observer) {
                if (state == null)
                    observer.onNext(Observable.just(new Foo()));
                else
                    observer.onNext(Observable.just(new Bar()));
            }});
    }
    
    @Test
    public void testUnderdeliveryCorrection() {
        OnSubscribe<Integer> os = AsyncOnSubscribe.createStateful(new Func0<Integer>(){
            @Override
            public Integer call() {
                return 1;
            }}, 
        new Func3<Integer, Long, Observer<Observable<? extends Integer>>, Integer>(){
            @Override
            public Integer call(Integer state, Long requested, Observer<Observable<? extends Integer>> observer) {
                switch (state) {
                case 1:
                    observer.onNext(Observable.just(1));
                    break;
                default:
                    observer.onNext(Observable.range(1, requested.intValue()));
                    break;
                }
                return state + 1;
            }});
        Observable.create(os).subscribe(subscriber);
        
        subscriber.assertNoErrors();
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
        
        subscriber.requestMore(2);
        
        subscriber.assertNoErrors();
        subscriber.assertValueCount(2);
        
        subscriber.requestMore(5);

        subscriber.assertNoErrors();
        subscriber.assertValueCount(7);
        
        subscriber.assertNotCompleted();
    }
}