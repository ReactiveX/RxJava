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

package io.reactivex.rxjava3.internal.observers;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class QueueDrainObserverTest extends RxJavaTest {

    static final QueueDrainObserver<Integer, Integer, Integer> createUnordered(TestObserver<Integer> to, final Disposable d) {
        return new QueueDrainObserver<Integer, Integer, Integer>(to, new SpscArrayQueue<>(4)) {
            @Override
            public void onNext(Integer t) {
                fastPathEmit(t, false, d);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void accept(Observer<? super Integer> a, Integer v) {
                super.accept(a, v);
                a.onNext(v);
            }
        };
    }

    static final QueueDrainObserver<Integer, Integer, Integer> createOrdered(TestObserver<Integer> to, final Disposable d) {
        return new QueueDrainObserver<Integer, Integer, Integer>(to, new SpscArrayQueue<>(4)) {
            @Override
            public void onNext(Integer t) {
                fastPathOrderedEmit(t, false, d);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void accept(Observer<? super Integer> a, Integer v) {
                super.accept(a, v);
                a.onNext(v);
            }
        };
    }

    @Test
    public void unorderedSlowPath() {
        TestObserver<Integer> to = new TestObserver<>();
        Disposable d = Disposable.empty();
        QueueDrainObserver<Integer, Integer, Integer> qd = createUnordered(to, d);
        to.onSubscribe(Disposable.empty());

        qd.enter();
        qd.onNext(1);

        to.assertEmpty();
    }

    @Test
    public void orderedSlowPath() {
        TestObserver<Integer> to = new TestObserver<>();
        Disposable d = Disposable.empty();
        QueueDrainObserver<Integer, Integer, Integer> qd = createOrdered(to, d);
        to.onSubscribe(Disposable.empty());

        qd.enter();
        qd.onNext(1);

        to.assertEmpty();
    }

    @Test
    public void orderedSlowPathNonEmptyQueue() {
        TestObserver<Integer> to = new TestObserver<>();
        Disposable d = Disposable.empty();
        QueueDrainObserver<Integer, Integer, Integer> qd = createOrdered(to, d);
        to.onSubscribe(Disposable.empty());

        qd.queue.offer(0);
        qd.onNext(1);

        to.assertValuesOnly(0, 1);
    }

    @Test
    public void unorderedOnNextRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            TestObserver<Integer> to = new TestObserver<>();
            Disposable d = Disposable.empty();
            final QueueDrainObserver<Integer, Integer, Integer> qd = createUnordered(to, d);
            to.onSubscribe(Disposable.empty());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    qd.onNext(1);
                }
            };

            TestHelper.race(r1, r1);

            to.assertValuesOnly(1, 1);
        }
    }

    @Test
    public void orderedOnNextRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            TestObserver<Integer> to = new TestObserver<>();
            Disposable d = Disposable.empty();
            final QueueDrainObserver<Integer, Integer, Integer> qd = createOrdered(to, d);
            to.onSubscribe(Disposable.empty());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    qd.onNext(1);
                }
            };

            TestHelper.race(r1, r1);

            to.assertValuesOnly(1, 1);
        }
    }

}
