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

package io.reactivex.internal.operators.flowable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableDematerializeTest {

    @Test
    public void testDematerialize1() {
        Flowable<Notification<Integer>> notifications = Flowable.just(1, 2).materialize();
        Flowable<Integer> dematerialize = notifications.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDematerialize2() {
        Throwable exception = new Throwable("test");
        Flowable<Integer> observable = Flowable.error(exception);
        Flowable<Integer> dematerialize = observable.materialize().dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testDematerialize3() {
        Exception exception = new Exception("test");
        Flowable<Integer> observable = Flowable.error(exception);
        Flowable<Integer> dematerialize = observable.materialize().dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testErrorPassThru() {
        Exception exception = new Exception("test");
        Flowable<Integer> observable = Flowable.error(exception);
        Flowable<Integer> dematerialize = observable.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testCompletePassThru() {
        Flowable<Integer> observable = Flowable.empty();
        Flowable<Integer> dematerialize = observable.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(observer);
        dematerialize.subscribe(ts);

        System.out.println(ts.errors());

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testHonorsContractWhenCompleted() {
        Flowable<Integer> source = Flowable.just(1);

        Flowable<Integer> result = source.materialize().dematerialize();

        Subscriber<Integer> o = TestHelper.mockSubscriber();

        result.subscribe(o);

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testHonorsContractWhenThrows() {
        Flowable<Integer> source = Flowable.error(new TestException());

        Flowable<Integer> result = source.materialize().dematerialize();

        Subscriber<Integer> o = TestHelper.mockSubscriber();

        result.subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(Notification.createOnComplete()).dematerialize());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> o) throws Exception {
                return o.dematerialize();
            }
        });
    }

    @Test
    public void eventsAfterDematerializedTerminal() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Object>() {
                @Override
                protected void subscribeActual(Subscriber<? super Object> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onNext(Notification.createOnComplete());
                    observer.onNext(Notification.createOnNext(1));
                    observer.onNext(Notification.createOnError(new TestException("First")));
                    observer.onError(new TestException("Second"));
                }
            }
            .dematerialize()
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "First");
            TestHelper.assertUndeliverable(errors, 1, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
