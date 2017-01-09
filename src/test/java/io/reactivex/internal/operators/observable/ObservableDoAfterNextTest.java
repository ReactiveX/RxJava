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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.TestHelper;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.observers.*;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.*;

public class ObservableDoAfterNextTest {

    final List<Integer> values = new ArrayList<Integer>();

    final Consumer<Integer> afterNext = new Consumer<Integer>() {
        @Override
        public void accept(Integer e) throws Exception {
            values.add(-e);
        }
    };

    final TestObserver<Integer> ts = new TestObserver<Integer>() {
        @Override
        public void onNext(Integer t) {
            super.onNext(t);
            ObservableDoAfterNextTest.this.values.add(t);
        }
    };

    @Test
    public void just() {
        Observable.just(1)
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void range() {
        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1, -1, 2, -2, 3, -3, 4, -4, 5, -5), values);
    }

    @Test
    public void error() {
        Observable.<Integer>error(new TestException())
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void empty() {
        Observable.<Integer>empty()
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult();

        assertTrue(values.isEmpty());
    }

    @Test
    public void syncFused() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueSubscription.SYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueSubscription.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedRejected() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueSubscription.ASYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFused() {
        TestSubscriber<Integer> ts0 = SubscriberFusion.newTest(QueueSubscription.ASYNC);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doAfterNext(afterNext)
        .subscribe(ts0);

        SubscriberFusion.assertFusion(ts0, QueueSubscription.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test(expected = NullPointerException.class)
    public void consumerNull() {
        Observable.just(1).doAfterNext(null);
    }

    @Test
    public void justConditional() {
        Observable.just(1)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void rangeConditional() {
        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1, -1, 2, -2, 3, -3, 4, -4, 5, -5), values);
    }

    @Test
    public void errorConditional() {
        Observable.<Integer>error(new TestException())
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void emptyConditional() {
        Observable.<Integer>empty()
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult();

        assertTrue(values.isEmpty());
    }

    @Test
    public void syncFusedConditional() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueSubscription.SYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueSubscription.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedRejectedConditional() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueSubscription.ASYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedConditional() {
        TestSubscriber<Integer> ts0 = SubscriberFusion.newTest(QueueSubscription.ASYNC);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        SubscriberFusion.assertFusion(ts0, QueueSubscription.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void consumerThrows() {
        Observable.just(1, 2)
        .doAfterNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void consumerThrowsConditional() {
        Observable.just(1, 2)
        .doAfterNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void consumerThrowsConditional2() {
        Observable.just(1, 2).hide()
        .doAfterNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class, 1);
    }
}
