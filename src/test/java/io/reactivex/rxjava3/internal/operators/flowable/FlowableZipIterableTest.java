/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.CrashingIterable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableZipIterableTest extends RxJavaTest {
    BiFunction<String, String, String> concat2Strings;
    PublishProcessor<String> s1;
    PublishProcessor<String> s2;
    Flowable<String> zipped;

    Subscriber<String> subscriber;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = new BiFunction<String, String, String>() {
            @Override
            public String apply(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = PublishProcessor.create();
        s2 = PublishProcessor.create();
        zipped = Flowable.zip(s1, s2, concat2Strings);

        subscriber = TestHelper.mockSubscriber();
        inOrder = inOrder(subscriber);

        zipped.subscribe(subscriber);
    }

    BiFunction<Object, Object, String> zipr2 = new BiFunction<Object, Object, String>() {

        @Override
        public String apply(Object t1, Object t2) {
            return "" + t1 + t2;
        }

    };
    Function3<Object, Object, Object, String> zipr3 = new Function3<Object, Object, Object, String>() {

        @Override
        public String apply(Object t1, Object t2, Object t3) {
            return "" + t1 + t2 + t3;
        }

    };

    @Test
    public void zipIterableSameSize() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(subscriber).onNext("one-1");
        io.verify(subscriber).onNext("two-2");
        io.verify(subscriber).onNext("three-3");
        io.verify(subscriber).onComplete();

        verify(subscriber, never()).onError(any(Throwable.class));

    }

    @Test
    public void zipIterableEmptyFirstSize() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onComplete();

        io.verify(subscriber).onComplete();

        verify(subscriber, never()).onNext(any(String.class));
        verify(subscriber, never()).onError(any(Throwable.class));

    }

    @Test
    public void zipIterableEmptySecond() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = Arrays.asList();

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(subscriber).onComplete();

        verify(subscriber, never()).onNext(any(String.class));
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void zipIterableFirstShorter() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onComplete();

        io.verify(subscriber).onNext("one-1");
        io.verify(subscriber).onNext("two-2");
        io.verify(subscriber).onComplete();

        verify(subscriber, never()).onError(any(Throwable.class));

    }

    @Test
    public void zipIterableSecondShorter() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = Arrays.asList("1", "2");

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(subscriber).onNext("one-1");
        io.verify(subscriber).onNext("two-2");
        io.verify(subscriber).onComplete();

        verify(subscriber, never()).onError(any(Throwable.class));

    }

    @Test
    public void zipIterableFirstThrows() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(subscriber).onNext("one-1");
        io.verify(subscriber).onNext("two-2");
        io.verify(subscriber).onError(any(TestException.class));

        verify(subscriber, never()).onComplete();

    }

    @Test
    public void zipIterableIteratorThrows() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                throw new TestException();
            }
        };

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(subscriber).onError(any(TestException.class));

        verify(subscriber, never()).onComplete();
        verify(subscriber, never()).onNext(any(String.class));

    }

    @Test
    public void zipIterableHasNextThrows() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    int count;

                    @Override
                    public boolean hasNext() {
                        if (count == 0) {
                            return true;
                        }
                        throw new TestException();
                    }

                    @Override
                    public String next() {
                        count++;
                        return "1";
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                };
            }

        };

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onNext("one-");
        r1.onError(new TestException());

        io.verify(subscriber).onNext("one-1");
        io.verify(subscriber).onError(any(TestException.class));

        verify(subscriber, never()).onComplete();

    }

    @Test
    public void zipIterableNextThrows() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder io = inOrder(subscriber);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public String next() {
                        throw new TestException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                };
            }

        };

        r1.zipWith(r2, zipr2).subscribe(subscriber);

        r1.onError(new TestException());

        io.verify(subscriber).onError(any(TestException.class));

        verify(subscriber, never()).onNext(any(String.class));
        verify(subscriber, never()).onComplete();

    }

    Consumer<String> printer = new Consumer<String>() {
        @Override
        public void accept(String pv) {
            System.out.println(pv);
        }
    };

    static final class SquareStr implements Function<Integer, String> {
        final AtomicInteger counter = new AtomicInteger();
        @Override
        public String apply(Integer t1) {
            counter.incrementAndGet();
            System.out.println("Omg I'm calculating so hard: " + t1 + "*" + t1 + "=" + (t1 * t1));
            return " " + (t1 * t1);
        }
    }

    @Test
    public void take2() {
        Flowable<Integer> f = Flowable.just(1, 2, 3, 4, 5);
        Iterable<String> it = Arrays.asList("a", "b", "c", "d", "e");

        SquareStr squareStr = new SquareStr();

        f.map(squareStr).zipWith(it, concat2Strings).take(2).subscribe(printer);

        assertEquals(2, squareStr.counter.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).zipWith(Arrays.asList(1), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Integer>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Integer> f) throws Exception {
                return f.zipWith(Arrays.asList(1), new BiFunction<Integer, Integer, Object>() {
                    @Override
                    public Object apply(Integer a, Integer b) throws Exception {
                        return a + b;
                    }
                });
            }
        });
    }

    @Test
    public void iteratorThrows() {
        Flowable.just(1).zipWith(new CrashingIterable(100, 1, 100), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        })
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    subscriber.onNext(1);
                    subscriber.onComplete();
                    subscriber.onNext(2);
                    subscriber.onError(new TestException());
                    subscriber.onComplete();
                }
            }
            .zipWith(Arrays.asList(1), new BiFunction<Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b) throws Exception {
                    return a + b;
                }
            })
            .test()
            .assertResult(2);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
