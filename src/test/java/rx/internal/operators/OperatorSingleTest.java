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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.TestSubscriber;

public class OperatorSingleTest {

    @Test
    public void testSingle() {
        Observable<Integer> observable = Observable.just(1).single();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElements() {
        Observable<Integer> observable = Observable.just(1, 2).single();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmpty() {
        Observable<Integer> observable = Observable.<Integer> empty().single();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitItem() {
        final AtomicLong request = new AtomicLong();
        Observable.just(1).doOnRequest(new Action1<Long>() {
            @Override
            public void call(Long n) {
                request.addAndGet(n);
            }
        }).toBlocking().single();
        assertEquals(2, request.get());
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitErrorFromEmpty() {
        final AtomicLong request = new AtomicLong();
        try {
            Observable.empty().doOnRequest(new Action1<Long>() {
                @Override
                public void call(Long n) {
                    request.addAndGet(n);
                }
            }).toBlocking().single();
        } catch (NoSuchElementException e) {
            assertEquals(2, request.get());
        }
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitErrorFromMoreThanOne() {
        final AtomicLong request = new AtomicLong();
        try {
            Observable.just(1, 2).doOnRequest(new Action1<Long>() {
                @Override
                public void call(Long n) {
                    request.addAndGet(n);
                }
            }).toBlocking().single();
        } catch (IllegalArgumentException e) {
            assertEquals(2, request.get());
        }
    }
    
    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsIf1Then2Requested() {
        final List<Long> requests = new ArrayList<Long>();
        Observable.just(1)
        //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        requests.add(n);
                    }
                })
                //
                .single()
                //
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(2);
                    }
                });
        assertEquals(Arrays.asList(2L), requests);
    }
    
    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsIf3Requested() {
        final List<Long> requests = new ArrayList<Long>();
        Observable.just(1)
        //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        requests.add(n);
                    }
                })
                //
                .single()
                //
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                    }
                });
        assertEquals(Arrays.asList(2L), requests);
    }
    
    @Test
    public void testSingleRequestsExactlyWhatItNeedsIf1Requested() {
        final List<Long> requests = new ArrayList<Long>();
        Observable.just(1)
        //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        requests.add(n);
                    }
                })
                //
                .single()
                //
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                    }
                });
        assertEquals(Arrays.asList(2L), requests);
    }


    @Test
    public void testSingleWithPredicate() {
        Observable<Integer> observable = Observable.just(1, 2).single(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElements() {
        Observable<Integer> observable = Observable.just(1, 2, 3, 4).single(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmpty() {
        Observable<Integer> observable = Observable.just(1).single(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefault() {
        Observable<Integer> observable = Observable.just(1).singleOrDefault(2);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElements() {
        Observable<Integer> observable = Observable.just(1, 2).singleOrDefault(
                3);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmpty() {
        Observable<Integer> observable = Observable.<Integer> empty()
                .singleOrDefault(1);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicate() {
        Observable<Integer> observable = Observable.just(1, 2).singleOrDefault(
                4, new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElements() {
        Observable<Integer> observable = Observable.just(1, 2, 3, 4)
                .singleOrDefault(6, new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmpty() {
        Observable<Integer> observable = Observable.just(1).singleOrDefault(2,
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithBackpressure() {
        Observable<Integer> observable = Observable.just(1, 2).single();

        Subscriber<Integer> subscriber = spy(new Subscriber<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });
        observable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 30000)
    public void testIssue1527() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Observable<Integer> reduced = source.reduce(new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer i1, Integer i2) {
                return i1 + i2;
            }
        });

        Integer r = reduced.toBlocking().first();
        assertEquals(21, r.intValue());
    }
    
    @Test
    public void defaultBackpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        Observable.<Integer>empty().singleOrDefault(1).subscribe(ts);
        
        ts.assertNoValues();
        
        ts.requestMore(1);
        
        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();
    }
}
