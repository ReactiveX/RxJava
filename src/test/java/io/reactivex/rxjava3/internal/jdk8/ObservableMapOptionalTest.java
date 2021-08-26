/*
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

package io.reactivex.rxjava3.internal.jdk8;

import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableMapOptionalTest extends RxJavaTest {

    static final Function<? super Integer, Optional<? extends Integer>> MODULO = v -> v % 2 == 0 ? Optional.of(v) : Optional.<Integer>empty();

    @Test
    public void allPresent() {
        Observable.range(1, 5)
        .mapOptional(Optional::of)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void allEmpty() {
        Observable.range(1, 5)
        .mapOptional(v -> Optional.<Integer>empty())
        .test()
        .assertResult();
    }

    @Test
    public void mixed() {
        Observable.range(1, 10)
        .mapOptional(MODULO)
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void mapperChash() {
        BehaviorSubject<Integer> source = BehaviorSubject.createDefault(1);

        source
        .mapOptional(v -> { throw new TestException(); })
        .test()
        .assertFailure(TestException.class);

        assertFalse(source.hasObservers());
    }

    @Test
    public void mapperNull() {
        BehaviorSubject<Integer> source = BehaviorSubject.createDefault(1);

        source
        .mapOptional(v -> null)
        .test()
        .assertFailure(NullPointerException.class);

        assertFalse(source.hasObservers());
    }

    @Test
    public void crashDropsOnNexts() {
        Observable<Integer> source = new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onNext(1);
                observer.onNext(2);
            }
        };

        source
        .mapOptional(v -> { throw new TestException(); })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void syncFusedAll() {
        Observable.range(1, 5)
        .mapOptional(Optional::of)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void asyncFusedAll() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5);

        us
        .mapOptional(Optional::of)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void boundaryFusedAll() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5);

        us
        .mapOptional(Optional::of)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void syncFusedNone() {
        Observable.range(1, 5)
        .mapOptional(v -> Optional.empty())
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult();
    }

    @Test
    public void asyncFusedNone() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5);

        us
        .mapOptional(v -> Optional.empty())
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void boundaryFusedNone() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5);

        us
        .mapOptional(v -> Optional.empty())
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult();
    }

    @Test
    public void syncFusedMixed() {
        Observable.range(1, 10)
        .mapOptional(MODULO)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void asyncFusedMixed() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        us
        .mapOptional(MODULO)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void boundaryFusedMixed() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        us
        .mapOptional(MODULO)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void allPresentConditional() {
        Observable.range(1, 5)
        .mapOptional(Optional::of)
        .filter(v -> true)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void allEmptyConditional() {
        Observable.range(1, 5)
        .mapOptional(v -> Optional.<Integer>empty())
        .filter(v -> true)
        .test()
        .assertResult();
    }

    @Test
    public void mixedConditional() {
        Observable.range(1, 10)
        .mapOptional(MODULO)
        .filter(v -> true)
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void mapperChashConditional() {
        BehaviorSubject<Integer> source = BehaviorSubject.createDefault(1);

        source
        .mapOptional(v -> { throw new TestException(); })
        .filter(v -> true)
        .test()
        .assertFailure(TestException.class);

        assertFalse(source.hasObservers());
    }

    @Test
    public void mapperNullConditional() {
        BehaviorSubject<Integer> source = BehaviorSubject.createDefault(1);

        source
        .mapOptional(v -> null)
        .filter(v -> true)
        .test()
        .assertFailure(NullPointerException.class);

        assertFalse(source.hasObservers());
    }

    @Test
    public void crashDropsOnNextsConditional() {
        Observable<Integer> source = new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onNext(1);
                observer.onNext(2);
            }
        };

        source
        .mapOptional(v -> { throw new TestException(); })
        .filter(v -> true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void syncFusedAllConditional() {
        Observable.range(1, 5)
        .mapOptional(Optional::of)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void asyncFusedAllConditional() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5);

        us
        .mapOptional(Optional::of)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void boundaryFusedAllConditiona() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5);

        us
        .mapOptional(Optional::of)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void syncFusedNoneConditional() {
        Observable.range(1, 5)
        .mapOptional(v -> Optional.empty())
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult();
    }

    @Test
    public void asyncFusedNoneConditional() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5);

        us
        .mapOptional(v -> Optional.empty())
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void boundaryFusedNoneConditional() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5);

        us
        .mapOptional(v -> Optional.empty())
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult();
    }

    @Test
    public void syncFusedMixedConditional() {
        Observable.range(1, 10)
        .mapOptional(MODULO)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void asyncFusedMixedConditional() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        us
        .mapOptional(MODULO)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void boundaryFusedMixedConditional() {
        UnicastSubject<Integer> us = UnicastSubject.create();
        TestHelper.emit(us, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        us
        .mapOptional(MODULO)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(2, 4, 6, 8, 10);
    }
}
