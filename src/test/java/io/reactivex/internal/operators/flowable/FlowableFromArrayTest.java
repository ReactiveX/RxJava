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

import org.junit.*;

import io.reactivex.*;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.ScalarCallable;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableFromArrayTest {

    Flowable<Integer> create(int n) {
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        return Flowable.fromArray(array);
    }
    @Test
    public void simple() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        create(1000).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValueCount(1000);
        ts.assertComplete();
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        create(1000).subscribe(ts);

        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotComplete();

        ts.request(10);

        ts.assertNoErrors();
        ts.assertValueCount(10);
        ts.assertNotComplete();

        ts.request(1000);

        ts.assertNoErrors();
        ts.assertValueCount(1000);
        ts.assertComplete();
    }

    @Test
    public void conditionalBackpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        create(1000)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotComplete();

        ts.request(10);

        ts.assertNoErrors();
        ts.assertValueCount(10);
        ts.assertNotComplete();

        ts.request(1000);

        ts.assertNoErrors();
        ts.assertValueCount(1000);
        ts.assertComplete();
    }

    @Test
    public void empty() {
        Assert.assertSame(Flowable.empty(), Flowable.fromArray(new Object[0]));
    }

    @Test
    public void just() {
        Flowable<Integer> source = Flowable.fromArray(new Integer[] { 1 });
        Assert.assertTrue(source.getClass().toString(), source instanceof ScalarCallable);
    }

    @Test
    public void just10Arguments() {
        Flowable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.just(1, 2, 3));
    }

    @Test
    public void conditionalOneIsNull() {
        Flowable.fromArray(new Integer[] { null, 1 })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void conditionalOneIsNullSlowPath() {
        Flowable.fromArray(new Integer[] { null, 1 })
        .filter(Functions.alwaysTrue())
        .test(2L)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void conditionalOneByOne() {
        Flowable.fromArray(new Integer[] { 1, 2, 3, 4, 5 })
        .filter(Functions.alwaysTrue())
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void conditionalFiltered() {
        Flowable.fromArray(new Integer[] { 1, 2, 3, 4, 5 })
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
    public void conditionalSlowPathCancel() {
        Flowable.fromArray(new Integer[] { 1, 2, 3, 4, 5 })
        .filter(Functions.alwaysTrue())
        .subscribeWith(new TestSubscriber<Integer>(5L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cancel();
                    onComplete();
                }
            }
        })
        .assertResult(1);
    }

    @Test
    public void conditionalSlowPathSkipCancel() {
        Flowable.fromArray(new Integer[] { 1, 2, 3, 4, 5 })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v < 2;
            }
        })
        .subscribeWith(new TestSubscriber<Integer>(5L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cancel();
                    onComplete();
                }
            }
        })
        .assertResult(1);
    }
}
