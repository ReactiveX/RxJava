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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.internal.schedulers.IoScheduler;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableMergeMaxConcurrentTest extends RxJavaTest {

    Observer<String> stringObserver;

    @Before
    public void before() {
        stringObserver = TestHelper.mockObserver();
    }

    @Test
    public void whenMaxConcurrentIsOne() {
        for (int i = 0; i < 100; i++) {
            List<Observable<String>> os = new ArrayList<>();
            os.add(Observable.just("one", "two", "three", "four", "five").subscribeOn(Schedulers.newThread()));
            os.add(Observable.just("one", "two", "three", "four", "five").subscribeOn(Schedulers.newThread()));
            os.add(Observable.just("one", "two", "three", "four", "five").subscribeOn(Schedulers.newThread()));

            List<String> expected = Arrays.asList("one", "two", "three", "four", "five", "one", "two", "three", "four", "five", "one", "two", "three", "four", "five");
            Iterator<String> iter = Observable.merge(os, 1).blockingIterable().iterator();
            List<String> actual = new ArrayList<>();
            while (iter.hasNext()) {
                actual.add(iter.next());
            }
            assertEquals(expected, actual);
        }
    }

    @Test
    public void maxConcurrent() {
        for (int times = 0; times < 100; times++) {
            int observableCount = 100;
            // Test maxConcurrent from 2 to 12
            int maxConcurrent = 2 + (times % 10);
            AtomicInteger subscriptionCount = new AtomicInteger(0);

            List<Observable<String>> os = new ArrayList<>();
            List<SubscriptionCheckObservable> scos = new ArrayList<>();
            for (int i = 0; i < observableCount; i++) {
                SubscriptionCheckObservable sco = new SubscriptionCheckObservable(subscriptionCount, maxConcurrent);
                scos.add(sco);
                os.add(Observable.unsafeCreate(sco));
            }

            Iterator<String> iter = Observable.merge(os, maxConcurrent).blockingIterable().iterator();
            List<String> actual = new ArrayList<>();
            while (iter.hasNext()) {
                actual.add(iter.next());
            }
            //            System.out.println("actual: " + actual);
            assertEquals(5 * observableCount, actual.size());
            for (SubscriptionCheckObservable sco : scos) {
                assertFalse(sco.failed);
            }
        }
    }

    private static class SubscriptionCheckObservable implements ObservableSource<String> {

        private final AtomicInteger subscriptionCount;
        private final int maxConcurrent;
        volatile boolean failed;

        SubscriptionCheckObservable(AtomicInteger subscriptionCount, int maxConcurrent) {
            this.subscriptionCount = subscriptionCount;
            this.maxConcurrent = maxConcurrent;
        }

        @Override
        public void subscribe(final Observer<? super String> t1) {
            t1.onSubscribe(Disposable.empty());
            new Thread(new Runnable() {

                @Override
                public void run() {
                    if (subscriptionCount.incrementAndGet() > maxConcurrent) {
                        failed = true;
                    }
                    t1.onNext("one");
                    t1.onNext("two");
                    t1.onNext("three");
                    t1.onNext("four");
                    t1.onNext("five");
                    // We could not decrement subscriptionCount in the unsubscribe method
                    // as "unsubscribe" is not guaranteed to be called before the next "subscribe".
                    subscriptionCount.decrementAndGet();
                    t1.onComplete();
                }

            }).start();
        }

    }

    @Test
    public void mergeALotOfSourcesOneByOneSynchronously() {
        int n = 10000;
        List<Observable<Integer>> sourceList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            sourceList.add(Observable.just(i));
        }
        Iterator<Integer> it = Observable.merge(Observable.fromIterable(sourceList), 1).blockingIterable().iterator();
        int j = 0;
        while (it.hasNext()) {
            assertEquals((Integer)j, it.next());
            j++;
        }
        assertEquals(j, n);
    }

    @Test
    public void mergeALotOfSourcesOneByOneSynchronouslyTakeHalf() {
        int n = 10000;
        List<Observable<Integer>> sourceList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            sourceList.add(Observable.just(i));
        }
        Iterator<Integer> it = Observable.merge(Observable.fromIterable(sourceList), 1).take(n / 2).blockingIterable().iterator();
        int j = 0;
        while (it.hasNext()) {
            assertEquals((Integer)j, it.next());
            j++;
        }
        assertEquals(j, n / 2);
    }

    @Test
    public void simple() {
        for (int i = 1; i < 100; i++) {
            TestObserverEx<Integer> to = new TestObserverEx<>();
            List<Observable<Integer>> sourceList = new ArrayList<>(i);
            List<Integer> result = new ArrayList<>(i);
            for (int j = 1; j <= i; j++) {
                sourceList.add(Observable.just(j));
                result.add(j);
            }

            Observable.merge(sourceList, i).subscribe(to);

            to.assertNoErrors();
            to.assertTerminated();
            to.assertValueSequence(result);
        }
    }

    @Test
    public void simpleOneLess() {
        for (int i = 2; i < 100; i++) {
            TestObserverEx<Integer> to = new TestObserverEx<>();
            List<Observable<Integer>> sourceList = new ArrayList<>(i);
            List<Integer> result = new ArrayList<>(i);
            for (int j = 1; j <= i; j++) {
                sourceList.add(Observable.just(j));
                result.add(j);
            }

            Observable.merge(sourceList, i - 1).subscribe(to);

            to.assertNoErrors();
            to.assertTerminated();
            to.assertValueSequence(result);
        }
    }

    @Test
    public void simpleAsyncLoop() {
        IoScheduler ios = (IoScheduler)Schedulers.io();
        int c = ios.size();
        for (int i = 0; i < 200; i++) {
            simpleAsync();
            int c1 = ios.size();
            if (c + 60 < c1) {
                throw new AssertionError("Worker leak: " + c + " - " + c1);
            }
        }
    }

    @Test
    public void simpleAsync() {
        for (int i = 1; i < 50; i++) {
            TestObserver<Integer> to = new TestObserver<>();
            List<Observable<Integer>> sourceList = new ArrayList<>(i);
            Set<Integer> expected = new HashSet<>(i);
            for (int j = 1; j <= i; j++) {
                sourceList.add(Observable.just(j).subscribeOn(Schedulers.io()));
                expected.add(j);
            }

            Observable.merge(sourceList, i).subscribe(to);

            to.awaitDone(1, TimeUnit.SECONDS);
            to.assertNoErrors();
            Set<Integer> actual = new HashSet<>(to.values());

            assertEquals(expected, actual);
        }
    }

    @Test
    public void simpleOneLessAsyncLoop() {
        for (int i = 0; i < 200; i++) {
            simpleOneLessAsync();
        }
    }

    @Test
    public void simpleOneLessAsync() {
        long t = System.currentTimeMillis();
        for (int i = 2; i < 50; i++) {
            if (System.currentTimeMillis() - t > TimeUnit.SECONDS.toMillis(9)) {
                break;
            }
            TestObserver<Integer> to = new TestObserver<>();
            List<Observable<Integer>> sourceList = new ArrayList<>(i);
            Set<Integer> expected = new HashSet<>(i);
            for (int j = 1; j <= i; j++) {
                sourceList.add(Observable.just(j).subscribeOn(Schedulers.io()));
                expected.add(j);
            }

            Observable.merge(sourceList, i - 1).subscribe(to);

            to.awaitDone(1, TimeUnit.SECONDS);
            to.assertNoErrors();
            Set<Integer> actual = new HashSet<>(to.values());

            assertEquals(expected, actual);
        }
    }

    @Test
    public void take() throws Exception {
        List<Observable<Integer>> sourceList = new ArrayList<>(3);

        sourceList.add(Observable.range(0, 100000).subscribeOn(Schedulers.io()));
        sourceList.add(Observable.range(0, 100000).subscribeOn(Schedulers.io()));
        sourceList.add(Observable.range(0, 100000).subscribeOn(Schedulers.io()));

        TestObserver<Integer> to = new TestObserver<>();

        Observable.merge(sourceList, 2).take(5).subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        to.assertValueCount(5);
    }
}
