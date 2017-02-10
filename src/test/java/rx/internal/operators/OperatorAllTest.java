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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaHooks;

public class OperatorAllTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testAll() {
        Observable<String> obs = Observable.just("one", "two", "six");

        Observer<Boolean> observer = mock(Observer.class);
        obs.all(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return s.length() == 3;
            }
        }).subscribe(observer);

        verify(observer).onNext(true);
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotAll() {
        Observable<String> obs = Observable.just("one", "two", "three", "six");

        Observer<Boolean> observer = mock(Observer.class);
        obs.all(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return s.length() == 3;
            }
        }).subscribe(observer);

        verify(observer).onNext(false);
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmpty() {
        Observable<String> obs = Observable.empty();

        Observer<Boolean> observer = mock(Observer.class);
        obs.all(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return s.length() == 3;
            }
        }).subscribe(observer);

        verify(observer).onNext(true);
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testError() {
        Throwable error = new Throwable();
        Observable<String> obs = Observable.error(error);

        Observer<Boolean> observer = mock(Observer.class);
        obs.all(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return s.length() == 3;
            }
        }).subscribe(observer);

        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testFollowingFirst() {
        Observable<Integer> o = Observable.from(Arrays.asList(1, 3, 5, 6));
        Observable<Boolean> allOdd = o.all(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer i) {
                return i % 2 == 1;
            }
        });
        assertFalse(allOdd.toBlocking().first());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1)
            .all(new Func1<Object, Boolean>() {
                @Override
                public Boolean call(Object t1) {
                    return false;
                }
            })
            .flatMap(new Func1<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
        });
        assertEquals((Object)2, source.toBlocking().first());
    }

    @Test
    public void testBackpressureIfNoneRequestedNoneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(0);
        Observable.empty().all(new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object t1) {
                return false;
            }
        }).subscribe(ts);
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(1);
        Observable.empty().all(new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object object) {
                return false;
            }
        }).subscribe(ts);
        ts.assertTerminalEvent();
        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValue(true);
    }

    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(0);
        final IllegalArgumentException ex = new IllegalArgumentException();
        Observable.just("Boo!").all(new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object object) {
                throw ex;
            }
        }).subscribe(ts);
        ts.assertTerminalEvent();
        ts.assertNoValues();
        ts.assertNotCompleted();
        List<Throwable> errors = ts.getOnErrorEvents();
        assertEquals(1, errors.size());
        assertEquals(ex, errors.get(0));
        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void testDoesNotEmitMultipleTerminalEvents() {
        TestSubscriber<Boolean> ts = TestSubscriber.create();
        Observable.unsafeCreate(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> sub) {
                sub.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (n > 0) {
                            sub.onNext(1);
                            sub.onCompleted();
                        }
                    }
                });
            }
        })
        .all(new Func1<Integer,Boolean>() {

            @Override
            public Boolean call(Integer t) {
                throw new RuntimeException("boo");
            }})
        .unsafeSubscribe(ts);
        ts.assertError(RuntimeException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void testUpstreamEmitsOnNextAfterFailureWithoutCheckingSubscription() {
        TestSubscriber<Boolean> ts = TestSubscriber.create();
        Observable.unsafeCreate(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> sub) {
                sub.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (n > 1) {
                            sub.onNext(1);
                            sub.onNext(2);
                        }
                    }
                });
            }
        })
        .all(new Func1<Integer,Boolean>() {
            boolean once = true;
            @Override
            public Boolean call(Integer t) {
                if (once) {
                    throw new RuntimeException("boo");
                } else  {
                    once = false;
                    return true;
                }
            }})
        .unsafeSubscribe(ts);
        ts.assertNoValues();
        ts.assertError(RuntimeException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void testDoesNotEmitMultipleErrorEventsAndReportsSecondErrorToHooks() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
            RxJavaHooks.setOnError(new Action1<Throwable>() {

                @Override
                public void call(Throwable t) {
                    list.add(t);
                }
            });
            TestSubscriber<Boolean> ts = TestSubscriber.create();
            final RuntimeException e1 = new RuntimeException();
            final Throwable e2 = new RuntimeException();
            Observable.unsafeCreate(new OnSubscribe<Integer>() {

                @Override
                public void call(final Subscriber<? super Integer> sub) {
                    sub.setProducer(new Producer() {

                        @Override
                        public void request(long n) {
                            if (n > 0) {
                                sub.onNext(1);
                                sub.onError(e2);
                            }
                        }
                    });
                }
            }).all(new Func1<Integer, Boolean>() {

                @Override
                public Boolean call(Integer t) {
                    throw e1;
                }
            }).unsafeSubscribe(ts);
            ts.assertNotCompleted();
            assertEquals(Arrays.asList(e1), ts.getOnErrorEvents());
            assertEquals(Arrays.asList(e2), list);
        } finally {
            RxJavaHooks.reset();
        }
    }
}
