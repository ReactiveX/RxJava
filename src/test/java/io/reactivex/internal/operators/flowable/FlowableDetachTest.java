/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.subscribers.TestSubscriber;


public class FlowableDetachTest {

    Object o;

    @Test
    public void just() throws Exception {
        o = new Object();

        WeakReference<Object> wr = new WeakReference<Object>(o);

        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        Flowable.just(o).count().toFlowable().onTerminateDetach().subscribe(ts);

        ts.assertValue(1L);
        ts.assertComplete();
        ts.assertNoErrors();

        o = null;

        System.gc();
        Thread.sleep(200);

        Assert.assertNull("Object retained!", wr.get());

    }

    @Test
    public void error() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        Flowable.error(new TestException()).onTerminateDetach().subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void empty() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        Flowable.empty().onTerminateDetach().subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void range() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        Flowable.range(1, 1000).onTerminateDetach().subscribe(ts);

        ts.assertValueCount(1000);
        ts.assertNoErrors();
        ts.assertComplete();
    }


    @Test
    public void backpressured() throws Exception {
        o = new Object();

        WeakReference<Object> wr = new WeakReference<Object>(o);

        TestSubscriber<Object> ts = new TestSubscriber<Object>(0L);

        Flowable.just(o).count().toFlowable().onTerminateDetach().subscribe(ts);

        ts.assertNoValues();

        ts.request(1);

        ts.assertValue(1L);
        ts.assertComplete();
        ts.assertNoErrors();

        o = null;

        System.gc();
        Thread.sleep(200);

        Assert.assertNull("Object retained!", wr.get());
    }

    @Test
    public void justUnsubscribed() throws Exception {
        o = new Object();

        WeakReference<Object> wr = new WeakReference<Object>(o);

        TestSubscriber<Object> ts = new TestSubscriber<Object>(0);

        Flowable.just(o).count().toFlowable().onTerminateDetach().subscribe(ts);

        ts.cancel();
        o = null;

        System.gc();
        Thread.sleep(200);

        Assert.assertNull("Object retained!", wr.get());

    }

    @Test
    public void deferredUpstreamProducer() {
        final AtomicReference<Subscriber<? super Object>> subscriber = new AtomicReference<Subscriber<? super Object>>();

        TestSubscriber<Object> ts = new TestSubscriber<Object>(0);

        Flowable.unsafeCreate(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> t) {
                subscriber.set(t);
            }
        }).onTerminateDetach().subscribe(ts);

        ts.request(2);

        new FlowableRange(1, 3).subscribe(subscriber.get());

        ts.assertValues(1, 2);

        ts.request(1);

        ts.assertValues(1, 2, 3);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.never().onTerminateDetach());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> o) throws Exception {
                return o.onTerminateDetach();
            }
        });
    }
}
