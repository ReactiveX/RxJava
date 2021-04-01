/*
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableDistinctUntilChangedTest extends RxJavaTest {

    Subscriber<String> w;
    Subscriber<String> w2;

    // nulls lead to exceptions
    final Function<String, String> TO_UPPER_WITH_EXCEPTION = new Function<String, String>() {
        @Override
        public String apply(String s) {
            if (s.equals("x")) {
                return "xx";
            }
            return s.toUpperCase();
        }
    };

    @Before
    public void before() {
        w = TestHelper.mockSubscriber();
        w2 = TestHelper.mockSubscriber();
    }

    @Test
    public void distinctUntilChangedOfNone() {
        Flowable<String> src = Flowable.empty();
        src.distinctUntilChanged().subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void distinctUntilChangedOfNoneWithKeySelector() {
        Flowable<String> src = Flowable.empty();
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void distinctUntilChangedOfNormalSource() {
        Flowable<String> src = Flowable.just("a", "b", "c", "c", "c", "b", "b", "a", "e");
        src.distinctUntilChanged().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void distinctUntilChangedOfNormalSourceWithKeySelector() {
        Flowable<String> src = Flowable.just("a", "b", "c", "C", "c", "B", "b", "a", "e");
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("B");
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void directComparer() {
        Flowable.fromArray(1, 2, 2, 3, 2, 4, 1, 1, 2)
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .test()
        .assertResult(1, 2, 3, 2, 4, 1, 2);
    }

    @Test
    public void directComparerConditional() {
        Flowable.fromArray(1, 2, 2, 3, 2, 4, 1, 1, 2)
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        })
        .test()
        .assertResult(1, 2, 3, 2, 4, 1, 2);
    }

    @Test
    public void directComparerFused() {
        Flowable.fromArray(1, 2, 2, 3, 2, 4, 1, 1, 2)
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .to(TestHelper.<Integer>testSubscriber(Long.MAX_VALUE, QueueFuseable.ANY, false))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 2, 4, 1, 2);
    }

    @Test
    public void directComparerConditionalFused() {
        Flowable.fromArray(1, 2, 2, 3, 2, 4, 1, 1, 2)
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        })
        .to(TestHelper.<Integer>testSubscriber(Long.MAX_VALUE, QueueFuseable.ANY, false))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 2, 4, 1, 2);
    }

    private static final Function<String, String> THROWS_NON_FATAL = new Function<String, String>() {
        @Override
        public String apply(String s) {
            throw new RuntimeException();
        }
    };

    @Test
    public void distinctUntilChangedWhenNonFatalExceptionThrownByKeySelectorIsNotReportedByUpstream() {
        Flowable<String> src = Flowable.just("a", "b", "null", "c");
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);
        src
          .doOnError(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) {
                    errorOccurred.set(true);
                }
            })
          .distinctUntilChanged(THROWS_NON_FATAL)
          .subscribe(w);
        Assert.assertFalse(errorOccurred.get());
    }

    @Test
    public void customComparator() {
        Flowable<String> source = Flowable.just("a", "b", "B", "A", "a", "C");

        TestSubscriber<String> ts = TestSubscriber.create();

        source.distinctUntilChanged(new BiPredicate<String, String>() {
            @Override
            public boolean test(String a, String b) {
                return a.compareToIgnoreCase(b) == 0;
            }
        })
        .subscribe(ts);

        ts.assertValues("a", "b", "A", "C");
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void customComparatorThrows() {
        Flowable<String> source = Flowable.just("a", "b", "B", "A", "a", "C");

        TestSubscriber<String> ts = TestSubscriber.create();

        source.distinctUntilChanged(new BiPredicate<String, String>() {
            @Override
            public boolean test(String a, String b) {
                throw new TestException();
            }
        })
        .subscribe(ts);

        ts.assertValue("a");
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void fused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.just(1, 2, 2, 3, 3, 4, 5)
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) throws Exception {
                return a.equals(b);
            }
        })
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void fusedAsync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) throws Exception {
                return a.equals(b);
            }
        })
        .subscribe(ts);

        TestHelper.emit(up, 1, 2, 2, 3, 3, 4, 5);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void ignoreCancel() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            new Flowable<Integer>() {
                @Override
                public void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onNext(3);
                    s.onError(new IOException());
                    s.onComplete();
                }
            }
            .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
                @Override
                public boolean test(Integer a, Integer b) throws Exception {
                    throw new TestException();
                }
            })
            .test()
            .assertFailure(TestException.class, 1);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    class Mutable {
        int value;
    }

    @Test
    public void mutableWithSelector() {
        Mutable m = new Mutable();

        PublishProcessor<Mutable> pp = PublishProcessor.create();

        TestSubscriber<Mutable> ts = pp.distinctUntilChanged(new Function<Mutable, Object>() {
            @Override
            public Object apply(Mutable m) throws Exception {
                return m.value;
            }
        })
        .test();

        pp.onNext(m);
        m.value = 1;
        pp.onNext(m);
        pp.onComplete();

        ts.assertResult(m, m);
    }

    @Test
    public void conditionalNormal() {
        Flowable.just(1, 2, 1, 3, 3, 4, 3, 5, 5)
        .distinctUntilChanged()
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .test()
        .assertResult(2, 4);
    }

    @Test
    public void conditionalNormal2() {
        Flowable.just(1, 2, 1, 3, 3, 4, 3, 5, 5).hide()
        .distinctUntilChanged()
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .test()
        .assertResult(2, 4);
    }

    @Test
    public void conditionalNormal3() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestSubscriber<Integer> ts = up.hide()
        .distinctUntilChanged()
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .test();

        TestHelper.emit(up, 1, 2, 1, 3, 3, 4, 3, 5, 5);

        ts
        .assertResult(2, 4);
    }

    @Test
    public void conditionalSelectorCrash() {
        Flowable.just(1, 2, 1, 3, 3, 4, 3, 5, 5)
        .distinctUntilChanged(new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer a, Integer b) throws Exception {
                throw new TestException();
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void conditionalFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.just(1, 2, 1, 3, 3, 4, 3, 5, 5)
        .distinctUntilChanged()
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 4);
    }

    @Test
    public void conditionalAsyncFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .distinctUntilChanged()
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        TestHelper.emit(up, 1, 2, 1, 3, 3, 4, 3, 5, 5);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 4);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.distinctUntilChanged().filter(Functions.alwaysTrue());
            }
        }, false, 1, 1, 1);
    }
}
