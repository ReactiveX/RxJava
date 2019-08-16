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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableSkipUntilTest extends RxJavaTest {
    Subscriber<Object> subscriber;

    @Before
    public void before() {
        subscriber = TestHelper.mockSubscriber();
    }

    @Test
    public void normal1() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> m = source.skipUntil(other);
        m.subscribe(subscriber);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onNext(3);
        verify(subscriber, times(1)).onNext(4);
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void otherNeverFires() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<Integer> m = source.skipUntil(Flowable.never());

        m.subscribe(subscriber);

        source.onNext(0);
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, never()).onNext(any());
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void otherEmpty() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<Integer> m = source.skipUntil(Flowable.empty());

        m.subscribe(subscriber);

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onComplete();
    }

    @Test
    public void otherFiresAndCompletes() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> m = source.skipUntil(other);
        m.subscribe(subscriber);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);
        other.onComplete();

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onNext(3);
        verify(subscriber, times(1)).onNext(4);
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void sourceThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> m = source.skipUntil(other);
        m.subscribe(subscriber);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);
        other.onComplete();

        source.onNext(2);
        source.onError(new RuntimeException("Forced failure"));

        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onError(any(Throwable.class));
        verify(subscriber, never()).onComplete();
    }

    @Test
    public void otherThrowsImmediately() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> m = source.skipUntil(other);
        m.subscribe(subscriber);

        source.onNext(0);
        source.onNext(1);

        other.onError(new RuntimeException("Forced failure"));

        verify(subscriber, never()).onNext(any());
        verify(subscriber, times(1)).onError(any(Throwable.class));
        verify(subscriber, never()).onComplete();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().skipUntil(PublishProcessor.create()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.skipUntil(Flowable.never());
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return Flowable.never().skipUntil(f);
            }
        });
    }
}
