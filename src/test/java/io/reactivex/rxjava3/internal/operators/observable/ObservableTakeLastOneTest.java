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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableTakeLastOneTest extends RxJavaTest {

    @Test
    public void lastOfManyReturnsLast() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        Observable.range(1, 10).takeLast(1).subscribe(to);
        to.assertValue(10);
        to.assertNoErrors();
        to.assertTerminated();
    }

    @Test
    public void lastOfEmptyReturnsEmpty() {
        TestObserverEx<Object> to = new TestObserverEx<>();
        Observable.empty().takeLast(1).subscribe(to);
        to.assertNoValues();
        to.assertNoErrors();
        to.assertTerminated();
    }

    @Test
    public void lastOfOneReturnsLast() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        Observable.just(1).takeLast(1).subscribe(to);
        to.assertValue(1);
        to.assertNoErrors();
        to.assertTerminated();
    }

    @Test
    public void unsubscribesFromUpstream() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Action unsubscribeAction = new Action() {
            @Override
            public void run() {
                unsubscribed.set(true);
            }
        };
        Observable.just(1)
        .concatWith(Observable.<Integer>never())
        .doOnDispose(unsubscribeAction)
        .takeLast(1)
        .subscribe()
        .dispose();

        assertTrue(unsubscribed.get());
    }

    @Test
    public void takeLastZeroProcessesAllItemsButIgnoresThem() {
        final AtomicInteger upstreamCount = new AtomicInteger();
        final int num = 10;
        long count = Observable.range(1, num).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t) {
                upstreamCount.incrementAndGet();
            }})
            .takeLast(0).count().blockingGet();
        assertEquals(num, upstreamCount.get());
        assertEquals(0L, count);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).takeLast(1));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> f) throws Exception {
                return f.takeLast(1);
            }
        });
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .takeLast(1)
        .test()
        .assertFailure(TestException.class);
    }
}
