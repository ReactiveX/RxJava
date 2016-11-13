/**
 * Copyright 2016 Netflix, Inc.
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
package rx.schedulers;

import static java.util.concurrent.TimeUnit.SECONDS;
import static rx.Observable.just;
import static rx.Observable.merge;

import org.junit.Test;

import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.schedulers.SchedulerWhen;
import rx.observers.TestSubscriber;

public class SchedulerWhenTest {
    @Test
    public void testAsyncMaxConcurrent() {
        TestScheduler tSched = new TestScheduler();
        SchedulerWhen sched = maxConcurrentScheduler(tSched);
        TestSubscriber<Long> tSub = TestSubscriber.create();

        asyncWork(sched).subscribe(tSub);

        tSub.assertValueCount(0);

        tSched.advanceTimeBy(0, SECONDS);
        tSub.assertValueCount(0);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(2);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(4);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(5);
        tSub.assertCompleted();

        sched.unsubscribe();
    }

    @Test
    public void testAsyncDelaySubscription() {
        final TestScheduler tSched = new TestScheduler();
        SchedulerWhen sched = throttleScheduler(tSched);
        TestSubscriber<Long> tSub = TestSubscriber.create();

        asyncWork(sched).subscribe(tSub);

        tSub.assertValueCount(0);

        tSched.advanceTimeBy(0, SECONDS);
        tSub.assertValueCount(0);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(1);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(1);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(2);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(2);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(3);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(3);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(4);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(4);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(5);
        tSub.assertCompleted();

        sched.unsubscribe();
    }

    @Test
    public void testSyncMaxConcurrent() {
        TestScheduler tSched = new TestScheduler();
        SchedulerWhen sched = maxConcurrentScheduler(tSched);
        TestSubscriber<Long> tSub = TestSubscriber.create();

        syncWork(sched).subscribe(tSub);

        tSub.assertValueCount(0);
        tSched.advanceTimeBy(0, SECONDS);

        // since all the work is synchronous nothing is blocked and its all done
        tSub.assertValueCount(5);
        tSub.assertCompleted();

        sched.unsubscribe();
    }

    @Test
    public void testSyncDelaySubscription() {
        final TestScheduler tSched = new TestScheduler();
        SchedulerWhen sched = throttleScheduler(tSched);
        TestSubscriber<Long> tSub = TestSubscriber.create();

        syncWork(sched).subscribe(tSub);

        tSub.assertValueCount(0);

        tSched.advanceTimeBy(0, SECONDS);
        tSub.assertValueCount(1);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(2);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(3);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(4);

        tSched.advanceTimeBy(1, SECONDS);
        tSub.assertValueCount(5);
        tSub.assertCompleted();

        sched.unsubscribe();
    }

    private Observable<Long> asyncWork(final Scheduler sched) {
        return Observable.range(1, 5).flatMap(new Func1<Integer, Observable<Long>>() {
            @Override
            public Observable<Long> call(Integer t) {
                return Observable.timer(1, SECONDS, sched);
            }
        });
    }

    private Observable<Long> syncWork(final Scheduler sched) {
        return Observable.range(1, 5).flatMap(new Func1<Integer, Observable<Long>>() {
            @Override
            public Observable<Long> call(Integer t) {
                return Observable.defer(new Func0<Observable<Long>>() {
                    @Override
                    public Observable<Long> call() {
                        return Observable.just(0l);
                    }
                }).subscribeOn(sched);
            }
        });
    }

    private SchedulerWhen maxConcurrentScheduler(TestScheduler tSched) {
        SchedulerWhen sched = new SchedulerWhen(new Func1<Observable<Observable<Completable>>, Completable>() {
            @Override
            public Completable call(Observable<Observable<Completable>> workerActions) {
                Observable<Completable> workers = workerActions.map(new Func1<Observable<Completable>, Completable>() {
                    @Override
                    public Completable call(Observable<Completable> actions) {
                        return Completable.concat(actions);
                    }
                });
                return Completable.merge(workers, 2);
            }
        }, tSched);
        return sched;
    }

    private SchedulerWhen throttleScheduler(final TestScheduler tSched) {
        SchedulerWhen sched = new SchedulerWhen(new Func1<Observable<Observable<Completable>>, Completable>() {
            @Override
            public Completable call(Observable<Observable<Completable>> workerActions) {
                Observable<Completable> workers = workerActions.map(new Func1<Observable<Completable>, Completable>() {
                    @Override
                    public Completable call(Observable<Completable> actions) {
                        return Completable.concat(actions);
                    }
                });
                return Completable.concat(workers.map(new Func1<Completable, Completable>() {
                    @Override
                    public Completable call(Completable worker) {
                        return worker.delay(1, SECONDS, tSched);
                    }
                }));
            }
        }, tSched);
        return sched;
    }

    @Test(timeout = 1000)
    public void testRaceConditions() {
        Scheduler comp = Schedulers.computation();
        Scheduler limited = comp.when(new Func1<Observable<Observable<Completable>>, Completable>() {
            @Override
            public Completable call(Observable<Observable<Completable>> t) {
                return Completable.merge(Observable.merge(t, 10));
            }
        });

        merge(just(just(1).subscribeOn(limited).observeOn(comp)).repeat(1000)).toBlocking().subscribe();
    }
}
