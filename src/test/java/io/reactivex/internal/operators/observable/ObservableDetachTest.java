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

        TestObserver<Object> to = new TestObserver<Object>();

        Observable.just(o).count().toObservable().onTerminateDetach().subscribe(to);

        to.assertValue(1L);
        to.assertComplete();
        to.assertNoErrors();

        o = null;

        System.gc();
        Thread.sleep(200);

        Assert.assertNull("Object retained!", wr.get());

    }

    @Test
    public void error() {
        TestObserver<Object> to = new TestObserver<Object>();

        Observable.error(new TestException()).onTerminateDetach().subscribe(to);

        to.assertNoValues();
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void empty() {
        TestObserver<Object> to = new TestObserver<Object>();

        Observable.empty().onTerminateDetach().subscribe(to);

        to.assertNoValues();
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void range() {
        TestObserver<Object> to = new TestObserver<Object>();

        Observable.range(1, 1000).onTerminateDetach().subscribe(to);

        to.assertValueCount(1000);
        to.assertNoErrors();
        to.assertComplete();
    }


    @Test
    @Ignore("Observable doesn't do backpressure")
    public void backpressured() throws Exception {
//        o = new Object();
//
//        WeakReference<Object> wr = new WeakReference<Object>(o);
//
//        TestObserver<Object> to = new TestObserver<Object>(0L);
//
//        Observable.just(o).count().onTerminateDetach().subscribe(ts);
//
//        to.assertNoValues();
//
//        to.request(1);
//
//        to.assertValue(1L);
//        to.assertComplete();
//        to.assertNoErrors();
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

        TestObserver<Long> to = Observable.just(o).count().toObservable().onTerminateDetach().test();

        o = null;
        to.cancel();

        System.gc();
        Thread.sleep(200);

        Assert.assertNull("Object retained!", wr.get());

    }

    @Test
    @Ignore("Observable doesn't do backpressure")
    public void deferredUpstreamProducer() {
//        final AtomicReference<Subscriber<? super Object>> subscriber = new AtomicReference<Subscriber<? super Object>>();
//
//        TestObserver<Object> to = new TestObserver<Object>(0);
//
//        Observable.unsafeCreate(new ObservableSource<Object>() {
//            @Override
//            public void subscribe(Subscriber<? super Object> t) {
//                subscriber.set(t);
//            }
//        }).onTerminateDetach().subscribe(to);
//
//        to.request(2);
//
//        new ObservableRange(1, 3).subscribe(subscriber.get());
//
//        to.assertValues(1, 2);
//
//        to.request(1);
//
//        to.assertValues(1, 2, 3);
//        to.assertComplete();
//        to.assertNoErrors();
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
