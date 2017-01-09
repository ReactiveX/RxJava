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
package io.reactivex.internal.operators.observable;

import java.lang.ref.WeakReference;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;


public class ObservableDetachTest {

    Object o;

    @Test
    public void just() throws Exception {
        o = new Object();

        WeakReference<Object> wr = new WeakReference<Object>(o);

        TestObserver<Object> ts = new TestObserver<Object>();

        Observable.just(o).count().toObservable().onTerminateDetach().subscribe(ts);

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
        TestObserver<Object> ts = new TestObserver<Object>();

        Observable.error(new TestException()).onTerminateDetach().subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void empty() {
        TestObserver<Object> ts = new TestObserver<Object>();

        Observable.empty().onTerminateDetach().subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void range() {
        TestObserver<Object> ts = new TestObserver<Object>();

        Observable.range(1, 1000).onTerminateDetach().subscribe(ts);

        ts.assertValueCount(1000);
        ts.assertNoErrors();
        ts.assertComplete();
    }


    @Test
    @Ignore("Observable doesn't do backpressure")
    public void backpressured() throws Exception {
//        o = new Object();
//
//        WeakReference<Object> wr = new WeakReference<Object>(o);
//
//        TestObserver<Object> ts = new TestObserver<Object>(0L);
//
//        Observable.just(o).count().onTerminateDetach().subscribe(ts);
//
//        ts.assertNoValues();
//
//        ts.request(1);
//
//        ts.assertValue(1L);
//        ts.assertComplete();
//        ts.assertNoErrors();
//
//        o = null;
//
//        System.gc();
//        Thread.sleep(200);
//
//        Assert.assertNull("Object retained!", wr.get());
    }

    @Test
    public void justUnsubscribed() throws Exception {
        o = new Object();

        WeakReference<Object> wr = new WeakReference<Object>(o);

        TestObserver<Long> ts = Observable.just(o).count().toObservable().onTerminateDetach().test();

        o = null;
        ts.cancel();

        System.gc();
        Thread.sleep(200);

        Assert.assertNull("Object retained!", wr.get());

    }

    @Test
    @Ignore("Observable doesn't do backpressure")
    public void deferredUpstreamProducer() {
//        final AtomicReference<Subscriber<? super Object>> subscriber = new AtomicReference<Subscriber<? super Object>>();
//
//        TestObserver<Object> ts = new TestObserver<Object>(0);
//
//        Observable.unsafeCreate(new ObservableSource<Object>() {
//            @Override
//            public void subscribe(Subscriber<? super Object> t) {
//                subscriber.set(t);
//            }
//        }).onTerminateDetach().subscribe(ts);
//
//        ts.request(2);
//
//        new ObservableRange(1, 3).subscribe(subscriber.get());
//
//        ts.assertValues(1, 2);
//
//        ts.request(1);
//
//        ts.assertValues(1, 2, 3);
//        ts.assertComplete();
//        ts.assertNoErrors();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.never().onTerminateDetach());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.onTerminateDetach();
            }
        });
    }
}
