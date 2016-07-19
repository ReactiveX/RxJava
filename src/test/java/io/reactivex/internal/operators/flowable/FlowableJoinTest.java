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
package io.reactivex.internal.operators.flowable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.mockito.MockitoAnnotations;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.processors.PublishProcessor;

public class FlowableJoinTest {
    Subscriber<Object> observer = TestHelper.mockSubscriber();

    BiFunction<Integer, Integer, Integer> add = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    <T> Function<Integer, Flowable<T>> just(final Flowable<T> observable) {
        return new Function<Integer, Flowable<T>>() {
            @Override
            public Flowable<T> apply(Integer t1) {
                return observable;
            }
        };
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void normal1() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> m = source1.join(source2,
                just(Flowable.never()),
                just(Flowable.never()), add);

        m.subscribe(observer);

        source1.onNext(1);
        source1.onNext(2);
        source1.onNext(4);

        source2.onNext(16);
        source2.onNext(32);
        source2.onNext(64);

        source1.onComplete();
        source2.onComplete();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(20);
        verify(observer, times(1)).onNext(33);
        verify(observer, times(1)).onNext(34);
        verify(observer, times(1)).onNext(36);
        verify(observer, times(1)).onNext(65);
        verify(observer, times(1)).onNext(66);
        verify(observer, times(1)).onNext(68);

        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void normal1WithDuration() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        PublishProcessor<Integer> duration1 = PublishProcessor.create();

        Flowable<Integer> m = source1.join(source2,
                just(duration1),
                just(Flowable.never()), add);
        m.subscribe(observer);

        source1.onNext(1);
        source1.onNext(2);
        source2.onNext(16);

        duration1.onNext(1);

        source1.onNext(4);
        source1.onNext(8);

        source1.onComplete();
        source2.onComplete();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(20);
        verify(observer, times(1)).onNext(24);

        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

    }

    @Test
    public void normal2() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> m = source1.join(source2,
                just(Flowable.never()),
                just(Flowable.never()), add);

        m.subscribe(observer);

        source1.onNext(1);
        source1.onNext(2);
        source1.onComplete();

        source2.onNext(16);
        source2.onNext(32);
        source2.onNext(64);

        source2.onComplete();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(33);
        verify(observer, times(1)).onNext(34);
        verify(observer, times(1)).onNext(65);
        verify(observer, times(1)).onNext(66);

        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void leftThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> m = source1.join(source2,
                just(Flowable.never()),
                just(Flowable.never()), add);

        m.subscribe(observer);

        source2.onNext(1);
        source1.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> m = source1.join(source2,
                just(Flowable.never()),
                just(Flowable.never()), add);

        m.subscribe(observer);

        source1.onNext(1);
        source2.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void leftDurationThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> duration1 = Flowable.<Integer> error(new RuntimeException("Forced failure"));

        Flowable<Integer> m = source1.join(source2,
                just(duration1),
                just(Flowable.never()), add);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> duration1 = Flowable.<Integer> error(new RuntimeException("Forced failure"));

        Flowable<Integer> m = source1.join(source2,
                just(Flowable.never()),
                just(duration1), add);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void leftDurationSelectorThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> fail = new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Flowable<Integer> m = source1.join(source2,
                fail,
                just(Flowable.never()), add);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationSelectorThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> fail = new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Flowable<Integer> m = source1.join(source2,
                just(Flowable.never()),
                fail, add);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void resultSelectorThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        BiFunction<Integer, Integer, Integer> fail = new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                throw new RuntimeException("Forced failure");
            }
        };

        Flowable<Integer> m = source1.join(source2,
                just(Flowable.never()),
                just(Flowable.never()), fail);
        m.subscribe(observer);

        source1.onNext(1);
        source2.onNext(2);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }
}
