/**
 * Copyright 2014 Netflix, Inc.
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

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.*;
import rx.Observer;
import rx.Scheduler.Worker;
import rx.Observable;
import rx.functions.*;
import rx.observers.Observers;
import rx.observers.TestSubscriber;
import rx.subscriptions.CompositeSubscription;

public class TrampolineSchedulerTest extends AbstractSchedulerTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.trampoline();
    }

    @Test
    public final void testMergeWithCurrentThreadScheduler1() {

        final String currentThreadName = Thread.currentThread().getName();

        Observable<Integer> o1 = Observable.<Integer> just(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> just(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).subscribeOn(Schedulers.trampoline()).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().equals(currentThreadName));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public void testNestedTrampolineWithUnsubscribe() {
        final ArrayList<String> workDone = new ArrayList<String>();
        final CompositeSubscription workers = new CompositeSubscription();
        Worker worker = Schedulers.trampoline().createWorker();
        try {
            workers.add(worker);
            worker.schedule(new Action0() {
    
                @Override
                public void call() {
                    workers.add(doWorkOnNewTrampoline("A", workDone));
                }
    
            });
    
            final Worker worker2 = Schedulers.trampoline().createWorker();
            workers.add(worker2);
            worker2.schedule(new Action0() {
    
                @Override
                public void call() {
                    workers.add(doWorkOnNewTrampoline("B", workDone));
                    // we unsubscribe worker2 ... it should not affect work scheduled on a separate Trampoline.Worker
                    worker2.unsubscribe();
                }
    
            });
        
            assertEquals(6, workDone.size());
            assertEquals(Arrays.asList("A.1", "A.B.1", "A.B.2", "B.1", "B.B.1", "B.B.2"), workDone);
        } finally {
            workers.unsubscribe();
        }
    }

    /**
     * This is a regression test for #1702. Concurrent work scheduling that is improperly synchronized can cause an
     * action to be added or removed onto the priority queue during a poll, which can result in NPEs during queue
     * sifting. While it is difficult to isolate the issue directly, we can easily trigger the behavior by spamming the
     * trampoline with enqueue requests from multiple threads concurrently.
     */
    @Test
    public void testTrampolineWorkerHandlesConcurrentScheduling() {
        final Worker trampolineWorker = Schedulers.trampoline().createWorker();
        final Observer<Subscription> observer = Observers.empty();
        final TestSubscriber<Subscription> ts = new TestSubscriber<Subscription>(observer);

        // Spam the trampoline with actions.
        Observable.range(0, 50)
                .flatMap(new Func1<Integer, Observable<Subscription>>() {

                    @Override
                    public Observable<Subscription> call(Integer count) {
                        return Observable.interval(1, TimeUnit.MICROSECONDS).map(
                                new Func1<Long, Subscription>() {

                                     @Override
                                     public Subscription call(Long count) {
                                         return trampolineWorker.schedule(new Action0() {

                                             @Override
                                             public void call() {}

                                         });
                                     }

                                }).limit(100);
                    }

                })
                .subscribeOn(Schedulers.computation())
                .subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    private static Worker doWorkOnNewTrampoline(final String key, final ArrayList<String> workDone) {
        Worker worker = Schedulers.trampoline().createWorker();
        worker.schedule(new Action0() {

            @Override
            public void call() {
                String msg = key + ".1";
                workDone.add(msg);
                System.out.println(msg);
                Worker worker3 = Schedulers.trampoline().createWorker();
                worker3.schedule(createPrintAction(key + ".B.1", workDone));
                worker3.schedule(createPrintAction(key + ".B.2", workDone));
            }

        });
        return worker;
    }

    private static Action0 createPrintAction(final String message, final ArrayList<String> workDone) {
        return new Action0() {

            @Override
            public void call() {
                System.out.println(message);
                workDone.add(message);
            }

        };
    }
}
