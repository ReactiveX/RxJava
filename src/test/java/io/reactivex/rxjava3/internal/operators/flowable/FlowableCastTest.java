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
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableCastTest extends RxJavaTest {

    @Test
    public void cast() {
        Flowable<?> source = Flowable.just(1, 2);
        Flowable<Integer> flowable = source.cast(Integer.class);

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext(1);
        verify(subscriber, times(1)).onNext(1);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void castWithWrongType() {
        Flowable<?> source = Flowable.just(1, 2);
        Flowable<Boolean> flowable = source.cast(Boolean.class);

        Subscriber<Boolean> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onError(any(ClassCastException.class));
    }

    @Test
    public void castCrashUnsubscribes() {

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<String> ts = TestSubscriber.create();

        pp.cast(String.class).subscribe(ts);

        Assert.assertTrue("Not subscribed?", pp.hasSubscribers());

        pp.onNext(1);

        Assert.assertFalse("Subscribed?", pp.hasSubscribers());

        ts.assertError(ClassCastException.class);
    }
}
