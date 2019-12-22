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

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.UnicastSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableDoAfterNextTest extends RxJavaTest {

    final List<Integer> values = new ArrayList<>();

    final Consumer<Integer> afterNext = new Consumer<Integer>() {
        @Override
        public void accept(Integer e) throws Exception {
            values.add(-e);
        }
    };

    final TestObserver<Integer> to = new TestObserver<Integer>() {
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
        .subscribeWith(to)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void justHidden() {
        Observable.just(1)
        .hide()
        .doAfterNext(afterNext)
        .subscribeWith(to)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void range() {
        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribeWith(to)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1, -1, 2, -2, 3, -3, 4, -4, 5, -5), values);
    }

    @Test
    public void error() {
        Observable.<Integer>error(new TestException())
        .doAfterNext(afterNext)
        .subscribeWith(to)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void empty() {
        Observable.<Integer>empty()
        .doAfterNext(afterNext)
        .subscribeWith(to)
        .assertResult();

        assertTrue(values.isEmpty());
    }

    @Test
    public void syncFused() {
        TestObserverEx<Integer> to0 = new TestObserverEx<>(QueueFuseable.SYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribe(to0);

        to0.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedRejected() {
        TestObserverEx<Integer> to0 = new TestObserverEx<>(QueueFuseable.ASYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribe(to0);

        to0.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFused() {
        TestObserverEx<Integer> to0 = new TestObserverEx<>(QueueFuseable.ASYNC);

        UnicastSubject<Integer> up = UnicastSubject.create();

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doAfterNext(afterNext)
        .subscribe(to0);

        to0.assertFusionMode(QueueFuseable.ASYNC)
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
        .subscribeWith(to)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void rangeConditional() {
        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(to)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1, -1, 2, -2, 3, -3, 4, -4, 5, -5), values);
    }

    @Test
    public void errorConditional() {
        Observable.<Integer>error(new TestException())
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(to)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void emptyConditional() {
        Observable.<Integer>empty()
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(to)
        .assertResult();

        assertTrue(values.isEmpty());
    }

    @Test
    public void syncFusedConditional() {
        TestObserverEx<Integer> to0 = new TestObserverEx<>(QueueFuseable.SYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(to0);

        to0.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedRejectedConditional() {
        TestObserverEx<Integer> to0 = new TestObserverEx<>(QueueFuseable.ASYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(to0);

        to0.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedConditional() {
        TestObserverEx<Integer> to0 = new TestObserverEx<>(QueueFuseable.ASYNC);

        UnicastSubject<Integer> up = UnicastSubject.create();

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(to0);

        to0.assertFusionMode(QueueFuseable.ASYNC)
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
