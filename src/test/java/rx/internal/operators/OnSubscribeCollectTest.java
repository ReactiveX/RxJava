/**
 * Copyright 2016 Netflix, Inc.
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
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.Observable.OnSubscribe;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaHooks;

public class OnSubscribeCollectTest {

    @Test
    public void testCollectToList() {
        Observable<List<Integer>> o = Observable.just(1, 2, 3).collect(new Func0<List<Integer>>() {

            @Override
            public List<Integer> call() {
                return new ArrayList<Integer>();
            }

        }, new Action2<List<Integer>, Integer>() {

            @Override
            public void call(List<Integer> list, Integer v) {
                list.add(v);
            }
        });

        List<Integer> list =  o.toBlocking().last();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(2, list.get(1).intValue());
        assertEquals(3, list.get(2).intValue());

        // test multiple subscribe
        List<Integer> list2 =  o.toBlocking().last();

        assertEquals(3, list2.size());
        assertEquals(1, list2.get(0).intValue());
        assertEquals(2, list2.get(1).intValue());
        assertEquals(3, list2.get(2).intValue());
    }

    @Test
    public void testCollectToString() {
        String value = Observable.just(1, 2, 3).collect(new Func0<StringBuilder>() {

            @Override
            public StringBuilder call() {
                return new StringBuilder();
            }

        }, new Action2<StringBuilder, Integer>() {

            @Override
            public void call(StringBuilder sb, Integer v) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(v);
            }
        }).toBlocking().last().toString();

        assertEquals("1-2-3", value);
    }

    @Test
    public void testFactoryFailureResultsInErrorEmission() {
        TestSubscriber<Object> ts = TestSubscriber.create();
        final RuntimeException e = new RuntimeException();
        Observable.just(1).collect(new Func0<List<Integer>>() {

            @Override
            public List<Integer> call() {
                throw e;
            }
        }, new Action2<List<Integer>, Integer>() {

            @Override
            public void call(List<Integer> list, Integer t) {
                list.add(t);
            }
        }).subscribe(ts);
        ts.assertNoValues();
        ts.assertError(e);
        ts.assertNotCompleted();
    }

    @Test
    public void testCollectorFailureDoesNotResultInTwoErrorEmissions() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
            RxJavaHooks.setOnError(new Action1<Throwable>() {

                @Override
                public void call(Throwable t) {
                    list.add(t);
                }
            });
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();
            TestSubscriber<List<Integer>> ts = TestSubscriber.create();
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
            }).collect(new Func0<List<Integer>>() {

                @Override
                public List<Integer> call() {
                    return new ArrayList<Integer>();
                }
            }, //
                    new Action2<List<Integer>, Integer>() {

                        @Override
                        public void call(List<Integer> t1, Integer t2) {
                            throw e1;
                        }
                    }).unsafeSubscribe(ts);
            assertEquals(Arrays.asList(e1), ts.getOnErrorEvents());
            ts.assertNotCompleted();
            assertEquals(Arrays.asList(e2), list);
        } finally {
            RxJavaHooks.reset();
        }
    }

    @Test
    public void testCollectorFailureDoesNotResultInErrorAndCompletedEmissions() {
        final RuntimeException e1 = new RuntimeException();
        TestSubscriber<List<Integer>> ts = TestSubscriber.create();
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
        }).collect(new Func0<List<Integer>>() {

            @Override
            public List<Integer> call() {
                return new ArrayList<Integer>();
            }
        }, //
                new Action2<List<Integer>, Integer>() {

                    @Override
                    public void call(List<Integer> t1, Integer t2) {
                        throw e1;
                    }
                }).unsafeSubscribe(ts);
        assertEquals(Arrays.asList(e1), ts.getOnErrorEvents());
        ts.assertNotCompleted();
    }

    @Test
    public void testCollectorFailureDoesNotResultInErrorAndOnNextEmissions() {
        final RuntimeException e1 = new RuntimeException();
        TestSubscriber<List<Integer>> ts = TestSubscriber.create();
        final AtomicBoolean added = new AtomicBoolean();
        Observable.unsafeCreate(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> sub) {
                sub.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (n > 0) {
                            sub.onNext(1);
                            sub.onNext(2);
                        }
                    }
                });
            }
        }).collect(new Func0<List<Integer>>() {

            @Override
            public List<Integer> call() {
                return new ArrayList<Integer>();
            }
        }, //
                new Action2<List<Integer>, Integer>() {
                    boolean once = true;
                    @Override
                    public void call(List<Integer> list, Integer t) {
                        if (once) {
                            once = false;
                            throw e1;
                        } else {
                            added.set(true);
                        }
                    }
                }).unsafeSubscribe(ts);
        assertEquals(Arrays.asList(e1), ts.getOnErrorEvents());
        ts.assertNoValues();
        ts.assertNotCompleted();
        assertFalse(added.get());
    }

}
