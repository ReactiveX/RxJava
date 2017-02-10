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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaHooks;

public class OnSubscribeReduceTest {
    @Mock
    Observer<Object> observer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    Func2<Integer, Integer, Integer> sum = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    @Test
    public void testAggregateAsIntSum() {

        Observable<Integer> result = Observable.just(1, 2, 3, 4, 5).reduce(0, sum).map(UtilityFunctions.<Integer> identity());

        result.subscribe(observer);

        verify(observer).onNext(1 + 2 + 3 + 4 + 5);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAggregateAsIntSumSourceThrows() {
        Observable<Integer> result = Observable.concat(Observable.just(1, 2, 3, 4, 5),
                Observable.<Integer> error(new TestException()))
                .reduce(0, sum).map(UtilityFunctions.<Integer> identity());

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumAccumulatorThrows() {
        Func2<Integer, Integer, Integer> sumErr = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        Observable<Integer> result = Observable.just(1, 2, 3, 4, 5)
                .reduce(0, sumErr).map(UtilityFunctions.<Integer> identity());

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumResultSelectorThrows() {

        Func1<Integer, Integer> error = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                throw new TestException();
            }
        };

        Observable<Integer> result = Observable.just(1, 2, 3, 4, 5)
                .reduce(0, sum).map(error);

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testBackpressureWithNoInitialValue() throws InterruptedException {
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Observable<Integer> reduced = source.reduce(sum);

        Integer r = reduced.toBlocking().first();
        assertEquals(21, r.intValue());
    }

    @Test
    public void testBackpressureWithInitialValue() throws InterruptedException {
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Observable<Integer> reduced = source.reduce(0, sum);

        Integer r = reduced.toBlocking().first();
        assertEquals(21, r.intValue());
    }

    @Test
    public void testNoInitialValueDoesNotEmitMultipleTerminalEvents() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable.unsafeCreate(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> sub) {
                sub.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (n > 0) {
                            sub.onNext(1);
                            sub.onNext(2);
                            sub.onCompleted();
                        }
                    }
                });
            }
        })
        .reduce(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer a, Integer b) {
                throw new RuntimeException("boo");
            }})
        .unsafeSubscribe(ts);
        ts.assertError(RuntimeException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void testNoInitialValueUpstreamEmitsMoreOnNextDespiteUnsubscription() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable.unsafeCreate(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> sub) {
                sub.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (n > 2) {
                            sub.onNext(1);
                            sub.onNext(2);
                            sub.onNext(3);
                            sub.onCompleted();
                        }
                    }
                });
            }
        })
        .reduce(new Func2<Integer, Integer, Integer>() {
            boolean once = true;

            @Override
            public Integer call(Integer a, Integer b) {
                if (once) {
                    throw new RuntimeException("boo");
                } else {
                    once = false;
                    return a + b;
                }
            }})
        .unsafeSubscribe(ts);
        ts.assertNoValues();
        ts.assertError(RuntimeException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void testNoInitialValueDoesNotEmitMultipleErrorEventsAndReportsSecondErrorToHooks() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
            RxJavaHooks.setOnError(new Action1<Throwable>() {

                @Override
                public void call(Throwable t) {
                    list.add(t);
                }
            });
            TestSubscriber<Integer> ts = TestSubscriber.create();
            final RuntimeException e1 = new RuntimeException("e1");
            final Throwable e2 = new RuntimeException("e2");
            Observable.unsafeCreate(new OnSubscribe<Integer>() {

                @Override
                public void call(final Subscriber<? super Integer> sub) {
                    sub.setProducer(new Producer() {

                        @Override
                        public void request(long n) {
                            if (n > 1) {
                                sub.onNext(1);
                                sub.onNext(2);
                                sub.onError(e2);
                            }
                        }
                    });
                }
            })
            .reduce(new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer a, Integer b) {
                    throw e1;
            }})
            .unsafeSubscribe(ts);
            ts.assertNotCompleted();
            System.out.println(ts.getOnErrorEvents());
            assertEquals(Arrays.asList(e1), ts.getOnErrorEvents());
            assertEquals(Arrays.asList(e2), list);
        } finally {
            RxJavaHooks.reset();
        }
    }


    @Test
    public void testNoInitialValueEmitsNoSuchElementExceptionIfEmptyStream() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable.<Integer>empty().reduce(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }
        }).subscribe(ts);
        ts.assertError(NoSuchElementException.class);
    }

}
