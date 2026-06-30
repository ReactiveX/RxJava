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

package io.reactivex.rxjava4.parallel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StandardBufferedConfig;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.internal.subscribers.BasicFuseableSubscriber;
import io.reactivex.rxjava4.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava4.operators.*;
import io.reactivex.rxjava4.processors.*;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.testsupport.*;

public class ParallelFromPublisherTest extends RxJavaTest {

    @Test
    public void sourceOverflow() {
        new Flowable<Integer>() /* NFI */ {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < 10; i++) {
                    s.onNext(i);
                }
            }
        }
        .parallel(1, 1)
        .sequential(1)
        .test(0)
        .assertFailure(QueueOverflowException.class);
    }

    @Test
    public void fusedFilterBecomesEmpty() {
        Flowable.just(1)
        .filter(Functions.alwaysFalse())
        .parallel()
        .sequential()
        .test()
        .assertResult();
    }

    static final class StripBoundary<T> extends Flowable<T> implements FlowableTransformer<T, T> {

        final Flowable<T> source;

        StripBoundary(Flowable<T> source) {
            this.source = source;
        }

        @Override
        public Publisher<T> apply(Flowable<T> upstream) {
            return new StripBoundary<>(upstream);
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> s) {
            source.subscribe(new StripBoundarySubscriber<>(s));
        }

        static final class StripBoundarySubscriber<T> extends BasicFuseableSubscriber<T, T> {

            StripBoundarySubscriber(Subscriber<? super T> downstream) {
                super(downstream);
            }

            @Override
            public void onNext(T t) {
                downstream.onNext(t);
            }

            @Override
            public int requestFusion(int mode) {
                QueueSubscription<T> fs = qs;
                if (fs != null) {
                    int m = fs.requestFusion(mode & ~QueueFuseable.BOUNDARY);
                    this.sourceMode = m;
                    return m;
                }
                return QueueFuseable.NONE;
            }

            @Override
            public T poll() throws Throwable {
                return qs.poll();
            }
        }
    }

    @Test
    public void syncFusedMapCrash() {
        Flowable.just(1)
        .map(_ -> {
            throw new TestException();
        })
        .compose(new StripBoundary<>(null))
        .parallel()
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void asyncFusedMapCrash() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up.onNext(1);

        up
        .map(_ -> {
            throw new TestException();
        })
        .compose(new StripBoundary<>(null))
        .parallel()
        .sequential()
        .test()
        .assertFailure(TestException.class);

        assertFalse(up.hasSubscribers());
    }

    @Test
    public void boundaryConfinement() {
        final Set<String> between = new HashSet<>();
        final ConcurrentHashMap<String, String> processing = new ConcurrentHashMap<>();

        TestSubscriberEx<Object> ts = Flowable.range(1, 10)
        .observeOn(Schedulers.single(), new StandardBufferedConfig(false, 1))
        .doOnNext(_ -> between.add(Thread.currentThread().getName()))
        .parallel(2, 1)
        .runOn(Schedulers.computation(), 1)
        .map((Function<Integer, Object>) v -> {
            processing.putIfAbsent(Thread.currentThread().getName(), "");
            return v;
        })
        .sequential()
        .to(TestHelper.<Object>testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertComplete()
        .assertNoErrors()
        ;

        TestHelper.assertValueSet(ts, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertEquals(1, between.size(), between.toString());
        assertTrue(between.iterator().next().contains("RxSingleScheduler"), between.toString());

        Map<String, String> map = processing; // AnimalSniffer: CHM.keySet() in Java 8 returns KeySetView

        for (String e : map.keySet()) {
            assertTrue(e.contains("RxComputationThreadPool"), map.toString());
        }
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(PublishProcessor.create().parallel());
    }

    @Test
    public void syncFusedEmptyPoll() {
        Flowable.just(1, 2)
        .filter(v -> v == 1)
        .compose(TestHelper.flowableStripBoundary())
        .parallel(1)
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void asyncFusedEmptyPoll() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        up.onNext(1);
        up.onNext(2);
        up.onComplete();

        up
        .filter(v -> v == 1)
        .compose(TestHelper.flowableStripBoundary())
        .parallel(1)
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(f -> f.parallel().sequential());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void requestUnboundedRace() {
        var fs = new FlowableSubscriber<Integer>() /* NFI */ {

            @Override
            public void onNext(@NonNull Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSubscribe(@NonNull Subscription s) {
                for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                    TestHelper.race(
                            () -> s.request(Long.MAX_VALUE),
                            () -> s.request(Long.MAX_VALUE)
                    );
                }
            }
        };

        PublishProcessor.create()
        .parallel(1)
        .subscribe(new FlowableSubscriber[] { fs });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void requestRace() {
        var fs = new FlowableSubscriber<Integer>() /* NFI */ {

            @Override
            public void onNext(@NonNull Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSubscribe(@NonNull Subscription s) {
                for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                    TestHelper.race(
                            () -> s.request(1),
                            () -> s.request(1)
                    );
                }
            }
        };

        PublishProcessor.create()
        .parallel(1)
        .subscribe(new FlowableSubscriber[] { fs });
    }
}
